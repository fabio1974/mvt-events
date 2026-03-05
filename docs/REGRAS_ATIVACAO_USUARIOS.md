# Regras de Ativação e Bloqueio de Usuários

O sistema possui **dois campos booleanos** independentes no usuário:

| Campo | Propósito | Quem controla | Impacto |
|-------|-----------|---------------|---------|
| **`enabled`** | Requisitos mínimos de dados por role | Backend automático | FE/mobile bloqueia funcionalidades nas telas |
| **`blocked`** | Bloqueio administrativo/segurança | Admin manual | Impede login (Spring Security) |

> **Login**: usuário com `enabled=false` **consegue** fazer login. Usuário com `blocked=true` **NÃO** consegue.

---

## Campos no JWT e na API

Ambos os campos são retornados:

**No token JWT** (claims):
```json
{
  "enabled": true,
  "blocked": false
}
```

**Na resposta da API** (`GET /api/users`, `PUT /api/users/{id}`):
```json
{
  "id": "...",
  "name": "João",
  "role": "COURIER",
  "enabled": false,
  "blocked": false
}
```

---

## `enabled` — Requisitos Mínimos por Role

### Tabela de requisitos

| Role | Conta Bancária | Dados de Saque (Pagar.me) | Veículo | Tipo de Serviço | Meio de Pagamento |
|------|:-:|:-:|:-:|:-:|:-:|
| **ORGANIZER** | ✅ Obrigatório | ✅ Obrigatório | — | — | — |
| **COURIER** | ✅ Obrigatório | ✅ Obrigatório | ✅ Obrigatório | ✅ Obrigatório | — |
| **CUSTOMER** | — | — | — | — | ✅ Obrigatório ¹ |
| **CLIENT** | — | — | — | — | ✅ Obrigatório ¹ |
| **ADMIN** | — | — | — | — | — |

¹ Cartão ativo **OU** preferência PIX cadastrada (basta um dos dois)

### ORGANIZER

Não pode ter `enabled=true` sem:
1. **Conta bancária** — registro em `bank_accounts`
2. **Dados de saque** — `pagarme_recipient_id` preenchido E `pagarme_status = 'active'`

### COURIER

Não pode ter `enabled=true` sem:
1. **Conta bancária** — registro em `bank_accounts`
2. **Dados de saque** — `pagarme_recipient_id` preenchido E `pagarme_status = 'active'`
3. **Veículo** — pelo menos um registro em `vehicles`
4. **Tipo de serviço** — `service_type` preenchido (`DELIVERY`, `PASSENGER_TRANSPORT` ou `BOTH`)

### CUSTOMER / CLIENT

Não pode ter `enabled=true` sem **pelo menos um**:
- **Cartão ativo** — registro em `customer_cards` com `is_active = true`
- **Preferência PIX** — registro em `customer_payment_preferences` com `preferred_payment_type = 'PIX'`

### ADMIN

Sem restrições. Sempre pode ser `enabled=true`.

### Mensagens de erro (retornadas pela API ao tentar ativar)

| Role | Mensagem |
|------|----------|
| ORGANIZER | `"Organizer precisa ter conta bancária cadastrada para ser ativado"` |
| ORGANIZER | `"Organizer precisa ter dados de saque configurados no Pagar.me para ser ativado"` |
| COURIER | `"Courier precisa ter conta bancária cadastrada para ser ativado"` |
| COURIER | `"Courier precisa ter dados de saque configurados no Pagar.me para ser ativado"` |
| COURIER | `"Courier precisa ter veículo cadastrado para ser ativado"` |
| COURIER | `"Courier precisa ter tipo de serviço definido para ser ativado"` |
| CUSTOMER/CLIENT | `"Cliente precisa ter cartão ativo ou preferência PIX cadastrada para ser ativado"` |

---

## `blocked` — Bloqueio Administrativo

- Controlado manualmente pelo admin via `PUT /api/users/{id}` com `{ "blocked": true }`
- Quando `blocked=true`, o login retorna **403** com:
```json
{
  "error": "USER_BLOCKED",
  "message": "Sua conta está bloqueada. Entre em contato com o suporte."
}
```
- Não é alterado automaticamente pelo sistema (apenas por admin)

