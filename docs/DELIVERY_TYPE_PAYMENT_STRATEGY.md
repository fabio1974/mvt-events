# 💰 Estratégia: DeliveryType e Momentos de Pagamento

## 🎯 Objetivo

Diferenciar **entregas de objetos** e **viagens de passageiros**, com momentos de pagamento distintos:

| Tipo | Quando Paga | Método |
|------|-------------|--------|
| **DELIVERY** (entrega) | ❗ **Quando motoboy ACEITA** | PIX ou Cartão |
| **RIDE** (viagem) | ❗ **Quando motoboy INICIA viagem** | PIX ou Cartão |

---

## 💳 Split de Pagamento

### Regras de Split

| Cenário | Courier | Organizer | Plataforma |
|---------|---------|-----------|------------|
| **Com Organizer** | 87% | 5% | 8% |
| **Sem Organizer** (criado por CUSTOMER) | 87% | - | 13% |

### Quando NÃO há Organizer?

Deliveries criadas diretamente por um **CUSTOMER** (cliente final) não possuem a figura do organizer. Neste caso:
- O split é feito apenas entre **courier (87%)** e **plataforma (13%)**
- A plataforma absorve automaticamente os 5% que iriam para o organizer

### Implementação no Pagar.me

```java
// Com organizer (3 participantes)
splits = [
    { amount: courierAmount, recipientId: "re_courier", type: "flat" },
    { amount: organizerAmount, recipientId: "re_organizer", type: "flat" }
    // Plataforma (8%) = remainder automático
]

// Sem organizer (2 participantes)
splits = [
    { amount: courierAmount, recipientId: "re_courier", type: "flat" }
    // Plataforma (13%) = remainder automático
]
```

---

## 💳 Pagamento via Cartão de Crédito

### Endpoint Pagar.me: `POST /core/v5/orders`

```json
{
    "closed": true,
    "items": [
        {
            "amount": 2990,
            "description": "Entrega #123",
            "quantity": 1,
            "code": "DELIVERY"
        }
    ],
    "customer": {
        "name": "Cliente",
        "type": "individual",
        "email": "cliente@email.com",
        "document": "12345678900"
    },
    "payments": [
        {
            "payment_method": "credit_card",
            "credit_card": {
                "operation_type": "auth_and_capture",
                "installments": 1,
                "statement_descriptor": "ZAPI10",
                "card_token": "{{card_token}}",
                "card": {
                    "billing_address": {
                        "line_1": "123, Rua Exemplo, Centro",
                        "zip_code": "01234567",
                        "city": "São Paulo",
                        "state": "SP",
                        "country": "BR"
                    }
                }
            },
            "split": [
                {
                    "amount": 2601,
                    "recipient_id": "re_courier_id",
                    "type": "flat",
                    "options": {
                        "charge_processing_fee": false,
                        "charge_remainder_fee": false,
                        "liable": false
                    }
                },
                {
                    "amount": 150,
                    "recipient_id": "re_organizer_id",
                    "type": "flat",
                    "options": {
                        "charge_processing_fee": false,
                        "charge_remainder_fee": false,
                        "liable": false
                    }
                }
            ]
        }
    ]
}
```

### Observações Importantes:
- **`liable: false`** para courier e organizer - a plataforma assume o risco de chargebacks
- **`charge_processing_fee: false`** - a plataforma paga todas as taxas
- A plataforma recebe o **remainder** automaticamente (não precisa ir no array)
- **`statement_descriptor`** máximo 13 caracteres (aparece na fatura do cliente)

---

## 🏗️ Arquitetura Implementada

### 1️⃣ Enum `DeliveryType`

```java
public enum DeliveryType {
    DELIVERY,  // Entrega de objeto (ex: comida, pacote)
    RIDE       // Viagem de passageiro (ex: Uber)
}
```

### 2️⃣ Enum `PaymentTiming` (quando o pagamento deve acontecer)

```java
public enum PaymentTiming {
    ON_ACCEPT,        // Paga quando motoboy aceita (DELIVERY)
    ON_TRANSIT_START  // Paga quando motoboy inicia viagem (RIDE)
}
```

### 3️⃣ Adicionar campos na entidade `Delivery`

