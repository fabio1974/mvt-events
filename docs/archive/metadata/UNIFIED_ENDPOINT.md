# ✅ Metadata Unificado: Tabela + Formulário

## 🎯 Objetivo

**Um único endpoint `/api/metadata/{entity}` retorna TUDO:**
- ✅ Campos de tabela (display/visualização)
- ✅ Campos de formulário (edição/validação)
- ✅ Filtros
- ✅ Paginação

---

## 📤 Estrutura da Resposta

### GET /api/metadata/event

```json
{
  "name": "event",
  "label": "Eventos",
  "endpoint": "/api/events",
  
  "fields": [...],           // ⚠️ DEPRECATED - usar tableFields
  
  "tableFields": [           // ✅ CAMPOS PARA TABELA
    {
      "name": "name",
      "label": "Nome do Evento",
      "type": "string",
      "width": 200,
      "sortable": true,
      "searchable": true,
      "visible": true
    },
    {
      "name": "eventDate",
      "label": "Data",
      "type": "date",
      "format": "dd/MM/yyyy",
      "width": 120,
      "align": "center"
    },
    {
      "name": "eventType",
      "label": "Esporte",
      "type": "enum",
      "width": 120
    },
    {
      "name": "status",
      "label": "Status",
      "type": "enum",
      "width": 120,
      "align": "center"
    },
    {
      "name": "actions",
      "label": "Ações",
      "type": "actions",
      "width": 150,
      "sortable": false,
      "searchable": false,
      "align": "center"
    }
  ],
  
  "formFields": [            // ✅ CAMPOS PARA FORMULÁRIO
    {
      "name": "name",
      "label": "Name",
      "type": "string",
      "required": true,
      "maxLength": 200,
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
      "options": [
        {"value": "RUNNING", "label": "Corrida"},
        {"value": "CYCLING", "label": "Ciclismo"},
        {"value": "TRIATHLON", "label": "Triathlon"}
      ]
    },
    {
      "name": "status",
      "label": "Status",
      "type": "select",
      "required": false,
      "placeholder": "Selecione o status",
      "options": [
        {"value": "DRAFT", "label": "Rascunho"},
        {"value": "PUBLISHED", "label": "Publicado"},
        {"value": "CANCELLED", "label": "Cancelado"}
      ]
    },
    {
      "name": "categories",
      "label": "Categories",
      "type": "nested",
      "visible": false,
      "relationship": {
        "type": "ONE_TO_MANY",
        "targetEntity": "eventCategory",
        "targetEndpoint": "/api/event-categories",
        "cascade": true,
        "fields": [
          {
            "name": "name",
            "label": "Name",
            "type": "string",
            "required": true,
            "maxLength": 100,
            "placeholder": "Nome da categoria"
          },
          {
            "name": "price",
            "label": "Price",
            "type": "currency",
            "required": true,
            "min": 0.0
          },
          {
            "name": "gender",
            "label": "Gender",
            "type": "select",
            "options": [
              {"value": "MALE", "label": "Masculino"},
              {"value": "FEMALE", "label": "Feminino"},
              {"value": "MIXED", "label": "Misto"}
            ]
          }
        ]
      }
    }
  ],
  
  "filters": [...],          // Filtros de busca
  "pagination": {...}        // Configuração de paginação
}
```

---

## 🔄 Como Funciona

### 1. Backend Processa

```java
@Service
public class MetadataService {
    
    @Autowired
    private JpaMetadataExtractor jpaExtractor;
    
    private EntityMetadata getEventMetadata() {
        EntityMetadata metadata = new EntityMetadata("event", "Eventos", "/api/events");
        
        // 1️⃣ CAMPOS DE TABELA (manual)
        List<FieldMetadata> tableFields = new ArrayList<>();
        tableFields.add(new FieldMetadata("name", "Nome", "string", 200));
        tableFields.add(new FieldMetadata("eventDate", "Data", "date", 120));
        metadata.setTableFields(tableFields);
        
        // 2️⃣ CAMPOS DE FORMULÁRIO (automático via JPA)
        List<FieldMetadata> formFields = jpaExtractor.extractFields(Event.class);
        customizeEventFormFields(formFields);
        metadata.setFormFields(formFields);
        
        // 3️⃣ FILTROS (manual)
        metadata.setFilters(filters);
        
        // 4️⃣ PAGINAÇÃO (manual)
        metadata.setPagination(pagination);
        
        // Compatibilidade
        metadata.setFields(tableFields);
        
        return metadata;
    }
}
```

### 2. Frontend Usa

