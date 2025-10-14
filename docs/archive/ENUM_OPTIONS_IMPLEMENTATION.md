# ✅ Sistema de Metadata Genérico Baseado em JPA

## 🎯 Problema Resolvido

O sistema agora **extrai metadata automaticamente das entidades JPA** usando reflexão. Não há mais providers específicos por entidade!

---

## 🏗️ Nova Arquitetura

### 1. JpaMetadataExtractor - Extrator Genérico

**Arquivo:** `src/main/java/com/mvt/mvt_events/metadata/JpaMetadataExtractor.java`

Este componente lê as anotações JPA e gera metadata automaticamente:

- ✅ **@Column** → `required`, `maxLength`, validações
- ✅ **@Enumerated** + Enum → `type="select"` + `options` auto-extraídas
- ✅ **@OneToMany** → `RelationshipMetadata` com campos recursivos
- ✅ **Enum.getDisplayName()** → Labels traduzidos nas options

### 2. Metadata vem das Entidades

**Event.eventType**

```java
@Enumerated(EnumType.STRING)
@Column(nullable = false)
private EventType eventType;

public enum EventType {
    RUNNING("Corrida"),
    CYCLING("Ciclismo"),
    // ...

    private final String displayName;
    EventType(String displayName) { this.displayName = displayName; }
    public String getDisplayName() { return displayName; }
}
```

**EventCategory.gender**

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
    Gender(String displayName) { this.displayName = displayName; }
    public String getDisplayName() { return displayName; }
}
```

---

## 📤 JSON Retornado pela API

### GET /api/metadata/event

```json
{
  "name": "event",
  "label": "Eventos",
  "endpoint": "/api/events",
  "fields": [
    {
      "name": "eventType",
      "label": "Event Type",
      "type": "select",
      "required": true,
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
      "required": true,
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
      "relationship": {
        "type": "ONE_TO_MANY",
        "targetEntity": "eventCategory",
        "fields": [
          {
            "name": "gender",
            "label": "Gender",
            "type": "select",
            "options": [
              { "value": "MALE", "label": "Masculino" },
              { "value": "FEMALE", "label": "Feminino" },
              { "value": "MIXED", "label": "Misto" },
              { "value": "OTHER", "label": "Outro" }
            ]
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
          }
        ]
      }
    }
  ]
}
```

---

## 🎨 Como o Frontend Usa

### Antes (SEM options):

```tsx
// ❌ Select vazio - sem opções
<select name="eventType">{/* Nenhuma opção renderizada */}</select>
```

**Console:**

```
📋 Renderizando campo SELECT: eventType
   Options presentes: NÃO
   Quantidade: 0
```

### Depois (COM options automáticas):

```tsx
// ✅ Select com opções extraídas do Enum
<select name="eventType" required>
  <option value="">Selecione o esporte</option>
  <option value="RUNNING">Corrida</option>
  <option value="CYCLING">Ciclismo</option>
  <option value="TRIATHLON">Triathlon</option>
  {/* ... opções geradas automaticamente */}
</select>
```

**Console:**

```
📋 Renderizando campo SELECT: eventType
   Type: select
   Options presentes: SIM
   Quantidade: 10
