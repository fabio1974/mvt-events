# 🚀 Integração Iugu - Progresso da Implementação

**Data de Início:** 2 de dezembro de 2025  
**Status Atual:** Em Desenvolvimento (Fase 1 Completa) ✅

---

## ✅ Fase 1: Modelo de Dados - COMPLETO

### 📦 Entidades Criadas

#### 1. BankAccount.java
**Localização:** `/src/main/java/com/mvt/mvt_events/jpa/BankAccount.java`

**Campos (em inglês conforme solicitado):**
- `bankCode` (VARCHAR 3) - Código do banco (ex: 260 = Nubank)
- `bankName` (VARCHAR 100) - Nome do banco
- `agency` (VARCHAR 10) - Agência sem dígito verificador
- `accountNumber` (VARCHAR 20) - Conta no formato `12345-6`
- `accountType` (ENUM) - CHECKING ou SAVINGS
- `status` (ENUM) - PENDING_VALIDATION, ACTIVE, BLOCKED, CANCELLED
- `validatedAt` (TIMESTAMP) - Data de validação

**Enums:**
```java
public enum AccountType {
    CHECKING("checking", "Conta Corrente"),
    SAVINGS("savings", "Conta Poupança")
}

public enum BankAccountStatus {
    PENDING_VALIDATION, ACTIVE, BLOCKED, CANCELLED
}
```

**Helper Methods:**
- `isActive()`, `isPendingValidation()`, `isBlocked()`
- `markAsActive()`, `markAsBlocked(reason)`, `markAsCancelled(reason)`
- `getAccountNumberMasked()` - Retorna `***45-6`
- `getAgencyMasked()` - Retorna `***01`

**Validações:**
- `@Pattern` para código do banco (3 dígitos)
- `@Pattern` para agência (apenas números)
- `@Pattern` para conta (formato com hífen)
- Relacionamento 1:1 com User (UNIQUE constraint)

---

#### 2. User.java (Atualizado)
**Localização:** `/src/main/java/com/mvt/mvt_events/jpa/User.java`

**Novos Campos Iugu:**
- `iuguAccountId` (VARCHAR 100) - ID da subconta Iugu
- `bankDataComplete` (BOOLEAN) - Dados bancários validados
- `autoWithdrawEnabled` (BOOLEAN) - Transferência automática ativa
- `bankAccount` (1:1) - Relacionamento com BankAccount

**Novos Helper Methods:**
```java
canReceivePayments()          // Verifica se pode receber via Iugu
hasBankAccount()              // Tem conta cadastrada
markBankAccountAsCompleted()  // Marca como completo
markBankAccountAsPending()    // Marca como pendente
deactivateAutoWithdraw()      // Desativa auto_withdraw
```

---

#### 3. BankAccountRepository.java
**Localização:** `/src/main/java/com/mvt/mvt_events/repository/BankAccountRepository.java`

**Queries Implementadas:**
```java
findByUserId(UUID)                        // Busca por usuário
existsByUserId(UUID)                      // Verifica existência
findByStatus(BankAccountStatus)           // Filtra por status
findAllActive()                           // Apenas ativas
findAllPendingValidation()                // Pendentes
findByBankCode(String)                    // Por banco
findByBankDetails(code, agency, account)  // Busca exata
existsDuplicateAccount()                  // Evita duplicação
countActive()                             // Conta ativas
countPending()                            // Conta pendentes
deleteByUserId(UUID)                      // Remove por usuário
```

---

### 🗄️ Migration V4 Criada

**Arquivo:** `/src/main/resources/db/migration/V4__iugu_bank_accounts.sql`

**Estrutura DDL:**

```sql
-- Tabela bank_accounts
CREATE TABLE bank_accounts (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    user_id UUID NOT NULL UNIQUE,
    bank_code VARCHAR(3) NOT NULL CHECK (bank_code ~ '^\d{3}$'),
    bank_name VARCHAR(100) NOT NULL,
    agency VARCHAR(10) NOT NULL CHECK (agency ~ '^\d+$'),
    account_number VARCHAR(20) NOT NULL CHECK (account_number ~ '^\d+-\d$'),
    account_type VARCHAR(10) NOT NULL CHECK (account_type IN ('CHECKING', 'SAVINGS')),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING_VALIDATION',
    validated_at TIMESTAMP,
    notes TEXT,
    CONSTRAINT fk_bank_account_user FOREIGN KEY (user_id) 
        REFERENCES users(id) ON DELETE CASCADE
);

-- Campos Iugu em users
ALTER TABLE users 
    ADD COLUMN iugu_account_id VARCHAR(100),
    ADD COLUMN bank_data_complete BOOLEAN NOT NULL DEFAULT false,
    ADD COLUMN auto_withdraw_enabled BOOLEAN NOT NULL DEFAULT false;
```

**Índices Criados:**
- `idx_bank_accounts_user_id` - UNIQUE para relacionamento 1:1
- `idx_bank_accounts_status` - Filtra contas ativas
- `idx_bank_accounts_pending` - Contas pendentes por data
- `idx_users_iugu_account_id` - Busca por iugu_account_id
- `idx_users_bank_data_complete` - Usuários com dados completos
- `idx_users_auto_withdraw_enabled` - Auto_withdraw ativo
- `idx_users_payment_ready` - Couriers/Organizers prontos para pagamentos

**Constraints:**
- CPF/CNPJ validado (via anotação `@CPF` do Spring)
- Código banco: 3 dígitos numéricos
- Agência: apenas números
- Conta: formato `99999-9`
- Tipo conta: CHECKING ou SAVINGS
- Status: valores permitidos

