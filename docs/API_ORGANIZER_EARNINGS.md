# API - Recebimentos do Organizer (Empresa)

## Endpoint

```
GET /api/organizers/me/earnings?recent={true|false}
```

## Descrição

Lista o histórico completo de recebimentos do organizer logado. Retorna apenas corridas **COMPLETED** (finalizadas) com pagamento **PAID** (pago), onde o organizer participou, mostrando o detalhamento da repartição de valores para cada corrida.

**Parâmetro opcional `recent`**: Se `true`, filtra apenas corridas recentes. O número de dias é definido no campo `deliveryHistoryDays` da configuração do site (padrão: 7 dias). Se `false` ou omitido, retorna todas as corridas.

---

## Autenticação

**Bearer Token** (obrigatório) - Apenas **ORGANIZER**

```
Authorization: Bearer <token_jwt>
```

---

## Request

**Método:** `GET`  
**Headers:**
```
Authorization: Bearer <token_organizer>
Content-Type: application/json
```

**Query Parameters:**

| Parâmetro | Tipo | Obrigatório | Descrição |
|-----------|------|-------------|-----------|
| `recent` | Boolean | Não | Se `true`, filtra corridas recentes (número de dias vem do `deliveryHistoryDays` da configuração, padrão: 7 dias). Se `false` ou omitido, retorna todas as corridas. |

**Body:** Nenhum

**Exemplos de URL:**
```
GET /api/organizers/me/earnings              # Todas as corridas
GET /api/organizers/me/earnings?recent=true  # Corridas recentes (padrão: 7 dias)
GET /api/organizers/me/earnings?recent=false # Todas as corridas (explícito)
```

---

## Response

### ✅ Sucesso (200 OK)

```json
{
  "totalDeliveries": 42,
  "totalEarnings": 210.00,
  "deliveries": [
    {
      "deliveryId": 42,
      "completedAt": "2026-03-13T15:30:45",
      "fromAddress": "Av. Paulista, 1000 - Bela Vista, São Paulo",
      "toAddress": "Rua Augusta, 2500 - Consolação, São Paulo",
      "distanceKm": 5.2,
      "clientName": "João Silva",
      "deliveryType": "DELIVERY",
      "courierName": "Pedro Oliveira",
      "paymentId": 128,
      "totalAmount": 100.00,
      "paymentStatus": "PAID",
      "paymentMethod": "CREDIT_CARD",
      "courierAmount": 87.00,
      "courierPercentage": 87.00,
      "organizerAmount": 5.00,
      "organizerPercentage": 5.00,
      "platformAmount": 8.00,
      "platformPercentage": 8.00
    },
    {
      "deliveryId": 38,
      "completedAt": "2026-03-13T14:15:22",
      "fromAddress": "Shopping Iguatemi - Faria Lima, São Paulo",
      "toAddress": "Av. Rebouças, 3000 - Pinheiros, São Paulo",
      "distanceKm": 3.8,
      "clientName": "Cliente App",
      "deliveryType": "RIDE",
      "courierName": "Carlos Santos",
      "paymentId": 124,
      "totalAmount": 80.00,
      "paymentStatus": "PAID",
      "paymentMethod": "PIX",
      "courierAmount": 69.60,
      "courierPercentage": 87.00,
      "organizerAmount": 4.00,
      "organizerPercentage": 5.00,
      "platformAmount": 6.40,
      "platformPercentage": 8.00
    }
  ]
}
```

### 📊 Estrutura do Response

#### Campos Principais

| Campo | Tipo | Descrição |
|-------|------|-----------|
| `totalDeliveries` | Integer | Total de corridas completadas e pagas onde o organizer participou |
| `totalEarnings` | BigDecimal | Total ganho pelo organizer (soma de todos os `organizerAmount`) |
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
| `courierName` | String | Nome do motoboy que realizou a entrega |

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
| `courierAmount` | BigDecimal | Valor que o courier recebeu (87% do total) |
| `courierPercentage` | BigDecimal | Percentual do courier (geralmente 87%) |
| `organizerAmount` | BigDecimal | **Valor que o organizer recebeu** (5% do total) |
| `organizerPercentage` | BigDecimal | Percentual do organizer (5%) |
| `platformAmount` | BigDecimal | Valor que a plataforma recebeu (8% com organizer) |
| `platformPercentage` | BigDecimal | Percentual da plataforma (8%) |

---

### ⚠️ Erros

#### 401 Unauthorized
Token inválido ou expirado.

#### 403 Forbidden
Usuário não é ORGANIZER.

#### 404 Not Found
SiteConfiguration não encontrada (configuração do sistema não existe).

---

## Implementação Mobile

### Exemplo React Native / JavaScript:

