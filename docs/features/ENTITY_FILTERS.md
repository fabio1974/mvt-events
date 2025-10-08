# 🔗 Entity Filters Feature

## 🎯 Problema Resolvido

**Antes:** Filtros por ID numérico - péssima UX

```
Filtrar por Organização: [___1___] 👎
```

**Depois:** Filtros por nome com busca

```
Filtrar por Organização: [Acme Corp ▼] 👍
```

---

## 💡 Solução

Sistema automático que detecta relacionamentos e configura filtros inteligentes.

---

## 🏗️ Arquitetura

### 1. @DisplayLabel Annotation

Marca o campo principal de exibição em cada entidade.

```java
@Entity
public class Organization {
    @Id
    private Long id;

    @DisplayLabel  // ← Este campo será usado em selects
    private String name;
}
```

### 2. EntityFilterConfig

DTO com todas as informações para renderizar o filtro.

```java
@Data @Builder
public class EntityFilterConfig {
    private String entityName;        // "organization"
    private String endpoint;          // "/api/organizations"
    private String labelField;        // "name"
    private String valueField;        // "id"
    private Boolean searchable;       // true
    private String searchPlaceholder; // "Buscar Organization..."
}
```

### 3. EntityFilterHelper

Classe utilitária com reflection para autodiscovery.

**Funções:**

- Detecta filtros terminados em "Id"
- Resolve entidade relacionada
- Encontra campo @DisplayLabel
- Gera endpoint automaticamente
- Cria EntityFilterConfig

### 4. MetadataService

Integra EntityFilterHelper em todos os métodos de metadata.

```java
FilterMetadata.builder()
    .name("organizationId")
    .label("Organização")
    .type("entity")
    .field("organization.id")
    .entityConfig(
        EntityFilterHelper.createEntityFilterConfig("organizationId", Long.class)
    )
    .build();
```

---

## 🔄 Fluxo Completo

```
1. Frontend solicita metadata
   GET /api/metadata
   ↓
2. MetadataService.getEventMetadata()
   ↓
3. Encontra filtro "organizationId"
   ↓
4. EntityFilterHelper.createEntityFilterConfig("organizationId", Long.class)
   ↓
5. Reflection
   ├── organizationId → Organization.class
   ├── Busca campo com @DisplayLabel
   ├── Encontra: name
   └── Gera endpoint: /api/organizations
   ↓
6. Retorna EntityFilterConfig
   {
     entityName: "organization",
     endpoint: "/api/organizations",
     labelField: "name",
     valueField: "id",
     searchable: true
   }
   ↓
7. Frontend renderiza
   <EntitySelect
     endpoint="/api/organizations"
     labelField="name"
     valueField="id"
   />
```

---

## 📦 Entidades Suportadas

| Entidade      | Campo Display | Endpoint              |
| ------------- | ------------- | --------------------- |
| Event         | name          | /api/events           |
| User          | name          | /api/users            |
| Organization  | name          | /api/organizations    |
| EventCategory | name          | /api/event-categories |
| Registration  | id\*          | /api/registrations    |

\*Registrations ainda usa ID - considerar adicionar display name composto

---

## 🎨 Frontend Components

### EntitySelect (Poucas Opções)

```tsx
<EntitySelect
  endpoint={config.endpoint}
  labelField={config.labelField}
  valueField={config.valueField}
  value={selectedId}
  onChange={handleChange}
/>
```

### EntityTypeahead (Muitas Opções)

```tsx
<EntityTypeahead
  endpoint={config.endpoint}
  labelField={config.labelField}
  valueField={config.valueField}
  searchable={config.searchable}
  placeholder={config.searchPlaceholder}
  value={selectedId}
  onChange={handleChange}
/>
```

---

## 🔧 Adicionar Nova Entidade

### Passo 1: Anotar Entidade

```java
@Entity
public class Venue {
    @Id
    private Long id;

    @DisplayLabel
    private String name;

    private String address;
}
```

### Passo 2: Criar Relacionamento

```java
@Entity
public class Event {
    // ...

    @ManyToOne
    private Venue venue;
}
```

### Passo 3: Adicionar Filtro

```java
// No MetadataService.getEventMetadata()
FilterMetadata.builder()
    .name("venueId")
    .label("Local")
    .type("entity")
    .field("venue.id")
    .entityConfig(
        EntityFilterHelper.createEntityFilterConfig("venueId", Long.class)
    )
    .build()
```

**Pronto!** O sistema automaticamente:

- Detecta Venue como entidade relacionada
- Encontra "name" como campo display
- Gera endpoint "/api/venues"
- Configura como searchable

---

## 🧪 Testes

### Backend

```java
@Test
void testEntityFilterGeneration() {
    EntityFilterConfig config = EntityFilterHelper
        .createEntityFilterConfig("organizationId", Long.class);

    assertEquals("organization", config.getEntityName());
    assertEquals("/api/organizations", config.getEndpoint());
    assertEquals("name", config.getLabelField());
    assertEquals("id", config.getValueField());
    assertTrue(config.getSearchable());
}
```

### Frontend

```typescript
describe("EntitySelect", () => {
  it("should load options from endpoint", async () => {
    const { getByRole } = render(
      <EntitySelect
        endpoint="/api/organizations"
        labelField="name"
        valueField="id"
      />
    );

    const select = getByRole("combobox");
    fireEvent.click(select);

    await waitFor(() => {
      expect(screen.getByText("Acme Corp")).toBeInTheDocument();
    });
  });
});
```

---

## ⚡ Performance

### Backend

- Reflection executada apenas no startup
- Resultado cacheado em MetadataService
- Zero overhead em runtime

### Frontend

- Lazy loading de opções
- Debounce em searchable (300ms)
- Virtual scrolling em listas grandes
- Cache de requests com React Query

---

## 🔮 Futuro

### Próximas Features

- [ ] Campo `renderAs` para forçar Select ou Typeahead
- [ ] Suporte a filtros com múltipla seleção
- [ ] Filtros dependentes (cascading)
- [ ] Display composto (ex: "nome - email")
- [ ] Agrupamento de opções

### Considerações

- [ ] Adicionar @DisplayLabel secundário
- [ ] Suporte a i18n no labelField
- [ ] Pré-carregamento de opções
- [ ] Infinite scroll nativo

---

## 🔗 Referências

- [Metadata Architecture](../architecture/METADATA_ARCHITECTURE.md)
- [Filters Guide](../api/FILTERS_GUIDE.md)
- [Select vs Typeahead](./SELECT_VS_TYPEAHEAD.md)
