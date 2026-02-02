# 💳 Sistema de Cartões Tokenizados - Guia Completo (Integrado com Pagar.me API)

## 🔐 **Segurança (PCI Compliance)**

Este sistema segue os padrões de segurança PCI-DSS:

✅ **NUNCA** armazena número completo do cartão  
✅ **NUNCA** armazena CVV  
✅ Armazena apenas: ID do cartão Pagar.me + últimos 4 dígitos + bandeira  
✅ Tokenização feita no frontend (Pagar.me JS)  
✅ Cartões gerenciados via API Pagar.me  
✅ Comunicação via HTTPS obrigatório  

---

## 📋 **Estrutura Criada**

### 1. **Entity: CustomerCard**
- ✅ Relacionamento N:1 com User (customer)
- ✅ ID do cartão Pagar.me (card_xxxxx)
- ✅ Últimos 4 dígitos + bandeira + validade
- ✅ Flag is_default (cartão padrão)
- ✅ Soft delete (mantém histórico)
- ✅ Verificação de expiração automática

### 2. **Migrations**
- ✅ V45: Tabela `customer_cards` criada
- ✅ V46: Campo `pagarme_customer_id` adicionado em `users`

### 3. **PagarMeService (métodos novos)**
- ✅ `createCustomer()` - Cria customer no Pagar.me
- ✅ `getCustomer()` - Busca customer
- ✅ `createCard()` - Cria cartão a partir de token
- ✅ `listCustomerCards()` - Lista cartões
- ✅ `getCard()` - Busca cartão específico
- ✅ `deleteCard()` - Deleta cartão

### 4. **CustomerCardService**
- ✅ `addCard()` - Cria customer se necessário + adiciona cartão
- ✅ Integração completa com API Pagar.me
- ✅ Gerenciamento automático de cartão padrão

---

## 🎯 **API Endpoints**

### **POST /api/customer-cards**
Adiciona novo cartão (token gerado no frontend).

**Request:**
```json
{
  "cardToken": "tok_abc123xyz",
  "setAsDefault": true
}
```

**Response:**
```json
{
  "id": 1,
  "lastFourDigits": "4242",
  "brand": "Visa",
  "holderName": "JOAO DA SILVA",
  "expiration": "12/26",
  "isDefault": true,
  "isActive": true,
  "isExpired": false,
  "maskedNumber": "Visa **** 4242",
  "createdAt": "2026-02-02T10:30:00",
  "lastUsedAt": null
}
```

---

### **GET /api/customer-cards**
Lista todos os cartões do cliente.

---

### **GET /api/customer-cards/default**
Retorna o cartão padrão do cliente.

---

### **PUT /api/customer-cards/{cardId}/set-default**
Define um cartão como padrão.

---

### **DELETE /api/customer-cards/{cardId}**
Remove um cartão (soft delete local + delete na API Pagar.me).

---

### **GET /api/customer-cards/has-cards**
Verifica se cliente tem cartões cadastrados.

---

## 🎨 **Fluxo Frontend (React Native)**

### **1. Adicionar Cartão**

```javascript
// IMPORTANTE: Criar token usando Pagar.me JS SDK
// https://docs.pagar.me/reference/criacao-de-token-usando-biblioteca-javascript

// 1. Incluir SDK do Pagar.me
<script src="https://api.pagar.me/core/v5/js"></script>

// 2. Criar token
const pagarme = window.PagarMe;

const cardData = {
  number: '4242424242424242',
  holder_name: 'JOAO DA SILVA',
  exp_month: 12,
  exp_year: 2026,
  cvv: '123'
};

// Tokenizar com chave pública
const token = await pagarme.client.encrypt({
  type: 'card',
  ...cardData
}, 'YOUR_PUBLIC_KEY');

// 3. Enviar token para backend
const response = await fetch('http://api/customer-cards', {
  method: 'POST',
  headers: {
    'Authorization': `Bearer ${jwtToken}`,
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({
    cardToken: token,
    setAsDefault: true
  })
});

const card = await response.json();
console.log('Cartão adicionado:', card.maskedNumber);
```

---

### **2. Listar Cartões**

```javascript
const cards = await fetch('http://api/customer-cards', {
  headers: { 'Authorization': `Bearer ${jwtToken}` }
}).then(res => res.json());

// Renderizar
cards.map(card => (
  <View key={card.id}>
    <Text>{card.maskedNumber}</Text>
    <Text>Validade: {card.expiration}</Text>
    {card.isDefault && <Badge>Padrão</Badge>}
    {card.isExpired && <Badge color="red">Expirado</Badge>}
  </View>
));
```

---

