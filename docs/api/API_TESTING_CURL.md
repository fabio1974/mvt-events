# 🧪 Testes com cURL - Validação da API

## 🔑 Configuração

```bash
# Definir token de autenticação
TOKEN="eyJhbGciOiJIUzI1NiJ9.eyJvcmdhbml6YXRpb25JZCI6Niwicm9sZSI6IkFETUlOIiwidXNlcklkIjoiNzQyZjU4ZWEtNWJjMS00YmI1LTg0ZGMtNWVhNDYzZDE1MDQ0IiwiYXV0aG9yaXRpZXMiOlsiUk9MRV9BRE1JTiJdLCJlbWFpbCI6ImFkbWluQHRlc3QuY29tIiwic3ViIjoiYWRtaW5AdGVzdC5jb20iLCJpYXQiOjE3NjAxNDYwMTcsImV4cCI6MTc2MDE2NDAxN30.Wx79tEB5rGF-0g2-wzKkdFw7W5cmpSCQ5ydG_ze8-rs"

# URL base
API_URL="http://localhost:8080"
```

---

## 1️⃣ **Listar Eventos**

```bash
# Listar todos os eventos
curl -s "${API_URL}/api/events" \
  -H "Authorization: Bearer ${TOKEN}" | jq

# Listar com paginação
curl -s "${API_URL}/api/events?page=0&size=10&sort=eventDate,desc" \
  -H "Authorization: Bearer ${TOKEN}" | jq

# Listar com filtros
curl -s "${API_URL}/api/events?eventType=RUNNING&status=PUBLISHED" \
  -H "Authorization: Bearer ${TOKEN}" | jq

# Buscar por nome
curl -s "${API_URL}/api/events?name=corrida" \
  -H "Authorization: Bearer ${TOKEN}" | jq
```

---

## 2️⃣ **Buscar Evento por ID**

```bash
# Buscar evento específico
curl -s "${API_URL}/api/events/10" \
  -H "Authorization: Bearer ${TOKEN}" | jq

# Buscar apenas campos específicos
curl -s "${API_URL}/api/events/10" \
  -H "Authorization: Bearer ${TOKEN}" | \
  jq '{id, name, city: .city.name, organization: .organization.name}'
```

---

## 3️⃣ **Criar Evento** ✅

```bash
curl -s "${API_URL}/api/events" \
  -X POST \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Maratona Test",
    "slug": "maratona-test",
    "description": "Evento de teste",
    "eventType": "MARATHON",
    "eventDate": "2025-12-15T07:00:00",
    "city": {
      "id": 123
    },
    "location": "Parque Ibirapuera",
    "maxParticipants": 1000,
    "registrationOpen": true,
    "registrationStartDate": "2025-10-01",
    "registrationEndDate": "2025-12-10",
    "price": 150.00,
    "currency": "BRL",
    "status": "PUBLISHED",
    "transferFrequency": "WEEKLY",
    "organization": {
      "id": 6
    },
    "platformFeePercentage": 5.0
  }' | jq
```

**Resultado esperado:**

```json
{
  "id": 11,
  "name": "Maratona Test",
  "city": {
    "id": 123,
    "name": "Rio Preto da Eva",
    "stateCode": "AM"
  },
  "organization": {
    "id": 6,
    "name": "Moveltrack Sistemas"
  },
  "status": "PUBLISHED",
  ...
}
```

---

## 4️⃣ **Atualizar Evento** ✅ (MUDANÇA PRINCIPAL)

### Atualizar apenas alguns campos

```bash
curl -s "${API_URL}/api/events/10" \
  -X PUT \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "id": 10,
    "name": "Corrida Atualizada",
    "city": {
      "id": 964
    },
    "price": 200,
    "maxParticipants": 500
  }' | jq '{id, name, city: .city.name, price, maxParticipants}'
```

### Atualizar evento completo

```bash
curl -s "${API_URL}/api/events/10" \
  -X PUT \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "id": 10,
    "name": "Maratona SP 2025",
    "slug": "maratona-sp-2025",
    "description": "Maior maratona do Brasil",
    "eventType": "MARATHON",
    "eventDate": "2025-12-15T07:00:00",
    "city": {
      "id": 123
    },
    "location": "Ibirapuera",
    "maxParticipants": 5000,
    "registrationOpen": true,
    "registrationStartDate": "2025-10-01",
    "registrationEndDate": "2025-12-10",
    "price": 150.00,
    "currency": "BRL",
    "status": "PUBLISHED",
    "transferFrequency": "WEEKLY",
    "organization": {
      "id": 6
    },
    "platformFeePercentage": 5.0,
    "termsAndConditions": "Aceito os termos e condições"
  }' | jq
```

