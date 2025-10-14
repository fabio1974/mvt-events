# 🚀 Quick Start - API de Eventos

## TL;DR - O que mudou?

### ❌ ANTES

```javascript
// Payload com DTO específico
PUT /api/events/10
{
  "name": "Evento",
  "organizationId": 6,  // ID direto
  "cityId": 964         // ID direto (NÃO FUNCIONAVA!)
}
```

### ✅ AGORA

```javascript
// Payload com entidade genérica
PUT /api/events/10
{
  "name": "Evento",
  "organization": { "id": 6 },  // Objeto com ID
  "city": { "id": 964 }         // Objeto com ID (FUNCIONA!)
}
```

---

## 📋 Exemplos Rápidos

### 1️⃣ **Criar Evento**

```bash
POST http://localhost:8080/api/events
Authorization: Bearer {token}
Content-Type: application/json

{
  "name": "Corrida de São Paulo",
  "slug": "corrida-sp",
  "eventType": "RUNNING",
  "eventDate": "2025-12-15T07:00:00",
  "city": { "id": 123 },
  "location": "Ibirapuera",
  "organization": { "id": 6 },
  "price": 100,
  "currency": "BRL"
}
```

### 2️⃣ **Atualizar Evento**

```bash
PUT http://localhost:8080/api/events/10
Authorization: Bearer {token}
Content-Type: application/json

{
  "id": 10,
  "name": "Maratona SP - ATUALIZADA",
  "city": { "id": 964 },        # Mudou a cidade
  "price": 150,                 # Aumentou preço
  "maxParticipants": 5000       # Aumentou vagas
}
```

### 3️⃣ **Listar com Filtros**

```bash
GET http://localhost:8080/api/events?eventType=RUNNING&name=corrida&page=0&size=10
Authorization: Bearer {token}
```

### 4️⃣ **Buscar Cidades (Autocomplete)**

```bash
GET http://localhost:8080/api/cities/search?q=são paulo
```

**Response:**

```json
[
  { "id": 123, "name": "São Paulo", "stateCode": "SP" },
  { "id": 456, "name": "São Paulo de Olivença", "stateCode": "AM" }
]
```

---

## 🎯 Padrão de Relacionamentos

| Campo Backend  | Tipo         | Como Enviar      | Exemplo                         |
| -------------- | ------------ | ---------------- | ------------------------------- |
| `organization` | Organization | `{"id": number}` | `{"organization": {"id": 6}}`   |
| `city`         | City         | `{"id": number}` | `{"city": {"id": 964}}`         |
| `user`         | User         | `{"id": uuid}`   | `{"user": {"id": "uuid-here"}}` |
| `event`        | Event        | `{"id": number}` | `{"event": {"id": 10}}`         |

---

## 💡 Fluxo Típico no Frontend

### **Component: EventForm.tsx**

```typescript
// 1. Carregar evento existente
const event = await fetch(`/api/events/${id}`).then((r) => r.json());

// 2. Preencher formulário
setFormData({
  name: event.name,
  cityId: event.city?.id, // Extrair ID
  organizationId: event.organization.id,
  eventType: event.eventType,
  price: event.price,
  // ...
});

// 3. Ao salvar, reconstruir payload
const payload = {
  id: event.id,
  name: formData.name,
  city: formData.cityId ? { id: formData.cityId } : null,
  organization: { id: formData.organizationId },
  eventType: formData.eventType,
  price: formData.price,
  // ...
};

// 4. Enviar
await fetch(`/api/events/${id}`, {
  method: "PUT",
  headers: { "Content-Type": "application/json" },
  body: JSON.stringify(payload),
});
```

---

## 🔥 Dica: Converter Form para Payload

```typescript
function formToPayload(formData: FormData, organizationId: number) {
  return {
    name: formData.name,
    slug: formData.slug,
    description: formData.description,
    eventType: formData.eventType,
    eventDate: formData.eventDate,
    location: formData.location,
    maxParticipants: formData.maxParticipants
      ? parseInt(formData.maxParticipants)
      : null,
    registrationOpen: formData.registrationOpen === "true",
    registrationStartDate: formData.registrationStartDate,
    registrationEndDate: formData.registrationEndDate,
    price: formData.price ? parseFloat(formData.price) : null,
    currency: formData.currency || "BRL",
    status: formData.status,
    transferFrequency: formData.transferFrequency,

    // Relacionamentos
    organization: { id: organizationId },
    city: formData.cityId ? { id: parseInt(formData.cityId) } : null,

    // Remover campos internos
    createdAt: undefined,
    updatedAt: undefined,
    tenantId: undefined,
  };
}
```

---

## ⚡ Autocomplete de Cidade

```typescript
// Debounced search
const [cities, setCities] = useState([]);

async function searchCities(query: string) {
  if (query.length < 2) return;

  const results = await fetch(`/api/cities/search?q=${query}`).then((r) =>
    r.json()
  );

  setCities(results);
}

// No select
function onCitySelect(city) {
  setFormData((prev) => ({
    ...prev,
    cityId: city.id,
    cityName: city.name, // Apenas para exibição
  }));
}
```

---

## 🎨 Metadados da API

```typescript
// Carregar metadados uma vez
const metadata = await fetch("/api/metadata/event").then((r) => r.json());

// Usar para renderizar form
metadata.formFields.forEach((field) => {
  console.log(field.name); // "eventType"
  console.log(field.label); // "Tipo de Evento"
  console.log(field.type); // "select"
  console.log(field.required); // true
  console.log(field.options); // [{ label: "Corrida", value: "RUNNING" }, ...]
});
```

---

## 🚨 Troubleshooting

### **Erro: "Cidade não encontrada"**

```typescript
// ✅ Verificar se cityId é válido
if (formData.cityId) {
  const city = await fetch(`/api/cities/${formData.cityId}`);
  if (!city.ok) {
    alert("Cidade inválida");
    return;
  }
}
```

### **Erro: "Could not initialize proxy"**

- ✅ **JÁ CORRIGIDO** no backend com @EntityGraph
- Se ainda ocorrer, verifique se está usando endpoint correto

### **Cidade não está sendo salva**

```typescript
// ❌ ERRADO
{ "city": "São Paulo" }
{ "cityId": 123 }

// ✅ CORRETO
{ "city": { "id": 123 } }
```

---

## 📞 Endpoints Principais

| Método       | Endpoint                       | Descrição               |
| ------------ | ------------------------------ | ----------------------- |
| **Events**   |
| `GET`        | `/api/events`                  | Listar (com filtros)    |
| `GET`        | `/api/events/{id}`             | Buscar por ID           |
| `POST`       | `/api/events`                  | Criar                   |
| `PUT`        | `/api/events/{id}`             | Atualizar (genérico) ✅ |
| `DELETE`     | `/api/events/{id}`             | Deletar                 |
| **Cities**   |
| `GET`        | `/api/cities/search?q={query}` | Autocomplete            |
| `GET`        | `/api/cities/{id}`             | Buscar por ID           |
| **Metadata** |
| `GET`        | `/api/metadata/event`          | Metadados de Event      |

---

## ✅ Checklist Rápido

- [ ] Atualizar `PUT /api/events/{id}` para usar formato `{city: {id}}`
- [ ] Implementar autocomplete de cidades
- [ ] Remover campos `createdAt`, `updatedAt`, `tenantId` do payload
- [ ] Testar update de cidade
- [ ] Validar metadados retornados

---

**Pronto para usar!** 🚀

Qualquer dúvida, consulte: [FRONTEND_API_UPDATE_GUIDE.md](./FRONTEND_API_UPDATE_GUIDE.md)
