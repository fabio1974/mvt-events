# API de Gerenciamento de Motoboys do Grupo

API para gerenciar os motoboys vinculados à organização do ORGANIZER logado.

---

## Base URL
```
https://mvt-events.onrender.com/api/users
```

## Autenticação
Todos os endpoints requerem token JWT no header:
```
Authorization: Bearer <token>
```

---

## 1. Listar Motoboys do Grupo

### `GET /my-couriers`

Retorna todos os motoboys vinculados à organização do ORGANIZER logado.

#### Response (200 OK)
```json
[
  {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "name": "João da Silva",
    "email": "joao@email.com",
    "phone": "(11) 987654321",
    "documentNumber": "123.456.789-00",
    "dateOfBirth": "1990-05-15",
    "gender": "MALE",
    "gpsLatitude": -23.550520,
    "gpsLongitude": -46.633308,
    "lastLocationUpdate": "2026-01-29T21:30:00",
    "isActive": true,
    "linkedAt": "2026-01-15T10:00:00",
    "pagarmeStatus": "active"
  }
]
```

#### cURL
```bash
curl -X GET 'https://mvt-events.onrender.com/api/users/my-couriers' \
  -H 'Authorization: Bearer <token>'
```

---

## 2. Buscar Motoboys para Adicionar (Typeahead)

### `GET /search/couriers`

Busca motoboys que **NÃO** pertencem ao grupo do ORGANIZER (para adicionar).

#### Query Parameters
| Param  | Tipo   | Obrigatório | Descrição |
|--------|--------|-------------|-----------|
| search | string | Não         | Termo de busca (nome ou email) |
| limit  | int    | Não         | Limite de resultados (default: 10) |

#### Response (200 OK)
```json
[
  {
    "id": "550e8400-e29b-41d4-a716-446655440001",
    "name": "Maria Santos",
    "email": "maria@email.com",
    "phone": "(21) 912345678"
  }
]
```

#### cURL
```bash
curl -X GET 'https://mvt-events.onrender.com/api/users/search/couriers?search=maria&limit=5' \
  -H 'Authorization: Bearer <token>'
```

---

## 3. Adicionar Motoboy ao Grupo

### `POST /my-couriers/{courierId}`

Adiciona um motoboy à organização do ORGANIZER logado.

#### Path Parameters
| Param     | Tipo | Descrição |
|-----------|------|-----------|
| courierId | UUID | ID do motoboy a adicionar |

#### Response (200 OK)
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440001",
  "name": "Maria Santos",
  "email": "maria@email.com",
  "phone": "(21) 912345678",
  "documentNumber": "987.654.321-00",
  "dateOfBirth": "1995-03-20",
  "gender": "FEMALE",
  "gpsLatitude": null,
  "gpsLongitude": null,
  "lastLocationUpdate": null,
  "isActive": true,
  "linkedAt": "2026-01-29T22:00:00",
  "pagarmeStatus": null
}
```

#### Erros (400 Bad Request)
```json
{ "error": "Motoboy já está vinculado à sua organização" }
{ "error": "Usuário não é um motoboy (COURIER)" }
{ "error": "Motoboy não encontrado" }
```

#### cURL
```bash
curl -X POST 'https://mvt-events.onrender.com/api/users/my-couriers/550e8400-e29b-41d4-a716-446655440001' \
  -H 'Authorization: Bearer <token>'
```

---

## 4. Remover Motoboy do Grupo

### `DELETE /my-couriers/{courierId}`

Remove um motoboy da organização do ORGANIZER logado.

#### Path Parameters
| Param     | Tipo | Descrição |
|-----------|------|-----------|
| courierId | UUID | ID do motoboy a remover |

#### Response (200 OK)
```json
{
  "message": "Motoboy removido do grupo com sucesso"
}
```

#### Erros (400 Bad Request)
```json
{ "error": "Motoboy não está vinculado à sua organização" }
{ "error": "Motoboy não encontrado" }
```

#### cURL
```bash
curl -X DELETE 'https://mvt-events.onrender.com/api/users/my-couriers/550e8400-e29b-41d4-a716-446655440001' \
  -H 'Authorization: Bearer <token>'
