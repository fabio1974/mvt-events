# 🏦 Sistema de Validação de Bancos Brasileiros

**Data**: 2025-12-02  
**Status**: ✅ Implementado e Compilando

---

## 📋 Visão Geral

Sistema de validação de códigos bancários usando **classe de constantes** + **validador customizado**.

### ✅ Por que NÃO usamos Enum?

- ❌ Brasil tem ~150+ bancos (enum ficaria enorme)
- ❌ Novos bancos surgem frequentemente (precisaria recompilar)
- ❌ Códigos numéricos (260, 341, 001) não combinam bem com enums
- ✅ String + validador é mais flexível e manutenível

---

## 🏗️ Arquitetura Implementada

### 1. **BrazilianBanks** - Classe de Constantes

**Arquivo**: `src/main/java/com/mvt/mvt_events/util/BrazilianBanks.java`

```java
@Component
public class BrazilianBanks {
    private static final Map<String, String> BANKS;
    
    static {
        Map<String, String> banks = new LinkedHashMap<>();
        banks.put("001", "Banco do Brasil");
        banks.put("260", "Nubank (Nu Pagamentos)");
        banks.put("341", "Banco Itaú");
        // ... ~50 bancos cadastrados
        BANKS = Collections.unmodifiableMap(banks);
    }
}
```

**Recursos**:
- ✅ 50+ bancos cadastrados (tradicionais + digitais)
- ✅ Mapa imutável (thread-safe)
- ✅ Métodos utilitários
- ✅ Constantes para bancos comuns

---

### 2. **ValidBankCode** - Anotação de Validação

**Arquivo**: `src/main/java/com/mvt/mvt_events/validation/ValidBankCode.java`

```java
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidBankCode.BankCodeValidator.class)
public @interface ValidBankCode {
    String message() default "Código de banco inválido...";
    boolean formatOnly() default false;
}
```

**Funcionamento**:
1. Valida formato (3 dígitos numéricos)
2. Verifica se código existe no mapa `BrazilianBanks`
3. Retorna mensagem de erro customizada

---

### 3. **BankAccount** - Uso da Validação

**Antes**:
```java
@Pattern(regexp = "\\d{3}", message = "...")
@Size(min = 3, max = 3, message = "...")
private String bankCode;
```

**Depois**:
```java
@ValidBankCode(message = "Código do banco inválido ou não cadastrado")
private String bankCode;
```

✅ **Mais simples, mais claro, mais robusto!**

---

## 🔍 Métodos Disponíveis

### `BrazilianBanks` - Métodos Públicos

| Método | Descrição | Exemplo |
|--------|-----------|---------|
| `getAllBanks()` | Retorna mapa completo | `Map<String, String>` |
| `getAllBankCodes()` | Retorna Set com códigos | `Set<String>` |
| `isValidBankCode(code)` | Verifica se código existe | `isValidBankCode("260")` → `true` |
| `getBankName(code)` | Retorna nome do banco | `getBankName("260")` → `"Nubank..."` |
| `hasValidFormat(code)` | Valida formato (3 dígitos) | `hasValidFormat("260")` → `true` |
| `isValid(code)` | Valida formato + existência | `isValid("999")` → `false` |
| `getFormattedBankInfo(code)` | Retorna "código - nome" | `"260 - Nubank"` |

### Constantes de Acesso Rápido

```java
BrazilianBanks.Common.NUBANK        // "260"
BrazilianBanks.Common.ITAU          // "341"
BrazilianBanks.Common.BRADESCO      // "237"
BrazilianBanks.Common.BANCO_DO_BRASIL // "001"
BrazilianBanks.Common.INTER         // "077"
// ... etc
```

---

## 💡 Exemplos de Uso

### 1. **Validação Automática (Bean Validation)**

```java
@Entity
public class BankAccount {
    @ValidBankCode  // ← Validação automática!
    private String bankCode;
}
```

Se tentar salvar com código inválido:
```java
BankAccount account = new BankAccount();
account.setBankCode("999"); // Código inexistente
repository.save(account); 
// ❌ ConstraintViolationException: 
// "Código de banco '999' não encontrado no cadastro do Banco Central"
```

---

### 2. **Validação Manual no Service**

```java
@Service
public class BankAccountService {
    
    public void createAccount(BankAccountDTO dto) {
        // Validação manual
        if (!BrazilianBanks.isValid(dto.getBankCode())) {
            throw new IllegalArgumentException(
                "Banco inválido: " + dto.getBankCode()
            );
        }
        
        // Auto-preencher nome do banco
        String bankName = BrazilianBanks.getBankName(dto.getBankCode());
        account.setBankName(bankName);
        
        // Salvar...
    }
}
```

---

### 3. **Endpoint para Listar Bancos**

```java
@RestController
@RequestMapping("/api/banks")
public class BankController {
    
    @GetMapping
    public Map<String, String> getAllBanks() {
        return BrazilianBanks.getAllBanks();
    }
    
    @GetMapping("/{code}")
    public ResponseEntity<BankInfoDTO> getBankInfo(@PathVariable String code) {
        if (!BrazilianBanks.isValid(code)) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(new BankInfoDTO(
            code,
            BrazilianBanks.getBankName(code)
        ));
    }
}
```

**Response** de `GET /api/banks`:
```json
{
  "001": "Banco do Brasil",
  "033": "Banco Santander",
  "104": "Caixa Econômica Federal",
  "237": "Banco Bradesco",
  "260": "Nubank (Nu Pagamentos)",
  "341": "Banco Itaú",
  ...
}
```

