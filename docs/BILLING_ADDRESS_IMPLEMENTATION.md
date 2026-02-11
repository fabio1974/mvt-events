# 💳 Implementação: Endereço de Cobrança para Cartões

## ✅ Mudanças Implementadas

### 1. Novo DTO: `BillingAddressDTO`

**Arquivo:** `src/main/java/com/mvt/mvt_events/payment/dto/BillingAddressDTO.java`

DTO criado para representar o endereço de cobrança no formato esperado pelo Pagar.me:

```java
@Data
public class BillingAddressDTO {
    @NotBlank @Size(max = 255)
    private String line1;  // "{numero}, {rua}, {bairro}"
    
    @Size(max = 255)
    private String line2;  // complemento (opcional)
    
    @NotBlank @Pattern(regexp = "\\d{8}")
    private String zipCode;  // CEP (8 dígitos)
    
    @NotBlank
    private String city;
    
    @NotBlank @Pattern(regexp = "[A-Z]{2}")
    private String state;  // UF (SP, RJ, etc)
    
    @NotBlank @Pattern(regexp = "[A-Z]{2}")
    private String country;  // BR
}
```

### 2. Controller: `CustomerCardController`

**Mudanças:**
- Adicionado import do `BillingAddressDTO`
- `AddCardRequest` agora inclui campo `billingAddress` (opcional)
- Método `addCard()` repassa o `billingAddress` para o service

```java
@Data
public static class AddCardRequest {
    @NotBlank
    private String cardToken;
    
    private Boolean setAsDefault = false;
    
    @Valid
    private BillingAddressDTO billingAddress;  // NOVO - opcional
}
```

### 3. Service: `CustomerCardService`

**Mudanças:**
- Assinatura do método `addCard()` atualizada para aceitar `BillingAddressDTO`
- Endereço é repassado ao `PagarMeService.createCard()`

```java
public CustomerCard addCard(
    UUID customerId, 
    String cardToken, 
    Boolean setAsDefault, 
    BillingAddressDTO billingAddress  // NOVO
) { ... }
```

### 4. Service: `PagarMeService`

**Mudanças:**
- Método `createCard()` atualizado para aceitar `BillingAddressDTO`
- Se fornecido, monta o objeto `billing_address` em **snake_case** conforme API do Pagar.me
- Envia no payload da requisição `POST /customers/{id}/cards`

```java
public Map<String, Object> createCard(
    String customerId, 
    String cardToken, 
    BillingAddressDTO billingAddress  // NOVO
) {
    Map<String, Object> cardData = new HashMap<>();
    cardData.put("token", cardToken);
    
    if (billingAddress != null) {
        Map<String, String> address = new HashMap<>();
        address.put("line_1", billingAddress.getLine1());
        if (billingAddress.getLine2() != null) {
            address.put("line_2", billingAddress.getLine2());
        }
        address.put("zip_code", billingAddress.getZipCode());
        address.put("city", billingAddress.getCity());
        address.put("state", billingAddress.getState());
        address.put("country", billingAddress.getCountry());
        
        cardData.put("billing_address", address);
    }
    // ...
}
```

---

## 📝 Exemplo de Request

### Sem endereço (comportamento anterior mantido):

```json
POST /api/customer-cards
{
  "cardToken": "tok_abc123xyz",
  "setAsDefault": true
}
```

### Com endereço de cobrança (novo):

```json
POST /api/customer-cards
{
  "cardToken": "tok_abc123xyz",
  "setAsDefault": true,
  "billingAddress": {
    "line1": "7221, Avenida Dra Ruth Cardoso, Pinheiros",
    "line2": "Apto 42",
    "zipCode": "01311000",
    "city": "São Paulo",
    "state": "SP",
    "country": "BR"
  }
}
```

---

## 🧪 Teste Manual com cURL

