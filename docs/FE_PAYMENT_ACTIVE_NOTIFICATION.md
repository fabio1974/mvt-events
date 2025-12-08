# 📧 Para: Time de Frontend

**De:** Backend Team  
**Data:** 04/12/2025  
**Assunto:** ✅ Nova Feature: Campo `payments` em Deliveries + Validação de Pagamento Único

---

## 🎯 TL;DR

A partir de agora, cada **delivery** retorna um array `payments` com status, e o sistema **garante** que cada delivery terá **no máximo 1 pagamento ativo** (PENDING ou COMPLETED).

---

## 📦 O que mudou?

### 1. Novo campo no response de deliveries

```json
{
  "id": 123,
  "status": "COMPLETED",
  "totalAmount": 50.00,
  "payments": [           ← NOVO!
    {
      "id": 456,
      "status": "PENDING"
    }
  ]
}
```

### 2. Regra de negócio

**Pagamento Ativo** = `PENDING` ou `COMPLETED`

- ✅ Delivery **SEM** pagamento ativo → pode criar novo pagamento
- ❌ Delivery **COM** pagamento ativo → **NÃO pode** criar novo pagamento

---

## 💻 O que o FE precisa fazer?

### 1. Verificar se delivery tem pagamento ativo

```typescript
function hasActivePayment(delivery: Delivery): boolean {
  return delivery.payments?.some(p => 
    p.status === 'PENDING' || p.status === 'COMPLETED'
  ) ?? false;
}
```

### 2. Desabilitar checkbox se tiver pagamento ativo

```typescript
<Checkbox 
  disabled={hasActivePayment(delivery) || delivery.status !== 'COMPLETED'}
  // ...
/>
```

### 3. Mostrar badge visual

```tsx
{hasActivePayment(delivery) ? (
  <Badge color="yellow">⏳ Aguardando Pagamento</Badge>
) : delivery.payments?.some(p => p.status === 'COMPLETED') ? (
  <Badge color="green">✅ Pago</Badge>
) : (
  <Badge color="gray">Sem pagamento</Badge>
)}
```

---

## 📚 Documentação Completa

👉 **LEIA AQUI**: `docs/PAYMENT_ACTIVE_STATUS.md`

O documento contém:
- ✅ Exemplos de código TypeScript/React
- ✅ Todos os cenários e fluxos
- ✅ Mensagens de erro do backend
- ✅ Testes recomendados

---

## ❓ FAQ

**P: O que acontece se tentar criar pagamento para delivery que já tem pagamento ativo?**  
R: Backend retorna erro 500 com mensagem clara:
```json
{
  "message": "❌ Já existe um pagamento PENDENTE (ID: 456) para as entregas: 1, 2, 3..."
}
```

**P: Posso criar novo pagamento depois que o anterior falhou?**  
R: ✅ Sim! Status `FAILED`, `CANCELLED` e `REFUNDED` não bloqueiam.

**P: Preciso atualizar alguma API call?**  
R: ❌ Não! O campo `payments` já vem automaticamente em `GET /api/deliveries`.

---

## 🚀 Quando entra em produção?

- ✅ Já está em **staging**
- 📅 Produção: **próxima release** (aguardando deploy)

---

## 🧪 Como testar?

1. Liste deliveries: `GET /api/deliveries?status=COMPLETED`
2. Verifique o campo `payments` no response
3. Tente criar pagamento para uma delivery que já tem pagamento PENDING
4. Deve retornar erro amigável

---

## 📞 Dúvidas?

- 📖 **Leia primeiro**: `docs/PAYMENT_ACTIVE_STATUS.md`
- 💬 **Slack**: #backend-zapi10
- ✉️ **Email**: backend@zapi10.com

---

**Happy coding! 🎉**
