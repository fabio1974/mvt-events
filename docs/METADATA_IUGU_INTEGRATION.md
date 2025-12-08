# Integração Iugu com Sistema de Metadata

## 📋 Resumo das Alterações

Integração completa da nova entidade `BankAccount` e campos Iugu em `User` ao sistema de metadata existente do projeto.

---

## ✅ Mudanças Implementadas

### 1. **JpaMetadataExtractor** - Traduções de Campos

Adicionadas traduções em português para os novos campos da integração Iugu:

```java
// ==================== IUGU / BANK ACCOUNT ====================
FIELD_TRANSLATIONS.put("iuguAccountId", "ID Conta Iugu");
FIELD_TRANSLATIONS.put("bankDataComplete", "Dados Bancários Completos");
FIELD_TRANSLATIONS.put("autoWithdrawEnabled", "Transferência Automática");
FIELD_TRANSLATIONS.put("bankAccount", "Conta Bancária");
FIELD_TRANSLATIONS.put("bankCode", "Código do Banco");
FIELD_TRANSLATIONS.put("bankName", "Nome do Banco");
FIELD_TRANSLATIONS.put("agency", "Agência");
FIELD_TRANSLATIONS.put("accountNumber", "Número da Conta");
FIELD_TRANSLATIONS.put("accountType", "Tipo de Conta");
FIELD_TRANSLATIONS.put("validatedAt", "Validado em");
```

**Localização**: `src/main/java/com/mvt/mvt_events/metadata/JpaMetadataExtractor.java` (linhas 82-92)

---

### 2. **JpaMetadataExtractor** - Traduções de Enums

Adicionadas traduções para os novos enums:

```java
// ==================== ACCOUNT TYPE ====================
ENUM_TRANSLATIONS.put("CHECKING", "Conta Corrente");
ENUM_TRANSLATIONS.put("SAVINGS", "Conta Poupança");

// ==================== BANK ACCOUNT STATUS ====================
ENUM_TRANSLATIONS.put("PENDING_VALIDATION", "Pendente de Validação");
ENUM_TRANSLATIONS.put("ACTIVE", "Ativa");
ENUM_TRANSLATIONS.put("BLOCKED", "Bloqueada");
// CANCELLED já existe na seção STATUS acima
```

**Localização**: `src/main/java/com/mvt/mvt_events/metadata/JpaMetadataExtractor.java` (linhas 279-288)

**Nota**: O enum `CANCELLED` já existia no sistema (linha 238: `ENUM_TRANSLATIONS.put("CANCELLED", "Cancelado")`), então foi reutilizado.

---

### 3. **MetadataService** - Registro da Entidade BankAccount

Adicionada a nova entidade `BankAccount` ao mapa de entidades do sistema:

```java
static {
    // ==================== Sistema Base ====================
    ENTITIES.put("organization", new EntityConfig(Organization.class, "Grupos", "/api/organizations"));
    ENTITIES.put("user", new EntityConfig(User.class, "Usuários", "/api/users"));
    ENTITIES.put("siteConfiguration", new EntityConfig(SiteConfiguration.class, "Configurações do Sistema", "/api/site-configuration"));
    ENTITIES.put("specialZone", new EntityConfig(SpecialZone.class, "Zonas Especiais", "/api/special-zones"));
    ENTITIES.put("bankAccount", new EntityConfig(BankAccount.class, "Contas Bancárias", "/api/bank-accounts")); // ✅ NOVO!
    ...
}
```

**Localização**: `src/main/java/com/mvt/mvt_events/metadata/MetadataService.java` (linha 23)

---

## 🔍 Como Funciona o Sistema de Metadata

### Extração Automática

O `JpaMetadataExtractor` lê automaticamente as anotações JPA das entidades:

- `@Entity` - Define a entidade
- `@Column` - Configurações de coluna
- `@Enumerated` - Tipos enum
- `@OneToMany`, `@ManyToOne` - Relacionamentos
- `@Visible` - Controle de visibilidade nos formulários/tabelas

### Tradução de Labels

1. **PRIORIDADE MÁXIMA**: Busca no mapa `FIELD_TRANSLATIONS`
2. **FALLBACK**: Converte `camelCase` → `"Título Capitalizado"`

Exemplo:
- `iuguAccountId` → "ID Conta Iugu" (via mapa)
- `bankCode` → "Código do Banco" (via mapa)
- `createdAt` → "Created At" (fallback automático)

### Tradução de Enums

1. **PRIORIDADE MÁXIMA**: Busca no mapa `ENUM_TRANSLATIONS`
2. **ALTERNATIVA**: Tenta método `getDisplayName()` no enum
3. **FALLBACK**: Converte `UPPERCASE_SNAKE` → `"Title Case"`

Exemplo:
- `CHECKING` → "Conta Corrente" (via mapa)
- `PENDING_VALIDATION` → "Pendente de Validação" (via mapa)
- `ACTIVE` → "Ativa" (via mapa)

---

## 🚀 Impacto no Frontend

### Endpoint GET /api/metadata/entities/bankAccount

