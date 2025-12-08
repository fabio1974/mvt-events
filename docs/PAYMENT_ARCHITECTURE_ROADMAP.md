# 🏗️ Arquitetura de Pagamentos - Roadmap v1.0 → v2.0

**Data**: 2025-12-02  
**Status**: ✅ v1.0 Implementada | 🔮 v2.0 Planejada

---

## 📋 Visão Geral

Evolução da arquitetura de pagamentos para suportar múltiplos métodos de pagamento (recebimento e envio).

---

## ✅ v1.0 - Implementação Atual (2025)

### Arquitetura

```
User (1) ←→ (0..1) BankAccount
                    └── Iugu SubAccount (recebimento PIX)
```

### Casos de Uso

| Role | Tem BankAccount? | Finalidade |
|------|------------------|------------|
| **COURIER** | ✅ Obrigatório | Recebe 87% das entregas via PIX (D+1) |
| **ORGANIZER** | ✅ Obrigatório | Recebe 5% de comissão via PIX (D+1) |
| **CLIENT** | ❌ Não tem | Paga entregas (método não definido ainda) |
| **ADMIN** | ⚠️ Opcional | Gerencia sistema |

### Limitações

- ❌ Cliente não pode pagar (sem integração cartão)
- ❌ User tem apenas 1 conta bancária
- ❌ Não suporta múltiplas formas de pagamento
- ❌ Acoplado ao Iugu (difícil migrar gateway)

---

## 🔮 v2.0 - Arquitetura Futura (2026+)

### Nova Arquitetura

```
User (1) ←→ (0..N) PaymentMethod (abstract)
                    ├── BankAccount (receber via Iugu PIX)
                    ├── CreditCard (pagar via Stripe/Cielo)
                    ├── DebitCard (pagar via Cielo)
                    ├── DigitalWallet (pagar via PicPay/MercadoPago)
                    └── Boleto (pagar via Bradesco/BB)
```

### Design Pattern: **Strategy + Composite**

```java
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "payment_method_type")
public abstract class PaymentMethod {
    @Id
    private UUID id;
    
    @ManyToOne
    private User user;
    
    private PaymentDirection direction; // SEND | RECEIVE
    private PaymentMethodStatus status; // ACTIVE | BLOCKED | EXPIRED
    private Boolean isPrimary;
    
    // Strategy pattern
    public abstract PaymentResult process(PaymentRequest request);
    public abstract boolean canProcess(PaymentRequest request);
}

@Entity
@DiscriminatorValue("BANK_ACCOUNT")
public class BankAccount extends PaymentMethod {
    private String bankCode;
    private String agency;
    private String accountNumber;
    private String iuguAccountId;
    
    @Override
    public PaymentResult process(PaymentRequest request) {
        // Lógica Iugu PIX
    }
}

@Entity
@DiscriminatorValue("CREDIT_CARD")
public class CreditCard extends PaymentMethod {
    private String cardNumber; // encrypted
    private String holderName;
    private String expiryDate;
    private String stripeCardId;
    
    @Override
    public PaymentResult process(PaymentRequest request) {
        // Lógica Stripe/Cielo
    }
}
```

### User com Múltiplos Métodos

```java
@Entity
public class User {
    @OneToMany(mappedBy = "user", cascade = ALL)
    private List<PaymentMethod> paymentMethods = new ArrayList<>();
    
    // Método principal para receber
    public BankAccount getPrimaryReceivingAccount() {
        return paymentMethods.stream()
            .filter(pm -> pm instanceof BankAccount)
            .filter(PaymentMethod::isPrimary)
            .map(pm -> (BankAccount) pm)
            .findFirst()
            .orElse(null);
    }
    
    // Método principal para pagar
    public CreditCard getPrimaryPaymentCard() {
        return paymentMethods.stream()
            .filter(pm -> pm instanceof CreditCard)
            .filter(PaymentMethod::isPrimary)
            .map(pm -> (CreditCard) pm)
            .findFirst()
            .orElse(null);
    }
    
    public boolean canReceivePayments() {
        return getPrimaryReceivingAccount() != null &&
               getPrimaryReceivingAccount().isActive();
    }
    
    public boolean canMakePayments() {
        return paymentMethods.stream()
            .anyMatch(pm -> pm.getDirection() == SEND && pm.isActive());
    }
}
```

---

## 🔄 Migração v1.0 → v2.0

### Fase 1: Preparação (v1.5)

1. **Criar interface PaymentMethodProvider**
   ```java
   public interface PaymentMethodProvider {
       String getProviderName(); // "IUGU", "STRIPE", "CIELO"
       PaymentResult charge(PaymentRequest request);
       PaymentResult payout(PaymentRequest request);
   }
   ```