```

---

## 🔄 Como Funciona

1. **Entity tem Enum:**

   ```java
   @Enumerated(EnumType.STRING)
   private Gender gender;
   ```

2. **JpaMetadataExtractor detecta:**

   - Campo é `@Enumerated` ✅
   - Tipo é Enum ✅
   - Define `type = "select"` ✅

3. **Extrai options do Enum:**

   ```java
   for (Object constant : Gender.values()) {
       String value = ((Enum<?>) constant).name();  // "MALE"
       String label = constant.getDisplayName();     // "Masculino"
       options.add(new FilterOption(value, label));
   }
   ```

4. **Frontend recebe JSON pronto:**
   ```json
   {
     "type": "select",
     "options": [{ "value": "MALE", "label": "Masculino" }]
   }
   ```

---

## 📝 Arquivos Importantes

### ✅ Criados

1. **`JpaMetadataExtractor.java`** - Extrator genérico via reflexão
2. **`docs/JPA_METADATA_REFACTORING.md`** - Documentação da arquitetura

### 🗑️ Removidos

1. **`EventCategoryMetadataProvider.java`** - Provider específico (não é mais necessário!)

### 📄 Modificados

1. **`FieldMetadata.java`** - Adicionado campo `options`
2. **`MetadataService.java`** - Mudou de `type="enum"` para `type="select"` (correção do bug)

---

## ✅ Checklist Completo

- [x] Criar `JpaMetadataExtractor` com reflexão
- [x] Detectar campos `@Enumerated` automaticamente
- [x] Extrair `options` de Enums com `getDisplayName()`
- [x] Detectar relacionamentos `@OneToMany`
- [x] Gerar `RelationshipMetadata` recursivamente
- [x] Corrigir bug: `type="enum"` → `type="select"`
- [x] Remover `EventCategoryMetadataProvider` (não é mais necessário)
- [x] Documentação completa

---

## 🎉 Resultado Final

**Todos os campos ENUM agora são extraídos automaticamente das entidades JPA!**

- ✅ Não precisa criar providers específicos
- ✅ Mudanças no Enum refletem automaticamente na API
- ✅ `type="select"` correto para campos com options
- ✅ Frontend renderiza dropdowns populados
- ✅ Sistema 100% baseado em metadata genérica

### Adicionar novo Enum? É só:

1. Criar o Enum na entidade:

   ```java
   public enum MyEnum {
       OPTION_1("Label 1"),
       OPTION_2("Label 2");

       private final String displayName;
       MyEnum(String displayName) { this.displayName = displayName; }
       public String getDisplayName() { return displayName; }
   }
   ```

2. Anotar o campo:

   ```java
   @Enumerated(EnumType.STRING)
   private MyEnum myField;
   ```

3. **Pronto!** A metadata já tem `options` automaticamente! 🚀

---

## 📤 JSON Retornado pela API

### GET /api/metadata/event

```json
{
  "name": "event",
  "label": "Eventos",
  "endpoint": "/api/events",
  "fields": [
    {
      "name": "eventType",
      "label": "Esporte",
      "type": "enum",
      "sortable": true,
      "searchable": true,
      "visible": true,
      "width": 120,
      "align": "left",
      "required": true,
      "placeholder": "Selecione o esporte",
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
      "type": "enum",
      "sortable": true,
      "searchable": true,
      "visible": true,
      "width": 120,
      "align": "center",
      "required": true,
      "options": [
        { "value": "DRAFT", "label": "Rascunho" },
        { "value": "PUBLISHED", "label": "Publicado" },
        { "value": "CANCELLED", "label": "Cancelado" },
        { "value": "COMPLETED", "label": "Finalizado" }
      ]
    },
    {
      "name": "categories",
      "label": "Categorias do Evento",
      "type": "nested",
      "visible": false,
      "sortable": false,
      "searchable": false,
      "relationship": {
        "type": "ONE_TO_MANY",
        "targetEntity": "eventCategory",
        "targetEndpoint": "/api/event-categories",
        "cascade": true,
        "orphanRemoval": true,
        "fields": [
          {
            "name": "gender",
            "label": "Gênero",
            "type": "select",
            "required": false,
            "placeholder": "Selecione o gênero",
            "options": [
              { "value": "MALE", "label": "Masculino" },
              { "value": "FEMALE", "label": "Feminino" },
              { "value": "MIXED", "label": "Misto" },
              { "value": "OTHER", "label": "Outro" }
            ]
          },
          {
            "name": "distanceUnit",
            "label": "Unidade",
            "type": "select",
            "required": true,
            "options": [
              { "value": "KM", "label": "Quilômetros (km)" },
              { "value": "MILES", "label": "Milhas (mi)" },
              { "value": "METERS", "label": "Metros (m)" }
            ]
          }
        ]
      }
    }
  ]
}
```

---

## 🎨 Como o Frontend Usa

### Antes (SEM options):

```tsx
// ❌ Select vazio - sem opções
<select name="eventType">{/* Nenhuma opção renderizada */}</select>
```

**Console:**

```
📋 Renderizando campo SELECT: eventType
   Label: Esporte
   Options presentes: NÃO
   Quantidade: 0
