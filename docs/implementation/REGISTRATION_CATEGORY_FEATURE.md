# 🏷️ Feature: Categoria na Inscrição

**Data:** 15 de outubro de 2025  
**Tipo:** Nova Feature  
**Impacto:** Schema + Backend

---

## 🎯 Objetivo

Permitir que usuários escolham em **qual categoria** se inscrevem quando fazem uma inscrição em um evento.

### Problema Anterior

Antes, a inscrição (`Registration`) estava vinculada apenas ao evento (`Event`), mas não ficava registrado em qual categoria específica o usuário se inscreveu (5KM, 10KM, Masculino, Feminino, etc.).

### Solução

Adicionar relacionamento `Registration → EventCategory` para registrar a categoria escolhida na inscrição.

---

## 📊 Mudanças no Schema

### Migration: V19\_\_add_category_to_registrations.sql

```sql
-- Adicionar coluna category_id
ALTER TABLE registrations
ADD COLUMN category_id BIGINT;

-- Adicionar foreign key para event_categories
ALTER TABLE registrations
ADD CONSTRAINT fk_registration_category
    FOREIGN KEY (category_id)
    REFERENCES event_categories(id)
    ON DELETE SET NULL;

-- Criar índice para performance
CREATE INDEX idx_registration_category_id ON registrations(category_id);
```

**Características:**

- ✅ `category_id` é **opcional** (`NULL` permitido)
- ✅ `ON DELETE SET NULL` - Se categoria for deletada, inscrição mantém-se válida
- ✅ Índice criado para queries mais rápidas

---

## 🔧 Mudanças no Código

### 1. Entidade `Registration.java`

```java
@Entity
@Table(name = "registrations")
public class Registration extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")  // ← NOVO
    private EventCategory category;

    // ...existing fields...
}
```

**Características:**

- ✅ `@ManyToOne` - Muitas inscrições → Uma categoria
- ✅ `FetchType.LAZY` - Não carrega categoria automaticamente
- ✅ **Não é obrigatório** - Pode ser `null`

---

### 2. DTO `MyRegistrationResponse.java`

Adicionada classe interna `CategorySummary`:

```java
@Data
public class MyRegistrationResponse {
    private Long id;
    private EventSummary event;
    private CategorySummary category;  // ← NOVO
    private UserSummary user;

    @Data
    public static class CategorySummary {
        private Long id;
        private String name;              // Ex: "5KM - Masculino - 30 a 39 anos"
        private BigDecimal distance;      // Ex: 5.00
        private String gender;            // Ex: "Masculino"
        private Integer minAge;           // Ex: 30
        private Integer maxAge;           // Ex: 39
        private BigDecimal price;         // Ex: 100.00
    }
}
```

**Retorno da API:**

```json
{
  "id": 1,
  "registrationDate": "2025-10-15T10:30:00",
  "status": "ACTIVE",
  "event": {
    "id": 19,
    "name": "Maratona de São Paulo 2025",
    "eventDate": "2025-12-15T08:00:00"
  },
  "category": {
    "id": 22,
    "name": "5KM - Masculino - 30 a 39 anos",
    "distance": 5.0,
    "gender": "Masculino",
    "minAge": 30,
    "maxAge": 39,
    "price": 100.0
  },
  "user": {
    "id": "fdb72229-2c17-4a3d-8951-066f82305155",
    "name": "Maria Organizadora"
  }
}
```

---

### 3. DTO `RegistrationListDTO.java`

Adicionados campos de categoria:

```java
public class RegistrationListDTO {
    // ...existing fields...

    // Event
    private Long eventId;
    private String eventName;

    // Category
    private Long categoryId;      // ← NOVO
    private String categoryName;  // ← NOVO

    // Construtor atualizado com 2 novos parâmetros
    public RegistrationListDTO(Long id, LocalDateTime registrationDate,
            RegistrationStatus status, String notes,
            String userId, String userName, String userUsername,
            Long eventId, String eventName, LocalDateTime eventDate,
            Long categoryId, String categoryName) {  // ← NOVOS
        // ...
    }
}
```

---

### 4. Mapper `RegistrationMapperService.java`

