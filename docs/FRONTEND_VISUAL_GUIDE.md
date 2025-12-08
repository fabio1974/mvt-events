# 📱 Guia Visual - Como Exibir QR Code PIX no Frontend

**Para**: Time de Frontend  
**Data**: 04/12/2025

---

## 🎯 O Que Você Precisa Fazer

Criar uma tela de pagamento que mostre o **QR Code PIX** para o cliente escanear e pagar.

---

## 📸 Layout Sugerido

```
┌─────────────────────────────────────┐
│                                     │
│     Pagamento de 5 Entregas        │
│                                     │
│         💰 R$ 200,00               │
│                                     │
├─────────────────────────────────────┤
│                                     │
│        ████████████████            │
│        ██          ██              │
│        ██  QR CODE ██              │
│        ██          ██              │
│        ████████████████            │
│                                     │
│   Escaneie com o app do banco     │
│                                     │
├─────────────────────────────────────┤
│                                     │
│  [📋 Copiar código PIX]           │
│                                     │
│  [🌐 Pagar no navegador]          │
│                                     │
├─────────────────────────────────────┤
│                                     │
│  ℹ️ Como será distribuído:         │
│                                     │
│  👨‍🚀 3 Motoboys   R$ 174,00        │
│  👔 2 Gerentes    R$ 10,00         │
│  🏢 Plataforma    R$ 16,00         │
│                                     │
├─────────────────────────────────────┤
│                                     │
│  ⏰ Expira em: 05/12 às 19:00     │
│                                     │
│  ⏳ Aguardando pagamento...        │
│                                     │
└─────────────────────────────────────┘
```

---

## 🔗 1. Chamar o Endpoint

```javascript
const createPayment = async (deliveryIds) => {
  const token = localStorage.getItem('authToken');
  
  const response = await fetch('http://localhost:8080/api/payment/create-invoice', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`
    },
    body: JSON.stringify({
      deliveryIds: deliveryIds,              // [1, 2, 3, 4, 5]
      clientEmail: 'cliente@example.com',    // Email do cliente
      expirationHours: 24                    // Opcional (padrão: 24h)
    })
  });
  
  return await response.json();
};
```

---

## 🖼️ 2. Exibir QR Code (Imagem)

Use o campo `pixQrCodeUrl` do response:

```jsx
// React
<img 
  src={payment.pixQrCodeUrl} 
  alt="QR Code PIX" 
  style={{ width: 256, height: 256 }}
/>
```

```vue
<!-- Vue -->
<img 
  :src="payment.pixQrCodeUrl" 
  alt="QR Code PIX" 
  style="width: 256px; height: 256px"
/>
```

```html
<!-- HTML Puro -->
<img 
  id="qr-code" 
  alt="QR Code PIX"
  style="width: 256px; height: 256px"
/>

<script>
  const payment = await createPayment([1, 2, 3]);
  document.getElementById('qr-code').src = payment.pixQrCodeUrl;
</script>
```

---

## 📋 3. Botão "Copiar Código PIX"

Use o campo `pixQrCode` do response:

```jsx
// React
const copyToClipboard = () => {
  navigator.clipboard.writeText(payment.pixQrCode);
  toast.success('✅ Código PIX copiado!');
};

<button onClick={copyToClipboard}>
  📋 Copiar código PIX
</button>
```

```vue
<!-- Vue -->
<template>
  <button @click="copyCode">
    📋 Copiar código PIX
  </button>
</template>

<script>
export default {
  methods: {
    async copyCode() {
      await navigator.clipboard.writeText(this.payment.pixQrCode);
      this.$toast.success('✅ Código PIX copiado!');
    }
  }
}
</script>
```

---

## 🌐 4. Link "Pagar no Navegador"

Use o campo `secureUrl` do response:

```jsx
// React
<a 
  href={payment.secureUrl} 
  target="_blank" 
  rel="noopener noreferrer"
  className="btn-link"
>
  🌐 Pagar no navegador
</a>
```

---

## 💰 5. Mostrar Valor e Detalhes

```jsx
// React
<div className="payment-details">
  <h2>Pagamento de {payment.deliveryCount} Entrega(s)</h2>
  <p className="amount">R$ {payment.amount.toFixed(2)}</p>
  
  <div className="splits">
    <h3>Como será distribuído:</h3>
    <ul>
      <li>
        👨‍🚀 {payment.splits.couriersCount} Motoboy(s): 
        <strong>R$ {payment.splits.couriersAmount.toFixed(2)}</strong>
      </li>
      <li>
        👔 {payment.splits.managersCount} Gerente(s): 
        <strong>R$ {payment.splits.managersAmount.toFixed(2)}</strong>
      </li>
      <li>
        🏢 Plataforma: 
        <strong>R$ {payment.splits.platformAmount.toFixed(2)}</strong>
      </li>
    </ul>
  </div>