```java
@Entity
@Table(name = "deliveries")
public class Delivery extends BaseEntity {
    
    // ... campos existentes ...
    
    /**
     * Tipo de entrega: DELIVERY (objeto) ou RIDE (passageiro)
     */
    @NotNull(message = "Tipo de entrega é obrigatório")
    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_type", nullable = false, length = 20)
    @Visible(table = true, form = true, filter = true)
    private DeliveryType deliveryType;
    
    /**
     * Indica se o pagamento já foi realizado.
     * - Para DELIVERY: true após pagamento quando motoboy aceita
     * - Para RIDE: true após pagamento quando inicia viagem
     */
    @Column(name = "payment_completed", nullable = false)
    @Visible(table = true, form = false, filter = true)
    private Boolean paymentCompleted = false;
    
    /**
     * Indica se o pagamento foi capturado/confirmado.
     * Para cartão: muda de false para true quando captura é confirmada.
     */
    @Column(name = "payment_captured", nullable = false)
    @Visible(table = false, form = false, filter = true)
    private Boolean paymentCaptured = false;
    
    /**
     * Retorna quando o pagamento deve ser feito baseado no tipo
     */
    public PaymentTiming getPaymentTiming() {
        return deliveryType == DeliveryType.DELIVERY 
            ? PaymentTiming.ON_ACCEPT 
            : PaymentTiming.ON_TRANSIT_START;
    }
}
```

---

## 📊 Fluxos Detalhados

### 🚚 Fluxo DELIVERY (entrega de objeto)

```
1. Cliente cria solicitação no mobile (SEM PAGAR)
   POST /api/deliveries
   Body: {
     deliveryType: "DELIVERY",
     paymentCompleted: false,  ← ainda não pago
     fromAddress: "...",
     toAddress: "...",
     ...
   }
   → Status: PENDING
   → Mobile mostra: "Aguardando motoboy aceitar..."

2. Motoboy vê solicitação no app
   GET /api/deliveries/available
   → Lista entregas PENDING próximas
   → Mostra valor estimado: R$ 15,00

3. Motoboy clica "Aceitar Entrega"
   PATCH /api/deliveries/{id}/accept
   
   → ❌ Backend valida: paymentCompleted == false
   → ❌ Backend retorna: HTTP 402 Payment Required
   
   Response: {
     "error": "PAYMENT_REQUIRED",
     "message": "Cliente precisa pagar antes de aceitar entrega",
     "amount": "15.00",
     "paymentMethods": ["PIX", "CREDIT_CARD"]
   }

4. Mobile cliente recebe notificação
   → "Um motoboy quer aceitar sua entrega! Confirme o pagamento."
   → Tela de pagamento abre automaticamente
   → Cliente escolhe PIX ou Cartão e paga

5. Backend confirma pagamento
   POST /api/payments/confirm
   → Payment.status = PAID
   → Delivery.paymentCompleted = true
   → Notifica mobile do motoboy: "Pagamento confirmado!"

6. Motoboy clica "Aceitar Entrega" novamente (ou automático)
   PATCH /api/deliveries/{id}/accept
   → Validação: ✅ paymentCompleted == true
   → Status: ACCEPTED
   → Entrega aceita com sucesso!

7. Motoboy coleta e inicia
   PATCH /api/deliveries/{id}/pickup
   → Status: IN_TRANSIT

8. Motoboy finaliza
   PATCH /api/deliveries/{id}/complete
   → Status: COMPLETED
   → Transferência split automática (87% motoboy, 5% gerente, 8% plataforma)
```

### 🚗 Fluxo RIDE (viagem de passageiro)

