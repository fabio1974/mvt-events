# Relacionamentos N:M: COURIER ↔ Organization e CLIENT ↔ Organization

**Data:** 22 de outubro de 2025  
**Status:** 🔄 Em Implementação  
**Versão:** 2.0

---

## 📋 Modelo Completo de Relacionamentos

### Relacionamentos com Organization

```
┌─────────────────┐
│  Gerente ADM    │
│  (ORGANIZER)    │
└────────┬────────┘
         │ N:1 (organization_id)
         │
         ▼
┌─────────────────────────────────────────┐
│           ORGANIZATION                   │
│  - id                                    │
│  - name                                  │
│  - ...                                   │
└─────────────────────────────────────────┘
         ▲                    ▲
         │                    │
         │ N:M                │ N:M
         │                    │
┌────────┴────────┐   ┌───────┴──────────┐
│   COURIER       │   │     CLIENT       │
│   (Motoboy)     │   │    (Cliente)     │
└─────────────────┘   └──────────────────┘
```

---

## 🗄️ Estrutura das Tabelas

### 1️⃣ Tabela: `courier_organizations`

**Objetivo:** Vincular motoboys às organizações (grupos)  
**Regra:** Um motoboy pode trabalhar para múltiplas organizações

```sql
CREATE TABLE courier_organizations (
    id BIGSERIAL PRIMARY KEY,

    -- Foreign Keys
    courier_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    organization_id BIGINT NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,

    -- Metadados
    linked_at TIMESTAMP NOT NULL DEFAULT NOW(),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,

    -- Audit
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),

    -- Constraints
    UNIQUE(courier_id, organization_id)
);

CREATE INDEX idx_courier_orgs_courier ON courier_organizations(courier_id);
CREATE INDEX idx_courier_orgs_organization ON courier_organizations(organization_id);
CREATE INDEX idx_courier_orgs_active ON courier_organizations(is_active);
```

### 2️⃣ Tabela: `contracts` (CLIENT ↔ Organization)

**Objetivo:** Vincular clientes às organizações através de contratos  
**Regra:** Um cliente pode ter múltiplos contratos, mas **apenas 1 pode ser titular** (is_primary = true)

```sql
CREATE TABLE contracts (
    id BIGSERIAL PRIMARY KEY,

    -- Foreign Keys
    client_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    organization_id BIGINT NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,

    -- Contract Metadata
    contract_number VARCHAR(50) UNIQUE,
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE', -- ACTIVE, SUSPENDED, CANCELLED

    -- Dates
    contract_date DATE NOT NULL DEFAULT CURRENT_DATE,
    start_date DATE NOT NULL,
    end_date DATE,

    -- Audit
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),

    -- Constraints
    UNIQUE(client_id, organization_id),

    -- Garantir que apenas 1 contrato por cliente seja titular
    CHECK (
        CASE WHEN is_primary = TRUE
        THEN (SELECT COUNT(*) FROM contracts c2
              WHERE c2.client_id = client_id
              AND c2.is_primary = TRUE
              AND c2.id != id) = 0
        ELSE TRUE END
    )
);

CREATE INDEX idx_contracts_client ON contracts(client_id);
CREATE INDEX idx_contracts_organization ON contracts(organization_id);
CREATE INDEX idx_contracts_primary ON contracts(is_primary);
CREATE INDEX idx_contracts_status ON contracts(status);
CREATE INDEX idx_contracts_number ON contracts(contract_number);

-- Trigger para garantir apenas 1 contrato titular por cliente
CREATE OR REPLACE FUNCTION check_primary_contract()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.is_primary = TRUE THEN
        -- Desativar outros contratos titulares do mesmo cliente
        UPDATE contracts
        SET is_primary = FALSE
        WHERE client_id = NEW.client_id
        AND id != NEW.id
        AND is_primary = TRUE;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER enforce_single_primary_contract
BEFORE INSERT OR UPDATE ON contracts
FOR EACH ROW
EXECUTE FUNCTION check_primary_contract();
```

