# API de Veículos - Especificação Mobile

## Base URL
```
http://10.48.235.110:8080
```

## Autenticação
Todos os endpoints requerem token JWT no header:
```
Authorization: Bearer {seu_token_jwt}
```

---

## 📱 Endpoints para Mobile (Motorista)

### 1. **Listar Meus Veículos**
Retorna todos os veículos ativos do motorista logado.

**Endpoint:**
```
GET /api/vehicles/me
```

**Headers:**
```json
{
  "Authorization": "Bearer {token}",
  "Content-Type": "application/json"
}
```

**Response 200 OK:**
```json
[
  {
    "id": 1,
    "type": "MOTORCYCLE",
    "plate": "ABC1234",
    "brand": "Honda",
    "model": "CG 160",
    "color": "VERMELHO",
    "year": "2023",
    "isActive": true,
    "isActiveVehicle": true,
    "ownerName": "João Silva",
    "ownerId": "6e8104d7-285e-4a80-9f13-857200d27987"
  },
  {
    "id": 2,
    "type": "CAR",
    "plate": "XYZ5678",
    "brand": "Fiat",
    "model": "Uno",
    "color": "BRANCO",
    "year": "2022",
    "isActive": true,
    "isActiveVehicle": false,
    "ownerName": "João Silva",
    "ownerId": "6e8104d7-285e-4a80-9f13-857200d27987"
  }
]
```

**Response 401 Unauthorized:**
```json
{
  "error": "Token inválido ou expirado"
}
```

---

### 2. **Cadastrar Novo Veículo**
Permite que o motorista cadastre um novo veículo.

**Endpoint:**
```
POST /api/vehicles
```

**Headers:**
```json
{
  "Authorization": "Bearer {token}",
  "Content-Type": "application/json"
}
```

**Request Body:**
```json
{
  "type": "MOTORCYCLE",
  "plate": "DEF9876",
  "brand": "Yamaha",
  "model": "Factor 150",
  "color": "PRETO",
  "year": "2024"
}
```

**Campos:**
| Campo | Tipo | Obrigatório | Descrição |
|-------|------|-------------|-----------|
| type | String | Sim | Tipo do veículo: `MOTORCYCLE` ou `CAR` |
| plate | String | Sim | Placa do veículo (será convertida para maiúsculas) |
| brand | String | Sim | Marca do veículo (ex: Honda, Yamaha, Fiat) |
| model | String | Sim | Modelo do veículo |
| color | String | Sim | Cor do veículo (enum): `BRANCO`, `PRETO`, `PRATA`, `CINZA`, `VERMELHO`, `AZUL`, `VERDE`, `AMARELO`, `LARANJA`, `MARROM`, `BEGE`, `DOURADO`, `ROSA`, `ROXO`, `VINHO`, `FANTASIA`, `OUTROS` |
| year | String | Não | Ano do veículo (formato: "2024") |

**Response 201 Created:**
```json
{
  "id": 3,
  "type": "MOTORCYCLE",
  "plate": "DEF9876",
  "brand": "Yamaha",
  "model": "Factor 150",
  "color": "PRETO",
  "year": "2024",
  "isActive": true,
  "isActiveVehicle": false,
  "ownerName": "João Silva",
  "ownerId": "6e8104d7-285e-4a80-9f13-857200d27987"
}
```

**Response 409 Conflict:**
```json
"Placa DEF9876 já está cadastrada"
```

**Response 401 Unauthorized:**
```json
{
  "error": "Token inválido ou expirado"
}
```

---

### 3. **Atualizar Veículo**
Permite que o motorista atualize os dados de um veículo dele.

**Endpoint:**
```
PUT /api/vehicles/{id}
```

**Path Parameters:**
- `id` (Long): ID do veículo a ser atualizado

**Headers:**
```json
{
  "Authorization": "Bearer {token}",
  "Content-Type": "application/json"
}
```

