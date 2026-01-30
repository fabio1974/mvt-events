# API de Gerenciamento de Clientes do Grupo

API para gerenciar os clientes vinculados à organização do ORGANIZER logado.

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

## 1. Listar Clientes do Grupo

### `GET /my-clients`

Retorna todos os clientes vinculados à organização do ORGANIZER logado.

#### Response (200 OK)
```json
[
  {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "name": "Empresa ABC Ltda",
    "email": "contato@empresaabc.com",
    "phone": "(11) 987654321",
    "documentNumber": "12.345.678/0001-00",
    "dateOfBirth": null,
    "gender": null,
    "contractStatus": "ACTIVE",
    "isPrimary": true,
    "startDate": "2026-01-15",
    "endDate": null
  }
]
```

#### cURL
```bash
curl -X GET 'https://mvt-events.onrender.com/api/users/my-clients' \
  -H 'Authorization: Bearer <token>'
```

---

## 2. Buscar Clientes para Adicionar (Typeahead)

### `GET /search/clients`

Busca clientes que **NÃO** pertencem ao grupo do ORGANIZER (para adicionar).

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
    "name": "Restaurante XYZ",
    "email": "contato@restaurantexyz.com",
    "phone": "(21) 912345678"
  }
]
```

#### cURL
```bash
curl -X GET 'https://mvt-events.onrender.com/api/users/search/clients?search=restaurante&limit=5' \
  -H 'Authorization: Bearer <token>'
```

---

## 3. Adicionar Cliente ao Grupo

### `POST /my-clients/{clientId}`

Adiciona um cliente à organização do ORGANIZER logado.

#### Path Parameters
| Param    | Tipo | Descrição |
|----------|------|-----------|
| clientId | UUID | ID do cliente a adicionar |

#### Response (200 OK)
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440001",
  "name": "Restaurante XYZ",
  "email": "contato@restaurantexyz.com",
  "phone": "(21) 912345678",
  "documentNumber": "98.765.432/0001-00",
  "dateOfBirth": null,
  "gender": null,
  "contractStatus": "ACTIVE",
  "isPrimary": false,
  "startDate": "2026-01-30",
  "endDate": null
}
```

#### Erros (400 Bad Request)
```json
{ "error": "Cliente já está vinculado à sua organização" }
{ "error": "Usuário não é um cliente (CLIENT/CUSTOMER)" }
{ "error": "Cliente não encontrado" }
```

#### cURL
```bash
curl -X POST 'https://mvt-events.onrender.com/api/users/my-clients/550e8400-e29b-41d4-a716-446655440001' \
  -H 'Authorization: Bearer <token>'
```

---

## 4. Remover Cliente do Grupo

### `DELETE /my-clients/{clientId}`

Remove um cliente da organização do ORGANIZER logado.

#### Path Parameters
| Param    | Tipo | Descrição |
|----------|------|-----------|
| clientId | UUID | ID do cliente a remover |

#### Response (200 OK)
```json
{
  "message": "Cliente removido do grupo com sucesso"
}
```

#### Erros (400 Bad Request)
```json
{ "error": "Cliente não está vinculado à sua organização" }
{ "error": "Cliente não encontrado" }
```

#### cURL
```bash
curl -X DELETE 'https://mvt-events.onrender.com/api/users/my-clients/550e8400-e29b-41d4-a716-446655440001' \
  -H 'Authorization: Bearer <token>'
```

---

## Resumo dos Endpoints

| Método | Endpoint | Descrição |
|--------|----------|-----------|
| GET    | `/my-clients` | Listar clientes do grupo |
| GET    | `/search/clients?search=X` | Buscar clientes para adicionar |
| POST   | `/my-clients/{clientId}` | Adicionar cliente ao grupo |
| DELETE | `/my-clients/{clientId}` | Remover cliente do grupo |

---

## Campos do Cliente (ClientForOrganizerResponse)

