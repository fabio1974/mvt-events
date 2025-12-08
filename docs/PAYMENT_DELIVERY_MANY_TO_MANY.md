# 🏗️ Decisão Arquitetural: Payment-Delivery Many-to-Many

**Data:** 02 de Dezembro de 2025  
**Status:** ✅ APROVADO E IMPLEMENTADO

---

## 🤔 Problema Original

O modelo inicial tinha um relacionamento **1:1** entre Payment e Delivery:

```
Payment (N) → (1) Delivery
```

**Limitações:**
- ❌ Um pagamento só podia cobrir UMA entrega
- ❌ Cliente precisava fazer múltiplos pagamentos PIX (um por entrega)
- ❌ UX ruim: cliente vê 5 QR Codes diferentes
- ❌ Custos: 5x taxa do Iugu (R$ 0,59 cada)

---

## 💡 Solução: Many-to-Many

Novo modelo:

```
Payment (1) ←→ (N) Delivery
```

**Benefícios:**
- ✅ Um pagamento pode incluir múltiplas entregas
- ✅ Cliente faz UM pagamento PIX para todas as entregas pendentes
- ✅ UX melhor: um QR Code, uma transação
- ✅ Economia: uma taxa do Iugu para N entregas

---

## 📊 Modelo de Dados

### Tabelas

#### `payments` (existente)
```sql
id                  BIGINT PRIMARY KEY
payer_id            UUID NOT NULL
amount              DECIMAL(10,2) NOT NULL
status              VARCHAR(20) NOT NULL
iugu_invoice_id     VARCHAR(100) UNIQUE
pix_qr_code         TEXT
pix_qr_code_url     TEXT
expires_at          TIMESTAMP
split_rules         JSONB
created_at          TIMESTAMP
```

#### `payment_deliveries` (nova - tabela associativa)
```sql
payment_id          BIGINT NOT NULL
delivery_id         BIGINT NOT NULL

PRIMARY KEY (payment_id, delivery_id)
FOREIGN KEY (payment_id) REFERENCES payments(id) ON DELETE CASCADE
FOREIGN KEY (payment_id) REFERENCES deliveries(id) ON DELETE CASCADE

INDEX idx_payment_deliveries_payment_id
INDEX idx_payment_deliveries_delivery_id
```

#### `deliveries` (existente - sem mudanças)
```sql
id                  BIGINT PRIMARY KEY
client_id           UUID NOT NULL
courier_id          UUID
status              VARCHAR(20) NOT NULL
amount              DECIMAL(10,2)
...
```

---

## 🔄 Fluxo de Pagamento

### Cenário: Cliente tem 5 entregas completadas

**1. Cliente solicita pagamento**

Backend busca entregas elegíveis:
```sql
SELECT * FROM deliveries
WHERE client_id = 'xxx'
AND status = 'COMPLETED'
AND id NOT IN (
    SELECT delivery_id FROM payment_deliveries
    JOIN payments ON payments.id = payment_deliveries.payment_id
    WHERE payments.status IN ('PENDING', 'COMPLETED')
)
ORDER BY created_at ASC
LIMIT 10; -- Máximo de entregas por pagamento
```

**2. Sistema calcula valor total**

```java
BigDecimal totalAmount = deliveries.stream()
    .map(Delivery::getAmount)
    .reduce(BigDecimal.ZERO, BigDecimal::add);

// Exemplo: 5 entregas de R$ 20 = R$ 100
```

**3. Sistema cria fatura PIX única**

```java
PaymentRequest request = new PaymentRequest();
request.setDeliveryIds(List.of(1L, 2L, 3L, 4L, 5L));
request.setAmount(new BigDecimal("100.00"));
request.setClientEmail("cliente@example.com");
request.setMotoboyAccountId("acc_motoboy");
request.setManagerAccountId("acc_gestor");
```

**4. Iugu cria fatura com split**

