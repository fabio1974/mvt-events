# 💰 Integração Mobile - Recebimentos do Courier

## 📌 Resumo

Novo endpoint para listar os recebimentos do motoboy, mostrando o detalhamento completo de cada corrida paga (quanto ele recebeu, quanto foi para o organizer, quanto para a plataforma).

---

## 🔗 Endpoint

```
GET /api/couriers/me/earnings?recent={true|false}
```

**Autenticação:** Bearer Token (apenas COURIER)

**Query Parameters (opcionais):**
- `recent`: Se `true`, filtra apenas corridas recentes (número de dias vem do `deliveryHistoryDays` da configuração, padrão: 7 dias). Se `false` ou omitido, retorna todas as corridas.

---

## 📥 Request

### Headers
```
Authorization: Bearer {token_do_courier}
Content-Type: application/json
```

### Exemplos de URL
```
GET /api/couriers/me/earnings              # Todas as corridas
GET /api/couriers/me/earnings?recent=true  # Corridas recentes (padrão: 7 dias)
GET /api/couriers/me/earnings?recent=false # Todas as corridas (explícito)
```

---

## 📤 Response (200 OK)

### Exemplo Completo de Response

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
    },
    {
      "deliveryId": 40,
      "completedAt": "2026-03-12T18:45:12",
      "fromAddress": "Rua Oscar Freire, 500 - Jardins, São Paulo",
      "toAddress": "Av. Brigadeiro Faria Lima, 2000 - Jardim Paulistano, São Paulo",
      "distanceKm": 2.5,
      "clientName": "Restaurante Bella Vista",
      "deliveryType": "DELIVERY",
      "paymentId": 126,
      "totalAmount": 50.00,
      "paymentStatus": "PAID",
      "paymentMethod": "CREDIT_CARD",
      "courierAmount": 43.50,
      "courierPercentage": 87.00,
      "organizerAmount": 2.50,
      "organizerPercentage": 5.00,
      "organizerName": "Maria Santos",
      "platformAmount": 4.00,
      "platformPercentage": 8.00
    },
    {
      "deliveryId": 38,
      "completedAt": "2026-03-11T12:20:30",
      "fromAddress": "Av. Ibirapuera, 3000 - Moema, São Paulo",
      "toAddress": "Rua Bela Cintra, 1500 - Consolação, São Paulo",
      "distanceKm": 6.8,
      "clientName": "Pedro Costa",
      "deliveryType": "RIDE",
      "paymentId": 125,
      "totalAmount": 120.00,
      "paymentStatus": "PAID",
      "paymentMethod": "PIX",
      "courierAmount": 104.40,
      "courierPercentage": 87.00,
      "organizerAmount": 0.00,
      "organizerPercentage": 0.00,
      "organizerName": null,
      "platformAmount": 15.60,
      "platformPercentage": 13.00
    }
  ]
}
```

### Response Vazio (Sem Corridas)

```json
{
  "totalDeliveries": 0,
  "totalEarnings": 0.00,
  "deliveries": []
}
```

---

## 📊 Campos Principais

### Response Root

| Campo | Tipo | Descrição |
|-------|------|-----------|
| `totalDeliveries` | Int | Total de corridas pagas |
| `totalEarnings` | Decimal | Total que o courier recebeu (soma de todos os `courierAmount`) |
| `deliveries` | Array | Lista de corridas |

### Cada Corrida (deliveries[])

#### Informações da Corrida

| Campo | Tipo | Descrição |
|-------|------|-----------|
| `deliveryId` | Long | ID da entrega |
| `completedAt` | String (ISO 8601) | Data/hora de conclusão |
| `fromAddress` | String | Endereço de origem |
| `toAddress` | String | Endereço de destino |
| `distanceKm` | Decimal | Distância em km |
| `clientName` | String | Nome do cliente |
| `deliveryType` | String | `DELIVERY` ou `RIDE` |

#### Informações do Pagamento

| Campo | Tipo | Descrição |
|-------|------|-----------|
| `paymentId` | Long | ID do pagamento |
| `totalAmount` | Decimal | Valor total da corrida |
| `paymentStatus` | String | Sempre `PAID` |
| `paymentMethod` | String | `PIX` ou `CREDIT_CARD` |

#### Repartição (Split)

| Campo | Tipo | Descrição |
|-------|------|-----------|
| `courierAmount` | Decimal | **Valor que o courier recebeu** |
| `courierPercentage` | Decimal | % do courier (geralmente 87%) |
| `organizerAmount` | Decimal | Valor do organizer (5% ou 0) |
| `organizerPercentage` | Decimal | % do organizer (5% ou 0) |
| `organizerName` | String | Nome do organizer (`null` se não houver) |
| `platformAmount` | Decimal | Valor da plataforma |
| `platformPercentage` | Decimal | % da plataforma (8% ou 13%) |

---

## 💡 Regras de Negócio

### Splits (Repartição)

**Com Organizer (estabelecimento):**
- Courier: **87%**
- Organizer: **5%**
- Plataforma: **8%**

**Sem Organizer (cliente app):**
- Courier: **87%**
- Plataforma: **13%** (incorpora os 5% do organizer)

### Filtros Aplicados no Backend

- ✅ Apenas corridas **COMPLETED** (finalizadas)
- ✅ Apenas pagamentos **PAID** (confirmados)
- ✅ Apenas do courier logado
- 📅 Se `recent=true`, filtra por data de conclusão usando `deliveryHistoryDays` da configuração (padrão: 7 dias)

**Observação:** O número de dias do filtro `recent` é configurado no painel admin (campo `deliveryHistoryDays` em Site Configuration).

### Ordenação

Corridas vêm ordenadas por **data de conclusão** (mais recente primeiro).

---

## 💻 Código React Native

```typescript
// Service
import AsyncStorage from '@react-native-async-storage/async-storage';

