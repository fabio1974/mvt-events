# 🔄 Refatoração: CourierOrganization → EmploymentContract

**Data:** 22 de outubro de 2025  
**Versão:** 3.0

---

## 📝 Resumo das Mudanças

### **Motivação**

A tabela e entidade `CourierOrganization` foi renomeada para `EmploymentContract` para tornar mais clara a semântica da relação **empregado-empresa**.

### **Nomenclatura**

| Conceito                | Nome Antigo                     | Nome Novo                    | Justificativa                             |
| ----------------------- | ------------------------------- | ---------------------------- | ----------------------------------------- |
| **Tabela**              | `courier_organizations`         | `employment_contracts`       | Deixa claro que é um contrato de trabalho |
| **Entidade**            | `CourierOrganization`           | `EmploymentContract`         | Alinhado com terminologia de RH           |
| **Campo User**          | `courierOrganizations`          | `employmentContracts`        | Mais descritivo                           |
| **Campo Organization**  | `organizationCouriers`          | `employmentContracts`        | Consistente com a entidade                |
| **Método User**         | `getCourierOrganizationsList()` | `getEmployerOrganizations()` | Retorna os "empregadores"                 |
| **Método User**         | `hasCourierOrganizations()`     | `hasActiveEmployment()`      | Pergunta se "tem emprego ativo"           |
| **Método Organization** | `getCouriers()`                 | `getEmployees()`             | Retorna os "funcionários"                 |
| **Método Organization** | `getActiveCouriersCount()`      | `getActiveEmployeesCount()`  | Conta "funcionários ativos"               |

---

## 🎯 Dois Tipos de Contratos

### **1. Employment Contracts** (Contratos de Trabalho)

- **Quem:** COURIER (funcionário)
- **Com quem:** Organization (empregador)
- **Tipo:** Empregado-Empresa
- **Tabela:** `employment_contracts`
- **Campos especiais:** `is_active`, `linked_at`

### **2. Service Contracts** (Contratos de Serviço)

- **Quem:** CLIENT (cliente)
- **Com quem:** Organization (fornecedor)
- **Tipo:** Cliente-Fornecedor
- **Tabela:** `contracts`
- **Campos especiais:** `is_primary`, `status`, `contract_number`, `start_date`, `end_date`

---

## 📊 Estrutura Final

```
COURIER (Funcionário)
    ↓ N:M via employment_contracts
Organization (Empregador)
    ↑ N:M via contracts
CLIENT (Cliente)
```

---

## ✅ Arquivos Criados

1. `/src/main/java/com/mvt/mvt_events/jpa/EmploymentContract.java` - Nova entidade
2. `/src/main/resources/db/migration/V40__create_employment_contracts_and_service_contracts.sql` - Migration de criação
3. `/src/main/resources/db/migration/V41__migrate_legacy_data_and_cleanup.sql` - Migration de migração e limpeza
4. `/docs/implementation/N_M_RELATIONSHIPS_V3.md` - Documentação atualizada

---

## ✅ Arquivos Modificados

1. `/src/main/java/com/mvt/mvt_events/jpa/User.java` - Atualizado relacionamentos e métodos
2. `/src/main/java/com/mvt/mvt_events/jpa/Organization.java` - Atualizado relacionamentos e métodos

---

## 🗑️ Arquivos para Remover (Após confirmar que tudo funciona)

1. `/src/main/java/com/mvt/mvt_events/jpa/CourierOrganization.java` - Entidade antiga

---

## 📋 Migrations Criadas

### **V40: Criar Tabelas**

- Cria `employment_contracts`
- Cria `contracts`
- Cria trigger `check_primary_contract()` para garantir apenas 1 contrato titular por cliente
- Adiciona índices para performance

### **V41: Migrar Dados Legacy**

- Migra `courier_organizations` → `employment_contracts` (se existir)
- Migra `courier_adm_links` → `employment_contracts` (se existir)
- Migra `client_manager_links` → `contracts` (se existir)
- Remove tabelas antigas: `courier_adm_links`, `client_manager_links`, `courier_organizations`
- Valida que todos os clientes tenham um contrato primário

---

## 🚀 Como Aplicar

### 1. **Verificar Código**

```bash
# Verificar se há erros de compilação
./gradlew clean build
```

### 2. **Executar Migrations**

```bash
# Iniciar o sistema (migrations rodam automaticamente)
./gradlew bootRun

# Ou executar migrations manualmente
./gradlew flywayMigrate
```

### 3. **Verificar Migrations**

```sql
-- Verificar se as tabelas foram criadas
SELECT table_name
FROM information_schema.tables
WHERE table_name IN ('employment_contracts', 'contracts');

-- Verificar trigger
SELECT trigger_name, event_manipulation
FROM information_schema.triggers
WHERE trigger_name = 'enforce_single_primary_contract';

-- Verificar dados migrados
SELECT COUNT(*) FROM employment_contracts;
SELECT COUNT(*) FROM contracts;
SELECT COUNT(*) FROM contracts WHERE is_primary = TRUE;
```

### 4. **Remover Arquivo Antigo**

```bash
# Após confirmar que tudo funciona
rm src/main/java/com/mvt/mvt_events/jpa/CourierOrganization.java
```

---

## ⚠️ Atenção

### **Breaking Changes**

Se houver código que usa os nomes antigos, será necessário atualizar:

- `CourierOrganization` → `EmploymentContract`
- `courierOrganizations` → `employmentContracts`
- `getCourierOrganizationsList()` → `getEmployerOrganizations()`
- `hasCourierOrganizations()` → `hasActiveEmployment()`

### **Validações Importantes**

1. Verificar se há código em Services/Controllers que use os nomes antigos
2. Verificar se há testes que referenciem os nomes antigos
3. Confirmar que as migrations rodaram sem erros
4. Verificar que os dados foram migrados corretamente

---

## 📞 Próximos Passos

1. ✅ Entidades criadas e atualizadas
2. ✅ Migrations criadas
3. ⏳ Executar migrations no banco
4. ⏳ Criar Repositories
5. ⏳ Criar Services
6. ⏳ Criar Controllers
7. ⏳ Testar funcionalidades
8. ⏳ Atualizar documentação de API

---

**Status:** ✅ Refatoração de código completa - Pronto para executar migrations
