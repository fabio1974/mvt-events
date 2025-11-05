# ✅ REFATORAÇÃO COMPLETA: Employment Contracts

**Data:** 22 de outubro de 2025  
**Status:** ✅ Código refatorado e pronto para deployment  
**Versão:** 3.0

---

## 🎯 O que foi feito?

Refatoração completa do sistema de relacionamentos N:M entre usuários (COURIER/CLIENT) e Organizations, com foco em **nomenclatura clara** e **semântica precisa**.

---

## 📝 Mudanças Principais

### **1. Renomeação: CourierOrganization → EmploymentContract**

**Por quê?**

- ✅ `CourierOrganization` era genérico e não deixava claro o tipo de relação
- ✅ `EmploymentContract` deixa **explícito** que é uma relação **empregado-empresa**
- ✅ Diferencia claramente de `Contract` (contratos de serviço cliente-fornecedor)

| Item                | Antes                   | Depois                   |
| ------------------- | ----------------------- | ------------------------ |
| **Tabela**          | `courier_organizations` | `employment_contracts`   |
| **Entidade**        | `CourierOrganization`   | `EmploymentContract`     |
| **Tipo de relação** | Vínculo genérico        | **Contrato de trabalho** |

---

### **2. Dois Tipos Distintos de Contratos**

```
┌──────────────────────────────────────────────────────┐
│           EMPLOYMENT CONTRACTS                       │
│  (Contratos de Trabalho - Empregado-Empresa)         │
│                                                      │
│  COURIER ←→ Organization                             │
│  • is_active (ativo/inativo)                         │
│  • linked_at (data de contratação)                   │
│  • Múltiplos empregos permitidos                    │
└──────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────┐
│             SERVICE CONTRACTS                        │
│  (Contratos de Serviço - Cliente-Fornecedor)         │
│                                                      │
│  CLIENT ←→ Organization                              │
│  • is_primary (titular/secundário)                   │
│  • status (ACTIVE/SUSPENDED/CANCELLED)              │
│  • contract_number, start_date, end_date             │
│  • Múltiplos contratos, mas apenas 1 titular        │
└──────────────────────────────────────────────────────┘
```

---

## 📁 Arquivos Criados

### **Entidades**

- ✅ `/src/main/java/com/mvt/mvt_events/jpa/EmploymentContract.java`

### **Migrations**

- ✅ `/src/main/resources/db/migration/V40__create_employment_contracts_and_service_contracts.sql`

  - Cria tabela `employment_contracts`
  - Cria tabela `contracts`
  - Cria trigger `check_primary_contract()` para garantir apenas 1 contrato titular
  - Adiciona índices para performance

- ✅ `/src/main/resources/db/migration/V41__migrate_legacy_data_and_cleanup.sql`
  - Migra dados de `courier_organizations` → `employment_contracts` (se existir)
  - Migra dados de `courier_adm_links` → `employment_contracts` (se existir)
  - Migra dados de `client_manager_links` → `contracts` (se existir)
  - Remove tabelas antigas (`courier_adm_links`, `client_manager_links`, `courier_organizations`)
  - Valida e corrige contratos primários

### **Documentação**

- ✅ `/docs/implementation/N_M_RELATIONSHIPS_V3.md` - Documentação completa atualizada
- ✅ `/docs/implementation/EMPLOYMENT_CONTRACT_REFACTORING.md` - Guia de refatoração

---

## 📝 Arquivos Modificados

### **User.java**

```java
// ANTES
private Set<CourierOrganization> courierOrganizations;
public Set<Organization> getCourierOrganizationsList() { ... }
public boolean hasCourierOrganizations() { ... }

// DEPOIS
private Set<EmploymentContract> employmentContracts;
public Set<Organization> getEmployerOrganizations() { ... }
public boolean hasActiveEmployment() { ... }
```

### **Organization.java**

```java
// ANTES
private Set<CourierOrganization> organizationCouriers;
private Set<Contract> organizationContracts;
public Set<User> getCouriers() { ... }
public long getActiveCouriersCount() { ... }

// DEPOIS
private Set<EmploymentContract> employmentContracts;
private Set<Contract> serviceContracts;
public Set<User> getEmployees() { ... }
public long getActiveEmployeesCount() { ... }
```

---

## 🗑️ Arquivos Removidos

- ✅ `/src/main/java/com/mvt/mvt_events/jpa/CourierOrganization.java` - Entidade antiga

---

## 🎯 Métodos Renomeados

### **Em User.java**

| Método Antigo                   | Método Novo                    | Descrição                                     |
| ------------------------------- | ------------------------------ | --------------------------------------------- |
| `getCourierOrganizationsList()` | `getEmployerOrganizations()`   | Retorna organizações onde trabalha            |
| `hasCourierOrganizations()`     | `hasActiveEmployment()`        | Verifica se tem emprego ativo                 |
| -                               | `getClientOrganizationsList()` | Retorna organizações onde é cliente (mantido) |
| -                               | `hasActiveContracts()`         | Verifica se tem contratos ativos (mantido)    |

### **Em Organization.java**

| Método Antigo               | Método Novo                        | Descrição                              |
| --------------------------- | ---------------------------------- | -------------------------------------- |
| `getCouriers()`             | `getEmployees()`                   | Retorna funcionários (couriers) ativos |
| `getActiveCouriersCount()`  | `getActiveEmployeesCount()`        | Conta funcionários ativos              |
| `getActiveContractsCount()` | `getActiveServiceContractsCount()` | Conta contratos de serviço ativos      |
| -                           | `getClients()`                     | Retorna clientes ativos (mantido)      |

