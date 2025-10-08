# 🏗️ Arquitetura de Metadata

## 📋 Visão Geral

O sistema de metadata fornece informações estruturadas sobre entidades, campos, filtros e paginação para o frontend, permitindo geração dinâmica de interfaces.

---

## 🎯 Objetivos

1. **Desacoplamento**: Frontend não precisa conhecer estrutura das entidades
2. **Flexibilidade**: Mudanças no backend refletem automaticamente no frontend
3. **Consistência**: Padrão único para todos os recursos
4. **Autodescoberta**: Sistema detecta automaticamente configurações

---

## 📦 Componentes Principais

### 1. EntityMetadata

Descreve a entidade como um todo.

```java
@Data @Builder
public class EntityMetadata {
    private String entityName;           // "event"
    private String displayName;          // "Eventos"
    private List<FieldMetadata> fields;  // Campos da entidade
    private List<FilterMetadata> filters;// Filtros disponíveis
    private PaginationConfig pagination; // Configuração de paginação
}
```

### 2. FieldMetadata

Descreve um campo individual da entidade.

```java
@Data @Builder
public class FieldMetadata {
    private String name;           // "name"
    private String label;          // "Nome"
    private String type;           // "string", "number", "date", "boolean"
    private Boolean required;      // true/false
    private Integer maxLength;     // 255
    private String format;         // "email", "url", "phone"
}
```

### 3. FilterMetadata

Descreve um filtro disponível.

```java
@Data @Builder
public class FilterMetadata {
    private String name;                  // "organizationId"
    private String label;                 // "Organização"
    private String type;                  // "text", "select", "entity", "date", "boolean"
    private String field;                 // "organization.id"
    private List<String> operators;       // ["equals", "contains", "between"]
    private Map<String, String> options;  // Para type="select"
    private EntityFilterConfig entityConfig; // Para type="entity"
}
```

### 4. EntityFilterConfig

Configuração para filtros de relacionamento (type="entity").

```java
@Data @Builder
public class EntityFilterConfig {
    private String entityName;        // "organization"
    private String endpoint;          // "/api/organizations"
    private String labelField;        // "name"
    private String valueField;        // "id"
    private Boolean searchable;       // true
    private String searchPlaceholder; // "Buscar Organização..."
    private String renderAs;          // "select" ou "typeahead"
}
```

### 5. PaginationConfig

Configuração de paginação.

```java
@Data @Builder
public class PaginationConfig {
    private Integer defaultPageSize;  // 10
    private List<Integer> pageSizeOptions; // [10, 20, 50, 100]
    private Integer maxPageSize;      // 100
    private String defaultSort;       // "name,asc"
}
```

---

## 🔄 Fluxo de Metadata

```
1. Frontend Request
   GET /api/metadata
   ↓
2. MetadataController.getAllMetadata()
   ↓
3. MetadataService
   ├── getEventMetadata()
   ├── getRegistrationMetadata()
   ├── getUserMetadata()
   ├── getPaymentMetadata()
   └── getCategoryMetadata()
   ↓
4. Para cada filtro terminado em "Id":
   EntityFilterHelper.createEntityFilterConfig()
   ↓
5. Reflection
   ├── Busca classe da entidade relacionada
   ├── Encontra campo com @DisplayLabel
   ├── Gera endpoint automaticamente
   └── Cria EntityFilterConfig
   ↓
6. FilterMetadata criado com entityConfig
   ↓
7. EntityMetadata montado
   ↓
8. JSON retornado ao frontend
   ↓
9. Frontend renderiza UI dinamicamente
```

---

## 🏷️ @DisplayLabel Annotation

### Propósito

Marca o campo principal de exibição de uma entidade para uso em filtros.

### Uso

```java
@Entity
public class Organization {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @DisplayLabel  // ← Campo usado em selects/typeaheads
    private String name;

    // ...outros campos
}
```

### Autodescoberta

O `EntityFilterHelper` usa reflection para:

1. Encontrar a entidade relacionada (ex: `organizationId` → `Organization`)
2. Buscar campo anotado com `@DisplayLabel`
3. Usar esse campo como `labelField` no EntityFilterConfig

---

## 🔍 EntityFilterHelper

### Responsabilidades

1. **Detecção**: Identifica filtros de relacionamento (terminam em "Id")
2. **Mapeamento**: Resolve entidade relacionada (especial para `categoryId` → `EventCategory`)
3. **Reflection**: Encontra campo @DisplayLabel
4. **Geração**: Cria EntityFilterConfig automaticamente
5. **Endpoints**: Gera URL do endpoint baseado no nome da entidade

### Exemplo de Uso

```java
// No MetadataService
FilterMetadata organizationFilter = FilterMetadata.builder()
    .name("organizationId")
    .label("Organização")
    .type("entity")
    .field("organization.id")
    .entityConfig(
        EntityFilterHelper.createEntityFilterConfig("organizationId", Long.class)
    )
    .build();
```

### Resultado Gerado

