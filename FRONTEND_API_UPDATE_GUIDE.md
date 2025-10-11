# 📚 Guia de API - Atualização de Entidades (Frontend)

## 🎯 Visão Geral

A API foi atualizada para suportar **atualização genérica** de entidades usando a própria entidade como payload, mantendo a consistência com o sistema de metadata automático.

---

## ✅ Mudanças Importantes

### **Endpoint de Update de Eventos**

#### ❌ ANTES (com DTO específico)

```
PUT /api/events/{id}
Body: EventUpdateRequest (DTO específico)
```

#### ✅ AGORA (genérico)

```
PUT /api/events/{id}
Body: Event (entidade completa)
```

---

## 🔄 Como Enviar Relacionamentos

### **Padrão: Objeto com ID**

Todos os relacionamentos (`@ManyToOne`) devem ser enviados como **objetos contendo apenas o ID**:

```json
{
  "id": 10,
  "name": "Corrida Atualizada",
  "organization": {
    "id": 6
  },
  "city": {
    "id": 964
  },
  "eventType": "RUNNING",
  "price": 150,
  "currency": "BRL"
}
```

### ✅ **Campos de Relacionamento Suportados**

| Campo          | Tipo         | Enviar Como                   |
| -------------- | ------------ | ----------------------------- |
| `organization` | Organization | `{"organization": {"id": 6}}` |
| `city`         | City         | `{"city": {"id": 964}}`       |

---

## 📋 Exemplo Completo de Update

### **Request**

```http
PUT http://localhost:8080/api/events/10
Authorization: Bearer {token}
Content-Type: application/json

{
  "id": 10,
  "name": "Maratona de São Paulo 2025",
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
  "termsAndConditions": "Aceito os termos..."
}
```

### **Response**

```json
{
  "id": 10,
  "name": "Maratona de São Paulo 2025",
  "slug": "maratona-sp-2025",
  "description": "Maior maratona do Brasil",
  "eventType": "MARATHON",
  "eventDate": "2025-12-15T07:00:00",
  "city": {
    "id": 123,
    "name": "Rio Preto da Eva",
    "state": "Amazonas",
    "stateCode": "AM",
    "ibgeCode": "1303569"
  },
  "location": "Ibirapuera",
  "maxParticipants": 5000,
  "registrationOpen": true,
  "registrationStartDate": "2025-10-01",
  "registrationEndDate": "2025-12-10",
  "price": 150.0,
  "currency": "BRL",
  "status": "PUBLISHED",
  "transferFrequency": "WEEKLY",
  "organization": {
    "id": 6,
    "name": "Moveltrack Sistemas",
    "slug": "moveltrack-sistemas",
    "contactEmail": "moveltrack@gmail.com",
    "phone": "+55 11 99999-9999",
    "website": "https://moveltrack.com.br"
  },
  "categories": [],
  "platformFeePercentage": 5.0,
  "termsAndConditions": "Aceito os termos...",
  "createdAt": "2025-10-09T21:11:44.754795",
  "updatedAt": "2025-10-11T12:30:00.123456"
}
```

---

## 🎨 Integração com Metadata API

### **Fluxo Recomendado**

1. **Obter metadados da entidade**

   ```javascript
   const metadata = await fetch("/api/metadata/event").then((r) => r.json());
   ```

2. **Renderizar formulário usando metadata**

   ```javascript
   metadata.formFields.forEach((field) => {
     if (field.type === "select") {
       // Renderizar dropdown com field.options
     } else if (field.relationship) {
       // Renderizar autocomplete para relacionamentos
     }
   });
   ```

3. **Ao salvar, montar payload genérico**

   ```javascript
   const payload = {
     id: event.id,
     name: formData.name,
     city: formData.cityId ? { id: formData.cityId } : null,
     organization: { id: formData.organizationId },
     eventType: formData.eventType,
     // ... outros campos
   };

   await fetch(`/api/events/${event.id}`, {
     method: "PUT",
     headers: { "Content-Type": "application/json" },
     body: JSON.stringify(payload),
   });
   ```

---

## 🔍 Campos de Relacionamento no Formulário

### **Campo City (Cidade)**

