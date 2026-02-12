# API - Simulação de Frete por Veículo e Taxa de Cartão

> **Data:** 11/02/2026 | **Versão:** v60-v61

---

## Resumo das Alterações

1. **Simulação de frete retorna preços para MOTO e AUTOMÓVEL** em um único objeto
2. **Taxa de cartão de crédito** é informada na simulação (acréscimo sobre o frete)
3. **Preferência de veículo** ao criar entrega: `MOTORCYCLE`, `CAR` ou `ANY`

---

## 1. POST `/api/deliveries/simulate-freight`

### Request (sem mudanças)

```json
{
  "fromLatitude": -23.5505,
  "fromLongitude": -46.6333,
  "fromAddress": "Av. Paulista, 1000 - São Paulo",
  "toLatitude": -23.5630,
  "toLongitude": -46.6543,
  "toAddress": "Rua Augusta, 500 - São Paulo",
  "distanceKm": 5.20
}
```

### Response (NOVO FORMATO)

```json
{
  "distanceKm": 5.20,
  "fromAddress": "Av. Paulista, 1000 - São Paulo",
  "toAddress": "Rua Augusta, 500 - São Paulo",
  "zoneName": null,
  "zoneType": null,
  "zoneFeePercentage": 0.00,
  "creditCardFeePercentage": 4.99,
  "motorcycle": {
    "vehicleType": "MOTORCYCLE",
    "vehicleLabel": "Moto",
    "pricePerKm": 1.00,
    "baseFee": 5.20,
    "minimumFee": 5.00,
    "minimumApplied": false,
    "feeBeforeZone": 5.20,
    "zoneSurcharge": 0.00,
    "totalShippingFee": 5.20,
    "creditCardFeePercentage": 4.99,
    "creditCardFeeAmount": 0.26,
    "totalWithCreditCardFee": 5.46
  },
  "car": {
    "vehicleType": "CAR",
    "vehicleLabel": "Automóvel",
    "pricePerKm": 2.00,
    "baseFee": 10.40,
    "minimumFee": 8.00,
    "minimumApplied": false,
    "feeBeforeZone": 10.40,
    "zoneSurcharge": 0.00,
    "totalShippingFee": 10.40,
    "creditCardFeePercentage": 4.99,
    "creditCardFeeAmount": 0.52,
    "totalWithCreditCardFee": 10.92
  }
}
```

### Campos por veículo (`motorcycle` / `car`)

| Campo | Tipo | Descrição |
|-------|------|-----------|
| `vehicleType` | String | `"MOTORCYCLE"` ou `"CAR"` |
| `vehicleLabel` | String | `"Moto"` ou `"Automóvel"` |
| `pricePerKm` | BigDecimal | Preço por km configurado para este veículo |
| `baseFee` | BigDecimal | `distanceKm × pricePerKm` |
| `minimumFee` | BigDecimal | Valor mínimo do frete (específico por veículo) |
| `minimumApplied` | Boolean | Se o mínimo foi aplicado |
| `feeBeforeZone` | BigDecimal | Frete antes da sobretaxa de zona |
| `zoneSurcharge` | BigDecimal | Valor da sobretaxa de zona |
| `totalShippingFee` | BigDecimal | **Frete final (sem taxa de cartão)** |
| `creditCardFeePercentage` | BigDecimal | % da taxa do cartão |
| `creditCardFeeAmount` | BigDecimal | Valor monetário da taxa do cartão |
| `totalWithCreditCardFee` | BigDecimal | **Frete final COM taxa de cartão** |

### Campos gerais (raiz)

| Campo | Tipo | Descrição |
|-------|------|-----------|
| `distanceKm` | BigDecimal | Distância informada |
| `fromAddress` | String | Endereço de origem |
| `toAddress` | String | Endereço de destino |
| `zoneName` | String? | Nome da zona especial (se houver) |
| `zoneType` | String? | `"DANGER"` ou `"HIGH_INCOME"` |
| `zoneFeePercentage` | BigDecimal | % da sobretaxa de zona |
| `creditCardFeePercentage` | BigDecimal | % da taxa do cartão (informativo geral) |

---

## 2. POST `/api/deliveries` (Criar Entrega)

### Novo campo: `preferredVehicleType`