interface CourierEarningsResponse {
  totalDeliveries: number;
  totalEarnings: number;
  deliveries: DeliveryEarning[];
}

interface DeliveryEarning {
  deliveryId: number;
  completedAt: string;
  fromAddress: string;
  toAddress: string;
  distanceKm: number;
  clientName: string;
  deliveryType: 'DELIVERY' | 'RIDE';
  paymentId: number;
  totalAmount: number;
  paymentStatus: string;
  paymentMethod: 'PIX' | 'CREDIT_CARD';
  courierAmount: number;
  courierPercentage: number;
  organizerAmount: number;
  organizerPercentage: number;
  organizerName: string | null;
  platformAmount: number;
  platformPercentage: number;
}

export const fetchCourierEarnings = async (recent?: boolean): Promise<CourierEarningsResponse> => {
  const token = await AsyncStorage.getItem('courierToken');
  
  let url = `${API_BASE_URL}/api/couriers/me/earnings`;
  if (recent) {
    url += '?recent=true';
  }

  const response = await fetch(url, {
    method: 'GET',
    headers: {
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json',
    },
  });

  if (!response.ok) {
    throw new Error('Erro ao buscar recebimentos');
  }

  return response.json();
};

// Uso no componente
import React, { useState, useEffect } from 'react';
import { View, Text, FlatList, TouchableOpacity } from 'react-native';