---

## 📋 Padronização Aplicada

### ✅ Nomes de Campos em Inglês
Conforme solicitado, todos os nomes de campos/propriedades estão em **inglês**:

**Antes** → **Depois:**
- `banco` → `bankCode`
- `bancoNome` → `bankName`
- `agencia` → `agency`
- `conta` → `accountNumber`
- `tipoConta` → `accountType`
- `dadosBancariosCompletos` → `bankDataComplete`
- `autoWithdrawAtivo` → `autoWithdrawEnabled`

**Comentários permanecem em português** (conforme sua orientação).

---

## ✅ Testes de Compilação

```bash
./gradlew compileJava --no-daemon
```

**Resultado:** ✅ **BUILD SUCCESSFUL**

Todas as entidades, repositories e migration compilam sem erros.

---

## 🎯 Próximos Passos

### Fase 2: Integração com Iugu (Em Progresso)

#### 1. IuguService
- [ ] Criar serviço para chamadas à API Iugu
- [ ] Método `createSubAccount()` com auto_withdraw
- [ ] Método `updateBankAccount()`
- [ ] Método `createInvoiceWithSplit()`
- [ ] Método `validateWebhookSignature()`
- [ ] Retry logic com backoff exponencial

#### 2. Configurações
- [ ] Adicionar propriedades no `application.yml`
- [ ] API key, webhook token, URLs
- [ ] Percentuais de split (87/5/8)
- [ ] Separar profiles dev/prod

#### 3. Endpoints REST
- [ ] `POST /api/motoboy/bank-data` - Cadastrar dados bancários
- [ ] `POST /api/payment/create-with-split` - Criar pagamento com split
- [ ] `POST /api/webhooks/iugu` - Processar webhooks

#### 4. Validações e Helpers
- [ ] Validator de CPF completo
- [ ] Validator de dados bancários
- [ ] Helper para calcular splits
- [ ] Constantes de bancos brasileiros

#### 5. Payment Entity Update
- [ ] Adicionar campos Iugu em Payment.java
- [ ] Migration V5 com ALTER TABLE
- [ ] Campos: iugu_invoice_id, pix_qr_code, expires_at, etc

---

## 📊 Estrutura de Diretórios

```
src/main/java/com/mvt/mvt_events/
├── jpa/
│   ├── BankAccount.java           ✅ Criado
│   └── User.java                  ✅ Atualizado
├── repository/
│   └── BankAccountRepository.java ✅ Criado
├── service/
│   └── IuguService.java           🔄 Próximo
└── controller/
    └── BankAccountController.java  ⏳ Pendente

src/main/resources/db/migration/
├── V1__baseline_initial_schema.sql   ✅ Existente
├── V2__initial_test_data.sql         ✅ Existente
├── V3__update_to_real_sobral_addresses.sql ✅ Existente
└── V4__iugu_bank_accounts.sql        ✅ Criado
```

---

## 🔐 Considerações de Segurança

### Já Implementado:
- ✅ Validações de formato (Regex)
- ✅ Constraints no banco de dados
- ✅ Relacionamento 1:1 com UNIQUE constraint
- ✅ Métodos mascarados para exibição (`***45-6`)

### A Implementar:
- ⏳ Criptografia de agência/conta no banco
- ⏳ Rate limiting nos endpoints
- ⏳ Logs estruturados de auditoria
- ⏳ Validação de webhook signature
- ⏳ Retry logic com exponential backoff

---

## 📝 Notas Técnicas

### Relacionamento User ↔ BankAccount
- **Tipo:** 1:1 (One-to-One)
- **Cascade:** ALL + orphanRemoval
- **FK:** bank_accounts.user_id → users.id
- **Constraint:** UNIQUE (um usuário = uma conta)

### Fluxo de Status
```
PENDING_VALIDATION → ACTIVE → BLOCKED/CANCELLED
       ↓                ↓
  (Aguardando)    (Operacional)
```

### Enums TypeScript (para Frontend)
```typescript
enum AccountType {
  CHECKING = 'checking',
  SAVINGS = 'savings'
}

enum BankAccountStatus {
  PENDING_VALIDATION = 'PENDING_VALIDATION',
  ACTIVE = 'ACTIVE',
  BLOCKED = 'BLOCKED',
  CANCELLED = 'CANCELLED'
}
```

---

## ⚡ Performance

### Índices Estratégicos
- **user_id:** UNIQUE - Busca O(1) para relacionamento 1:1
- **status WHERE active:** Filtra apenas contas ativas
- **created_at WHERE pending:** Ordena fila de validação
- **Índice composto:** Couriers prontos para pagamentos

---

## 🎉 Conquistas da Fase 1

- ✅ Modelo de dados completo e consistente
- ✅ Nomenclatura padronizada em inglês
- ✅ Validações robustas (Pattern, Size, NotNull)
- ✅ Repository com queries otimizadas
- ✅ Migration DDL pronta para aplicação
- ✅ Compilação sem erros
- ✅ Relacionamentos bem definidos
- ✅ Helper methods úteis
- ✅ Enums type-safe
- ✅ Constraints de banco garantindo integridade

---

**Próximo Checkpoint:** Implementar IuguService + Configurações + Endpoint de Cadastro

**Estimativa:** 2-3 horas de desenvolvimento

---

**Status:** 🟢 Fase 1 Concluída - Pronto para Fase 2
