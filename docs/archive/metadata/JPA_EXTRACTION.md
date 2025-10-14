# 🔄 Refatoração: Metadata Genérica Baseada em JPA

## ❌ PROBLEMA ATUAL

O sistema está criando **providers específicos** para cada entidade:
- `EventCategoryMetadataProvider.java` - hardcoded para EventCategory
- Cada entidade precisa de um provider manual
- **Quebra o conceito de metadata genérica!**

## ✅ SOLUÇÃO: Extração Automática via Reflexão

### Arquitetura Correta

```
┌─────────────────────┐
│   Entidade JPA      │
│  @Entity, @Column   │ ──────┐
│  @Enumerated, etc   │       │
└─────────────────────┘       │
                              │ Lê anotações
┌─────────────────────┐       │
│ JpaMetadataExtractor│◄──────┘
│   (Reflexão)        │
└───────┬─────────────┘
        │ Gera
        ▼
┌─────────────────────┐
│   FieldMetadata     │
│ + RelationshipMeta  │
└─────────────────────┘
```

### Componentes Criados

#### 1. JpaMetadataExtractor (✅ Criado)

**Localização:** `src/main/java/com/mvt/mvt_events/metadata/JpaMetadataExtractor.java`

**Funcionalidades:**
- ✅ Lê anotações `@Column` para validações (nullable, length, precision)
- ✅ Lê anotações `@Enumerated` e extrai options automaticamente do Enum
- ✅ Lê anotações `@OneToMany` e cria RelationshipMetadata recursivamente
- ✅ Detecta tipos automaticamente (String→string, BigDecimal→number/currency, etc.)
- ✅ Extrai displayName dos Enums via método `getDisplayName()`

**Exemplo de uso:**
```java
List<FieldMetadata> fields = metadataExtractor.extractFields(Event.class);
// Retorna TODOS os campos com metadata completa, incluindo:
// - eventType com options [RUNNING, CYCLING, ...]
// - status com options [DRAFT, PUBLISHED, ...]
// - categories com RelationshipMetadata para EventCategory
```

#### 2. MetadataService Refatorado (📝 Pendente)

**Mudanças necessárias:**

```java
@Service
public class MetadataService {
    
    @Autowired
    private JpaMetadataExtractor extractor;
    
    // ÚNICO mapa necessário - só classes e endpoints
    private static final Map<String, EntityConfig> ENTITIES = Map.of(
        "event", new EntityConfig(Event.class, "Eventos", "/api/events"),
        "eventCategory", new EntityConfig(EventCategory.class, "Categorias", "/api/event-categories")
    );
    
    public EntityMetadata getEntityMetadata(String entityName) {
        EntityConfig config = ENTITIES.get(entityName);
        
        // ✅ EXTRAÇÃO AUTOMÁTICA
        List<FieldMetadata> fields = extractor.extractFields(config.entityClass);
        
        // Apenas customizações de UI (width, align, etc.)
        customizeUI(entityName, fields);
        
        return new EntityMetadata(entityName, config.label, config.endpoint, fields);
    }
}
```

### Vantagens

1. **Zero Código Duplicado**
   - Não precisa criar `EventCategoryMetadataProvider`
   - Não precisa criar providers para outras entidades
   - Mudanças nas entities refletem automaticamente

2. **Single Source of Truth**
   - Anotações JPA são a única fonte
   - `@Column(nullable=false)` → `required=true`
   - `@Enumerated` + `Gender enum` → `options=[MALE, FEMALE, ...]`

3. **Manutenção Simples**
   - Adicionar novo enum? Só criar na entity
   - Adicionar novo campo? Só anotar com `@Column`
   - Mudar validação? Só mudar `@Column(length=100)`

### Como os Enums São Detectados

#### EventCategory.gender

