# 🚗 API de Veículos - Guia Rápido Mobile

## Base URL
```
http://192.168.18.75:8080
```

## Headers Obrigatórios
```
Authorization: Bearer {seu_token_jwt}
Content-Type: application/json
```

---

## 📋 Listar Meus Veículos

```http
GET /api/vehicles/me
```

**Resposta:**
```json
[
  {
    "id": 2,
    "type": "MOTORCYCLE",
    "plate": "ABV5678",
    "brand": "HONDA",
    "model": "GC160",
    "color": "ROXO",
    "year": "2025",
    "isActive": true,
    "ownerName": "Fábio Motoboy",
    "ownerId": "6e8104d7-285e-4a80-9f13-857200d27987"
  }
]
```

---

## ➕ Cadastrar Novo Veículo

```http
POST /api/vehicles
```

**Body:**
```json
{
  "type": "MOTORCYCLE",
  "plate": "ABC1234",
  "brand": "HONDA",
  "model": "CG 160",
  "color": "VERMELHO",
  "year": "2024"
}
```

**Importante:**
- **Todo veículo novo** é automaticamente definido como ativo (principal)
- Se você já tiver um veículo ativo, ele será desativado automaticamente
- Isso garante que sempre há apenas 1 veículo ativo por vez

**Cores disponíveis:**
`BRANCO`, `PRETO`, `PRATA`, `CINZA`, `VERMELHO`, `AZUL`, `VERDE`, `AMARELO`, `LARANJA`, `MARROM`, `BEGE`, `DOURADO`, `ROSA`, `ROXO`, `VINHO`, `FANTASIA`, `OUTROS`

**Tipos:**
- `MOTORCYCLE` - Moto
- `CAR` - Carro

---

## ✏️ Atualizar Veículo

```http
PUT /api/vehicles/{id}
```

**Body:**
```json
{
  "type": "MOTORCYCLE",
  "plate": "ABC1234",
  "brand": "YAMAHA",
  "model": "FACTOR 150",
  "color": "AZUL",
  "year": "2024"
}
```

---

## 🔄 Definir Veículo Ativo (Principal)

```http
PUT /api/vehicles/{id}/set-active
```

Este endpoint:
- Desativa todos os seus outros veículos
- Ativa apenas o veículo selecionado
- Apenas **1 veículo ativo** por usuário (garantido pelo banco)

**Resposta:**
```json
{
  "id": 2,
  "isActive": true,
  ...
}
```

---

## 🔍 Buscar Veículo Ativo

```http
GET /api/vehicles/me/active
```

Retorna o veículo que está marcado como ativo/principal.

---

## 🗑️ Desativar Veículo

```http
DELETE /api/vehicles/{id}
```

Soft delete - veículo continua no banco mas fica inativo.

---

## ♻️ Reativar Veículo

```http
PUT /api/vehicles/{id}/reactivate
```

**Comportamento:**
- Desativa **automaticamente** todos os seus outros veículos
- Ativa o veículo selecionado como principal
- Tudo em uma única transação (atômico)

**Observação:** Este endpoint funciona de forma similar ao `/set-active`, mas é específico para veículos que estavam inativos.

---

## 🚨 Regras Importantes

1. **Apenas 1 veículo ativo por usuário** - garantido pela constraint do banco
2. **Todo veículo novo é ativado automaticamente** ao cadastrar (desativa os outros)
3. **Placa deve ser única** no sistema
4. **Trocar veículo ativo**: use `/set-active` ou `/reactivate` - ambos desativam os outros automaticamente
5. **Todos os endpoints respeitam a transação atômica** - mudanças são aplicadas de forma completa ou não são aplicadas

---

## 📱 Exemplo de Uso no Mobile

```javascript
// 1. Cadastrar primeiro veículo (será ativo automaticamente)
const response = await fetch('http://192.168.18.75:8080/api/vehicles', {
  method: 'POST',
  headers: {
    'Authorization': `Bearer ${token}`,
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({
    type: 'MOTORCYCLE',
    plate: 'ABC1234',
    brand: 'HONDA',
    model: 'CG 160',
    color: 'VERMELHO',
    year: '2024'
  })
});

// 2. Listar meus veículos
const vehicles = await fetch('http://192.168.18.75:8080/api/vehicles/me', {
  headers: {
    'Authorization': `Bearer ${token}`
  }
}).then(r => r.json());

// 3. Trocar veículo ativo
await fetch(`http://192.168.18.75:8080/api/vehicles/${vehicleId}/set-active`, {
  method: 'PUT',
  headers: {
    'Authorization': `Bearer ${token}`
  }
});
```

---

## ❌ Erros Comuns

### 401 Unauthorized
Token expirado ou inválido. Faça login novamente.

### 403 Forbidden
Você está tentando modificar um veículo que não é seu.

### 409 Conflict
Placa já cadastrada no sistema.
