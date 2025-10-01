# Sistema de Pagamentos - MVT Events

Este documento descreve o sistema de pagamentos integrado com **Stripe**, **Mercado Pago** e **PayPal** para processar pagamentos via PIX, cartão de crédito/débito, PayPal e transferência bancária.

## Configuração

### Variáveis de Ambiente

```bash
# Provedor padrão (STRIPE, MERCADOPAGO ou PAYPAL)
PAYMENT_PROVIDER=MERCADOPAGO

# Configuração Stripe
STRIPE_PUBLIC_KEY=pk_test_...
STRIPE_SECRET_KEY=sk_test_...
STRIPE_WEBHOOK_SECRET=whsec_...

# Configuração MercadoPago
MERCADOPAGO_PUBLIC_KEY=TEST-...
MERCADOPAGO_ACCESS_TOKEN=TEST-...
MERCADOPAGO_WEBHOOK_SECRET=...

# Configuração PayPal
PAYPAL_CLIENT_ID=...
PAYPAL_CLIENT_SECRET=...
PAYPAL_ENVIRONMENT=sandbox  # ou production

# URLs de retorno
PAYMENT_RETURN_URL=http://localhost:3000/payment/success
PAYMENT_CANCEL_URL=http://localhost:3000/payment/cancel
PAYMENT_WEBHOOK_URL=http://localhost:8080/api/payments/webhook
```

### Estratégia de Seleção de Provedor

- **PIX**: Sempre usa MercadoPago (único que suporta PIX no Brasil)
- **PayPal Account**: Sempre usa PayPal (método específico)
- **Cartão**: Usa o provedor configurado em `PAYMENT_PROVIDER`
- **Transferência Bancária**: Usa o provedor configurado, com fallback para MercadoPago
- **Fallback**: Se o provedor não suportar o método, usa MercadoPago

## Métodos de Pagamento Suportados

### Stripe

- ✅ Cartão de Crédito (4.99% + R$ 0,39)
- ✅ Cartão de Débito (3.99% + R$ 0,39)
- ❌ PIX
- ❌ PayPal Account

### MercadoPago

- ✅ Cartão de Crédito (4.99% + R$ 0,60)
- ✅ Cartão de Débito (3.49% + R$ 0,39)
- ✅ PIX (1.99% + R$ 0,00)
- ✅ Transferência Bancária (1.99% + R$ 0,00)
- ❌ PayPal Account

### PayPal

- ✅ PayPal Account (3.49% + R$ 0,60)
- ✅ Cartão de Crédito (4.99% + R$ 0,60)
- ✅ Transferência Bancária (1.99% + R$ 0,00)
- ❌ PIX

## API Endpoints

### 1. Criar Pagamento

```http
POST /api/payments
Content-Type: application/json

{
  "registrationId": 123,
  "amount": 50.00,
  "paymentMethod": "PIX",
  "returnUrl": "http://localhost:3000/success",
  "cancelUrl": "http://localhost:3000/cancel",
  "pixExpirationMinutes": 30
}
```

**Resposta PIX:**

```json
{
  "success": true,
  "paymentId": "uuid-123",
  "status": "PENDING",
  "qrCode": "00020126580014br.gov.bcb.pix...",
  "qrCodeBase64": "iVBORw0KGgoAAAANSUhEUgA...",
  "pixCopyPaste": "00020126580014br.gov.bcb.pix...",
  "expiresAt": "2025-09-28T15:30:00"
}
```

**Exemplo PayPal:**

```json
{
  "registrationId": 123,
  "amount": 75.0,
  "paymentMethod": "PAYPAL_ACCOUNT",
  "returnUrl": "http://localhost:3000/success",
  "cancelUrl": "http://localhost:3000/cancel"
}
```

**Resposta PayPal:**

```json
{
  "success": true,
  "paymentId": "PAY-ABC123DEF456",
  "status": "CREATED",
  "approvalUrl": "https://www.sandbox.paypal.com/webapps/hermes?cmd=_express-checkout&token=PAY-ABC123DEF456",
  "fee": 3.22,
  "expiresAt": "2025-09-28T18:30:00"
}
```

**Resposta Cartão:**

```json
{
  "success": true,
  "paymentId": "pi_123",
  "status": "PENDING",
  "paymentUrl": "https://checkout.stripe.com/pay/pi_123",
  "fee": 2.34
}
```

**Resposta PayPal:**

```json
{
  "success": true,
  "paymentId": "PAY-ABC123DEF456",
  "status": "CREATED",
  "approvalUrl": "https://www.paypal.com/webapps/hermes?cmd=_express-checkout&token=PAY-ABC123DEF456",
  "fee": 3.49,
  "expiresAt": "2025-09-28T18:00:00"
}
```

