# 🚚 API - Simulação de Frete

## Endpoint

```
POST /api/deliveries/simulate-freight
```

**Autenticação:** Bearer Token (JWT)

---

## Descrição

Simula o preço do frete baseado na distância, preço por km configurado, valor mínimo e zonas geográficas especiais. Usa a **mesma lógica** da criação de delivery.

### Fórmula de cálculo

```
frete = max(distância × preço/km, valor_mínimo) × (1 + taxa_zona / 100)
```

---

## Request

### Headers

| Header          | Valor                    |
|-----------------|--------------------------|
| Content-Type    | `application/json`       |
| Authorization   | `Bearer {token}`         |

### Body (JSON)

| Campo          | Tipo     | Obrigatório | Descrição                                      |
|----------------|----------|-------------|------------------------------------------------|
| `fromLatitude`  | `number` | Não         | Latitude da origem                             |
| `fromLongitude` | `number` | Não         | Longitude da origem                            |
| `fromAddress`   | `string` | Não         | Endereço de origem (texto livre)               |
| `toLatitude`    | `number` | **Sim**     | Latitude do destino                            |
| `toLongitude`   | `number` | **Sim**     | Longitude do destino                           |
| `toAddress`     | `string` | Não         | Endereço de destino (texto livre)              |
| `distanceKm`    | `number` | **Sim**     | Distância em km (calculada via Google Routes)  |

### Exemplo de Request

```json
{
  "fromLatitude": -3.854,
  "fromLongitude": -40.918,
  "fromAddress": "Ubajara - CE",
  "toLatitude": -3.729,
  "toLongitude": -40.991,
  "toAddress": "Tianguá - CE",
  "distanceKm": 22.5
}
```

> ⚠️ **IMPORTANTE:** O `distanceKm` deve ser calculado no **mobile** usando a **Google Routes API** (distância real por estrada), **não** a distância em linha reta.

---

## Response

### ✅ 200 - Sucesso

| Campo              | Tipo      | Descrição                                                |
|--------------------|-----------|----------------------------------------------------------|
| `distanceKm`       | `number`  | Distância informada (km)                                 |
| `pricePerKm`       | `number`  | Preço por km configurado (ex: R$ 1,00)                   |
| `baseFee`          | `number`  | Frete base: `distanceKm × pricePerKm`                   |
| `minimumFee`       | `number`  | Valor mínimo do frete configurado (ex: R$ 5,00)          |
| `minimumApplied`   | `boolean` | `true` se o valor mínimo foi aplicado (base < mínimo)    |
| `feeBeforeZone`    | `number`  | Frete antes da sobretaxa de zona                         |
| `zoneName`         | `string?` | Endereço/nome da zona especial (ou `null`)               |
| `zoneType`         | `string?` | Tipo da zona: `DANGER` ou `HIGH_INCOME` (ou `null`)      |
| `zoneFeePercentage`| `number`  | Percentual de sobretaxa da zona (ex: 20)                 |
| `zoneSurcharge`    | `number`  | Valor da sobretaxa calculada em R$                       |
| `totalShippingFee` | `number`  | **Valor final do frete** (o que será cobrado)            |
| `fromAddress`      | `string?` | Endereço de origem (eco do request)                      |
| `toAddress`        | `string?` | Endereço de destino (eco do request)                     |

### Exemplo de Response (sem zona especial)

```json
{
  "distanceKm": 22.50,
  "pricePerKm": 1.00,
  "baseFee": 22.50,
  "minimumFee": 5.00,
  "minimumApplied": false,
  "feeBeforeZone": 22.50,
  "zoneName": null,
  "zoneType": null,
  "zoneFeePercentage": 0,
  "zoneSurcharge": 0,
  "totalShippingFee": 22.50,
  "fromAddress": "Ubajara - CE",
  "toAddress": "Tianguá - CE"
}
```

### Exemplo de Response (com zona DANGER)

```json
{
  "distanceKm": 5.00,
  "pricePerKm": 1.00,
  "baseFee": 5.00,
  "minimumFee": 5.00,
  "minimumApplied": false,
  "feeBeforeZone": 5.00,
  "zoneName": "Rua Perigosa, Bairro X",
  "zoneType": "DANGER",
  "zoneFeePercentage": 20,
  "zoneSurcharge": 1.00,
  "totalShippingFee": 6.00,
  "fromAddress": "Centro",
  "toAddress": "Bairro X"
}
```

### Exemplo de Response (com valor mínimo aplicado)

```json
{
  "distanceKm": 2.00,
  "pricePerKm": 1.00,
  "baseFee": 2.00,
  "minimumFee": 5.00,
  "minimumApplied": true,
  "feeBeforeZone": 5.00,
  "zoneName": null,
  "zoneType": null,
  "zoneFeePercentage": 0,
  "zoneSurcharge": 0,
  "totalShippingFee": 5.00,
  "fromAddress": "Rua A",
  "toAddress": "Rua B"
}
```

---

## Erros

### 400 - Bad Request

**Sem distância:**
```json
{
  "error": "distanceKm é obrigatório e deve ser maior que zero"
}
```

**Sem coordenadas do destino:**
```json
{
  "error": "toLatitude e toLongitude são obrigatórios para cálculo de zona geográfica"
}
```

### 401 - Unauthorized
Token JWT ausente ou expirado.

### 500 - Internal Server Error
Erro inesperado no servidor.

---

## CURL para teste

```bash
curl -X POST 'http://{HOST}:8080/api/deliveries/simulate-freight' \
  -H 'Content-Type: application/json' \
  -H 'Authorization: Bearer {TOKEN}' \
  -d '{
    "fromLatitude": -3.854,
    "fromLongitude": -40.918,
    "fromAddress": "Ubajara - CE",
    "toLatitude": -3.729,
    "toLongitude": -40.991,
    "toAddress": "Tianguá - CE",
    "distanceKm": 22.5
  }'
```

---

## Fluxo sugerido no Mobile

```
1. Usuário seleciona origem e destino no mapa
2. Mobile chama Google Routes API → obtém distanceKm (rota real por estrada)
3. Mobile chama POST /api/deliveries/simulate-freight com:
   - coordenadas de origem e destino
   - distanceKm da Google Routes
4. Backend retorna detalhamento do frete
5. Mobile exibe para o usuário:
   - Distância: 22.5 km
   - Frete: R$ 22,50
   - (se houver zona) Sobretaxa: +R$ X,XX (zona de risco)
6. Usuário confirma → Mobile chama POST /api/deliveries (criar entrega)
```

---

## Endpoints relacionados

| Endpoint                              | Método | Descrição                          |
|---------------------------------------|--------|------------------------------------|
| `GET /api/cities/search?q=tiangua`    | GET    | Buscar cidade por nome (autocomplete, min 2 chars) |
| `GET /api/cities/{id}`                | GET    | Buscar cidade por ID               |
| `GET /api/site-configuration/active`  | GET    | Ver configuração ativa (preço/km, mínimo, taxas) |
| `POST /api/deliveries`               | POST   | Criar delivery (após simulação)    |