2. **Refatorar IuguService para implementar interface**
   ```java
   @Service
   public class IuguPaymentProvider implements PaymentMethodProvider {
       @Override
       public String getProviderName() { return "IUGU"; }
       // ... resto da implementação
   }
   ```

3. **Criar PaymentMethodFactory**
   ```java
   @Component
   public class PaymentMethodFactory {
       public PaymentMethodProvider getProvider(String name) {
           return switch (name) {
               case "IUGU" -> iuguProvider;
               case "STRIPE" -> stripeProvider;
               case "CIELO" -> cieloProvider;
               default -> throw new UnsupportedProviderException();
           };
       }
   }
   ```

### Fase 2: Nova Estrutura (v2.0)

1. **Migration V10: Criar tabela payment_methods**
   ```sql
   CREATE TABLE payment_methods (
       id UUID PRIMARY KEY,
       user_id UUID NOT NULL,
       payment_method_type VARCHAR(50) NOT NULL,
       direction VARCHAR(10) NOT NULL, -- SEND | RECEIVE
       status VARCHAR(20) NOT NULL,
       is_primary BOOLEAN DEFAULT false,
       created_at TIMESTAMP DEFAULT now(),
       updated_at TIMESTAMP DEFAULT now(),
       FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
   );
   
   CREATE TABLE bank_accounts_v2 (
       id UUID PRIMARY KEY,
       -- Todos os campos atuais +
       FOREIGN KEY (id) REFERENCES payment_methods(id) ON DELETE CASCADE
   );
   
   CREATE TABLE credit_cards (
       id UUID PRIMARY KEY,
       card_number_encrypted TEXT NOT NULL,
       holder_name VARCHAR(100) NOT NULL,
       expiry_date VARCHAR(7) NOT NULL, -- MM/YYYY
       stripe_card_id VARCHAR(100),
       cielo_card_id VARCHAR(100),
       FOREIGN KEY (id) REFERENCES payment_methods(id) ON DELETE CASCADE
   );
   ```

2. **Migration V11: Migrar dados atuais**
   ```sql
   -- Inserir BankAccounts como PaymentMethods
   INSERT INTO payment_methods (id, user_id, payment_method_type, direction, status, is_primary)
   SELECT 
       gen_random_uuid(),
       user_id,
       'BANK_ACCOUNT',
       'RECEIVE',
       CASE status
           WHEN 'ACTIVE' THEN 'ACTIVE'
           WHEN 'BLOCKED' THEN 'BLOCKED'
           ELSE 'PENDING'
       END,
       true
   FROM bank_accounts;
   
   -- Atualizar bank_accounts_v2 com IDs de payment_methods
   -- ...
   ```

3. **Deprecar campos antigos em User**
   ```java
   @Deprecated(since = "2.0", forRemoval = true)
   @OneToOne(mappedBy = "user")
   private BankAccount bankAccount; // Manter por compatibilidade
   
   @OneToMany(mappedBy = "user")
   private List<PaymentMethod> paymentMethods; // NOVO
   ```

---

## 🎯 Casos de Uso v2.0

### 1. Cliente Paga Corrida com Cartão

```java
@PostMapping("/api/deliveries/{id}/pay")
public PaymentResponse payDelivery(@PathVariable UUID id) {
    Delivery delivery = deliveryService.findById(id);
    User client = delivery.getClient();
    
    // Busca cartão principal do cliente
    CreditCard card = client.getPrimaryPaymentCard();
    if (card == null) {
        throw new PaymentMethodNotFoundException("Cliente sem cartão cadastrado");
    }
    
    // Factory escolhe provider (Stripe ou Cielo)
    PaymentMethodProvider provider = factory.getProvider(card.getProvider());
    
    // Processa pagamento
    PaymentResult result = provider.charge(PaymentRequest.builder()
        .amount(delivery.getTotalAmount())
        .paymentMethod(card)
        .description("Pagamento entrega #" + delivery.getId())
        .build());
    
    // Cria split para motoboy + gerente
    if (result.isSuccess()) {
        createSplitPayment(delivery, result.getTransactionId());
    }
    
    return result;
}
```

### 2. Motoboy Recebe via Múltiplas Contas

