# 🔄 API - Retry Pagamento de Entregas Não Pagas

## Endpoint

```
POST /api/customer-cards/retry-unpaid-deliveries
```

**Autenticação:** Bearer Token (JWT do customer logado)

---

## O que faz?

Busca **todas** as entregas do cliente logado que estão com status `IN_TRANSIT` ou `COMPLETED` e que **ainda não foram pagas**, e cria automaticamente um pagamento para cada uma usando o **cartão padrão** atual do cliente.

### Regras:
- ✅ Só processa entregas com status `IN_TRANSIT` ou `COMPLETED`
- ✅ Ignora entregas que já possuem pagamento `PENDING` ou `PAID` (evita duplicatas)
- ✅ Valida se o cartão padrão está ativo e não expirado
- ✅ Cria pagamento individual para cada entrega via Pagar.me (com split courier/organizer/plataforma)
- ❌ Se não tem cartão padrão cadastrado, retorna erro

---

## Request

```http
POST /api/customer-cards/retry-unpaid-deliveries
Authorization: Bearer <token_jwt_do_customer>
Content-Type: application/json
```

**Não precisa enviar body.** O endpoint identifica o customer pelo token JWT.

---

## Responses

### ✅ 200 - Pagamentos criados com sucesso

```json
{
  "message": "Pagamento criado para 3 entrega(s) com cartão **** 4242",
  "total": 3,
  "success": 3,
  "failed": 0,
  "card": {
    "lastFourDigits": "4242",
    "brand": "Visa"
  },
  "details": [
    {
      "deliveryId": "101",
      "status": "success",
      "amount": "15.00"
    },
    {
      "deliveryId": "102",
      "status": "success",
      "amount": "22.50"
    },
    {
      "deliveryId": "103",
      "status": "success",
      "amount": "18.00"
    }
  ]
}
```

### ✅ 200 - Nenhuma entrega pendente

```json
{
  "message": "Nenhuma entrega pendente de pagamento",
  "total": 0,
  "success": 0,
  "failed": 0
}
```

### ✅ 200 - Todas já têm pagamento em processamento

```json
{
  "message": "Todas as entregas já possuem pagamento em processamento",
  "total": 2,
  "success": 0,
  "failed": 0,
  "skipped": 2
}
```

### ✅ 200 - Sucesso parcial (algumas falharam)

```json
{
  "message": "Pagamento criado para 2 entrega(s) com cartão **** 4242",
  "total": 3,
  "success": 2,
  "failed": 1,
  "card": {
    "lastFourDigits": "4242",
    "brand": "Visa"
  },
  "details": [
    {
      "deliveryId": "101",
      "status": "success",
      "amount": "15.00"
    },
    {
      "deliveryId": "102",
      "status": "success",
      "amount": "22.50"
    },
    {
      "deliveryId": "103",
      "status": "failed",
      "error": "Courier sem recipientId, pulando"
    }
  ]
}
```

### ❌ 400 - Sem cartão padrão

```json
{
  "error": "Nenhum cartão padrão cadastrado. Cadastre um cartão primeiro."
}
```

### ❌ 400 - Cartão inativo

```json
{
  "error": "Cartão padrão está inativo. Defina outro cartão como padrão."
}
```

### ❌ 400 - Cartão expirado

```json
{
  "error": "Cartão padrão está expirado. Defina outro cartão como padrão."
}
```

---

## Campos da Response

| Campo | Tipo | Descrição |
|-------|------|-----------|
| `message` | `string` | Mensagem legível para exibir ao usuário |
| `total` | `number` | Total de entregas processadas |
| `success` | `number` | Quantas tiveram pagamento criado com sucesso |
| `failed` | `number` | Quantas falharam |
| `skipped` | `number` | Quantas foram puladas (já tinham pagamento) |
| `card` | `object` | Info do cartão usado (só quando `success > 0`) |
| `card.lastFourDigits` | `string` | Últimos 4 dígitos do cartão |
| `card.brand` | `string` | Bandeira (Visa, Mastercard, Elo, etc.) |
| `details` | `array` | Detalhes por entrega processada |
| `details[].deliveryId` | `string` | ID da entrega |
| `details[].status` | `string` | `"success"` ou `"failed"` |
| `details[].amount` | `string` | Valor cobrado (só em success) |
| `details[].error` | `string` | Motivo da falha (só em failed) |
| `error` | `string` | Mensagem de erro (só em 400) |

---

## Exemplo de uso no Mobile (React Native)

```typescript
async function retryUnpaidDeliveries(token: string) {
  try {
    const response = await fetch(`${API_URL}/api/customer-cards/retry-unpaid-deliveries`, {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json',
      },
    });

    const data = await response.json();

    if (!response.ok) {
      // 400 - Sem cartão, cartão inativo, etc.
      Alert.alert('Erro', data.error);
      return;
    }

    if (data.success > 0) {
      Alert.alert(
        'Pagamento processado! ✅',
        data.message
      );
    } else if (data.total === 0) {
      Alert.alert('Tudo certo!', 'Nenhuma entrega pendente de pagamento.');
    } else {
      Alert.alert('Info', data.message);
    }

    return data;
  } catch (error) {
    Alert.alert('Erro', 'Falha ao processar pagamentos. Tente novamente.');
  }
}
```

---

## Fluxo sugerido no app

```
1. Cliente abre tela de entregas / cartões
2. App detecta que existem entregas sem pagamento
   (pode usar os dados locais ou chamar o endpoint de listar entregas)
3. Exibe botão: "Pagar entregas pendentes"
4. Ao clicar → chama POST /api/customer-cards/retry-unpaid-deliveries
5. Exibe resultado:
   - success > 0 → "✅ Pagamentos criados!"
   - total == 0  → "Nenhuma entrega pendente"
   - failed > 0  → "Algumas entregas falharam, tente novamente"
   - error       → Redireciona para tela de cartões
```

---

## Observações

- O pagamento é criado como **PENDING** no Pagar.me. A confirmação vem via **webhook** e atualiza o status automaticamente.
- Cada entrega gera um pagamento **individual** (não consolidado).
- O split de pagamento é automático: **87% courier**, **5% organizer** (se houver), restante **plataforma**.
- Pode chamar o endpoint múltiplas vezes sem risco — entregas já pagas ou com pagamento pendente são ignoradas.
- Se uma entrega já tiver pagamento ativo (race condition), ela será marcada como `"status": "skipped"` nos detalhes.
- Cada entrega é processada em **transação independente** — se uma falhar, as outras não são afetadas.
- ⚠️ **Não precisa chamar `PUT /set-default` antes** — o endpoint já usa o cartão padrão atual automaticamente.