**Request Body:**
```json
{
  "type": "MOTORCYCLE",
  "plate": "DEF9876",
  "brand": "Yamaha",
  "model": "Factor 150 ESDD",
  "color": "Preta Fosca",
  "year": "2024"
}
```

**Campos:**
| Campo | Tipo | Obrigatório | Descrição |
|-------|------|-------------|-----------|
| type | String | Sim | Tipo do veículo: `MOTORCYCLE` ou `CAR` |
| plate | String | Sim | Placa do veículo |
| brand | String | Sim | Marca do veículo |
| model | String | Sim | Modelo do veículo |
| color | String | Sim | Cor do veículo |
| year | String | Não | Ano do veículo |

**Response 200 OK:**
```json
{
  "id": 3,
  "type": "MOTORCYCLE",
  "plate": "DEF9876",
  "brand": "Yamaha",
  "model": "Factor 150 ESDD",
  "color": "Preta Fosca",
  "year": "2024",
  "isActive": true,
  "isActiveVehicle": false,
  "ownerName": "João Silva",
  "ownerId": "6e8104d7-285e-4a80-9f13-857200d27987"
}
```

**Response 403 Forbidden:**
```json
"Você não tem permissão para editar este veículo"
```

**Response 404 Not Found:**
```json
{
  "error": "Veículo não encontrado"
}
```

**Response 409 Conflict:**
```json
"Placa ABC1234 já está cadastrada"
```
*(Ocorre quando tenta mudar a placa para uma que já existe)*

---

### 4. **Desativar Veículo**
Desativa um veículo (soft delete - não remove do banco de dados).

**Endpoint:**
```
DELETE /api/vehicles/{id}
```

**Path Parameters:**
- `id` (Long): ID do veículo a ser desativado

**Headers:**
```json
{
  "Authorization": "Bearer {token}",
  "Content-Type": "application/json"
}
```

**Response 200 OK:**
```json
"Veículo desativado com sucesso"
```

**Response 403 Forbidden:**
```json
"Você não tem permissão para deletar este veículo"
```

**Response 404 Not Found:**
```json
{
  "error": "Veículo não encontrado"
}
```

---

### 5. **Reativar Veículo**
Reativa um veículo previamente desativado.

**Endpoint:**
```
PUT /api/vehicles/{id}/reactivate
```

**Path Parameters:**
- `id` (Long): ID do veículo a ser reativado

**Headers:**
```json
{
  "Authorization": "Bearer {token}",
  "Content-Type": "application/json"
}
```

**Response 200 OK:**
```json
{
  "id": 1,
  "type": "MOTORCYCLE",
  "plate": "ABC1234",
  "brand": "Honda",
  "model": "CG 160",
  "color": "VERMELHO",
  "year": "2023",
  "isActive": true,
  "isActiveVehicle": false,
  "ownerName": "João Silva",
  "ownerId": "6e8104d7-285e-4a80-9f13-857200d27987"
}
```

**Response 403 Forbidden:**
```json
"Você não tem permissão para reativar este veículo"
```

**Response 404 Not Found:**
```json
{
  "error": "Veículo não encontrado"
}
```

---

### 6. **Buscar Veículo por ID**
Retorna detalhes de um veículo específico.

**Endpoint:**
```
GET /api/vehicles/{id}
```

**Path Parameters:**
- `id` (Long): ID do veículo

**Headers:**
```json
{
  "Authorization": "Bearer {token}",
  "Content-Type": "application/json"
}
```

**Response 200 OK:**
```json
{
  "id": 1,
  "type": "MOTORCYCLE",
  "plate": "ABC1234",
  "brand": "Honda",
  "model": "CG 160",
  "color": "Vermelha",
  "year": "2023",
  "isActive": true,
  "isActiveVehicle": true,
  "ownerName": "João Silva",
  "ownerId": "6e8104d7-285e-4a80-9f13-857200d27987"
}
```

**Response 404 Not Found:**
```
(corpo vazio)
```

---

### 7. **Definir Veículo Ativo**
Define qual veículo está em uso ativo pelo motorista. Apenas um veículo pode estar ativo por vez.