```typescript
// 1. Autocomplete busca cidades
const cities = await fetch(`/api/cities/search?q=${query}`).then((r) =>
  r.json()
);

// 2. Usuário seleciona cidade
const selectedCity = cities[0]; // { id: 964, name: "Ibiapina", stateCode: "CE" }

// 3. Armazenar apenas o ID
formData.cityId = selectedCity.id;

// 4. No payload final
const payload = {
  ...formData,
  city: { id: formData.cityId },
};
```

### **Campo Organization (Organização)**

```typescript
// Organização geralmente vem do contexto do usuário logado
const payload = {
  ...formData,
  organization: { id: user.organizationId },
};
```

---

## ⚠️ Campos que NÃO Devem Ser Enviados

Alguns campos são **gerenciados pelo backend** e devem ser **ignorados** no payload:

```typescript
// ❌ NÃO enviar estes campos
const fieldsToIgnore = [
  "createdAt",
  "updatedAt",
  "tenantId", // Multi-tenancy automático
];

// ✅ Filtrar antes de enviar
const payload = Object.keys(formData)
  .filter((key) => !fieldsToIgnore.includes(key))
  .reduce((obj, key) => {
    obj[key] = formData[key];
    return obj;
  }, {});
```

---

## 🎯 Exemplo Prático - React/TypeScript

### **Interface Event**

```typescript
interface Event {
  id?: number;
  name: string;
  slug: string;
  description?: string;
  eventType: EventType;
  eventDate: string;
  city?: { id: number };
  location: string;
  maxParticipants?: number;
  registrationOpen?: boolean;
  registrationStartDate?: string;
  registrationEndDate?: string;
  price?: number;
  currency?: string;
  status?: EventStatus;
  transferFrequency?: TransferFrequency;
  organization: { id: number };
  platformFeePercentage?: number;
  termsAndConditions?: string;
}
```

### **Função de Update**

```typescript
async function updateEvent(eventId: number, formData: any): Promise<Event> {
  // Montar payload genérico
  const payload: Event = {
    id: eventId,
    name: formData.name,
    slug: formData.slug,
    description: formData.description,
    eventType: formData.eventType,
    eventDate: formData.eventDate,
    location: formData.location,
    maxParticipants: formData.maxParticipants,
    registrationOpen: formData.registrationOpen,
    registrationStartDate: formData.registrationStartDate,
    registrationEndDate: formData.registrationEndDate,
    price: formData.price,
    currency: formData.currency,
    status: formData.status,
    transferFrequency: formData.transferFrequency,
    platformFeePercentage: formData.platformFeePercentage,
    termsAndConditions: formData.termsAndConditions,

    // Relacionamentos como objetos com ID
    organization: { id: formData.organizationId },
    city: formData.cityId ? { id: formData.cityId } : undefined,
  };

  const response = await fetch(`/api/events/${eventId}`, {
    method: "PUT",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${token}`,
    },
    body: JSON.stringify(payload),
  });

  if (!response.ok) {
    throw new Error("Erro ao atualizar evento");
  }

  return response.json();
}
```

---

## 🚨 Tratamento de Erros

### **Erros Comuns**

| Erro                                | Causa                        | Solução                                  |
| ----------------------------------- | ---------------------------- | ---------------------------------------- |
| `Organização não encontrada`        | `organization.id` inválido   | Verificar se organizationId existe       |
| `Cidade não encontrada`             | `city.id` inválido           | Verificar se cityId existe               |
| `Já existe um evento com este slug` | Slug duplicado               | Gerar slug único                         |
| `Could not initialize proxy`        | Relacionamento não carregado | ✅ **JÁ CORRIGIDO** - @EntityGraph ativo |

### **Exemplo de Tratamento**

```typescript
try {
  const updatedEvent = await updateEvent(eventId, formData);
  showSuccess("Evento atualizado com sucesso!");
} catch (error) {
  if (error.message.includes("não encontrada")) {
    showError("Verifique se os dados selecionados são válidos");
  } else if (error.message.includes("slug")) {
    showError("Já existe um evento com este identificador");
  } else {
    showError("Erro ao atualizar evento");
  }
}
```

---

## 📊 Endpoints Disponíveis

### **Eventos**

| Método   | Endpoint                           | Descrição                                |
| -------- | ---------------------------------- | ---------------------------------------- |
| `GET`    | `/api/events`                      | Listar eventos (com filtros e paginação) |
| `GET`    | `/api/events/{id}`                 | Buscar evento por ID                     |
| `POST`   | `/api/events`                      | Criar novo evento                        |
| `PUT`    | `/api/events/{id}`                 | **Atualizar evento (genérico)**          |
| `PUT`    | `/api/events/{id}/with-categories` | Atualizar evento com categorias (DTO)    |
| `DELETE` | `/api/events/{id}`                 | Deletar evento                           |

### **Metadata**

| Método | Endpoint                     | Descrição                           |
| ------ | ---------------------------- | ----------------------------------- |
| `GET`  | `/api/metadata`              | Listar metadados de todas entidades |
| `GET`  | `/api/metadata/event`        | Metadados da entidade Event         |
| `GET`  | `/api/metadata/registration` | Metadados da entidade Registration  |
| `GET`  | `/api/metadata/payment`      | Metadados da entidade Payment       |

### **Cidades**

| Método | Endpoint                       | Descrição                     |
| ------ | ------------------------------ | ----------------------------- |
| `GET`  | `/api/cities/search?q={query}` | Buscar cidades (autocomplete) |
| `GET`  | `/api/cities/{id}`             | Buscar cidade por ID          |

---

## 🎯 Filtros na Listagem

### **Exemplo com Filtros**

```typescript
// Construir query params dinamicamente
const params = new URLSearchParams();
if (filters.eventType) params.append("eventType", filters.eventType);
if (filters.name) params.append("name", filters.name);
if (filters.status) params.append("status", filters.status);
if (filters.city) params.append("city", filters.city);
if (filters.organizationId)
  params.append("organization", filters.organizationId);