```
1. Cliente cria solicitação no mobile
   → Mobile mostra: "Calcular valor estimado" (baseado em distância)
   → Valor estimado: R$ 25,00
   → ⚠️ AINDA NÃO PAGA

2. Delivery é criada (SEM pagamento)
   POST /api/deliveries
   Body: {
     deliveryType: "RIDE",
     paymentCompleted: false,  ← ainda não pago
     ...
   }
   → Status: PENDING

3. Motoboy aceita viagem (SEM COBRAR)
   PATCH /api/deliveries/{id}/accept
   → Validação: ⚠️ Para RIDE, pode aceitar sem pagamento
   → Status: ACCEPTED
   → Mobile cliente: "Motorista a caminho!"
   → Mobile motoboy: "Vá buscar o passageiro"

4. Motoboy chega e coleta passageiro
   → Motoboy clica "Iniciar Viagem"
   
   PATCH /api/deliveries/{id}/pickup
   
   → ❌ Backend valida: paymentCompleted == false
   → ❌ Backend retorna: HTTP 402 Payment Required
   
   Response: {
     "error": "PAYMENT_REQUIRED",
     "message": "Cliente precisa pagar antes de iniciar viagem",
     "amount": "25.00",
     "paymentMethods": ["PIX", "CREDIT_CARD"]
   }

5. Mobile cliente recebe notificação
   → "Seu motorista está aguardando o pagamento"
   → Tela de pagamento abre automaticamente
   → Cliente escolhe PIX ou Cartão e paga

6. Backend confirma pagamento
   POST /api/payments/confirm
   → Payment.status = PAID
   → Delivery.paymentCompleted = true
   → Notifica mobile do motoboy: "Pagamento confirmado!"

7. Motoboy clica "Iniciar Viagem" novamente
   PATCH /api/deliveries/{id}/pickup
   → Validação: ✅ paymentCompleted == true
   → Status: IN_TRANSIT
   → Viagem iniciada!

8. Motoboy finaliza
   PATCH /api/deliveries/{id}/complete
   → Status: COMPLETED
   → Transferência split automática
```

---

## 🔐 Validações no Backend

### Método `acceptDelivery()`

```java
public Delivery acceptDelivery(Long deliveryId, UUID courierId) {
    Delivery delivery = deliveryRepository.findById(deliveryId)
        .orElseThrow(() -> new RuntimeException("Delivery não encontrada"));
    
    // ⚠️ CRÍTICO: Para DELIVERY, pagamento DEVE estar completo antes de aceitar
    if (delivery.getDeliveryType() == DeliveryType.DELIVERY) {
        if (!delivery.getPaymentCompleted()) {
            throw new PaymentRequiredException(
                "Cliente precisa pagar antes de aceitar entrega",
                delivery.getAmount()
            );
        }
    }
    
    // Para RIDE: pode aceitar sem pagamento (paga ao iniciar viagem depois)
    // Sem validação de pagamento aqui para RIDE
    
    delivery.setStatus(DeliveryStatus.ACCEPTED);
    delivery.setCourier(courier);
    delivery.setAcceptedAt(LocalDateTime.now());
    
    return deliveryRepository.save(delivery);
}
```

### Método `confirmPickup()` (iniciar viagem/transporte)

```java
public Delivery confirmPickup(Long deliveryId, UUID courierId) {
    Delivery delivery = deliveryRepository.findById(deliveryId)
        .orElseThrow(() -> new RuntimeException("Delivery não encontrada"));
    
    // Validar courier
    if (!delivery.getCourier().getId().equals(courierId)) {
        throw new RuntimeException("Delivery não pertence a este courier");
    }
    
    // ⚠️ CRÍTICO: Para RIDE, pagamento DEVE estar completo antes de iniciar
    if (delivery.getDeliveryType() == DeliveryType.RIDE) {
        if (!delivery.getPaymentCompleted()) {
            throw new PaymentRequiredException(
                "Cliente precisa pagar antes de iniciar viagem",
                delivery.getAmount()
            );
        }
    }
    
    // Para DELIVERY: pagamento já foi validado no accept, só continua
    
    delivery.setStatus(DeliveryStatus.IN_TRANSIT);
    delivery.setPickedUpAt(LocalDateTime.now());
    delivery.setInTransitAt(LocalDateTime.now());
    
    return deliveryRepository.save(delivery);
}
```

### Exception customizada

```java
@ResponseStatus(HttpStatus.PAYMENT_REQUIRED) // HTTP 402
public class PaymentRequiredException extends RuntimeException {
    private BigDecimal amount;
    
    public PaymentRequiredException(String message, BigDecimal amount) {
        super(message);
        this.amount = amount;
    }
    
    public PaymentRequiredException(String message) {
        super(message);
    }
}
```

---

## 🗄️ Migration V48