```typescript
// Buscar metadata
const metadata = await fetch('/api/metadata/event').then(r => r.json());

// Renderizar TABELA
const tableColumns = metadata.tableFields.map(field => ({
  field: field.name,
  headerName: field.label,
  width: field.width,
  align: field.align,
  type: field.type,
  format: field.format
}));

// Renderizar FORMULÁRIO
const formSchema = metadata.formFields.map(field => {
  if (field.type === 'select') {
    return (
      <Select 
        name={field.name}
        required={field.required}
        options={field.options}
        placeholder={field.placeholder}
      />
    );
  }
  
  if (field.type === 'nested') {
    return (
      <ArrayField name={field.name}>
        {field.relationship.fields.map(nestedField => (
          <FormField key={nestedField.name} field={nestedField} />
        ))}
      </ArrayField>
    );
  }
  
  return (
    <Input 
      name={field.name}
      type={field.type}
      required={field.required}
      maxLength={field.maxLength}
      min={field.min}
      max={field.max}
      placeholder={field.placeholder}
    />
  );
});
```

---

## 📊 Comparação: Campos de Tabela vs Formulário

| Campo | tableFields | formFields |
|-------|------------|------------|
| **name** | ✅ "Nome do Evento" (manual) | ✅ "Name" (do JPA) |
| **eventType** | type="enum", width=120 | type="select" + options[] |
| **categories** | ❌ Não aparece | ✅ type="nested" + relationship |
| **Validações** | ❌ Não tem | ✅ required, min, max, maxLength |
| **Opções** | ❌ Não tem | ✅ options[] para enums |
| **Placeholders** | ❌ Não tem | ✅ Customizado |

---

## ✅ Vantagens

### 1. Um Único Request
```typescript
// ❌ ANTES: 2 requests
const tableMetadata = await fetch('/api/metadata/event');
const formMetadata = await fetch('/api/metadata/forms/event');

// ✅ AGORA: 1 request
const metadata = await fetch('/api/metadata/event');
const { tableFields, formFields } = metadata;
```

### 2. Consistência Garantida
- Mesma entidade retorna dados para tabela E formulário
- Não precisa gerenciar endpoints separados

### 3. Compatibilidade
- `fields` mantido para código legado
- `tableFields` e `formFields` para código novo

### 4. Performance
- 1 request em vez de 2
- Menos overhead de rede

---

## 🧪 Como Testar

### 1. Request Completo

```bash
curl http://localhost:8080/api/metadata/event | jq '.'
```

### 2. Ver Apenas tableFields

```bash
curl http://localhost:8080/api/metadata/event | jq '.tableFields[] | {name, label, type, width}'
```

### 3. Ver Apenas formFields

```bash
curl http://localhost:8080/api/metadata/event | jq '.formFields[] | {name, type, required, options}'
```

### 4. Ver Campos Nested (Relacionamentos)

```bash
curl http://localhost:8080/api/metadata/event | jq '.formFields[] | select(.type == "nested") | .relationship'
```

### 5. Ver Options de Enums

```bash
curl http://localhost:8080/api/metadata/event | jq '.formFields[] | select(.type == "select") | {name, options}'
```

---

## 🚀 Migração Frontend

### Antes (Código Legado)

```typescript
// Busca metadata genérica
const metadata = await fetch('/api/metadata/event').then(r => r.json());
const fields = metadata.fields; // Misturava tudo
```

### Depois (Código Novo)

```typescript
// Busca metadata completa
const metadata = await fetch('/api/metadata/event').then(r => r.json());

// Para TABELA
const { tableFields, filters, pagination } = metadata;

// Para FORMULÁRIO
const { formFields } = metadata;
```

---

## 📋 Checklist de Implementação

- [x] Adicionar `tableFields` e `formFields` em `EntityMetadata`
- [x] Injetar `JpaMetadataExtractor` em `MetadataService`
- [x] Criar método `addFormFieldsToMetadata()` genérico
- [x] Aplicar em todas as entidades:
  - [x] Event
  - [x] Registration
  - [x] Organization
  - [x] User
  - [x] Payment
  - [x] EventCategory
- [x] Adicionar customização de placeholders
- [x] Manter compatibilidade com `fields`

---

## 🎉 Resultado

**Metadata 100% unificado!**

- ✅ Um único endpoint
- ✅ Dados de tabela + formulário
- ✅ Enums com options automáticas
- ✅ Relacionamentos nested completos
- ✅ Validações extraídas do JPA
- ✅ Frontend pode renderizar tudo dinamicamente

**Menos requests, mais performance, código mais limpo!** 🚀
