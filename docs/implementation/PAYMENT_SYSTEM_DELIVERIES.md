# Sistema de Pagamento para Deliveries - Zapi10

## 📋 Visão Geral

O Zapi10 reutilizará o sistema de pagamento existente, adaptando-o para **deliveries** (entregas) ao invés de eventos.

---

## 🎯 Objetivo

Permitir que:

- **Clientes** paguem por entregas realizadas
- **Organizações** recebam pagamentos de entregas
- **Sistema** processe pagamentos via Stripe, MercadoPago e PayPal

---

## 📦 Componentes Preservados

### ✅ Payment Providers (Mantidos)

```
/payment/providers/
├── StripePaymentProvider.java
├── MercadoPagoPaymentProvider.java
└── PayPalPaymentProvider.java
```

Esses providers serão reutilizados sem alterações.

---

## 🔄 Adaptações Necessárias

### 1. **Entidade Payment** (A Criar)

```java
@Entity
@Table(name = "payments")
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // NOVO: Referência à entrega ao invés de evento
    @ManyToOne
    @JoinColumn(name = "delivery_id", nullable = false)
    private Delivery delivery;

    // Quem pagou
    @ManyToOne
    @JoinColumn(name = "payer_id", nullable = false)
    private User payer;

    // Para qual organização
    @ManyToOne
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Column(name = "amount", nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PaymentStatus status; // PENDING, COMPLETED, FAILED, REFUNDED

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false)
    private PaymentMethod paymentMethod; // STRIPE, MERCADOPAGO, PAYPAL, PIX

    @Column(name = "transaction_id", unique = true)
    private String transactionId;

    @Column(name = "payment_date")
    private LocalDateTime paymentDate;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
```

### 2. **Migration V44** (A Criar)

```sql
-- V44__create_payments_for_deliveries.sql

CREATE TABLE payments (
    id BIGSERIAL PRIMARY KEY,
    delivery_id BIGINT NOT NULL REFERENCES deliveries(id) ON DELETE CASCADE,
    payer_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    organization_id BIGINT NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    amount DECIMAL(10, 2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    payment_method VARCHAR(20) NOT NULL,
    transaction_id VARCHAR(255) UNIQUE,
    payment_date TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE INDEX idx_payments_delivery ON payments(delivery_id);
CREATE INDEX idx_payments_payer ON payments(payer_id);
CREATE INDEX idx_payments_organization ON payments(organization_id);
CREATE INDEX idx_payments_status ON payments(status);
CREATE INDEX idx_payments_transaction_id ON payments(transaction_id);
```

### 3. **Enums** (A Criar)

```java
public enum PaymentStatus {
    PENDING,
    COMPLETED,
    FAILED,
    REFUNDED,
    CANCELLED
}

public enum PaymentMethod {
    STRIPE,
    MERCADOPAGO,
    PAYPAL,
    PIX,
    CASH
}
```

---

## 🚀 Próximos Passos

### Fase 1: Estrutura Base

1. ✅ Preservar payment providers
2. ⏳ Criar entidade `Delivery` (core do Zapi10)
3. ⏳ Criar entidade `Payment` com referência a `Delivery`
4. ⏳ Criar migration V44

### Fase 2: Repositories e Services

5. ⏳ Criar `PaymentRepository`
6. ⏳ Criar `PaymentService` com métodos:
   - `createPayment(Delivery, User, PaymentMethod, BigDecimal)`
   - `processPayment(Long paymentId)`
   - `refundPayment(Long paymentId)`
   - `getPaymentsByDelivery(Long deliveryId)`
   - `getPaymentsByUser(UUID userId)`

### Fase 3: Controllers

7. ⏳ Criar `PaymentController` com endpoints:
   - `POST /api/payments` - Criar pagamento
   - `POST /api/payments/{id}/process` - Processar pagamento
   - `POST /api/payments/{id}/refund` - Reembolso
   - `GET /api/payments/delivery/{deliveryId}` - Pagamentos de uma entrega
   - `GET /api/payments/user/{userId}` - Pagamentos de um usuário

### Fase 4: Webhooks

8. ⏳ Criar `PaymentWebhookController` para callbacks dos providers

---

## 🔗 Relacionamentos

```
Delivery (1) ─────── (N) Payment
User (1) ────────────── (N) Payment (como pagador)
Organization (1) ────── (N) Payment (como recebedor)
```

---

## 💡 Diferenças: Eventos vs Deliveries

| Aspecto                | Sistema Antigo (Eventos)    | Sistema Novo (Deliveries)                  |
| ---------------------- | --------------------------- | ------------------------------------------ |
| **Entidade Principal** | Event                       | Delivery                                   |
| **Quem Paga**          | Participante (User)         | Cliente (User)                             |
| **Para Quem**          | Organizador (Organization)  | Organização de Logística                   |
| **Quando**             | Inscrição no evento         | Após conclusão da entrega                  |
| **Métodos**            | Stripe, MercadoPago, PayPal | Stripe, MercadoPago, PayPal, PIX, Dinheiro |

---

## 📝 Notas

1. **Payment Providers**: Reutilizados sem alterações
2. **Tabela Antiga**: `payment_events` foi removida na V42
3. **Nova Tabela**: `payments` será criada na V44
4. **Foco**: Pagamentos de entregas, não de eventos

---

## ✅ Status Atual

- ✅ Payment providers preservados
- ✅ Código de eventos removido
- ✅ Sistema pronto para receber Payment para Deliveries
- ⏳ Aguardando criação da entidade `Delivery`

---

**Próximo Passo**: Criar a entidade `Delivery` antes de implementar o sistema de pagamento.
