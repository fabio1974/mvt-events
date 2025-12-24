# API de Relatório de Pagamento - Documentação Frontend

## 📋 Visão Geral

Endpoint para gerar relatório detalhado de um pagamento consolidado, mostrando a composição completa com todas as deliveries incluídas e splits por recipient (courier, organizer, plataforma).

---

## 🔗 Endpoint

```
GET /api/payments/{id}/report
```

### Parâmetros

| Parâmetro | Tipo | Localização | Obrigatório | Descrição |
|-----------|------|-------------|-------------|-----------|
| `id` | `Long` | Path | ✅ Sim | ID do pagamento |

### Autenticação

✅ **Requer autenticação JWT**

### Permissões

Roles permitidas:
- `ADMIN`
- `COURIER`
- `ORGANIZER`
- `CLIENT`

---

## 📤 Exemplo de Request

```bash
GET /api/payments/69/report HTTP/1.1
Host: api.example.com
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

```bash
# cURL
curl -X GET "http://localhost:8080/api/payments/69/report" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

---

## 📥 Response

### Status Code: `200 OK`

### Response Body (JSON)

```json
{
  "paymentId": 69,
  "pagarmeOrderId": "or_3e5Ab0iaBUpgRy2r",
  "status": "FAILED",
  "totalAmount": 40.00,
  "currency": "BRL",
  "createdAt": "2025-12-23T15:40:57",
  "pixQrCode": "00020126580014br.gov.bcb.pix...",
  "pixQrCodeUrl": "https://api.pagar.me/core/v5/orders/or_3e5Ab0iaBUpgRy2r/qrcode",
  "expiresAt": "2025-12-23T17:40:57",
  "deliveries": [
    {
      "deliveryId": 8,
      "shippingFee": 22.00,
      "clientName": "Padaria Dom Pão",
      "pickupAddress": "Rua Conselheiro José Júlio, 892, Fortaleza - CE",
      "deliveryAddress": "Av. Santos Dumont, 1234, Fortaleza - CE",
      "splits": [
        {
          "recipientId": "re_cmj135cro00280l9t4fnpb66t",
          "recipientName": "João Silva",
          "recipientRole": "COURIER",
          "amount": 19.14,
          "percentage": 87.0,
          "liable": true
        },
        {
          "recipientId": "re_abc123def456",
          "recipientName": "Maria Santos",
          "recipientRole": "ORGANIZER",
          "amount": 1.10,
          "percentage": 5.0,
          "liable": false
        },
        {
          "recipientId": "re_cmjixi23o011w0k9tdigurq8t",
          "recipientName": "Plataforma Zapi10",
          "recipientRole": "PLATFORM",
          "amount": 1.76,
          "percentage": 8.0,
          "liable": false
        }
      ]
    },
    {
      "deliveryId": 12,
      "shippingFee": 18.00,
      "clientName": "Padaria Dom Pão",
      "pickupAddress": "Rua Major Facundo, 500, Fortaleza - CE",
      "deliveryAddress": "Rua Barão de Studart, 1000, Fortaleza - CE",
      "splits": [
        {
          "recipientId": "re_cmj135cro00280l9t4fnpb66t",
          "recipientName": "João Silva",
          "recipientRole": "COURIER",
          "amount": 15.66,
          "percentage": 87.0,
          "liable": true
        },
        {
          "recipientId": "re_abc123def456",
          "recipientName": "Maria Santos",
          "recipientRole": "ORGANIZER",
          "amount": 0.90,
          "percentage": 5.0,
          "liable": false
        },
        {
          "recipientId": "re_cmjixi23o011w0k9tdigurq8t",
          "recipientName": "Plataforma Zapi10",
          "recipientRole": "PLATFORM",
          "amount": 1.44,
          "percentage": 8.0,
          "liable": false
        }
      ]
    }
  ],
  "consolidatedSplits": [
    {
      "recipientId": "re_cmj135cro00280l9t4fnpb66t",
      "recipientName": "João Silva",
      "recipientRole": "COURIER",
      "amount": 34.80,
      "percentage": 87.0,
      "liable": true
    },
    {
      "recipientId": "re_abc123def456",
      "recipientName": "Maria Santos",
      "recipientRole": "ORGANIZER",
      "amount": 2.00,
      "percentage": 5.0,
      "liable": false
    },
    {
      "recipientId": "re_cmjixi23o011w0k9tdigurq8t",
      "recipientName": "Plataforma Zapi10",
      "recipientRole": "PLATFORM",
      "amount": 3.20,
      "percentage": 8.0,
      "liable": false
    }
  ]
}
```

