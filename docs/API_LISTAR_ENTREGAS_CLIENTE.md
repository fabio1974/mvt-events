# 📦 API - Listar Entregas do Cliente Logado

## Endpoint

```
GET /api/deliveries
```

**Autenticação:** Bearer Token (JWT)

---

## Descrição

Retorna as entregas do usuário logado (CLIENT/CUSTOMER) com paginação e filtros opcionais. O backend **identifica automaticamente** o usuário pelo token JWT e retorna apenas as entregas dele — **não precisa passar `clientId`**.

---

## Request

### Headers

| Header          | Valor                    |
|-----------------|--------------------------|
| Authorization   | `Bearer {token}`         |

### Query Params (todos opcionais)

| Parâmetro        | Tipo      | Descrição                                                  |
|------------------|-----------|-------------------------------------------------------------|
| `status`         | `string`  | Filtrar por status (ver valores abaixo)                     |
| `startDate`      | `ISO`     | Data inicial (ex: `2026-02-01T00:00:00`)                    |
| `endDate`        | `ISO`     | Data final (ex: `2026-02-28T23:59:59`)                      |
| `hasPayment`     | `boolean` | `true` = só com pagamento, `false` = só sem pagamento       |
| `page`           | `number`  | Número da página (começa em 0). Default: `0`                |
| `size`           | `number`  | Itens por página. Default: `20`                             |
| `sort`           | `string`  | Ordenação. Default: `updatedAt,desc`                        |

### Valores de `status`

| Status          | Descrição                              |
|-----------------|----------------------------------------|
| `PENDING`       | Aguardando motoboy aceitar             |
| `ACCEPTED`      | Motoboy aceitou                        |
| `PICKED_UP`     | Motoboy coletou o pedido               |
| `IN_TRANSIT`    | Em trânsito                            |
| `COMPLETED`     | Entrega concluída                      |
| `CANCELLED`     | Entrega cancelada                      |

### Exemplos de chamada

```
GET /api/deliveries
GET /api/deliveries?status=PENDING
GET /api/deliveries?status=COMPLETED&page=0&size=10
GET /api/deliveries?startDate=2026-02-01T00:00:00&endDate=2026-02-28T23:59:59
```

---

## Response

### ✅ 200 - Sucesso (Paginado)

```json
{
  "content": [ ... lista de deliveries ... ],
  "totalElements": 15,
  "totalPages": 2,
  "number": 0,
  "size": 10,
  "first": true,
  "last": false
}
```

### Objeto Delivery (cada item do `content`)

| Campo              | Tipo       | Descrição                                     |
|--------------------|------------|-----------------------------------------------|
| `id`               | `number`   | ID da entrega                                 |
| `createdAt`        | `ISO date` | Data de criação                               |
| `status`           | `string`   | Status atual da entrega                       |
| `fromAddress`      | `string`   | Endereço de origem                            |
| `fromLatitude`     | `number`   | Latitude da origem                            |
| `fromLongitude`    | `number`   | Longitude da origem                           |
| `fromCity`         | `string`   | Cidade de origem                              |
| `toAddress`        | `string`   | Endereço de destino                           |
| `toLatitude`       | `number`   | Latitude do destino                           |
| `toLongitude`      | `number`   | Longitude do destino                          |
| `toCity`           | `string`   | Cidade do destino                             |
| `recipientName`    | `string`   | Nome do destinatário                          |
| `recipientPhone`   | `string`   | Telefone do destinatário                      |
| `itemDescription`  | `string?`  | Descrição do item                             |
| `totalAmount`      | `number`   | Valor do pedido                               |
| `shippingFee`      | `number`   | Valor do frete calculado                      |
| `distanceKm`       | `number`   | Distância em km                               |
| `scheduledPickupAt`| `ISO date?`| Data/hora agendada para coleta                |
| `acceptedAt`       | `ISO date?`| Data/hora que motoboy aceitou                 |
| `pickedUpAt`       | `ISO date?`| Data/hora que motoboy coletou                 |
| `inTransitAt`      | `ISO date?`| Data/hora que saiu para entrega               |
| `completedAt`      | `ISO date?`| Data/hora que entregou                        |
| `cancelledAt`      | `ISO date?`| Data/hora do cancelamento                     |
| `cancellationReason`| `string?` | Motivo do cancelamento                        |
| `rating`           | `number?`  | Avaliação (1-5)                               |
| `hasEvaluation`    | `boolean?` | Se já foi avaliada                            |
| `notes`            | `string?`  | Observações                                   |
| `client`           | `object`   | Dados do cliente (ver abaixo)                 |
| `courier`          | `object?`  | Dados do motoboy (ver abaixo) — `null` se PENDING |
| `organizer`        | `object?`  | Dados do organizador                          |
| `partnership`      | `object?`  | Parceria associada                            |
| `payments`         | `array`    | Lista de pagamentos (id + status)             |

