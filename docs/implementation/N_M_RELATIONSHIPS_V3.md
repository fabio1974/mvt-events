# Relacionamentos N:M: Employment Contracts e Service Contracts

**Data:** 22 de outubro de 2025  
**Status:** ✅ Implementado  
**Versão:** 3.0 - Refatorado com nomenclatura clara

---

## 📋 Visão Geral

Este documento descreve o design dos relacionamentos N:M entre usuários (COURIER e CLIENT) e Organizations no sistema Zapi10.

### **Conceito Principal**

Temos **dois tipos de contratos** diferentes:

1. **Employment Contracts** (Contratos de Trabalho)

   - Relação: **COURIER ↔ Organization**
   - Tipo: Empregado-Empresa
   - Tabela: `employment_contracts`
   - Um motoboy pode trabalhar para múltiplas organizações

2. **Service Contracts** (Contratos de Serviço)
   - Relação: **CLIENT ↔ Organization**
   - Tipo: Cliente-Fornecedor
   - Tabela: `contracts`
   - Um cliente pode ter múltiplos contratos, mas apenas 1 pode ser titular

---

## 🎨 Diagrama de Relacionamentos

```
┌─────────────────┐
│  Gerente ADM    │
│  (ORGANIZER)    │
└────────┬────────┘
         │ N:1 (organization_id)
         │ "É dono da"
         ▼
┌──────────────────────────────────────────────┐
│            ORGANIZATION                      │
│  - id                                        │
│  - name                                      │
│  - commission_percentage                     │
└──────────────────────────────────────────────┘
         ▲                          ▲
         │                          │
         │ N:M                      │ N:M
         │ (employment_contracts)   │ (contracts)
         │                          │
┌────────┴──────────┐      ┌────────┴─────────┐
│    COURIER        │      │     CLIENT       │
│   (Funcionário)   │      │    (Cliente)     │
│                   │      │                  │
│ • Trabalha para   │      │ • Contrata de    │
│ • is_active       │      │ • is_primary     │
└───────────────────┘      └──────────────────┘
```

**Legendas:**

- **COURIER → Organization**: Relação de **trabalho** (empregado-empresa)
- **CLIENT → Organization**: Relação de **contratação** (cliente-fornecedor)

---

## 🗄️ Estrutura das Tabelas

### 1️⃣ Tabela: `employment_contracts`

**Objetivo:** Representar contratos de trabalho entre COURIER e Organization  
**Tipo:** Relação Empregado-Empresa  
**Regra:** Um motoboy pode ter múltiplos contratos de trabalho (trabalhar para várias organizações)

```sql
CREATE TABLE employment_contracts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Relacionamentos
    courier_id UUID NOT NULL,
    organization_id UUID NOT NULL,

    -- Metadados do contrato de trabalho
    linked_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,

    -- Foreign Keys
    CONSTRAINT fk_employment_courier FOREIGN KEY (courier_id)
        REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_employment_organization FOREIGN KEY (organization_id)
        REFERENCES organizations(id) ON DELETE CASCADE,

    -- Constraints
    CONSTRAINT uq_employment_courier_org UNIQUE (courier_id, organization_id)
);

-- Índices para performance
CREATE INDEX idx_employment_courier ON employment_contracts(courier_id);
CREATE INDEX idx_employment_organization ON employment_contracts(organization_id);
CREATE INDEX idx_employment_active ON employment_contracts(is_active);
```

**Campos:**

- `courier_id`: Referência ao motoboy
- `organization_id`: Referência à organização empregadora
- `linked_at`: Data/hora que o courier foi contratado
- `is_active`: Se o contrato de trabalho está ativo

---

### 2️⃣ Tabela: `contracts`

**Objetivo:** Representar contratos de serviço entre CLIENT e Organization  
**Tipo:** Relação Cliente-Fornecedor  
**Regra:** Um cliente pode ter múltiplos contratos, mas **apenas 1 pode ser titular** (is_primary = true)