---

## 📦 DTOs (TypeScript)

### PaymentReportResponse

```typescript
interface PaymentReportResponse {
  paymentId: number;
  pagarmeOrderId: string | null;
  status: PaymentStatus;
  totalAmount: number;
  currency: string;
  createdAt: string; // ISO 8601 DateTime
  pixQrCode: string | null;
  pixQrCodeUrl: string | null;
  expiresAt: string | null; // ISO 8601 DateTime
  deliveries: DeliveryItem[];
  consolidatedSplits: SplitItem[];
}

interface DeliveryItem {
  deliveryId: number;
  shippingFee: number;
  clientName: string;
  pickupAddress: string;
  deliveryAddress: string;
  splits: SplitItem[];
}

interface SplitItem {
  recipientId: string;
  recipientName: string;
  recipientRole: RecipientRole;
  amount: number;
  percentage: number;
  liable: boolean;
}

type PaymentStatus = 
  | 'PENDING'    // Aguardando pagamento
  | 'PROCESSING' // Processando pagamento
  | 'COMPLETED'  // Pagamento concluído
  | 'FAILED'     // Pagamento falhou
  | 'REFUNDED'   // Pagamento reembolsado
  | 'CANCELLED'  // Pagamento cancelado
  | 'EXPIRED';   // Pagamento expirado

type RecipientRole = 
  | 'COURIER'    // Motoboy (87% do frete por padrão)
  | 'ORGANIZER'  // Gerente ADM (5% do frete por padrão)
  | 'PLATFORM';  // Plataforma Zapi10 (8% do frete por padrão)
```

---

## 📊 Estrutura de Dados

### Campos Principais

#### `paymentId`
- **Tipo:** `number`
- **Descrição:** ID único do pagamento no sistema

#### `pagarmeOrderId`
- **Tipo:** `string | null`
- **Descrição:** ID da order no Pagar.me (gateway de pagamento)

#### `status`
- **Tipo:** `PaymentStatus`
- **Descrição:** Status atual do pagamento
- **Valores possíveis:**
  - `PENDING`: Aguardando pagamento (PIX gerado)
  - `PROCESSING`: Processando pagamento
  - `COMPLETED`: Pagamento confirmado
  - `FAILED`: Pagamento falhou
  - `REFUNDED`: Pagamento reembolsado
  - `CANCELLED`: Pagamento cancelado
  - `EXPIRED`: PIX expirou sem pagamento

#### `totalAmount`
- **Tipo:** `number`
- **Descrição:** Valor total do pagamento em R$ (soma de todas as deliveries)
- **Exemplo:** `40.00` = R$ 40,00

#### `currency`
- **Tipo:** `string`
- **Descrição:** Moeda do pagamento
- **Valor padrão:** `"BRL"` (Real Brasileiro)

#### `createdAt`
- **Tipo:** `string` (ISO 8601)
- **Descrição:** Data/hora de criação do pagamento
- **Formato:** `"2025-12-23T15:40:57"`

#### `pixQrCode`
- **Tipo:** `string | null`
- **Descrição:** Código PIX copia-e-cola (texto completo)
- **Uso:** Para pagamento via PIX no mobile banking

#### `pixQrCodeUrl`
- **Tipo:** `string | null`
- **Descrição:** URL da imagem do QR Code PIX
- **Uso:** Para exibir QR Code visual

#### `expiresAt`
- **Tipo:** `string | null` (ISO 8601)
- **Descrição:** Data/hora de expiração do PIX
- **Padrão:** 2 horas após criação

---

### Array `deliveries`

Lista de todas as entregas incluídas neste pagamento consolidado.

#### Campos de `DeliveryItem`

##### `deliveryId`
- **Tipo:** `number`
- **Descrição:** ID único da entrega

##### `shippingFee`
- **Tipo:** `number`
- **Descrição:** Valor do frete desta entrega em R$
- **Exemplo:** `22.00` = R$ 22,00

##### `clientName`
- **Tipo:** `string`
- **Descrição:** Nome do cliente que solicitou a entrega