```json
{
  "client": { "id": "uuid-do-cliente" },
  "fromAddress": "Av. Paulista, 1000",
  "fromLatitude": -23.5505,
  "fromLongitude": -46.6333,
  "toAddress": "Rua Augusta, 500",
  "toLatitude": -23.5630,
  "toLongitude": -46.6543,
  "distanceKm": 5.20,
  "recipientName": "João Silva",
  "recipientPhone": "11999999999",
  "itemDescription": "Pacote pequeno",
  "preferredVehicleType": "CAR"
}
```

### Valores aceitos para `preferredVehicleType`

| Valor | Descrição |
|-------|-----------|
| `MOTORCYCLE` | Somente entregadores de moto |
| `CAR` | Somente entregadores de automóvel |
| `ANY` | Qualquer veículo (sem preferência) — **padrão** |

> Se o campo for omitido ou inválido, o valor será `ANY`.

### Response da criação (novo campo)

O objeto `DeliveryResponse` agora inclui:

```json
{
  "id": 123,
  "status": "PENDING",
  "preferredVehicleType": "CAR",
  "shippingFee": 10.40,
  "distanceKm": 5.20,
  ...
}
```

---

## 3. Lógica de Cálculo do Frete na Criação

Quando uma delivery é criada, o frete (`shippingFee`) é calculado automaticamente:

- Se `preferredVehicleType = MOTORCYCLE` → usa `pricePerKm` (moto)
- Se `preferredVehicleType = CAR` → usa `carPricePerKm` (automóvel)
- Se `preferredVehicleType = ANY` → usa `pricePerKm` (moto, por padrão)

A taxa de cartão de crédito **NÃO** é adicionada ao `shippingFee` na criação — ela é aplicada pelo gateway de pagamento no momento da cobrança.

---

## 4. Fluxo Sugerido no Mobile

```
┌─────────────────────────────┐
│  1. Tela de Nova Entrega    │
│     - Informar origem       │
│     - Informar destino      │
│     - Calcular distância    │
└──────────┬──────────────────┘
           │
           ▼
┌─────────────────────────────┐
│  2. Chamar simulate-freight │
│     POST /simulate-freight  │
└──────────┬──────────────────┘
           │
           ▼
┌─────────────────────────────────────┐
│  3. Tela de Escolha de Veículo      │
│                                     │
│  ┌───────────────────────────────┐  │
│  │ 🏍️ Moto                      │  │
│  │ R$ 5,20                      │  │
│  │ (cartão: R$ 5,46)            │  │
│  └───────────────────────────────┘  │
│                                     │
│  ┌───────────────────────────────┐  │
│  │ 🚗 Automóvel                  │  │
│  │ R$ 10,40                     │  │
│  │ (cartão: R$ 10,92)           │  │
│  └───────────────────────────────┘  │
│                                     │
│  ┌───────────────────────────────┐  │
│  │ 🔄 Qualquer veículo          │  │
│  │ A partir de R$ 5,20          │  │
│  └───────────────────────────────┘  │
└──────────┬──────────────────────────┘
           │
           ▼
┌─────────────────────────────┐
│  4. Criar delivery com      │
│     preferredVehicleType    │
│     POST /api/deliveries    │
└─────────────────────────────┘
```

### Como exibir os preços

- **Sem cartão de crédito:** usar `totalShippingFee`
- **Com cartão de crédito:** usar `totalWithCreditCardFee`
- Para a opção "Qualquer veículo", mostre o menor preço: `motorcycle.totalShippingFee`

---

## 5. Configurações do Admin (SiteConfiguration)

Novos campos gerenciáveis via painel admin:

| Campo | Coluna DB | Default | Descrição |
|-------|-----------|---------|-----------|
| `pricePerKm` | `price_per_km` | R$ 1,00 | Preço por km para moto |
| `carPricePerKm` | `car_price_per_km` | R$ 2,00 | Preço por km para automóvel |
| `minimumShippingFee` | `minimum_shipping_fee` | R$ 5,00 | Frete mínimo para moto |
| `carMinimumShippingFee` | `car_minimum_shipping_fee` | R$ 8,00 | Frete mínimo para automóvel |
| `creditCardFeePercentage` | `credit_card_fee_percentage` | 0% | Taxa de cartão de crédito |

---

## 6. Migrações de Banco

- **V60**: Adiciona `car_price_per_km` e `credit_card_fee_percentage` em `site_configurations`
- **V61**: Adiciona `preferred_vehicle_type` em `deliveries` (default `'ANY'`)
- **V62**: Adiciona `car_minimum_shipping_fee` em `site_configurations` (default R$ 8,00)
