# Tratamento de Erros de Pagamento - Mobile

## Estrutura de Resposta do Backend

Quando um pagamento com cartão de crédito **falha**, o backend retorna HTTP **200** com a seguinte estrutura no campo da order:

```json
{
  "id": "or_xQnDM3LtQRCVEodW",
  "status": "failed",
  "charges": [
    {
      "id": "ch_oNXmKA4IaCa8Y4QM",
      "status": "failed",
      "last_transaction": {
        "id": "tran_xxxxx",
        "status": "not_authorized",
        "success": false,
        "gateway_response": {
          "code": "200",
          "errors": []
        },
        "acquirer_message": "Transação não autorizada",
        "acquirer_name": "simulator",
        "acquirer_return_code": "05",
        "antifraud_response": {
          "status": "rejected",
          "score": 85,
          "recommendation": "deny",
          "reason": "High risk transaction"
        }
      }
    }
  ]
}
```

## Campos Importantes para o Usuário

### 1. Status da Order
- **`status: "failed"`** - Pagamento recusado

### 2. Last Transaction
Dentro de `charges[0].last_transaction`:

| Campo | Descrição | Exemplo |
|-------|-----------|---------|
| `status` | Status da transação | `"not_authorized"` |
| `success` | Se foi autorizada | `false` |
| `acquirer_message` | Mensagem da adquirente/bandeira | `"Transação não autorizada"` |
| `acquirer_return_code` | Código de retorno | `"05"`, `"51"`, `"57"` |
| `antifraud_response` | Detalhes do antifraude (pode ser null) | Ver abaixo |

### 3. Antifraud Response (quando presente)
```json
{
  "status": "rejected",
  "score": 85,
  "recommendation": "deny",
  "reason": "High risk transaction"
}
```

## Códigos de Retorno da Adquirente (acquirer_return_code)

| Código | Significado | Mensagem para o Usuário |
|--------|-------------|------------------------|
| `05` | Não autorizada (genérico) | "Pagamento não autorizado. Entre em contato com seu banco." |
| `51` | Saldo insuficiente | "Saldo insuficiente. Verifique seu limite ou use outro cartão." |
| `54` | Cartão vencido | "Cartão vencido. Verifique a validade ou use outro cartão." |
| `57` | Transação não permitida | "Transação não permitida para este cartão. Entre em contato com seu banco." |
| `65` | Limite de transações excedido | "Limite de transações excedido. Tente novamente mais tarde." |

## Status do Antifraude (quando presente)

| Status | Recomendação | Ação |
|--------|--------------|------|
| `approved` | `approve` | Pagamento aprovado pelo antifraude |
| `rejected` | `deny` | Pagamento bloqueado por suspeita de fraude |
| `under_review` | `review` | Em análise manual |

## Lógica de Exibição de Mensagem ao Usuário

### Prioridade de Mensagens

1. **Se `antifraud_response.status === "rejected"`**:
   ```
   ⚠️ Pagamento bloqueado por segurança
   
   Por motivos de segurança, não foi possível processar este pagamento.
   
   Sugestões:
   - Tente usar outro cartão
   - Entre em contato com seu banco
   - Verifique os dados do cartão
   ```

2. **Se `acquirer_return_code` está presente**, use a tabela de códigos acima

3. **Se apenas `acquirer_message` está disponível**, mostre a mensagem:
   ```
   ❌ Pagamento não autorizado
   
   [acquirer_message]
   
   Entre em contato com seu banco para mais informações.
   ```

4. **Fallback (nenhum campo disponível)**:
   ```
   ❌ Não foi possível processar o pagamento
   
   Por favor, tente novamente ou use outro método de pagamento.
   ```

## Exemplo de Implementação React Native

```javascript
function getPaymentErrorMessage(order) {
  if (order.status !== 'failed') {
    return null;
  }
  
  const charge = order.charges?.[0];
  if (!charge) {
    return 'Não foi possível processar o pagamento';
  }
  
  const tx = charge.last_transaction;
  if (!tx) {
    return 'Não foi possível processar o pagamento';
  }
  
  // 1. Verificar antifraude
  if (tx.antifraud_response?.status === 'rejected') {
    return {
      title: '⚠️ Pagamento bloqueado por segurança',
      message: 'Por motivos de segurança, não foi possível processar este pagamento.',
      suggestions: [
        'Tente usar outro cartão',
        'Entre em contato com seu banco',
        'Verifique os dados do cartão'
      ]
    };
  }
  
  // 2. Verificar código da adquirente
  const acquirerCode = tx.acquirer_return_code;
  const codeMessages = {
    '05': 'Pagamento não autorizado. Entre em contato com seu banco.',
    '51': 'Saldo insuficiente. Verifique seu limite ou use outro cartão.',
    '54': 'Cartão vencido. Verifique a validade ou use outro cartão.',
    '57': 'Transação não permitida para este cartão. Entre em contato com seu banco.',
    '65': 'Limite de transações excedido. Tente novamente mais tarde.'
  };
  
  if (acquirerCode && codeMessages[acquirerCode]) {
    return {
      title: '❌ Pagamento não autorizado',
      message: codeMessages[acquirerCode]
    };
  }
  
  // 3. Usar mensagem da adquirente
  if (tx.acquirer_message) {
    return {
      title: '❌ Pagamento não autorizado',
      message: `${tx.acquirer_message}\n\nEntre em contato com seu banco para mais informações.`
    };
  }
  
  // 4. Fallback
  return {
    title: '❌ Não foi possível processar o pagamento',
    message: 'Por favor, tente novamente ou use outro método de pagamento.'
  };
}
```

## Logs para Debug

Para ajudar no debug, sempre logar:
```javascript
console.log('Payment Failed Details:', {
  orderId: order.id,
  chargeId: charge.id,
  transactionStatus: tx.status,
  acquirerCode: tx.acquirer_return_code,
  acquirerMessage: tx.acquirer_message,
  antifraudStatus: tx.antifraud_response?.status,
  antifraudReason: tx.antifraud_response?.reason
});
```

## Notas Importantes

1. **Nunca expor detalhes técnicos** (IDs de transação, códigos internos) diretamente ao usuário
2. **Sempre oferecer alternativas**: sugerir outro cartão, contato com banco, etc.
3. **Antifraud rejections** são sensíveis - não detalhar demais para não ensinar fraudadores
4. **Ambiente Sandbox**: Cartões de teste podem simular diferentes cenários de rejeição

## Exemplo de Tela de Erro

```
┌─────────────────────────────────────┐
│  ⚠️ Pagamento bloqueado             │
│                                     │
│  Por motivos de segurança, não foi  │
│  possível processar este pagamento. │
│                                     │
│  O que você pode fazer:             │
│  • Tente usar outro cartão          │
│  • Verifique os dados do cartão     │
│  • Entre em contato com seu banco   │
│                                     │
│  [Tentar Outro Cartão]              │
│  [Voltar]                           │
└─────────────────────────────────────┘
```

## Contato com Backend

Se o erro persistir e o log não for claro, o mobile deve enviar ao backend:
- `orderId`
- `chargeId` 
- `transactionId`

Para que possamos investigar nos logs completos do Pagar.me.