##### `pickupAddress`
- **Tipo:** `string`
- **Descrição:** Endereço de coleta (origem)

##### `deliveryAddress`
- **Tipo:** `string`
- **Descrição:** Endereço de entrega (destino)

##### `splits`
- **Tipo:** `SplitItem[]`
- **Descrição:** Lista de splits (divisões de pagamento) desta delivery específica
- **Sempre contém 3 items:**
  1. Split do COURIER (motoboy)
  2. Split do ORGANIZER (gerente)
  3. Split da PLATFORM (Zapi10)

---

### Array `consolidatedSplits`

Lista consolidada (soma total) de todos os splits por recipient.

**Útil para:**
- Mostrar quanto cada pessoa receberá no total
- Exibir resumo financeiro do pagamento
- Dashboard de motoboys/gerentes

#### Campos de `SplitItem`

##### `recipientId`
- **Tipo:** `string`
- **Descrição:** ID do recipient no Pagar.me
- **Formato:** `"re_xxxxxxxxxxxxxxxxxx"`

##### `recipientName`
- **Tipo:** `string`
- **Descrição:** Nome do recebedor
- **Exemplos:**
  - `"João Silva"` (motoboy)
  - `"Maria Santos"` (gerente)
  - `"Plataforma Zapi10"` (plataforma)

##### `recipientRole`
- **Tipo:** `RecipientRole`
- **Descrição:** Papel/função do recebedor
- **Valores possíveis:**
  - `COURIER`: Motoboy que executou a entrega
  - `ORGANIZER`: Gerente ADM da organização
  - `PLATFORM`: Plataforma Zapi10

##### `amount`
- **Tipo:** `number`
- **Descrição:** Valor em R$ que este recipient receberá
- **Formato:** Sempre com 2 casas decimais
- **Exemplo:** `19.14` = R$ 19,14

##### `percentage`
- **Tipo:** `number`
- **Descrição:** Percentual do frete que este recipient recebe
- **Valores padrão:**
  - COURIER: `87.0` (87%)
  - ORGANIZER: `5.0` (5%)
  - PLATFORM: `8.0` (8%)

##### `liable`
- **Tipo:** `boolean`
- **Descrição:** Indica se este recipient é responsável por pagar as taxas do gateway
- **Valores:**
  - `true`: COURIER (paga as taxas)
  - `false`: ORGANIZER e PLATFORM (não pagam taxas)

---

## 🎯 Casos de Uso

### 1. Exibir Detalhes do Pagamento

```typescript
async function showPaymentReport(paymentId: number) {
  const response = await fetch(`/api/payments/${paymentId}/report`, {
    headers: {
      'Authorization': `Bearer ${token}`
    }
  });
  
  const report: PaymentReportResponse = await response.json();
  
  console.log(`Pagamento #${report.paymentId}`);
  console.log(`Status: ${report.status}`);
  console.log(`Valor Total: R$ ${report.totalAmount.toFixed(2)}`);
  console.log(`Deliveries: ${report.deliveries.length}`);
}
```

### 2. Calcular Total por Recipient

```typescript
function calculateRecipientTotal(
  report: PaymentReportResponse, 
  role: RecipientRole
): number {
  const split = report.consolidatedSplits.find(
    s => s.recipientRole === role
  );
  return split?.amount || 0;
}

