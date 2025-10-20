# 🚀 Zapi10 Migration Plan - MVT Events → Zapi10

**Data:** 18 de outubro de 2025  
**Objetivo:** Transformar o projeto MVT Events em Zapi10 (plataforma de entregas)  
**Estratégia:** Aproveitar infraestrutura existente e fazer merge inteligente das tabelas

---

## 📋 Índice

1. [Análise Comparativa](#-análise-comparativa)
2. [Estratégia de Migração](#-estratégia-de-migração)
3. [Mapeamento de Entidades](#-mapeamento-de-entidades)
4. [Plano de Execução](#-plano-de-execução)
5. [Cronograma](#-cronograma)

---

## 🔍 Análise Comparativa

### MVT Events (Estado Atual)

```
Core Entities:
├── User (UUID, role: USER|ORGANIZER|ADMIN)
├── Organization (multi-tenancy)
├── Event
├── EventCategory
├── Registration (inscrição em evento)
├── Payment (pagamento de inscrição)
└── City (referência geográfica)

Features:
✅ Multi-tenancy (Organization)
✅ Sistema de metadata genérico
✅ Autenticação JWT
✅ Filtros dinâmicos
✅ Cascade updates (1:N)
✅ Annotations customizadas (@DisplayLabel, @Visible, @Computed)
```

### Zapi10 (Alvo)

```
Core Entities:
├── User (base, type: CLIENT|COURIER|ADM|ADMIN)
├── CourierProfile (especialização 1:1 de User)
├── ADMProfile (especialização 1:1 de User)
├── Delivery (entrega)
├── Payment (pagamento de entrega)
├── UnifiedPayout (repasse periódico)
├── PayoutItem (item de repasse)
├── Evaluation (avaliação)
├── MunicipalPartnership (parceria institucional)
├── ClientManagerLink (N:M entre Client e ADM)
└── CourierADMLink (N:M entre Courier e ADM) ⭐ NOVO

Features Necessárias:
🆕 Entidades de entrega (Delivery)
🆕 Sistema de repasses (Payout)
🆕 Relacionamento N:M Client↔ADM
🆕 Relacionamento N:M Courier↔ADM ⭐ NOVO
🆕 Avaliações
🆕 Parcerias municipais
♻️ Reutilizar: User, Payment, metadata, auth
```

---

## 🎯 Estratégia de Migração

### Abordagem: **Especialização da Tabela `users`**

Em vez de criar tabelas separadas (`client`, `courier`, `adm`), vamos:

1. ✅ **Manter tabela `users` como base** (já existe, bem estruturada)
2. ✅ **Expandir enum `Role`** para incluir `CLIENT`, `COURIER`, `ADM`
3. ✅ **Criar tabelas especializadas** apenas para campos específicos:
   - `courier_profiles` (dados do motoboy)
   - `adm_profiles` (dados do gerente local)
4. ✅ **Remover entidades de eventos** (Event, EventCategory, Registration)
5. ✅ **Criar entidades de entregas** (Delivery, Evaluation, Payout)

### Vantagens

✅ Não quebra autenticação existente  
✅ Aproveita validações (CPF, email, etc.)  
✅ Mantém sistema de metadata  
✅ Multi-tenancy pode virar multi-region  
✅ Cascade updates reutilizável

---

## 🗺️ Mapeamento de Entidades

### 1. User (Base) - **EXPANDIR**

**Estado Atual:**

```java
@Entity
@Table(name = "users")
public class User {
    UUID id;
    String username;  // email
    String password;
    Role role;  // USER, ORGANIZER, ADMIN
    String name;
    String cpf;
    String phone;
    LocalDate dateOfBirth;
    Gender gender;
    String city, state, country;
    String address;
    String emergencyContact;
    Organization organization;  // ← REMOVER (multi-tenancy)
}
```

**Migração para Zapi10:**

```java
@Entity
@Table(name = "users")
public class User {
    UUID id;
    String username;  // email
    String password;
    Role role;  // CLIENT, COURIER, ADM, ADMIN ← EXPANDIR ENUM
    String name;
    String cpf;
    String phone;
    LocalDate dateOfBirth;
    Gender gender;
    String city, state, country;  // Manter para geolocalização
    String address;
    String emergencyContact;
    Boolean active;  // ← ADICIONAR
    String taxId;  // ← ADICIONAR (para formalização)

    // REMOVER:
    // Organization organization;
}
```

**Migration:**

```sql
-- V20: Expandir roles para Zapi10
ALTER TABLE users DROP CONSTRAINT IF EXISTS users_role_check;
ALTER TABLE users ADD CONSTRAINT users_role_check
    CHECK (role IN ('USER', 'ORGANIZER', 'ADMIN', 'CLIENT', 'COURIER', 'ADM'));

-- Adicionar campos novos
ALTER TABLE users ADD COLUMN IF NOT EXISTS active BOOLEAN DEFAULT true;
ALTER TABLE users ADD COLUMN IF NOT EXISTS tax_id VARCHAR(20);

-- Remover organization_id (será substituído por region)
ALTER TABLE users DROP COLUMN IF EXISTS organization_id;
```

---

### 2. CourierProfile (Nova) - **CRIAR**

Dados específicos de motoboys:

> ⚠️ **IMPORTANTE**: Courier ↔ ADM é **N:M** (um motoboy pode ter vários gerentes)  
> Relacionamento via tabela `courier_adm_links` (criada separadamente)

```java
@Entity
@Table(name = "courier_profiles")
public class CourierProfile extends BaseEntity {

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;  // user.role = COURIER

    // REMOVIDO: @ManyToOne adm_id (agora é N:M via courier_adm_links)    @Column(name = "vehicle_plate", length = 10)
    private String vehiclePlate;

    @Column(name = "vehicle_model", length = 100)
    private String vehicleModel;

    @Column(name = "license_number", length = 20)
    private String licenseNumber;

    @Column(name = "cnh_category", length = 5)
    private String cnhCategory;

    @Column(name = "has_insurance")
    private Boolean hasInsurance = false;

    @Column(name = "insurance_number", length = 50)
    private String insuranceNumber;

    @Column(name = "rating", precision = 3, scale = 2)
    private BigDecimal rating;  // Média de avaliações

    @Column(name = "total_deliveries")
    private Integer totalDeliveries = 0;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private CourierStatus status = CourierStatus.AVAILABLE;

    public enum CourierStatus {
        AVAILABLE,      // Disponível
        ON_DELIVERY,    // Em entrega
        OFFLINE,        // Offline
        SUSPENDED       // Suspenso
    }
}
```

**Migration:**

```sql
-- V21: Criar tabela courier_profiles
CREATE TABLE courier_profiles (
    id BIGSERIAL PRIMARY KEY,
    user_id UUID NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    vehicle_plate VARCHAR(10),
    vehicle_model VARCHAR(100),
    license_number VARCHAR(20),
    cnh_category VARCHAR(5),
    has_insurance BOOLEAN DEFAULT false,
    insurance_number VARCHAR(50),
    rating DECIMAL(3, 2),
    total_deliveries INTEGER DEFAULT 0,
    status VARCHAR(20) DEFAULT 'AVAILABLE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_courier_user CHECK ((SELECT role FROM users WHERE id = user_id) = 'COURIER')
);

CREATE INDEX idx_courier_status ON courier_profiles(status);
CREATE INDEX idx_courier_rating ON courier_profiles(rating DESC);
```

---

### 3. ADMProfile (Nova) - **CRIAR**

Dados específicos de gerentes locais:

```java
@Entity
@Table(name = "adm_profiles")
public class ADMProfile extends BaseEntity {

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;  // user.role = ADM

    @Column(name = "region", length = 100)
    private String region;  // Região de atuação

    @Column(name = "commission_percentage", precision = 5, scale = 2)
    private BigDecimal commissionPercentage = new BigDecimal("5.00");  // 5%

    @Column(name = "total_couriers_managed")
    private Integer totalCouriersManaged = 0;

    @Column(name = "total_deliveries_managed")
    private Integer totalDeliveriesManaged = 0;

    @ManyToOne
    @JoinColumn(name = "partnership_id")
    private MunicipalPartnership partnership;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ADMStatus status = ADMStatus.ACTIVE;

    public enum ADMStatus {
        ACTIVE,
        INACTIVE,
        SUSPENDED
    }
}
```

**Migration:**

```sql
-- V22: Criar tabela adm_profiles
CREATE TABLE adm_profiles (
    id BIGSERIAL PRIMARY KEY,
    user_id UUID NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    region VARCHAR(100),
    commission_percentage DECIMAL(5, 2) DEFAULT 5.00,
    total_couriers_managed INTEGER DEFAULT 0,
    total_deliveries_managed INTEGER DEFAULT 0,
    partnership_id BIGINT REFERENCES municipal_partnerships(id) ON DELETE SET NULL,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_adm_user CHECK ((SELECT role FROM users WHERE id = user_id) = 'ADM')
);

CREATE INDEX idx_adm_region ON adm_profiles(region);
CREATE INDEX idx_adm_partnership ON adm_profiles(partnership_id);
```

---

### 4. Delivery (Nova) - **CRIAR**

Substitui `Registration` (era inscrição em evento, agora é entrega):

```java
@Entity
@Table(name = "deliveries")
public class Delivery extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private User client;  // user.role = CLIENT

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "courier_id")
    private User courier;  // user.role = COURIER

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "adm_id")
    private User adm;  // user.role = ADM (gerente responsável)

    @Column(name = "from_address", nullable = false, columnDefinition = "TEXT")
    private String fromAddress;

    @Column(name = "from_lat")
    private BigDecimal fromLat;

    @Column(name = "from_lng")
    private BigDecimal fromLng;

    @Column(name = "to_address", nullable = false, columnDefinition = "TEXT")
    private String toAddress;

    @Column(name = "to_lat")
    private BigDecimal toLat;

    @Column(name = "to_lng")
    private BigDecimal toLng;

    @Column(name = "distance_km", precision = 10, scale = 2)
    private BigDecimal distanceKm;

    @Column(name = "total_value", precision = 10, scale = 2, nullable = false)
    private BigDecimal totalValue;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private DeliveryStatus status = DeliveryStatus.PENDING;

    @Column(name = "delivery_type", length = 50)
    private String deliveryType;  // "Express", "Normal", "Scheduled"

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "accepted_at")
    private LocalDateTime acceptedAt;

    @Column(name = "picked_up_at")
    private LocalDateTime pickedUpAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "cancellation_reason", columnDefinition = "TEXT")
    private String cancellationReason;

    public enum DeliveryStatus {
        PENDING,        // Aguardando aceite
        ACCEPTED,       // Aceito por motoboy
        PICKED_UP,      // Coletado
        IN_TRANSIT,     // Em trânsito
        COMPLETED,      // Entregue
        CANCELLED       // Cancelado
    }
}
```

**Migration:**

```sql
-- V23: Criar tabela deliveries
CREATE TABLE deliveries (
    id BIGSERIAL PRIMARY KEY,
    client_id UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    courier_id UUID REFERENCES users(id) ON DELETE SET NULL,
    adm_id UUID REFERENCES users(id) ON DELETE SET NULL,
    from_address TEXT NOT NULL,
    from_lat DECIMAL(10, 8),
    from_lng DECIMAL(11, 8),
    to_address TEXT NOT NULL,
    to_lat DECIMAL(10, 8),
    to_lng DECIMAL(11, 8),
    distance_km DECIMAL(10, 2),
    total_value DECIMAL(10, 2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    delivery_type VARCHAR(50),
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    accepted_at TIMESTAMP,
    picked_up_at TIMESTAMP,
    completed_at TIMESTAMP,
    cancelled_at TIMESTAMP,
    cancellation_reason TEXT,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_delivery_client CHECK ((SELECT role FROM users WHERE id = client_id) = 'CLIENT'),
    CONSTRAINT fk_delivery_courier CHECK ((SELECT role FROM users WHERE id = courier_id) = 'COURIER'),
    CONSTRAINT fk_delivery_adm CHECK ((SELECT role FROM users WHERE id = adm_id) = 'ADM')
);

CREATE INDEX idx_delivery_client ON deliveries(client_id);
CREATE INDEX idx_delivery_courier ON deliveries(courier_id);
CREATE INDEX idx_delivery_adm ON deliveries(adm_id);
CREATE INDEX idx_delivery_status ON deliveries(status);
CREATE INDEX idx_delivery_created ON deliveries(created_at);
```

---

### 5. Payment (Refatorar) - **ADAPTAR**

**Estado Atual:** Pagamento de inscrição em evento  
**Novo Propósito:** Pagamento de entrega com split (courier, ADM, plataforma)

```java
@Entity
@Table(name = "payments")
public class Payment extends BaseEntity {

    @OneToOne
    @JoinColumn(name = "delivery_id", nullable = false, unique = true)
    private Delivery delivery;  // ← ERA: registration_id

    @Column(name = "total_value", precision = 10, scale = 2, nullable = false)
    private BigDecimal totalValue;

    // Split de valores
    @Column(name = "courier_value", precision = 10, scale = 2)
    private BigDecimal courierValue;  // 85%

    @Column(name = "adm_value", precision = 10, scale = 2)
    private BigDecimal admValue;  // 5%

    @Column(name = "platform_value", precision = 10, scale = 2)
    private BigDecimal platformValue;  // 10%

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private PaymentStatus status = PaymentStatus.PENDING;

    @Column(name = "payment_method", length = 50)
    private String paymentMethod;  // PIX, CARD, etc.

    @Column(name = "transaction_id", length = 100)
    private String transactionId;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    public enum PaymentStatus {
        PENDING,
        PROCESSING,
        COMPLETED,
        FAILED,
        REFUNDED
    }
}
```

**Migration:**

```sql
-- V24: Refatorar payments para deliveries
ALTER TABLE payments RENAME COLUMN registration_id TO delivery_id;

ALTER TABLE payments ADD COLUMN IF NOT EXISTS courier_value DECIMAL(10, 2);
ALTER TABLE payments ADD COLUMN IF NOT EXISTS adm_value DECIMAL(10, 2);
ALTER TABLE payments ADD COLUMN IF NOT EXISTS platform_value DECIMAL(10, 2);

-- Constraint para garantir split correto
ALTER TABLE payments ADD CONSTRAINT check_payment_split
    CHECK (total_value = courier_value + adm_value + platform_value);
```

---

### 6. UnifiedPayout (Nova) - **CRIAR**

Repasses periódicos para couriers e ADMs:

```java
@Entity
@Table(name = "unified_payouts")
public class UnifiedPayout extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PayoutType type;  // COURIER ou ADM

    @ManyToOne
    @JoinColumn(name = "beneficiary_id", nullable = false)
    private User beneficiary;  // user.role = COURIER ou ADM

    @Column(nullable = false, length = 7)
    private String period;  // "2025-10" (YYYY-MM)

    @Column(name = "total_value", precision = 10, scale = 2, nullable = false)
    private BigDecimal totalValue;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private PayoutStatus status = PayoutStatus.PENDING;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "bank_account", length = 50)
    private String bankAccount;

    @Column(name = "pix_key", length = 100)
    private String pixKey;

    public enum PayoutType {
        COURIER,
        ADM
    }

    public enum PayoutStatus {
        PENDING,
        PROCESSING,
        COMPLETED,
        FAILED
    }
}
```

**Migration:**

```sql
-- V25: Criar tabela unified_payouts
CREATE TABLE unified_payouts (
    id BIGSERIAL PRIMARY KEY,
    type VARCHAR(20) NOT NULL,
    beneficiary_id UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    period VARCHAR(7) NOT NULL,
    total_value DECIMAL(10, 2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    processed_at TIMESTAMP,
    bank_account VARCHAR(50),
    pix_key VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    UNIQUE(beneficiary_id, period, type)
);

CREATE INDEX idx_payout_beneficiary ON unified_payouts(beneficiary_id);
CREATE INDEX idx_payout_period ON unified_payouts(period);
CREATE INDEX idx_payout_status ON unified_payouts(status);
```

---

### 7. PayoutItem (Nova) - **CRIAR**

Itens que compõem um repasse:

```java
@Entity
@Table(name = "payout_items")
public class PayoutItem extends BaseEntity {

    @ManyToOne
    @JoinColumn(name = "payout_id", nullable = false)
    private UnifiedPayout payout;

    @ManyToOne
    @JoinColumn(name = "payment_id", nullable = false)
    private Payment payment;

    @Column(name = "item_value", precision = 10, scale = 2, nullable = false)
    private BigDecimal itemValue;
}
```

**Migration:**

```sql
-- V26: Criar tabela payout_items
CREATE TABLE payout_items (
    id BIGSERIAL PRIMARY KEY,
    payout_id BIGINT NOT NULL REFERENCES unified_payouts(id) ON DELETE CASCADE,
    payment_id BIGINT NOT NULL REFERENCES payments(id) ON DELETE RESTRICT,
    item_value DECIMAL(10, 2) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    UNIQUE(payout_id, payment_id)
);

CREATE INDEX idx_payout_item_payout ON payout_items(payout_id);
CREATE INDEX idx_payout_item_payment ON payout_items(payment_id);
```

---

### 8. Evaluation (Nova) - **CRIAR**

Avaliações pós-entrega:

```java
@Entity
@Table(name = "evaluations")
public class Evaluation extends BaseEntity {

    @OneToOne
    @JoinColumn(name = "delivery_id", nullable = false, unique = true)
    private Delivery delivery;

    @ManyToOne
    @JoinColumn(name = "evaluator_id", nullable = false)
    private User evaluator;  // Cliente que avalia

    @ManyToOne
    @JoinColumn(name = "evaluated_id", nullable = false)
    private User evaluated;  // Courier avaliado

    @Column(nullable = false)
    private Integer rating;  // 1-5

    @Column(columnDefinition = "TEXT")
    private String comment;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
```

**Migration:**

```sql
-- V27: Criar tabela evaluations
CREATE TABLE evaluations (
    id BIGSERIAL PRIMARY KEY,
    delivery_id BIGINT NOT NULL UNIQUE REFERENCES deliveries(id) ON DELETE CASCADE,
    evaluator_id UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    evaluated_id UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    rating INTEGER NOT NULL CHECK (rating >= 1 AND rating <= 5),
    comment TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_evaluation_evaluator ON evaluations(evaluator_id);
CREATE INDEX idx_evaluation_evaluated ON evaluations(evaluated_id);
CREATE INDEX idx_evaluation_rating ON evaluations(rating);
```

---

### 9. MunicipalPartnership (Nova) - **CRIAR**

Parcerias institucionais com prefeituras:

```java
@Entity
@Table(name = "municipal_partnerships")
public class MunicipalPartnership extends BaseEntity {

    @Column(nullable = false, length = 200)
    private String name;  // Ex: "Prefeitura de Ubajara"

    @Column(length = 100)
    private String city;

    @Column(length = 100)
    private String state;

    @Column(unique = true, length = 18)
    private String cnpj;

    @Column(name = "contact_name", length = 200)
    private String contactName;

    @Column(name = "contact_email")
    private String contactEmail;

    @Column(name = "contact_phone", length = 20)
    private String contactPhone;

    @Column(name = "agreement_number", length = 50)
    private String agreementNumber;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private PartnershipStatus status = PartnershipStatus.ACTIVE;

    @Column(columnDefinition = "TEXT")
    private String notes;

    public enum PartnershipStatus {
        PENDING,
        ACTIVE,
        SUSPENDED,
        EXPIRED
    }
}
```

**Migration:**

```sql
-- V28: Criar tabela municipal_partnerships
CREATE TABLE municipal_partnerships (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    city VARCHAR(100),
    state VARCHAR(100),
    cnpj VARCHAR(18) UNIQUE,
    contact_name VARCHAR(200),
    contact_email VARCHAR(255),
    contact_phone VARCHAR(20),
    agreement_number VARCHAR(50),
    start_date DATE,
    end_date DATE,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_partnership_city ON municipal_partnerships(city);
CREATE INDEX idx_partnership_status ON municipal_partnerships(status);
```

---

### 10. ClientManagerLink (Nova) - **CRIAR**

Relacionamento N:M entre Client e ADM (opcional):

```java
@Entity
@Table(name = "client_manager_links")
public class ClientManagerLink {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "client_id", nullable = false)
    private User client;  // user.role = CLIENT

    @ManyToOne
    @JoinColumn(name = "adm_id", nullable = false)
    private User adm;  // user.role = ADM

    @Column(name = "linked_at", nullable = false)
    private LocalDateTime linkedAt = LocalDateTime.now();
}
```

**Migration:**

```sql
-- V29: Criar tabela client_manager_links
CREATE TABLE client_manager_links (
    id BIGSERIAL PRIMARY KEY,
    client_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    adm_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    linked_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    UNIQUE(client_id, adm_id),

    CONSTRAINT fk_link_client CHECK ((SELECT role FROM users WHERE id = client_id) = 'CLIENT'),
    CONSTRAINT fk_link_adm CHECK ((SELECT role FROM users WHERE id = adm_id) = 'ADM')
);

CREATE INDEX idx_link_client ON client_manager_links(client_id);
CREATE INDEX idx_link_adm ON client_manager_links(adm_id);
```

---

### 11. CourierADMLink (Nova) - **CRIAR** ⭐

Relacionamento N:M entre Courier e ADM:

> **Regra de Negócio:** Um motoboy pode estar associado a múltiplos ADMs (gerentes)  
> Isso permite que um courier trabalhe para diferentes grupos/regiões

```java
@Entity
@Table(name = "courier_adm_links")
public class CourierADMLink {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "courier_id", nullable = false)
    private User courier;  // user.role = COURIER

    @ManyToOne
    @JoinColumn(name = "adm_id", nullable = false)
    private User adm;  // user.role = ADM

    @Column(name = "linked_at", nullable = false)
    private LocalDateTime linkedAt = LocalDateTime.now();

    @Column(name = "is_primary")
    private Boolean isPrimary = false;  // ADM principal do courier

    @Column(name = "is_active")
    private Boolean isActive = true;
}
```

**Migration:**

```sql
-- V30: Criar tabela courier_adm_links (N:M)
CREATE TABLE courier_adm_links (
    id BIGSERIAL PRIMARY KEY,
    courier_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    adm_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    linked_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_primary BOOLEAN DEFAULT false,
    is_active BOOLEAN DEFAULT true,

    UNIQUE(courier_id, adm_id),

    CONSTRAINT fk_courier_link CHECK ((SELECT role FROM users WHERE id = courier_id) = 'COURIER'),
    CONSTRAINT fk_adm_link CHECK ((SELECT role FROM users WHERE id = adm_id) = 'ADM')
);

CREATE INDEX idx_courier_link_courier ON courier_adm_links(courier_id);
CREATE INDEX idx_courier_link_adm ON courier_adm_links(adm_id);
CREATE INDEX idx_courier_link_primary ON courier_adm_links(is_primary) WHERE is_primary = true;
CREATE INDEX idx_courier_link_active ON courier_adm_links(is_active);

COMMENT ON COLUMN courier_adm_links.is_primary IS 'ADM principal responsável pelo courier';
COMMENT ON COLUMN courier_adm_links.is_active IS 'Link ativo (courier pode desativar associação)';
```

---

## 🗑️ Entidades para REMOVER

```sql
-- V31: Remover entidades de eventos
DROP TABLE IF EXISTS registrations CASCADE;
DROP TABLE IF EXISTS event_categories CASCADE;
DROP TABLE IF EXISTS events CASCADE;
DROP TABLE IF EXISTS organizations CASCADE;
DROP TABLE IF EXISTS cities CASCADE;
```

---

## 📊 Plano de Execução

### Fase 1: Preparação (Sem quebrar nada) ✅

- [x] Analisar estrutura atual
- [x] Mapear entidades
- [x] Criar plano de migração
- [ ] Criar backup completo do banco

### Fase 2: Expansão da Tabela Users 🔄

- [ ] V20: Expandir enum `Role` (CLIENT, COURIER, ADM)
- [ ] V20: Adicionar campos `active`, `tax_id`
- [ ] Atualizar entidade `User.java`
- [ ] Testar autenticação com novos roles

### Fase 3: Criar Tabelas Especializadas 🆕

- [ ] V21: Criar `courier_profiles`
- [ ] V22: Criar `adm_profiles`
- [ ] Criar entidades Java correspondentes
- [ ] Testar relacionamentos 1:1 com User

### Fase 4: Criar Entidades de Entrega 🚚

- [ ] V23: Criar `deliveries`
- [ ] V24: Refatorar `payments` (adicionar split)
- [ ] V25: Criar `unified_payouts`
- [ ] V26: Criar `payout_items`
- [ ] V27: Criar `evaluations`
- [ ] Criar controllers, services, repositories

### Fase 5: Criar Entidades Institucionais 🏛️

- [ ] V28: Criar `municipal_partnerships`
- [ ] V29: Criar `client_manager_links`
- [ ] Implementar lógica de relacionamento N:M

### Fase 6: Remover Entidades Antigas 🗑️

- [ ] V30: Drop tables de eventos
- [ ] Remover classes Java de eventos
- [ ] Limpar controllers/services de eventos

### Fase 7: Adaptar Features Existentes ♻️

- [ ] Atualizar sistema de metadata para novas entidades
- [ ] Configurar filtros dinâmicos para Delivery
- [ ] Implementar cascade updates para Delivery → Payment
- [ ] Adaptar annotations (@DisplayLabel, @Visible, @Computed)

### Fase 8: Criar Lógica de Negócio Zapi10 💼

- [ ] Service: criar entrega
- [ ] Service: aceitar entrega (courier)
- [ ] Service: completar entrega
- [ ] Service: processar pagamento com split
- [ ] Service: gerar repasses periódicos
- [ ] Service: calcular rating de courier
- [ ] Service: dashboard ADM

### Fase 9: Testes e Validação ✅

- [ ] Testes unitários
- [ ] Testes de integração
- [ ] Testes E2E
- [ ] Testar com dados reais (piloto Ubajara)

### Fase 10: Deploy 🚀

- [ ] Deploy em ambiente de staging
- [ ] Testes de carga
- [ ] Deploy em produção
- [ ] Monitoramento

---

## 📅 Cronograma Estimado

| Fase                        | Tempo Estimado | Prioridade |
| --------------------------- | -------------- | ---------- |
| 1. Preparação               | 1 dia          | 🔴 Alta    |
| 2. Expansão Users           | 2 dias         | 🔴 Alta    |
| 3. Tabelas Especializadas   | 3 dias         | 🔴 Alta    |
| 4. Entidades de Entrega     | 1 semana       | 🔴 Alta    |
| 5. Entidades Institucionais | 3 dias         | 🟡 Média   |
| 6. Remover Antigas          | 1 dia          | 🟢 Baixa   |
| 7. Adaptar Features         | 1 semana       | 🔴 Alta    |
| 8. Lógica de Negócio        | 2 semanas      | 🔴 Alta    |
| 9. Testes                   | 1 semana       | 🔴 Alta    |
| 10. Deploy                  | 3 dias         | 🔴 Alta    |

**Total Estimado: 6-7 semanas**

---

## 🎯 Próximos Passos Imediatos

1. ✅ **Fazer backup completo**

   ```bash
   pg_dump -h localhost -U postgres mvt_events > backup_before_zapi10.sql
   ```

2. ✅ **Criar branch de migração**

   ```bash
   git checkout -b feature/zapi10-migration
   ```

3. ✅ **Começar Fase 2** - Expandir tabela users

4. ✅ **Documentar cada mudança**

---

## 📚 Referências

- [Zapi10 MER (English)](../obsidian/Zapi10_MER_EN.md)
- [Zapi10 Documento Base](../obsidian/Zapi10_Documento_Base_Completo.md)
- [Zapi10 Fluxo de Negócio](../obsidian/Zapi10_Fluxo_de_Negocio_V1.md)
- [Annotations Guide](./implementation/ANNOTATIONS_GUIDE.md)
- [Cascade Update Helper](./implementation/CASCADE_HELPER_README.md)

---

**📅 Criado em:** 18 de outubro de 2025  
**✍️ Autor:** MVT Events → Zapi10 Migration Team  
**📌 Status:** 📝 Em Planejamento