**Endpoint:**
```
PUT /api/vehicles/{id}/set-active
```

**Path Parameters:**
- `id` (Long): ID do veículo a ser definido como ativo

**Headers:**
```json
{
  "Authorization": "Bearer {token}",
  "Content-Type": "application/json"
}
```

**Response 200 OK:**
```json
{
  "id": 1,
  "type": "MOTORCYCLE",
  "plate": "ABC1234",
  "brand": "Honda",
  "model": "CG 160",
  "color": "Vermelha",
  "year": "2023",
  "isActive": true,
  "isActiveVehicle": true,
  "ownerName": "João Silva",
  "ownerId": "6e8104d7-285e-4a80-9f13-857200d27987"
}
```

**Response 400 Bad Request:**
```json
"Não é possível definir um veículo inativo como ativo"
```

**Response 403 Forbidden:**
```json
"Você não tem permissão para modificar este veículo"
```

**Response 404 Not Found:**
```json
{
  "error": "Veículo não encontrado"
}
```

---

### 8. **Buscar Meu Veículo Ativo**
Retorna o veículo que está em uso ativo pelo motorista logado.

**Endpoint:**
```
GET /api/vehicles/me/active
```

**Headers:**
```json
{
  "Authorization": "Bearer {token}",
  "Content-Type": "application/json"
}
```

**Response 200 OK:**
```json
{
  "id": 1,
  "type": "MOTORCYCLE",
  "plate": "ABC1234",
  "brand": "Honda",
  "model": "CG 160",
  "color": "Vermelha",
  "year": "2023",
  "isActive": true,
  "isActiveVehicle": true,
  "ownerName": "João Silva",
  "ownerId": "6e8104d7-285e-4a80-9f13-857200d27987"
}
```

**Response 404 Not Found:**
```json
"Nenhum veículo ativo definido"
```

---

## 🎨 Tipos de Veículos (Enum)

### VehicleType
```typescript
enum VehicleType {
  MOTORCYCLE = "MOTORCYCLE",  // Moto
  CAR = "CAR"                 // Automóvel
}
```

**Tradução para exibição:**
- `MOTORCYCLE` → "Moto"
- `CAR` → "Automóvel"

### VehicleColor
```typescript
enum VehicleColor {
  BRANCO = "BRANCO",       // Branco
  PRETO = "PRETO",         // Preto
  PRATA = "PRATA",         // Prata
  CINZA = "CINZA",         // Cinza
  VERMELHO = "VERMELHO",   // Vermelho
  AZUL = "AZUL",           // Azul
  VERDE = "VERDE",         // Verde
  AMARELO = "AMARELO",     // Amarelo
  LARANJA = "LARANJA",     // Laranja
  MARROM = "MARROM",       // Marrom
  BEGE = "BEGE",           // Bege
  DOURADO = "DOURADO",     // Dourado
  ROSA = "ROSA",           // Rosa
  ROXO = "ROXO",           // Roxo
  VINHO = "VINHO",         // Vinho
  FANTASIA = "FANTASIA",   // Fantasia (adesivadas/personalizadas)
  OUTROS = "OUTROS"        // Outros
}
```

**Cores disponíveis:**
- **Básicas**: Branco, Preto, Prata, Cinza
- **Vibrantes**: Vermelho, Azul, Verde, Amarelo, Laranja
- **Sofisticadas**: Marrom, Bege, Dourado, Rosa, Roxo, Vinho
- **Especiais**: Fantasia (para veículos adesivados/customizados), Outros

---

## 📝 Exemplo de Fluxo Mobile

### 1. Tela de Listagem de Veículos
```typescript
async function loadMyVehicles() {
  const token = await getAuthToken();
  
  const response = await fetch('http://10.48.235.110:8080/api/vehicles/me', {
    method: 'GET',
    headers: {
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json'
    }
  });
  
  if (response.ok) {
    const vehicles = await response.json();
    // vehicles é um array de VehicleResponse
    displayVehicles(vehicles);
  } else {
    showError('Erro ao carregar veículos');
  }
}
```

