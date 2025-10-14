# ✅ IMPLEMENTADO: Metadata de Formulários com Relacionamentos

## 🎯 O Que Foi Criado

### 1. FormMetadataController ✅

**Arquivo:** `src/main/java/com/mvt/mvt_events/metadata/FormMetadataController.java`

**Endpoints:**

- `GET /api/metadata/forms` - Retorna metadata de formulário para TODAS as entidades
- `GET /api/metadata/forms/{entityName}` - Retorna metadata para uma entidade específica

**Características:**

- ✅ Usa `JpaMetadataExtractor` para extrair campos automaticamente
- ✅ Retorna campos com validações (`required`, `min`, `max`, `maxLength`)
- ✅ Retorna enums com `options` automáticas
- ✅ Retorna relacionamentos `OneToMany` com estrutura `nested` completa
- ✅ Customiza placeholders para melhor UX

---

## 📤 Exemplo de Resposta

### GET /api/metadata/forms/event

```json
{
  "name": "event",
  "label": "Eventos",
  "endpoint": "/api/events",
  "fields": [
    {
      "name": "name",
      "label": "Name",
      "type": "string",
      "required": true,
      "placeholder": "Digite o nome do evento",
      "sortable": true,
      "searchable": true,
      "visible": true
    },
    {
      "name": "eventDate",
      "label": "Event Date",
      "type": "date",
      "required": false,
      "sortable": true,
      "searchable": true,
      "visible": true
    },
    {
      "name": "eventType",
      "label": "Event Type",
      "type": "select",
      "required": true,
      "placeholder": "Selecione o esporte",
      "sortable": true,
      "searchable": true,
      "visible": true,
      "options": [
        { "value": "RUNNING", "label": "Corrida" },
        { "value": "CYCLING", "label": "Ciclismo" },
        { "value": "TRIATHLON", "label": "Triathlon" },
        { "value": "SWIMMING", "label": "Natação" },
        { "value": "OBSTACLE_RACE", "label": "Corrida de Obstáculos" },
        { "value": "HIKING", "label": "Caminhada" },
        { "value": "TRAIL_RUNNING", "label": "Trail Running" },
        { "value": "ULTRA_MARATHON", "label": "Ultra Maratona" },
        { "value": "MOUNTAIN_BIKING", "label": "Mountain Bike" },
        { "value": "ADVENTURE_RACE", "label": "Corrida de Aventura" }
      ]
    },
    {
      "name": "status",
      "label": "Status",
      "type": "select",
      "required": false,
      "placeholder": "Selecione o status",
      "options": [
        { "value": "DRAFT", "label": "Rascunho" },
        { "value": "PUBLISHED", "label": "Publicado" },
        { "value": "CANCELLED", "label": "Cancelado" },
        { "value": "COMPLETED", "label": "Finalizado" }
      ]
    },
    {
      "name": "categories",
      "label": "Categories",
      "type": "nested",
      "visible": false,
      "sortable": false,
      "searchable": false,
      "relationship": {
        "type": "ONE_TO_MANY",
        "targetEntity": "eventCategory",
        "targetEndpoint": "/api/event-categories",
        "cascade": true,
        "orphanRemoval": false,
        "fields": [
          {
            "name": "name",
            "label": "Name",
            "type": "string",
            "required": true,
            "maxLength": 100
          },
          {
            "name": "gender",
            "label": "Gender",
            "type": "select",
            "required": false,
            "options": [
              { "value": "MALE", "label": "Masculino" },
              { "value": "FEMALE", "label": "Feminino" },
              { "value": "MIXED", "label": "Misto" },
              { "value": "OTHER", "label": "Outro" }
            ]
          },
          {
            "name": "minAge",
            "label": "Min Age",
            "type": "number",
            "required": false,
            "min": 0.0,
            "max": 120.0
          },
          {
            "name": "maxAge",
            "label": "Max Age",
            "type": "number",
            "required": false,
            "min": 0.0,
            "max": 120.0
          },
          {
            "name": "distance",
            "label": "Distance",
            "type": "number",
            "required": true,
            "min": 0.1,
            "max": 500.0
          },
          {
            "name": "distanceUnit",
            "label": "Distance Unit",
            "type": "select",
            "required": true,
            "options": [
              { "value": "KM", "label": "Quilômetros (km)" },
              { "value": "MILES", "label": "Milhas (mi)" },
              { "value": "METERS", "label": "Metros (m)" }
            ]
          },
          {
            "name": "price",
            "label": "Price",
            "type": "currency",
            "required": true,
            "min": 0.0
          },
          {
            "name": "maxParticipants",
            "label": "Max Participants",
            "type": "number",
            "required": false,
            "min": 1.0
          },
          {
            "name": "observations",
            "label": "Observations",
            "type": "textarea",
            "required": false,
            "maxLength": 500
          }
        ]
      }
    }
  ]
}
```

