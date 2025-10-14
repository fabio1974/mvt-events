# Atualização em Cascata (1:N) - Eventos e Categorias

## Resumo

Implementada lógica de **atualização em cascata** no endpoint `PUT /api/events/{id}` para gerenciar relacionamentos 1:N (Event → Categories) em uma única transação.

## Comportamento

### Regras de Negócio

Quando um `Event` é atualizado via `PUT /api/events/{id}` com categories no payload:

1. **INSERT**: Categorias **sem `id`** → Novas categorias são **criadas**
2. **UPDATE**: Categorias **com `id`** → Categorias existentes são **atualizadas**
3. **DELETE**: Categorias que existem no banco mas **não estão no payload** → São **deletadas**

Tudo acontece em uma **única transação** (`@Transactional`).

## Exemplo de Uso

### Request

```bash
curl 'http://localhost:8080/api/events/19' \
  -X 'PUT' \
  -H 'Content-Type: application/json' \
  -H 'Authorization: Bearer {token}' \
  --data-raw '{
    "id": 19,
    "name": "Evento Teste",
    "eventType": "RUNNING",
    "status": "DRAFT",
    "organizationId": 6,
    "categories": [
      {
        "id": 22,
        "distance": 10,
        "gender": "MALE",
        "minAge": 10,
        "maxAge": 20,
        "distanceUnit": "KM",
        "name": "10KM - Masculino - 10 a 20",
        "price": 100,
        "maxParticipants": 100
      },
      {
        "distance": 21,
        "gender": "MALE",
        "minAge": 10,
        "maxAge": 20,
        "distanceUnit": "KM",
        "name": "21KM - Masculino - 10 a 20",
        "price": 100,
        "maxParticipants": 100
      }
    ]
  }'
```

### Cenários

#### ✅ Cenário 1: Adicionar Nova Categoria

**Situação:** Evento tem 1 categoria (ID=22), payload envia 2 categorias (ID=22 + nova sem ID)

**Resultado:**

- Categoria ID=22 → **ATUALIZADA**
- Categoria sem ID → **INSERIDA**
- Total final: 2 categorias

#### ✅ Cenário 2: Remover Categoria

**Situação:** Evento tem 2 categorias (ID=22, ID=23), payload envia apenas 1 (ID=22)

**Resultado:**

- Categoria ID=22 → **ATUALIZADA**
- Categoria ID=23 → **DELETADA** (não está no payload)
- Total final: 1 categoria

#### ✅ Cenário 3: Atualizar Categoria Existente

**Situação:** Evento tem 1 categoria (ID=22), payload envia mesma categoria com valores diferentes

**Resultado:**

- Categoria ID=22 → **ATUALIZADA** com novos valores
- Total final: 1 categoria

#### ✅ Cenário 4: Substituir Todas as Categorias

**Situação:** Evento tem 2 categorias (ID=22, ID=23), payload envia 2 novas (sem IDs)

**Resultado:**

- Categoria ID=22 → **DELETADA**
- Categoria ID=23 → **DELETADA**
- 2 novas categorias → **INSERIDAS**
- Total final: 2 categorias (novas)

## Implementação

### Arquivo: `EventService.java`

#### Método: `update(Long id, Event eventData)`

```java
@Transactional
public Event update(Long id, Event eventData) {
    Event existing = repository.findById(id)
            .orElseThrow(() -> new RuntimeException("Evento não encontrado"));

    // ... atualiza campos do evento ...

    Event savedEvent = repository.save(existing);

    // ==================== HANDLE CATEGORIES (CASCADE UPDATE) ====================
    if (eventData.getCategories() != null) {
        // 1. Buscar categorias existentes no banco
        Specification<EventCategory> spec = EventCategorySpecification.belongsToEvent(savedEvent.getId());
        List<EventCategory> existingCategories = categoryRepository.findAll(spec);

        // 2. Extrair IDs das categorias no payload
        Set<Long> payloadCategoryIds = eventData.getCategories().stream()
                .map(EventCategory::getId)
                .filter(catId -> catId != null)
                .collect(Collectors.toSet());

        // 3. DELETAR categorias que NÃO estão no payload
        List<EventCategory> categoriesToDelete = new ArrayList<>();
        for (EventCategory existingCat : existingCategories) {
            if (!payloadCategoryIds.contains(existingCat.getId())) {
                categoriesToDelete.add(existingCat);
            }
        }
        if (!categoriesToDelete.isEmpty()) {
            categoryRepository.deleteAll(categoriesToDelete);
            categoryRepository.flush();
        }

        // 4. PROCESSAR categorias do payload
        for (EventCategory payloadCat : eventData.getCategories()) {
            if (payloadCat.getId() != null) {
                // UPDATE: Categoria existente
                EventCategory existingCat = categoryRepository.findById(payloadCat.getId())
                        .orElseThrow(() -> new RuntimeException("Categoria não encontrada"));

                // Atualiza campos
                existingCat.setName(payloadCat.getName());
                existingCat.setDistance(payloadCat.getDistance());
                // ... outros campos ...

                categoryRepository.save(existingCat);
            } else {
                // INSERT: Nova categoria
                payloadCat.setEvent(savedEvent);
                payloadCat.setCurrentParticipants(0);
                categoryRepository.save(payloadCat);
            }
        }
    }

    // 5. Retornar evento atualizado com categories carregadas
    Event refreshedEvent = repository.findById(savedEvent.getId())
            .orElseThrow(() -> new RuntimeException("Evento não encontrado"));
    refreshedEvent.getCategories().size(); // Force lazy load

    return refreshedEvent;
}
```

