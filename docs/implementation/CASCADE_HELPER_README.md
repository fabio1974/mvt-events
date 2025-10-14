# CascadeUpdateHelper - Quick Reference

## 🎯 O Que É?

Helper genérico e reutilizável para **atualização em cascata** de relacionamentos 1:N (pai → filhos).

## 📦 Localização

```
src/main/java/com/mvt/mvt_events/util/CascadeUpdateHelper.java
```

## ⚡ Uso Rápido

### 1. Injete o Helper

```java
@Service
@Transactional
public class MyService {
    private final CascadeUpdateHelper cascadeUpdateHelper;

    public MyService(CascadeUpdateHelper helper) {
        this.cascadeUpdateHelper = helper;
    }
}
```

### 2. Use no Método Update

```java
public Parent update(Long id, Parent parentData) {
    Parent savedParent = repository.save(existing);

    if (parentData.getChildren() != null) {
        List<Child> existingChildren = childRepo.findByParentId(savedParent.getId());

        cascadeUpdateHelper.updateChildrenWithInit(
            savedParent,                  // 1. Entidade pai
            parentData.getChildren(),     // 2. Filhos do payload
            existingChildren,             // 3. Filhos existentes
            Child::getId,                 // 4. Como pegar ID
            Child::setParent,             // 5. Como setar pai
            (existing, payload) -> {      // 6. Como atualizar
                existing.setName(payload.getName());
            },
            (child) -> {                  // 7. Como inicializar novos
                child.setActive(true);
            },
            childRepo                     // 8. Repository
        );
    }

    return savedParent;
}
```

## 🔄 O Que Faz?

| Situação                              | Ação          |
| ------------------------------------- | ------------- |
| Filho **sem ID** no payload           | ➕ **INSERT** |
| Filho **com ID** no payload           | ✏️ **UPDATE** |
| Filho no banco **mas não no payload** | 🗑️ **DELETE** |

## 📚 Documentação Completa

- **[CASCADE_UPDATE_HELPER_USAGE.md](CASCADE_UPDATE_HELPER_USAGE.md)** - Guia completo com exemplos
- **[CASCADE_UPDATE_1_N.md](CASCADE_UPDATE_1_N.md)** - Conceitos e detalhes técnicos

## ✨ Benefícios

- ✅ **15 linhas** em vez de 80
- ✅ **Reutilizável** em qualquer serviço
- ✅ **Type-safe** com generics
- ✅ **Logs consistentes** (`📦`, `➕`, `✏️`, `🗑️`)
- ✅ **Testável** e mockável

## 🎓 Exemplos Reais

### Event → EventCategory (Implementado)

```java
cascadeUpdateHelper.updateChildrenWithInit(
    savedEvent,
    eventData.getCategories(),
    existingCategories,
    EventCategory::getId,
    EventCategory::setEvent,
    (e, p) -> { e.setName(p.getName()); },
    (c) -> { c.setCurrentParticipants(0); },
    categoryRepository
);
```

### Organization → User

```java
cascadeUpdateHelper.updateChildrenWithInit(
    savedOrg,
    orgData.getUsers(),
    existingUsers,
    User::getId,
    User::setOrganization,
    (e, p) -> { e.setRole(p.getRole()); },
    (u) -> { u.setActive(true); },
    userRepository
);
```

### Order → OrderItem

```java
cascadeUpdateHelper.updateChildrenWithInit(
    savedOrder,
    orderData.getItems(),
    existingItems,
    OrderItem::getId,
    OrderItem::setOrder,
    (e, p) -> {
        e.setQuantity(p.getQuantity());
        e.setSubtotal(e.getQuantity() * e.getPrice());
    },
    (i) -> { i.setQuantity(1); },
    orderItemRepository
);
```

## 🧪 Como Testar?

### Teste 1: INSERT (novo filho)

```bash
curl -X PUT /api/events/1 -d '{
  "categories": [
    {"name": "Nova Categoria", "price": 100}
  ]
}'
```

**Log esperado:**

```
📦 CASCADE UPDATE - Existing children: 0
📦 CASCADE UPDATE - Payload IDs: []
➕ CASCADE INSERT - Creating new child
✅ CASCADE UPDATE - Complete
```

### Teste 2: UPDATE (filho existente)

```bash
curl -X PUT /api/events/1 -d '{
  "categories": [
    {"id": 22, "name": "Nome Atualizado", "price": 150}
  ]
}'
```

**Log esperado:**

```
📦 CASCADE UPDATE - Existing children: 1
📦 CASCADE UPDATE - Payload IDs: [22]
✏️  CASCADE UPDATE - Updating child ID: 22
✅ CASCADE UPDATE - Complete
```

### Teste 3: DELETE (remover filho)

```bash
curl -X PUT /api/events/1 -d '{
  "categories": []
}'
```

**Log esperado:**

```
📦 CASCADE UPDATE - Existing children: 1
📦 CASCADE UPDATE - Payload IDs: []
🗑️  CASCADE DELETE - Deleting 1 children
   🗑️  Deleting child ID: 22
✅ CASCADE UPDATE - Complete
```

## 💡 Dicas

### ⚠️ Sempre use `@Transactional`

```java
@Transactional  // ← IMPORTANTE!
public Parent update(Long id, Parent data) {
    // ...
}
```

### ⚠️ Force load de relacionamentos

```java
Parent refreshed = repository.findById(id).orElseThrow();
refreshed.getChildren().size(); // ← Force load
return refreshed;
```

### ⚠️ Não enviar children = não altera

Se `parentData.getChildren()` for `null`, não modifica children existentes.

Para **deletar todos**, envie `children: []` (array vazio).

## 🔗 Ver Também

- Implementação: `EventService.update()`
- Testes: `EventServiceTest`
- JPA Cascade: [Hibernate Docs](https://docs.jboss.org/hibernate/orm/6.0/userguide/html_single/Hibernate_User_Guide.html#pc-cascade)