```sql
-- ============================================================================
-- V48: Adicionar delivery_type e campos de controle de pagamento
-- ============================================================================

-- 1. Adicionar coluna delivery_type
ALTER TABLE deliveries 
ADD COLUMN delivery_type VARCHAR(20) NOT NULL DEFAULT 'DELIVERY';

-- 2. Adicionar coluna payment_completed
ALTER TABLE deliveries 
ADD COLUMN payment_completed BOOLEAN NOT NULL DEFAULT false;

-- 3. Adicionar coluna payment_captured
ALTER TABLE deliveries 
ADD COLUMN payment_captured BOOLEAN NOT NULL DEFAULT false;

-- 4. Atualizar entregas existentes (assumir que são DELIVERY e já pagas)
-- Apenas entregas já aceitas/completadas são marcadas como pagas
UPDATE deliveries 
SET delivery_type = 'DELIVERY',
    payment_completed = true, 
    payment_captured = true
WHERE status IN ('ACCEPTED', 'IN_TRANSIT', 'COMPLETED');

-- Entregas PENDING não têm pagamento ainda (comportamento novo)
UPDATE deliveries 
SET delivery_type = 'DELIVERY',
    payment_completed = false, 
    payment_captured = false
WHERE status = 'PENDING';

-- 5. Adicionar constraint CHECK para delivery_type
ALTER TABLE deliveries 
ADD CONSTRAINT chk_delivery_type 
    CHECK (delivery_type IN ('DELIVERY', 'RIDE'));

-- 6. Criar índice para queries por tipo
CREATE INDEX idx_deliveries_type_status 
ON deliveries(delivery_type, status) 
WHERE deleted_at IS NULL;

-- 7. Comentários
COMMENT ON COLUMN deliveries.delivery_type IS 
    'Tipo: DELIVERY (entrega de objeto, paga antes) ou RIDE (viagem, paga ao iniciar)';
COMMENT ON COLUMN deliveries.payment_completed IS 
    'Se o pagamento foi realizado (momento varia por tipo)';
COMMENT ON COLUMN deliveries.payment_captured IS 
    'Se o pagamento foi capturado/confirmado pelo gateway';
```

---

## 📱 Mudanças no Mobile

### 1. Criar Delivery (DELIVERY)

```typescript
// Passo 1: Calcular valor
const estimate = await api.post('/deliveries/estimate', {
  fromAddress,
  toAddress,
  deliveryType: 'DELIVERY'
});
// Response: { amount: 15.00, distance: 5.2 }

// Passo 2: Criar delivery SEM PAGAR
const delivery = await api.post('/deliveries', {
  fromAddress,
  toAddress,
  deliveryType: 'DELIVERY',
  paymentCompleted: false,  // ← ainda não pago
  amount: estimate.amount
});

// Passo 3: Aguardar motoboy aceitar...
// (pagamento só será cobrado quando motoboy tentar aceitar)
```

### 2. Criar Delivery (RIDE)

```typescript
// Passo 1: Calcular valor estimado
const estimate = await api.post('/deliveries/estimate', {
  fromAddress,
  toAddress,
  deliveryType: 'RIDE'
});
// Response: { amount: 25.00, distance: 8.5 }

// Passo 2: Criar delivery SEM PAGAR
const delivery = await api.post('/deliveries', {
  fromAddress,
  toAddress,
  deliveryType: 'RIDE',
  paymentCompleted: false,  // ← ainda não pago
  amount: estimate.amount
});

// Passo 3: Aguardar motoboy aceitar...
// (pagamento só será cobrado quando tentar iniciar viagem)
```

### 3. Aceitar Entrega (DELIVERY) - Mobile Motoboy

```typescript
try {
  // Tentar aceitar entrega
  const response = await api.patch(`/deliveries/${id}/accept`);
  
  // Sucesso: pagamento já foi feito
  navigation.navigate('DeliveryAccepted', { delivery: response.data });
  
} catch (error) {
  if (error.status === 402) { // Payment Required
    // Cliente precisa pagar
    Alert.alert(
      'Aguardando Pagamento',
      'O cliente precisa confirmar o pagamento antes de aceitar a entrega.',
      [
        {
          text: 'Notificar Cliente',
          onPress: () => {
            // Envia notificação push pro cliente
            api.post(`/deliveries/${id}/notify-payment-required`);
          }
        },
        { text: 'Cancelar', style: 'cancel' }
      ]
    );
    
    // Listener para quando pagamento for confirmado
    socket.on(`delivery:${id}:payment-confirmed`, () => {
      Alert.alert('Pagamento Confirmado!', 'Pode aceitar a entrega agora.');
    });
  }
}
```