```bash
# Obter token JWT (substitua por credenciais válidas)
TOKEN="seu_jwt_token_aqui"

# Criar cartão COM billing address
curl -X POST http://localhost:8080/api/customer-cards \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "cardToken": "card_token_do_pagarme",
    "setAsDefault": true,
    "billingAddress": {
      "line1": "123, Rua Exemplo, Centro",
      "line2": "Sala 5",
      "zipCode": "01310100",
      "city": "São Paulo",
      "state": "SP",
      "country": "BR"
    }
  }'
```

---

## 🔍 Validações Implementadas

| Campo     | Validação                        | Obrigatório |
|-----------|----------------------------------|-------------|
| line1     | Max 255 caracteres               | ✅ Sim      |
| line2     | Max 255 caracteres               | ❌ Não      |
| zipCode   | Exatamente 8 dígitos numéricos   | ✅ Sim      |
| city      | Não vazio                        | ✅ Sim      |
| state     | 2 letras maiúsculas (UF)         | ✅ Sim      |
| country   | 2 letras maiúsculas              | ✅ Sim      |

---

## ⚠️ Observações Importantes

1. **Não é persistido no banco**: O endereço é apenas repassado ao Pagar.me (passthrough)
2. **Opcional**: Se o mobile não enviar `billingAddress`, funciona normalmente
3. **Retrocompatibilidade**: Requests antigos sem `billingAddress` continuam funcionando
4. **Snake_case no Pagar.me**: Internamente convertemos `line1` → `line_1`, `zipCode` → `zip_code`, etc.
5. **Logs**: Quando um endereço é fornecido, aparece no log: `📍 Billing address incluído`

---

## 🎯 Status

✅ **Implementação completa e funcional**
- DTO criado e validado
- Controller atualizado
- Service layers atualizados
- Mantém retrocompatibilidade
- Deleção sincronizada com Pagar.me
- Aplicação rodando sem erros

---

## 🗑️ Deleção de Cartão Sincronizada

### Fluxo de Deleção

Ao deletar um cartão via `DELETE /api/customer-cards/{cardId}`, o backend executa:

1. **Valida autorização** do cliente
2. **Deleta no Pagar.me PRIMEIRO** via API
3. **Se sucesso**, faz soft delete localmente
4. **Se falhar no Pagar.me**, aborta a exclusão local (rollback automático)

### Estratégia Implementada

```java
@Transactional
public void deleteCard(...) {
    // 1. Buscar cartão local
    CustomerCard card = cardRepository.findByIdAndCustomerId(...)
    
    // 2. DELETAR NO PAGAR.ME PRIMEIRO
    //    Se falhar, exceção impede exclusão local
    pagarMeService.deleteCard(customer.getPagarmeCustomerId(), card.getPagarmeCardId());
    
    // 3. Se chegou aqui, deletar localmente (soft delete)
    card.softDelete();
    cardRepository.save(card);
}
```

### Por que essa ordem?

| Cenário | Resultado |
|---------|-----------|
| ✅ Pagar.me OK → DB OK | Cartão deletado em ambos (ideal) |
| ❌ Pagar.me falha | Nada deletado, erro retornado (consistente) |
| ✅ Pagar.me OK → ❌ DB falha | Rollback local, cartão fica deletado apenas no Pagar.me (cenário raro, menos problemático) |

**Observações:**
- Cartão que existe apenas no Pagar.me não consegue ser usado em cobranças (seguro)
- Inverso (deletar DB primeiro) seria pior: cartão sumiria do app mas ficaria ativo no gateway

### Logs

Ao deletar um cartão, os seguintes logs são gerados:

```
🗑️ Deletando cartão: card_xyz do customer: cus_abc
   └─ ✅ Cartão deletado no Pagar.me: card_xyz
Cartão 123 deletado (soft) para customer uuid-xxx
```

Se falhar no Pagar.me:
```
🗑️ Deletando cartão: card_xyz do customer: cus_abc
   └─ ❌ Falha ao deletar no Pagar.me, abortando exclusão local
```

---

## 📚 Referências

- [Documentação Pagar.me - Create Card](https://docs.pagar.me/reference/criar-cart%C3%A3o)
- Formato do billing_address: https://docs.pagar.me/docs/endereco