```java
@Service
public class RegistrationMapperService {

    public MyRegistrationResponse toMyRegistrationResponse(Registration registration) {
        MyRegistrationResponse response = new MyRegistrationResponse();
        // ...existing code...

        // Category summary
        if (registration.getCategory() != null) {
            CategorySummary categorySummary = new CategorySummary();
            categorySummary.setId(registration.getCategory().getId());
            categorySummary.setName(registration.getCategory().getName());
            categorySummary.setDistance(registration.getCategory().getDistance());
            categorySummary.setGender(registration.getCategory().getGender() != null ?
                registration.getCategory().getGender().getDisplayName() : null);
            categorySummary.setMinAge(registration.getCategory().getMinAge());
            categorySummary.setMaxAge(registration.getCategory().getMaxAge());
            categorySummary.setPrice(registration.getCategory().getPrice());
            response.setCategory(categorySummary);
        }

        return response;
    }
}
```

---

### 5. Service `RegistrationService.java`

Todos os métodos que retornam `RegistrationListDTO` foram atualizados:

```java
// Exemplo: listWithFilters()
return registrations.map(r -> new RegistrationListDTO(
    r.getId(),
    r.getRegistrationDate(),
    r.getStatus(),
    r.getNotes(),
    r.getUser().getId().toString(),
    r.getUser().getName(),
    r.getUser().getUsername(),
    r.getEvent().getId(),
    r.getEvent().getName(),
    r.getEvent().getEventDate(),
    r.getCategory() != null ? r.getCategory().getId() : null,    // ← NOVO
    r.getCategory() != null ? r.getCategory().getName() : null   // ← NOVO
));
```

**Métodos atualizados:**

- ✅ `list(Pageable)`
- ✅ `listAll()`
- ✅ `listWithFilters()`
- ✅ `listByStatus(status, pageable)` (deprecated)
- ✅ `listByStatus(status)` (deprecated)

---

## 🔍 Especificações (Filtros)

A especificação `RegistrationSpecification` pode ser estendida para filtrar por categoria:

```java
// Exemplo de uso futuro
public static Specification<Registration> hasCategory(Long categoryId) {
    return (root, query, cb) -> {
        if (categoryId == null) {
            return cb.conjunction();
        }
        return cb.equal(root.get("category").get("id"), categoryId);
    };
}
```

**Uso:**

```bash
# Filtrar inscrições de uma categoria específica
GET /api/registrations?category=22
```

---

## 📝 Exemplos de Uso

### 1. Criar Inscrição com Categoria

```bash
POST /api/registrations
{
  "user": { "id": "fdb72229-2c17-4a3d-8951-066f82305155" },
  "event": { "id": 19 },
  "category": { "id": 22 },  // ← Categoria escolhida
  "status": "PENDING"
}
```

### 2. Listar Inscrições (com categoria)

```bash
GET /api/registrations?event=19
```

**Resposta:**

```json
{
  "content": [
    {
      "id": 1,
      "registrationDate": "2025-10-15T10:30:00",
      "status": "ACTIVE",
      "eventId": 19,
      "eventName": "Maratona de São Paulo 2025",
      "categoryId": 22,
      "categoryName": "5KM - Masculino - 30 a 39 anos",
      "userId": "fdb72229-...",
      "userName": "Maria Organizadora"
    }
  ]
}
```

### 3. Minhas Inscrições (com categoria)

```bash
GET /api/registrations/my-registrations
Authorization: Bearer {token}
```

**Resposta:**

```json
[
  {
    "id": 1,
    "event": {
      "id": 19,
      "name": "Maratona de São Paulo 2025"
    },
    "category": {
      "id": 22,
      "name": "5KM - Masculino - 30 a 39 anos",
      "distance": 5.0,
      "gender": "Masculino",
      "price": 100.0
    },
    "user": {
      "name": "Maria Organizadora"
    }
  }
]
```

---

## 🎯 Casos de Uso

### Cenário 1: Evento com Múltiplas Categorias

**Evento:** Corrida da Pampulha 2025

**Categorias:**

- 5KM - Masculino - 18 a 29 anos (R$ 80)
- 5KM - Feminino - 18 a 29 anos (R$ 80)
- 10KM - Masculino - 30 a 39 anos (R$ 100)
- 10KM - Feminino - 30 a 39 anos (R$ 100)

**Inscrição:**

```json
{
  "user": { "id": "..." },
  "event": { "id": 19 },
  "category": { "id": 22 } // 5KM - Masculino - 18 a 29 anos
}
```

---

### Cenário 2: Evento Sem Categorias (Categoria Opcional)

**Evento:** Workshop de Running

**Inscrição sem categoria:**

```json
{
  "user": { "id": "..." },
  "event": { "id": 20 },
  "category": null // Não tem categorias
}
```

