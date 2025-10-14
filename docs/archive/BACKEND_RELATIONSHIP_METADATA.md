# 🔗 Backend Relationship Metadata Implementation

## 📋 Resumo

Implementação completa do suporte a **metadata de relacionamentos** no backend, permitindo que o frontend renderize **formulários aninhados dinâmicos** (como categorias de eventos) com base em informações vindas da API.

---

## 🎯 Objetivo

Permitir que o frontend:

1. **Receba metadata** sobre relacionamentos ONE_TO_MANY do backend
2. **Renderize automaticamente** formulários de arrays/listas aninhadas
3. **Saiba quais campos** cada item do array deve ter
4. **Valide** os dados conforme especificações do backend

---

## 🏗️ Arquitetura Implementada

### 1. RelationshipMetadata (Nova Classe)

**Localização:** `src/main/java/com/mvt/mvt_events/metadata/RelationshipMetadata.java`

```java
public class RelationshipMetadata {
    private String type;              // "ONE_TO_ONE", "ONE_TO_MANY", "MANY_TO_ONE", "MANY_TO_MANY"
    private String targetEntity;      // Nome da entidade relacionada
    private String targetEndpoint;    // Endpoint para buscar/salvar
    private Boolean cascade;          // Salva em cascata?
    private Boolean orphanRemoval;    // Remove órfãos?
    private List<FieldMetadata> fields; // Campos da entidade relacionada
}
```

**Propósito:**

- Descreve completamente um relacionamento entre entidades
- Fornece os campos necessários para renderizar formulários aninhados
- Define comportamentos de persistência (cascade, orphanRemoval)

---

### 2. FieldMetadata (Estendido)

**Localização:** `src/main/java/com/mvt/mvt_events/metadata/FieldMetadata.java`

**Campos adicionados:**

```java
// Propriedades de formulário
private Boolean required;           // Campo obrigatório?
private String placeholder;         // Placeholder do input
private Integer minLength;          // Tamanho mínimo (string)
private Integer maxLength;          // Tamanho máximo (string)
private Double min;                 // Valor mínimo (number)
private Double max;                 // Valor máximo (number)
private String pattern;             // Regex de validação

// Metadata de relacionamento
private RelationshipMetadata relationship; // Para type="nested"
```

**Novo tipo suportado:**

- `type = "nested"` → Indica um campo de relacionamento aninhado

---

### 3. EventCategoryMetadataProvider

**Localização:** `src/main/java/com/mvt/mvt_events/metadata/providers/EventCategoryMetadataProvider.java`

**Propósito:**

- Provider centralizado para metadata de Event Category
- Define todos os campos de uma categoria de evento
- Usado tanto standalone quanto em relacionamentos

**Método principal:**

```java
public static RelationshipMetadata createCategoriesRelationship() {
    RelationshipMetadata relationship = new RelationshipMetadata(
        "ONE_TO_MANY",
        "eventCategory",
        "/api/event-categories"
    );

    relationship.setCascade(true);
    relationship.setOrphanRemoval(true);
    relationship.setFields(getCategoryFields());

    return relationship;
}
```

**Campos definidos:**

- Nome da categoria (string, required)
- Gênero (select, optional)
- Idade mínima/máxima (number, optional, 0-120)
- Distância (number, required, 0.1-500)
- Unidade de distância (select, required)
- Preço (currency, required)
- Máximo de participantes (number, optional)
- Descrição (textarea, optional, max 500 chars)

---

### 4. MetadataService (Atualizado)

**Localização:** `src/main/java/com/mvt/mvt_events/metadata/MetadataService.java`

**Modificação no método `getEventMetadata()`:**

```java
// Campo de relacionamento ONE_TO_MANY para categorias
FieldMetadata categoriesField = new FieldMetadata("categories", "Categorias do Evento", "nested");
categoriesField.setVisible(false); // Não exibir na tabela
categoriesField.setSortable(false);
categoriesField.setSearchable(false);
categoriesField.setRelationship(EventCategoryMetadataProvider.createCategoriesRelationship());
fields.add(categoriesField);
```

---

## 📤 JSON Retornado pela API

### Endpoint: `GET /api/metadata/event`

```json
{
  "name": "event",
  "label": "Eventos",
  "endpoint": "/api/events",
  "fields": [
    {
      "name": "name",
      "label": "Nome do Evento",
      "type": "string",
      "sortable": true,
      "searchable": true,
      "visible": true
    },
    ...
    {
      "name": "categories",
      "label": "Categorias do Evento",
      "type": "nested",
      "sortable": false,
      "searchable": false,
      "visible": false,
      "relationship": {
        "type": "ONE_TO_MANY",
        "targetEntity": "eventCategory",
        "targetEndpoint": "/api/event-categories",
        "cascade": true,
        "orphanRemoval": true,
        "fields": [
          {
            "name": "name",
            "label": "Nome da Categoria",
            "type": "string",
            "required": true,
            "placeholder": "Ex: 5KM - Masculino - 20-30 anos",
            "maxLength": 100
          },
          {
            "name": "distance",
            "label": "Distância",
            "type": "number",
            "required": true,
            "min": 0.1,
            "max": 500.0,
            "placeholder": "Ex: 5.0"
          },
          {
            "name": "price",
            "label": "Preço (R$)",
            "type": "currency",
            "required": true,
            "min": 0.0,
            "placeholder": "Ex: 50.00"
          },
          ...
        ]
      }
    }
  ],
  "filters": [...],
  "pagination": {...}
}
```