```

### Depois (COM options):

```tsx
// ✅ Select com opções do backend
<select name="eventType" required>
  <option value="">Selecione o esporte</option>
  <option value="RUNNING">Corrida</option>
  <option value="CYCLING">Ciclismo</option>
  <option value="TRIATHLON">Triathlon</option>
  <option value="SWIMMING">Natação</option>
  {/* ... mais opções */}
</select>
```

**Console:**

```
📋 Renderizando campo SELECT: eventType
   Label: Esporte
   Options presentes: SIM
   Quantidade: 10
```

---

## 🔄 Pattern Reutilizável

Para adicionar options a qualquer campo enum futuro:

```java
// 1. Crie o campo
FieldMetadata myEnumField = new FieldMetadata("fieldName", "Label", "enum");

// 2. Configure propriedades básicas
myEnumField.setRequired(true);
myEnumField.setPlaceholder("Selecione...");

// 3. Adicione as options
myEnumField.setOptions(Arrays.asList(
    new FilterOption("VALUE_1", "Label 1"),
    new FilterOption("VALUE_2", "Label 2"),
    new FilterOption("VALUE_3", "Label 3")
));

// 4. Adicione aos fields
fields.add(myEnumField);
```

---

## 📝 Arquivos Modificados

### ✅ Modificados

1. `src/main/java/com/mvt/mvt_events/metadata/FieldMetadata.java`

   - Adicionado campo `options`
   - Adicionados getters/setters

2. `src/main/java/com/mvt/mvt_events/metadata/MetadataService.java`

   - `eventType` com 10 options
   - `status` com 4 options

3. `src/main/java/com/mvt/mvt_events/metadata/providers/EventCategoryMetadataProvider.java`
   - `gender` com 4 options
   - `distanceUnit` com 3 options

### 📄 Criados

1. `docs/ENUM_OPTIONS_IMPLEMENTATION.md` (este arquivo)

---

## ✅ Checklist Completo

- [x] Classe `FilterOption` já existe em `FilterMetadata`
- [x] Campo `options` adicionado em `FieldMetadata`
- [x] Getters/Setters para `options` implementados
- [x] Opções de `eventType` adicionadas (10 opções)
- [x] Opções de `status` adicionadas (4 opções)
- [x] Opções de `gender` (em eventCategory) adicionadas (4 opções)
- [x] Opções de `distanceUnit` (em eventCategory) adicionadas (3 opções)
- [x] Compilação sem erros
- [x] Documentação completa

---

## 🧪 Como Testar

### 1. Iniciar Servidor

```bash
./gradlew bootRun
```

### 2. Testar Endpoint

```bash
# Ver campo eventType com options
curl -s http://localhost:8080/api/metadata/event | jq '.fields[] | select(.name == "eventType")'

# Ver campo status com options
curl -s http://localhost:8080/api/metadata/event | jq '.fields[] | select(.name == "status")'

# Ver campos nested com options
curl -s http://localhost:8080/api/metadata/event | jq '.fields[] | select(.name == "categories") | .relationship.fields[] | select(.name == "gender")'
```

### 3. Verificar no Frontend

O console não deve mais mostrar:

```
Options presentes: NÃO
Quantidade: 0
```

Deve mostrar:

```
Options presentes: SIM
Quantidade: 10  (para eventType)
Quantidade: 4   (para status)
```

---

## 🎉 Resultado Final

**Todos os campos ENUM agora têm options** e renderizam corretamente no frontend!

- ✅ `eventType` → 10 opções de esportes
- ✅ `status` → 4 opções de status
- ✅ `gender` (nested) → 4 opções de gênero
- ✅ `distanceUnit` (nested) → 3 opções de unidade

O frontend agora pode renderizar formulários 100% dinâmicos com selects populados automaticamente pelo backend! 🚀