---

### 4. **Auto-complete no Frontend**

```typescript
// Frontend pode buscar lista de bancos
const banks = await fetch('/api/banks').then(r => r.json());

// Renderizar dropdown
<select name="bankCode">
  {Object.entries(banks).map(([code, name]) => (
    <option value={code}>{code} - {name}</option>
  ))}
</select>
```

---

## 🏦 Bancos Cadastrados

### Bancos Tradicionais (11)
- `001` - Banco do Brasil
- `033` - Banco Santander
- `104` - Caixa Econômica Federal
- `237` - Banco Bradesco
- `341` - Banco Itaú
- `422` - Banco Safra
- `745` - Banco Citibank
- `399` - HSBC Bank Brasil
- `389` - Banco Mercantil do Brasil
- `756` - Banco Cooperativo do Brasil (Bancoob)
- `748` - Banco Cooperativo Sicredi

### Bancos Digitais / Fintechs (15)
- `260` - Nubank (Nu Pagamentos) ⭐
- `077` - Banco Inter ⭐
- `290` - PagSeguro (PagBank) ⭐
- `323` - Mercado Pago ⭐
- `380` - PicPay ⭐
- `403` - Cora Sociedade de Crédito Direto
- `197` - Stone Pagamentos
- `084` - Uniprime Norte do Paraná
- `329` - QI Sociedade de Crédito Direto
- `364` - Gerencianet Pagamentos do Brasil
- `102` - XP Investimentos
- `348` - Banco XP
- `654` - Banco Digimais
- `655` - Banco Votorantim
- `136` - Unicred Cooperativa

### Bancos de Investimento (7)
- `208` - Banco BTG Pactual ⭐
- `069` - Banco Crefisa
- `021` - Banco Banestes
- `047` - Banco do Estado de Sergipe (Banese)
- `041` - Banco do Estado do Rio Grande do Sul (Banrisul)
- `070` - Banco de Brasília (BRB)
- `085` - Cooperativa Central de Crédito (Ailos)

### Outros Bancos Relevantes (17)
- `212` - Banco Original
- `336` - Banco C6
- `652` - Itaú Unibanco Holding
- `623` - Banco PAN
- `612` - Banco Guanabara
- `604` - Banco Industrial do Brasil
- `630` - Banco Smartbank
- `637` - Banco Sofisa
- `643` - Banco Pine
- `633` - Banco Rendimento
- `376` - Banco J.P. Morgan
- `394` - Banco Bradesco Financiamentos
- `464` - Banco Sumitomo Mitsui Brasileiro
- `479` - Banco ItauBank
- `613` - Omni Banco
- `739` - Banco Cetelem
- `741` - Banco Ribeirão Preto

**Total**: 50 bancos cadastrados

---

## 🧪 Testes

### Teste de Validação

```java
@Test
void testValidBankCodes() {
    assertTrue(BrazilianBanks.isValid("260")); // Nubank
    assertTrue(BrazilianBanks.isValid("341")); // Itaú
    assertTrue(BrazilianBanks.isValid("001")); // BB
}

@Test
void testInvalidBankCodes() {
    assertFalse(BrazilianBanks.isValid("999")); // Não existe
    assertFalse(BrazilianBanks.isValid("12"));  // Formato errado
    assertFalse(BrazilianBanks.isValid("ABC")); // Não numérico
}

@Test
void testBankNames() {
    assertEquals("Nubank (Nu Pagamentos)", 
                 BrazilianBanks.getBankName("260"));
    
    assertTrue(BrazilianBanks.getBankName("999")
              .contains("desconhecido"));
}
```

---

## 🔄 Adicionando Novos Bancos

Para adicionar um novo banco:

1. Edite `BrazilianBanks.java`
2. Adicione entrada no mapa:
   ```java
   banks.put("999", "Nome do Novo Banco");
   ```
3. Recompile e redeploy
4. ✅ Pronto! Validação automática já funciona

**Futuramente**: Migrar para tabela `banks` no DB para não precisar redeploy.

---

## 🚀 Próximos Passos

### Fase 2: Migrar para Tabela no DB

```sql
CREATE TABLE banks (
    code VARCHAR(3) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT now()
);

ALTER TABLE bank_accounts 
ADD CONSTRAINT fk_bank_code 
FOREIGN KEY (bank_code) REFERENCES banks(code);
```

**Benefícios**:
- ✅ Admin pode adicionar bancos sem redeploy
- ✅ FK garante integridade referencial
- ✅ Endpoint GET /api/banks vira CRUD completo

---

## 📝 Resumo

| Item | Status | Descrição |
|------|--------|-----------|
| **BrazilianBanks** | ✅ | Classe com 50+ bancos cadastrados |
| **ValidBankCode** | ✅ | Validador customizado |
| **BankAccount** | ✅ | Usando @ValidBankCode |
| **Compilação** | ✅ | BUILD SUCCESSFUL |
| **Métodos Utilitários** | ✅ | 7 métodos públicos |
| **Constantes** | ✅ | 14 bancos comuns |
| **Documentação** | ✅ | Este arquivo |
| **Testes** | ⏳ | Próximo passo |
| **Endpoint /api/banks** | ⏳ | Próximo passo |
| **Migração para DB** | 🔮 | Futuro |

---

✅ **Sistema de validação de bancos implementado com sucesso!**
