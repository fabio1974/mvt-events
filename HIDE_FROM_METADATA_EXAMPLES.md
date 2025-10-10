# 📝 Como usar @HideFromMetadata

A anotação `@HideFromMetadata` permite ocultar campos específicos do metadata enviado ao frontend.

## 🎯 Sintaxe

```java
@HideFromMetadata // Oculta de tudo (tabela, form e filtros) por padrão
@HideFromMetadata(table = true) // Oculta apenas da tabela
@HideFromMetadata(form = true) // Oculta apenas do formulário
@HideFromMetadata(filter = true) // Oculta apenas dos filtros
@HideFromMetadata(table = true, filter = true) // Oculta da tabela e filtros
```

## 📋 Exemplos Práticos

### 1. Ocultar senha de tudo (mais comum)

```java
@Entity
public class User extends BaseEntity {

    @HideFromMetadata  // ← Oculta de tabela, form e filtros
    private String password;

    private String email;
    private String name;
}
```

**Resultado:**

- ❌ Não aparece na tabela
- ❌ Não aparece no formulário
- ❌ Não aparece nos filtros

---

### 2. Ocultar descrição apenas da tabela (campo grande)

```java
@Entity
public class Event extends BaseEntity {

    private String name;

    @HideFromMetadata(table = true)  // ← Oculta só da tabela
    @Column(columnDefinition = "TEXT")
    private String description;
}
```

**Resultado:**

- ❌ Não aparece na tabela (economiza espaço)
- ✅ Aparece no formulário (pode editar)
- ✅ Aparece nos filtros (pode buscar)

---

### 3. Ocultar slug do formulário (gerado automaticamente)

```java
@Entity
public class Event extends BaseEntity {

    private String name;

    @HideFromMetadata(form = true)  // ← Oculta só do form
    @Column(unique = true)
    private String slug;
}
```

**Resultado:**

- ✅ Aparece na tabela (exibe o slug)
- ❌ Não aparece no formulário (gerado pelo backend a partir do nome)
- ✅ Aparece nos filtros (pode buscar por slug)

---

### 4. Ocultar notas internas da tabela e filtros

```java
@Entity
public class Event extends BaseEntity {

    private String name;

    @HideFromMetadata(table = true, filter = true)  // ← Oculta de 2 contextos
    private String internalNotes;
}
```

**Resultado:**

- ❌ Não aparece na tabela
- ✅ Aparece no formulário (admin pode adicionar notas)
- ❌ Não aparece nos filtros

---

### 5. Campo completamente oculto (temporário/calculado)

```java
@Entity
public class Event extends BaseEntity {

    private String name;

    @HideFromMetadata  // ← Oculta de tudo
    @Transient  // ← Não persiste no DB
    private String tempCalculation;
}
```

**Resultado:**

- ❌ Não aparece na tabela
- ❌ Não aparece no formulário
- ❌ Não aparece nos filtros
- ❌ Não persiste no banco de dados

---

## 🔧 Combinação com outras anotações

```java
@Entity
public class User extends BaseEntity {

    @DisplayLabel  // ← Aparece destacado
    private String name;

    @HideFromMetadata(table = true)
    @Column(columnDefinition = "TEXT")
    private String bio;

    @HideFromMetadata  // ← Completamente oculto
    @JsonIgnore  // ← Também oculto nas respostas da API
    private String password;

    @HideFromMetadata(form = true)
    @Column(unique = true)
    private String email;  // ← Não pode editar email no form
}
```

---

## ⚙️ Prioridade de Ocultação

O sistema verifica na seguinte ordem:

1. **`@HideFromMetadata`** (anotação no campo) ← **MAIOR PRIORIDADE**
2. **`shouldHideFromTable/Form/Filter()`** (regras no MetadataService)
3. **`isSystemField()`** (campos globais de sistema)

**Exemplo:**

```java
// Na entidade
@HideFromMetadata(table = true)
private String description;

// No MetadataService
private boolean shouldHideFromTable(String entityName, String fieldName) {
    if ("description".equals(fieldName)) {
        return true;  // ← Esta regra é redundante, @HideFromMetadata já oculta
    }
    return false;
}
```

💡 **Recomendação:** Use `@HideFromMetadata` para regras específicas de campos e `shouldHideFrom*()` para regras globais.

---

## 🚀 Casos de Uso Comuns

| Caso            | Anotação                                         | Motivo                    |
| --------------- | ------------------------------------------------ | ------------------------- |
| Senha           | `@HideFromMetadata`                              | Segurança - nunca mostrar |
| Descrição longa | `@HideFromMetadata(table = true)`                | Não cabe na tabela        |
| Slug/URL        | `@HideFromMetadata(form = true)`                 | Gerado automaticamente    |
| Notas internas  | `@HideFromMetadata(table = true, filter = true)` | Apenas para edição        |
| Token/Secret    | `@HideFromMetadata` + `@JsonIgnore`              | Segurança máxima          |
| Campo calculado | `@HideFromMetadata` + `@Transient`               | Não persiste nem exibe    |

---

## ✅ Compilar e Testar

```bash
# Compilar
./gradlew compileJava

# Rodar servidor
./gradlew bootRun

# Testar metadata
curl http://localhost:8080/api/metadata/event | jq '.tableFields[] | select(.name == "description")'
# Deve retornar vazio se @HideFromMetadata(table = true)
```

---

## 📊 Antes x Depois

### SEM @HideFromMetadata

```json
{
  "tableFields": [
    { "name": "name", "label": "Nome" },
    { "name": "description", "label": "Descrição" }, // ← Aparece!
    { "name": "password", "label": "Senha" } // ← PROBLEMA!
  ]
}
```

### COM @HideFromMetadata

```java
@HideFromMetadata(table = true)
private String description;

@HideFromMetadata
private String password;
```

```json
{
  "tableFields": [{ "name": "name", "label": "Nome" }]
}
```

✅ Campos sensíveis/grandes corretamente ocultados!