## 🔄 **Fluxo Completo (Backend)**

### **1. Cliente adiciona cartão**

```
Frontend                  Backend                    Pagar.me API
   |                         |                            |
   |-- Token do cartão -->   |                            |
   |                         |                            |
   |                         |-- GET /customers/{id} -->  |
   |                         |<-- 404 Not Found --------  |
   |                         |                            |
   |                         |-- POST /customers ------->  |
   |                         |<-- cus_xxxxx -------------  |
   |                         |                            |
   |                         | (salva pagarmeCustomerId)  |
   |                         |                            |
   |                         |-- POST /customers/{id}/cards ->|
   |                         |<-- card_xxxxx + dados --------|
   |                         |                            |
   |                         | (salva CustomerCard)       |
   |                         |                            |
   |<-- CardResponse ------  |                            |
```

### **2. Cliente usa cartão em pagamento**

```java
// No PaymentService
CustomerCard card = cardService.getDefaultCard(customerId);
User customer = userRepository.findById(customerId).orElseThrow();

// Criar order no Pagar.me
OrderRequest orderRequest = new OrderRequest();
orderRequest.setCustomer(CustomerRequest.builder()
    .id(customer.getPagarmeCustomerId())
    .build());

orderRequest.setPayments(List.of(
    PaymentRequest.builder()
        .paymentMethod("credit_card")
        .cardId(card.getPagarmeCardId()) // card_xxxxx
        .amount(amount)
        .installments(1)
        .build()
));

String orderId = pagarMeService.createOrder(orderRequest);

// Marcar cartão como usado
cardService.markCardAsUsed(card.getPagarmeCardId());
```

---

## 📊 **Banco de Dados**

### Tabela `users` (campo novo):

| Campo | Tipo | Descrição |
|-------|------|-----------|
| pagarme_customer_id | VARCHAR(100) | ID customer Pagar.me (cus_xxxxx) |
| pagarme_recipient_id | VARCHAR(100) | ID recipient Pagar.me (re_xxxxx) |

- **customer_id** → Para CUSTOMER/CLIENT pagar
- **recipient_id** → Para COURIER receber

### Tabela `customer_cards`:

| Campo | Tipo | Descrição |
|-------|------|-----------|
| id | BIGSERIAL | PK |
| customer_id | UUID | FK → users |
| pagarme_card_id | VARCHAR(100) | ID cartão Pagar.me (card_xxxxx) |
| last_four_digits | VARCHAR(4) | Últimos 4 dígitos |
| brand | VARCHAR(20) | Bandeira |
| holder_name | VARCHAR(100) | Nome titular |
| exp_month | INTEGER | Mês expiração |
| exp_year | INTEGER | Ano expiração |
| is_default | BOOLEAN | Cartão padrão |
| is_active | BOOLEAN | Ativo |
| deleted_at | TIMESTAMP | Soft delete |

---

## ✅ **O que mudou da versão anterior**

### ❌ **Antes (incorreto)**
- Frontend enviava: token + lastFourDigits + brand + holderName + expMonth + expYear
- Backend apenas armazenava no banco
- Não criava cartão na API Pagar.me
- Não criava customer no Pagar.me

### ✅ **Agora (correto)**
- Frontend envia apenas: token
- Backend cria customer no Pagar.me (se não existir)
- Backend cria cartão na API Pagar.me
- Backend extrai dados do cartão da resposta da API
- Backend armazena no banco local para consulta rápida

---

## 🔑 **Vantagens da Integração com API Pagar.me**

1. ✅ **Segurança**: Cartões gerenciados pelo Pagar.me
2. ✅ **Validação**: Pagar.me valida dados do cartão
3. ✅ **Sincronização**: Sempre em sync com Pagar.me
4. ✅ **Facilidade**: Pagamentos usam `card_id` direto
5. ✅ **Auditoria**: Pagar.me mantém histórico completo

---

## 📞 **Próximos Passos**

1. ✅ Backend pronto e integrado
2. 🔲 Frontend: Implementar tokenização Pagar.me JS
3. 🔲 Frontend: Tela "Meus Cartões"
4. 🔲 Backend: Integrar com fluxo de pagamento
5. 🔲 Testes end-to-end com sandbox Pagar.me

---

## 🔒 **Segurança - Checklist Final**

- [x] NUNCA armazena número completo
- [x] NUNCA armazena CVV
- [x] Tokenização no frontend
- [x] Cartões gerenciados via API Pagar.me
- [x] HTTPS obrigatório (produção)
- [x] Customer criado automaticamente
- [x] Soft delete (auditoria)
- [x] Validação de propriedade do cartão