```java
@GetMapping("/api/motoboy/bank-accounts")
public List<BankAccount> getMyBankAccounts() {
    User courier = getCurrentUser();
    
    return courier.getPaymentMethods().stream()
        .filter(pm -> pm instanceof BankAccount)
        .filter(pm -> pm.getDirection() == RECEIVE)
        .map(pm -> (BankAccount) pm)
        .toList();
}

@PutMapping("/api/motoboy/bank-accounts/{id}/set-primary")
public void setPrimaryAccount(@PathVariable UUID id) {
    User courier = getCurrentUser();
    
    // Desativa todas
    courier.getPaymentMethods().forEach(pm -> pm.setIsPrimary(false));
    
    // Ativa a escolhida
    BankAccount account = (BankAccount) courier.getPaymentMethods()
        .stream()
        .filter(pm -> pm.getId().equals(id))
        .findFirst()
        .orElseThrow();
    
    account.setIsPrimary(true);
    userRepository.save(courier);
}
```

### 3. Admin Gerencia Múltiplos Gateways

```java
@GetMapping("/api/admin/payment-providers")
public List<PaymentProviderConfig> getProviders() {
    return List.of(
        new PaymentProviderConfig("IUGU", "PIX recebimento", true),
        new PaymentProviderConfig("STRIPE", "Cartão internacional", true),
        new PaymentProviderConfig("CIELO", "Cartão nacional", false),
        new PaymentProviderConfig("MERCADO_PAGO", "Wallet", false)
    );
}
```

---

## 📊 Comparação de Arquiteturas

| Característica | v1.0 (Atual) | v2.0 (Futura) |
|----------------|--------------|---------------|
| **Métodos de Pagamento** | 1 (BankAccount) | N (BankAccount, CreditCard, etc.) |
| **Gateways Suportados** | 1 (Iugu) | N (Iugu, Stripe, Cielo, etc.) |
| **User → Payment** | 1:1 obrigatório | 1:N opcional |
| **Cliente pode pagar?** | ❌ Não | ✅ Sim (cartão) |
| **Motoboy múltiplas contas?** | ❌ Não | ✅ Sim |
| **Troca de gateway** | ❌ Difícil (acoplado) | ✅ Fácil (Strategy pattern) |
| **Complexidade** | 🟢 Baixa | 🟡 Média |
| **Flexibilidade** | 🔴 Baixa | 🟢 Alta |

---

## 🚀 Próximos Passos (Prioridade)

### Curto Prazo (2025 Q4)
- [x] ✅ Implementar v1.0 com BankAccount + Iugu
- [ ] ⏳ Testar sistema atual em produção
- [ ] ⏳ Coletar feedback de motoboys/gerentes

### Médio Prazo (2026 Q1-Q2)
- [ ] 🔮 Implementar v1.5: Interface PaymentMethodProvider
- [ ] 🔮 Refatorar IuguService para usar interface
- [ ] 🔮 Criar PaymentMethodFactory

### Longo Prazo (2026 Q3+)
- [ ] 🔮 Implementar v2.0: Hierarquia PaymentMethod
- [ ] 🔮 Migrar dados (V10, V11)
- [ ] 🔮 Integrar Stripe para cartões
- [ ] 🔮 Integrar Cielo para cartões nacionais
- [ ] 🔮 Suporte a múltiplos métodos por User

---

## 📝 Decisões Arquiteturais

### Por que não fazer v2.0 agora?

1. **YAGNI (You Aren't Gonna Need It)**: Cliente pagar com cartão não é requisito atual
2. **Simplicidade**: v1.0 é mais simples, rápida de implementar e testar
3. **Validação**: Precisa validar modelo de negócio antes de complexificar
4. **Custo**: Integração com múltiplos gateways é cara (mensalidades, taxas)

### Por que planejar v2.0 agora?

1. **Evitar Débito Técnico**: Design v1.0 já contempla expansão futura
2. **Documentação**: Equipe entende a direção arquitetural
3. **Refatoração**: Código v1.0 já usa padrões compatíveis com v2.0
4. **Negócio**: Cliente perguntou sobre pagamento com cartão

---

## ✅ Conclusão

**v1.0 está PERFEITA para o escopo atual:**
- ✅ COURIER/ORGANIZER recebem via Iugu PIX (87/5/8 split)
- ✅ Relacionamento 1:1 opcional (CLIENT não tem BankAccount)
- ✅ Simples, testável, manutenível

**v2.0 será implementada quando necessário:**
- 🔮 Cliente precisar pagar com cartão
- 🔮 Motoboy quiser múltiplas contas
- 🔮 Necessidade de trocar gateway

**A arquitetura atual JÁ PERMITE essa evolução sem reescrever tudo!** 🎯

---

**Mantido por**: Equipe de Arquitetura  
**Última atualização**: 2025-12-02