---

## ✅ Compilação

```bash
# Status: ✅ SEM ERROS
- User.java: ✅
- Organization.java: ✅
- EmploymentContract.java: ✅
- Contract.java: ✅
```

---

## 🚀 Próximos Passos

### **Fase 1: Executar Migrations** ⏳

```bash
# Opção 1: Rodar o sistema (migrations automáticas)
./gradlew bootRun

# Opção 2: Migrations manuais
./gradlew flywayMigrate
```

### **Fase 2: Verificar Banco de Dados** ⏳

```sql
-- Verificar tabelas criadas
SELECT table_name FROM information_schema.tables
WHERE table_name IN ('employment_contracts', 'contracts');

-- Verificar dados migrados
SELECT COUNT(*) FROM employment_contracts;
SELECT COUNT(*) FROM contracts;

-- Verificar contratos titulares
SELECT COUNT(*) FROM contracts WHERE is_primary = TRUE;

-- Verificar trigger
SELECT trigger_name FROM information_schema.triggers
WHERE trigger_name = 'enforce_single_primary_contract';
```

### **Fase 3: Criar Repositories** ⏳

- [ ] `EmploymentContractRepository.java`
- [ ] `ContractRepository.java`

### **Fase 4: Criar Services** ⏳

- [ ] `EmploymentContractService.java`
  - `linkCourierToOrganization()`
  - `unlinkCourierFromOrganization()`
  - `activateEmployment()`
  - `deactivateEmployment()`
- [ ] `ContractService.java`
  - `createContract()`
  - `setPrimaryContract()`
  - `suspendContract()`
  - `cancelContract()`

### **Fase 5: Criar Controllers** ⏳

- [ ] `EmploymentContractController.java`
  - `POST /api/employment-contracts` - Criar contrato de trabalho
  - `GET /api/employment-contracts` - Listar contratos
  - `PUT /api/employment-contracts/{id}/activate` - Ativar
  - `PUT /api/employment-contracts/{id}/deactivate` - Desativar
- [ ] `ContractController.java`
  - `POST /api/contracts` - Criar contrato de serviço
  - `GET /api/contracts` - Listar contratos
  - `PUT /api/contracts/{id}/set-primary` - Marcar como titular
  - `PUT /api/contracts/{id}/suspend` - Suspender
  - `PUT /api/contracts/{id}/cancel` - Cancelar

### **Fase 6: Testes** ⏳

- [ ] Testar criação de employment contracts
- [ ] Testar ativação/desativação de employment
- [ ] Testar criação de service contracts
- [ ] Testar contrato titular único (trigger)
- [ ] Testar status de contratos
- [ ] Testar datas de vigência

---

## 📊 Resumo das Regras de Negócio

### **Employment Contracts (COURIER ↔ Organization)**

1. ✅ Um motoboy pode trabalhar para **múltiplas organizações**
2. ✅ Contratos podem ser **ativados/desativados**
3. ✅ **Histórico mantido** via `linked_at`
4. ✅ **Unicidade**: 1 courier não pode ter 2 contratos com a mesma org

### **Service Contracts (CLIENT ↔ Organization)**

1. ✅ Um cliente pode ter **múltiplos contratos**
2. ✅ **Apenas 1 contrato titular** por cliente (`is_primary = true`)
3. ✅ **Trigger automático** desmarca outros ao marcar um como titular
4. ✅ **Status**: ACTIVE, SUSPENDED, CANCELLED
5. ✅ **Vigência**: start_date, end_date (opcional)
6. ✅ **Número único** de contrato no sistema

---

## 💡 Benefícios da Refatoração

| Antes                  | Depois                | Benefício                               |
| ---------------------- | --------------------- | --------------------------------------- |
| `CourierOrganization`  | `EmploymentContract`  | ✅ Nome auto-explicativo                |
| `courierOrganizations` | `employmentContracts` | ✅ Consistência de nomenclatura         |
| `getCouriers()`        | `getEmployees()`      | ✅ Terminologia de RH padrão            |
| Genérico               | Específico            | ✅ Semântica clara: trabalho vs serviço |
| 1 tipo de vínculo      | 2 tipos de contrato   | ✅ Separação de responsabilidades       |

---

## ⚠️ Breaking Changes

Se houver código externo (testes, outros serviços) que usem:

- `CourierOrganization` → Atualizar para `EmploymentContract`
- `getCourierOrganizationsList()` → Atualizar para `getEmployerOrganizations()`
- `hasCourierOrganizations()` → Atualizar para `hasActiveEmployment()`
- `getCouriers()` → Atualizar para `getEmployees()`

---

## 📞 Suporte

Para dúvidas ou problemas:

1. Consulte `/docs/implementation/N_M_RELATIONSHIPS_V3.md`
2. Consulte `/docs/implementation/EMPLOYMENT_CONTRACT_REFACTORING.md`
3. Verifique logs das migrations em `flyway_schema_history`

---

**Status Final:** ✅ Código refatorado, compilado e pronto para deployment  
**Próximo Passo:** Executar migrations no banco de dados com `./gradlew bootRun`