const EarningsScreen = () => {
  const [earnings, setEarnings] = useState<CourierEarningsResponse | null>(null);
  const [showRecent, setShowRecent] = useState(true); // Recentes por padrão
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    loadEarnings();
  }, [showRecent]);

  const loadEarnings = async () => {
    try {
      setLoading(true);
      const data = await fetchCourierEarnings(showRecent);
      setEarnings(data);
    } catch (error) {
      console.error('Erro ao carregar recebimentos:', error);
    } finally {
      setLoading(false);
    }
  };

  const formatCurrency = (value: number) => {
    return `R$ ${value.toFixed(2).replace('.', ',')}`;
  };

  const formatDate = (dateString: string) => {
    const date = new Date(dateString);
    return date.toLocaleDateString('pt-BR', { 
      day: '2-digit', 
      month: '2-digit', 
      hour: '2-digit', 
      minute: '2-digit' 
    });
  };

  return (
    <View style={{ flex: 1, padding: 16 }}>
      {/* Filtro de Período */}
      <View style={{ flexDirection: 'row', marginBottom: 16 }}>
        <TouchableOpacity onPress={() => setShowRecent(true)}>
          <Text style={showRecent ? { fontWeight: 'bold' } : {}}>Recentes</Text>
        </TouchableOpacity>
        <TouchableOpacity onPress={() => setShowRecent(false)}>
          <Text style={!showRecent ? { fontWeight: 'bold' } : {}}>Todas</Text>
        </TouchableOpacity>
      </View>

      {/* Resumo */}
      {earnings && (
        <View style={{ backgroundColor: '#f0f0f0', padding: 16, marginBottom: 16 }}>
          <Text style={{ fontSize: 18, fontWeight: 'bold' }}>
            Total Ganho: {formatCurrency(earnings.totalEarnings)}
          </Text>
          <Text>Total de Corridas: {earnings.totalDeliveries}</Text>
        </View>
      )}

      {/* Lista de Corridas */}
      <FlatList
        data={earnings?.deliveries || []}
        keyExtractor={(item) => item.deliveryId.toString()}
        renderItem={({ item }) => (
          <View style={{ padding: 12, marginBottom: 8, backgroundColor: 'white', borderRadius: 8 }}>
            <Text style={{ fontWeight: 'bold', fontSize: 16 }}>
              Corrida #{item.deliveryId} - {formatCurrency(item.courierAmount)}
            </Text>
            <Text>✓ Pago via {item.paymentMethod === 'PIX' ? 'PIX' : 'Cartão'}</Text>
            <Text>📍 {item.fromAddress.substring(0, 30)}... → {item.toAddress.substring(0, 30)}...</Text>
            <Text>📅 {formatDate(item.completedAt)}</Text>
            <Text>👤 {item.clientName}</Text>
            
            {/* Detalhamento (expansível) */}
            <View style={{ marginTop: 8, paddingTop: 8, borderTopWidth: 1, borderTopColor: '#eee' }}>
              <Text>Detalhamento:</Text>
              <Text>• Você: {formatCurrency(item.courierAmount)} ({item.courierPercentage}%)</Text>
              {item.organizerAmount > 0 && (
                <Text>• Organizer ({item.organizerName}): {formatCurrency(item.organizerAmount)} ({item.organizerPercentage}%)</Text>
              )}
              <Text>• Plataforma: {formatCurrency(item.platformAmount)} ({item.platformPercentage}%)</Text>
              <Text style={{ fontWeight: 'bold' }}>• Total: {formatCurrency(item.totalAmount)}</Text>
            </View>
          </View>
        )}
      />
    </View>
  );
};

