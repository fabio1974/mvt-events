# 📦 API de Fluxo de Entregas (Deliveries)

## 🌐 Base URL
```
http://192.168.18.23:8080
```

## 🔐 Autenticação
Todos os endpoints requerem autenticação via Bearer Token no header:
```
Authorization: Bearer {seu_token_jwt}
```

---

## 📋 Endpoints de Fluxo da Entrega

### 1. 🎯 Aceitar Entrega (Accept)
**Status:** `PENDING` → `ACCEPTED`

```http
PATCH /api/deliveries/{id}/accept
Content-Type: application/json
Authorization: Bearer {token}

Body:
{
  "courierId": "6186c7af-2311-4756-bfc6-ce98bd31ed27"
}
```

**Descrição:** O courier aceita uma entrega disponível. Após aceitar, a entrega é atribuída ao motoboy.

---

### 2. 📦 Confirmar Coleta (Pickup)
**Status:** `ACCEPTED` → `PICKED_UP`

```http
PATCH /api/deliveries/{id}/pickup
Authorization: Bearer {token}
```

**Descrição:** O courier confirma que coletou o item no local de origem. Sem body necessário - usa o courier do token.

---

### 3. 🚚 Iniciar Transporte (Transit)
**Status:** `PICKED_UP` → `IN_TRANSIT`

```http
PATCH /api/deliveries/{id}/transit
Authorization: Bearer {token}
```

**Descrição:** O courier inicia o transporte do item para o destino. Sem body necessário - usa o courier do token.

---

### 4. ✅ Completar Entrega (Complete)
**Status:** `IN_TRANSIT` → `COMPLETED`

```http
PATCH /api/deliveries/{id}/complete
Authorization: Bearer {token}
```

**Descrição:** O courier confirma que entregou o item com sucesso no destino. Sem body necessário - usa o courier do token.

---

### 5. ❌ Cancelar Entrega (Cancel)
**Status:** `QUALQUER` → `PENDING` (sem courier)

```http
PATCH /api/deliveries/{id}/cancel?reason={motivo}
Authorization: Bearer {token}
```

**Descrição:** Cancela a entrega. O courier é removido e a entrega volta para PENDING. Requer um motivo (reason) como query parameter.

**Importante:** Só pode cancelar se NÃO estiver COMPLETED.

---

### 6. 🔄 Atualizar Status (Genérico)
**Status:** Qualquer transição válida

```http
PATCH /api/deliveries/{id}/status
Content-Type: application/json
Authorization: Bearer {token}

Body:
{
  "status": "IN_TRANSIT",
  "reason": "opcional - usado principalmente para cancelamento"
}
```

**Status válidos:**
- `PENDING`
- `ACCEPTED`
- `PICKED_UP`
- `IN_TRANSIT`
- `COMPLETED`
- `CANCELLED`

---

## 📄 Response Format (DeliveryResponse)

Todos os endpoints retornam o mesmo formato de resposta:

```json
{
  "id": 27,
  "createdAt": "2025-11-13T10:30:00",
  "status": "ACCEPTED",
  
  "client": {
    "id": "uuid-do-cliente",
    "name": "João Silva",
    "phone": "85999999999"
  },
  
  "courier": {
    "id": "uuid-do-motoboy",
    "name": "Motoboy1",
    "phone": "85997572919"
  },
  
  "organization": {
    "id": 1,
    "name": "Empresa XYZ"
  },
  
  "fromAddress": "Rua Origem, 123",
  "fromLatitude": -3.7319,
  "fromLongitude": -38.5267,
  
  "toAddress": "Rua Destino, 456",
  "toLatitude": -3.7419,
  "toLongitude": -38.5367,
  
  "recipientName": "Maria Santos",
  "recipientPhone": "85988888888",
  
  "itemDescription": "Documento importante",
  
  "totalAmount": 50.00,
  
  "scheduledPickupAt": null,
  "acceptedAt": "2025-11-13T10:35:00",
  "pickedUpAt": null,
  "inTransitAt": null,
  "completedAt": null,
  "cancelledAt": null,
  "cancellationReason": null
}
```

---

## 📊 Campos do Response

### Informações Básicas
| Campo | Tipo | Descrição |
|-------|------|-----------|
| `id` | Long | ID da entrega |
| `createdAt` | DateTime | Data/hora de criação |
| `status` | String | Status atual da entrega |

### Timestamps do Fluxo
| Campo | Tipo | Descrição |
|-------|------|-----------|
| `scheduledPickupAt` | DateTime | Data agendada para coleta |
| `acceptedAt` | DateTime | Quando foi aceita pelo courier |
| `pickedUpAt` | DateTime | Quando o item foi coletado |
| `inTransitAt` | DateTime | Quando iniciou o transporte |
| `completedAt` | DateTime | Quando foi completada |
| `cancelledAt` | DateTime | Quando foi cancelada |
| `cancellationReason` | String | Motivo do cancelamento |

### Cliente, Courier e Organização
| Campo | Tipo | Descrição |
|-------|------|-----------|
| `client` | Object | Dados do cliente (id, name, phone) |
| `courier` | Object | Dados do motoboy (id, name, phone) |
| `organization` | Object | Dados da organização (id, name) |