---

## Como o Mobile/FE Deve Usar

### Na tela de login
- Se receber `error: "USER_BLOCKED"` → mostrar mensagem de conta bloqueada
- Se login OK → decodificar JWT e salvar `enabled` e `blocked`

### Após login (com `enabled=false`)
- O usuário **entra no app normalmente**
- O mobile deve verificar `enabled` do JWT/user data
- Se `enabled=false` → bloquear funcionalidades específicas do role e redirecionar para tela de completar cadastro
- Usar o endpoint `GET /api/users/me/activation-status` para saber **o que está faltando**

### Fluxo recomendado

```
Login OK → JWT contém { enabled: false, blocked: false }
         → Mobile exibe tela: "Complete seu cadastro para começar"
         → Usuário preenche dados (veículo, banco, etc.)
         → Mobile chama GET /api/users/me/activation-status
         → Quando missing=[] → enabled vira true automaticamente no próximo update
```

---

## Endpoint: Status de Ativação

```
GET /api/users/me/activation-status
Authorization: Bearer <token>
```

### Resposta

```json
{
  "enabled": false,
  "role": "COURIER",
  "missing": ["vehicle", "bankAccount"],
  "messages": {
    "vehicle": "Cadastre um veículo",
    "bankAccount": "Cadastre sua conta bancária"
  },
  "suggested": ["defaultAddress"]
}
```

| Campo | Tipo | Descrição |
|-------|------|-----------|
| `enabled` | boolean | `true` se todos os requisitos estão ok |
| `role` | string | Role do usuário |
| `missing` | string[] | Lista de itens obrigatórios que faltam |
| `messages` | object | Mensagens amigáveis para cada item faltante |
| `suggested` | string[] | Itens sugeridos (não obrigatórios) |

### Valores possíveis em `missing`

| Valor | Role(s) | Significado |
|-------|---------|-------------|
| `bankAccount` | ORGANIZER, COURIER | Falta conta bancária |
| `transferSettings` | ORGANIZER, COURIER | Falta configuração de saque no Pagar.me |
| `vehicle` | COURIER | Falta veículo cadastrado |
| `serviceType` | COURIER | Falta tipo de serviço |
| `paymentMethod` | CUSTOMER, CLIENT | Falta cartão ativo ou preferência PIX |

---

## Verificação Automática no Startup

Ao iniciar a aplicação, o `UserIntegrityCheck` corrige dados legados — seta `enabled=false` para quem não atende os requisitos:

| Role | Condição para desativar |
|------|------------------------|
| ORGANIZER | Sem conta bancária OU sem `pagarme_recipient_id` OU `pagarme_status != 'active'` |
| COURIER | Sem conta bancária OU sem saque OU sem veículo OU sem `service_type` |
| CUSTOMER/CLIENT | Sem cartão ativo (`is_active=true`) **E** sem preferência PIX |

### Logs

```
[INTEGRITY] ORGANIZER: 1 desativado(s) — 1 sem conta bancária, 0 sem dados de saque
[INTEGRITY] COURIER: 3 desativado(s) — 1 sem conta bancária, 0 sem dados de saque, 1 sem veículo, 1 sem tipo de serviço
[INTEGRITY] CUSTOMER/CLIENT: 9 desativado(s) — sem cartão ativo e sem preferência PIX
```

Ou se tudo ok:
```
[INTEGRITY] Todos os usuários ativos possuem os requisitos obrigatórios
```

> O startup **NÃO** altera o campo `blocked`. Apenas `enabled`.

## Resumo Técnico

| Arquivo | Responsabilidade |
|---------|-----------------|
| `UserService.validateActivationRequirements()` | Valida enabled por role no update (ORGANIZER, COURIER, CUSTOMER, CLIENT) |
| `UserService.getActivationStatus()` | Retorna status informativo para mobile |
| `UserIntegrityCheck` | Startup: desativa usuários inconsistentes no banco |
| `ActivationStatusResponse` | DTO: enabled, role, missing, messages, suggested |
