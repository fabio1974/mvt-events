# API - Endereço Padrão do Usuário

Documentação dos endpoints para gerenciamento do endereço padrão do usuário logado.

## 📋 Resumo

O sistema permite que cada usuário tenha **múltiplos endereços**, mas apenas **um pode ser marcado como padrão**. Esta documentação cobre os endpoints específicos para gerenciar o endereço padrão.

**Base URL:** `http://localhost:8080` (desenvolvimento) / `https://api.mvt-events.com` (produção)

**Autenticação:** Todos os endpoints requerem token JWT no header `Authorization: Bearer {token}`

---

## 🔍 1. GET - Buscar Endereço Padrão

Retorna o endereço marcado como padrão do usuário logado.

### Endpoint
```
GET /api/addresses/me/default
```

### Headers
```
Authorization: Bearer {jwt_token}
```

### Exemplo de Request
```bash
curl -X GET "http://localhost:8080/api/addresses/me/default" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

### Resposta de Sucesso (200 OK)
```json
{
  "id": 123,
  "street": "Rua Principal",
  "number": "100",
  "complement": "Apto 501",
  "neighborhood": "Centro",
  "referencePoint": "Próximo ao mercado",
  "zipCode": "60000000",
  "latitude": -3.7327,
  "longitude": -38.5270,
  "isDefault": true,
  "city": {
    "id": 1234,
    "name": "Fortaleza",
    "state": "CE"
  }
}
```

### Resposta de Erro (404 Not Found)
Quando o usuário não possui endereço padrão cadastrado.

```json
// Sem corpo de resposta
```

### Comportamento Mobile
- Se receber **404**, exibir tela de cadastro de novo endereço
- Se receber **200**, exibir os dados do endereço para edição

---

## ➕ 2. POST - Criar Endereço Padrão

Cria um novo endereço e automaticamente o marca como padrão do usuário. Se existirem outros endereços, eles serão desmarcados como padrão.

### Endpoint
```
POST /api/addresses/me/default
```

### Headers
```
Authorization: Bearer {jwt_token}
Content-Type: application/json
```

### Body (Request Payload)
```json
{
  "street": "Rua Principal",
  "number": "100",
  "complement": "Apto 501",
  "neighborhood": "Centro",
  "referencePoint": "Próximo ao mercado",
  "zipCode": "60000000",
  "latitude": -3.7327,
  "longitude": -38.5270,
  "cityId": 1234
}
```

### Campos do Payload

| Campo | Tipo | Obrigatório | Descrição |
|-------|------|-------------|-----------|
| `street` | String | ✅ Sim | Nome da rua/avenida |
| `number` | String | ✅ Sim | Número do endereço |
| `complement` | String | ❌ Não | Complemento (apto, bloco, etc) |
| `neighborhood` | String | ✅ Sim | Bairro |
| `referencePoint` | String | ❌ Não | Ponto de referência |
| `zipCode` | String | ❌ Não | CEP (apenas números) |
| `latitude` | Number | ❌ Não | Latitude GPS |
| `longitude` | Number | ❌ Não | Longitude GPS |
| `cityId` | Number | ✅ Sim | ID da cidade (use endpoint de cidades) |

### Exemplo de Request
```bash
curl -X POST "http://localhost:8080/api/addresses/me/default" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -H "Content-Type: application/json" \
  -d '{
    "street": "Rua das Flores",
    "number": "200",
    "complement": "Casa",
    "neighborhood": "Centro",
    "referencePoint": "Perto da praça",
    "zipCode": "60000000",
    "latitude": -3.7327,
    "longitude": -38.5270,
    "cityId": 1234
  }'
```

### Resposta de Sucesso (201 Created)
```json
{
  "id": 125,
  "street": "Rua das Flores",
  "number": "200",
  "complement": "Casa",
  "neighborhood": "Centro",
  "referencePoint": "Perto da praça",
  "zipCode": "60000000",
  "latitude": -3.7327,
  "longitude": -38.5270,
  "isDefault": true,
  "city": {
    "id": 1234,
    "name": "Fortaleza",
    "state": "CE"
  }
}
```

### Resposta de Erro (400 Bad Request)
```json
{
  "error": "Cidade não encontrada"
}
```

---

## ✏️ 3. PUT - Atualizar Endereço Padrão

Atualiza os dados do endereço que está marcado como padrão do usuário. Apenas os campos enviados serão atualizados.

### Endpoint
```
PUT /api/addresses/me/default
```

### Headers
```
Authorization: Bearer {jwt_token}
Content-Type: application/json
```

### Body (Request Payload)
Todos os campos são **opcionais**. Envie apenas os campos que deseja atualizar.

```json
{
  "street": "Rua Atualizada",
  "number": "300",
  "complement": "Apto 201",
  "neighborhood": "Bairro Novo",
  "referencePoint": "Esquina com a avenida",
  "zipCode": "60100000",
  "latitude": -3.7500,
  "longitude": -38.5400,
  "cityId": 5678
}
```

### Exemplo de Request (Atualização Parcial)
```bash
curl -X PUT "http://localhost:8080/api/addresses/me/default" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -H "Content-Type: application/json" \
  -d '{
    "street": "Rua Modificada",
    "number": "350"
  }'
