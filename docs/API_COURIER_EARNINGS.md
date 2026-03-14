# API - Recebimentos do Courier (Motoboy)

## Endpoint

```
GET /api/couriers/me/earnings?recent={true|false}
```

## Descrição

Lista o histórico completo de recebimentos do courier logado. Retorna apenas corridas **COMPLETED** (finalizadas) com pagamento **PAID** (pago), mostrando o detalhamento da repartição de valores para cada corrida.

**Parâmetro opcional `recent`**: Se `true`, filtra apenas corridas recentes. O número de dias é definido no campo `deliveryHistoryDays` da configuração do site (padrão: 7 dias). Se `false` ou omitido, retorna todas as corridas.

---

## Autenticação

**Bearer Token** (obrigatório) - Apenas **COURIER**

```
Authorization: Bearer <token_jwt>
```

---

## Request

**Método:** `GET`  
**Headers:**
```
Authorization: Bearer <token_courier>
Content-Type: application/json
```

**Query Parameters:**

| Parâmetro | Tipo | Obrigatório | Descrição |
|-----------|------|-------------|-----------|
| `recent` | Boolean | Não | Se `true`, filtra corridas recentes (número de dias vem do `deliveryHistoryDays` da configuração, padrão: 7 dias). Se `false` ou omitido, retorna todas as corridas. |

**Body:** Nenhum

**Exemplos de URL:**
```
GET /api/couriers/me/earnings              # Todas as corridas
GET /api/couriers/me/earnings?recent=true  # Corridas recentes (padrão: 7 dias)
GET /api/couriers/me/earnings?recent=false # Todas as corridas (explícito)
```

---

## Response

### ✅ Sucesso (200 OK)

```json
{
  "totalDeliveries": 15,
  "totalEarnings": 1305.00,
  "deliveries": [
    {
      "deliveryId": 42,
      "completedAt": "2026-03-13T15:30:45",
      "fromAddress": "Av. Paulista, 1000 - Bela Vista, São Paulo",
      "toAddress": "Rua Augusta, 2500 - Consolação, São Paulo",
      "distanceKm": 5.2,
      "clientName": "João Silva",
      "deliveryType": "DELIVERY",
      "paymentId": 128,
      "totalAmount": 100.00,
      "paymentStatus": "PAID",
      "paymentMethod": "CREDIT_CARD",
      "courierAmount": 87.00,
      "courierPercentage": 87.00,
      "organizerAmount": 5.00,
      "organizerPercentage": 5.00,
      "organizerName": "Maria Santos",
      "platformAmount": 8.00,
      "platformPercentage": 8.00
    },
    {
      "deliveryId": 41,
      "completedAt": "2026-03-13T14:15:22",
      "fromAddress": "Shopping Iguatemi - Faria Lima, São Paulo",
      "toAddress": "Av. Rebouças, 3000 - Pinheiros, São Paulo",
      "distanceKm": 3.8,
      "clientName": "Cliente App",
      "deliveryType": "DELIVERY",
      "paymentId": 127,
      "totalAmount": 80.00,
      "paymentStatus": "PAID",
      "paymentMethod": "PIX",
      "courierAmount": 69.60,
      "courierPercentage": 87.00,
      "organizerAmount": 0.00,
      "organizerPercentage": 0.00,
      "organizerName": null,
      "platformAmount": 10.40,
      "platformPercentage": 13.00
    }
  ]
}
```

### 📊 Estrutura do Response

#### Campos Principais

| Campo | Tipo | Descrição |
|-------|------|-----------|
| `totalDeliveries` | Integer | Total de corridas completadas e pagas |
| `totalEarnings` | BigDecimal | Total ganho pelo courier (soma de todos os `courierAmount`) |
| `deliveries` | Array | Lista de corridas com detalhamento |

#### Campos de Cada Corrida (`deliveries[]`)

**Informações da Delivery:**