```typescript
try {
  // Tentar iniciar viagem
  const response = await api.patch(`/deliveries/${id}/pickup`);
  
  // Sucesso: pagamento já foi feito
  navigation.navigate('InTransit', { delivery: response.data });
  
} catch (error) {
  if (error.status === 402) { // Payment Required
    // Cliente precisa pagar
    Alert.alert(
      'Aguardando Pagamento',
      'O cliente precisa confirmar o pagamento antes de iniciar a viagem.',
      [
        {
          text: 'Notificar Cliente',
          onPress: () => {
            // Envia notificação push pro cliente
            api.post(`/deliveries/${id}/notify-payment-required`);
          }
        },
        { text: 'Cancelar', style: 'cancel' }
      ]
    );
    
    // Listener para quando pagamento for confirmado
    socket.on(`delivery:${id}:payment-confirmed`, () => {
      Alert.alert('Pagamento Confirmado!', 'Pode iniciar a viagem agora.');
    });
  }
}
```

### 4. Pagar Entrega (DELIVERY) - Mobile Cliente

```typescript
// Cliente recebe notificação quando motoboy tenta aceitar
socket.on(`delivery:${deliveryId}:payment-required`, (data) => {
  // Abrir tela de pagamento
  navigation.navigate('PaymentScreen', {
    deliveryId: data.deliveryId,
    amount: data.amount,
    message: 'Um motoboy quer aceitar sua entrega! Confirme o pagamento.'
  });
});

// Cliente escolhe método e paga
const payment = await api.post('/payments/create', {
  deliveryId,
  amount,
  method: paymentMethod,
  cardToken: cardToken // se cartão
});

await api.post('/payments/confirm', {
  paymentId: payment.id
});

// Notifica motoboy que pode aceitar
// (backend faz automaticamente via WebSocket)
```

### 5. Iniciar Viagem (RIDE) - Mobile Motoboy

```typescript
try {
  // Tentar iniciar viagem
  const response = await api.patch(`/deliveries/${id}/pickup`);
  
  // Sucesso: pagamento já foi feito
  navigation.navigate('InTransit', { delivery: response.data });
  
} catch (error) {
  if (error.status === 402) { // Payment Required
    // Cliente precisa pagar
    Alert.alert(
      'Aguardando Pagamento',
      'O cliente precisa confirmar o pagamento antes de iniciar a viagem.',
      [
        {
          text: 'Notificar Cliente',
          onPress: () => {
            // Envia notificação push pro cliente
            api.post(`/deliveries/${id}/notify-payment-required`);
          }
        },
        { text: 'Cancelar', style: 'cancel' }
      ]
    );
    
    // Listener para quando pagamento for confirmado
    socket.on(`delivery:${id}:payment-confirmed`, () => {
      Alert.alert('Pagamento Confirmado!', 'Pode iniciar a viagem agora.');
    });
  }
}
```

### 6. Pagar Viagem (RIDE) - Mobile Cliente

```typescript
// Cliente recebe notificação ou vê tela automaticamente
socket.on(`delivery:${deliveryId}:payment-required`, (data) => {
  // Abrir tela de pagamento
  navigation.navigate('PaymentScreen', {
    deliveryId: data.deliveryId,
    amount: data.amount,
    message: 'Seu motorista está aguardando o pagamento'
  });
});

// Cliente escolhe método e paga
const payment = await api.post('/payments/create', {
  deliveryId,
  amount,
  method: paymentMethod,
  cardToken: cardToken // se cartão
});

await api.post('/payments/confirm', {
  paymentId: payment.id
});

// Notifica motoboy que pode iniciar
// (backend faz automaticamente via WebSocket)
```

---

## 🔔 Notificações Push

### Eventos a implementar:

1. **`delivery:payment-required`** (para cliente)
   - **DELIVERY**: Quando motoboy tenta aceitar sem pagamento
   - **RIDE**: Quando motoboy tenta iniciar viagem sem pagamento
   - Abre tela de pagamento automaticamente

