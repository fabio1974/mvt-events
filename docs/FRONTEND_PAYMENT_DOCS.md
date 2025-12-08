# 📱 Documentação Frontend - Invoice Consolidada com PIX

**Data**: 04/12/2025  
**Versão da API**: v1.0  
**Endpoint Base**: `http://localhost:8080/api/payment`

---

## 📋 Índice

1. [Visão Geral](#visão-geral)
2. [Endpoint Principal](#endpoint-principal)
3. [Request e Response](#request-e-response)
4. [Exibindo QR Code PIX](#exibindo-qr-code-pix)
5. [Exemplos de Implementação](#exemplos-de-implementação)
6. [Casos de Uso](#casos-de-uso)
7. [Tratamento de Erros](#tratamento-de-erros)
8. [FAQ](#faq)

---

## 🎯 Visão Geral

### O que é?

Um endpoint simplificado onde o **Frontend envia apenas os IDs das deliveries** e o **backend calcula automaticamente** quanto cada motoboy/gerente deve receber, criando **uma única invoice** no Iugu com **QR Code PIX** para pagamento.

### Como funciona?

```
┌─────────────┐
│   Cliente   │ Seleciona N deliveries
│  (Frontend) │ 
└──────┬──────┘
       │
       │ POST /api/payment/create-invoice
       │ { "deliveryIds": [1, 2, 3, 4, 5] }
       │
       ▼
┌─────────────┐
│   Backend   │ 1. Busca deliveries
│             │ 2. Calcula splits (87% motoboy, 5% gerente)
│             │ 3. Agrupa por pessoa
│             │ 4. Cria invoice no Iugu
└──────┬──────┘
       │
       │ Response com QR Code PIX
       │
       ▼
┌─────────────┐
│   Cliente   │ Escaneia QR Code
│  (Frontend) │ e paga via PIX
└──────┬──────┘
       │
       │ Pagamento confirmado
       │
       ▼
┌─────────────┐
│    Iugu     │ Distribui valores automaticamente
│             │ para motoboys, gerentes e plataforma
└─────────────┘
```

---

## 🔗 Endpoint Principal

### `POST /api/payment/create-invoice`

Cria uma invoice consolidada para múltiplas deliveries.

**URL**: `/api/payment/create-invoice`  
**Método**: `POST`  
**Autenticação**: Bearer Token (JWT)  
**Content-Type**: `application/json`

---

## 📤📥 Request e Response

### Request Body

```json
{
  "deliveryIds": [1, 2, 3, 4, 5],
  "clientEmail": "cliente@example.com",
  "expirationHours": 24
}
```

#### Campos

| Campo | Tipo | Obrigatório | Descrição | Padrão |
|-------|------|-------------|-----------|--------|
| `deliveryIds` | `number[]` | ✅ Sim | Array com IDs das deliveries a pagar | - |
| `clientEmail` | `string` | ✅ Sim | Email do cliente (recebe invoice) | - |
| `expirationHours` | `number` | ❌ Não | Horas até expirar (1-168) | `24` |

#### Validações

- ✅ `deliveryIds`: Não pode ser vazio, todas as deliveries devem existir
- ✅ `clientEmail`: Deve ser um email válido
- ✅ `expirationHours`: Mínimo 1, máximo 168 (7 dias)

---

### Response Body

```json
{
  "paymentId": 123,
  "iuguInvoiceId": "F7C8A9B1234567890",
  "pixQrCode": "00020126360014BR.GOV.BCB.PIX0114+5511987654321520400005303986540550.005802BR5913Zapi10 Ltda6009SAO PAULO62290525PAGAMENTO5ENTREGAS123456304A1B2",
  "pixQrCodeUrl": "https://faturas.iugu.com/qr/F7C8A9B1234567890.png",
  "secureUrl": "https://faturas.iugu.com/F7C8A9B1234567890",
  "amount": 200.00,
  "deliveryCount": 5,
  "splits": {
    "couriersCount": 3,
    "managersCount": 2,
    "couriersAmount": 174.00,
    "managersAmount": 10.00,
    "platformAmount": 16.00,
    "recipients": {
      "COURIER - João Silva": 113.10,
      "COURIER - Maria Santos": 26.10,
      "COURIER - Pedro Costa": 34.80,
      "MANAGER - Carlos Admin": 7.00,
      "MANAGER - Ana Gerente": 3.00,
      "Plataforma": 16.00
    }
  },
  "status": "PENDING",
  "expiresAt": "2025-12-05T19:00:00",
  "statusMessage": "⏳ Aguardando pagamento. Escaneie o QR Code PIX ou copie o código.",
  "expired": false
}
```

#### Campos do Response

| Campo | Tipo | Descrição |
|-------|------|-----------|
| `paymentId` | `number` | ID do pagamento no sistema |
| `iuguInvoiceId` | `string` | ID da invoice no Iugu |
| **`pixQrCode`** | **`string`** | **🔑 QR Code PIX (copiar/colar)** |
| **`pixQrCodeUrl`** | **`string`** | **🖼️ URL da imagem do QR Code** |
| **`secureUrl`** | **`string`** | **🔗 URL para abrir no navegador** |
| `amount` | `number` | Valor total em reais |
| `deliveryCount` | `number` | Quantidade de deliveries |
| `splits` | `object` | Detalhes de como o valor foi dividido |
| `splits.couriersCount` | `number` | Quantidade de motoboys |
| `splits.managersCount` | `number` | Quantidade de gerentes |
| `splits.couriersAmount` | `number` | Valor total para motoboys |
| `splits.managersAmount` | `number` | Valor total para gerentes |
| `splits.platformAmount` | `number` | Valor para plataforma |
| `splits.recipients` | `object` | Mapa de quem recebe quanto |
| `status` | `string` | Status: `PENDING`, `COMPLETED`, `EXPIRED`, `CANCELLED` |
| `expiresAt` | `string` | Data/hora de expiração (ISO 8601) |
| `statusMessage` | `string` | Mensagem amigável sobre o status |
| `expired` | `boolean` | Se a invoice já expirou |

---

## 📱 Exibindo QR Code PIX

### Opção 1: Exibir Imagem do QR Code (Recomendado)

Use `pixQrCodeUrl` para exibir a imagem diretamente:

```jsx
// React/Next.js
<img 
  src={response.pixQrCodeUrl} 
  alt="QR Code PIX" 
  width={256} 
  height={256}
/>
```

```vue
<!-- Vue.js -->
<img 
  :src="response.pixQrCodeUrl" 
  alt="QR Code PIX" 
  width="256" 
  height="256"
/>
```

```html
<!-- HTML Puro -->
<img 
  id="qr-code" 
  alt="QR Code PIX" 
  width="256" 
  height="256"
/>
<script>
  document.getElementById('qr-code').src = response.pixQrCodeUrl;
</script>
```

---

### Opção 2: Botão "Copiar Código PIX"

Use `pixQrCode` para permitir copiar o código:

```jsx
// React/Next.js
function CopyPixButton({ pixCode }) {
  const copyToClipboard = () => {
    navigator.clipboard.writeText(pixCode);
    alert('Código PIX copiado!');
  };

  return (
    <button onClick={copyToClipboard}>
      📋 Copiar código PIX
    </button>
  );
}
```

```vue
<!-- Vue.js -->
<template>
  <button @click="copyPixCode">
    📋 Copiar código PIX
  </button>
</template>

<script>
export default {
  methods: {
    copyPixCode() {
      navigator.clipboard.writeText(this.response.pixQrCode);
      this.$toast.success('Código PIX copiado!');
    }
  }
}
</script>
```

---

### Opção 3: Link "Pagar no Navegador"

Use `secureUrl` para abrir página de pagamento do Iugu:

```jsx
// React/Next.js
<a 
  href={response.secureUrl} 
  target="_blank" 
  rel="noopener noreferrer"
>
  🌐 Pagar no navegador
</a>
```

---

### Interface Completa (Exemplo React)

```jsx
import React, { useState } from 'react';

function PaymentPage({ deliveryIds }) {
  const [payment, setPayment] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const createInvoice = async () => {
    setLoading(true);
    setError(null);

    try {
      const token = localStorage.getItem('authToken');
      
      const response = await fetch('http://localhost:8080/api/payment/create-invoice', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`
        },
        body: JSON.stringify({
          deliveryIds: deliveryIds,
          clientEmail: 'cliente@example.com',
          expirationHours: 24
        })
      });

      if (!response.ok) {
        throw new Error('Erro ao criar invoice');
      }

      const data = await response.json();
      setPayment(data);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  const copyPixCode = () => {
    navigator.clipboard.writeText(payment.pixQrCode);
    alert('✅ Código PIX copiado para área de transferência!');
  };

  if (loading) {
    return <div>⏳ Gerando QR Code PIX...</div>;
  }

  if (error) {
    return <div>❌ Erro: {error}</div>;
  }

  if (!payment) {
    return (
      <button onClick={createInvoice}>
        💳 Gerar Pagamento PIX
      </button>
    );
  }

  return (
    <div className="payment-container">
      <h2>Pagamento de {payment.deliveryCount} Entrega(s)</h2>
      <p className="amount">R$ {payment.amount.toFixed(2)}</p>

      {/* QR Code */}
      <div className="qr-code-section">
        <img 
          src={payment.pixQrCodeUrl} 
          alt="QR Code PIX" 
          width={256} 
          height={256}
        />
        <p>Escaneie o QR Code com o app do seu banco</p>
      </div>

      {/* Botão Copiar */}
      <button onClick={copyPixCode} className="copy-button">
        📋 Copiar código PIX
      </button>

      {/* Link Navegador */}
      <a 
        href={payment.secureUrl} 
        target="_blank" 
        rel="noopener noreferrer"
        className="browser-link"
      >
        🌐 Abrir no navegador
      </a>

      {/* Detalhes dos Splits */}
      <div className="splits-details">
        <h3>Como o valor será distribuído:</h3>
        <ul>
          <li>
            👨‍🚀 {payment.splits.couriersCount} Motoboy(s): 
            <strong> R$ {payment.splits.couriersAmount.toFixed(2)}</strong>
          </li>
          <li>
            👔 {payment.splits.managersCount} Gerente(s): 
            <strong> R$ {payment.splits.managersAmount.toFixed(2)}</strong>
          </li>
          <li>
            🏢 Plataforma: 
            <strong> R$ {payment.splits.platformAmount.toFixed(2)}</strong>
          </li>
        </ul>
      </div>

      {/* Contador de Expiração */}
      <div className="expiration">
        <p>⏰ Expira em: {new Date(payment.expiresAt).toLocaleString('pt-BR')}</p>
        <p className="status">{payment.statusMessage}</p>
      </div>
    </div>
  );
}

export default PaymentPage;
```

---

## 💡 Casos de Uso

### Caso 1: Cliente paga 1 entrega

```json
// Request
{
  "deliveryIds": [42],
  "clientEmail": "cliente@example.com"
}

// Response
{
  "amount": 50.00,
  "deliveryCount": 1,
  "splits": {
    "couriersCount": 1,
    "managersCount": 1,
    "couriersAmount": 43.50,  // 87%
    "managersAmount": 2.50,    // 5%
    "platformAmount": 4.00     // 8%
  }
}
```

---

### Caso 2: Cliente paga 5 entregas (motoboys diferentes)

```json
// Request
{
  "deliveryIds": [1, 2, 3, 4, 5],
  "clientEmail": "cliente@example.com"
}

// Response (consolidado)
{
  "amount": 200.00,
  "deliveryCount": 5,
  "splits": {
    "couriersCount": 3,        // 3 motoboys diferentes
    "managersCount": 2,        // 2 gerentes diferentes
    "couriersAmount": 174.00,  // Soma dos 3 motoboys
    "managersAmount": 10.00,   // Soma dos 2 gerentes
    "platformAmount": 16.00,
    "recipients": {
      "COURIER - João (3 entregas)": 113.10,
      "COURIER - Maria (1 entrega)": 26.10,
      "COURIER - Pedro (1 entrega)": 34.80,
      "MANAGER - Carlos (2 entregas)": 7.00,
      "MANAGER - Ana (3 entregas)": 3.00,
      "Plataforma": 16.00
    }
  }
}
```

**Nota**: Backend automaticamente agrupa valores se a mesma pessoa aparece em múltiplas deliveries!

---

### Caso 3: Todas entregas do mesmo motoboy/gerente

```json
// Request
{
  "deliveryIds": [10, 11, 12],
  "clientEmail": "cliente@example.com"
}

// Response
{
  "amount": 150.00,
  "deliveryCount": 3,
  "splits": {
    "couriersCount": 1,         // Apenas 1 motoboy
    "managersCount": 1,         // Apenas 1 gerente
    "couriersAmount": 130.50,   // 87% do total
    "managersAmount": 7.50,     // 5% do total
    "platformAmount": 12.00
  }
}
```

---

## ⚠️ Tratamento de Erros

### Erro 400: Validation Error

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "deliveryIds: É necessário informar ao menos uma delivery",
  "path": "/api/payment/create-invoice"
}
```

**Solução**: Verificar se `deliveryIds` não está vazio.

---

### Erro 401: Unauthorized

```json
{
  "status": 401,
  "error": "Unauthorized",
  "message": "Token JWT inválido ou expirado"
}
```

**Solução**: Fazer login novamente e obter novo token.

---

### Erro 404: Deliveries não encontradas

```json
{
  "status": 404,
  "error": "Not Found",
  "message": "Deliveries não encontradas: [99, 100]"
}
```

**Solução**: Verificar se os IDs das deliveries existem.

---

### Erro 400: Motoboy sem conta Iugu

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Motoboy João Silva não tem conta Iugu configurada"
}
```

**Solução**: Motoboy precisa cadastrar dados bancários antes.

---

### Erro 500: Erro na API do Iugu

```json
{
  "status": 500,
  "error": "Internal Server Error",
  "message": "Erro ao criar fatura no Iugu"
}
```

**Solução**: Verificar logs do backend ou tentar novamente.

---

## ❓ FAQ

### 1. O QR Code expira?

✅ **Sim**. Por padrão expira em 24 horas. Você pode ajustar com `expirationHours` (1-168 horas).

---

### 2. Posso pagar múltiplas vezes a mesma delivery?

❌ **Não**. Uma delivery só pode ter 1 pagamento `COMPLETED`. Múltiplos payments representam histórico de tentativas.

---

### 3. E se o cliente não pagar a tempo?

⏰ Após expiração, o status muda para `EXPIRED`. Cliente precisa gerar novo QR Code.

---

### 4. Como sei se o pagamento foi confirmado?

📡 O backend recebe webhook do Iugu e atualiza o status para `COMPLETED`. Frontend pode:
- Polling: consultar status a cada 5 segundos
- WebSocket: receber notificação em tempo real
- Webhook próprio: Iugu envia para URL do frontend

---

### 5. Posso testar sem pagar de verdade?

✅ **Sim**! Use credenciais de **Sandbox** do Iugu (API Key começa com `test_`). Pagamentos são simulados.

---

### 6. Quanto tempo demora para o dinheiro cair na conta?

⏱️ **Iugu PIX**: Instantâneo após pagamento (até 2 minutos).  
💰 **Repasse para motoboys/gerentes**: Conforme configuração de auto-withdraw (diário/semanal).

---

### 7. E se der erro no meio do processo?

🔄 **Idempotência**: Você pode chamar o endpoint novamente com os mesmos `deliveryIds`. Backend criará nova invoice.

---

## 📞 Suporte

**Backend Logs**: `/app-boot-iugu-sandbox.log`  
**Iugu Dashboard**: https://iugu.com/  
**Documentação Iugu**: https://dev.iugu.com/

---

## 🎉 Checklist de Implementação

- [ ] Criar botão "Gerar Pagamento PIX"
- [ ] Exibir QR Code usando `pixQrCodeUrl`
- [ ] Implementar botão "Copiar código PIX" com `pixQrCode`
- [ ] Adicionar link "Pagar no navegador" com `secureUrl`
- [ ] Mostrar valor total e quantidade de deliveries
- [ ] Exibir detalhes dos splits (opcional, para transparência)
- [ ] Implementar contador de expiração
- [ ] Adicionar tratamento de erros (401, 404, 500)
- [ ] Testar com 1 delivery
- [ ] Testar com múltiplas deliveries (motoboys diferentes)
- [ ] Testar expiração (mudar `expirationHours` para 1 minuto)

---

**Versão**: 1.0  
**Última atualização**: 04/12/2025  
**Status**: ✅ Pronto para uso
