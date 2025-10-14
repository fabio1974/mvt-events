# 📚 Guia de Annotations Customizadas do Metadata

> **Para iniciantes no projeto** - Guia completo sobre como usar as annotations que controlam o comportamento do sistema de metadata.

---

## 📖 Índice

1. [Introdução](#-introdução)
2. [Annotations Disponíveis](#-annotations-disponíveis)
3. [@DisplayLabel](#-displaylabel)
4. [@Visible](#-visible)
5. [@Computed](#-computed)
6. [Combinando Annotations](#-combinando-annotations)
7. [Casos de Uso Comuns](#-casos-de-uso-comuns)
8. [Ordem de Precedência](#-ordem-de-precedência)
9. [Troubleshooting](#-troubleshooting)

---

## 🎯 Introdução

O sistema de **metadata** do MVT Events gera automaticamente toda a configuração do frontend (tabelas, formulários, filtros) a partir das entidades JPA. As **annotations customizadas** permitem que você controle como cada campo se comporta nesse sistema.

### Por que usar annotations?

✅ **Sem duplicação de código** - Configure uma vez na entidade  
✅ **Type-safe** - Erros detectados em tempo de compilação  
✅ **Autodocumentado** - Fica claro o propósito de cada campo  
✅ **Frontend automático** - O React lê o metadata e renderiza tudo

---

## 📋 Annotations Disponíveis

| Annotation      | Propósito                                             | Package                       |
| --------------- | ----------------------------------------------------- | ----------------------------- |
| `@DisplayLabel` | Marca o campo principal para exibição em dropdowns    | `com.mvt.mvt_events.metadata` |
| `@Visible`      | Controla visibilidade em tabela/form/filtros          | `com.mvt.mvt_events.metadata` |
| `@Computed`     | Marca campos calculados automaticamente pelo frontend | `com.mvt.mvt_events.metadata` |

---

## 🏷️ @DisplayLabel

### O que faz?

Marca o campo que deve ser usado como **label principal** quando a entidade é exibida em:

- Dropdowns de filtros
- Listas de seleção
- Referências de relacionamentos

### Sintaxe

```java
@DisplayLabel
private String fieldName;
```

### Quando usar?

✅ Em campos que melhor identificam a entidade  
✅ Geralmente em campos `name`, `title`, ou similares  
✅ **Uma única vez por entidade**

### Exemplos Práticos

#### ✅ Exemplo 1: Event (entidade simples)

```java
@Entity
@Table(name = "events")
public class Event extends BaseEntity {

    @DisplayLabel  // ← "Marathon 2025" aparecerá nos dropdowns
    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true, length = 100)
    private String slug;

    private String description;
}
```

**Resultado no Frontend:**

```json
{
  "labelField": "name",
  "formFields": [
    {
      "name": "name",
      "label": "Name",
      "visible": true // ← SEMPRE visível se tem @DisplayLabel
    }
  ]
}
```

#### ✅ Exemplo 2: EventCategory (com campo calculado)

```java
@Entity
@Table(name = "event_categories")
public class EventCategory extends BaseEntity {

    private BigDecimal distance;
    private Gender gender;
    private Integer minAge;
    private Integer maxAge;

    @DisplayLabel  // ← "5KM - Masculino - 30 a 39 anos"
    @Computed(function = "categoryName",
              dependencies = {"distance", "gender", "minAge", "maxAge"})
    private String name;
}
```

### ⚠️ Regras Importantes

1. **Apenas um campo por entidade** deve ter `@DisplayLabel`
2. **Sempre será visível** no formulário (sobrescreve `@Visible(form = false)`)
3. **Deve ser do tipo String** (ou conversível para String)

---

## 👁️ @Visible

### O que faz?

Controla a **visibilidade** de um campo em três contextos diferentes:

- **Tabela** (`table`) - Lista de registros
- **Formulário** (`form`) - Criação/edição
- **Filtros** (`filter`) - Barra lateral de busca

### Sintaxe

```java
// Valores padrão (tudo visível)
@Visible(table = true, form = true, filter = true)

// Exemplos de uso
@Visible(table = false)                    // Oculta apenas da tabela
@Visible(form = false)                     // Oculta apenas do form
@Visible(filter = false)                   // Remove o filtro
@Visible(table = false, filter = false)    // Oculta de 2 contextos
```

### Quando usar?

| Cenário                    | Annotation Recomendada                    |
| -------------------------- | ----------------------------------------- |
| Campo grande (TEXT)        | `@Visible(table = false)`                 |
| Campo calculado no backend | `@Visible(form = false)`                  |
| Campo interno (slug, hash) | `@Visible(filter = false, table = false)` |
| Relacionamento parent      | `@Visible(form = false)`                  |

### Exemplos Práticos

#### ✅ Exemplo 1: Campo TEXT grande

```java
@Entity
public class Event extends BaseEntity {

    @DisplayLabel
    private String name;

    @Visible(table = false, filter = false)  // ← Só aparece no form
    @Size(max = 500)
    @Column(columnDefinition = "TEXT")
    private String description;
}
```

**Por quê?**

- ❌ **Tabela**: Ocuparia muito espaço
- ✅ **Form**: Necessário para edição
- ❌ **Filtros**: Não faz sentido filtrar por texto longo

#### ✅ Exemplo 2: Campo calculado (currentParticipants)

```java
@Entity
public class EventCategory extends BaseEntity {

    @Column(name = "max_participants")
    private Integer maxParticipants;

    @Visible(form = false)  // ← Apenas leitura, calculado pelo backend
    @Column(name = "current_participants")
    private Integer currentParticipants = 0;
}
```

**Por quê?**

- ✅ **Tabela**: Mostrar quantos estão inscritos
- ❌ **Form**: Valor gerenciado pelo backend
- ✅ **Filtros**: Útil filtrar por ocupação

#### ✅ Exemplo 3: Relacionamento parent (evitar loop infinito)

```java
@Entity
public class EventCategory extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    @Visible(form = false)  // ← Não editar no form de categoria
    private Event event;

    private String name;
    private BigDecimal price;
}
```

**Por quê?**

- ✅ **Tabela**: Ver a qual evento pertence
- ❌ **Form**: Categoria é criada dentro do Event
- ✅ **Filtros**: Filtrar categorias por evento

#### ✅ Exemplo 4: Campos internos (slug, registrationOpen)

```java
@Entity
public class Event extends BaseEntity {

    @DisplayLabel
    private String name;

    @Visible(filter = false, table = false, form = false)  // ← Totalmente oculto
    @Column(nullable = false, unique = true, length = 100)
    private String slug;

    @Visible(table = false, form = false, filter = false)  // ← Gerenciado por lógica
    @Column(name = "registration_open")
    private Boolean registrationOpen = true;
}
```

### 📊 Matriz de Visibilidade

| Annotation                                              | Tabela | Form | Filtros |
| ------------------------------------------------------- | ------ | ---- | ------- |
| _(sem annotation)_                                      | ✅     | ✅   | ✅      |
| `@Visible(table = false)`                               | ❌     | ✅   | ✅      |
| `@Visible(form = false)`                                | ✅     | ❌   | ✅      |
| `@Visible(filter = false)`                              | ✅     | ✅   | ❌      |
| `@Visible(table = false, form = false)`                 | ❌     | ❌   | ✅      |
| `@Visible(table = false, filter = false)`               | ❌     | ✅   | ❌      |
| `@Visible(form = false, filter = false)`                | ✅     | ❌   | ❌      |
| `@Visible(table = false, form = false, filter = false)` | ❌     | ❌   | ❌      |

---

## 🧮 @Computed

### O que faz?

Marca campos que são **calculados automaticamente pelo frontend** com base em outros campos.

### Características

- ✅ **Readonly no frontend** - Usuário não pode editar
- ✅ **Recalculado em tempo real** - Quando campos dependentes mudam
- ✅ **Backend aceita o valor** - Mas não precisa recalcular
- ✅ **Combinável com @DisplayLabel** - Ideal para labels compostas

### Sintaxe

```java
@Computed(
    function = "nomeDaFuncao",
    dependencies = {"campo1", "campo2", "campo3"}
)
private String fieldName;
```

### Parâmetros

| Parâmetro      | Tipo     | Obrigatório | Descrição                     |
| -------------- | -------- | ----------- | ----------------------------- |
| `function`     | String   | ✅          | Nome da função JS no frontend |
| `dependencies` | String[] | ✅          | Campos que disparam recálculo |

### Funções Disponíveis

| Função         | Descrição                        | Dependências Esperadas                                   |
| -------------- | -------------------------------- | -------------------------------------------------------- |
| `categoryName` | Gera nome de categoria esportiva | `distance`, `gender`, `minAge`, `maxAge`, `distanceUnit` |

> 💡 **Nota**: Novas funções podem ser adicionadas no frontend (`src/utils/computedFields.js`)

### Exemplo Completo

```java
@Entity
@Table(name = "event_categories")
public class EventCategory extends BaseEntity {

    // Campos base
    @Column(precision = 10, scale = 2)
    private BigDecimal distance;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Gender gender;

    @Column(name = "min_age")
    private Integer minAge;

    @Column(name = "max_age")
    private Integer maxAge;

    @Enumerated(EnumType.STRING)
    @Column(name = "distance_unit", length = 10)
    private DistanceUnit distanceUnit = DistanceUnit.KM;

    // Campo calculado
    @DisplayLabel  // ← Usado em dropdowns
    @Computed(
        function = "categoryName",
        dependencies = {"distance", "gender", "minAge", "maxAge", "distanceUnit"}
    )
    @Column(nullable = false, length = 100)
    private String name;
}
```

### Comportamento no Frontend

#### Quando o usuário digita:

- **distance**: `5`
- **gender**: `MALE`
- **minAge**: `30`
- **maxAge**: `39`
- **distanceUnit**: `KM`

#### O campo `name` é automaticamente preenchido:

```
"5KM - Masculino - 30 a 39 anos"
```

### Resultado no Metadata

```json
{
  "labelField": "name",
  "formFields": [
    {
      "name": "name",
      "label": "Name",
      "type": "STRING",
      "computed": {
        "function": "categoryName",
        "dependencies": [
          "distance",
          "gender",
          "minAge",
          "maxAge",
          "distanceUnit"
        ]
      },
      "readonly": true, // ← Automaticamente readonly
      "visible": true
    }
  ]
}
```

### ⚠️ Regras Importantes

1. **Dependências devem existir** - Nomes de campos devem estar corretos
2. **Função deve estar implementada** - No frontend JS
3. **Sempre readonly** - Usuário não pode editar manualmente
4. **Backend recebe o valor** - Mas não valida/recalcula

---

## 🔗 Combinando Annotations

Você pode (e deve!) combinar annotations para obter o comportamento desejado.

### Padrões Comuns

#### 1️⃣ Label Calculada + Visível

```java
@DisplayLabel
@Computed(function = "categoryName", dependencies = {"distance", "gender"})
@Column(nullable = false)
private String name;
```

**Uso**: Campos que servem como identificador mas são gerados automaticamente

---

#### 2️⃣ Campo Interno + Invisível em Tudo

```java
@Visible(table = false, form = false, filter = false)
@Column(unique = true)
private String slug;
```

**Uso**: Campos gerenciados pelo backend, não expostos ao usuário

---

#### 3️⃣ Relacionamento Parent + Invisível no Form

```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "event_id", nullable = false)
@Visible(form = false)
private Event event;
```

**Uso**: Relacionamentos 1:N onde o filho é criado dentro do pai

---

#### 4️⃣ Campo Grande + Apenas no Form

```java
@Visible(table = false, filter = false)
@Size(max = 500)
@Column(columnDefinition = "TEXT")
private String description;
```

**Uso**: Campos TEXT longos que só fazem sentido no formulário

---

#### 5️⃣ Campo Calculado + Apenas Leitura

```java
@Visible(form = false)
@Column(name = "current_participants")
private Integer currentParticipants = 0;
```

**Uso**: Contadores e agregações gerenciadas pelo backend

---

## 💡 Casos de Uso Comuns

### Caso 1: Nova Entidade Simples

```java
@Entity
@Table(name = "organizations")
public class Organization extends BaseEntity {

    @DisplayLabel  // ← Campo principal
    @Column(nullable = false)
    private String name;

    @Visible(table = false, filter = false)  // ← Só no form
    @Column(columnDefinition = "TEXT")
    private String description;

    @Visible(form = false, filter = false)  // ← Só na tabela
    @Column(unique = true)
    private String slug;
}
```

---

### Caso 2: Relacionamento 1:N (Parent)

```java
@Entity
@Table(name = "events")
public class Event extends BaseEntity {

    @DisplayLabel
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    @Visible(form = false)  // ← Criado via /organizations/{id}/events
    private Organization organization;

    @Visible(form = true)  // ← Cascade update habilitado
    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL)
    private List<EventCategory> categories = new ArrayList<>();
}
```

---

### Caso 3: Relacionamento 1:N (Child)

```java
@Entity
@Table(name = "event_categories")
public class EventCategory extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    @Visible(form = false)  // ← Event é o parent
    private Event event;

    @DisplayLabel
    @Computed(function = "categoryName", dependencies = {"distance", "gender"})
    private String name;

    @Visible(form = false)  // ← Gerenciado pelo backend
    private Integer currentParticipants = 0;
}
```

---

### Caso 4: Enum com Tradução

```java
@Entity
public class Event extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    private EventType eventType = EventType.RUNNING;

    public enum EventType {
        RUNNING("Running"),
        CYCLING("Cycling"),
        TRIATHLON("Triathlon");

        private final String displayName;

        EventType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }
}
```

> 💡 **Nota**: Enums com `getDisplayName()` são automaticamente traduzidos no metadata

---

## 🔢 Ordem de Precedência

Quando múltiplas regras se aplicam ao mesmo campo, a ordem de prioridade é:

### 1. `@DisplayLabel` (maior prioridade)

- ✅ **Sempre força `visible = true`** no `formFields`
- ✅ **Define `labelField`** no metadata da entidade

### 2. `@Visible`

- ✅ **Controla visibilidade** em cada contexto (table/form/filter)
- ⚠️ **Exceto se houver `@DisplayLabel`** - aí `form` sempre é `true`

### 3. `@Computed`

- ✅ **Define `readonly = true`**
- ✅ **Adiciona `computed` metadata** (function + dependencies)

### 4. JPA Annotations

- ✅ **`@Column(nullable = false)`** → `required = true`
- ✅ **`@Enumerated`** → `type = "ENUM"` + `options`
- ✅ **`@ManyToOne`** → `type = "RELATIONSHIP"` + `relationshipMetadata`

### Exemplo com Todas as Camadas

```java
@DisplayLabel                           // ← 1. Prioridade máxima
@Computed(function = "categoryName",    // ← 2. Readonly + computed
          dependencies = {"distance"})
@Visible(form = false)                  // ← 3. IGNORADO (DisplayLabel vence)
@Column(nullable = false)               // ← 4. required = true
private String name;
```

**Resultado:**

```json
{
  "name": "name",
  "label": "Name",
  "type": "STRING",
  "required": true, // ← Do @Column
  "visible": true, // ← Do @DisplayLabel (ignora @Visible)
  "readonly": true, // ← Do @Computed
  "computed": {
    // ← Do @Computed
    "function": "categoryName",
    "dependencies": ["distance"]
  }
}
```

---

## 🐛 Troubleshooting

### Problema 1: Campo com `@DisplayLabel` não aparece visível no form

**Causa**: Bug já corrigido na v1.1.0

**Solução**: Certifique-se de estar usando a versão mais recente do `MetadataService.java`

```java
// ✅ Código correto (v1.1.0+)
if (isDisplayLabel) {
    copy.setVisible(true);  // Sempre visível
}
```

---

### Problema 2: Campo `@Computed` não recalcula no frontend

**Checklist**:

1. ✅ Função existe em `src/utils/computedFields.js`?
2. ✅ Nomes das dependências estão corretos?
3. ✅ Campos dependentes estão no mesmo formulário?
4. ✅ Frontend está lendo `computed` do metadata?

**Exemplo de debug no frontend:**

```javascript
console.log("Metadata recebido:", metadata.formFields);
// Procure por: computed: { function: "...", dependencies: [...] }
```

---

### Problema 3: `@Visible(form = false)` não funciona com `@DisplayLabel`

**Causa**: Comportamento esperado! `@DisplayLabel` tem prioridade.

**Solução**: Remova `@DisplayLabel` se não quiser o campo visível no form.

---

### Problema 4: Relacionamento causa loop infinito no JSON

**Causa**: Serialização bidirecional sem `@JsonIgnore`

**Solução**: Adicione `@JsonIgnore` no lado "child":

```java
// ✅ No EventCategory (child)
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "event_id", nullable = false)
@JsonIgnore  // ← Evita loop
@Visible(form = false)
private Event event;
```

---

## 📚 Referências Relacionadas

- [Arquitetura do Metadata](../architecture/METADATA_ARCHITECTURE.md)
- [Cascade Update Helper](CASCADE_HELPER_README.md)
- [API de Filtros](../api/FILTERS_GUIDE.md)
- [Documentação de API](../API_DOCUMENTATION.md)

---

## 🎓 Exercícios Práticos

### Exercício 1: Criar entidade `Product`

Crie uma entidade `Product` com:

- ✅ Campo `name` como `@DisplayLabel`
- ✅ Campo `description` visível apenas no form
- ✅ Campo `sku` invisível em filtros
- ✅ Campo `price` visível em tudo

<details>
<summary>💡 Ver Solução</summary>

```java
@Entity
@Table(name = "products")
public class Product extends BaseEntity {

    @DisplayLabel
    @Column(nullable = false)
    private String name;

    @Visible(table = false, filter = false)
    @Column(columnDefinition = "TEXT")
    private String description;

    @Visible(filter = false)
    @Column(unique = true, length = 50)
    private String sku;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;
}
```

</details>

---

### Exercício 2: Relacionamento `Order → OrderItem`

Crie um relacionamento 1:N onde:

- ✅ `Order` tem lista de `OrderItem`
- ✅ `OrderItem` não mostra `order` no form
- ✅ `OrderItem` tem campo calculado `totalPrice` (quantidade × preço)

<details>
<summary>💡 Ver Solução</summary>

```java
// Order.java
@Entity
@Table(name = "orders")
public class Order extends BaseEntity {

    @DisplayLabel
    @Column(name = "order_number", unique = true)
    private String orderNumber;

    @Visible(form = true)
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    private List<OrderItem> items = new ArrayList<>();
}

// OrderItem.java
@Entity
@Table(name = "order_items")
public class OrderItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    @JsonIgnore
    @Visible(form = false)
    private Order order;

    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private Integer quantity;

    @Visible(form = false)
    @Computed(function = "orderItemTotal",
              dependencies = {"quantity", "product.price"})
    private BigDecimal totalPrice;
}
```

</details>

---

### Exercício 3: Entidade Event Real do Projeto

Analise a entidade `Event` real do MVT Events e identifique:

- ✅ Qual campo é o `@DisplayLabel`?
- ✅ Quais campos deveriam ser `@Visible(table = false, filter = false)`?
- ✅ Qual campo é calculado e deveria ser `@Visible(form = false)`?

<details>
<summary>💡 Ver Análise</summary>

```java
@Entity
@Table(name = "events")
public class Event extends BaseEntity {

    // Parent relationship - Não editar no form do Event
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    @Visible(form = false)  // ✅ Event é criado via Organization
    private Organization organization;

    // Campo principal - Usado em dropdowns
    @DisplayLabel  // ✅ Identificador principal
    @Column(nullable = false)
    private String name;

    // Campo interno gerado - Ocultar de tudo
    @Visible(filter = false, table = false, form = false)  // ✅ Gerado automaticamente
    @Column(nullable = false, unique = true, length = 100)
    private String slug;

    // Campo TEXT grande - Apenas no form
    @Visible(table = false, filter = false)  // ✅ Muito grande para tabela
    @Size(max = 500)
    @Column(columnDefinition = "TEXT")
    private String description;

    // Enum com tradução
    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 50)
    private EventType eventType = EventType.RUNNING;  // ✅ getDisplayName() traduz

    // Campo de data
    @Column(name = "event_date", nullable = false)
    private LocalDateTime eventDate;

    // Relacionamento
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "city_id", nullable = false)
    private City city;

    // Campos do formulário
    @Visible(filter = false, table = false, form = true)
    @Column(length = 150, nullable = false)
    private String location;

    @Visible(filter = false, table = false, form = true)
    @Column(name = "max_participants")
    @Max(1000)
    private Integer maxParticipants;

    // Campo calculado por lógica - Ocultar
    @Visible(table = false, form = false, filter = false)  // ✅ Gerenciado por isRegistrationOpen()
    @Column(name = "registration_open")
    private Boolean registrationOpen = true;

    // Datas de inscrição
    @Visible(filter = false, table = false, form = true)
    @Column(name = "registration_start_date", nullable = false)
    private LocalDate registrationStartDate;

    @Visible(filter = false, table = false, form = true)
    @Column(name = "registration_end_date", nullable = false)
    private LocalDate registrationEndDate;

    // Preço
    @Visible(filter = false, table = false, form = true)
    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal price;

    @Visible(filter = false, table = false, form = true)
    @Column(length = 3)
    private String currency = "BRL";

    // Termos - Campo TEXT grande
    @Visible(table = false, form = false, filter = false)  // ✅ Muito grande
    @Column(name = "terms_and_conditions", columnDefinition = "TEXT")
    private String termsAndConditions;

    // Status
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private EventStatus status = EventStatus.DRAFT;  // ✅ getDisplayName() traduz

    // Configurações financeiras - Ocultas
    @Visible(table = false, form = false, filter = false)
    @Column(name = "platform_fee_percentage", precision = 5, scale = 4)
    private BigDecimal platformFeePercentage;

    @Visible(filter = false, table = false)
    @Enumerated(EnumType.STRING)
    @Column(name = "transfer_frequency", length = 20)
    private TransferFrequency transferFrequency = TransferFrequency.WEEKLY;

    // Relacionamento 1:N com cascade update
    @Visible(form = true)  // ✅ Habilita cascade update
    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL)
    private List<EventCategory> categories = new ArrayList<>();
}
```

**Pontos-chave aprendidos:**

1. **`@DisplayLabel`** sempre no campo principal (`name`)
2. **`@Visible(table = false, filter = false)`** para campos TEXT grandes
3. **`@Visible(form = false)`** para relacionamentos parent
4. **`@Visible(table = false, form = false, filter = false)`** para campos internos
5. **`@Visible(form = true)`** em `@OneToMany` habilita cascade update
6. **Enums** devem ter `getDisplayName()` para tradução automática

</details>

---

## ✅ Checklist de Boas Práticas

Ao criar uma nova entidade:

- [ ] Defini um campo `@DisplayLabel`?
- [ ] Campos TEXT grandes têm `@Visible(table = false)`?
- [ ] Relacionamentos parent têm `@Visible(form = false)`?
- [ ] Campos calculados têm `@Visible(form = false)` ou `@Computed`?
- [ ] Relacionamentos bidirecionais têm `@JsonIgnore` no child?
- [ ] Enums têm método `getDisplayName()`?
- [ ] Campos internos (slug, hash) estão ocultos?

---

## 🎯 Resumo Final

| Annotation                 | Quando Usar                 | Exemplo                        |
| -------------------------- | --------------------------- | ------------------------------ |
| `@DisplayLabel`            | Campo principal da entidade | `name`, `title`                |
| `@Visible(table = false)`  | Campos grandes              | `description` (TEXT)           |
| `@Visible(form = false)`   | Campos calculados/parent    | `currentParticipants`, `event` |
| `@Visible(filter = false)` | Campos sem filtro           | `slug`, `hash`                 |
| `@Computed`                | Cálculos do frontend        | `categoryName`, `totalPrice`   |

---

**📅 Última atualização:** 14 de outubro de 2025  
**📌 Versão:** 1.1.0  
**✍️ Autor:** MVT Events Team

---

## 🤝 Contribuindo

Encontrou um problema ou tem uma sugestão?

- Abra uma issue no repositório
- Proponha uma melhoria na documentação
- Compartilhe novos casos de uso

---

**Próximos Passos:**

1. [Implementar Cascade Update](CASCADE_HELPER_README.md)
2. [Configurar Filtros Avançados](../api/FILTERS_GUIDE.md)
3. [Entender a Arquitetura](../architecture/METADATA_ARCHITECTURE.md)
