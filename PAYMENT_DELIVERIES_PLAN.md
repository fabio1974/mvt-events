# 🎯 Zapi10 - Sistema de Pagamento para Deliveries

## 📋 Status Atual

```
✅ LIMPEZA COMPLETA
├── 42 arquivos de eventos removidos
├── 4 tabelas de banco removidas
├── Payment providers preservados
└── Sistema compilando sem erros
```

---

## 💳 Sistema de Pagamento Preservado

### Payment Providers Disponíveis

```
/src/main/java/com/mvt/mvt_events/payment/providers/
├── ✅ StripePaymentProvider.java
├── ✅ MercadoPagoPaymentProvider.java
└── ✅ PayPalPaymentProvider.java
```

**Status**: Prontos para uso em deliveries

---

## 🔄 Mudança de Paradigma

### ❌ Sistema Antigo: EVENTOS

```
User ──► Registration ──► Event ──► Payment ──► Organization
         (inscrição)      (evento)  (pagamento) (organizador)
```

### ✅ Sistema Novo: DELIVERIES

```
User ──► Delivery ──► Payment ──► Organization
(cliente) (entrega)   (pagamento) (logística)
```

---

## 🏗️ Arquitetura de Pagamento para Deliveries

### Entidades Core

```java
// 1. Delivery (a criar)
Delivery {
    id: Long
    client: User           // Quem solicitou
    courier: User          // Quem entrega
    organization: Organization  // Empresa de logística
    contract: Contract     // Contrato de serviço
    status: DeliveryStatus
    origin: Address
    destination: Address
    scheduledDate: LocalDateTime
    completedDate: LocalDateTime
}

// 2. Payment (a recriar)
Payment {
    id: Long
    delivery: Delivery     // MUDANÇA: Era "event", agora "delivery"
    payer: User            // Cliente que paga
    organization: Organization  // Recebedor
    amount: BigDecimal
    status: PaymentStatus
    method: PaymentMethod
    transactionId: String
}
```

---

## 💰 Fluxo de Pagamento

### 1. Cliente Solicita Entrega

```
POST /api/deliveries
{
  "origin": {...},
  "destination": {...},
  "scheduledDate": "2025-10-23T10:00:00"
}
```

### 2. Sistema Calcula Valor

```java
BigDecimal price = calculateDeliveryPrice(
    distance,
    weight,
    urgency,
    contract.pricing
);
```

### 3. Cliente Confirma Pagamento

```
POST /api/payments
{
  "deliveryId": 123,
  "paymentMethod": "STRIPE",
  "amount": 25.50
}
```

### 4. Sistema Processa

```java
// Usa provider preservado
StripePaymentProvider.processPayment(payment);

// Atualiza status
payment.setStatus(COMPLETED);
delivery.setStatus(CONFIRMED);
```

### 5. Courier Realiza Entrega

```
PATCH /api/deliveries/123/status
{
  "status": "DELIVERED"
}
```

### 6. Pagamento para Organização

```java
// Sistema transfere para organização
transferToOrganization(
    payment.getOrganization(),
    payment.getAmount()
);
```

---

## 🛠️ Implementação Necessária

### Fase 1: Entidades Base ⏳

```
[ ] Delivery.java
[ ] DeliveryStatus.java (enum)
[ ] Payment.java (adaptado)
[ ] PaymentStatus.java (enum)
[ ] PaymentMethod.java (enum)
```

### Fase 2: Repositories ⏳

```
[ ] DeliveryRepository.java
[ ] PaymentRepository.java
```

### Fase 3: Services ⏳

```
[ ] DeliveryService.java
    ├── createDelivery()
    ├── assignCourier()
    ├── updateStatus()
    └── calculatePrice()

[ ] PaymentService.java
    ├── createPayment()
    ├── processPayment()
    ├── refundPayment()
    └── transferToOrganization()
```

### Fase 4: Controllers ⏳

