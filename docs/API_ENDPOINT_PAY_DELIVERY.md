# API Endpoint: Pagamento Automático de Delivery

## Visão Geral

Endpoint inteligente que processa pagamento de uma delivery detectando automaticamente a preferência de pagamento do cliente (PIX ou Cartão de Crédito).

---

## Endpoint

### POST `/api/payments/pay-delivery/{deliveryId}`

Processa o pagamento de uma delivery específica usando a preferência configurada pelo cliente.

**Autenticação**: Requerida (JWT Token)

---

## Parâmetros

### Path Parameters

| Parâmetro | Tipo | Obrigatório | Descrição |
|-----------|------|-------------|-----------|
| `deliveryId` | UUID | Sim | ID da delivery a ser paga |

---

## Comportamento

### Detecção Automática de Método

O endpoint consulta a preferência de pagamento do cliente e executa o fluxo apropriado:

1. **PIX**: Gera QR Code via Pagar.me
   - Validade: 2 horas
   - Status inicial: `PENDING`
   - Retorna QR Code e Pix Copy-Paste

2. **Cartão de Crédito**: Processa cobrança imediata
   - Usa cartão padrão tokenizado
   - Operação: `auth_and_capture`
   - Status pode ser `PAID` ou `PENDING`

### Status Válidos para Pagamento

O pagamento pode ser realizado quando a delivery está em um dos seguintes status:
- **ACCEPTED**: Motoboy aceitou a entrega
- **IN_TRANSIT**: Motoboy está executando a entrega
- **COMPLETED**: Entrega finalizada

❌ **Não é possível pagar** entregas com status:
- `PENDING` - Aguardando motoboy aceitar (pagamento só liberado após aceitação)
- `CANCELLED` - Entrega cancelada

### Split Automático

Divisão de valores aplicada automaticamente:
- **87%**: Courier (motoboy)
- **5%**: Organizer (se existir)
- **8%**: Plataforma (liable, paga taxas)

---

## Resposta de Sucesso

### HTTP 200 OK

#### Pagamento PIX

```json
{
  "paymentId": "550e8400-e29b-41d4-a716-446655440000",
  "orderId": "or_abc123xyz789",
  "status": "PENDING",
  "amount": 29.90,
  "pixQrCode": "00020101021226820014br.gov.bcb.pix...",
  "pixQrCodeUrl": "https://api.pagar.me/core/v5/transactions/...",
  "expiresAt": "2026-02-12T16:30:00",
  "paymentMethod": "PIX",
  "deliveries": [
    {
      "deliveryId": "550e8400-e29b-41d4-a716-446655440001",
      "description": "Entrega #550e8400-e29b-41d4-a716-446655440001",
      "amount": 29.90
    }
  ],
  "createdAt": "2026-02-12T14:30:00"
}
```

#### Pagamento com Cartão

```json
{
  "paymentId": "550e8400-e29b-41d4-a716-446655440002",
  "orderId": "or_def456uvw012",
  "status": "PAID",
  "amount": 29.90,
  "paymentMethod": "CREDIT_CARD",
  "cardLastFour": "4242",
  "cardBrand": "Visa",
  "deliveries": [
    {
      "deliveryId": "550e8400-e29b-41d4-a716-446655440001",
      "description": "Entrega #550e8400-e29b-41d4-a716-446655440001",
      "amount": 29.90
    }
  ],
  "createdAt": "2026-02-12T14:30:00",
  "paidAt": "2026-02-12T14:30:02"
}
```

---

## Respostas de Erro

### HTTP 404 Not Found

```json
{
  "timestamp": "2026-02-12T14:30:00",
  "status": 404,
  "error": "Not Found",
  "message": "Delivery não encontrada",
  "path": "/api/payments/pay-delivery/550e8400-e29b-41d4-a716-446655440099"
}
```

### HTTP 400 Bad Request

#### Cliente sem preferência configurada

