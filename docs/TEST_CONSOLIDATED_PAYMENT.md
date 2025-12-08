# 🧪 Teste do Sistema de Pagamento Consolidado

## 📋 Pré-requisitos

1. ✅ Servidor rodando em modo `dry-run`
2. ✅ Deliveries existentes no banco (IDs: 1, 13, etc.)
3. ✅ JWT Token válido

## 🚀 Exemplo de Requisição

### **Request Body Completo:**

```json
{
  "deliveryIds": [1, 13],
  "clientEmail": "cliente@example.com",
  "clientName": "João Silva",
  "expirationHours": 24
}
```

### **cURL Command:**

```bash
curl -X POST 'http://localhost:8080/api/payment/create-invoice' \
  -H 'Content-Type: application/json' \
  -H 'Authorization: Bearer SEU_TOKEN_JWT_AQUI' \
  -d '{
    "deliveryIds": [1, 13],
    "clientEmail": "cliente@example.com",
    "clientName": "João Silva",
    "expirationHours": 24
  }'
```

## 📊 Campos do Request

| Campo | Tipo | Obrigatório | Descrição |
|-------|------|-------------|-----------|
| `deliveryIds` | `Long[]` | ✅ Sim | IDs das deliveries a pagar |
| `clientEmail` | `String` | ✅ Sim | Email do pagador (usado como `username`) |
| `clientName` | `String` | ⚪ Não | Nome do pagador (se não existir, será criado) |
| `expirationHours` | `Integer` | ⚪ Não | Horas até expirar (padrão: 24h) |

## 🔄 Fluxo de Processamento

### **1. Buscar Pagador (Payer)**
```
┌─────────────────────────────────────────┐
│ Email: cliente@example.com              │
│ ↓                                       │
│ Busca User por username (email)         │
│ ↓                                       │
│ Se NÃO existe:                          │
│   ❌ ERRO 400 Bad Request               │
│   "Cliente não encontrado. Por favor,   │
│    cadastre o cliente primeiro."        │
│                                         │
│ Se existe:                              │
│   ✅ Usa User existente como payer      │
└─────────────────────────────────────────┘
```

**⚠️ IMPORTANTE:** O cliente **DEVE estar cadastrado** na tabela `users` antes de criar o pagamento!

### **2. Buscar Deliveries**
```sql
SELECT * FROM deliveries WHERE id IN (1, 13);
```

### **3. Calcular Splits (baseado em `shippingFee`)**
```
Delivery #1:  R$ 10,00 (frete)
Delivery #13: R$ 15,00 (frete)
────────────────────────────
Total:        R$ 25,00

Splits:
  👨‍🚀 Motoboys (87%):  R$ 21,75
  👔 Gerentes (5%):    R$  1,25
  🏢 Plataforma (8%):  R$  2,00
  ────────────────────────────
  Total:              R$ 25,00 ✅
```

### **4. Criar Invoice (Mock em dry-run)**
```javascript
{
  "id": "MOCK_INV_1733356800000",
  "pixQrCode": "00020126360014BR.GOV.BCB.PIX...",
  "pixQrCodeUrl": "https://via.placeholder.com/300x300.png?text=QR+CODE+MOCK",
  "secureUrl": "https://mock.iugu.com/invoice/MOCK_INV_1733356800000",
  "dueDate": "2025-12-05T21:30:00"
}
```

### **5. Salvar Payment no Banco**
```sql
INSERT INTO payments (
  iugu_invoice_id,
  amount,
  status,
  payer_id,  -- ✅ AGORA PREENCHIDO!
  pix_qr_code,
  pix_qr_code_url,
  expires_at
) VALUES (
  'MOCK_INV_1733356800000',
  25.00,
  'PENDING',
  'uuid-do-cliente',  -- ✅ User criado/encontrado
  '00020126360014BR.GOV.BCB.PIX...',
  'https://via.placeholder.com/300x300.png',
  '2025-12-05 21:30:00'
);
```

## ✅ Response Esperado

```json
{
  "paymentId": 123,
  "iuguInvoiceId": "MOCK_INV_1733356800000",
  "amount": 25.00,
  "pixQrCode": "00020126360014BR.GOV.BCB.PIX0014BR.COM.IUGU...",
  "pixQrCodeUrl": "https://via.placeholder.com/300x300.png?text=QR+CODE+MOCK",
  "secureUrl": "https://mock.iugu.com/invoice/MOCK_INV_1733356800000",
  "expiresAt": "2025-12-05T21:30:00",
  "status": "PENDING",
  "deliveryCount": 2,
  "splits": {
    "couriersCount": 2,
    "couriersAmount": 21.75,
    "managersCount": 2,
    "managersAmount": 1.25,
    "platformAmount": 2.00,
    "recipients": {
      "COURIER - José Motoboy": 10.87,
      "COURIER - Maria Entregadora": 10.88,
      "MANAGER - João Gerente": 0.62,
      "MANAGER - Ana Coordenadora": 0.63,
      "Plataforma": 2.00
    }
  },
  "deliveries": [
    {
      "id": 1,
      "shippingFee": 10.00,
      "totalAmount": 25.00,
      "status": "DELIVERED"
    },
    {
      "id": 13,
      "shippingFee": 15.00,
      "totalAmount": 15.00,
      "status": "DELIVERED"
    }
  ]
}
```