```
[ ] DeliveryController.java
    ├── POST   /api/deliveries
    ├── GET    /api/deliveries/{id}
    ├── PATCH  /api/deliveries/{id}/status
    └── GET    /api/deliveries/user/{userId}

[ ] PaymentController.java
    ├── POST   /api/payments
    ├── POST   /api/payments/{id}/process
    ├── POST   /api/payments/{id}/refund
    └── GET    /api/payments/delivery/{deliveryId}

[ ] PaymentWebhookController.java
    ├── POST   /api/webhooks/stripe
    ├── POST   /api/webhooks/mercadopago
    └── POST   /api/webhooks/paypal
```

### Fase 5: Migration ⏳

```
[ ] V44__create_deliveries.sql
[ ] V45__create_payments_for_deliveries.sql
```

---

## 🎨 Métodos de Pagamento

| Provider        | Status           | Uso                      |
| --------------- | ---------------- | ------------------------ |
| **Stripe**      | ✅ Pronto        | Cartão de crédito/débito |
| **MercadoPago** | ✅ Pronto        | PIX, boleto, cartão      |
| **PayPal**      | ✅ Pronto        | PayPal account           |
| **PIX**         | ⏳ A implementar | Via MercadoPago          |
| **Dinheiro**    | ⏳ A implementar | Pagamento na entrega     |

---

## 📊 Exemplo de Uso

### Criar Delivery + Payment

```bash
# 1. Cliente cria delivery
curl -X POST http://localhost:8080/api/deliveries \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer {token}" \
  -d '{
    "origin": {
      "street": "Rua A, 123",
      "city": "Sobral",
      "state": "CE"
    },
    "destination": {
      "street": "Rua B, 456",
      "city": "Fortaleza",
      "state": "CE"
    },
    "scheduledDate": "2025-10-23T10:00:00"
  }'

# Response:
{
  "id": 123,
  "price": 25.50,
  "status": "PENDING_PAYMENT"
}

# 2. Cliente paga delivery
curl -X POST http://localhost:8080/api/payments \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer {token}" \
  -d '{
    "deliveryId": 123,
    "paymentMethod": "STRIPE",
    "cardToken": "tok_visa"
  }'

# Response:
{
  "id": 456,
  "status": "COMPLETED",
  "transactionId": "pi_abc123",
  "delivery": {
    "id": 123,
    "status": "CONFIRMED"  // Mudou após pagamento
  }
}
```

---

## ✅ Vantagens do Sistema

1. **Reutilização**: Payment providers já testados
2. **Flexibilidade**: Múltiplos métodos de pagamento
3. **Rastreabilidade**: Cada delivery tem seu payment
4. **Segurança**: Processamento via providers certificados
5. **Escalabilidade**: Webhooks para processamento assíncrono

---

## 📝 Notas Importantes

1. **Payment != PaymentEvent**:

   - `PaymentEvent` era específico para eventos e foi removido
   - `Payment` será recriado para deliveries

2. **Providers Preservados**:

   - Não precisam de alteração
   - Já funcionam com qualquer entidade

3. **Webhooks**:

   - Necessários para callbacks de Stripe, MercadoPago, PayPal
   - Devem atualizar status de Payment e Delivery

4. **Refunds**:
   - Implementar lógica de reembolso
   - Atualizar status de Delivery para CANCELLED

---

## 🚀 Próximo Passo

**Decidir**: Qual implementar primeiro?

### Opção A: Contratos (Employment + Service)

- Permite testar relacionamentos N:M
- Valida triggers de `is_primary`
- Base para Deliveries

### Opção B: Deliveries + Payments

- Core do negócio Zapi10
- Valida payment providers
- Mais valor imediato

**Recomendação**: Começar por **Opção A (Contratos)** pois são pré-requisito para Deliveries.

---

**Documentação**: `/docs/implementation/PAYMENT_SYSTEM_DELIVERIES.md`  
**Status**: Sistema pronto para receber implementação ✅