export default EarningsScreen;
```

---

## 🎨 Sugestões de UI

### 1. Filtro de Período (Toggle Simples)
```
[ Recentes ] [ Todas ]
```

**Observação:** "Recentes" usa o número de dias configurado no backend (padrão: 7 dias, configurável pelo admin).

### 2. Card de Resumo
```
┌─────────────────────────────────┐
│  💰 Seus Ganhos (Últimos 7 dias)│
│  R$ 1.305,00                    │
│  15 corridas completadas        │
└─────────────────────────────────┘
```

**Nota:** O número de dias deve ser obtido dinamicamente da configuração ativa via endpoint `GET /api/site-configuration/active` (campo `deliveryHistoryDays`).

### 3. Lista de Corridas (expandível)
```
┌─────────────────────────────────┐
│ ▼ Corrida #42 - R$ 87,00        │
│   ✓ Cartão • 13/03 15:30        │
│   📍 Paulista → Augusta          │
│   👤 João Silva                  │
│                                 │
│   Detalhamento:                 │
│   • Você: R$ 87,00 (87%)        │
│   • Organizer: R$ 5,00 (5%)     │
│   • Plataforma: R$ 8,00 (8%)    │
└─────────────────────────────────┘
```

---

## ⚠️ Erros Possíveis

| Status | Descrição |
|--------|-----------|
| 401 | Token inválido/expirado |
| 403 | Usuário não é COURIER |
| 500 | Erro interno do servidor |

---

## ✅ Checklist de Integração

- [ ] Criar tela "Meus Ganhos" no menu do courier
- [ ] Implementar service para chamar o endpoint
- [ ] Adicionar toggle simples: **Recentes** (recent=true) / **Todas** (recent=false)
- [ ] Exibir resumo no topo (total ganho + total de corridas)
- [ ] Listar corridas com cards expansíveis
- [ ] Mostrar detalhamento do split ao expandir
- [ ] Loading state durante requisição
- [ ] Tratamento de erros (401, 403, 500)
- [ ] Pull-to-refresh para atualizar dados
- [ ] Empty state quando não houver corridas

---

## 📝 Notas

- Endpoint retorna apenas corridas **completadas** com pagamento **confirmado**
- Dados em **tempo real** (não há cache no backend)
- Ordenação: mais recente primeiro
- Valores sempre em **BRL** (Real)
- Datas em formato **ISO 8601** (UTC)
- **Filtro "recent":** Número de dias configurável no painel admin (campo `deliveryHistoryDays` em Site Configuration, padrão: 7 dias)

---

## 🔧 Configuração Backend

O número de dias para o filtro `recent=true` é configurável:

1. **Localização:** Tabela `site_configurations`, campo `delivery_history_days`
2. **Padrão:** 7 dias
3. **Range:** 1 a 365 dias
4. **Editável por:** ADMIN via painel de administração
5. **Aplicação:** Imediata (não requer restart)

---

## 📊 Cenários de Uso

### Cenário 1: Courier com poucas corridas
```
Request: GET /api/couriers/me/earnings
Response: 2 corridas, R$ 156,60 total
Recomendação: Usar recent=false (mostrar todas)
```

### Cenário 2: Courier ativo com muitas corridas
```
Request: GET /api/couriers/me/earnings?recent=true
Response: 15 corridas (últimos 7 dias), R$ 1.305,00 total
Recomendação: Usar recent=true por padrão para performance
```

### Cenário 3: Courier sem corridas pagas
```
Request: GET /api/couriers/me/earnings
Response: totalDeliveries: 0, totalEarnings: 0.00, deliveries: []
UI: Exibir empty state com mensagem
```

---

## ❓ FAQ (Perguntas Frequentes)

### 1. O que acontece se não houver corridas pagas?
O endpoint retorna um objeto com `totalDeliveries: 0`, `totalEarnings: 0.00` e `deliveries: []` vazio. Implemente um empty state na UI.

### 2. Por que algumas corridas não aparecem?
Apenas corridas **COMPLETED** (finalizadas) com pagamento **PAID** (confirmado) aparecem no histórico.

### 3. Como funciona o filtro "recent"?
Quando `recent=true`, o backend busca o número de dias configurado em `deliveryHistoryDays` (padrão: 7) e filtra corridas concluídas nesse período.

### 4. Posso cachear os dados no app?
Sim, mas implemente pull-to-refresh para o usuário atualizar quando necessário. Os dados mudam quando novos pagamentos são confirmados.

### 5. O split sempre é 87% / 5% / 8%?
- **Com organizer:** 87% (courier) + 5% (organizer) + 8% (plataforma)
- **Sem organizer:** 87% (courier) + 13% (plataforma)

### 6. Como diferenciar entrega de corrida de passageiro?
Use o campo `deliveryType`: `"DELIVERY"` (entrega de objeto) ou `"RIDE"` (transporte de passageiro).

### 7. O que fazer em caso de erro 403?
Significa que o usuário logado não é COURIER. Verifique o role do usuário antes de acessar essa tela.

### 8. Há paginação?
Atualmente não. O endpoint retorna todas as corridas do período filtrado. Se performance for um problema no futuro, paginação pode ser implementada.

---

## 🔗 Endpoints Relacionados

Para uma experiência completa, você pode combinar este endpoint com:

| Endpoint | Descrição |
|----------|-----------|
| `GET /api/deliveries/courier/completed` | Lista todas as entregas completadas (sem detalhamento de split) |
| `GET /api/users/me/activation-status` | Verifica se courier tem conta bancária cadastrada |
| `GET /api/bank-accounts/me` | Detalhes da conta bancária do courier |

---

## 🚀 Disponibilidade

✅ **Já disponível em:**
- Local: `http://localhost:8080`
- Dev: aguardando deploy
- Prod: aguardando deploy

**Versão da API:** v1  
**Data de disponibilização:** 14/03/2026

---

## 📞 Contato

**Dúvidas técnicas:**  
Entre em contato com o time backend.

**Documentação completa:**  
- [`API_COURIER_EARNINGS.md`](./API_COURIER_EARNINGS.md) - Documentação técnica detalhada
- Swagger/OpenAPI: Disponível em `http://localhost:8080/swagger-ui.html` (local)
