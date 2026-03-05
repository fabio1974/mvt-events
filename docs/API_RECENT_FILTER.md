# Filtro `recent` — Entregas e Pagamentos

Parâmetro opcional `recent=true` disponível nos endpoints de listagem de entregas e pagamentos. Quando ativado, retorna apenas registros dentro de uma janela de dias configurável no painel admin (padrão: **7 dias**).

---

## Endpoints com suporte ao filtro

### Entregas

| Método | Endpoint | Campo de corte |
|--------|----------|----------------|
| GET | `/api/deliveries` | `startDate >= hoje - delivery_history_days` |
| GET | `/api/deliveries/organizer` | `startDate >= hoje - delivery_history_days` |
| GET | `/api/deliveries/organizer/completed` | `completedAt >= hoje - delivery_history_days` |
| GET | `/api/deliveries/courier/completed` | `completedAt >= hoje - delivery_history_days` |

### Pagamentos

| Método | Endpoint | Campo de corte |
|--------|----------|----------------|
| GET | `/api/payments` | `createdAt >= hoje - payment_history_days` |
| GET | `/api/payments/organizer` | `createdAt >= hoje - payment_history_days` |

---

## Como usar

Adicionar `?recent=true` na query string:

```
GET /api/deliveries?recent=true
GET /api/deliveries/organizer?recent=true
GET /api/deliveries/organizer/completed?recent=true
GET /api/deliveries/courier/completed?recent=true

GET /api/payments?recent=true
GET /api/payments/organizer?recent=true
```

Sem o parâmetro (ou `recent=false`), o comportamento permanece o mesmo de antes — **sem filtro de data**.

---

## Comportamento

- `recent=false` (padrão) → sem corte de data, retorna todos os registros normalmente
- `recent=true` → aplica corte: só retorna registros dos últimos N dias
- O valor de N é lido de `site_configurations`:
  - `delivery_history_days` → para entregas (padrão: **7**)
  - `payment_history_days` → para pagamentos (padrão: **7**)
- O admin pode alterar esses valores pelo painel sem necessidade de novo deploy

---

## Combinação com outros filtros

O parâmetro `recent=true` é **combinável** com os demais filtros já existentes:

```
GET /api/deliveries?recent=true&status=COMPLETED
GET /api/payments?recent=true&page=0&size=20
```

### Prioridade em entregas (`/api/deliveries`)
Se `recent=true` for passado **sem** `startDate`, o sistema define `startDate = hoje - delivery_history_days` automaticamente.  
Se `startDate` for passado explicitamente junto com `recent=true`, o `startDate` explícito **prevalece**.

---

## Recomendação de uso no mobile

- Usar `recent=true` ao abrir a tela principal de histórico (carregamento inicial)
- Usar `recent=false` ou omitir o parâmetro ao aplicar filtros customizados de data pelo usuário
- Isso evita payload grande no carregamento inicial e melhora performance

---

## Campos de data relevantes nas respostas

### Entregas
```json
{
  "id": 123,
  "startDate": "2026-02-25T10:00:00",
  "completedAt": "2026-02-25T11:30:00",
  ...
}
```

### Pagamentos
```json
{
  "id": 456,
  "createdAt": "2026-02-25T10:05:00",
  ...
}
```
