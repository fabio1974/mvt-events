# Handshake — Pagamento PIX Customer (Backend ↔ Mobile)

> Documento de validação para alinhamento Backend/Mobile antes do teste.
> Data: 18 de fevereiro de 2026

---

## 1. Fluxo Resumido

```
CUSTOMER cria delivery → PENDING
    → Courier aceita (PATCH /accept)
    → BE verifica preferência do CUSTOMER
    │
    ├─ PIX → WAITING_PAYMENT (QR Code criado, 5 min)
    │    → Customer paga (descobre via polling)
    │    → Webhook order.paid → ACCEPTED
    │    → Fluxo normal segue
    │
    ├─ PIX expirou sem pagar (5 min)
    │    → Cron (30s) ou webhook charge.expired
    │    → Payment → EXPIRED
    │    → Delivery → PENDING (courier desassociado)
    │    → Push enviado para motoboys disponíveis
    │    → Outro courier pode aceitar
    │
    └─ Outro método → ACCEPTED (direto)
```

---

## 2. Endpoints Envolvidos

### 2.1. `PATCH /api/deliveries/{id}/accept`

**Mudança:** Se CUSTOMER + PIX → retorna status `WAITING_PAYMENT` ao invés de `ACCEPTED`.

**Response quando WAITING_PAYMENT:**
```json
{
  "id": 123,
  "status": "WAITING_PAYMENT",
  "courier": {
    "id": "uuid-do-courier",
    "name": "João Motoboy",
    "phone": "11999999999",
    "gpsLatitude": -23.55,
    "gpsLongitude": -46.63
  },
  "client": {
    "id": "uuid-do-customer",
    "name": "Maria",
    "phone": "11988888888"
  },
  "shippingFee": 15.00,
  "payments": [
    {
      "id": 456,
      "status": "PENDING",
      "paymentMethod": "PIX",
      "amount": 15.00,
      "pixQrCode": "00020126...(copia-e-cola PIX)...",
      "pixQrCodeUrl": "https://api.pagar.me/..../qrcode.png",
      "expiresAt": "2026-02-18T21:40:00"
    }
  ],
  "paymentStatus": "PENDING",
  ...
}
```

**Response quando não é PIX (fluxo normal):**
```json
{
  "id": 123,
  "status": "ACCEPTED",
  "payments": [],
  "paymentStatus": "UNPAID",
  ...
}
```

---

### 2.2. `GET /api/deliveries/{id}`

**Mudança:** Quando status for `WAITING_PAYMENT` ou `ACCEPTED`, retorna campo `payments` com dados PIX.

**Response é idêntica ao accept** — mesma estrutura, com `payments[]` populado.

O mobile faz polling neste endpoint a cada 5-10 segundos. Quando:
- `status` muda de `WAITING_PAYMENT` para `ACCEPTED` → pagamento confirmado.
- `status` muda de `WAITING_PAYMENT` para `PENDING` e `courier` é `null` → PIX expirou, delivery voltou pro pool.

---

### 2.3. `GET /api/payments/{id}/report`

**Retorna detalhes completos do pagamento** (usado para polling do status do pagamento).

```json
{
  "paymentId": 456,
  "providerPaymentId": "or_xxxxxxxxxx",
  "status": "PENDING",
  "totalAmount": 15.00,
  "currency": "BRL",
  "createdAt": "2026-02-18T21:35:00",
  "pixQrCode": "00020126...",
  "pixQrCodeUrl": "https://api.pagar.me/..../qrcode.png",
  "expiresAt": "2026-02-18T21:40:00",
  "deliveries": [...],
  "consolidatedSplits": [...]
}
```

**Polling:** Mobile faz GET a cada 10 segundos.  
**Transições esperadas de `status`:**
- `PENDING` → pagamento ainda não confirmado
- `PAID` → pagamento confirmado pelo webhook
- `EXPIRED` → QR Code expirou

---

## 3. Campos Críticos para o Mobile

| Campo | Tipo | Onde vem | Garantia |
|-------|------|----------|----------|
| `status` (delivery) | String | `GET /deliveries/{id}` | `WAITING_PAYMENT`, `ACCEPTED`, `PENDING`, etc. |
| `payments[0].pixQrCode` | String | `GET /deliveries/{id}` e `PATCH /accept` | Copia-e-cola do PIX. Presente quando `WAITING_PAYMENT`. |
| `payments[0].pixQrCodeUrl` | String | Idem | URL da imagem do QR Code. Presente quando `WAITING_PAYMENT`. |
| `payments[0].expiresAt` | DateTime | Idem | **NUNCA é null.** Formato: `2026-02-18T21:40:00` (timezone local BRT). |
| `payments[0].amount` | Decimal | Idem | Valor do frete (`shippingFee`). |
| `payments[0].status` | String | Idem | `PENDING`, `PAID`, ou `EXPIRED`. |
| `payments[0].id` | Long | Idem | ID para usar no endpoint `/payments/{id}/report`. |
| `paymentStatus` | String | Idem | Consolidado: `PENDING`, `PAID`, `UNPAID`, `EXPIRED`, `FAILED`. |
| `courier` | Object | Idem | Dados do courier quando `WAITING_PAYMENT`. `null` se PIX expirou e delivery voltou. |