### ✅ Testar mudança de cidade

```bash
# Mudar cidade do evento 10
curl -s "${API_URL}/api/events/10" \
  -X PUT \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "id": 10,
    "city": {
      "id": 964
    }
  }' | jq '{id, name, city: {id: .city.id, name: .city.name, state: .city.stateCode}}'
```

**Resultado esperado:**

```json
{
  "id": 10,
  "name": "Corrida Melhor Ainda ATUALIZADA",
  "city": {
    "id": 964,
    "name": "Ibiapina",
    "state": "CE"
  }
}
```

---

## 5️⃣ **Deletar Evento**

```bash
# Deletar evento
curl -X DELETE "${API_URL}/api/events/11" \
  -H "Authorization: Bearer ${TOKEN}"
```

---

## 6️⃣ **Buscar Cidades (Autocomplete)**

```bash
# Buscar cidades por nome
curl -s "${API_URL}/api/cities/search?q=são paulo" | jq

# Resultado:
# [
#   {
#     "id": 123,
#     "name": "São Paulo",
#     "state": "São Paulo",
#     "stateCode": "SP",
#     "ibgeCode": "3550308"
#   },
#   {
#     "id": 456,
#     "name": "São Paulo de Olivença",
#     "state": "Amazonas",
#     "stateCode": "AM",
#     "ibgeCode": "1303908"
#   }
# ]
```

```bash
# Buscar cidade por ID
curl -s "${API_URL}/api/cities/964" | jq

# Resultado:
# {
#   "id": 964,
#   "name": "Ibiapina",
#   "state": "Ceará",
#   "stateCode": "CE",
#   "ibgeCode": "2305001"
# }
```

---

## 7️⃣ **Metadata API**

```bash
# Buscar metadados de Event
curl -s "${API_URL}/api/metadata/event" | jq

# Campos do formulário
curl -s "${API_URL}/api/metadata/event" | jq '.formFields'

# Campos da tabela
curl -s "${API_URL}/api/metadata/event" | jq '.tableFields'

# Filtros disponíveis
curl -s "${API_URL}/api/metadata/event" | jq '.filters'

# Options de um campo ENUM específico
curl -s "${API_URL}/api/metadata/event" | \
  jq '.formFields[] | select(.name == "eventType") | .options'

# Resultado:
# [
#   { "label": "Corrida", "value": "RUNNING" },
#   { "label": "Ciclismo", "value": "CYCLING" },
#   { "label": "Maratona", "value": "MARATHON" },
#   ...
# ]
```

---

## 8️⃣ **Testes de Validação**

### ❌ Teste de erro - Cidade não encontrada

```bash
curl -s "${API_URL}/api/events/10" \
  -X PUT \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "id": 10,
    "city": {
      "id": 99999
    }
  }' | jq

# Resultado esperado:
# {
#   "error": "Internal Server Error",
#   "message": "Cidade não encontrada"
# }
```

### ❌ Teste de erro - Organização não encontrada

```bash
curl -s "${API_URL}/api/events/10" \
  -X PUT \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "id": 10,
    "organization": {
      "id": 99999
    }
  }' | jq

# Resultado esperado:
# {
#   "error": "Internal Server Error",
#   "message": "Organização não encontrada"
# }
```

### ❌ Teste de erro - Slug duplicado

```bash
curl -s "${API_URL}/api/events" \
  -X POST \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Evento Duplicado",
    "slug": "corrida-melhor-ainda",
    "eventType": "RUNNING",
    "eventDate": "2025-12-01T07:00:00",
    "city": {"id": 123},
    "location": "Local",
    "organization": {"id": 6}
  }' | jq

# Resultado esperado:
# {
#   "error": "Internal Server Error",
#   "message": "Já existe um evento com este slug"
# }
```

---

## 9️⃣ **Testes Completos - Fluxo**

### Criar → Buscar → Atualizar → Deletar

```bash
# 1. Criar evento
EVENT_ID=$(curl -s "${API_URL}/api/events" \
  -X POST \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Evento Teste Fluxo",
    "slug": "evento-teste-fluxo",
    "eventType": "RUNNING",
    "eventDate": "2025-12-01T07:00:00",
    "city": {"id": 123},
    "location": "Teste",
    "organization": {"id": 6},
    "price": 50,
    "currency": "BRL",
    "status": "DRAFT"
  }' | jq -r '.id')

echo "Evento criado com ID: ${EVENT_ID}"

# 2. Buscar evento criado
curl -s "${API_URL}/api/events/${EVENT_ID}" \
  -H "Authorization: Bearer ${TOKEN}" | jq

# 3. Atualizar cidade
curl -s "${API_URL}/api/events/${EVENT_ID}" \
  -X PUT \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json" \
  -d "{
    \"id\": ${EVENT_ID},
    \"city\": {\"id\": 964}
  }" | jq '{id, name, city: .city.name}'

# 4. Verificar atualização
curl -s "${API_URL}/api/events/${EVENT_ID}" \
  -H "Authorization: Bearer ${TOKEN}" | \
  jq '{id, name, city: .city.name}'

# 5. Deletar evento
curl -X DELETE "${API_URL}/api/events/${EVENT_ID}" \
  -H "Authorization: Bearer ${TOKEN}"

echo "Evento ${EVENT_ID} deletado"
```