---

## 🎨 Como o Frontend Usa

### 1. Hook de Formulário (useFormMetadata.ts)

```typescript
const { formMetadata, isLoading, error } = useFormMetadata("event");
```

### 2. Conversão Automática (convertEntityMetadataToFormMetadata)

```typescript
// Detecta type: 'nested' + relationship.type === 'ONE_TO_MANY'
// Converte para campo array com fields do backend

{
  name: 'categories',
  label: 'Categorias do Evento',
  type: 'array',
  fields: [
    { name: 'name', type: 'string', required: true, ... },
    { name: 'distance', type: 'number', required: true, min: 0.1, max: 500, ... },
    { name: 'price', type: 'currency', required: true, min: 0, ... },
    ...
  ]
}
```

### 3. Renderização (EntityForm)

```typescript
<EntityForm metadata={formMetadata} onSubmit={handleSubmit} />
```

Automaticamente renderiza:

- ✅ Campos simples (string, number, date, select, etc.)
- ✅ **Arrays dinâmicos** com botão "Adicionar Categoria"
- ✅ Formulário expansível para cada categoria
- ✅ Validações conforme metadata (required, min, max, etc.)

---

## 🚀 Benefícios

### Para o Backend

- ✅ **Controle centralizado** das regras de negócio
- ✅ **Single Source of Truth** para estrutura de dados
- ✅ **Validações consistentes** entre backend e frontend
- ✅ **Facilita manutenção** - mudanças em um só lugar

### Para o Frontend

- ✅ **Formulários 100% dinâmicos** - sem hardcode
- ✅ **Validações automáticas** baseadas no backend
- ✅ **Menos código** - sem definir schemas manualmente
- ✅ **Type-safe** com TypeScript
- ✅ **Escalável** - novos campos são automaticamente renderizados

---

## 📝 Próximos Passos (Opcional)

### 1. Enum Options

Adicionar suporte para options de campos enum:

```java
FieldMetadata genderField = new FieldMetadata("gender", "Gênero", "select");
genderField.setOptions(Arrays.asList(
    new FieldOption("Masculino", "MALE"),
    new FieldOption("Feminino", "FEMALE"),
    new FieldOption("Misto", "MIXED")
));
```

### 2. Validações Customizadas

```java
FieldMetadata emailField = new FieldMetadata("email", "E-mail", "string");
emailField.setPattern("^[A-Za-z0-9+_.-]+@(.+)$");
emailField.setValidationMessage("E-mail inválido");
```

### 3. Conditional Fields

```java
FieldMetadata maxParticipantsField = new FieldMetadata(...);
maxParticipantsField.setVisibleWhen("hasLimitedSeats", true);
```

### 4. Helpers para Outros Relacionamentos

Similar ao `EventCategoryMetadataProvider`, criar:

- `RegistrationMetadataProvider`
- `PaymentMetadataProvider`
- `TransferMetadataProvider`

---

## 🧪 Testando

### 1. Verificar Metadata

```bash
curl http://localhost:8080/api/metadata/event | jq '.fields[] | select(.name == "categories")'
```

### 2. Resposta Esperada

```json
{
  "name": "categories",
  "label": "Categorias do Evento",
  "type": "nested",
  "sortable": false,
  "searchable": false,
  "visible": false,
  "relationship": {
    "type": "ONE_TO_MANY",
    "targetEntity": "eventCategory",
    "targetEndpoint": "/api/event-categories",
    "cascade": true,
    "orphanRemoval": true,
    "fields": [...]
  }
}
```

---

## 📚 Arquivos Criados/Modificados

### Novos Arquivos

1. `src/main/java/com/mvt/mvt_events/metadata/RelationshipMetadata.java`
2. `src/main/java/com/mvt/mvt_events/metadata/providers/EventCategoryMetadataProvider.java`
3. `docs/BACKEND_RELATIONSHIP_METADATA.md` (este arquivo)

### Arquivos Modificados

1. `src/main/java/com/mvt/mvt_events/metadata/FieldMetadata.java`

   - Adicionados campos de validação (required, min, max, etc.)
   - Adicionado campo `relationship`

2. `src/main/java/com/mvt/mvt_events/metadata/MetadataService.java`
   - Importado `EventCategoryMetadataProvider`
   - Adicionado campo `categories` com relacionamento ao metadata de Event

---

## ✅ Status

**✅ Implementado e Funcionando**

- [x] Classe `RelationshipMetadata`
- [x] Extensão de `FieldMetadata`
- [x] Provider `EventCategoryMetadataProvider`
- [x] Integração no `MetadataService`
- [x] Compilação sem erros
- [x] Servidor iniciado com sucesso
- [x] Metadata sendo retornado via API

**🎉 Pronto para uso no Frontend!**

O frontend pode agora consumir `GET /api/metadata/event` e renderizar automaticamente formulários com categorias aninhadas.