- **R$ 87,00** → Motoboy (87%)
- **R$ 5,00** → Gestor (5%)
- **R$ 8,00** → Plataforma (8%)

**5. Payment criado e associado às 5 deliveries**

```sql
INSERT INTO payments (amount, payer_id, iugu_invoice_id, ...)
VALUES (100.00, 'xxx', 'INV123', ...);

INSERT INTO payment_deliveries (payment_id, delivery_id) VALUES
(1001, 1),
(1001, 2),
(1001, 3),
(1001, 4),
(1001, 5);
```

**6. Cliente paga via PIX**

- Escaneia UM QR Code
- Paga R$ 100,00
- Dinheiro cai automaticamente nas contas (Iugu split)

**7. Webhook confirma pagamento**

```java
paymentService.processPaymentConfirmation("INV123");
// Payment status: PENDING → COMPLETED
// As 5 deliveries agora estão pagas
```

---

## 📝 Mudanças no Código

### Payment.java

**ANTES:**
```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "delivery_id", nullable = false)
private Delivery delivery;
```

**DEPOIS:**
```java
@ManyToMany(fetch = FetchType.LAZY)
@JoinTable(
    name = "payment_deliveries",
    joinColumns = @JoinColumn(name = "payment_id"),
    inverseJoinColumns = @JoinColumn(name = "delivery_id")
)
private Set<Delivery> deliveries = new HashSet<>();
```

---

### PaymentRequest.java

**ANTES:**
```java
@NotNull(message = "ID da entrega é obrigatório")
private Long deliveryId;
```

**DEPOIS:**
```java
@NotEmpty(message = "Lista de entregas é obrigatória")
@Size(min = 1, max = 10, message = "Mínimo 1, máximo 10 entregas por pagamento")
private List<Long> deliveryIds;
```

---

### PaymentService.java

**ANTES:**
```java
Delivery delivery = deliveryRepository.findById(request.getDeliveryId())
    .orElseThrow(...);

payment.setDelivery(delivery);
```

**DEPOIS:**
```java
Set<Delivery> deliveries = deliveryRepository.findAllById(request.getDeliveryIds())
    .stream()
    .collect(Collectors.toSet());

// Validar todas COMPLETED e não pagas
deliveries.forEach(d -> {
    if (d.getStatus() != DeliveryStatus.COMPLETED) {
        throw new IllegalArgumentException("Delivery " + d.getId() + " não está COMPLETED");
    }
});

BigDecimal totalAmount = deliveries.stream()
    .map(Delivery::getAmount)
    .reduce(BigDecimal.ZERO, BigDecimal::add);

payment.setDeliveries(deliveries);
```

---

## 📊 Relatórios

### Relatório para o motoboy

**Query SQL:**
```sql
SELECT 
    p.id AS payment_id,
    p.amount AS total_paid,
    p.created_at AS paid_at,
    (p.split_rules->'motoboy'->>'amount')::DECIMAL AS motoboy_amount,
    d.id AS delivery_id,
    d.from_address,
    d.to_address,
    d.amount AS delivery_amount
FROM payments p
JOIN payment_deliveries pd ON pd.payment_id = p.id
JOIN deliveries d ON d.id = pd.delivery_id
WHERE d.courier_id = 'MOTOBOY_UUID'
AND p.status = 'COMPLETED'
ORDER BY p.created_at DESC, d.id;
```

**Resultado:**
| payment_id | total_paid | paid_at | motoboy_amount | delivery_id | from_address | to_address | delivery_amount |
|------------|------------|---------|----------------|-------------|--------------|------------|-----------------|
| 1001 | R$ 100,00 | 2025-12-02 23:00 | R$ 87,00 | 1 | Rua A | Rua B | R$ 20,00 |
| 1001 | R$ 100,00 | 2025-12-02 23:00 | R$ 87,00 | 2 | Rua C | Rua D | R$ 20,00 |
| 1001 | R$ 100,00 | 2025-12-02 23:00 | R$ 87,00 | 3 | Rua E | Rua F | R$ 20,00 |
| 1001 | R$ 100,00 | 2025-12-02 23:00 | R$ 87,00 | 4 | Rua G | Rua H | R$ 20,00 |
| 1001 | R$ 100,00 | 2025-12-02 23:00 | R$ 87,00 | 5 | Rua I | Rua J | R$ 20,00 |

