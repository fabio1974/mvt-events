# ✅ IMPLEMENTADO: Endpoint Unificado de Metadata

## 🎯 O Que Foi Feito

Modificado o endpoint `/api/metadata/{entity}` para retornar **tudo de uma vez**:

- ✅ `tableFields` - Campos para exibição em tabelas
- ✅ `formFields` - Campos para formulários (com validações, enums options, relacionamentos nested)
- ✅ `filters` - Filtros de busca
- ✅ `pagination` - Configuração de paginação

---

## 📤 Resposta do Endpoint

### Antes (Separado)

```bash
# Tinha que fazer 2 requests
GET /api/metadata/event        → tableFields
GET /api/metadata/forms/event  → formFields
```

### Agora (Unificado)

```bash
# 1 request retorna TUDO
GET /api/metadata/event
```

```json
{
  "name": "event",
  "label": "Eventos",
  "endpoint": "/api/events",

  "tableFields": [
    {"name": "name", "label": "Nome do Evento", "type": "string", "width": 200},
    {"name": "eventDate", "label": "Data", "type": "date", "width": 120},
    {"name": "eventType", "label": "Esporte", "type": "enum", "width": 120}
  ],

  "formFields": [
    {
      "name": "name",
      "type": "string",
      "required": true,
      "maxLength": 200,
      "placeholder": "Digite o nome do evento"
    },
    {
      "name": "eventType",
      "type": "select",
      "required": true,
      "placeholder": "Selecione o esporte",
      "options": [
        {"value": "RUNNING", "label": "Corrida"},
        {"value": "CYCLING", "label": "Ciclismo"}
      ]
    },
    {
      "name": "categories",
      "type": "nested",
      "relationship": {
        "type": "ONE_TO_MANY",
        "fields": [
          {"name": "name", "type": "string", "required": true},
          {"name": "price", "type": "currency", "required": true}
        ]
      }
    }
  ],

  "filters": [...],
  "pagination": {...}
}
```

---

## 🔧 Alterações no Código

### 1. EntityMetadata.java

```java
public class EntityMetadata {
    private List<FieldMetadata> fields;        // Deprecated
    private List<FieldMetadata> tableFields;   // ✅ NOVO
    private List<FieldMetadata> formFields;    // ✅ NOVO
    private List<FilterMetadata> filters;
    private PaginationConfig pagination;
}
```

### 2. MetadataService.java

```java
@Service
public class MetadataService {

    @Autowired
    private JpaMetadataExtractor jpaExtractor;  // ✅ NOVO

    private EntityMetadata getEventMetadata() {
        // ... configuração manual de tableFields

        // ✅ EXTRAI formFields via JPA
        List<FieldMetadata> formFields = jpaExtractor.extractFields(Event.class);
        customizeEventFormFields(formFields);

        metadata.setTableFields(tableFields);
        metadata.setFormFields(formFields);

        return metadata;
    }
}
```

---

## 🚀 Como Usar no Frontend

### Renderizar Tabela

```typescript
const metadata = await fetch("/api/metadata/event").then((r) => r.json());

const columns = metadata.tableFields.map((field) => ({
  field: field.name,
  headerName: field.label,
  width: field.width,
  type: field.type,
}));
```

### Renderizar Formulário

```typescript
const metadata = await fetch("/api/metadata/event").then((r) => r.json());

metadata.formFields.forEach((field) => {
  if (field.type === "select") {
    // Renderiza select com options
    <Select options={field.options} required={field.required} />;
  } else if (field.type === "nested") {
    // Renderiza relacionamento
    <ArrayField fields={field.relationship.fields} />;
  } else {
    // Renderiza input normal
    <Input type={field.type} required={field.required} />;
  }
});
```

---

## ✅ Benefícios

1. **Performance**: 1 request em vez de 2
2. **Consistência**: Mesma entidade, dados completos
3. **Compatibilidade**: `fields` mantido para código legado
4. **Automação**: `formFields` extraídos automaticamente do JPA
5. **Relacionamentos**: Campos nested completos
6. **Enums**: Options automáticas

---

## 🧪 Testar

```bash
# Ver tudo
curl http://localhost:8080/api/metadata/event | jq '.'

# Ver apenas tableFields
curl http://localhost:8080/api/metadata/event | jq '.tableFields'

# Ver apenas formFields
curl http://localhost:8080/api/metadata/event | jq '.formFields'

# Ver campos nested
curl http://localhost:8080/api/metadata/event | jq '.formFields[] | select(.type == "nested")'

# Ver enums com options
curl http://localhost:8080/api/metadata/event | jq '.formFields[] | select(.type == "select") | .options'
```

---

## 📁 Arquivos Modificados

- ✅ `EntityMetadata.java` - Adicionado `tableFields` e `formFields`
- ✅ `MetadataService.java` - Injetado `JpaMetadataExtractor` e método `addFormFieldsToMetadata()`
- ✅ Todas as entidades atualizadas: Event, Registration, Organization, User, Payment, EventCategory

---

## 📚 Documentação Completa

Ver: [`docs/UNIFIED_METADATA_ENDPOINT.md`](./UNIFIED_METADATA_ENDPOINT.md)

---

## 🎉 Pronto!

**Agora `/api/metadata/{entity}` retorna TUDO de uma vez!**

Frontend pode fazer 1 único request e obter:

- Estrutura da tabela
- Estrutura do formulário com validações
- Options de enums
- Relacionamentos nested
- Filtros
- Paginação

**Menos código, mais performance!** 🚀