```json
{
  "timestamp": "2026-02-12T14:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Cliente não possui preferência de pagamento configurada. Configure em Configurações > Pagamentos.",
  "path": "/api/payments/pay-delivery/550e8400-e29b-41d4-a716-446655440001"
}
```

#### Cliente optou por cartão mas não tem cartão cadastrado

```json
{
  "timestamp": "2026-02-12T14:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Cliente não possui cartão padrão cadastrado. Configure um cartão em suas preferências.",
  "path": "/api/payments/pay-delivery/550e8400-e29b-41d4-a716-446655440001"
}
```

#### Cartão padrão está inativo

```json
{
  "timestamp": "2026-02-12T14:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Cartão padrão está inativo. Por favor, ative-o ou selecione outro cartão.",
  "path": "/api/payments/pay-delivery/550e8400-e29b-41d4-a716-446655440001"
}
```

#### Motoboy sem conta Pagar.me

```json
{
  "timestamp": "2026-02-12T14:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Motoboy não possui conta Pagar.me configurada",
  "path": "/api/payments/pay-delivery/550e8400-e29b-41d4-a716-446655440001"
}
```

#### Status da delivery não permite pagamento

```json
{
  "timestamp": "2026-02-12T14:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Delivery não pode ser paga no status atual: PENDING. Status permitidos: ACCEPTED (após motoboy aceitar), IN_TRANSIT, COMPLETED",
  "path": "/api/payments/pay-delivery/550e8400-e29b-41d4-a716-446655440001"
}
```

### HTTP 409 Conflict

#### Delivery já possui pagamento

```json
{
  "timestamp": "2026-02-12T14:30:00",
  "status": 409,
  "error": "Conflict",
  "message": "Delivery já possui pagamento ativo/processando",
  "path": "/api/payments/pay-delivery/550e8400-e29b-41d4-a716-446655440001"
}
```

### HTTP 500 Internal Server Error

```json
{
  "timestamp": "2026-02-12T14:30:00",
  "status": 500,
  "error": "Internal Server Error",
  "message": "Erro ao processar pagamento: Connection timeout",
  "path": "/api/payments/pay-delivery/550e8400-e29b-41d4-a716-446655440001"
}
```

---

## Fluxo de Negócio

### 1. Validações Iniciais

- ✅ Delivery existe?
- ✅ Usuário autenticado é o cliente da delivery?
- ✅ Delivery está em status válido para pagamento? (ACCEPTED, IN_TRANSIT, COMPLETED)
- ✅ Cliente possui preferência configurada?
- ✅ Delivery ainda não possui pagamento ativo?
- ✅ Courier possui conta Pagar.me?

### 2. Branch por Preferência

#### Se preferência = PIX

1. Cria order PIX no Pagar.me
2. Gera QR Code com validade de 2h
3. Salva Payment com status `PENDING`
4. Retorna QR Code e Pix Copy-Paste

#### Se preferência = CREDIT_CARD

1. Valida que possui cartão padrão ativo
2. Cria order com cartão tokenizado no Pagar.me
3. Executa `auth_and_capture` (cobrança imediata)
4. Salva Payment com status baseado na resposta
5. Se `paid`: marca delivery como `paymentCaptured=true` e `paymentCompleted=true`
6. Retorna informações do pagamento (últimos 4 dígitos, bandeira)

### 3. Persistência

Todas as transações geram um registro `Payment` com:
- Provider Payment ID (Pagar.me Order ID)
- Request/Response JSON completo
- Status inicial
- Relacionamento N:M com Delivery
- Timestamps de criação e pagamento

---

## Status de Pagamento

| Status | Descrição |
|--------|-----------|
| `PENDING` | Aguardando confirmação (PIX não pago, ou cartão em processamento) |
| `PAID` | Pagamento confirmado e capturado |
| `FAILED` | Pagamento falhou (cartão recusado, erro gateway) |
| `EXPIRED` | PIX expirou sem pagamento |
| `UNPAID` | Nenhum pagamento criado ainda |