---

## 4. Comportamentos do Mobile

### 4.1. Tela do Customer

| Trigger | Ação |
|---------|------|
| `status == WAITING_PAYMENT` + `courier != null` + `payments[0].pixQrCode` presente | Abrir modal PIX com QR Code |
| Timer countdown baseado em `payments[0].expiresAt` | Mostrar tempo restante |
| `status` muda para `ACCEPTED` (polling) | Fechar modal PIX, mostrar sucesso |
| `status` volta para `PENDING` + `courier == null` (polling) | PIX expirou, delivery voltou pro pool. Mostrar mensagem. |

### 4.2. Tela do Courier

| Trigger | Ação |
|---------|------|
| Accept retorna `status == WAITING_PAYMENT` | Mostrar banner "Aguardando Pagamento PIX" com countdown |
| Timer countdown baseado em `payments[0].expiresAt` | Mostrar tempo restante |
| `status` muda para `ACCEPTED` (polling) | Pagamento confirmado, habilitar botões |
| `status` volta para `PENDING` (nunca vai ver isso pois courier foi desassociado) | N/A — courier não está mais vinculado |

---

## 5. Expiração (5 minutos)

- **Quem cria:** Backend, ao criar o PIX no Pagar.me com `expires_in: 300` segundos.
- **`expiresAt`:** Data/hora local (BRT) de expiração do QR Code. **Nunca null.**
- **Detecção:** Dupla proteção:
  - **Cron job:** A cada 30s, busca payments PIX PENDING expirados de CUSTOMER.
  - **Webhook:** `charge.expired` / `order.canceled` da Pagar.me.
- **Ação (Opção B):** 
  1. Payment → `EXPIRED`
  2. Delivery → `PENDING` (courier desassociado)
  3. Push notification enviado para motoboys disponíveis (mesmo fluxo de delivery nova)
- **O que o mobile NÃO precisa fazer:** Não precisa chamar nenhum endpoint de expiração. O backend trata tudo. O mobile só precisa detectar a mudança de status via polling.

---

## 6. Checklist de Validação (Handshake)

### Backend (confirmado implementado ✅)

- [x] `PATCH /accept` retorna `WAITING_PAYMENT` com `payments[]` quando CUSTOMER + PIX
- [x] `GET /deliveries/{id}` retorna `payments[]` com dados PIX quando `WAITING_PAYMENT`/`ACCEPTED`
- [x] `payments[0].expiresAt` **nunca é null** (fallback: `now + 300s`)
- [x] `payments[0].pixQrCode` e `pixQrCodeUrl` preenchidos
- [x] Webhook `order.paid` → Payment `PAID` + Delivery `WAITING_PAYMENT → ACCEPTED`
- [x] Webhook `charge.expired` / `order.canceled` → Payment `EXPIRED` + Delivery `PENDING`
- [x] Cron job (30s) expira PIX de CUSTOMER com `expiresAt < now`
- [x] Ao expirar, push notification enviado para motoboys (mesmo fluxo de delivery nova)
- [x] `GET /payments/{id}/report` retorna status atualizado com dados PIX
- [x] Somente pagamentos de CUSTOMER sofrem Opção B (CLIENT ignora)

### Mobile (a confirmar)

- [ ] Detecta `WAITING_PAYMENT` no polling de `GET /deliveries/{id}`
- [ ] Usa `payments[0].pixQrCode` para exibir QR Code (copia-e-cola)
- [ ] Usa `payments[0].pixQrCodeUrl` para exibir imagem do QR
- [ ] Usa `payments[0].expiresAt` para countdown timer
- [ ] Usa `payments[0].id` para polling em `GET /payments/{id}/report`
- [ ] Detecta transição `WAITING_PAYMENT → ACCEPTED` (pagamento OK)
- [ ] Detecta transição `WAITING_PAYMENT → PENDING` com `courier == null` (PIX expirou)
- [ ] Courier mostra banner "Aguardando Pagamento" quando `WAITING_PAYMENT`
- [ ] Courier recebe push notification quando delivery volta para PENDING (novo aceite possível)

---

## 7. Teste Sugerido

1. **Setup:** Customer com preferência PIX no banco. Courier com push token ativo.
2. **Criar delivery:** POST via Customer → status `PENDING`.
3. **Courier aceita:** PATCH `/accept` → verificar response com `WAITING_PAYMENT` + `payments[]`.
4. **Polling delivery:** GET `/deliveries/{id}` a cada 5s → verificar `payments[0]` presente.
5. **Polling payment:** GET `/payments/{paymentId}/report` a cada 10s → status `PENDING`.
6. **Cenário A — Pagar:** Simular webhook `order.paid` → verificar delivery muda para `ACCEPTED`.
7. **Cenário B — Expirar:** Esperar 5 min (ou forçar expiresAt no banco para passado) → verificar delivery volta para `PENDING`, push chega para couriers.
