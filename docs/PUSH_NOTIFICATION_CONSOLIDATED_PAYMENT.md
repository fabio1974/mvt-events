# Push Notification - Pagamento Consolidado Pendente

## 📋 Especificação Técnica

### Tipo de Notificação
```
type: "consolidated_payment_reminder"
```

### Quando é Disparada
- **Horário**: 16:05 (todos os dias)
- **Scheduler**: `ConsolidatedPaymentReminderScheduler`
- **Condição**: Cliente (role=CLIENT) tem pagamento PIX PENDING

---

## 📦 Payload da Notificação

### Estrutura Recebida
```json
{
  "title": "💳 Pagamento pendente",
  "body": "Você tem R$ XX.XX em fretes pendentes. Pague via PIX agora e evite bloqueios.",
  "data": {
    "type": "consolidated_payment_reminder",
    "paymentId": "uuid-do-payment",
    "amount": "123.45",
    "pixQrCode": "00020126580014br.gov.bcb.pix...",
    "pixQrCodeUrl": "https://storage.googleapis.com/.../qrcode.png"
  }
}
```

### Campos do Data Payload

| Campo | Tipo | Descrição |
|-------|------|-----------|
| `type` | String | Identificador da notificação: `"consolidated_payment_reminder"` |
| `paymentId` | UUID | ID do payment no backend |
| `amount` | String | Valor total do pagamento (formato: "123.45") |
| `pixQrCode` | String | Código PIX copiável (texto longo) |
| `pixQrCodeUrl` | String | URL da imagem do QR code (PNG hospedado no GCS) |

---

## 🎯 Comportamento Esperado

### Quando usuário toca na notificação:

1. **Abrir tela de detalhe do pagamento** (sugestão: `PaymentDetailScreen` ou `PendingPixPaymentScreen`)

2. **Exibir informações:**
   - Valor total: `R$ {amount}`
   - QR Code (imagem): carregar `pixQrCodeUrl`
   - Código PIX copiável: `pixQrCode`
   - Botão "Copiar Código PIX"
   - Mensagem: "Pague via PIX até às 23:59 para evitar bloqueio"

3. **Funcionalidades:**
   - Copiar código PIX para clipboard
   - Abrir app bancário (se disponível)
   - Ver lista de entregas incluídas no pagamento (opcional: chamar `/api/payments/{paymentId}` para detalhes)

---

## 📱 Integração no App

### Handler de Notificação
```javascript
// Listener deve interceptar:
if (notification.data.type === 'consolidated_payment_reminder') {
  // Navegar para tela de detalhe do pagamento
  // Passar: paymentId, amount, pixQrCode, pixQrCodeUrl
}
```

### Tela de Destino
- **Nome sugerido**: `PaymentDetailScreen` ou `PendingPixPaymentScreen`
- **Parâmetros de navegação**: `{ paymentId, amount, pixQrCode, pixQrCodeUrl }`

---

## 🔄 Frequência
- **1x por dia às 16:05** para cada cliente com PIX pendente
- Não envia se o cliente não tiver token push ativo
- Não envia notificações duplicadas (1 push = 1 payment)

---

## ⚠️ Observações Importantes

1. **QR Code tem validade**: Expira às 23:59 do dia de geração
2. **Múltiplas entregas**: Um payment consolidado pode ter N entregas
3. **Fallback**: Se `pixQrCodeUrl` falhar no carregamento, sempre ter o `pixQrCode` copiável
4. **UX**: Facilitar ao máximo a cópia do código PIX (botão grande, feedback visual)

---

## 📞 Endpoint de Detalhes (se necessário)

Se precisar buscar mais informações do payment:

```
GET /api/payments/{paymentId}
```

**Response:**
```json
{
  "id": "uuid",
  "amount": 123.45,
  "status": "PENDING",
  "pixQrCode": "00020126...",
  "pixQrCodeUrl": "https://...",
  "deliveries": [
    {
      "id": "uuid",
      "pickupAddress": "...",
      "deliveryAddress": "...",
      "freight": 12.50
    }
  ],
  "createdAt": "2026-03-16T16:00:00-03:00",
  "expiresAt": "2026-03-16T23:59:59-03:00"
}
```

---

## ✅ Checklist de Implementação

- [ ] Handler de notificação capturando `type === 'consolidated_payment_reminder'`
- [ ] Tela de detalhe do pagamento criada
- [ ] Exibição da imagem do QR code (`pixQrCodeUrl`)
- [ ] Campo de texto copiável com o `pixQrCode`
- [ ] Botão "Copiar Código PIX" com feedback
- [ ] Navegação automática ao tocar na notificação
- [ ] Tratamento de erro caso `pixQrCodeUrl` falhe no carregamento
- [ ] Teste em device físico (iOS e Android)

---

**Contato Backend**: Se precisar de campos adicionais no payload, avisar para ajustar o scheduler.