2. **`delivery:payment-confirmed`** (para motoboy)
   - Quando cliente confirma pagamento
   - **DELIVERY**: Libera botão "Aceitar Entrega"
   - **RIDE**: Libera botão "Iniciar Viagem"

3. **`delivery:payment-failed`** (para ambos)
   - Se pagamento falhar
   - Permite retry

---

## 🎨 Sugestões de UX

### Para DELIVERY:
- Cliente cria solicitação SEM pagar: "Solicitar Entrega (R$ 15,00 estimado)"
- Aguarda motoboy: "Procurando motoboy disponível..."
- Quando motoboy aceita: "Motoboy encontrado! Confirme o pagamento"
- Após pagar: "Entrega aceita! Motoboy a caminho do local de coleta"
- Status: "Aguardando coleta" → "Em transporte" → "Entregue"

### Para RIDE:
- Cliente cria solicitação: "Solicitar Viagem (R$ 25,00 estimado)"
- Aguarda motorista: "Procurando motorista disponível..."
- Quando aceita: "Motorista chegando em 5 min" (sem pagar ainda)
- Quando motorista chega: "Motorista aguardando você!" 
- Quando tenta iniciar: "Confirmar Pagamento (R$ 25,00)" (destaque)
- Após pagar: "Viagem iniciada!"
- Status: "Motorista a caminho" → "Aguardando embarque" → "Em viagem" → "Finalizada"

---

## ✅ Checklist de Implementação

### Backend:
- [ ] Criar enum `DeliveryType`
- [ ] Criar enum `PaymentTiming`
- [ ] Adicionar campos na entidade `Delivery`
- [ ] Criar migration V48
- [ ] Implementar validações em `acceptDelivery()`
- [ ] Implementar validações em `confirmPickup()`
- [ ] Criar `PaymentRequiredException`
- [ ] Adicionar endpoint `/deliveries/estimate`
- [ ] Adicionar endpoint `/deliveries/{id}/notify-payment-required`
- [ ] Implementar WebSocket para notificações de pagamento
- [ ] Atualizar testes unitários

### Mobile:
- [ ] Adicionar campo `deliveryType` no form de criação
- [ ] Implementar fluxo de pagamento ANTES para DELIVERY
- [ ] Implementar fluxo de pagamento DURANTE para RIDE
- [ ] Adicionar tratamento de erro HTTP 402
- [ ] Implementar listeners WebSocket para pagamento
- [ ] Criar tela de "Aguardando Pagamento" (motoboy)
- [ ] Criar notificação push de pagamento requerido
- [ ] Atualizar fluxo de estados/status

---

## 🔒 Segurança

1. **Validar pagamento server-side**: NUNCA confiar no mobile
2. **Tokenizar cartões**: usar Pagar.me tokenization
3. **Timeout de pagamento**: 
   - **DELIVERY**: 5 minutos para pagar após motoboy aceitar
   - **RIDE**: 5 minutos para pagar após motoboy chegar
4. **Cancelamento automático**: se não pagar em 5 min, cancela e libera motoboy
5. **Log de tentativas**: registrar todas as tentativas de aceitar/iniciar sem pagamento
6. **Prevent race conditions**: lock otimista ao aceitar delivery

---

## 📈 Métricas a Monitorar

1. **Taxa de conversão**: 
   - **DELIVERY**: quantos % pagam quando motoboy aceita
   - **RIDE**: quantos % pagam quando solicitado na viagem
2. **Tempo médio de pagamento**: quanto demora entre solicitar pagamento e confirmar
3. **Taxa de cancelamento por não pagamento**: quantos % cancelam quando cobrado
4. **Comparação DELIVERY vs RIDE**: qual tem mais sucesso
5. **Abandono no checkout**: quantos iniciam mas não completam pagamento

---

## 🚀 Próximos Passos

1. **Revisar proposta** com time
2. **Aprovar fluxos** de UX com design
3. **Implementar backend** (2-3 dias)
4. **Implementar mobile** (3-4 dias)
5. **Testar em staging** com Pagar.me sandbox
6. **Deploy gradual** (feature flag)
7. **Monitorar métricas**

---

**Última atualização:** 02/02/2026  
**Status:** 📝 Proposta em análise