</div>
```

---

## ⏰ 6. Contador de Expiração

```jsx
// React
import { useState, useEffect } from 'react';

function ExpirationCountdown({ expiresAt }) {
  const [timeLeft, setTimeLeft] = useState('');

  useEffect(() => {
    const timer = setInterval(() => {
      const now = new Date();
      const expiry = new Date(expiresAt);
      const diff = expiry - now;

      if (diff <= 0) {
        setTimeLeft('⏰ EXPIRADO');
        clearInterval(timer);
      } else {
        const hours = Math.floor(diff / (1000 * 60 * 60));
        const minutes = Math.floor((diff % (1000 * 60 * 60)) / (1000 * 60));
        const seconds = Math.floor((diff % (1000 * 60)) / 1000);
        setTimeLeft(`⏰ ${hours}h ${minutes}m ${seconds}s`);
      }
    }, 1000);

    return () => clearInterval(timer);
  }, [expiresAt]);

  return <p className="expiration">{timeLeft}</p>;
}
```

---

## 🔄 7. Polling (Verificar Status)

```jsx
// React
import { useState, useEffect } from 'react';

function PaymentStatus({ paymentId }) {
  const [status, setStatus] = useState('PENDING');

  useEffect(() => {
    const pollStatus = setInterval(async () => {
      const response = await fetch(
        `http://localhost:8080/api/payment/${paymentId}/status`,
        {
          headers: {
            'Authorization': `Bearer ${localStorage.getItem('authToken')}`
          }
        }
      );
      
      const data = await response.json();
      setStatus(data.status);

      if (data.status === 'COMPLETED') {
        clearInterval(pollStatus);
        alert('🎉 Pagamento confirmado!');
        // Redirecionar ou atualizar UI
      }
    }, 5000); // Verifica a cada 5 segundos

    return () => clearInterval(pollStatus);
  }, [paymentId]);

  return (
    <div className="status">
      {status === 'PENDING' && '⏳ Aguardando pagamento...'}
      {status === 'COMPLETED' && '✅ Pagamento confirmado!'}
      {status === 'EXPIRED' && '❌ Pagamento expirado'}
    </div>
  );
}
```

---

## 🎨 CSS Sugerido

```css
.payment-container {
  max-width: 500px;
  margin: 0 auto;
  padding: 20px;
  text-align: center;
}

.amount {
  font-size: 48px;
  font-weight: bold;
  color: #2ecc71;
  margin: 20px 0;
}

.qr-code-section {
  background: white;
  padding: 30px;
  border-radius: 12px;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
  margin: 20px 0;
}

.qr-code-section img {
  display: block;
  margin: 0 auto;
}

.copy-button, .browser-link {
  display: block;
  width: 100%;
  padding: 15px;
  margin: 10px 0;
  background: #3498db;
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 16px;
  cursor: pointer;
  text-decoration: none;
}

.copy-button:hover {
  background: #2980b9;
}

.splits-details {
  background: #f8f9fa;
  padding: 20px;
  border-radius: 8px;
  margin: 20px 0;
  text-align: left;
}

.splits-details ul {
  list-style: none;
  padding: 0;
}

.splits-details li {
  padding: 10px 0;
  border-bottom: 1px solid #dee2e6;
}

.expiration {
  font-size: 14px;
  color: #e74c3c;
  margin-top: 20px;
}

.status {
  font-size: 18px;
  font-weight: bold;
  padding: 15px;
  border-radius: 8px;
  margin-top: 10px;
}
```

---

## 📦 Response Completo (Exemplo)

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

---

## ✅ Checklist de Implementação

- [ ] Chamar endpoint `/api/payment/create-invoice`
- [ ] Exibir QR Code usando `pixQrCodeUrl`
- [ ] Botão "Copiar código PIX" usando `pixQrCode`
- [ ] Link "Pagar no navegador" usando `secureUrl`
- [ ] Mostrar valor total (`amount`)
- [ ] Mostrar quantidade de deliveries (`deliveryCount`)
- [ ] Mostrar detalhes dos splits (opcional)
- [ ] Contador de expiração
- [ ] Polling para verificar status
- [ ] Tratamento de erros (401, 404, 500)
- [ ] Testes com deliveries reais

---

## 🎯 Resultado Final

Cliente verá:
1. ✅ **QR Code grande** para escanear
2. ✅ **Botão copiar** se preferir colar o código
3. ✅ **Link navegador** para abrir página do Iugu
4. ✅ **Valor total** destacado
5. ✅ **Detalhes** de como o dinheiro será distribuído
6. ✅ **Contador** até expiração
7. ✅ **Status** atualizado automaticamente

---

**Dúvidas?** Consulte `FRONTEND_PAYMENT_DOCS.md` para documentação completa!
