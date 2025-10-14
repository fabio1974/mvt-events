# Implementação de Campos Computados - Backend

## ✅ Implementação Completa

### 1. Anotação `@Computed`

**Localização:** `/src/main/java/com/mvt/mvt_events/metadata/Computed.java`

```java
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Computed {
    /**
     * Nome da função de cálculo a ser executada pelo frontend.
     * Exemplo: "categoryName"
     */
    String function();

    /**
     * Lista de campos que, quando mudarem, disparam o recálculo.
     * Exemplo: {"distance", "gender", "minAge", "maxAge"}
     */
    String[] dependencies();
}
```

### 2. Aplicação na Entidade `EventCategory`

**Localização:** `/src/main/java/com/mvt/mvt_events/jpa/EventCategory.java`

```java
@Entity
@Table(name = "event_categories")
public class EventCategory extends BaseEntity {

    @DisplayLabel
    @Computed(function = "categoryName", dependencies = {"distance", "gender", "minAge", "maxAge"})
    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "min_age")
    private Integer minAge;

    @Column(name = "max_age")
    private Integer maxAge;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Gender gender;

    @Column(precision = 10, scale = 2)
    private BigDecimal distance;

    // ... outros campos
}
```

### 3. Metadata Estendido

**Campos adicionados em `FieldMetadata`:**

```java
// Computed field properties (@Computed)
private String computed; // Nome da função de cálculo (ex: "categoryName")
private List<String> computedDependencies; // Campos que disparam recálculo
```

**Getters e Setters:**

- `getComputed()` / `setComputed(String)`
- `getComputedDependencies()` / `setComputedDependencies(List<String>)`

### 4. Processamento no Extrator

**Localização:** `/src/main/java/com/mvt/mvt_events/metadata/JpaMetadataExtractor.java`

Método `createFieldMetadata` agora processa `@Computed`:

```java
// ✅ Verifica anotação @Computed para campos computados
if (field.isAnnotationPresent(Computed.class)) {
    Computed computed = field.getAnnotation(Computed.class);
    metadata.setComputed(computed.function());
    metadata.setComputedDependencies(java.util.Arrays.asList(computed.dependencies()));

    System.out.println("DEBUG: Campo '" + field.getName() + "' é computado - function="
            + computed.function() + ", dependencies=" + Arrays.toString(computed.dependencies()));
}
```

### 5. Cópia de Metadata

**Localização:** `/src/main/java/com/mvt/mvt_events/metadata/MetadataService.java`

Método `copyField` agora copia propriedades computadas:

```java
copy.setComputed(source.getComputed());
copy.setComputedDependencies(source.getComputedDependencies());
```

## 📋 Metadata JSON Gerado

### Exemplo de Campo Computado no Metadata

```json
{
  "name": "name",
  "label": "Nome",
  "type": "string",
  "required": true,
  "maxLength": 100,
  "width": 200,
  "computed": "categoryName",
  "computedDependencies": ["distance", "gender", "minAge", "maxAge"]
}
```

### Endpoint de Teste

```bash
GET /api/metadata/eventCategory
```

**Resposta:**

```json
{
  "entityName": "eventCategory",
  "label": "Categorias",
  "endpoint": "/api/event-categories",
  "formFields": [
    {
      "name": "distance",
      "label": "Distância",
      "type": "currency"
    },
    {
      "name": "gender",
      "label": "Gênero",
      "type": "select",
      "options": [...]
    },
    {
      "name": "minAge",
      "label": "Idade Mínima",
      "type": "number"
    },
    {
      "name": "maxAge",
      "label": "Idade Máxima",
      "type": "number"
    },
    {
      "name": "name",
      "label": "Nome",
      "type": "string",
      "required": true,
      "computed": "categoryName",
      "computedDependencies": ["distance", "gender", "minAge", "maxAge"]
    }
  ]
}
```

## 🔄 Fluxo de Funcionamento

### 1. Frontend Carrega Metadata

```typescript
const metadata = await fetchMetadata("eventCategory");
const nameField = metadata.formFields.find((f) => f.name === "name");

if (nameField.computed) {
  // Campo é computado!
  // function: "categoryName"
  // dependencies: ["distance", "gender", "minAge", "maxAge"]
}
```

### 2. Frontend Observa Dependências

```typescript
// Quando distance, gender, minAge ou maxAge mudam:
watch([distance, gender, minAge, maxAge], () => {
  name.value = computeCategoryName(distance, gender, minAge, maxAge);
});
```