### Endereços
| Campo | Tipo | Descrição |
|-------|------|-----------|
| `fromAddress` | String | Endereço de origem |
| `fromLatitude` | Double | Latitude da origem |
| `fromLongitude` | Double | Longitude da origem |
| `toAddress` | String | Endereço de destino |
| `toLatitude` | Double | Latitude do destino |
| `toLongitude` | Double | Longitude do destino |

### Destinatário e Item
| Campo | Tipo | Descrição |
|-------|------|-----------|
| `recipientName` | String | Nome do destinatário |
| `recipientPhone` | String | Telefone do destinatário |
| `itemDescription` | String | Descrição do item |
| `totalAmount` | Decimal | Valor total da entrega |

---

## 🔄 Fluxo Completo da Entrega

```
1. PENDING (Aguardando aceitação)
   ↓ PATCH /accept (com courierId no body)
   
2. ACCEPTED (Aceita pelo motoboy)
   ↓ PATCH /pickup
   
3. PICKED_UP (Item coletado)
   ↓ PATCH /transit
   
4. IN_TRANSIT (Em trânsito)
   ↓ PATCH /complete
   
5. COMPLETED (Entregue com sucesso)
```

**Cancelamento pode ocorrer em qualquer etapa (exceto COMPLETED)**
```
QUALQUER STATUS
   ↓ PATCH /cancel?reason=motivo
   
PENDING (sem courier atribuído)
```

---

## 🚨 Validações e Regras

### Transições de Status Válidas
- `PENDING` → apenas `ACCEPTED`
- `ACCEPTED` → apenas `PICKED_UP`
- `PICKED_UP` → apenas `IN_TRANSIT`
- `IN_TRANSIT` → apenas `COMPLETED`
- `COMPLETED` → **não pode mudar**
- `CANCELLED` → **não pode mudar**

### Cancelamento
- ✅ Pode cancelar de: PENDING, ACCEPTED, PICKED_UP, IN_TRANSIT
- ❌ NÃO pode cancelar: COMPLETED
- Ao cancelar:
  - Remove o courier da entrega
  - Volta para status PENDING
  - Salva o motivo do cancelamento
  - Atualiza métricas do courier

### Atualização Automática
- `updatedAt` é atualizado automaticamente em toda mudança
- Timestamps específicos são preenchidos conforme o status muda
- Timestamps futuros são limpos quando há transição reversa

---

## 📱 Exemplos de Uso no App

### Aceitar uma entrega
```javascript
const acceptDelivery = async (deliveryId, courierId) => {
  const response = await fetch(`http://192.168.18.23:8080/api/deliveries/${deliveryId}/accept`, {
    method: 'PATCH',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`
    },
    body: JSON.stringify({ courierId })
  });
  return response.json();
};
```

### Confirmar coleta
```javascript
const confirmPickup = async (deliveryId) => {
  const response = await fetch(`http://192.168.18.23:8080/api/deliveries/${deliveryId}/pickup`, {
    method: 'PATCH',
    headers: {
      'Authorization': `Bearer ${token}`
    }
  });
  return response.json();
};
```

### Iniciar transporte
```javascript
const startTransit = async (deliveryId) => {
  const response = await fetch(`http://192.168.18.23:8080/api/deliveries/${deliveryId}/transit`, {
    method: 'PATCH',
    headers: {
      'Authorization': `Bearer ${token}`
    }
  });
  return response.json();
};
```

### Completar entrega
```javascript
const completeDelivery = async (deliveryId) => {
  const response = await fetch(`http://192.168.18.23:8080/api/deliveries/${deliveryId}/complete`, {
    method: 'PATCH',
    headers: {
      'Authorization': `Bearer ${token}`
    }
  });
  return response.json();
};
```

### Cancelar entrega
```javascript
const cancelDelivery = async (deliveryId, reason) => {
  const response = await fetch(`http://192.168.18.23:8080/api/deliveries/${deliveryId}/cancel?reason=${encodeURIComponent(reason)}`, {
    method: 'PATCH',
    headers: {
      'Authorization': `Bearer ${token}`
    }
  });
  return response.json();
};
```

---

## 🐛 Tratamento de Erros

### Códigos de Status HTTP
- `200 OK` - Sucesso
- `400 Bad Request` - Dados inválidos ou transição de status inválida
- `401 Unauthorized` - Token inválido ou ausente
- `403 Forbidden` - Sem permissão para a operação
- `404 Not Found` - Entrega não encontrada
- `500 Internal Server Error` - Erro interno do servidor

### Exemplos de Erros
```json
{
  "message": "Delivery não está pendente",
  "status": 400
}
```

```json
{
  "message": "De ACCEPTED só pode ir para PICKED_UP",
  "status": 400
}
```

---

## 📝 Notas Importantes

1. **Ordenação**: Todas as consultas de deliveries são ordenadas por `updatedAt DESC` (mais recentes primeiro)

2. **Lazy Loading**: Os relacionamentos (client, courier, organization) são carregados automaticamente no response

3. **Push Notifications**: O sistema envia notificações automáticas para os couriers quando uma nova entrega está disponível

4. **Métricas**: O sistema atualiza automaticamente as métricas do courier (total de entregas, completadas, canceladas)

5. **Token JWT**: O courier é identificado automaticamente pelo token, não precisa passar o ID em pickup, transit e complete

---

## 📞 Suporte

Para dúvidas ou problemas, entre em contato com a equipe de desenvolvimento.

**Versão da API:** 1.0  
**Última atualização:** 13/11/2025