params.append("page", page.toString());
params.append("size", "10");
params.append("sort", "eventDate,desc");

const response = await fetch(`/api/events?${params.toString()}`);
const data = await response.json();

// Response tem estrutura Page<Event>
console.log(data.content); // Array de eventos
console.log(data.totalElements); // Total de registros
console.log(data.totalPages); // Total de páginas
```

### **Filtros Disponíveis para Events**

- `eventType` - Tipo do evento (RUNNING, CYCLING, etc.)
- `name` - Nome (busca parcial)
- `status` - Status (DRAFT, PUBLISHED, CANCELLED, COMPLETED)
- `city` - Nome da cidade (busca parcial)
- `state` - Sigla do estado (ex: "SP", "RJ")
- `organization` - ID da organização
- `category` - ID da categoria

---

## ✅ Checklist de Migração

### **Para o Frontend Developer:**

- [ ] Atualizar chamadas de `PUT /api/events/{id}` para usar entidade Event
- [ ] Converter campos de relacionamento para formato `{id: number}`
- [ ] Remover campos internos (createdAt, updatedAt, tenantId) do payload
- [ ] Usar metadata API para renderizar formulários dinamicamente
- [ ] Implementar autocomplete para City usando `/api/cities/search`
- [ ] Testar update de eventos com diferentes combinações de campos
- [ ] Validar que city está sendo salva corretamente
- [ ] Atualizar testes automatizados

---

## 🚀 Benefícios da Abordagem Genérica

✅ **Consistência** - Mesmo padrão para todas as entidades  
✅ **Menos código** - Não precisa criar DTOs específicos  
✅ **Metadata-driven** - Frontend usa metadados para gerar UI automaticamente  
✅ **Manutenibilidade** - Mudanças na entidade refletem automaticamente na API  
✅ **Type-safe** - TypeScript infere tipos da entidade diretamente

---

## 📞 Suporte

Em caso de dúvidas ou problemas:

1. Verifique os logs do backend para mensagens de erro detalhadas
2. Valide o payload usando a estrutura da entidade Event
3. Confirme que IDs de relacionamentos (city, organization) existem no banco
4. Use o endpoint `/api/metadata/event` para verificar campos disponíveis

---

**Última atualização:** 11 de outubro de 2025  
**Versão da API:** 1.0.0  
**Backend:** Spring Boot 3.5.6
