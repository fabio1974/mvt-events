# Transferência Automática - Cadastro de Conta Bancária

## Nova Flag: `automaticTransfer`

### 1. Criação (POST `/api/bank-accounts`)

Adicionado campo opcional `automaticTransfer` no payload de criação de conta bancária:

```json
{
  "user": {
    "id": "uuid-do-usuario"
  },
  "bankCode": "260",
  "bankName": "Nubank",
  "agency": "1234",
  "agencyDigit": "5",
  "accountNumber": "435355",
  "accountDigit": "7",
  "accountType": "CHECKING",
  "automaticTransfer": true  // ⬅️ NOVO CAMPO
}
```

### 2. Atualização (PUT `/api/bank-accounts/{userId}`)

A mesma flag pode ser enviada no PUT para **atualizar** a configuração:

```json
{
  "user": {
    "id": "uuid-do-usuario"
  },
  "bankCode": "260",
  "bankName": "Nubank",
  "agency": "1234",
  "agencyDigit": "8",
  "accountNumber": "435355",
  "accountDigit": "7",
  "accountType": "CHECKING",
  "automaticTransfer": false  // ⬅️ Mudando para manual
}
```

**Comportamento no PUT:**
- Se `automaticTransfer` mudar, o backend chama automaticamente o PATCH do Pagar.me para atualizar `transfer_settings`
- Se os dados bancários mudarem, chama o PATCH de `default-bank-account`
- Ambos podem ser atualizados na mesma requisição

### Comportamento

| Valor | Descrição | Pagar.me |
|-------|-----------|----------|
| `true` | Transferência automática **diária** habilitada | `transfer_enabled: true, transfer_interval: Daily` |
| `false` | Transferência **manual** (saldo fica retido) | `transfer_enabled: false` |
| `null` ou ausente | **Default: `true`** (diária) | `transfer_enabled: true, transfer_interval: Daily` |

### Recomendações UX

- **Checkbox no formulário**: "Habilitar transferência automática diária"
- **Default marcado**: `true` (melhor UX)
- **Tooltip**: "Receba automaticamente na sua conta todos os dias"
- **Edição**: Permitir alterar no PUT (usuário pode ativar/desativar depois)

### Backend

- **POST**: Cria recipient com `transfer_settings` configurado
- **PUT**: Se flag mudar, atualiza via PATCH `/recipients/{id}/transfer-settings`
- **Dados salvos**: Campo `automatic_transfer` na tabela `bank_accounts`

---

**Implementado em:** 28/12/2025  
**Endpoints:** `POST /api/bank-accounts`, `PUT /api/bank-accounts/{userId}`  
**Pagar.me Docs:** https://docs.pagar.me/reference/criar-recebedor