---

## Exemplos de Requisição

### cURL

```bash
curl -X POST 'https://api.mvt-events.com/api/payments/pay-delivery/550e8400-e29b-41d4-a716-446655440001' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...' \
  -H 'Content-Type: application/json'
```

### JavaScript (Fetch)

```javascript
const response = await fetch(
  `https://api.mvt-events.com/api/payments/pay-delivery/${deliveryId}`,
  {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json'
    }
  }
);

const payment = await response.json();

if (response.ok) {
  if (payment.paymentMethod === 'PIX') {
    // Mostrar QR Code
    console.log('QR Code:', payment.pixQrCode);
    console.log('Expira em:', payment.expiresAt);
  } else if (payment.paymentMethod === 'CREDIT_CARD') {
    // Mostrar confirmação
    console.log('Cartão:', payment.cardBrand, '****' + payment.cardLastFour);
    console.log('Status:', payment.status);
  }
}
```

---

## Dependências

### Entidades JPA

- `Payment`: Registro de pagamento
- `Delivery`: Entrega a ser paga
- `CustomerPaymentPreference`: Preferência do cliente (PIX/CARD)
- `CustomerCard`: Cartões tokenizados do cliente
- `SiteConfiguration`: Configurações de split

### Serviços

- `PaymentService`: Orquestra o fluxo de pagamento
- `PagarMeService`: Integração com gateway Pagar.me
- `CustomerPaymentPreferenceService`: Consulta preferências
- `PaymentSplitCalculator`: Calcula divisão de valores

---

## Notas Importantes

### Segurança

- ✅ Endpoint protegido por JWT
- ✅ Validação que usuário é dono da delivery
- ✅ Cartões armazenados apenas como tokens no Pagar.me
- ✅ Request/Response completos salvos para auditoria

### Performance

- ⚡ Transação única para criar Payment + Order Pagar.me
- ⚡ Validações fail-fast antes de chamar gateway
- ⚡ Sem múltiplas queries desnecessárias

### Idempotência

- ⚠️ Endpoint **NÃO é idempotente** por segurança
- ⚠️ Chamadas duplicadas resultam em HTTP 409 se já existe payment ativo
- ✅ Clientes devem implementar debounce/loading state

### Webhooks

- O status do pagamento PIX é atualizado via webhook do Pagar.me
- Endpoint: `POST /api/payments/webhook`
- Cartão geralmente já retorna status final (`paid` ou `failed`)

---

## Integração com Lista de Deliveries

### Endpoint: GET `/api/deliveries`

Retorna campo consolidado `paymentStatus` para cada delivery:

```json
{
  "deliveryId": "550e8400-e29b-41d4-a716-446655440001",
  "shippingFee": 29.90,
  "status": "DELIVERED",
  "paymentStatus": "PAID",
  "courierName": "João Silva",
  "pickup": {...},
  "delivery": {...},
  "createdAt": "2026-02-12T10:00:00"
}
```

**Valores possíveis de `paymentStatus`**:
- `"PAID"`: Todos os payments da delivery estão pagos
- `"PENDING"`: Existe payment pendente (PIX aguardando ou cartão processando)
- `"UNPAID"`: Nenhum payment criado ainda
- `"EXPIRED"`: PIX expirou sem pagamento
- `"FAILED"`: Pagamento falhou

---

## Changelog

| Data | Versão | Alteração |
|------|--------|-----------|
| 2026-02-12 | 1.0.2 | 🔧 Corrigida regra de negócio: removido PENDING dos status válidos (só pode pagar após motoboy aceitar) |
| 2026-02-12 | 1.0.1 | 🐛 Corrigido erro "Transaction rollback-only" removendo @Transactional do controller |
| 2026-02-12 | 1.0.0 | Versão inicial do endpoint |