### 2. Confirmar Pagamento

```http
POST /api/payments/{paymentId}/confirm
```

### 3. Status do Pagamento

```http
GET /api/payments/{paymentId}/status
```

### 4. Calcular Taxa

```http
GET /api/payments/calculate-fee?amount=100.00&paymentMethod=CREDIT_CARD
```

### 5. Métodos Suportados

```http
GET /api/payments/methods
```

### 6. Reembolso

```http
POST /api/payments/{paymentId}/refund
Content-Type: application/json

{
  "amount": 50.00,
  "reason": "Cancelamento do evento"
}
```

## Webhooks

### Stripe

```http
POST /api/payments/webhook/stripe
Stripe-Signature: t=123,v1=abc...
```

### MercadoPago

```http
POST /api/payments/webhook/mercadopago?topic=payment&id=123
```

## Taxas dos Provedores

### MercadoPago (Brasil)

- **PIX**: 1.99%
- **Cartão de Crédito**: 4.99% + R$ 0.60
- **Cartão de Débito**: 3.49% + R$ 0.39
- **Transferência Bancária**: 1.99%

### Stripe (Brasil)

- **Cartão de Crédito**: 3.99% + R$ 0.39
- **Cartão de Débito**: 3.99% + R$ 0.39
- **PIX**: Não suportado

### PayPal (Brasil)

- **PayPal Account**: 3.49% + R$ 0.60
- **Cartão de Crédito**: 4.99% + R$ 0.60
- **Transferência Bancária**: 1.99%
- **PIX**: Não suportado

## Fluxo de Pagamento

### PIX (MercadoPago)

1. Cliente escolhe PIX
2. Sistema gera QR Code e código copia-e-cola
3. Cliente escaneia QR Code ou copia código
4. Cliente faz pagamento no app do banco
5. MercadoPago notifica via webhook
6. Sistema atualiza status do pagamento

### Cartão (Stripe/MercadoPago/PayPal)

1. Cliente insere dados do cartão
2. Sistema cria payment intent/preference
3. Cliente é redirecionado para checkout
4. Cliente completa pagamento
5. Provedor processa pagamento
6. Sistema recebe confirmação via webhook

### PayPal Account

1. Cliente escolhe pagar com PayPal
2. Sistema cria PayPal payment
3. Cliente é redirecionado para PayPal
4. Cliente faz login e autoriza pagamento
5. Cliente é redirecionado de volta ao site
6. Sistema confirma pagamento via API PayPal
7. Sistema recebe confirmação via webhook

## Webhooks

### Endpoints

- **Stripe**: `POST /api/payments/webhook/stripe`
- **MercadoPago**: `POST /api/payments/webhook/mercadopago`
- **PayPal**: `POST /api/payments/webhook/paypal`
- **Genérico**: `POST /api/payments/webhook` (auto-detecção)

### Headers de Segurança

**Stripe:**

```
Stripe-Signature: t=1234567890,v1=signature
```

**MercadoPago:**

```
x-signature: ts=timestamp,v1=signature
```

**PayPal:**

```
PAYPAL-TRANSMISSION-ID: transmission-id
PAYPAL-CERT-ID: cert-id
PAYPAL-TRANSMISSION-SIG: signature
PAYPAL-TRANSMISSION-TIME: timestamp
```

## Segurança

- ✅ Validação de assinatura de webhooks
- ✅ Tokens de pagamento seguros
- ✅ Dados de cartão nunca armazenados
- ✅ HTTPS obrigatório em produção
- ✅ Logs de auditoria de pagamentos

## Testes

### Cartões de Teste (Stripe)

```
Visa: 4242424242424242
Mastercard: 5555555555554444
American Express: 378282246310005
```

### Cartões de Teste (MercadoPago)

```
Visa: 4170068810108020
Mastercard: 5031433215406351
American Express: 371449635398431
```

## Monitoramento

- Logs estruturados com `@Slf4j`
- Métricas de pagamentos via Actuator
- Health checks dos provedores
- Alertas para falhas de webhook

## Ambiente de Desenvolvimento