## Logs de Debug

Durante a execução, o sistema imprime logs para rastreamento:

```
📦 UPDATE CASCADE - Categorias existentes: 2
📦 UPDATE CASCADE - IDs no payload: [22]
🗑️  DELETE - Categoria ID: 23 - 21KM - Masculino
✏️  UPDATE - Categoria ID: 22
```

## Transação

Todo o processo ocorre em **uma única transação** devido à anotação `@Transactional`:

- Se qualquer operação falhar, **todas são revertidas** (rollback)
- Garante **consistência** dos dados
- Evita **estados intermediários** inválidos

## Alternativa: Endpoint com DTO

Se preferir usar um DTO específico em vez da entidade completa:

```bash
PUT /api/events/{id}/with-categories
```

Este endpoint usa `EventUpdateRequest` DTO e tem comportamento similar.

## Vantagens

1. ✅ **Simplicidade**: Frontend envia estado completo, backend sincroniza
2. ✅ **Consistência**: Transação garante atomicidade
3. ✅ **Flexibilidade**: Suporta INSERT, UPDATE e DELETE em uma chamada
4. ✅ **Performance**: Batch operations, poucas queries
5. ✅ **Manutenibilidade**: Lógica centralizada no backend

## Desvantagens e Cuidados

⚠️ **Atenção**: Se não enviar `categories` no payload, as categorias existentes **não serão alteradas**. Para deletar todas, envie `categories: []`.

⚠️ **IDs Inválidos**: Se enviar ID de categoria que não existe, retorna erro 400.

⚠️ **Concorrência**: Se dois usuários editarem simultaneamente, o último "vence" (last-write-wins).

## Testes

### Teste 1: Adicionar Categoria

```bash
# Estado inicial: 1 categoria
curl -s http://localhost:8080/api/events/19 | jq '.categories | length'
# Output: 1

# Adicionar mais uma
curl -X PUT http://localhost:8080/api/events/19 \
  -H 'Content-Type: application/json' \
  -d '{
    "id": 19,
    "categories": [
      {"id": 22, "name": "Categoria Existente"},
      {"name": "Nova Categoria", "distance": 5, "gender": "MIXED", "distanceUnit": "KM", "price": 50}
    ]
  }'

# Verificar
curl -s http://localhost:8080/api/events/19 | jq '.categories | length'
# Output: 2
```

### Teste 2: Remover Categoria

```bash
# Remover categoria ID=22
curl -X PUT http://localhost:8080/api/events/19 \
  -H 'Content-Type: application/json' \
  -d '{
    "id": 19,
    "categories": [
      {"id": 23, "name": "Categoria que fica"}
    ]
  }'

# Verificar (categoria 22 foi deletada)
curl -s http://localhost:8080/api/events/19 | jq '.categories[].id'
# Output: 23
```

### Teste 3: Atualizar Categoria

```bash
# Atualizar preço da categoria
curl -X PUT http://localhost:8080/api/events/19 \
  -H 'Content-Type: application/json' \
  -d '{
    "id": 19,
    "categories": [
      {"id": 22, "name": "10KM", "price": 150}
    ]
  }'

# Verificar
curl -s http://localhost:8080/api/events/19 | jq '.categories[] | {id, name, price}'
# Output: {"id": 22, "name": "10KM", "price": 150}
```

## Padrão Aplicável a Outros Relacionamentos

Esta lógica foi **encapsulada em uma classe utilitária reutilizável**: `CascadeUpdateHelper`.

### ✨ Usando o Helper Genérico

Em vez de duplicar código, use o helper:

```java
@Service
@Transactional
public class MyEntityService {
    private final CascadeUpdateHelper cascadeUpdateHelper;

    public MyEntity update(Long id, MyEntity entityData) {
        MyEntity savedEntity = repository.save(existing);

        if (entityData.getChildren() != null) {
            List<MyChild> existingChildren = childRepository.findByParentId(savedEntity.getId());

            cascadeUpdateHelper.updateChildrenWithInit(
                savedEntity,                      // Pai
                entityData.getChildren(),         // Filhos do payload
                existingChildren,                 // Filhos existentes
                MyChild::getId,                   // ID extractor
                MyChild::setParent,               // Parent setter
                (existing, payload) -> {          // Update function
                    existing.setName(payload.getName());
                },
                (child) -> {                      // Initializer
                    child.setActive(true);
                },
                childRepository
            );
        }

        return savedEntity;
    }
}
```

### 📚 Documentação Completa

Veja exemplos completos para diversos relacionamentos em:

**→ [CASCADE_UPDATE_HELPER_USAGE.md](CASCADE_UPDATE_HELPER_USAGE.md)**

Exemplos incluem:

- Organization → User
- Order → OrderItem
- Post → Comment
- Playlist → Song
- E muito mais!

## Referências

- JPA Cascade Types: [Hibernate Cascade](https://docs.jboss.org/hibernate/orm/6.0/userguide/html_single/Hibernate_User_Guide.html#pc-cascade)
- Spring Transactions: [@Transactional](https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/annotations.html)