| Campo | Tipo | Descrição |
|-------|------|-----------|
| `deliveryId` | Long | ID da entrega |
| `completedAt` | String (ISO 8601) | Data/hora de conclusão |
| `fromAddress` | String | Endereço de origem |
| `toAddress` | String | Endereço de destino |
| `distanceKm` | BigDecimal | Distância em quilômetros |
| `clientName` | String | Nome do cliente que solicitou |
| `deliveryType` | String | Tipo: `DELIVERY` (entrega de objeto) ou `RIDE` (transporte de passageiro) |

**Informações do Pagamento:**

| Campo | Tipo | Descrição |
|-------|------|-----------|
| `paymentId` | Long | ID do pagamento |
| `totalAmount` | BigDecimal | Valor total da corrida (frete) |
| `paymentStatus` | String | Status do pagamento (sempre `PAID` neste endpoint) |
| `paymentMethod` | String | Método: `PIX` ou `CREDIT_CARD` |

**Repartição (Split):**

| Campo | Tipo | Descrição |
|-------|------|-----------|
| `courierAmount` | BigDecimal | **Valor que o courier recebeu** (87% do total) |
| `courierPercentage` | BigDecimal | Percentual do courier (geralmente 87%) |
| `organizerAmount` | BigDecimal | Valor que o organizer recebeu (5% se houver, 0 caso contrário) |
| `organizerPercentage` | BigDecimal | Percentual do organizer (5% se houver, 0 caso contrário) |
| `organizerName` | String | Nome do organizer (`null` se não houver) |
| `platformAmount` | BigDecimal | Valor que a plataforma recebeu |
| `platformPercentage` | BigDecimal | Percentual da plataforma (8% com organizer, 13% sem) |

### ❌ Erro - Não Autenticado (401 Unauthorized)

```json
{
  "error": "Token inválido ou expirado"
}
```

### ❌ Erro - Usuário Não é Courier (403 Forbidden)

Retorna status `403` sem body quando o usuário logado não possui role `COURIER`.

---

## Regras de Repartição

### 🔢 Percentuais Padrão

A repartição de valores segue a regra:

#### **Com Organizer (estabelecimento):**
- **Courier:** 87% do frete
- **Organizer:** 5% do frete
- **Plataforma:** 8% do frete

#### **Sem Organizer (cliente app):**
- **Courier:** 87% do frete
- **Plataforma:** 13% do frete (incorpora os 5% do organizer)

### 💡 Exemplo de Cálculo

**Corrida de R$ 100,00 com organizer:**
- Courier recebe: R$ 87,00 (87%)
- Organizer recebe: R$ 5,00 (5%)
- Plataforma recebe: R$ 8,00 (8%)

**Corrida de R$ 100,00 sem organizer:**
- Courier recebe: R$ 87,00 (87%)
- Plataforma recebe: R$ 13,00 (13%)

---

## Filtros Aplicados

O endpoint retorna apenas corridas que atendem **TODOS** os critérios:

1. ✅ **Status:** `COMPLETED` (entrega finalizada)
2. ✅ **Pagamento:** Status `PAID` (pagamento confirmado)
3. ✅ **Courier:** Corridas do courier logado
4. 📅 **Data (se recent=true):** Usa `deliveryHistoryDays` da configuração (padrão: 7 dias)

**Observação:** O número de dias para o filtro `recent` é configurado no painel admin (campo `deliveryHistoryDays` em Site Configuration).

---

## Ordenação

As corridas são retornadas ordenadas por **data de conclusão** (mais recente primeiro).

---

## Implementação Mobile

### Exemplo React Native / JavaScript:

