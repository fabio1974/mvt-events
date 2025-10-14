# Correção: @DisplayLabel no FormFields

## Problema

Campos anotados com `@DisplayLabel` não estavam sempre visíveis no array `formFields` do metadata, mesmo sendo essenciais para identificar entidades em relacionamentos.

## Exemplo

```java
@Entity
public class EventCategory {
    @DisplayLabel
    @Computed(function = "categoryName", dependencies = {...})
    private String name;
}
```

O campo `name` precisa estar no `formFields` porque:

- É usado como label em selects/dropdowns
- É referenciado em relacionamentos (ex: Event → EventCategory)
- O frontend precisa saber qual campo exibir

## Solução Implementada

### Arquivo: `MetadataService.java`

#### 1. Modificação no processamento de formFields

**Antes:**

```java
List<FieldMetadata> formFields = fields.stream()
    .filter(f -> !isSystemField(f.getName()))
    .map(f -> {
        FieldMetadata copy = copyField(f);
        if (f.getHiddenFromForm() != null && f.getHiddenFromForm()) {
            copy.setVisible(false);
        }
        return copy;
    })
    .toList();
```

**Depois:**

```java
List<FieldMetadata> formFields = fields.stream()
    .filter(f -> !isSystemField(f.getName()))
    .map(f -> {
        FieldMetadata copy = copyField(f);

        // ⚠️ IMPORTANTE: Campos com @DisplayLabel devem SEMPRE estar visíveis
        boolean isDisplayLabel = isDisplayLabelField(config.entityClass, f.getName());

        if (isDisplayLabel) {
            copy.setVisible(true);  // Sempre visível
        } else if (f.getHiddenFromForm() != null && f.getHiddenFromForm()) {
            copy.setVisible(false);
        }
        return copy;
    })
    .toList();
```

#### 2. Novo método auxiliar

```java
/**
 * Verifica se um campo tem a anotação @DisplayLabel.
 * Campos com @DisplayLabel devem sempre estar visíveis no formFields
 * porque são usados como label em relacionamentos.
 */
private boolean isDisplayLabelField(Class<?> entityClass, String fieldName) {
    try {
        java.lang.reflect.Field field = entityClass.getDeclaredField(fieldName);
        return field.isAnnotationPresent(DisplayLabel.class);
    } catch (NoSuchFieldException e) {
        // Campo pode estar em superclasse
        Class<?> superclass = entityClass.getSuperclass();
        if (superclass != null) {
            try {
                java.lang.reflect.Field field = superclass.getDeclaredField(fieldName);
                return field.isAnnotationPresent(DisplayLabel.class);
            } catch (NoSuchFieldException ex) {
                return false;
            }
        }
        return false;
    }
}
```

## Comportamento Garantido

### Campos com @DisplayLabel

- ✅ **SEMPRE** incluídos no `formFields`
- ✅ **SEMPRE** com `visible: true`
- ✅ Mesmo que tenham `@Computed`
- ✅ Mesmo que tenham `@Visible(form = false)`

### Outros campos

- Comportamento normal baseado em `@Visible(form)`
- Se `@Visible(form = false)` → `visible: false`
- Se sem anotação → `visible: true` (padrão)

## Resultado Esperado

### Endpoint: `GET /api/metadata/eventCategory`

```json
{
  "formFields": [
    {
      "name": "name",
      "label": "Nome",
      "type": "string",
      "visible": true, // ← Sempre true
      "computed": "categoryName",
      "computedDependencies": [
        "distance",
        "gender",
        "minAge",
        "maxAge",
        "distanceUnit"
      ]
    },
    {
      "name": "distance",
      "label": "Distância",
      "type": "number",
      "visible": true
    }
    // ... outros campos
  ]
}
```

## Entidades Afetadas

Atualmente têm `@DisplayLabel`:

- ✅ `Event.name`
- ✅ `EventCategory.name`
- ✅ `User.email`
- ✅ `Organization.name`

## Teste

Execute o script de teste:

```bash
./test-displaylabel-formfields.sh
```

Ou teste manualmente:

```bash
# 1. Inicie o servidor
./gradlew bootRun

# 2. Em outro terminal, teste o endpoint
curl -s http://localhost:8080/api/metadata/eventCategory | jq '.formFields[] | select(.name == "name")'
```

**Resultado esperado:**

```json
{
  "name": "name",
  "label": "Nome",
  "type": "string",
  "visible": true,
  "computed": "categoryName",
  "computedDependencies": [...]
}
```

## Benefícios

1. **Consistência**: Campos de label sempre disponíveis
2. **UX**: Frontend sempre sabe qual campo exibir em relacionamentos
3. **Manutenibilidade**: Regra clara e documentada
4. **Flexibilidade**: Funciona com campos computed e não-computed

## Próximos Passos

Se precisar de mais customização, pode-se:

- Adicionar propriedade `isDisplayLabel: true` no metadata
- Criar endpoint específico `/api/metadata/{entity}/displayLabel`
- Adicionar validação para garantir que cada entidade tem exatamente 1 @DisplayLabel