### 2. Tela de Cadastro de Veículo
```typescript
async function registerVehicle(vehicleData) {
  const token = await getAuthToken();
  
  const response = await fetch('http://10.48.235.110:8080/api/vehicles', {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({
      type: vehicleData.type,        // "MOTORCYCLE" ou "CAR"
      plate: vehicleData.plate,      // "ABC1234"
      brand: vehicleData.brand,      // "Honda"
      model: vehicleData.model,      // "CG 160"
      color: vehicleData.color,      // "Vermelha"
      year: vehicleData.year         // "2023"
    })
  });
  
  if (response.status === 201) {
    const newVehicle = await response.json();
    showSuccess('Veículo cadastrado com sucesso!');
    navigateToVehiclesList();
  } else if (response.status === 409) {
    const message = await response.text();
    showError(message); // "Placa ABC1234 já está cadastrada"
  } else {
    showError('Erro ao cadastrar veículo');
  }
}
```

### 3. Tela de Edição de Veículo
```typescript
async function updateVehicle(vehicleId, vehicleData) {
  const token = await getAuthToken();
  
  const response = await fetch(`http://10.48.235.110:8080/api/vehicles/${vehicleId}`, {
    method: 'PUT',
    headers: {
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({
      type: vehicleData.type,
      plate: vehicleData.plate,
      brand: vehicleData.brand,
      model: vehicleData.model,
      color: vehicleData.color,
      year: vehicleData.year
    })
  });
  
  if (response.ok) {
    const updatedVehicle = await response.json();
    showSuccess('Veículo atualizado com sucesso!');
    navigateToVehiclesList();
  } else if (response.status === 403) {
    const message = await response.text();
    showError(message); // "Você não tem permissão..."
  } else if (response.status === 409) {
    const message = await response.text();
    showError(message); // "Placa já está cadastrada"
  } else {
    showError('Erro ao atualizar veículo');
  }
}
```

### 4. Excluir Veículo
```typescript
async function deleteVehicle(vehicleId) {
  const token = await getAuthToken();
  
  const confirmed = await showConfirmDialog(
    'Deseja realmente desativar este veículo?'
  );
  
  if (!confirmed) return;
  
  const response = await fetch(`http://10.48.235.110:8080/api/vehicles/${vehicleId}`, {
    method: 'DELETE',
    headers: {
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json'
    }
  });
  
  if (response.ok) {
    const message = await response.text();
    showSuccess(message); // "Veículo desativado com sucesso"
    loadMyVehicles(); // Recarrega a lista
  } else if (response.status === 403) {
    const message = await response.text();
    showError(message);
  } else {
    showError('Erro ao desativar veículo');
  }
}
```

---

## 🔒 Segurança

1. **Validação de Propriedade**: O backend valida que o motorista só pode editar/deletar veículos que pertencem a ele
2. **Token JWT**: Todas as requisições precisam do token de autenticação
3. **Placa Única**: Não permite cadastrar placas duplicadas
4. **Soft Delete**: Veículos não são removidos do banco, apenas marcados como inativos
5. **Veículo Ativo Único**: Apenas um veículo por motorista pode estar ativo por vez. Ao definir um veículo como ativo, todos os outros são automaticamente marcados como inativos

---

## ⚠️ Validações

### No Cadastro/Atualização:
- ✅ Tipo deve ser `MOTORCYCLE` ou `CAR`
- ✅ Placa é obrigatória e será convertida para maiúsculas
- ✅ Placa deve ser única no sistema
- ✅ Marca e modelo são obrigatórios
- ✅ Cor deve ser um dos valores do enum VehicleColor
- ✅ Ano é opcional
- ✅ Apenas o proprietário pode editar/deletar/reativar o veículo

### Regras de Negócio:
- Motorista pode ter múltiplos veículos
- Placa não pode estar duplicada no sistema
- Veículos desativados não aparecem na listagem de `GET /api/vehicles/me`
- **Veículos desativados podem ser reativados** usando `PUT /api/vehicles/{id}/reactivate`
- Soft delete: veículos marcados como `isActive: false` permanecem no banco
- **Veículo Ativo**: Apenas um veículo por motorista pode ter `isActiveVehicle: true`
- **Constraint no Banco**: UNIQUE INDEX garante que apenas 1 veículo ativo por usuário (impossível burlar)
- Ao definir um veículo como ativo, todos os outros veículos do motorista são automaticamente marcados como inativos
- Não é possível definir um veículo desativado (`isActive: false`) como veículo ativo

---

## 🧪 Testes com cURL

### Listar meus veículos:
```bash
curl -X GET http://10.48.235.110:8080/api/vehicles/me \
  -H "Authorization: Bearer SEU_TOKEN_JWT"