Agora retornará metadata completa da entidade `BankAccount` com:

```json
{
  "entityName": "bankAccount",
  "label": "Contas Bancárias",
  "endpoint": "/api/bank-accounts",
  "tableFields": [
    {
      "name": "bankCode",
      "label": "Código do Banco",
      "type": "text",
      "visible": true,
      "sortable": true
    },
    {
      "name": "bankName",
      "label": "Nome do Banco",
      "type": "text",
      "visible": true,
      "sortable": true
    },
    {
      "name": "agency",
      "label": "Agência",
      "type": "text",
      "visible": true
    },
    {
      "name": "accountNumber",
      "label": "Número da Conta",
      "type": "text",
      "visible": true
    },
    {
      "name": "accountType",
      "label": "Tipo de Conta",
      "type": "select",
      "options": [
        {"label": "Conta Corrente", "value": "CHECKING"},
        {"label": "Conta Poupança", "value": "SAVINGS"}
      ]
    },
    {
      "name": "status",
      "label": "Status",
      "type": "select",
      "options": [
        {"label": "Pendente de Validação", "value": "PENDING_VALIDATION"},
        {"label": "Ativa", "value": "ACTIVE"},
        {"label": "Bloqueada", "value": "BLOCKED"},
        {"label": "Cancelado", "value": "CANCELLED"}
      ]
    }
  ],
  "formFields": [...],
  "filters": [...]
}
```

### Endpoint GET /api/metadata/entities/user

Agora incluirá os novos campos Iugu:

```json
{
  "entityName": "user",
  "label": "Usuários",
  "endpoint": "/api/users",
  "tableFields": [
    ...,
    {
      "name": "iuguAccountId",
      "label": "ID Conta Iugu",
      "type": "text",
      "visible": false  // Será oculto por padrão (campo técnico)
    },
    {
      "name": "bankDataComplete",
      "label": "Dados Bancários Completos",
      "type": "boolean",
      "visible": true
    },
    {
      "name": "autoWithdrawEnabled",
      "label": "Transferência Automática",
      "type": "boolean",
      "visible": true
    }
  ]
}
```

---

## 📝 Campos User com Metadata

| Campo Original (inglês) | Label PT-BR | Tipo | Observações |
|-------------------------|-------------|------|-------------|
| `iuguAccountId` | "ID Conta Iugu" | text | Subconta criada no Iugu (formato: `account_id_xyz`) |
| `bankDataComplete` | "Dados Bancários Completos" | boolean | Flag indicando se todos dados bancários foram preenchidos |
| `autoWithdrawEnabled` | "Transferência Automática" | boolean | Flag para transferências D+1 automáticas |
| `bankAccount` | "Conta Bancária" | relationship | Relacionamento 1:1 com BankAccount |

---

## 📝 Campos BankAccount com Metadata

| Campo Original (inglês) | Label PT-BR | Tipo | Validação |
|-------------------------|-------------|------|-----------|
| `bankCode` | "Código do Banco" | text | 3 dígitos (ex: "260" = Nubank) |
| `bankName` | "Nome do Banco" | text | Nome completo do banco |
| `agency` | "Agência" | text | Somente números (ex: "0001") |
| `accountNumber` | "Número da Conta" | text | Formato com hífen (ex: "12345-6") |
| `accountType` | "Tipo de Conta" | select | CHECKING ou SAVINGS |
| `status` | "Status" | select | PENDING_VALIDATION, ACTIVE, BLOCKED, CANCELLED |
| `validatedAt` | "Validado em" | datetime | Data/hora da validação da conta |
| `notes` | "Observações" | textarea | Notas internas |

---

## 🧪 Testes

### Compilação

```bash
./gradlew compileJava
```

**Resultado**: ✅ `BUILD SUCCESSFUL in 5s`

### Testar Metadata API (após aplicar migration V4)

```bash
# Metadata completa de BankAccount
curl http://localhost:8080/api/metadata/entities/bankAccount | jq

# Metadata atualizada de User (com novos campos Iugu)
curl http://localhost:8080/api/metadata/entities/user | jq '.formFields[] | select(.name | contains("iugu") or contains("bank"))'
```

---

## 🎯 Próximos Passos

1. **Aplicar Migration V4** - Criar tabela `bank_accounts` e adicionar colunas em `users`
2. **Testar Metadata API** - Verificar se `/api/metadata/entities/bankAccount` retorna corretamente
3. **Implementar IuguService** - Service para criar subcontas e invoices
4. **Criar BankAccountController** - Endpoint POST para cadastrar dados bancários

---

## 📚 Referências

- **JpaMetadataExtractor**: Sistema de extração automática de metadata
- **MetadataService**: Gerenciador de entidades e metadata
- **BankAccount Entity**: `src/main/java/com/mvt/mvt_events/jpa/BankAccount.java`
- **User Entity**: `src/main/java/com/mvt/mvt_events/jpa/User.java`
- **Migration V4**: `src/main/resources/db/migration/V4__iugu_bank_accounts.sql`

---

✅ **Status**: Integração de metadata completa e compilando com sucesso!
