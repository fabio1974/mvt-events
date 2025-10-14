# 🚀 Quick Reference - Annotations Customizadas

> **Cheat sheet** rápido para consulta durante o desenvolvimento

---

## 📌 Annotations Disponíveis

```java
@DisplayLabel              // Marca o campo principal da entidade
@Visible                   // Controla visibilidade (table/form/filter)
@Computed                  // Campo calculado automaticamente
```

---

## 🎯 @DisplayLabel - Campo Principal

### ✅ Usar quando

- Campo que melhor identifica a entidade
- Aparecerá em dropdowns e listas
- Geralmente `name`, `title`, `description`

### 📝 Sintaxe

```java
@DisplayLabel
private String name;
```

### ⚡ Efeito

```json
{
  "labelField": "name",
  "formFields": [
    {
      "name": "name",
      "visible": true // ← SEMPRE visível
    }
  ]
}
```

### ⚠️ Regras

- ✅ **UM por entidade**
- ✅ **Sempre visível** no form (sobrescreve `@Visible(form = false)`)
- ✅ **Tipo String** recomendado

---

## 👁️ @Visible - Controle de Visibilidade

### 📊 Matriz de Uso

| Caso de Uso                      | Annotation                                              |
| -------------------------------- | ------------------------------------------------------- |
| **Campo TEXT grande**            | `@Visible(table = false, filter = false)`               |
| **Campo calculado no backend**   | `@Visible(form = false)`                                |
| **Relacionamento parent**        | `@Visible(form = false)`                                |
| **Campo interno (slug, hash)**   | `@Visible(table = false, form = false, filter = false)` |
| **Relacionamento 1:N (cascade)** | `@Visible(form = true)`                                 |

### 📝 Sintaxes Comuns

```java
// 1. Campo grande (description, termsAndConditions)
@Visible(table = false, filter = false)
@Column(columnDefinition = "TEXT")
private String description;

// 2. Campo calculado (currentParticipants, totalPrice)
@Visible(form = false)
@Column(name = "current_participants")
private Integer currentParticipants;

// 3. Relacionamento parent
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "event_id", nullable = false)
@Visible(form = false)
private Event event;

// 4. Campo interno (slug, hash, token)
@Visible(table = false, form = false, filter = false)
@Column(unique = true)
private String slug;

// 5. Relacionamento 1:N com cascade
@Visible(form = true)
@OneToMany(mappedBy = "event", cascade = CascadeType.ALL)
private List<EventCategory> categories;
```

### 🎨 Resultados no Metadata

| Annotation                                              | Tabela | Form | Filtros |
| ------------------------------------------------------- | ------ | ---- | ------- |
| _(padrão)_                                              | ✅     | ✅   | ✅      |
| `@Visible(table = false)`                               | ❌     | ✅   | ✅      |
| `@Visible(form = false)`                                | ✅     | ❌   | ✅      |
| `@Visible(filter = false)`                              | ✅     | ✅   | ❌      |
| `@Visible(table = false, filter = false)`               | ❌     | ✅   | ❌      |
| `@Visible(table = false, form = false, filter = false)` | ❌     | ❌   | ❌      |

---

## 🧮 @Computed - Campos Calculados

### ✅ Usar quando

- Campo é gerado automaticamente pelo frontend
- Valor depende de outros campos
- Usuário não deve editar manualmente

### 📝 Sintaxe

```java
@Computed(
    function = "nomeDaFuncao",
    dependencies = {"campo1", "campo2"}
)
private String fieldName;
```

### ⚡ Funções Disponíveis

| Função         | Descrição                   | Dependências                                             |
| -------------- | --------------------------- | -------------------------------------------------------- |
| `categoryName` | Nome de categoria esportiva | `distance`, `gender`, `minAge`, `maxAge`, `distanceUnit` |

### 💡 Exemplo Completo

```java
@Entity
public class EventCategory extends BaseEntity {

    private BigDecimal distance;
    private Gender gender;
    private Integer minAge;
    private Integer maxAge;
    private DistanceUnit distanceUnit;

    @DisplayLabel
    @Computed(
        function = "categoryName",
        dependencies = {"distance", "gender", "minAge", "maxAge", "distanceUnit"}
    )
    private String name;
}
```

**Resultado Frontend:**

```
Entrada: distance=5, gender=MALE, minAge=30, maxAge=39, distanceUnit=KM
Saída: "5KM - Masculino - 30 a 39 anos"
```

### ⚠️ Regras

- ✅ **Readonly** no frontend
- ✅ **Recalculado** quando dependências mudam
- ✅ **Backend aceita o valor** (não valida)

---

## 🔗 Combinações Comuns

### 1️⃣ Label Calculada