```sql
CREATE TABLE contracts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Relacionamentos
    client_id UUID NOT NULL,
    organization_id UUID NOT NULL,

    -- Metadados do contrato de serviço
    contract_number VARCHAR(50) UNIQUE,
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',

    -- Datas
    contract_date DATE NOT NULL DEFAULT CURRENT_DATE,
    start_date DATE NOT NULL,
    end_date DATE,

    -- Foreign Keys
    CONSTRAINT fk_contract_client FOREIGN KEY (client_id)
        REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_contract_organization FOREIGN KEY (organization_id)
        REFERENCES organizations(id) ON DELETE CASCADE,

    -- Constraints
    CONSTRAINT uq_contract_client_org UNIQUE (client_id, organization_id),
    CONSTRAINT chk_contract_status CHECK (status IN ('ACTIVE', 'SUSPENDED', 'CANCELLED')),
    CONSTRAINT chk_contract_dates CHECK (end_date IS NULL OR end_date >= start_date)
);

-- Índices para performance
CREATE INDEX idx_contract_client ON contracts(client_id);
CREATE INDEX idx_contract_organization ON contracts(organization_id);
CREATE INDEX idx_contract_status ON contracts(status);
CREATE INDEX idx_contract_primary ON contracts(is_primary);

-- Trigger para garantir apenas 1 contrato titular por cliente
CREATE OR REPLACE FUNCTION check_primary_contract()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.is_primary = TRUE THEN
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

**Campos:**

- `client_id`: Referência ao cliente
- `organization_id`: Referência à organização fornecedora
- `contract_number`: Número único do contrato
- `is_primary`: Se este é o contrato titular (apenas 1 por cliente)
- `status`: ACTIVE, SUSPENDED, CANCELLED
- `contract_date`: Data de assinatura do contrato
- `start_date`: Data de início da vigência
- `end_date`: Data de fim da vigência (NULL = indeterminado)

---

## 💻 Implementação em Java

### 1. Entidade: `EmploymentContract.java`

```java
package com.mvt.mvt_events.jpa;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.mvt.mvt_events.metadata.Visible;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Contrato de trabalho entre COURIER e Organization.
 * Representa a relação empregado-empresa.
 */