```

---

## Resumo dos Endpoints

| Método | Endpoint | Descrição |
|--------|----------|-----------|
| GET    | `/my-couriers` | Listar motoboys do grupo |
| GET    | `/search/couriers?search=X` | Buscar motoboys para adicionar |
| POST   | `/my-couriers/{courierId}` | Adicionar motoboy ao grupo |
| DELETE | `/my-couriers/{courierId}` | Remover motoboy do grupo |

---

## Campos do Motoboy (CourierForOrganizerResponse)

| Campo              | Tipo    | Descrição |
|--------------------|---------|-----------|
| id                 | UUID    | ID do motoboy |
| name               | string  | Nome completo |
| email              | string  | Email/username |
| phone              | string  | Telefone formatado (DDD) XXXXX-XXXX |
| documentNumber     | string  | CPF formatado |
| dateOfBirth        | string  | Data nascimento (YYYY-MM-DD) |
| gender             | string  | MALE, FEMALE, OTHER |
| gpsLatitude        | number  | Latitude GPS tempo real (rastreamento) |
| gpsLongitude       | number  | Longitude GPS tempo real (rastreamento) |
| lastLocationUpdate | string  | Timestamp última localização |
| isActive           | boolean | Contrato ativo com a organização |
| linkedAt           | string  | Data de vínculo com organização |
| pagarmeStatus      | string  | Status no Pagar.me (active/pending/null) |

---

## Campos da Busca (CourierSearchResponse)

| Campo | Tipo   | Descrição |
|-------|--------|-----------|
| id    | UUID   | ID do motoboy |
| name  | string | Nome completo |
| email | string | Email/username |
| phone | string | Telefone formatado |

---

## Exemplo TypeScript/React Native

```typescript
const API_URL = 'https://mvt-events.onrender.com/api/users';

// Listar motoboys do grupo
export const getMyCouriers = async (token: string) => {
  const response = await fetch(`${API_URL}/my-couriers`, {
    headers: { 'Authorization': `Bearer ${token}` }
  });
  return response.json();
};

// Buscar motoboys para adicionar
export const searchCouriers = async (token: string, search?: string) => {
  const params = search ? `?search=${encodeURIComponent(search)}` : '';
  const response = await fetch(`${API_URL}/search/couriers${params}`, {
    headers: { 'Authorization': `Bearer ${token}` }
  });
  return response.json();
};

// Adicionar motoboy ao grupo
export const addCourierToGroup = async (token: string, courierId: string) => {
  const response = await fetch(`${API_URL}/my-couriers/${courierId}`, {
    method: 'POST',
    headers: { 'Authorization': `Bearer ${token}` }
  });
  if (!response.ok) {
    const error = await response.json();
    throw new Error(error.error);
  }
  return response.json();
};

// Remover motoboy do grupo
export const removeCourierFromGroup = async (token: string, courierId: string) => {
  const response = await fetch(`${API_URL}/my-couriers/${courierId}`, {
    method: 'DELETE',
    headers: { 'Authorization': `Bearer ${token}` }
  });
  if (!response.ok) {
    const error = await response.json();
    throw new Error(error.error);
  }
  return response.json();
};
```

---

## Fluxo de Uso no Mobile

```
┌─────────────────────────────────────────────────────────────┐
│                   TELA DE GERENCIAR GRUPO                   │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  🔍 Buscar motoboy para adicionar...                        │
│  ┌─────────────────────────────────────────────────────┐    │
│  │ GET /search/couriers?search=...                     │    │
│  │ → Mostra lista de motoboys não vinculados           │    │
│  │ → Tap → POST /my-couriers/{id} → Adiciona           │    │
│  └─────────────────────────────────────────────────────┘    │
│                                                             │
│  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━  │
│                                                             │
│  👥 Meus Motoboys (GET /my-couriers)                        │
│  ┌─────────────────────────────────────────────────────┐    │
│  │ 👤 João da Silva          📍 Online    [🗑️ Remover] │    │
│  │    (11) 987654321                                   │    │
│  ├─────────────────────────────────────────────────────┤    │
│  │ 👤 Maria Santos           📍 Offline   [🗑️ Remover] │    │
│  │    (21) 912345678                                   │    │
│  └─────────────────────────────────────────────────────┘    │
│                                                             │
│  [🗑️ Remover] → DELETE /my-couriers/{id}                   │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```