---

## 💻 Implementação em Java

### 1. Entidade: `CourierOrganization`

```java
package com.mvt.mvt_events.jpa;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "courier_organizations",
       uniqueConstraints = @UniqueConstraint(columnNames = {"courier_id", "organization_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class CourierOrganization extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "courier_id", nullable = false)
    @JsonIgnore
    private User courier;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    @JsonIgnore
    private Organization organization;

    @Column(name = "linked_at", nullable = false)
    private LocalDateTime linkedAt = LocalDateTime.now();

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;
}
```

### 2. Entidade: `Contract` (CLIENT ↔ Organization)

```java
package com.mvt.mvt_events.jpa;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "contracts",
       uniqueConstraints = @UniqueConstraint(columnNames = {"client_id", "organization_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Contract extends BaseEntity {

    // Foreign Keys
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    @JsonIgnore
    private User client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    @JsonIgnore
    private Organization organization;

    // Contract Metadata
    @Column(name = "contract_number", length = 50, unique = true)
    private String contractNumber;

    @Column(name = "is_primary", nullable = false)
    private boolean isPrimary = false;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private ContractStatus status = ContractStatus.ACTIVE;

    // Dates
    @NotNull(message = "Data do contrato é obrigatória")
    @Column(name = "contract_date", nullable = false)
    private LocalDate contractDate = LocalDate.now();

    @NotNull(message = "Data de início é obrigatória")
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    public enum ContractStatus {
        ACTIVE,      // Ativo
        SUSPENDED,   // Suspenso
        CANCELLED    // Cancelado
    }

    /**
     * Helper method para verificar se o contrato está ativo
     */
    public boolean isActive() {
        return status == ContractStatus.ACTIVE;
    }

    /**
     * Helper method para verificar se o contrato está vigente
     */
    public boolean isValid() {
        LocalDate today = LocalDate.now();
        return isActive()
            && !startDate.isAfter(today)
            && (endDate == null || !endDate.isBefore(today));
    }
}
```

### 3. Atualizar: `User.java`

```java
// Adicionar relacionamentos N:M

// Para COURIER - múltiplas organizations
@OneToMany(mappedBy = "courier", cascade = CascadeType.ALL, orphanRemoval = true)
@JsonIgnore
private Set<CourierOrganization> courierOrganizations = new HashSet<>();

// Para CLIENT - múltiplos contratos
@OneToMany(mappedBy = "client", cascade = CascadeType.ALL, orphanRemoval = true)
@JsonIgnore
private Set<Contract> contracts = new HashSet<>();

// Helper methods
public Set<Organization> getCourierOrganizationsList() {
    return courierOrganizations.stream()
        .map(CourierOrganization::getOrganization)
        .collect(Collectors.toSet());
}

public Set<Organization> getClientOrganizationsList() {
    return contracts.stream()
        .filter(Contract::isActive)
        .map(Contract::getOrganization)
        .collect(Collectors.toSet());
}

public Contract getPrimaryContract() {
    return contracts.stream()
        .filter(Contract::isPrimary)
        .findFirst()
        .orElse(null);
}
```

### 4. Atualizar: `Organization.java`

```java
// Adicionar relacionamentos reversos

// Couriers vinculados a esta organização
@OneToMany(mappedBy = "organization", cascade = CascadeType.ALL, orphanRemoval = true)
@JsonIgnore
private Set<CourierOrganization> organizationCouriers = new HashSet<>();

// Clientes com contratos nesta organização
@OneToMany(mappedBy = "organization", cascade = CascadeType.ALL, orphanRemoval = true)
@JsonIgnore
private Set<Contract> organizationContracts = new HashSet<>();

// Helper methods
public Set<User> getCouriers() {
    return organizationCouriers.stream()
        .filter(CourierOrganization::isActive)
        .map(CourierOrganization::getCourier)
        .collect(Collectors.toSet());
}

public Set<User> getClients() {
    return organizationContracts.stream()
        .filter(Contract::isActive)
        .map(Contract::getClient)
        .collect(Collectors.toSet());
}

public long getActiveContractsCount() {
    return organizationContracts.stream()
        .filter(Contract::isActive)
        .count();
}
```

