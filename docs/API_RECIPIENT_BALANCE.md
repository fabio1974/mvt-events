# Endpoint — Saldo do Recebedor (Courier / Organizer)

Retorna o saldo do usuário logado diretamente da Pagar.me.

---

## Endpoint

```
GET /api/payments/my-balance
Authorization: Bearer <token>
```

**Roles permitidas:** `COURIER`, `ORGANIZER`, `ADMIN`

---

## Resposta de sucesso — `200 OK`

```json
{
  "recipientId": "re_abc123xyz",

  "available": {
    "amount": 1250
  },
  "waiting_funds": {
    "amount": 500
  },
  "transferred": {
    "amount": 20000
  },

  "availableBrl": 12.50,
  "waitingFundsBrl": 5.00,
  "transferredBrl": 200.00
}
```

### Campos

| Campo | Tipo | Descrição |
|---|---|---|
| `recipientId` | string | ID do recebedor na Pagar.me (ex: `re_abc123`) |
| `available.amount` | integer | Saldo disponível para saque, **em centavos** |
| `waiting_funds.amount` | integer | Valores a receber (pendentes de processamento), **em centavos** |
| `transferred.amount` | integer | Total já sacado/transferido, **em centavos** |
| `availableBrl` | decimal | Saldo disponível em **Reais** (ex: `12.50`) |
| `waitingFundsBrl` | decimal | Valores a receber em **Reais** |
| `transferredBrl` | decimal | Total transferido em **Reais** |

> Os campos `*Brl` são calculados pelo backend para facilitar exibição direta — basta formatar como moeda.

---

## Erros

### `422 Unprocessable Entity` — sem conta cadastrada na Pagar.me

Ocorre quando o usuário ainda não tem recebedor criado na Pagar.me (ex: courier sem conta bancária cadastrada).

```json
{
  "error": "NO_RECIPIENT",
  "message": "Usuário não possui conta de recebedor cadastrada no Pagar.me"
}
```

**Sugestão de UX:** exibir mensagem solicitando que o usuário cadastre sua conta bancária.

---

### `502 Bad Gateway` — erro na Pagar.me

```json
{
  "error": "PAGARME_ERROR",
  "message": "Erro ao consultar saldo na Pagar.me: ..."
}
```

**Sugestão de UX:** exibir mensagem genérica de "serviço indisponível, tente novamente".

---

## Exemplo de uso (Flutter/Dart)

```dart
final response = await dio.get(
  '/api/payments/my-balance',
  options: Options(headers: {'Authorization': 'Bearer $token'}),
);

final availableBrl = response.data['availableBrl']; // ex: 12.50
final waitingBrl   = response.data['waitingFundsBrl'];
```

## Exemplo de uso (React/JS)

```js
const res = await api.get('/api/payments/my-balance');
const { availableBrl, waitingFundsBrl, transferredBrl } = res.data;
```

---

## Sugestão de layout

```
┌────────────────────────────────┐
│  Seu saldo                     │
├────────────────────────────────┤
│  Disponível para saque         │
│  R$ 12,50                      │
│                                │
│  A receber                     │
│  R$ 5,00                       │
│                                │
│  Total transferido             │
│  R$ 200,00                     │
└────────────────────────────────┘
```