### 3. Frontend Envia Dados

```json
POST /api/event-categories
{
  "distance": 5,
  "gender": "MALE",
  "minAge": 30,
  "maxAge": 39,
  "name": "5KM - Masculino - 30 a 39 anos",
  "price": 50.00
}
```

### 4. Backend Aceita Dados

```java
@PostMapping
public EventCategory create(@RequestBody EventCategory category) {
    // Backend simplesmente salva o valor calculado
    // Não precisa recalcular - frontend já fez isso
    return repository.save(category);
}
```

## ✅ Checklist de Implementação Backend

- [x] ✅ Criar anotação `@Computed`
- [x] ✅ Adicionar campos `computed` e `computedDependencies` em `FieldMetadata`
- [x] ✅ Processar `@Computed` no `JpaMetadataExtractor`
- [x] ✅ Copiar propriedades computadas no `MetadataService.copyField()`
- [x] ✅ Aplicar `@Computed` no campo `name` de `EventCategory`
- [x] ✅ Testar metadata endpoint
- [x] ✅ Validar JSON gerado

## 🧪 Testes

### Teste do Metadata

```bash
curl -s 'http://localhost:8080/api/metadata/eventCategory' | \
  jq '.formFields[] | select(.name == "name")'
```

**Resultado Esperado:**

```json
{
  "name": "name",
  "label": "Nome",
  "type": "string",
  "required": true,
  "maxLength": 100,
  "computed": "categoryName",
  "computedDependencies": ["distance", "gender", "minAge", "maxAge"]
}
```

### Teste de CRUD

```bash
# Criar categoria com nome computado
curl -X POST http://localhost:8080/api/event-categories \
  -H 'Content-Type: application/json' \
  -H 'Authorization: Bearer TOKEN' \
  -d '{
    "eventId": 1,
    "distance": 5,
    "gender": "MALE",
    "minAge": 30,
    "maxAge": 39,
    "name": "5KM - Masculino - 30 a 39 anos",
    "price": 50.00
  }'
```

**Backend deve:**

- ✅ Aceitar o campo `name` normalmente
- ✅ Salvar no banco de dados
- ✅ Retornar a entidade salva com todos os campos

## 📝 Validações

### Backend (Atual)

```java
@Entity
public class EventCategory {
    @NotBlank(message = "Nome é obrigatório")
    @Size(max = 100, message = "Nome muito longo")
    @Column(nullable = false, length = 100)
    private String name;
}
```

**Validações aplicadas:**

- ✅ `required: true` → Campo obrigatório
- ✅ `maxLength: 100` → Máximo 100 caracteres
- ✅ Bean Validation funciona normalmente

## 🚀 Como Adicionar Novos Campos Computados

### 1. Defina a função no frontend

```typescript
// src/utils/computedFields.ts
export function computeFullAddress(street, number, city, state) {
  if (!street || !city || !state) return "Endereço Incompleto";
  const num = number || "s/n";
  return `${street}, ${num} - ${city}/${state}`;
}
```

### 2. Registre a função

```typescript
// src/utils/computedFields.ts
export const computedFieldFunctions = {
  categoryName: computeCategoryName,
  fullAddress: computeFullAddress, // ← Nova função
};
```

### 3. Aplique no backend

```java
@Entity
public class Address {
    @Computed(
        function = "fullAddress",
        dependencies = {"street", "number", "city", "state"}
    )
    private String fullAddress;

    private String street;
    private String number;
    private String city;
    private String state;
}
```

### 4. Pronto!

O metadata será gerado automaticamente:

```json
{
  "name": "fullAddress",
  "label": "Endereço Completo",
  "computed": "fullAddress",
  "computedDependencies": ["street", "number", "city", "state"]
}
```

## 📚 Documentação Relacionada

- Frontend: `/docs/frontend/COMPUTED_FIELDS_GUIDE.md`
- Anotações Metadata: `/docs/backend/METADATA_ANNOTATIONS.md`
- API Metadata: `/docs/api/METADATA_ENDPOINT.md`

## ✨ Benefícios

1. **Consistência**: Cálculos sempre corretos
2. **Manutenibilidade**: Lógica centralizada no frontend
3. **Performance**: Cálculo instantâneo sem round-trip ao backend
4. **Usabilidade**: Usuário não precisa digitar manualmente
5. **Extensibilidade**: Fácil adicionar novas funções

---

**Status:** ✅ Implementação completa e testada
**Data:** 2025-10-11
**Versão:** 1.0