```

### Resposta de Sucesso (200 OK)
```json
{
  "id": 123,
  "street": "Rua Modificada",
  "number": "350",
  "complement": "Apto 501",
  "neighborhood": "Centro",
  "referencePoint": "Próximo ao mercado",
  "zipCode": "60000000",
  "latitude": -3.7327,
  "longitude": -38.5270,
  "isDefault": true,
  "city": {
    "id": 1234,
    "name": "Fortaleza",
    "state": "CE"
  }
}
```

### Resposta de Erro (404 Not Found)
Quando o usuário não possui endereço padrão para atualizar.

```json
{
  "error": "Usuário não possui endereço padrão"
}
```

---

## 🎯 Fluxo Recomendado para o Mobile

### Cenário 1: Primeira vez / Usuário sem endereço padrão

```
1. Ao abrir a tela → GET /api/addresses/me/default
2. Recebe 404 → Exibir formulário vazio para criar
3. Usuário preenche formulário → POST /api/addresses/me/default
4. Recebe 201 → Endereço criado com sucesso
```

### Cenário 2: Usuário com endereço padrão existente

```
1. Ao abrir a tela → GET /api/addresses/me/default
2. Recebe 200 com dados → Preencher formulário com os dados
3. Usuário modifica campos → PUT /api/addresses/me/default
4. Recebe 200 → Endereço atualizado com sucesso
```

---

## 🚨 Códigos de Status HTTP

| Código | Descrição |
|--------|-----------|
| **200 OK** | Requisição bem-sucedida (GET e PUT) |
| **201 Created** | Endereço criado com sucesso (POST) |
| **400 Bad Request** | Dados inválidos no payload |
| **401 Unauthorized** | Token JWT inválido ou ausente |
| **403 Forbidden** | Usuário tentando acessar endereço de outro usuário |
| **404 Not Found** | Endereço padrão não encontrado |
| **500 Internal Server Error** | Erro no servidor |

---

## ✅ Regras de Negócio

1. **Apenas um endereço padrão por usuário**: Ao criar ou atualizar um endereço como padrão, todos os outros são automaticamente desmarcados.

2. **Constraint de banco**: Existe uma constraint no banco de dados que impede múltiplos endereços padrão para o mesmo usuário.

3. **Autenticação obrigatória**: Todos os endpoints requerem JWT válido.

4. **Validação de propriedade**: O usuário só pode modificar seus próprios endereços.

5. **CEP**: O campo `zipCode` aceita CEP com ou sem formatação, mas sempre armazena apenas números.

6. **Campos obrigatórios no POST**:
   - `street`
   - `number`
   - `neighborhood`
   - `cityId`

7. **Campos opcionais no PUT**: Todos os campos são opcionais - apenas os enviados serão atualizados.

---

## 🗺️ Endpoint Auxiliar - Buscar Cidades

Para obter o `cityId`, use o endpoint de busca de cidades:

```
GET /api/cities/search?name={nome_cidade}&state={UF}
```

Exemplo:
```bash
curl -X GET "http://localhost:8080/api/cities/search?name=Fortaleza&state=CE"
```

Resposta:
```json
[
  {
    "id": 1234,
    "name": "Fortaleza",
    "state": "CE"
  }
]
```

---

## 📱 Exemplo de Tela Mobile (Sugestão)

### Campos do Formulário

```
┌─────────────────────────────────────┐
│  Endereço Padrão                    │
├─────────────────────────────────────┤
│                                     │
│  Rua/Avenida: [____________] *      │
│  Número:      [____] *              │
│  Complemento: [____________]        │
│  Bairro:      [____________] *      │
│  Referência:  [____________]        │
│  CEP:         [____-___]            │
│                                     │
│  Cidade:      [Fortaleza - CE] *    │
│                                     │
│  📍 Localização GPS                 │
│  Lat: [-3.7327]  Long: [-38.5270]   │
│  [Usar minha localização atual]     │
│                                     │
│  [         SALVAR ENDEREÇO        ] │
│                                     │
└─────────────────────────────────────┘

* Campos obrigatórios
```

### Validações no Mobile (Sugeridas)

- ✅ Validar campos obrigatórios antes de enviar
- ✅ Formatar CEP automaticamente (00000-000)
- ✅ Validar formato de latitude/longitude
- ✅ Permitir buscar localização atual via GPS
- ✅ Buscar cidade com autocomplete
- ✅ Exibir mensagens de erro amigáveis

---

## 🔧 Ambiente de Desenvolvimento

- **URL Base:** `http://localhost:8080`
- **Porta:** 8080
- **Formato de Resposta:** JSON
- **Charset:** UTF-8

---

## 📞 Suporte

Em caso de dúvidas sobre a API, contate o time de backend.

**Data da documentação:** 05/02/2026
