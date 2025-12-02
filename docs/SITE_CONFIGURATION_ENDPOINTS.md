# Endpoints - Site Configuration (Configurações do Sistema)

Tabela de configurações globais do sistema (preço por km, percentuais de comissão).

---

## 📋 Estrutura da Entidade

```json
{
  "id": 1,
  "pricePerKm": 1.00,
  "organizerPercentage": 5.00,
  "platformPercentage": 10.00,
  "isActive": true,
  "createdAt": "2025-11-22T10:00:00",
  "updatedAt": "2025-11-22T10:00:00",
  "updatedBy": "admin@sistema.com",
  "notes": "Configuração inicial do sistema"
}
```

### Campos:
- `id` (Long): ID único da configuração
- `pricePerKm` (BigDecimal): Preço por km para cálculo de frete (ex: 1.00 = R$ 1,00/km)
- `organizerPercentage` (BigDecimal): Percentual de comissão do gerente (0-100)
- `platformPercentage` (BigDecimal): Percentual de comissão da plataforma (0-100)
- `isActive` (Boolean): Indica se é a configuração ativa (**apenas uma pode estar ativa**)
- `createdAt` (DateTime): Data de criação
- `updatedAt` (DateTime): Data da última atualização
- `updatedBy` (String): Email do usuário que atualizou
- `notes` (String): Observações sobre a configuração

### Regras de Negócio:
- ✅ Apenas **UMA** configuração pode ter `isActive = true` por vez (garantido por constraint no DB)
- ✅ Soma de `organizerPercentage + platformPercentage` não pode exceder 100%
- ✅ Apenas **ADMIN** pode criar/editar configurações
- ✅ Qualquer usuário autenticado pode **visualizar** a configuração ativa

---

## 🔓 GET `/api/site-configuration`
**Retorna a configuração ativa do sistema**

### Permissão: 
✅ **Público** (qualquer usuário autenticado)

### Headers:
```
Authorization: Bearer {token}
Content-Type: application/json
```

### Response (200 OK):
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
  "notes": "Configuração inicial padrão do sistema"
}
```

### Uso no Frontend:
```javascript
// Buscar configuração ativa
const response = await fetch('/api/site-configuration', {
  headers: {
    'Authorization': `Bearer ${token}`,
    'Content-Type': 'application/json'
  }
});
const config = await response.json();

// Calcular frete
const shippingFee = distanceKm * config.pricePerKm;