| Campo          | Tipo    | Descrição |
|----------------|---------|-----------|
| id             | UUID    | ID do cliente |
| name           | string  | Nome completo/Razão Social |
| email          | string  | Email/username |
| phone          | string  | Telefone formatado (DDD) XXXXX-XXXX |
| documentNumber | string  | CPF/CNPJ formatado |
| dateOfBirth    | string  | Data nascimento (YYYY-MM-DD) - pode ser null |
| gender         | string  | MALE, FEMALE, OTHER - pode ser null |
| contractStatus | string  | ACTIVE, SUSPENDED, CANCELLED |
| isPrimary      | boolean | Se é contrato titular |
| startDate      | string  | Data de início do contrato (YYYY-MM-DD) |
| endDate        | string  | Data de fim do contrato (pode ser null) |

---

## Campos da Busca (ClientSearchResponse)

| Campo | Tipo   | Descrição |
|-------|--------|-----------|
| id    | UUID   | ID do cliente |
| name  | string | Nome completo/Razão Social |
| email | string | Email/username |
| phone | string | Telefone formatado |

---

## Exemplo TypeScript/React Native

```typescript
const API_URL = 'https://mvt-events.onrender.com/api/users';

// Listar clientes do grupo
export const getMyClients = async (token: string) => {
  const response = await fetch(`${API_URL}/my-clients`, {
    headers: { 'Authorization': `Bearer ${token}` }
  });
  return response.json();
};

// Buscar clientes para adicionar
export const searchClients = async (token: string, search?: string) => {
  const params = search ? `?search=${encodeURIComponent(search)}` : '';
  const response = await fetch(`${API_URL}/search/clients${params}`, {
    headers: { 'Authorization': `Bearer ${token}` }
  });
  return response.json();
};

// Adicionar cliente ao grupo
export const addClientToGroup = async (token: string, clientId: string) => {
  const response = await fetch(`${API_URL}/my-clients/${clientId}`, {
    method: 'POST',
    headers: { 'Authorization': `Bearer ${token}` }
  });
  if (!response.ok) {
    const error = await response.json();
    throw new Error(error.error);
  }
  return response.json();
};

// Remover cliente do grupo
export const removeClientFromGroup = async (token: string, clientId: string) => {
  const response = await fetch(`${API_URL}/my-clients/${clientId}`, {
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
│                   TELA DE GERENCIAR CLIENTES                │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  🔍 Buscar cliente para adicionar...                        │
│  ┌─────────────────────────────────────────────────────┐    │
│  │ GET /search/clients?search=...                      │    │
│  │ → Mostra lista de clientes não vinculados           │    │
│  │ → Tap → POST /my-clients/{id} → Adiciona            │    │
│  └─────────────────────────────────────────────────────┘    │
│                                                             │
│  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━  │
│                                                             │
│  👥 Meus Clientes (GET /my-clients)                         │
│  ┌─────────────────────────────────────────────────────┐    │
│  │ 🏢 Empresa ABC Ltda       ⭐ Titular  [🗑️ Remover]  │    │
│  │    (11) 987654321          Status: ACTIVE           │    │
│  ├─────────────────────────────────────────────────────┤    │
│  │ 🍴 Restaurante XYZ                    [🗑️ Remover]  │    │
│  │    (21) 912345678          Status: ACTIVE           │    │
│  └─────────────────────────────────────────────────────┘    │
│                                                             │
│  [🗑️ Remover] → DELETE /my-clients/{id}                    │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

---

## Diferenças entre Motoboy e Cliente

| Aspecto | Motoboy (my-couriers) | Cliente (my-clients) |
|---------|----------------------|---------------------|
| Tabela de contrato | `employment_contracts` | `client_contracts` |
| Role do usuário | `COURIER` | `CLIENT` ou `CUSTOMER` |
| Status do contrato | `isActive` (boolean) | `status` (ACTIVE/SUSPENDED/CANCELLED) |
| Campo de vínculo | `linkedAt` (timestamp) | `startDate` / `endDate` (dates) |
| Contrato titular | N/A | `isPrimary` (boolean) |