```javascript
const fetchMyEarnings = async (recent = null) => {
  try {
    // Construir URL com parâmetro opcional
    let url = 'https://api.mvt-events.com/api/organizers/me/earnings';
    if (recent !== null) {
      url += `?recent=${recent}`;
    }

    const response = await fetch(url, {
      method: 'GET',
      headers: {
        'Authorization': `Bearer ${organizerToken}`,
        'Content-Type': 'application/json'
      }
    });

    const data = await response.json();

    if (response.ok) {
      // Sucesso: exibir lista de recebimentos
      console.log(`Total ganho: R$ ${data.totalEarnings}`);
      console.log(`Total de corridas: ${data.totalDeliveries}`);
      
      data.deliveries.forEach(delivery => {
        console.log(`\n📦 Corrida #${delivery.deliveryId}`);
        console.log(`   De: ${delivery.fromAddress}`);
        console.log(`   Para: ${delivery.toAddress}`);
        console.log(`   Distância: ${delivery.distanceKm} km`);
        console.log(`   Cliente: ${delivery.clientName}`);
        console.log(`   Courier: ${delivery.courierName}`);
        console.log(`   Tipo: ${delivery.deliveryType}`);
        console.log(`   Concluída em: ${delivery.completedAt}`);
        console.log(`\n💰 Valores:`);
        console.log(`   Total: R$ ${delivery.totalAmount}`);
        console.log(`   Seu ganho: R$ ${delivery.organizerAmount} (${delivery.organizerPercentage}%)`);
        console.log(`   Courier: R$ ${delivery.courierAmount} (${delivery.courierPercentage}%)`);
        console.log(`   Plataforma: R$ ${delivery.platformAmount} (${delivery.platformPercentage}%)`);
        console.log(`\n💳 Pagamento:`);
        console.log(`   Método: ${delivery.paymentMethod}`);
        console.log(`   Status: ${delivery.paymentStatus}`);
        console.log(`   ID: ${delivery.paymentId}`);
      });
    } else {
      console.error('Erro:', response.status, data);
    }
  } catch (error) {
    console.error('Erro ao buscar recebimentos:', error);
  }
};

// Uso:
await fetchMyEarnings();           // Todas as corridas
await fetchMyEarnings(true);       // Corridas recentes (últimos 7 dias)
await fetchMyEarnings(false);      // Todas as corridas (explícito)
```

### Exemplo Flutter / Dart:

```dart
import 'dart:convert';
import 'package:http/http.dart' as http;

Future<void> fetchMyEarnings({bool? recent}) async {
  try {
    // Construir URL com parâmetro opcional
    String url = 'https://api.mvt-events.com/api/organizers/me/earnings';
    if (recent != null) {
      url += '?recent=$recent';
    }

    final response = await http.get(
      Uri.parse(url),
      headers: {
        'Authorization': 'Bearer $organizerToken',
        'Content-Type': 'application/json',
      },
    );

    if (response.statusCode == 200) {
      final data = jsonDecode(response.body);
      
      print('Total ganho: R\$ ${data['totalEarnings']}');
      print('Total de corridas: ${data['totalDeliveries']}');
      
      for (var delivery in data['deliveries']) {
        print('\n📦 Corrida #${delivery['deliveryId']}');
        print('   De: ${delivery['fromAddress']}');
        print('   Para: ${delivery['toAddress']}');
        print('   Distância: ${delivery['distanceKm']} km');
        print('   Cliente: ${delivery['clientName']}');
        print('   Courier: ${delivery['courierName']}');
        print('   Tipo: ${delivery['deliveryType']}');
        print('   Concluída em: ${delivery['completedAt']}');
        print('\n💰 Valores:');
        print('   Total: R\$ ${delivery['totalAmount']}');
        print('   Seu ganho: R\$ ${delivery['organizerAmount']} (${delivery['organizerPercentage']}%)');
        print('   Courier: R\$ ${delivery['courierAmount']} (${delivery['courierPercentage']}%)');
        print('   Plataforma: R\$ ${delivery['platformAmount']} (${delivery['platformPercentage']}%)');
        print('\n💳 Pagamento:');
        print('   Método: ${delivery['paymentMethod']}');
        print('   Status: ${delivery['paymentStatus']}');
        print('   ID: ${delivery['paymentId']}');
      }
    } else {
      print('Erro: ${response.statusCode}');
    }
  } catch (e) {
    print('Erro ao buscar recebimentos: $e');
  }
}

// Uso:
await fetchMyEarnings();              // Todas as corridas
await fetchMyEarnings(recent: true);  // Corridas recentes (últimos 7 dias)
await fetchMyEarnings(recent: false); // Todas as corridas (explícito)
```

---

## Fórmula de Cálculo

Para cada corrida onde o organizer participou:

### Com Organizer (sempre):
```
Valor Total da Corrida = shippingFee

Courier:     87% do total  (valor base)
Organizer:   5% do total   (comissão da empresa)
Plataforma:  8% do total   (comissão da plataforma)
```

### Exemplo:
```
Corrida de R$ 100,00

Courier:     R$ 87,00  (87%)
Organizer:   R$ 5,00   (5%)
Plataforma:  R$ 8,00   (8%)
Total:       R$ 100,00
```

---

## Observações Importantes

1. **Apenas corridas completadas**: Retorna apenas deliveries com status `COMPLETED` e pagamento `PAID`
2. **Sempre com organizer**: Este endpoint só retorna corridas onde o organizer participou (organizerAmount sempre > 0)
3. **Ordenação**: Corridas ordenadas por data de conclusão (mais recente primeiro)
4. **Filtro de data**: O parâmetro `recent` usa o valor de `deliveryHistoryDays` da configuração do site
5. **Tipos de entrega**: `DELIVERY` (entrega de objeto) ou `RIDE` (transporte de passageiro)
6. **Métodos de pagamento**: `PIX` ou `CREDIT_CARD`
7. **Status sempre PAID**: Este endpoint só retorna pagamentos confirmados

---

## Suporte

Para dúvidas ou suporte, contate o time de desenvolvimento.