**Entity:**
```java
@Enumerated(EnumType.STRING)
@Column(length = 20)
private Gender gender;

public enum Gender {
    MALE("Masculino"),
    FEMALE("Feminino"),
    MIXED("Misto"),
    OTHER("Outro");
    
    private final String displayName;
    
    Gender(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}
```

**Metadata Gerada Automaticamente:**
```json
{
  "name": "gender",
  "label": "Gender",
  "type": "select",
  "required": false,
  "options": [
    {"value": "MALE", "label": "Masculino"},
    {"value": "FEMALE", "label": "Feminino"},
    {"value": "MIXED", "label": "Misto"},
    {"value": "OTHER", "label": "Outro"}
  ]
}
```

#### Event.eventType

**Entity:**
```java
@Enumerated(EnumType.STRING)
@Column(nullable = false)
private EventType eventType;

public enum EventType {
    RUNNING("Corrida"),
    CYCLING("Ciclismo"),
    TRIATHLON("Triathlon"),
    // ...
    
    private final String displayName;
    EventType(String displayName) { this.displayName = displayName; }
    public String getDisplayName() { return displayName; }
}
```

**Metadata Gerada Automaticamente:**
```json
{
  "name": "eventType",
  "label": "Event Type",
  "type": "select",
  "required": true,
  "options": [
    {"value": "RUNNING", "label": "Corrida"},
    {"value": "CYCLING", "label": "Ciclismo"},
    // ... todas as opções do enum
  ]
}
```

### Relacionamentos OneToMany

#### Event.categories

**Entity:**
```java
@OneToMany(mappedBy = "event", cascade = CascadeType.ALL)
private List<EventCategory> categories = new ArrayList<>();
```

**Metadata Gerada Automaticamente:**
```json
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
        "options": [/* auto-extracted */]
      },
      {
        "name": "distanceUnit",
        "label": "Distance Unit",
        "type": "select",
        "options": [
          {"value": "KM", "label": "Quilômetros (km)"},
          {"value": "MILES", "label": "Milhas (mi)"},
          {"value": "METERS", "label": "Metros (m)"}
        ]
      }
      // ... todos os campos de EventCategory
    ]
  }
}
```

## 📋 Checklist de Implementação

### ✅ Fase 1 - Extrator Genérico (Completa)
- [x] Criar `JpaMetadataExtractor.java`
- [x] Implementar `extractFields()` com reflexão
- [x] Implementar detecção de tipos (string, number, date, etc.)
- [x] Implementar extração de enums com options
- [x] Implementar extração de relacionamentos OneToMany
- [x] Implementar validações via @Column (nullable, length, etc.)

### 📝 Fase 2 - Refatorar MetadataService (Pendente)
- [ ] Remover código manual de criação de campos
- [ ] Usar `metadataExtractor.extractFields()` para todas entidades
- [ ] Manter apenas customizações de UI (width, align, format)
- [ ] Testar com Event e EventCategory

### 🗑️ Fase 3 - Remover Providers (Pendente)
- [ ] Deletar `EventCategoryMetadataProvider.java`
- [ ] Deletar quaisquer outros providers específicos
- [ ] Atualizar documentação

### 🧪 Fase 4 - Testes (Pendente)
- [ ] Testar GET /api/metadata/event
- [ ] Verificar se `categories` tem relationship completo
- [ ] Verificar se `gender` e `distanceUnit` têm options
- [ ] Testar frontend renderizando formulário

## 🎯 Resultado Final

### Antes (❌ Ruim)
```
EventCategoryMetadataProvider.java  ─┐
EventMetadataProvider.java          ├── 1000+ linhas duplicadas
RegistrationMetadataProvider.java   ├── Hardcoded para cada entity
UserMetadataProvider.java           │
PaymentMetadataProvider.java        ┘
```

### Depois (✅ Bom)
```
JpaMetadataExtractor.java  ──► Lê qualquer @Entity automaticamente
MetadataService.java       ──► Só endpoints e UI configs
```

**Redução:** ~80% menos código, 100% mais manutenível! 🎉