```javascript
const fetchMyEarnings = async (recent = null) => {
  try {
    // Construir URL com parâmetro opcional
    let url = 'https://api.mvt-events.com/api/couriers/me/earnings';
    if (recent !== null) {
      url += `?recent=${recent}`;
    }

    const response = await fetch(url, {
      method: 'GET',
      headers: {
        'Authorization': `Bearer ${courierToken}`,
        'Content-Type': 'application/json'
      }
    });

    const data = await response.json();

    if (response.ok) {
      // Sucesso: exibir lista de recebimentos
      console.log(`Total ganho: R$ ${data.totalEarnings}`);
      console.log(`Total de corridas: ${data.totalDeliveries}`);
      
      data.deliveries.forEach(delivery => {
        console.log(`Corrida #${delivery.deliveryId}: R$ ${delivery.courierAmount}`);
      });
    } else {
      // Erro: exibir mensagem
      Alert.alert('Erro', 'Não foi possível carregar os recebimentos');
    }
  } catch (error) {
    Alert.alert('Erro', 'Erro de conexão');
  }
};

// Uso:
fetchMyEarnings();        // Todas as corridas
fetchMyEarnings(true);    // Corridas recentes (padrão: 7 dias da config)
fetchMyEarnings(false);   // Todas as corridas (explícito)
```

### Exemplo de UI Recomendada:

```
┌─────────────────────────────────────┐
│  💰 Meus Recebimentos               │
├─────────────────────────────────────┤
│                                     │
│  Total Ganho: R$ 1.305,00          │
│  Total de Corridas: 15             │
│                                     │
├─────────────────────────────────────┤
│  ▼ Corrida #42 - R$ 87,00          │
│    ✓ Pago via Cartão               │
│    📍 Paulista → Augusta            │
│    📅 13/03/2026 15:30             │
│    👤 João Silva                    │
│                                     │
│    Detalhamento:                    │
│    • Você: R$ 87,00 (87%)          │
│    • Organizer: R$ 5,00 (5%)       │
│    • Plataforma: R$ 8,00 (8%)      │
│    • Total: R$ 100,00              │
├─────────────────────────────────────┤
│  ▼ Corrida #41 - R$ 69,60          │
│    ✓ Pago via PIX                  │
│    📍 Shopping → Rebouças           │
│    📅 13/03/2026 14:15             │
│    👤 Cliente App                   │
│                                     │
│    Detalhamento:                    │
│    • Você: R$ 69,60 (87%)          │
│    • Plataforma: R$ 10,40 (13%)    │
│    • Total: R$ 80,00               │
└─────────────────────────────────────┘
```

---

## Casos de Uso

### 📱 Para App Mobile (Courier)

1. **Tela "Meus Ganhos"**
   - Exibir total ganho no topo
   - Listar corridas pagas com valor recebido
   - Permitir expandir para ver detalhamento
   - Toggle simples: **"Recentes"** (recent=true) / **"Todas"** (recent=false)

2. **Observação sobre o Filtro "Recentes"**
   - O número de dias é configurado no backend (campo `deliveryHistoryDays`)
   - Padrão: 7 dias (última semana)
   - Administradores podem ajustar via painel admin

3. **Exportação (Futuro)**
   - Gerar PDF/Excel dos recebimentos
   - Enviar por email

---

## Notas Importantes

- 💰 **Apenas corridas pagas** - Só aparecem corridas com pagamento `PAID`
- ✅ **Apenas finalizadas** - Só aparecem corridas com status `COMPLETED`
- 🔒 **Segurança** - Cada courier vê apenas seus próprios recebimentos
- 📊 **Transparência** - Detalhamento completo da repartição de valores
- 🕐 **Tempo real** - Dados atualizados a cada requisição

---

## Testando

### cURL:
```bash
curl -X GET 'http://localhost:8080/api/couriers/me/earnings' \
  -H 'Authorization: Bearer SEU_TOKEN_COURIER' \
  -H 'Content-Type: application/json'
```

### Resposta esperada:
```json
{
  "totalDeliveries": 2,
  "totalEarnings": 156.60,
  "deliveries": [...]
}
```

---

## Próximas Melhorias

- [ ] Adicionar filtro por período (query params `startDate`, `endDate`)
- [ ] Adicionar filtro por método de pagamento
- [ ] Adicionar paginação para couriers com muitas corridas
- [ ] Endpoint para exportar em PDF/Excel
- [ ] Gráfico de evolução de ganhos ao longo do tempo