---

## 🔍 Como Funciona

### 1. Frontend Faz Request

```typescript
const response = await fetch("/api/metadata/forms/event");
const metadata = await response.json();
```

### 2. Backend Extrai da Entidade JPA

```java
// JpaMetadataExtractor lê automaticamente:
@Entity
public class Event {

    @Column(nullable = false, length = 200)
    private String name;  // → required=true, maxLength=200

    @Enumerated(EnumType.STRING)
    private EventType eventType;  // → type="select" + options[]

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL)
    private List<EventCategory> categories;  // → type="nested" + relationship
}
```

### 3. Frontend Renderiza Formulário

```tsx
// Para campos simples
<input name="name" required maxLength={200} />

// Para enums
<select name="eventType">
  {metadata.fields.find(f => f.name === 'eventType').options.map(opt => (
    <option value={opt.value}>{opt.label}</option>
  ))}
</select>

// Para relacionamentos nested
<ArrayField name="categories">
  {metadata.fields.find(f => f.name === 'categories').relationship.fields.map(field => (
    <FormField field={field} />
  ))}
</ArrayField>
```

---

## ✅ Checklist

- [x] Criar `FormMetadataController`
- [x] Endpoint `GET /api/metadata/forms`
- [x] Endpoint `GET /api/metadata/forms/{entityName}`
- [x] Integrar com `JpaMetadataExtractor`
- [x] Extrair validações automaticamente
- [x] Extrair enums com options
- [x] Extrair relacionamentos nested
- [x] Customizar placeholders
- [x] Compilação OK

---

## 🧪 Como Testar

### 1. Iniciar Servidor

```bash
./gradlew bootRun
```

### 2. Testar Endpoint

```bash
# Metadata de todas entidades
curl http://localhost:8080/api/metadata/forms | jq '.'

# Metadata de Event
curl http://localhost:8080/api/metadata/forms/event | jq '.fields[] | select(.type == "nested")'

# Ver options de eventType
curl http://localhost:8080/api/metadata/forms/event | jq '.fields[] | select(.name == "eventType") | .options'

# Ver campos de categories (nested)
curl http://localhost:8080/api/metadata/forms/event | jq '.fields[] | select(.name == "categories") | .relationship.fields[] | {name, type, options}'
```

### 3. Verificar Estrutura

O JSON deve conter:

- ✅ `fields[]` - Array de campos
- ✅ `fields[].type` - Tipos corretos (string, number, date, select, nested, etc.)
- ✅ `fields[].options[]` - Para campos select/enum
- ✅ `fields[].relationship` - Para campos nested
- ✅ `fields[].relationship.fields[]` - Campos da entidade relacionada

---

## 📋 Diferenças: Tabela vs Formulário

| Aspecto             | Metadata de Tabela       | Metadata de Formulário               |
| ------------------- | ------------------------ | ------------------------------------ |
| **Endpoint**        | `/api/metadata/{entity}` | `/api/metadata/forms/{entity}`       |
| **Foco**            | Exibição de dados        | Entrada/edição de dados              |
| **Campos**          | width, align, format     | required, min, max, placeholder      |
| **Enums**           | Só nome                  | Com options completas                |
| **Relacionamentos** | Não inclui detalhes      | Inclui campos nested completos       |
| **Validações**      | Não tem                  | Completas (required, min, max, etc.) |

---

## 🚀 Próximos Passos

### Frontend

1. Atualizar `useFormMetadata` para buscar de `/api/metadata/forms/{entity}`
2. Renderizar formulários baseado na metadata retornada
3. Validar campos usando as propriedades `required`, `min`, `max`, etc.
4. Renderizar campos nested com `ArrayField`

### Backend (Opcional)

1. Adicionar suporte a labels traduzidos via i18n
2. Adicionar suporte a validações customizadas (regex, custom validators)
3. Adicionar suporte a campos condicionais (show/hide based on other fields)

---

## 🎉 Resultado

**Sistema 100% baseado em metadata genérica!**

- ✅ Zero código duplicado
- ✅ Metadata vem direto das entidades JPA
- ✅ Enums extraídos automaticamente
- ✅ Relacionamentos nested completos
- ✅ Frontend pode renderizar formulários 100% dinâmicos
- ✅ Backend e Frontend totalmente desacoplados

**Uma mudança na entidade JPA reflete automaticamente no frontend!** 🚀