@Entity
@Table(name = "employment_contracts",
       uniqueConstraints = @UniqueConstraint(columnNames = {"courier_id", "organization_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class EmploymentContract extends BaseEntity {

    @NotNull(message = "Motoboy é obrigatório")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "courier_id", nullable = false)
    @JsonIgnore
    @Visible(table = true, form = true, filter = true)
    private User courier;

    @NotNull(message = "Organização é obrigatória")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    @JsonIgnore
    @Visible(table = true, form = true, filter = true)
    private Organization organization;

    @Column(name = "linked_at", nullable = false)
    @Visible(table = true, form = false, filter = false)
    private LocalDateTime linkedAt = LocalDateTime.now();

    @Column(name = "is_active", nullable = false)
    @Visible(table = true, form = true, filter = true)
    private boolean isActive = true;

    public void activate() {
        this.isActive = true;
    }

    public void deactivate() {
        this.isActive = false;
    }
}
```

---

### 2. Entidade: `Contract.java`

```java
package com.mvt.mvt_events.jpa;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.mvt.mvt_events.metadata.DisplayLabel;
import com.mvt.mvt_events.metadata.Visible;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;

/**
 * Contrato de serviço entre CLIENT e Organization.
 * Representa a relação cliente-fornecedor.
 */
@Entity
@Table(name = "contracts",
       uniqueConstraints = @UniqueConstraint(columnNames = {"client_id", "organization_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Contract extends BaseEntity {

    @NotNull(message = "Cliente é obrigatório")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    @JsonIgnore
    @Visible(table = true, form = true, filter = true)
    private User client;

    @NotNull(message = "Organização é obrigatória")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    @JsonIgnore
    @Visible(table = true, form = true, filter = true)
    private Organization organization;

    @DisplayLabel
    @Column(name = "contract_number", length = 50, unique = true)
    @Visible(table = true, form = true, filter = true)
    private String contractNumber;

    @Column(name = "is_primary", nullable = false)
    @Visible(table = true, form = true, filter = true)
    private boolean isPrimary = false;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    @Visible(table = true, form = true, filter = true)
    private ContractStatus status = ContractStatus.ACTIVE;

    @NotNull(message = "Data do contrato é obrigatória")
    @Column(name = "contract_date", nullable = false)
    @Visible(table = true, form = true, filter = false)
    private LocalDate contractDate = LocalDate.now();

    @NotNull(message = "Data de início é obrigatória")
    @Column(name = "start_date", nullable = false)
    @Visible(table = true, form = true, filter = true)
    private LocalDate startDate;

    @Column(name = "end_date")
    @Visible(table = true, form = true, filter = true)
    private LocalDate endDate;

    public enum ContractStatus {
        ACTIVE,      // Ativo
        SUSPENDED,   // Suspenso
        CANCELLED    // Cancelado
    }

    public boolean isActive() {
        return status == ContractStatus.ACTIVE;
    }

    public boolean isValid() {
        LocalDate today = LocalDate.now();
        return isActive()
            && !startDate.isAfter(today)
            && (endDate == null || !endDate.isBefore(today));
    }

    public boolean isExpired() {
        return endDate != null && endDate.isBefore(LocalDate.now());
    }
}
```

---

### 3. Atualizar: `User.java`

```java
// ============================================================================
// N:M RELATIONSHIPS
// ============================================================================

// Para COURIER - contratos de trabalho (empregado-empresa)
@OneToMany(mappedBy = "courier", cascade = CascadeType.ALL, orphanRemoval = true)
@JsonIgnore
private Set<EmploymentContract> employmentContracts = new HashSet<>();

// Para CLIENT - contratos de serviço (cliente-fornecedor)
@OneToMany(mappedBy = "client", cascade = CascadeType.ALL, orphanRemoval = true)
@JsonIgnore
private Set<Contract> contracts = new HashSet<>();

// ============================================================================
// HELPER METHODS
// ============================================================================

/**
 * Retorna lista de organizações onde o usuário trabalha como COURIER
 */
public Set<Organization> getEmployerOrganizations() {
    return employmentContracts.stream()
        .filter(EmploymentContract::isActive)
        .map(EmploymentContract::getOrganization)
        .collect(Collectors.toSet());
}

/**
 * Retorna lista de organizações onde o usuário é CLIENT
 */
public Set<Organization> getClientOrganizationsList() {
    return contracts.stream()
        .filter(Contract::isActive)
        .map(Contract::getOrganization)
        .collect(Collectors.toSet());
}

/**
 * Retorna o contrato de serviço titular do cliente
 */
public Contract getPrimaryContract() {
    return contracts.stream()
        .filter(Contract::isPrimary)
        .findFirst()
        .orElse(null);
}

/**
 * Retorna a organização do contrato titular (se CLIENT)
 */
public Organization getPrimaryOrganization() {
    Contract primary = getPrimaryContract();
    return primary != null ? primary.getOrganization() : null;
}

/**
 * Verifica se o usuário tem contratos de trabalho ativos como COURIER
 */
public boolean hasActiveEmployment() {
    return !employmentContracts.isEmpty() &&
           employmentContracts.stream().anyMatch(EmploymentContract::isActive);
}

/**
 * Verifica se o usuário tem contratos de serviço ativos como CLIENT
 */
public boolean hasActiveContracts() {
    return !contracts.isEmpty() &&
           contracts.stream().anyMatch(Contract::isActive);
}
```

---

### 4. Atualizar: `Organization.java`

```java
// ============================================================================
// N:M RELATIONSHIPS
// ============================================================================

// Contratos de trabalho (empregado-empresa) - Couriers que trabalham para esta organização
@OneToMany(mappedBy = "organization", cascade = CascadeType.ALL, orphanRemoval = true)
@JsonIgnore
private Set<EmploymentContract> employmentContracts = new HashSet<>();

// Contratos de serviço (cliente-fornecedor) - Clientes que contratam serviços desta organização
@OneToMany(mappedBy = "organization", cascade = CascadeType.ALL, orphanRemoval = true)
@JsonIgnore
private Set<Contract> serviceContracts = new HashSet<>();

// ============================================================================
// HELPER METHODS
// ============================================================================

/**
 * Retorna lista de couriers (funcionários) ativos desta organização
 */
public Set<User> getEmployees() {
    return employmentContracts.stream()
        .filter(EmploymentContract::isActive)
        .map(EmploymentContract::getCourier)
        .collect(Collectors.toSet());
}

/**
 * Retorna lista de clientes com contratos de serviço ativos
 */
public Set<User> getClients() {
    return serviceContracts.stream()
        .filter(Contract::isActive)
        .map(Contract::getClient)
        .collect(Collectors.toSet());
}

/**
 * Retorna contagem de contratos de serviço ativos
 */
public long getActiveServiceContractsCount() {
    return serviceContracts.stream()
        .filter(Contract::isActive)
        .count();
}

/**
 * Retorna contagem de funcionários (couriers) ativos
 */
public long getActiveEmployeesCount() {
    return employmentContracts.stream()
        .filter(EmploymentContract::isActive)
        .count();
}
```

---

## 📊 Casos de Uso

### Caso 1: Cliente com Múltiplos Contratos

```java
// Cliente João tem 3 contratos de serviço
Contract contract1 = new Contract();
contract1.setClient(joao);
contract1.setOrganization(org1);
contract1.setPrimary(true);  // ✅ Contrato titular
contract1.setContractNumber("CNT-2025-001");
contract1.setStatus(ContractStatus.ACTIVE);

Contract contract2 = new Contract();
contract2.setClient(joao);
contract2.setOrganization(org2);
contract2.setPrimary(false); // Contrato secundário
contract2.setContractNumber("CNT-2025-002");

Contract contract3 = new Contract();
contract3.setClient(joao);
contract3.setOrganization(org3);
contract3.setPrimary(false); // Contrato secundário
contract3.setContractNumber("CNT-2025-003");

// Trigger SQL garante que apenas 1 seja titular
// Se marcarmos contract2.setPrimary(true), contract1 será desmarcado automaticamente
```

---

### Caso 2: Motoboy em Múltiplas Organizações

```java
// Motoboy Maria trabalha em 3 organizações
EmploymentContract job1 = new EmploymentContract();
job1.setCourier(maria);
job1.setOrganization(org1);
job1.setActive(true);
job1.setLinkedAt(LocalDateTime.now());

EmploymentContract job2 = new EmploymentContract();
job2.setCourier(maria);
job2.setOrganization(org2);
job2.setActive(true);

EmploymentContract job3 = new EmploymentContract();
job3.setCourier(maria);
job3.setOrganization(org3);
job3.setActive(false); // Contrato de trabalho inativo (demitido ou suspenso)
```

---

### Caso 3: Listagem de Clientes de uma Organização

```java
// Buscar todos os clientes ativos de uma organização
Set<User> clients = organization.getClients();

// Buscar apenas contratos titulares
List<User> primaryClients = organization.getServiceContracts().stream()
    .filter(Contract::isPrimary)
    .filter(Contract::isActive)
    .map(Contract::getClient)
    .collect(Collectors.toList());

// Buscar todos os funcionários (couriers) ativos
Set<User> employees = organization.getEmployees();
```

---

## 🎯 Regras de Negócio

### Service Contracts (CLIENT ↔ Organization)

| Regra                     | Descrição                                                                                    |
| ------------------------- | -------------------------------------------------------------------------------------------- |
| ✅ Múltiplos contratos    | Um cliente pode ter **múltiplos contratos** com diferentes organizações                      |
| ✅ Contrato titular único | **Apenas 1 contrato** pode ser titular (`is_primary = true`) por cliente                     |
| ✅ Desmarcação automática | Quando um contrato se torna titular, os demais são automaticamente desmarcados (via trigger) |
| ✅ Status do contrato     | Contratos podem ter status: ACTIVE, SUSPENDED, CANCELLED                                     |
| ✅ Vigência               | Contratos têm data de início e fim (opcional)                                                |
| ✅ Número único           | Número de contrato é único no sistema                                                        |

### Employment Contracts (COURIER ↔ Organization)

| Regra                   | Descrição                                                          |
| ----------------------- | ------------------------------------------------------------------ |
| ✅ Múltiplos empregos   | Um motoboy pode trabalhar para **múltiplas organizações**          |
| ✅ Ativação/Desativação | Contratos de trabalho podem ser ativados/desativados (`is_active`) |
| ✅ Histórico            | Histórico de contratação é mantido (`linked_at`)                   |
| ✅ Unicidade            | Um motoboy não pode ter 2 contratos com a mesma organização        |

---

## ✅ Status da Implementação

### ✅ Fase 1: Entidades

- [x] Criar `EmploymentContract.java`
- [x] Criar `Contract.java`
- [x] Atualizar `User.java` (adicionar relacionamentos)
- [x] Atualizar `Organization.java` (adicionar relacionamentos)

### ⏳ Fase 2: Repositories

- [ ] Criar `EmploymentContractRepository.java`
- [ ] Criar `ContractRepository.java`

### ⏳ Fase 3: Services

- [ ] Criar `EmploymentContractService.java`
- [ ] Criar `ContractService.java`

### ⏳ Fase 4: Controllers

- [ ] Criar `EmploymentContractController.java`
- [ ] Criar `ContractController.java`

### ✅ Fase 5: Migrations

- [x] Criar V40: Tabelas `employment_contracts` e `contracts`
- [x] Criar V41: Migrar dados antigos e remover tabelas legacy
- [x] Trigger para `is_primary`

### ⏳ Fase 6: Testes

- [ ] Testar criação de contratos de serviço
- [ ] Testar validação de contrato titular único
- [ ] Testar vinculação de motoboys
- [ ] Testar listagens

---

## 📝 Comparação: Antes vs Depois

| Aspecto                 | Antes                           | Depois                       |
| ----------------------- | ------------------------------- | ---------------------------- |
| **Tabela COURIER**      | `courier_organizations`         | `employment_contracts`       |
| **Entidade COURIER**    | `CourierOrganization`           | `EmploymentContract`         |
| **Semântica COURIER**   | "Vínculo"                       | "Contrato de Trabalho"       |
| **Tabela CLIENT**       | `contracts`                     | `contracts` (mantido)        |
| **Entidade CLIENT**     | `Contract`                      | `Contract` (mantido)         |
| **Método User**         | `getCourierOrganizationsList()` | `getEmployerOrganizations()` |
| **Método User**         | `hasCourierOrganizations()`     | `hasActiveEmployment()`      |
| **Método Organization** | `getCouriers()`                 | `getEmployees()`             |
| **Método Organization** | `getActiveCouriersCount()`      | `getActiveEmployeesCount()`  |
| **Campo Organization**  | `organizationCouriers`          | `employmentContracts`        |
| **Campo Organization**  | `organizationContracts`         | `serviceContracts`           |

---

## 🚀 Próximos Passos

1. **Criar Repositories** para as entidades
2. **Criar Services** com regras de negócio
3. **Criar Controllers** com endpoints REST
4. **Executar Migrations** no banco de dados
5. **Testar** todas as funcionalidades
6. **Documentar APIs** no Swagger

---

**Documentação atualizada em:** 22 de outubro de 2025  
**Versão:** 3.0 - Employment Contracts refactoring
