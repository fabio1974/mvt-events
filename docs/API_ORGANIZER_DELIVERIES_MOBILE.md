# API - Entregas do Gerente (Organizer)

## Visão Geral

Dois novos endpoints para o gerente logado listar suas entregas ativas e concluídas.  
O backend identifica o gerente automaticamente pelo Bearer token — **nenhum parâmetro de ID necessário**.

---

## Endpoints

### 1. Entregas Ativas

```
GET /api/deliveries/organizer/active
Authorization: Bearer <token_do_gerente>
```

Retorna entregas com status **`ACCEPTED`** ou **`IN_TRANSIT`**, ordenadas por `updatedAt DESC`.

**Exemplo de resposta:**
```json
[
  {
    "id": 123,
    "status": "IN_TRANSIT",
    "fromAddress": "Rua A, 100",
    "toAddress": "Rua B, 200",
    "recipientName": "João Silva",
    "recipientPhone": "11999999999",
    "totalAmount": 25.50,
    "distanceKm": 3.2,
    "createdAt": "2026-03-03T10:00:00",
    "updatedAt": "2026-03-03T10:30:00",
    "client": {
      "id": "uuid-do-client",
      "name": "Maria",
      "email": "maria@email.com"
    },
    "courier": {
      "id": "uuid-do-courier",
      "name": "Carlos Moto",
      "email": "carlos@email.com"
    },
    "organizer": {
      "id": "uuid-do-gerente",
      "name": "Gerente X",
      "email": "gerente@email.com"
    },
    "payments": []
  }
]
```

---

### 2. Entregas Concluídas

```
GET /api/deliveries/organizer/completed
Authorization: Bearer <token_do_gerente>
```

Retorna entregas com status **`COMPLETED`**, ordenadas por `completedAt DESC`.

**Exemplo de resposta:** mesmo formato acima, com `"status": "COMPLETED"` e `completedAt` preenchido.

---

### 3. Todas as Entregas (paginado) — já existia, agora corrigido

```
GET /api/deliveries/organizer?page=0&size=100&sort=createdAt,desc
Authorization: Bearer <token_do_gerente>
```

Retorna **todas** as entregas do gerente com suporte a paginação e filtros opcionais.

**Parâmetros opcionais:**

| Parâmetro | Tipo | Descrição |
|---|---|---|
| `page` | int | Página (começa em 0) |
| `size` | int | Itens por página |
| `sort` | string | Ex: `createdAt,desc` |
| `status` | string | `PENDING`, `ACCEPTED`, `IN_TRANSIT`, `COMPLETED`, `CANCELLED` |
| `startDate` | string | ISO 8601 ex: `2026-01-01T00:00:00` |
| `endDate` | string | ISO 8601 |

**Resposta:** objeto `Page` com campo `content` contendo a lista de entregas.

---

## Comparação com Courier (referência)

| Organizer (novo) | Courier (existente) |
|---|---|
| `GET /api/deliveries/organizer/active` | `GET /api/deliveries/courier/active` |
| `GET /api/deliveries/organizer/completed` | `GET /api/deliveries/courier/completed` |

O formato da resposta é **idêntico** ao do courier. Pode reaproveitar os mesmos modelos/DTOs.

---

## Observações

- O token deve ser de um usuário com role **`ORGANIZER`**.
- Não passar nenhum ID no body ou query — o backend usa o `userId` do JWT.
- Endpoints `/organizer/active` e `/organizer/completed` retornam `List` (não paginado), igual ao courier.
