# Apple Review v1.0.5 — Alterações Backend

## Contexto

Correções implementadas após rejeição da Apple App Store Review v1.0.5.  
A Apple exige: **(1)** contas de demonstração para teste do revisor, **(2)** funcionalidade de exclusão de conta do usuário.

---

## 1. Contas de Demonstração (V74)

**Migration:** `V74__apple_review_demo_accounts.sql`

Quatro contas de demonstração pré-configuradas com dados completos para que o revisor da Apple consiga testar todas as funcionalidades do app.

### Credenciais

| Papel      | Email                        | Senha     | Nome       |
|------------|------------------------------|-----------|------------|
| CUSTOMER   | demo.customer@zapi10.com     | Demo@123  | João Demo  |
| CLIENT     | demo.client@zapi10.com       | Demo@123  | Maria Demo |
| COURIER    | demo.courier@zapi10.com      | Demo@123  | Pedro Demo |
| ORGANIZER  | demo.organizer@zapi10.com    | Demo@123  | Ana Demo   |

### Dados criados por conta

- **Todos os usuários:** endereço padrão (São Paulo), preferências de pagamento (PIX)
- **COURIER (Pedro Demo):** conta bancária (Banco do Brasil), veículo (Honda CG 160 Fan), contrato de emprego com organização
- **ORGANIZER (Ana Demo):** conta bancária (Itaú), organização "Demo Express Entregas" (comissão 5%)
- **CLIENT (Maria Demo):** contrato com a organização Demo Express
- **5 entregas de amostra:** COMPLETED (2), IN_TRANSIT (1), PENDING (1), CANCELLED (1)
- **5 pagamentos vinculados:** PAID (2), PENDING (2), CANCELLED (1)

### Convenção

Contas demo seguem o padrão de email `demo.*@zapi10.com`.  
Esse padrão é usado para identificar e proteger as contas contra exclusão.

---

## 2. Exclusão de Conta — DELETE /api/users/me (V75)

**Migration:** `V75__add_deleted_at_to_users.sql`  
**Endpoint:** `DELETE /api/users/me`

### Comportamento

| Cenário | Resposta |
|---------|----------|
| Usuário autenticado (não-demo) | `200` — `{"message": "Conta excluída com sucesso"}` |
| Conta de demonstração | `400` — `{"error": "Contas de demonstração não podem ser excluídas"}` |
| Sem autenticação | `401` — `{"error": "Unauthorized"}` |

### O que acontece no soft-delete

| Campo | Antes | Depois |
|-------|-------|--------|
| `username` | email real | `deleted_<uuid>@removed.com` |
| `name` | nome real | `Usuário Removido` |
| `document_number` | CPF real | CPF válido gerado a partir do UUID |
| `phone_ddd` | DDD | `null` |
| `phone_number` | telefone | `null` |
| `enabled` | `true` | `false` |
| `blocked` | `false` | `true` |
| `deleted_at` | `null` | timestamp da exclusão |

O usuário **não consegue mais fazer login** após a exclusão.  
Os dados são **anonimizados** (não apagados) para manter integridade referencial com entregas e pagamentos.

---

## Arquivos Alterados

| Arquivo | Tipo | Descrição |
|---------|------|-----------|
| `V74__apple_review_demo_accounts.sql` | Migration | Cria 4 contas demo + dados relacionados |
| `V75__add_deleted_at_to_users.sql` | Migration | Adiciona coluna `deleted_at` na tabela `users` |
| `User.java` | Entidade | Campo `deletedAt` + método `isDemoAccount()` |
| `UserService.java` | Serviço | Método `softDeleteMyAccount()` + `generateDeletedCpf()` |
| `UserController.java` | Controller | Endpoint `DELETE /api/users/me` |

---

## Teste Rápido

```bash
# Login com conta demo
curl -s -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"demo.customer@zapi10.com","password":"Demo@123"}'

# Tentar excluir conta demo (deve falhar)
TOKEN="<token do login acima>"
curl -s -X DELETE http://localhost:8080/api/users/me \
  -H "Authorization: Bearer $TOKEN"
# → {"error": "Contas de demonstração não podem ser excluídas"}
```