```bash
# Iniciar aplicação
./gradlew bootRun

# Testar PIX
curl -X POST http://localhost:8080/api/payments \
  -H "Content-Type: application/json" \
  -d '{
    "registrationId": 1,
    "amount": 50.00,
    "paymentMethod": "PIX"
  }'

# Testar cartão
curl -X POST http://localhost:8080/api/payments \
  -H "Content-Type: application/json" \
  -d '{
    "registrationId": 1,
    "amount": 50.00,
    "paymentMethod": "CREDIT_CARD",
    "cardToken": "tok_visa"
  }'

# Testar PayPal
curl -X POST http://localhost:8080/api/payments \
  -H "Content-Type: application/json" \
  -d '{
    "registrationId": 1,
    "amount": 50.00,
    "paymentMethod": "PAYPAL_ACCOUNT",
    "returnUrl": "http://localhost:3000/success",
    "cancelUrl": "http://localhost:3000/cancel"
  }'
```

## Integração Front-End

### Stripe + React

```bash
npm install @stripe/stripe-js @stripe/react-stripe-js
```

```javascript
import { loadStripe } from "@stripe/stripe-js";
import {
  Elements,
  CardElement,
  useStripe,
  useElements,
} from "@stripe/react-stripe-js";

const stripePromise = loadStripe("pk_test_...");

function CheckoutForm({ amount, registrationId }) {
  const stripe = useStripe();
  const elements = useElements();

  const handleSubmit = async (event) => {
    event.preventDefault();

    if (!stripe || !elements) return;

    // 1. Criar Payment Intent
    const response = await fetch("/api/payments", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        registrationId,
        amount,
        paymentMethod: "CREDIT_CARD",
        returnUrl: window.location.origin + "/success",
        cancelUrl: window.location.origin + "/cancel",
      }),
    });

    const { clientSecret } = await response.json();

    // 2. Confirmar pagamento
    const { error } = await stripe.confirmCardPayment(clientSecret, {
      payment_method: {
        card: elements.getElement(CardElement),
        billing_details: {
          name: "Cliente Teste",
        },
      },
    });

    if (error) {
      console.error("Erro no pagamento:", error);
    } else {
      console.log("Pagamento realizado com sucesso!");
      // Redirecionar para página de sucesso
    }
  };

  return (
    <form onSubmit={handleSubmit}>
      <CardElement />
      <button type="submit" disabled={!stripe}>
        Pagar R$ {amount}
      </button>
    </form>
  );
}

function App() {
  return (
    <Elements stripe={stripePromise}>
      <CheckoutForm amount={100.0} registrationId={1} />
    </Elements>
  );
}
```

### PayPal + JavaScript

```html
<script src="https://www.paypal.com/sdk/js?client-id=YOUR_CLIENT_ID&currency=BRL"></script>
```

```javascript
// 1. Criar PayPal payment
async function createPayPalPayment(amount, registrationId) {
  const response = await fetch("/api/payments", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      registrationId,
      amount,
      paymentMethod: "PAYPAL_ACCOUNT",
      returnUrl: window.location.origin + "/success",
      cancelUrl: window.location.origin + "/cancel",
    }),
  });

  return response.json();
}

// 2. Configurar botões PayPal
paypal
  .Buttons({
    createOrder: async function (data, actions) {
      const payment = await createPayPalPayment(100.0, 1);

      return actions.order.create({
        purchase_units: [
          {
            amount: {
              value: "100.00",
              currency_code: "BRL",
            },
          },
        ],
      });
    },

    onApprove: async function (data, actions) {
      // Confirmar pagamento no backend
      const response = await fetch(`/api/payments/${data.orderID}/confirm`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          PayerID: data.payerID,
          token: data.orderID,
        }),
      });

      if (response.ok) {
        window.location.href = "/success";
      }
    },

    onError: function (err) {
      console.error("Erro PayPal:", err);
      window.location.href = "/cancel";
    },
  })
  .render("#paypal-button-container");
```

### PIX + MercadoPago

```javascript
async function createPixPayment(amount, registrationId) {
  const response = await fetch("/api/payments", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      registrationId,
      amount,
      paymentMethod: "PIX",
      pixExpirationMinutes: 30,
    }),
  });

  const payment = await response.json();

  // Exibir QR Code
  document.getElementById("qr-code").innerHTML = `
    <img src="data:image/png;base64,${
      payment.qrCodeBase64
    }" alt="QR Code PIX" />
    <p>Código copia e cola:</p>
    <input type="text" value="${
      payment.pixCopyPaste
    }" readonly onclick="this.select()" />
    <p>Expira em: ${new Date(payment.expiresAt).toLocaleString()}</p>
  `;
}
```

## Próximos Passos

1. **Configurar webhooks** nos dashboards dos provedores
2. **Implementar persistência** de status de pagamentos
3. **Adicionar retry logic** para webhooks
4. **Implementar testes** automatizados
5. **Configurar monitoramento** de pagamentos
6. **Adicionar logs estruturados** para auditoria
