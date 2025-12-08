# 📦 Entrega: Constraint de Pagamento Único + Documentação FE

**Data**: 04/12/2025  
**Epic**: Sistema de Pagamentos  
**Story**: Prevenir pagamentos duplicados

---

## 🎯 O que foi feito?

### 1. ✅ Documentação para Frontend
📄 **Arquivo**: `docs/PAYMENT_ACTIVE_STATUS.md`

Documento completo explicando:
- O que é um pagamento ativo (PENDING ou COMPLETED)
- Como o FE deve verificar se uma delivery pode receber pagamento
- Exemplos de código TypeScript/React
- Fluxos e cenários de erro
- Testes recomendados

**Para o time de FE**: Leiam este documento! 📖

### 2. ✅ Constraint no Banco de Dados
📄 **Arquivo**: `src/main/resources/db/migration/V9__add_active_payment_constraint.sql`

Migration que cria:
- Função `is_payment_active()` - verifica se pagamento está ativo
- Índice único parcial - garante apenas UM pagamento ativo por delivery

**Proteção em nível de BD**: Impossível ter 2 pagamentos ativos! 🔒

### 3. ✅ Documentação Técnica
📄 **Arquivo**: `docs/PAYMENT_ACTIVE_CONSTRAINT_TECHNICAL.md`

Documento técnico explicando:
- Como a constraint funciona
- Cenários permitidos e bloqueados
- Testes de validação SQL
- Impacto na aplicação
- Performance

**Para o time de Backend/DevOps**: Referência completa! 📚

---

## 🔑 Conceitos Principais

### Pagamento Ativo
```
PENDING    → Cliente pode pagar (QR Code válido)
COMPLETED  → Cliente já pagou
```

### Pagamento Inativo
```
FAILED     → Pode criar novo pagamento
CANCELLED  → Pode criar novo pagamento  
REFUNDED   → Pode criar novo pagamento
```

---

## 🛡️ Defesa em Camadas

```
┌─────────────────────────────────────┐
│  1. Frontend (UI)                    │
│  Desabilita checkbox se tem          │
│  pagamento ativo                     │
└──────────────┬──────────────────────┘
               │
               ▼
┌─────────────────────────────────────┐
│  2. Backend (Validação)              │
│  PaymentService valida antes de      │
│  criar pagamento                     │
│  Retorna erro amigável               │
└──────────────┬──────────────────────┘
               │
               ▼
┌─────────────────────────────────────┐
│  3. Banco de Dados (Constraint)      │
│  Partial Unique Index bloqueia       │
│  inserções inválidas                 │
└─────────────────────────────────────┘
```

---

## 📊 Campo `payments` no Response

```json
{
  "id": 123,
  "status": "COMPLETED",
  "payments": [
    {
      "id": 456,
      "status": "PENDING"  ← Delivery aguardando pagamento
    }
  ]
}
```

**Frontend pode**:
- Verificar se `payments` tem algum item com status `PENDING` ou `COMPLETED`
- Desabilitar seleção se houver pagamento ativo
- Mostrar badge visual

---

## 🚀 Próximos Passos

### Frontend
1. ✅ Ler `docs/PAYMENT_ACTIVE_STATUS.md`
2. ✅ Implementar verificação `hasActivePayment()`
3. ✅ Desabilitar checkboxes conforme regra
4. ✅ Mostrar badges visuais de status
5. ✅ Testar cenários descritos no doc

### Backend
1. ✅ Rodar migration V9 (próximo restart)
2. ✅ Validar que constraint funciona
3. ✅ Monitorar logs para erros de constraint

### DevOps
1. ✅ Aplicar migration em staging primeiro
2. ✅ Testar cenários de duplicação
3. ✅ Aplicar em produção

---

## 📁 Arquivos Criados

```
docs/
  ├── PAYMENT_ACTIVE_STATUS.md              ← PARA O FRONTEND
  └── PAYMENT_ACTIVE_CONSTRAINT_TECHNICAL.md ← PARA BACKEND/DEVOPS

src/main/resources/db/migration/
  └── V9__add_active_payment_constraint.sql  ← MIGRATION
```

---

## 🎓 Para Saber Mais

- `docs/PAYMENT_ACTIVE_STATUS.md` - **Leia primeiro!** 📖
- `docs/PAYMENT_ACTIVE_CONSTRAINT_TECHNICAL.md` - Detalhes técnicos
- `src/main/java/com/mvt/mvt_events/service/PaymentService.java` - Validação de negócio

---

## 📞 Dúvidas?

- **Frontend**: Leia `PAYMENT_ACTIVE_STATUS.md`
- **Backend**: Leia `PAYMENT_ACTIVE_CONSTRAINT_TECHNICAL.md`
- **Slack**: #backend-zapi10

---

**🎉 Pronto para usar! A constraint será aplicada no próximo restart da aplicação.**