```java
@DisplayLabel
@Computed(function = "categoryName", dependencies = {"distance", "gender"})
private String name;
```

### 2️⃣ Campo Interno Oculto

```java
@Visible(table = false, form = false, filter = false)
@Column(unique = true)
private String slug;
```

### 3️⃣ Relacionamento Parent

```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "event_id", nullable = false)
@JsonIgnore
@Visible(form = false)
private Event event;
```

### 4️⃣ Campo TEXT Grande

```java
@Visible(table = false, filter = false)
@Size(max = 500)
@Column(columnDefinition = "TEXT")
private String description;
```

### 5️⃣ Contador Backend

```java
@Visible(form = false)
@Column(name = "current_participants")
private Integer currentParticipants = 0;
```

---

## 📋 Template de Nova Entidade

```java
@Entity
@Table(name = "table_name")
@Data
@EqualsAndHashCode(callSuper = true)
public class MyEntity extends BaseEntity {

    // 1. Relacionamento parent (se houver)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id", nullable = false)
    @Visible(form = false)
    private Parent parent;

    // 2. Campo principal (label)
    @DisplayLabel
    @Column(nullable = false)
    private String name;

    // 3. Campo interno (slug, hash, etc)
    @Visible(table = false, form = false, filter = false)
    @Column(unique = true)
    private String slug;

    // 4. Campos normais
    @Column(nullable = false)
    private String field1;

    private String field2;

    // 5. Campo TEXT grande
    @Visible(table = false, filter = false)
    @Column(columnDefinition = "TEXT")
    private String description;

    // 6. Enums
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Status status = Status.ACTIVE;

    // 7. Campo calculado (se houver)
    @Visible(form = false)
    private Integer calculatedField;

    // 8. Relacionamento 1:N (se houver)
    @Visible(form = true)
    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL)
    private List<Child> children = new ArrayList<>();

    // 9. Enum com tradução
    public enum Status {
        ACTIVE("Ativo"),
        INACTIVE("Inativo");

        private final String displayName;

        Status(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }
}
```

---

## 🔢 Ordem de Precedência

**Do mais importante para o menos:**

1. **`@DisplayLabel`** → Força `visible = true` no form
2. **`@Visible`** → Controla visibilidade em cada contexto
3. **`@Computed`** → Define `readonly = true`
4. **JPA Annotations** → Extrai tipo, required, etc.

### Exemplo

```java
@DisplayLabel                // 1. PRIORIDADE MÁXIMA
@Computed(function = "...")  // 2. Readonly + computed metadata
@Visible(form = false)       // 3. IGNORADO (DisplayLabel vence)
@Column(nullable = false)    // 4. required = true
private String name;
```

**Resultado:**

```json
{
  "name": "name",
  "required": true,      // ← @Column
  "visible": true,       // ← @DisplayLabel (ignora @Visible)
  "readonly": true,      // ← @Computed
  "computed": { ... }    // ← @Computed
}
```

---

## ✅ Checklist Rápido

Ao criar uma nova entidade:

- [ ] ✅ Um campo tem `@DisplayLabel`?
- [ ] ✅ Campos TEXT têm `@Visible(table = false, filter = false)`?
- [ ] ✅ Relacionamento parent tem `@Visible(form = false)`?
- [ ] ✅ Campos calculados têm `@Visible(form = false)` ou `@Computed`?
- [ ] ✅ Relacionamento bidirecional tem `@JsonIgnore` no child?
- [ ] ✅ Enums têm `getDisplayName()`?
- [ ] ✅ Campos internos estão ocultos?
- [ ] ✅ Relacionamento 1:N tem `@Visible(form = true)` se precisa cascade?

---

## 🐛 Troubleshooting Rápido

| Problema                                                  | Causa                             | Solução                        |
| --------------------------------------------------------- | --------------------------------- | ------------------------------ |
| Campo com `@DisplayLabel` não aparece no form             | Bug antigo                        | Atualizar para v1.1.0+         |
| `@Visible(form = false)` não funciona com `@DisplayLabel` | Comportamento esperado            | `@DisplayLabel` tem prioridade |
| Campo `@Computed` não recalcula                           | Função não existe ou deps erradas | Verificar `computedFields.js`  |
| Loop infinito no JSON                                     | Falta `@JsonIgnore`               | Adicionar no lado child        |

---

## 📚 Documentação Completa

Para detalhes completos, exemplos e exercícios:

👉 [ANNOTATIONS_GUIDE.md](./ANNOTATIONS_GUIDE.md)

---

**📅 Última atualização:** 14 de outubro de 2025  
**📌 Versão:** 1.1.0  
**✍️ MVT Events Team**