## 🎯 Frontend - Como Exibir QR Code

### **Opção 1: Exibir QR Code como Imagem**
```html
<img src="{{ pixQrCodeUrl }}" alt="QR Code PIX" />
```

### **Opção 2: Gerar QR Code do String**
```javascript
import QRCode from 'qrcode';

// Gerar QR Code a partir do pixQrCode
QRCode.toDataURL(response.pixQrCode)
  .then(url => {
    document.getElementById('qrcode').src = url;
  });
```

### **Opção 3: Link para Pagar**
```html
<a href="{{ secureUrl }}" target="_blank">
  Pagar via PIX
</a>
```

## 🔍 Logs do Backend (dry-run)

```
═══════════════════════════════════════════════════════════════
📨 REQUEST RECEBIDO - Invoice Consolidada
═══════════════════════════════════════════════════════════════
📦 Delivery IDs: [1, 13]
📧 Client Email: cliente@example.com
👤 Client Name: João Silva
⏰ Expiration Hours: 24
───────────────────────────────────────────────────────────────
🎯 Criando invoice consolidada para 2 deliveries
💳 Pagador: João Silva (ID: uuid..., Email: cliente@example.com)
═══════════════════════════════════════════════════════════════
📦 DELIVERIES ENCONTRADAS: 2
═══════════════════════════════════════════════════════════════
📦 Delivery #1
   💰 Valor do Frete: R$ 10.00
   🛒 Valor do Pedido: R$ 25.00 (não entra no split)
   👨‍🚀 Motoboy: José (Iugu: ACC_JOSE123)
   👔 Gerente: João (Iugu: ACC_JOAO456)
   📍 Status: DELIVERED
───────────────────────────────────────────────────────────────
📦 Delivery #13
   💰 Valor do Frete: R$ 15.00
   🛒 Valor do Pedido: R$ 15.00 (não entra no split)
   👨‍🚀 Motoboy: Maria (Iugu: ACC_MARIA789)
   👔 Gerente: Ana (Iugu: ACC_ANA012)
   📍 Status: DELIVERED
───────────────────────────────────────────────────────────────
💰 VALOR TOTAL DOS FRETES (para split): R$ 25.00
═══════════════════════════════════════════════════════════════
🧪 DRY-RUN MODE: Retornando invoice MOCKADA (não chamou Iugu real)
✅ Invoice consolidada criada: Payment #123 → Iugu Invoice MOCK_INV_...
```

## ⚠️ Possíveis Erros

### 1. **400 Bad Request - "Cliente não encontrado"**
```json
{
  "error": "Bad Request",
  "message": "Cliente com email 'cliente@example.com' não encontrado. Por favor, cadastre o cliente primeiro antes de criar o pagamento.",
  "status": 400
}
```

**Solução:** Cadastre o cliente na tabela `users` antes:
```sql
-- Verificar se cliente existe
SELECT * FROM users WHERE username = 'cliente@example.com';

-- Se não existir, cadastrar manualmente ou via endpoint de cadastro
```

### 2. **400 Bad Request - Delivery sem frete**
```json
{
  "error": "Bad Request",
  "message": "Delivery #5 não tem valor de frete (shippingFee) configurado"
}
```

**Solução:** Configure o `shippingFee` na delivery antes de criar o payment.

### 3. **404 Not Found - Deliveries não encontradas**
```json
{
  "error": "Not Found",
  "message": "Deliveries não encontradas: [99, 100]"
}
```

**Solução:** Verifique se os IDs das deliveries existem no banco.

## 🔐 Como Obter Token JWT

```bash
# Login
curl -X POST 'http://localhost:8080/api/auth/login' \
  -H 'Content-Type: application/json' \
  -d '{
    "username": "admin@mvt.com",
    "password": "admin123"
  }'

# Response
{
  "token": "eyJhbGciOiJIUzI1NiIs...",
  "type": "Bearer",
  "username": "admin@mvt.com"
}
```

Use o `token` no header `Authorization: Bearer eyJhbGci...`

## 📝 Notas Importantes

1. **Campo `username` = Email**: No User, o campo `username` armazena o email do usuário
2. **Auto-criação de Cliente**: Se o email não existir, um novo User com Role.CLIENT é criado automaticamente
3. **Modo dry-run**: Não faz requisições reais ao Iugu, retorna dados mockados
4. **Cálculo sobre frete**: Splits são calculados sobre `shippingFee`, NÃO sobre `totalAmount`
5. **QR Code fake**: Em dry-run, o QR Code é um placeholder para testes visuais