```json
{
  "name": "organizationId",
  "label": "Organização",
  "type": "entity",
  "field": "organization.id",
  "entityConfig": {
    "entityName": "organization",
    "endpoint": "/api/organizations",
    "labelField": "name",
    "valueField": "id",
    "searchable": true,
    "searchPlaceholder": "Buscar Organization..."
  }
}
```

---

## 📊 Tipos de Filtros

### 1. Text Filter

```json
{
  "name": "name",
  "label": "Nome",
  "type": "text",
  "field": "name",
  "operators": ["equals", "contains"]
}
```

### 2. Select Filter

```json
{
  "name": "status",
  "label": "Status",
  "type": "select",
  "field": "status",
  "options": {
    "ACTIVE": "Ativo",
    "CANCELLED": "Cancelado"
  }
}
```

### 3. Entity Filter

```json
{
  "name": "organizationId",
  "label": "Organização",
  "type": "entity",
  "field": "organization.id",
  "entityConfig": {
    "entityName": "organization",
    "endpoint": "/api/organizations",
    "labelField": "name",
    "valueField": "id",
    "searchable": true
  }
}
```

### 4. Date Filter

```json
{
  "name": "createdAt",
  "label": "Data de Criação",
  "type": "date",
  "field": "createdAt",
  "operators": ["equals", "between"]
}
```

### 5. Boolean Filter

```json
{
  "name": "enabled",
  "label": "Ativo",
  "type": "boolean",
  "field": "enabled"
}
```

---

## 🎨 Renderização no Frontend

### Select (Poucas Opções)

```typescript
// Quando: < 20 opções ou renderAs="select"
<Select
  endpoint={entityConfig.endpoint}
  labelField={entityConfig.labelField}
  valueField={entityConfig.valueField}
/>
```

### Typeahead (Muitas Opções)

```typescript
// Quando: > 20 opções ou renderAs="typeahead"
<Typeahead
  endpoint={entityConfig.endpoint}
  labelField={entityConfig.labelField}
  valueField={entityConfig.valueField}
  searchable={entityConfig.searchable}
  placeholder={entityConfig.searchPlaceholder}
/>
```

---

## 🔧 Configuração de Novas Entidades

### Passo 1: Anotar Entidade

```java
@Entity
public class NewEntity {
    @Id
    private Long id;

    @DisplayLabel
    private String name; // Campo principal para exibição
}
```

### Passo 2: Adicionar Metadata

```java
// No MetadataService
public EntityMetadata getNewEntityMetadata() {
    return EntityMetadata.builder()
        .entityName("newEntity")
        .displayName("Nova Entidade")
        .fields(getNewEntityFields())
        .filters(getNewEntityFilters())
        .pagination(getDefaultPagination())
        .build();
}
```

### Passo 3: Adicionar Filtros de Relacionamento

```java
private List<FilterMetadata> getNewEntityFilters() {
    return Arrays.asList(
        FilterMetadata.builder()
            .name("relatedEntityId")
            .label("Entidade Relacionada")
            .type("entity")
            .field("relatedEntity.id")
            .entityConfig(
                EntityFilterHelper.createEntityFilterConfig("relatedEntityId", Long.class)
            )
            .build()
    );
}
```

---

## 🧪 Testes

### Backend

```java
@Test
void testMetadataGeneration() {
    EntityMetadata metadata = metadataService.getEventMetadata();
    assertNotNull(metadata);
    assertEquals("event", metadata.getEntityName());

    FilterMetadata orgFilter = metadata.getFilters().stream()
        .filter(f -> f.getName().equals("organizationId"))
        .findFirst()
        .orElseThrow();

    assertEquals("entity", orgFilter.getType());
    assertNotNull(orgFilter.getEntityConfig());
    assertEquals("organization", orgFilter.getEntityConfig().getEntityName());
}
```

### Frontend

```typescript
describe("Metadata Integration", () => {
  it("should render entity filter as select", async () => {
    const metadata = await fetchMetadata("events");
    const orgFilter = metadata.filters.find((f) => f.name === "organizationId");

    expect(orgFilter.type).toBe("entity");
    expect(orgFilter.entityConfig.endpoint).toBe("/api/organizations");
  });
});
```

---

## 📈 Evolução Futura

### Planejado

- [ ] Campo `renderAs` para forçar Select ou Typeahead
- [ ] Suporte a filtros com múltipla seleção
- [ ] Cache de metadata no frontend
- [ ] Filtros salvos por usuário
- [ ] Validações customizadas por campo

### Em Consideração

- [ ] GraphQL endpoint para metadata
- [ ] Versionamento de metadata
- [ ] Metadata internacionalizado (i18n)
- [ ] Schema validation (JSON Schema)

---

## 🔗 Referências

- [JPA Specifications](./JPA_SPECIFICATIONS.md)
- [Guia de Filtros](../api/FILTERS_GUIDE.md)
- [Entity Filters Feature](../features/ENTITY_FILTERS.md)