**Interpretação:**
- Motoboy recebeu **R$ 87,00** referente a **5 entregas**
- Cliente pagou **R$ 100,00** de uma vez
- Cada entrega vale **R$ 20,00**

---

### Relatório agregado (dashboard)

```sql
SELECT 
    d.courier_id,
    u.name AS courier_name,
    COUNT(DISTINCT p.id) AS total_payments,
    COUNT(DISTINCT d.id) AS total_deliveries,
    SUM((p.split_rules->'motoboy'->>'amount')::DECIMAL) AS total_earnings,
    AVG(d.amount) AS avg_delivery_value
FROM payments p
JOIN payment_deliveries pd ON pd.payment_id = p.id
JOIN deliveries d ON d.id = pd.delivery_id
JOIN users u ON u.id = d.courier_id
WHERE p.status = 'COMPLETED'
AND p.created_at >= '2025-12-01'
AND p.created_at < '2025-12-31'
GROUP BY d.courier_id, u.name
ORDER BY total_earnings DESC;
```

---

## ❓ FAQ: PayoutItem é necessário?

### **Resposta: NÃO!**

**Razões:**

1. **Iugu faz split automático**
   - API do Iugu distribui valores instantaneamente
   - Dinheiro cai direto na conta de cada um (D+1)
   - Não há "repasse manual" a fazer

2. **splitRules (JSONB) é suficiente para auditoria**
   ```json
   {
     "motoboy": {
       "accountId": "acc_motoboy_123",
       "amount": 87.00,
       "percent": 87
     },
     "manager": {
       "accountId": "acc_gestor_456",
       "amount": 5.00,
       "percent": 5
     },
     "platform": {
       "amount": 8.00,
       "percent": 8
     }
   }
   ```

3. **Relatórios detalhados via JOIN**
   - `payment_deliveries` lista todas as entregas
   - `splitRules` mostra quanto cada um recebeu
   - Nenhuma informação perdida

4. **PayoutItem era para repasses manuais**
   - Útil se tivesse que "processar repasses" manualmente
   - Com Iugu split, isso é desnecessário
   - Adiciona complexidade sem benefício

---

## ✅ Checklist de Implementação

- [x] Migration V6 criada (`payment_deliveries` table)
- [x] Documentação arquitetural completa
- [ ] Atualizar `Payment.java` (@ManyToMany)
- [ ] Atualizar `PaymentRequest.java` (deliveryIds List)
- [ ] Atualizar `PaymentService.createInvoiceWithSplit`
- [ ] Adicionar `DeliveryRepository.findCompletedUnpaidByPayer`
- [ ] Atualizar `PaymentController` docs
- [ ] Testar fluxo completo
- [ ] Atualizar `IUGU_PAYMENT_IMPLEMENTATION.md`

---

## 🎯 Próximos Passos

1. ✅ Aplicar Migration V6 ao banco
2. 🔄 Refatorar código Java (Payment, Service, Controller)
3. 🧪 Testes end-to-end
4. 📊 Criar endpoint de relatórios para motoboy
5. 📱 Atualizar frontend (mostrar lista de entregas no QR Code)

---

## 📚 Referências

- [Iugu Split Documentation](https://dev.iugu.com/reference/splits)
- [JPA Many-to-Many](https://www.baeldung.com/jpa-many-to-many)
- [Flyway Migrations Best Practices](https://flywaydb.org/documentation/concepts/migrations)

---

**Conclusão:** Modelo simplificado, mais eficiente e reflete a realidade de como Iugu funciona. PayoutItem não é necessário! 🎉