```

### Cadastrar veículo:
```bash
curl -X POST http://10.48.235.110:8080/api/vehicles \
  -H "Authorization: Bearer SEU_TOKEN_JWT" \
  -H "Content-Type: application/json" \
  -d '{
    "type": "MOTORCYCLE",
    "plate": "ABC1234",
    "brand": "Honda",
    "model": "CG 160",
    "color": "Vermelha",
    "year": "2023"
  }'
```

### Atualizar veículo:
```bash
curl -X PUT http://10.48.235.110:8080/api/vehicles/1 \
  -H "Authorization: Bearer SEU_TOKEN_JWT" \
  -H "Content-Type: application/json" \
  -d '{
    "type": "MOTORCYCLE",
    "plate": "ABC1234",
    "brand": "Honda",
    "model": "CG 160 Titan",
    "color": "Vermelha",
    "year": "2023"
  }'
```

### Desativar veículo:
```bash
curl -X DELETE http://10.48.235.110:8080/api/vehicles/1 \
  -H "Authorization: Bearer SEU_TOKEN_JWT"
```

---

## 📱 TypeScript/React Native Types

```typescript
// Types para TypeScript/React Native
export enum VehicleType {
  MOTORCYCLE = "MOTORCYCLE",
  CAR = "CAR"
}

export interface VehicleResponse {
  id: number;
  type: VehicleType;
  plate: string;
  brand: string;
  model: string;
  color: string;
  year?: string;
  isActive: boolean;
  isActiveVehicle: boolean;
  ownerName: string;
  ownerId: string;
}

export interface VehicleRequest {
  type: VehicleType;
  plate: string;
  brand: string;
  model: string;
  color: VehicleColor;
  year?: string;
}

// Service de veículos
export class VehicleService {
  private baseUrl = 'http://10.48.235.110:8080/api/vehicles';
  
  async getMyVehicles(token: string): Promise<VehicleResponse[]> {
    const response = await fetch(`${this.baseUrl}/me`, {
      headers: {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json'
      }
    });
    
    if (!response.ok) throw new Error('Erro ao carregar veículos');
    return response.json();
  }
  
  async createVehicle(token: string, data: VehicleRequest): Promise<VehicleResponse> {
    const response = await fetch(this.baseUrl, {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(data)
    });
    
    if (!response.ok) {
      if (response.status === 409) {
        const message = await response.text();
        throw new Error(message);
      }
      throw new Error('Erro ao cadastrar veículo');
    }
    
    return response.json();
  }
  