### Objeto `client` / `courier` / `organizer`

```json
{
  "id": "189c7d79-cb21-40c1-9b7c-006ebaa3289a",
  "name": "João Silva",
  "phone": "(88) 99999-1234",
  "gpsLatitude": -3.854,
  "gpsLongitude": -40.918
}
```

### Objeto `partnership`

```json
{
  "id": 1,
  "name": "Parceiro XYZ"
}
```

### Objeto `payments` (array)

```json
[
  { "id": 10, "status": "PAID" },
  { "id": 11, "status": "PENDING" }
]
```

---

## Exemplo de Response completo

```json
{
  "content": [
    {
      "id": 42,
      "createdAt": "2026-02-05T19:30:00",
      "status": "COMPLETED",
      "fromAddress": "Rua José Rufino Pereira, 243 - Centro, Ubajara - CE",
      "fromLatitude": -3.854,
      "fromLongitude": -40.918,
      "fromCity": "Ubajara",
      "toAddress": "Av. Cel. Francisco Cavalcante, 553 - Tianguá - CE",
      "toLatitude": -3.729,
      "toLongitude": -40.991,
      "toCity": "Tianguá",
      "recipientName": "Maria Souza",
      "recipientPhone": "(88) 98888-5678",
      "itemDescription": "Pizza Grande",
      "totalAmount": 45.00,
      "shippingFee": 22.50,
      "distanceKm": 22.50,
      "scheduledPickupAt": null,
      "acceptedAt": "2026-02-05T19:32:00",
      "pickedUpAt": "2026-02-05T19:45:00",
      "inTransitAt": "2026-02-05T19:46:00",
      "completedAt": "2026-02-05T20:15:00",
      "cancelledAt": null,
      "cancellationReason": null,
      "rating": 5,
      "hasEvaluation": true,
      "notes": null,
      "client": {
        "id": "189c7d79-cb21-40c1-9b7c-006ebaa3289a",
        "name": "Cliente 1",
        "phone": "(88) 3611-2345",
        "gpsLatitude": -3.854,
        "gpsLongitude": -40.918
      },
      "courier": {
        "id": "abc12345-...",
        "name": "Motoboy João",
        "phone": "(88) 99999-0000",
        "gpsLatitude": -3.730,
        "gpsLongitude": -40.990
      },
      "organizer": null,
      "partnership": null,
      "payments": [
        { "id": 10, "status": "PAID" }
      ]
    }
  ],
  "totalElements": 1,
  "totalPages": 1,
  "number": 0,
  "size": 20,
  "first": true,
  "last": true
}
```

---

## CURL para teste

```bash
# Todas as entregas do cliente logado
curl -X GET 'http://{HOST}:8080/api/deliveries' \
  -H 'Authorization: Bearer {TOKEN}'

# Filtrar por status PENDING
curl -X GET 'http://{HOST}:8080/api/deliveries?status=PENDING' \
  -H 'Authorization: Bearer {TOKEN}'

# Paginado: página 0, 5 itens por página
curl -X GET 'http://{HOST}:8080/api/deliveries?page=0&size=5' \
  -H 'Authorization: Bearer {TOKEN}'

# Filtrar por período
curl -X GET 'http://{HOST}:8080/api/deliveries?startDate=2026-02-01T00:00:00&endDate=2026-02-28T23:59:59' \
  -H 'Authorization: Bearer {TOKEN}'
```

---

## Fluxo sugerido no Mobile

```
1. Tela "Minhas Entregas":
   → GET /api/deliveries?page=0&size=20
   → Exibir lista com status, endereço destino, valor, data

2. Tabs por status:
   → "Pendentes":  GET /api/deliveries?status=PENDING
   → "Em andamento": GET /api/deliveries?status=IN_TRANSIT
   → "Concluídas": GET /api/deliveries?status=COMPLETED

3. Tela de detalhe:
   → Usar os dados já retornados na lista (não precisa endpoint separado)
   → Exibir mapa com origem/destino
   → Exibir dados do motoboy (courier) quando disponível
   → Exibir timeline: criado → aceito → coletado → em trânsito → entregue

4. Pull-to-refresh:
   → Recarregar a mesma chamada GET /api/deliveries

5. Paginação infinita:
   → Incrementar page: ?page=0, ?page=1, ?page=2...
   → Parar quando last=true
```

---

## Endpoints relacionados

| Endpoint                                     | Método | Descrição                                |
|----------------------------------------------|--------|------------------------------------------|
| `POST /api/deliveries`                       | POST   | Criar nova entrega                       |
| `GET /api/deliveries/{id}`                   | GET    | Detalhe de uma entrega específica        |
| `POST /api/deliveries/simulate-freight`      | POST   | Simular preço do frete antes de criar    |
| `GET /api/addresses/me/default`              | GET    | Endereço padrão do cliente (para origem) |