✅ **Funciona normalmente** - `category_id` aceita `NULL`

---

## 🔄 Migração de Dados Existentes

Inscrições criadas **antes** desta migration terão `category_id = NULL`.

**Opções:**

### 1. Manter NULL (Recomendado)

- Inscrições antigas não têm categoria definida
- Compatibilidade total

### 2. Preencher com Primeira Categoria do Evento

```sql
-- Script opcional para popular category_id
UPDATE registrations r
SET category_id = (
    SELECT ec.id
    FROM event_categories ec
    WHERE ec.event_id = r.event_id
    LIMIT 1
)
WHERE r.category_id IS NULL;
```

⚠️ **Cuidado:** Só fazer se fizer sentido no negócio.

---

## ✅ Validações

### No Backend

**Validação futura (opcional):**

```java
@Service
public class RegistrationService {

    public Registration create(Registration registration) {
        // Validar se categoria pertence ao evento
        if (registration.getCategory() != null) {
            EventCategory category = registration.getCategory();
            Event event = registration.getEvent();

            if (!category.getEvent().getId().equals(event.getId())) {
                throw new IllegalArgumentException(
                    "Category does not belong to the selected event"
                );
            }
        }

        return registrationRepository.save(registration);
    }
}
```

### No Frontend

```typescript
// Validar categoria ao criar inscrição
if (selectedCategory && selectedCategory.eventId !== selectedEvent.id) {
  throw new Error("Categoria não pertence ao evento selecionado");
}
```

---

## 📊 Impacto na Performance

### Queries Antes

```sql
-- Buscar inscrições
SELECT * FROM registrations WHERE event_id = 19;

-- Queries N+1 para pegar categorias depois
SELECT * FROM event_categories WHERE id = 22;
SELECT * FROM event_categories WHERE id = 23;
-- ...
```

### Queries Depois (com JOIN)

```sql
-- Buscar inscrições com categoria em 1 query
SELECT r.*, ec.*
FROM registrations r
LEFT JOIN event_categories ec ON r.category_id = ec.id
WHERE r.event_id = 19;
```

✅ **Redução de queries** de N+1 para 1

---

## 🐛 Troubleshooting

### Problema 1: Category sempre retorna NULL

**Causa:** Inscrições antigas não têm `category_id` preenchido.

**Solução:** É esperado. Use validação no frontend:

```typescript
if (registration.category) {
  // Mostrar detalhes da categoria
} else {
  // Mostrar "Categoria não especificada"
}
```

---

### Problema 2: Lazy Loading Error ao acessar category

**Causa:** Tentar acessar `registration.getCategory()` fora de transação.

**Solução:** Usar `@Transactional` ou fazer join fetch:

```java
@Query("SELECT r FROM Registration r LEFT JOIN FETCH r.category WHERE r.id = :id")
Registration findByIdWithCategory(@Param("id") Long id);
```

---

## 📚 Referências

- [CASCADE_HELPER_README.md](CASCADE_HELPER_README.md) - Relacionamentos 1:N
- [ANNOTATIONS_GUIDE.md](ANNOTATIONS_GUIDE.md) - Annotations customizadas
- [METADATA_ARCHITECTURE.md](../architecture/METADATA_ARCHITECTURE.md) - Sistema de metadata

---

## ✅ Checklist

- [x] Migration criada (`V19__add_category_to_registrations.sql`)
- [x] Entidade `Registration` atualizada
- [x] DTO `MyRegistrationResponse` atualizado com `CategorySummary`
- [x] DTO `RegistrationListDTO` atualizado
- [x] Mapper `RegistrationMapperService` atualizado
- [x] Service `RegistrationService` atualizado (5 métodos)
- [x] Documentação criada
- [ ] Frontend atualizado (pendente)
- [ ] Testes criados (pendente)
- [ ] Validação de categoria → evento (opcional)

---

**📅 Data:** 15 de outubro de 2025  
**✍️ Autor:** MVT Events Team  
**📌 Versão:** 1.2.0  
**🎯 Status:** ✅ IMPLEMENTADO (Backend)

---

## 🚀 Próximos Passos

1. **Frontend:** Adicionar seletor de categoria no formulário de inscrição
2. **Validação:** Implementar validação de categoria → evento no backend
3. **Filtros:** Adicionar filtro por categoria em `RegistrationSpecification`
4. **Testes:** Criar testes unitários e de integração
5. **Relatórios:** Atualizar relatórios para mostrar categoria