---

## 🎯 Validações Importantes

### ✅ Verificar que cidade está sendo salva

```bash
# Criar evento com cidade
curl -s "${API_URL}/api/events" \
  -X POST \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Teste Cidade",
    "slug": "teste-cidade",
    "eventType": "RUNNING",
    "eventDate": "2025-12-01T07:00:00",
    "city": {"id": 123},
    "location": "Local",
    "organization": {"id": 6}
  }' | jq '{id, name, city: {id: .city.id, name: .city.name}}'

# Deve retornar:
# {
#   "id": 12,
#   "name": "Teste Cidade",
#   "city": {
#     "id": 123,
#     "name": "Rio Preto da Eva"
#   }
# }
```

### ✅ Verificar LazyInitializationException resolvido

```bash
# Buscar evento e verificar que organization e city estão carregadas
curl -s "${API_URL}/api/events/10" \
  -H "Authorization: Bearer ${TOKEN}" | \
  jq '{
    id,
    name,
    organization: {id: .organization.id, name: .organization.name},
    city: {id: .city.id, name: .city.name},
    categories: .categories
  }'

# Não deve retornar erro "Could not initialize proxy"
```

---

## 📊 Script de Teste Completo

Salve como `test_api.sh`:

```bash
#!/bin/bash

API_URL="http://localhost:8080"
TOKEN="eyJhbGciOiJIUzI1NiJ9.eyJvcmdhbml6YXRpb25JZCI6Niwicm9sZSI6IkFETUlOIiwidXNlcklkIjoiNzQyZjU4ZWEtNWJjMS00YmI1LTg0ZGMtNWVhNDYzZDE1MDQ0IiwiYXV0aG9yaXRpZXMiOlsiUk9MRV9BRE1JTiJdLCJlbWFpbCI6ImFkbWluQHRlc3QuY29tIiwic3ViIjoiYWRtaW5AdGVzdC5jb20iLCJpYXQiOjE3NjAxNDYwMTcsImV4cCI6MTc2MDE2NDAxN30.Wx79tEB5rGF-0g2-wzKkdFw7W5cmpSCQ5ydG_ze8-rs"

echo "🧪 Testando API..."

# 1. Testar listagem
echo "1️⃣ Listar eventos..."
curl -s "${API_URL}/api/events?page=0&size=5" \
  -H "Authorization: Bearer ${TOKEN}" | jq '.totalElements'

# 2. Testar criação
echo "2️⃣ Criar evento..."
EVENT_ID=$(curl -s "${API_URL}/api/events" \
  -X POST \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Teste Automático",
    "slug": "teste-automatico-'$(date +%s)'",
    "eventType": "RUNNING",
    "eventDate": "2025-12-01T07:00:00",
    "city": {"id": 123},
    "location": "Local Teste",
    "organization": {"id": 6}
  }' | jq -r '.id')

echo "   Evento criado: ${EVENT_ID}"

# 3. Testar atualização de cidade
echo "3️⃣ Atualizar cidade..."
curl -s "${API_URL}/api/events/${EVENT_ID}" \
  -X PUT \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json" \
  -d "{\"id\": ${EVENT_ID}, \"city\": {\"id\": 964}}" | \
  jq -r '.city.name'

# 4. Verificar atualização
echo "4️⃣ Verificar atualização..."
CITY_NAME=$(curl -s "${API_URL}/api/events/${EVENT_ID}" \
  -H "Authorization: Bearer ${TOKEN}" | jq -r '.city.name')

if [ "$CITY_NAME" == "Ibiapina" ]; then
  echo "   ✅ Cidade atualizada com sucesso!"
else
  echo "   ❌ Erro: cidade não foi atualizada"
fi

# 5. Deletar evento
echo "5️⃣ Deletar evento..."
curl -X DELETE "${API_URL}/api/events/${EVENT_ID}" \
  -H "Authorization: Bearer ${TOKEN}"

echo "   Evento ${EVENT_ID} deletado"

echo "✅ Testes concluídos!"
```

Execute:

```bash
chmod +x test_api.sh
./test_api.sh
```

---

**Todos os testes devem passar!** ✅