// Calcular comissões
const organizerCommission = totalAmount * (config.organizerPercentage / 100);
const platformCommission = totalAmount * (config.platformPercentage / 100);
const courierAmount = totalAmount - organizerCommission - platformCommission;
```

---

## 🔒 GET `/api/site-configuration/history`
**Lista histórico de todas as configurações**

### Permissão: 
🔐 **Apenas ADMIN**

### Headers:
```
Authorization: Bearer {token}
Content-Type: application/json
```

### Response (200 OK):
```json
[
  {
    "id": 3,
    "pricePerKm": 1.50,
    "organizerPercentage": 7.00,
    "platformPercentage": 12.00,
    "isActive": true,
    "createdAt": "2025-11-22T15:00:00",
    "updatedAt": "2025-11-22T15:00:00",
    "updatedBy": "admin@sistema.com",
    "notes": "Ajuste de valores após análise de mercado"
  },
  {
    "id": 2,
    "pricePerKm": 1.00,
    "organizerPercentage": 5.00,
    "platformPercentage": 10.00,
    "isActive": false,
    "createdAt": "2025-11-20T10:00:00",
    "updatedAt": "2025-11-20T10:00:00",
    "updatedBy": "admin@sistema.com",
    "notes": "Primeira atualização"
  },
  {
    "id": 1,
    "pricePerKm": 1.00,
    "organizerPercentage": 5.00,
    "platformPercentage": 10.00,
    "isActive": false,
    "createdAt": "2025-11-01T08:00:00",
    "updatedAt": "2025-11-01T08:00:00",
    "updatedBy": "SYSTEM",
    "notes": "Configuração inicial"
  }
]
```

### Erros:
- `403 Forbidden`: Se usuário não for ADMIN

---

## 🔒 GET `/api/site-configuration/{id}`
**Busca configuração específica por ID**

### Permissão: 
🔐 **Apenas ADMIN**

### Headers:
```
Authorization: Bearer {token}
Content-Type: application/json
```

### Path Parameters:
- `id` (Long): ID da configuração

### Exemplo:
```
GET /api/site-configuration/2
```

### Response (200 OK):
```json
{
  "id": 2,
  "pricePerKm": 1.00,
  "organizerPercentage": 5.00,
  "platformPercentage": 10.00,
  "isActive": false,
  "createdAt": "2025-11-20T10:00:00",
  "updatedAt": "2025-11-20T10:00:00",
  "updatedBy": "admin@sistema.com",
  "notes": "Primeira atualização"
}
```

### Erros:
- `403 Forbidden`: Se usuário não for ADMIN
- `404 Not Found`: Se ID não existir

---

## 🔒 POST `/api/site-configuration`
**Cria nova configuração (desativa automaticamente as anteriores)**

### Permissão: 
🔐 **Apenas ADMIN**

### Headers:
```
Authorization: Bearer {token}
Content-Type: application/json
```

### Request Body:
```json
{
  "pricePerKm": 1.50,
  "organizerPercentage": 7.00,
  "platformPercentage": 12.00,
  "notes": "Atualização de valores para o mês de dezembro"
}
```

### Campos obrigatórios:
- ✅ `pricePerKm` (min: 0.01, max: 100.00)
- ✅ `organizerPercentage` (min: 0.00, max: 100.00)
- ✅ `platformPercentage` (min: 0.00, max: 100.00)

### Campos opcionais:
- `notes` (String): Observações sobre a mudança

### Response (200 OK):
```json
{
  "id": 4,
  "pricePerKm": 1.50,
  "organizerPercentage": 7.00,
  "platformPercentage": 12.00,
  "isActive": true,
  "createdAt": "2025-11-22T16:00:00",
  "updatedAt": "2025-11-22T16:00:00",
  "updatedBy": "admin@sistema.com",
  "notes": "Atualização de valores para o mês de dezembro"
}
```

### Comportamento:
1. Valida se soma de percentuais ≤ 100%
2. Desativa todas as configurações anteriores (`isActive = false`)
3. Cria nova configuração com `isActive = true`
4. Define `updatedBy` com o email do admin logado
5. Retorna a nova configuração criada

### Erros:
- `400 Bad Request`: Se validação falhar
  ```json
  {
    "message": "Soma dos percentuais não pode exceder 100%"
  }
  ```
- `403 Forbidden`: Se usuário não for ADMIN

---

## 🎨 Exemplo de Formulário CRUD no Frontend

### Lista (Tabela):
```javascript
// GET /api/site-configuration/history
const columns = [
  { key: 'id', label: 'ID' },
  { key: 'pricePerKm', label: 'Preço/km (R$)' },
  { key: 'organizerPercentage', label: 'Gerente (%)' },
  { key: 'platformPercentage', label: 'Plataforma (%)' },
  { key: 'isActive', label: 'Ativo', type: 'boolean' },
  { key: 'updatedBy', label: 'Atualizado por' },
  { key: 'updatedAt', label: 'Data', type: 'datetime' },
  { key: 'notes', label: 'Observações' }
];
```

### Formulário de Criação:
```javascript
const form = {
  pricePerKm: {
    type: 'number',
    label: 'Preço por km (R$)',
    min: 0.01,
    max: 100.00,
    step: 0.01,
    required: true,
    placeholder: '1.00'
  },
  organizerPercentage: {
    type: 'number',
    label: 'Comissão do Gerente (%)',
    min: 0,
    max: 100,
    step: 0.01,
    required: true,
    placeholder: '5.00'
  },
  platformPercentage: {
    type: 'number',
    label: 'Comissão da Plataforma (%)',
    min: 0,
    max: 100,
    step: 0.01,
    required: true,
    placeholder: '10.00'
  },
  notes: {
    type: 'textarea',
    label: 'Observações',
    required: false,
    placeholder: 'Descreva o motivo da alteração...'
  }
};
```

### Validação no Frontend:
```javascript
function validateConfig(data) {
  const total = parseFloat(data.organizerPercentage) + parseFloat(data.platformPercentage);
  
  if (total > 100) {
    return 'A soma dos percentuais não pode exceder 100%';
  }
  
  if (data.pricePerKm <= 0) {
    return 'Preço por km deve ser maior que zero';
  }
  
  return null; // válido
}
```

---

## 📊 Informações Complementares

### Metadado disponível em `/api/metadata`:
```json
{
  "siteConfiguration": {
    "pricePerKm": 1.00,
    "organizerPercentage": 5.00,
    "platformPercentage": 10.00
  }
}
```

### Cálculo de Comissões:
Para uma entrega de **R$ 100,00** com configuração padrão:
- Gerente: R$ 5,00 (5%)
- Plataforma: R$ 10,00 (10%)
- Motoboy: R$ 85,00 (restante)

### Constraint de Unicidade:
O banco de dados garante que apenas **UMA** configuração pode ter `isActive = true`. Se tentar ativar uma segunda manualmente, o PostgreSQL retornará erro de constraint.

---

## 🔑 Resumo dos Endpoints

| Método | Endpoint | Permissão | Descrição |
|--------|----------|-----------|-----------|
| GET | `/api/site-configuration` | Todos | Busca config ativa |
| GET | `/api/site-configuration/history` | ADMIN | Lista histórico |
| GET | `/api/site-configuration/{id}` | ADMIN | Busca por ID |
| POST | `/api/site-configuration` | ADMIN | Cria nova config |

**Nota**: Não há endpoint PUT ou DELETE. Toda alteração cria um novo registro (histórico).
