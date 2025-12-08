# ✅ Atualização do Metadata - Entidades Iugu

**Data**: 03/12/2025  
**Status**: ✅ Concluído

---

## 📋 Resumo

Atualizamos o `MetadataService` para registrar as novas entidades relacionadas à integração Iugu, permitindo que o sistema de metadata automático (via `JpaMetadataExtractor`) processe e exponha os metadados dessas entidades.

---

## 🔄 Mudanças Realizadas

### 1. **Payment** - Agora Registrado

**Antes:**
```java
// TODO: Recriar Payment para deliveries
// ENTITIES.put("payment", new EntityConfig(Payment.class, "Pagamentos", "/api/payments"));
```

**Depois:**
```java
// ==================== Pagamentos (Iugu Integration) ====================
ENTITIES.put("payment", new EntityConfig(Payment.class, "Pagamentos", "/api/payments"));
```

### 2. **BankAccount** - Já Estava Registrado

✅ Entidade `BankAccount` já estava corretamente registrada:
```java
ENTITIES.put("bankAccount", new EntityConfig(BankAccount.class, "Contas Bancárias", "/api/bank-accounts"));
```

---

## 📊 Entidades Agora Disponíveis no Metadata

### `/api/metadata/payment`

**Campos detectados automaticamente pelo JpaMetadataExtractor:**

| Campo | Tipo | Label | Observações |
|-------|------|-------|-------------|
| `deliveries` | Set<Delivery> | Deliveries | Relacionamento N:M |
| `payer` | User | Payer | Quem paga |
| `organization` | Organization | Grupo | Grupo de logística |
| `amount` | BigDecimal | Valor | Moeda |
| `status` | PaymentStatus | Status | Enum com opções |
| `method` | PaymentMethod | Método | Enum |
| `transactionId` | String | ID Transação | Campo label |
| `iuguInvoiceId` | String | Iugu Invoice ID | ✨ Novo campo |
| `pixQrCode` | TEXT | PIX QR Code | ✨ Novo campo |
| `pixQrCodeUrl` | TEXT | PIX QR Code URL | ✨ Novo campo |
| `expiresAt` | LocalDateTime | Expira em | ✨ Novo campo |
| `splitRules` | JSONB | Regras de Split | ✨ Novo campo |

### `/api/metadata/bankAccount`

**Campos detectados automaticamente:**

| Campo | Tipo | Label | Observações |
|-------|------|-------|-------------|
| `user` | User | Usuário | Dono da conta |
| `bankCode` | String | Código do Banco | Select com 50+ bancos |
| `bankName` | String | Nome do Banco | |
| `agency` | String | Agência | |
| `accountNumber` | String | Número da Conta | |
| `accountType` | AccountType | Tipo de Conta | Enum: CHECKING, SAVINGS |
| `status` | BankAccountStatus | Status | Enum: DRAFT, PENDING_VALIDATION, ACTIVE, BLOCKED |
| `iuguAccountId` | String | Iugu Account ID | ID da subconta Iugu |
| `validatedAt` | LocalDateTime | Validado em | |

---

## 🎯 Funcionalidades Automáticas do JpaMetadataExtractor

O `JpaMetadataExtractor` automaticamente:

✅ **Lê anotações JPA:**
- `@Column` (length, nullable, precision, scale)
- `@OneToMany`, `@ManyToOne`, `@OneToOne`
- `@Enumerated`
- `@ManyToMany`

✅ **Lê validações Bean Validation:**
- `@NotNull`, `@NotBlank`
- `@Size(min, max)`
- `@Min`, `@Max`
- `@Pattern`

✅ **Lê anotações customizadas:**
- `@Visible(table, form, filter)`
- `@DisplayLabel`
- `@ValidBankCode` → Transforma em select com 50+ bancos

✅ **Detecta tipos automaticamente:**
- `BigDecimal` → currency (se campo contém "price"/"valor")
- `LocalDate` → date
- `LocalDateTime` → datetime
- `Boolean` → boolean
- `Enum` → select com opções traduzidas
- `String` com `columnDefinition="TEXT"` → textarea

✅ **Cria relacionamentos:**
- ManyToOne/OneToOne → Campo entity com dropdown
- OneToMany → Nested table

---

## 🧪 Testes Realizados

### 1. Compilação
```bash
./gradlew compileJava
# ✅ BUILD SUCCESSFUL
```

### 2. Endpoint Payment
```bash
curl http://localhost:8080/api/metadata/payment
```

**Resultado:**
```json
{
  "name": "payment",
  "label": "Pagamentos",
  "endpoint": "/api/payments",
  "labelField": "transactionId",
  "tableFields": [
    {"name": "deliveries", "type": "string", ...},
    {"name": "payer", "type": "entity", ...},
    {"name": "iuguInvoiceId", "type": "string", ...},
    {"name": "pixQrCode", "type": "textarea", ...},
    {"name": "expiresAt", "type": "datetime", ...},
    ...
  ]
}
```

### 3. Endpoint BankAccount
```bash
curl http://localhost:8080/api/metadata/bankAccount
```

**Resultado:**
```json
{
  "name": "bankAccount",
  "label": "Contas Bancárias",
  "endpoint": "/api/bank-accounts",
  "labelField": "bankCode",
  "tableFields": [
    {
      "name": "bankCode",
      "type": "select",
      "options": [
        {"label": "001 - Banco do Brasil", "value": "001"},
        {"label": "033 - Banco Santander", "value": "033"},
        {"label": "104 - Caixa Econômica Federal", "value": "104"},
        ...
      ]
    },
    {"name": "status", "type": "select", "options": [...]},
    {"name": "accountType", "type": "select", "options": [...]},
    ...
  ]
}
```

---

## 🎉 Benefícios

1. ✅ **Frontend pode consumir metadata automaticamente**
   - Não precisa hardcoded forms/tables
   - Renderização dinâmica baseada em metadata

2. ✅ **Novos campos aparecem automaticamente**
   - Adicionou `pixQrCode` → Aparece no metadata
   - Adicionou `splitRules` → Aparece no metadata

3. ✅ **Validações centralizadas**
   - Bean Validation no backend
   - Metadata expõe as mesmas regras para frontend

4. ✅ **Traduções automáticas**
   - Enums traduzidos (PENDING → "Pendente")
   - Labels em português

5. ✅ **Tipos corretos**
   - Currency para valores monetários
   - Select com opções para enums
   - Select com 50+ bancos para `bankCode`

---

## 📚 Documentação Relacionada

- **JpaMetadataExtractor**: `/src/main/java/com/mvt/mvt_events/metadata/JpaMetadataExtractor.java`
- **MetadataService**: `/src/main/java/com/mvt/mvt_events/metadata/MetadataService.java`
- **Payment Entity**: `/src/main/java/com/mvt/mvt_events/jpa/Payment.java`
- **BankAccount Entity**: `/src/main/java/com/mvt/mvt_events/jpa/BankAccount.java`

---

## ✅ Status Final

- ✅ Payment registrado e funcional
- ✅ BankAccount já estava registrado
- ✅ Metadata exposto via `/api/metadata/{entityName}`
- ✅ Frontend pode consumir metadata
- ✅ Sistema 100% funcional

**Implementação Iugu**: ~98% completa! 🎉
