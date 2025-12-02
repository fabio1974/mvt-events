# Documentação de Endpoints - Sistema CRUD Frontend

## 📋 Índice
- [Metadata](#metadata)
- [Organizations (Grupos)](#organizations)
- [Users (Usuários)](#users)
- [Deliveries (Entregas)](#deliveries)
- [Courier Profiles (Perfis de Motoboy)](#courier-profiles)
- [Evaluations (Avaliações)](#evaluations)
- [Site Configuration (Configurações do Site)](#site-configuration)
- [Authentication](#authentication)

---

## 🔧 Metadata

### GET `/api/metadata`
Retorna metadata de todas as entidades + configurações do site.

**Response:**
```json
{
  "entities": {
    "organization": { /* EntityMetadata */ },
    "user": { /* EntityMetadata */ },
    "delivery": { /* EntityMetadata */ },
    "courierProfile": { /* EntityMetadata */ },
    "evaluation": { /* EntityMetadata */ }
  },
  "siteConfiguration": {
    "pricePerKm": 1.00,
    "organizerPercentage": 5.00,
    "platformPercentage": 10.00
  }
}
```

### GET `/api/metadata/{entityName}`
Retorna metadata de uma entidade específica.

**Exemplo:** `/api/metadata/delivery`

---

## 🏢 Organizations (Grupos)

### GET `/api/organizations`
Lista todas as organizações.

**Query Parameters:**
- `page` (opcional): número da página (default: 0)
- `size` (opcional): tamanho da página (default: 20)

**Response:**
```json
{
  "content": [
    {
      "id": 1,
      "name": "Padaria Central",
      "owner": {
        "id": "uuid",
        "name": "João Silva",
        "username": "joao@email.com"
      },
      "createdAt": "2025-11-22T10:00:00"
    }
  ],
  "totalElements": 100,
  "totalPages": 5,
  "number": 0
}
```

### GET `/api/organizations/{id}`
Busca organização por ID.

### POST `/api/organizations`
Cria nova organização.

**Request Body:**
```json
{
  "name": "Nome da Organização",
  "owner": {
    "id": "uuid-do-owner"
  }
}
```

### PUT `/api/organizations/{id}`
Atualiza organização existente.

**Request Body:** igual ao POST

### DELETE `/api/organizations/{id}`
Remove organização.

---

## 👥 Users (Usuários)

### GET `/api/users`
Lista todos os usuários.

**Query Parameters:**
- `page`, `size`: paginação
- `role` (opcional): filtrar por role (CLIENT, COURIER, ORGANIZER, ADMIN)
- `organizationId` (opcional): filtrar por organização

**Response:**
```json
{
  "content": [
    {
      "id": "uuid",
      "name": "Maria Santos",
      "username": "maria@email.com",
      "phone": "+5511999999999",
      "role": "CLIENT",
      "organization": {
        "id": 1,
        "name": "Padaria Central"
      },
      "isActive": true,
      "createdAt": "2025-11-22T10:00:00"
    }
  ]
}
```

### GET `/api/users/{id}`
Busca usuário por ID (UUID).

### POST `/api/users`
Cria novo usuário.

**Request Body:**
```json
{
  "name": "Nome Completo",
  "username": "email@exemplo.com",
  "password": "senha123",
  "phone": "+5511999999999",
  "role": "CLIENT",
  "organizationId": 1,
  "isActive": true
}
```

**Roles disponíveis:**
- `CLIENT`: Cliente (solicita entregas)
- `COURIER`: Motoboy (realiza entregas)
- `ORGANIZER`: Gerente (gerencia entregas da organização)
- `ADMIN`: Administrador (acesso total)

### PUT `/api/users/{id}`
Atualiza usuário existente.

### DELETE `/api/users/{id}`
Remove usuário.

---

## 📦 Deliveries (Entregas)

### GET `/api/deliveries`
Lista todas as entregas (com filtros).

**Query Parameters:**
- `page`, `size`: paginação
- `status` (opcional): PENDING, ACCEPTED, PICKED_UP, IN_TRANSIT, COMPLETED, CANCELLED
- `clientId` (opcional): UUID do cliente
- `courierId` (opcional): UUID do motoboy
- `organizationId` (opcional): ID da organização

**Response:**
```json
{
  "content": [
    {
      "id": 1,
      "client": {
        "id": "uuid",
        "name": "João Silva",
        "phone": "+5511999999999"
      },
      "courier": {
        "id": "uuid",
        "name": "Pedro Motoboy",
        "phone": "+5511888888888"
      },
      "organizer": {
        "id": "uuid",
        "name": "Maria Gerente",
        "phone": "+5511777777777"
      },
      "fromAddress": "Rua A, 123 - Centro, São Paulo - SP",
      "fromLatitude": -23.550520,
      "fromLongitude": -46.633308,
      "toAddress": "Av. B, 456 - Jardins, São Paulo - SP",
      "toLatitude": -23.561414,
      "toLongitude": -46.656270,
      "recipientName": "Ana Costa",
      "recipientPhone": "+5511666666666",
      "itemDescription": "Pães e doces",
      "totalAmount": 50.00,
      "shippingFee": 12.50,
      "status": "PENDING",
      "scheduledPickupAt": "2025-11-22T14:00:00",
      "createdAt": "2025-11-22T10:00:00",
      "acceptedAt": null,
      "pickedUpAt": null,
      "inTransitAt": null,
      "completedAt": null,
      "cancelledAt": null,
      "cancellationReason": null
    }
  ]
}
```

### GET `/api/deliveries/{id}`
Busca entrega por ID.

### POST `/api/deliveries`
Cria nova entrega.

**Request Body:**
```json
{
  "clientId": "uuid-do-cliente",
  "fromAddress": "Endereço de origem completo",
  "fromLatitude": -23.550520,
  "fromLongitude": -46.633308,
  "toAddress": "Endereço de destino completo",
  "toLatitude": -23.561414,
  "toLongitude": -46.656270,
  "recipientName": "Nome do destinatário",
  "recipientPhone": "+5511999999999",
  "itemDescription": "Descrição do item",
  "totalAmount": 50.00,
  "shippingFee": 12.50,
  "scheduledPickupAt": "2025-11-22T14:00:00"
}
```

**Regras de criação:**
- `CLIENT`: pode criar entregas apenas para si mesmo
- `ADMIN`: pode criar entregas para qualquer cliente
- `ORGANIZER` e `COURIER`: não podem criar entregas

### PUT `/api/deliveries/{id}`
Atualiza entrega existente (apenas se status = PENDING).

**Request Body:** campos editáveis (todos opcionais)
```json
{
  "fromAddress": "Novo endereço origem",
  "fromLatitude": -23.550520,
  "fromLongitude": -46.633308,
  "toAddress": "Novo endereço destino",
  "toLatitude": -23.561414,
  "toLongitude": -46.656270,
  "recipientName": "Novo destinatário",
  "recipientPhone": "+5511999999999",
  "itemDescription": "Nova descrição",
  "totalAmount": 60.00,
  "shippingFee": 15.00,
  "scheduledPickupAt": "2025-11-22T15:00:00"
}
```

**Permissões:**
- `CLIENT`: pode editar apenas suas próprias entregas PENDING
- `ADMIN`: pode editar qualquer entrega PENDING

### POST `/api/deliveries/{id}/accept`
Motoboy aceita uma entrega.

**Headers:**
```
Authorization: Bearer {token-do-courier}
```

**Comportamento:**
- Define `courier = usuário logado`
- Busca organização comum entre courier e client
- Define `organizer = owner da organização comum`
- Muda status para `ACCEPTED`
- Define `acceptedAt`

### POST `/api/deliveries/{id}/pickup`
Marca entrega como retirada.

**Comportamento:**
- Muda status para `PICKED_UP`
- Define `pickedUpAt`

### POST `/api/deliveries/{id}/in-transit`
Marca entrega em trânsito.

**Comportamento:**
- Muda status para `IN_TRANSIT`
- Define `inTransitAt`

### POST `/api/deliveries/{id}/complete`
Completa entrega.

**Comportamento:**
- Muda status para `COMPLETED`
- Define `completedAt`

### POST `/api/deliveries/{id}/cancel`
Cancela entrega.

**Request Body:**
```json
{
  "reason": "Motivo do cancelamento"
}
```

**Comportamento:**
- Muda status para `CANCELLED`
- Define `cancelledAt` e `cancellationReason`

---

## 🏍️ Courier Profiles (Perfis de Motoboy)

### GET `/api/courier-profiles`
Lista todos os perfis de motoboy.

**Response:**
```json
{
  "content": [
    {
      "id": 1,
      "user": {
        "id": "uuid",
        "name": "Pedro Motoboy"
      },
      "vehicleType": "MOTORCYCLE",
      "vehiclePlate": "ABC1234",
      "status": "AVAILABLE",
      "rating": 4.8
    }
  ]
}
```

**Vehicle Types:**
- `MOTORCYCLE`: Moto
- `BICYCLE`: Bicicleta
- `CAR`: Carro

**Status:**
- `AVAILABLE`: Disponível
- `BUSY`: Ocupado
- `OFFLINE`: Offline

### GET `/api/courier-profiles/{id}`
Busca perfil por ID.

### POST `/api/courier-profiles`
Cria perfil de motoboy.

**Request Body:**
```json
{
  "userId": "uuid-do-usuario",
  "vehicleType": "MOTORCYCLE",
  "vehiclePlate": "ABC1234"
}
```

### PUT `/api/courier-profiles/{id}`
Atualiza perfil.

---

## ⭐ Evaluations (Avaliações)

### GET `/api/evaluations`
Lista avaliações.

**Query Parameters:**
- `deliveryId` (opcional): filtrar por entrega
- `courierId` (opcional): filtrar por motoboy

**Response:**
```json
{
  "content": [
    {
      "id": 1,
      "delivery": {
        "id": 1
      },
      "courier": {
        "id": "uuid",
        "name": "Pedro Motoboy"
      },
      "rating": 5,
      "comment": "Excelente serviço!",
      "createdAt": "2025-11-22T10:00:00"
    }
  ]
}
```

### POST `/api/evaluations`
Cria avaliação para uma entrega.

**Request Body:**
```json
{
  "deliveryId": 1,
  "rating": 5,
  "comment": "Ótimo atendimento!"
}
```

**Validações:**
- Rating: 1 a 5
- Apenas o cliente da entrega pode avaliar
- Entrega deve estar COMPLETED
- Não pode avaliar duas vezes

---

## ⚙️ Site Configuration (Configurações do Site)

### GET `/api/site-configuration`
Retorna configuração ativa (público).

**Response:**
```json
{
  "id": 1,
  "pricePerKm": 1.00,
  "organizerPercentage": 5.00,
  "platformPercentage": 10.00,
  "isActive": true,
  "createdAt": "2025-11-22T10:00:00",
  "updatedAt": "2025-11-22T10:00:00",
  "updatedBy": "SYSTEM",
  "notes": "Configuração inicial"
}
```

### GET `/api/site-configuration/history`
Lista histórico de configurações (apenas ADMIN).

### POST `/api/site-configuration`
Cria nova configuração (desativa as anteriores - apenas ADMIN).

**Request Body:**
```json
{
  "pricePerKm": 1.50,
  "organizerPercentage": 7.00,
  "platformPercentage": 12.00,
  "notes": "Atualização de valores"
}
```

**Validação:**
- Soma de percentuais não pode exceder 100%
- Apenas ADMIN pode alterar

### GET `/api/site-configuration/{id}`
Busca configuração específica por ID (apenas ADMIN).

---

## 🔐 Authentication

### POST `/api/auth/login`
Faz login e retorna JWT token.

**Request Body:**
```json
{
  "username": "usuario@email.com",
  "password": "senha123"
}
```

**Response:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "user": {
    "id": "uuid",
    "name": "Nome do Usuário",
    "username": "usuario@email.com",
    "role": "CLIENT",
    "organizationId": 1
  }
}
```

### POST `/api/auth/register`
Registra novo usuário.

**Request Body:**
```json
{
  "name": "Nome Completo",
  "username": "email@exemplo.com",
  "password": "senha123",
  "phone": "+5511999999999",
  "role": "CLIENT"
}
```

---

## 📝 Notas Importantes

### Headers necessários
Todos os endpoints (exceto auth e metadata público) requerem:
```
Authorization: Bearer {seu-jwt-token}
Content-Type: application/json
```

### Paginação padrão
- `page`: 0 (primeira página)
- `size`: 20 itens por página

### Formato de datas
Todas as datas seguem ISO 8601: `YYYY-MM-DDTHH:mm:ss`

### Códigos de resposta HTTP
- `200 OK`: Sucesso
- `201 Created`: Criado com sucesso
- `400 Bad Request`: Dados inválidos
- `401 Unauthorized`: Não autenticado
- `403 Forbidden`: Sem permissão
- `404 Not Found`: Recurso não encontrado
- `500 Internal Server Error`: Erro no servidor

### Cálculo do frete
O valor do frete (`shippingFee`) deve ser calculado no frontend usando:
```javascript
const distanceKm = calculateDistance(fromLat, fromLng, toLat, toLng);
const shippingFee = distanceKm * siteConfiguration.pricePerKm;
```

### Percentuais de comissão
- **Gerente (organizer)**: recebe `organizerPercentage` do valor da entrega
- **Plataforma**: recebe `platformPercentage` do valor da entrega
- **Motoboy**: recebe o restante (100% - organizer% - platform%)

Exemplo com entrega de R$ 100,00:
- Gerente: R$ 5,00 (5%)
- Plataforma: R$ 10,00 (10%)
- Motoboy: R$ 85,00 (85%)