// Exemplo: quanto o motoboy vai receber?
const courierTotal = calculateRecipientTotal(report, 'COURIER');
console.log(`Motoboy receberá: R$ ${courierTotal.toFixed(2)}`);
```

### 3. Listar Deliveries com Splits

```typescript
function renderDeliveryList(report: PaymentReportResponse) {
  return report.deliveries.map(delivery => (
    <div key={delivery.deliveryId}>
      <h3>Entrega #{delivery.deliveryId}</h3>
      <p>Cliente: {delivery.clientName}</p>
      <p>Frete: R$ {delivery.shippingFee.toFixed(2)}</p>
      
      <h4>Divisão do pagamento:</h4>
      <ul>
        {delivery.splits.map((split, idx) => (
          <li key={idx}>
            {split.recipientName} ({split.recipientRole}): 
            R$ {split.amount.toFixed(2)} ({split.percentage}%)
          </li>
        ))}
      </ul>
    </div>
  ));
}
```

### 4. Exibir Resumo Consolidado

```typescript
function renderConsolidatedSummary(report: PaymentReportResponse) {
  return (
    <div className="summary">
      <h2>Resumo do Pagamento</h2>
      <p>Total: R$ {report.totalAmount.toFixed(2)}</p>
      
      <h3>Distribuição:</h3>
      <table>
        <thead>
          <tr>
            <th>Recebedor</th>
            <th>Papel</th>
            <th>Valor</th>
            <th>%</th>
          </tr>
        </thead>
        <tbody>
          {report.consolidatedSplits.map((split, idx) => (
            <tr key={idx}>
              <td>{split.recipientName}</td>
              <td>{split.recipientRole}</td>
              <td>R$ {split.amount.toFixed(2)}</td>
              <td>{split.percentage}%</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
```

---

## ⚠️ Errors

### 404 Not Found

```json
{
  "timestamp": "2025-12-23T15:40:57",
  "status": 404,
  "error": "Not Found",
  "message": "Payment não encontrado: 999",
  "path": "/api/payments/999/report"
}
```

**Causa:** Payment ID não existe no banco de dados.

---

### 401 Unauthorized

```json
{
  "timestamp": "2025-12-23T15:40:57",
  "status": 401,
  "error": "Unauthorized",
  "message": "Full authentication is required to access this resource",
  "path": "/api/payments/69/report"
}
```

**Causa:** Token JWT ausente ou inválido.

---

### 403 Forbidden

```json
{
  "timestamp": "2025-12-23T15:40:57",
  "status": 403,
  "error": "Forbidden",
  "message": "Access Denied",
  "path": "/api/payments/69/report"
}
```

**Causa:** Usuário não tem permissão para acessar este payment.

---

### 500 Internal Server Error

```json
{
  "timestamp": "2025-12-23T15:40:57",
  "status": 500,
  "error": "Internal Server Error",
  "message": "Nenhuma configuração ativa encontrada",
  "path": "/api/payments/69/report"
}
```

**Causa:** Erro interno do servidor (configuração faltando, etc).

---

## 📝 Notas Importantes

### 1. Percentuais Padrão

Os percentuais de split são configuráveis no admin, mas os valores padrão são:
- **COURIER**: 87% (motoboy que executa a entrega)
- **ORGANIZER**: 5% (gerente ADM da organização)
- **PLATFORM**: 8% (plataforma Zapi10)

### 2. Liable (Responsável por Taxas)

- **COURIER** sempre é `liable: true` → paga as taxas do gateway (Pagar.me)
- **ORGANIZER** e **PLATFORM** são `liable: false` → não pagam taxas

### 3. Arredondamento

Valores são arredondados para baixo (floor) em centavos e depois convertidos para reais com 2 casas decimais.

### 4. Deliveries Múltiplas

Um pagamento pode consolidar **múltiplas deliveries** do mesmo cliente, permitindo:
- Pagamento único com um QR Code
- Economia de taxas de transação
- Melhor UX para o cliente

### 5. Status em Tempo Real

O campo `status` é atualizado automaticamente quando:
- Payment é criado: `PENDING`
- Gateway processa: `PROCESSING`
- Cliente paga: `COMPLETED`
- Erro no gateway: `FAILED`
- PIX expira sem pagamento: `EXPIRED`

---

## 🔄 Fluxo Completo

```mermaid
sequenceDiagram
    participant FE as Frontend
    participant BE as Backend
    participant DB as Database
    
    FE->>BE: GET /api/payments/69/report
    BE->>DB: Buscar Payment + Deliveries + Config
    DB-->>BE: Payment, Deliveries, SiteConfiguration
    BE->>BE: Calcular splits por delivery
    BE->>BE: Consolidar splits por recipient
    BE-->>FE: PaymentReportResponse
    FE->>FE: Renderizar relatório
```

---

## 📞 Suporte

Para dúvidas ou problemas:
1. Verificar logs da aplicação
2. Validar token JWT
3. Confirmar que Payment ID existe
4. Verificar permissões do usuário

---

## 📚 Recursos Relacionados

- **Criar Pagamento**: `POST /api/payment/create-with-split`
- **Listar Pagamentos**: `GET /api/payments`
- **Detalhes do Pagamento**: `GET /api/payments/{id}`
- **Atualizar Status**: `PUT /api/payments/{id}/status`