  async updateVehicle(token: string, id: number, data: VehicleRequest): Promise<VehicleResponse> {
    const response = await fetch(`${this.baseUrl}/${id}`, {
      method: 'PUT',
      headers: {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(data)
    });
    
    if (!response.ok) {
      if (response.status === 403) {
        throw new Error('Você não tem permissão para editar este veículo');
      }
      if (response.status === 409) {
        const message = await response.text();
        throw new Error(message);
      }
      throw new Error('Erro ao atualizar veículo');
    }
    
    return response.json();
  }
  
  async deleteVehicle(token: string, id: number): Promise<void> {
    const response = await fetch(`${this.baseUrl}/${id}`, {
      method: 'DELETE',
      headers: {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json'
      }
    });
    
    if (!response.ok) {
      if (response.status === 403) {
        throw new Error('Você não tem permissão para deletar este veículo');
      }
      throw new Error('Erro ao deletar veículo');
    }
  }
  
  async setActiveVehicle(token: string, id: number): Promise<VehicleResponse> {
    const response = await fetch(`${this.baseUrl}/${id}/set-active`, {
      method: 'PUT',
      headers: {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json'
      }
    });
    
    if (!response.ok) {
      if (response.status === 403) {
        throw new Error('Você não tem permissão para modificar este veículo');
      }
      if (response.status === 400) {
        const message = await response.text();
        throw new Error(message);
      }
      throw new Error('Erro ao definir veículo ativo');
    }
    
    return response.json();
  }
  
  async getMyActiveVehicle(token: string): Promise<VehicleResponse | null> {
    const response = await fetch(`${this.baseUrl}/me/active`, {
      headers: {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json'
      }
    });
    
    if (response.status === 404) {
      return null; // Nenhum veículo ativo
    }
    
    if (!response.ok) {
      throw new Error('Erro ao buscar veículo ativo');
    }
    
    return response.json();
  }
}
```

---

## 📊 Modelo de Dados

### Tabela: `vehicles`
```sql
CREATE TABLE vehicles (
    id BIGSERIAL PRIMARY KEY,
    owner_id UUID NOT NULL REFERENCES users(id),
    type VARCHAR(20) CHECK (type IN ('MOTORCYCLE', 'CAR')),
    plate VARCHAR(10) UNIQUE NOT NULL,
    brand VARCHAR(50) NOT NULL,
    model VARCHAR(50) NOT NULL,
    color VARCHAR(20) NOT NULL CHECK (color IN ('BRANCO', 'PRETO', 'PRATA', 'CINZA', 'VERMELHO', 'AZUL', 'VERDE', 'AMARELO', 'LARANJA', 'MARROM', 'BEGE', 'DOURADO', 'ROSA', 'ROXO', 'VINHO', 'FANTASIA', 'OUTROS')),
    year VARCHAR(4),
    is_active BOOLEAN DEFAULT TRUE,
    is_active_vehicle BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Constraint para garantir apenas 1 veículo ativo por usuário
CREATE UNIQUE INDEX idx_vehicles_unique_active_per_owner 
ON vehicles(owner_id) 
WHERE is_active = true AND is_active_vehicle = true;
```

**Observações**: 
- `is_active_vehicle` indica qual veículo está em uso ativo pelo motorista. 
- **Constraint**: Apenas um veículo por motorista pode ter `is_active_vehicle = true` (garantido por UNIQUE INDEX)
- `color` é um enum com 17 opções de cores pré-definidas

---

## 🎯 Próximos Passos para o Mobile

1. **Tela de Listagem**: Implementar lista de veículos com opções de editar/excluir
   - Exibir badge ou indicador visual para o veículo ativo (isActiveVehicle: true)
   - Botão para definir veículo como ativo
2. **Tela de Cadastro**: Formulário com campos de tipo, placa, marca, modelo, cor, ano
3. **Tela de Edição**: Pré-popular formulário com dados do veículo selecionado
4. **Veículo Ativo**: 
   - Indicador visual na listagem mostrando qual veículo está ativo
   - Botão "Usar este veículo" para definir como ativo
   - Buscar veículo ativo ao iniciar corrida/entrega
5. **Validações**: 
   - Placa no formato brasileiro (AAA1234 ou AAA1A23)
   - Campos obrigatórios
   - Tipo selecionado (Moto/Carro)
   - Não permitir definir veículo inativo como ativo
6. **Tratamento de Erros**: Mensagens amigáveis para conflito de placa, permissões, etc.
7. **Loading States**: Feedback visual durante requisições
8. **Confirmação de Exclusão**: Dialog antes de desativar veículo
9. **Dashboard**: Exibir veículo ativo no topo da tela principal do motorista