---

## 📊 Casos de Uso

### Caso 1: Cliente com Múltiplos Contratos

```java
// Cliente João tem 3 contratos
Contract contract1 = new Contract();
contract1.setClient(joao);
contract1.setOrganization(org1);
contract1.setPrimary(true);  // ✅ Contrato titular
contract1.setContractNumber("CNT-001");

Contract contract2 = new Contract();
contract2.setClient(joao);
contract2.setOrganization(org2);
contract2.setPrimary(false); // Contrato secundário

Contract contract3 = new Contract();
contract3.setClient(joao);
contract3.setOrganization(org3);
contract3.setPrimary(false); // Contrato secundário

// Trigger SQL garante que apenas 1 seja titular
```

### Caso 2: Motoboy em Múltiplas Organizações

```java
// Motoboy Maria trabalha em 3 organizações
CourierOrganization link1 = new CourierOrganization();
link1.setCourier(maria);
link1.setOrganization(org1);
link1.setActive(true);

CourierOrganization link2 = new CourierOrganization();
link2.setCourier(maria);
link2.setOrganization(org2);
link2.setActive(true);

CourierOrganization link3 = new CourierOrganization();
link3.setCourier(maria);
link3.setOrganization(org3);
link3.setActive(false); // Inativo
```

### Caso 3: Listagem de Clientes de uma Organização

```java
// Buscar todos os clientes ativos de uma organização
Set<User> clients = organization.getClients();

// Buscar apenas contratos titulares
List<User> primaryClients = organization.getOrganizationContracts().stream()
    .filter(Contract::isPrimary)
    .filter(Contract::isActive)
    .map(Contract::getClient)
    .collect(Collectors.toList());
```

---

## ✅ Checklist de Implementação

### Fase 1: Entidades

- [ ] Criar `CourierOrganization.java`
- [ ] Criar `Contract.java`
- [ ] Atualizar `User.java` (adicionar relacionamentos)
- [ ] Atualizar `Organization.java` (adicionar relacionamentos)

### Fase 2: Repositories

- [ ] Criar `CourierOrganizationRepository.java`
- [ ] Criar `ContractRepository.java`

### Fase 3: Services

- [ ] Criar `CourierOrganizationService.java`
- [ ] Criar `ContractService.java`

### Fase 4: Controllers

- [ ] Criar `CourierOrganizationController.java`
- [ ] Criar `ContractController.java`

### Fase 5: Migrations

- [ ] Criar migration para `courier_organizations`
- [ ] Criar migration para `contracts`
- [ ] Migrar dados de `courier_adm_links` (se existir)
- [ ] Criar triggers para `is_primary`

### Fase 6: Testes

- [ ] Testar criação de contratos
- [ ] Testar validação de contrato titular único
- [ ] Testar vinculação de motoboys
- [ ] Testar listagens

---

## 🎯 Regras de Negócio

### Contracts (CLIENT ↔ Organization)

1. ✅ Um cliente pode ter **múltiplos contratos**
2. ✅ **Apenas 1 contrato** pode ser titular (`is_primary = true`)
3. ✅ Quando um contrato se torna titular, os demais são automaticamente desmarcados
4. ✅ Contratos podem ter status: ACTIVE, SUSPENDED, CANCELLED
5. ✅ Contratos têm data de início e fim (opcional)
6. ✅ Número de contrato é único no sistema

### CourierOrganizations (COURIER ↔ Organization)

1. ✅ Um motoboy pode trabalhar para **múltiplas organizações**
2. ✅ Vínculos podem ser ativados/desativados (`is_active`)
3. ✅ Histórico de vinculação é mantido (`linked_at`)

---

**Quer que eu comece a implementação agora?** 🚀
