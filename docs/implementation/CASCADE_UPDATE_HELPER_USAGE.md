# CascadeUpdateHelper - Guia de Uso

## Visão Geral

O `CascadeUpdateHelper` é uma **classe utilitária genérica** que encapsula a lógica de atualização em cascata para relacionamentos 1:N.

**Benefícios:**

- ✅ **Reutilizável** para qualquer relacionamento 1:N
- ✅ **Type-safe** com generics
- ✅ **Manutenível** - lógica centralizada
- ✅ **Testável** - pode ser mockado
- ✅ **Logs consistentes** - mesmos emojis e formato

## Anatomia do Helper

```java
@Component
public class CascadeUpdateHelper {

    public <PARENT, CHILD, ID> void updateChildrenWithInit(
        PARENT parent,                      // Entidade pai (ex: Event)
        List<CHILD> payloadChildren,        // Lista de filhos no payload
        List<CHILD> existingChildren,       // Lista de filhos no banco
        Function<CHILD, ID> idExtractor,    // Como extrair ID do filho
        BiConsumer<CHILD, PARENT> parentSetter,  // Como vincular filho ao pai
        BiConsumer<CHILD, CHILD> updateFunction, // Como atualizar campos
        Consumer<CHILD> childInitializer,   // Como inicializar novos filhos
        JpaRepository<CHILD, ID> repository // Repository do filho
    )
}
```

## Exemplos Práticos

### 1. Event → EventCategory (Implementado)

```java
@Service
@Transactional
public class EventService {
    private final CascadeUpdateHelper cascadeUpdateHelper;
    private final EventCategoryRepository categoryRepository;

    public Event update(Long id, Event eventData) {
        Event savedEvent = repository.save(existing);

        if (eventData.getCategories() != null) {
            Specification<EventCategory> spec = EventCategorySpecification.belongsToEvent(savedEvent.getId());
            List<EventCategory> existingCategories = categoryRepository.findAll(spec);

            cascadeUpdateHelper.updateChildrenWithInit(
                savedEvent,                       // Pai: Event
                eventData.getCategories(),        // Filhos do payload
                existingCategories,               // Filhos existentes
                EventCategory::getId,             // ID extractor
                EventCategory::setEvent,          // Parent setter
                (existingCat, payloadCat) -> {    // Update function
                    if (payloadCat.getName() != null)
                        existingCat.setName(payloadCat.getName());
                    if (payloadCat.getPrice() != null)
                        existingCat.setPrice(payloadCat.getPrice());
                    // ... outros campos
                },
                (child) -> {                      // Initializer
                    if (child.getCurrentParticipants() == null) {
                        child.setCurrentParticipants(0);
                    }
                },
                categoryRepository
            );
        }

        return refreshedEvent;
    }
}
```

### 2. Organization → User (Exemplo)

```java
@Service
@Transactional
public class OrganizationService {
    private final CascadeUpdateHelper cascadeUpdateHelper;
    private final UserRepository userRepository;

    public Organization update(Long id, Organization organizationData) {
        Organization savedOrg = repository.save(existing);

        if (organizationData.getUsers() != null) {
            Specification<User> spec = UserSpecification.belongsToOrganization(savedOrg.getId());
            List<User> existingUsers = userRepository.findAll(spec);

            cascadeUpdateHelper.updateChildrenWithInit(
                savedOrg,                         // Pai: Organization
                organizationData.getUsers(),      // Filhos do payload
                existingUsers,                    // Filhos existentes
                User::getId,                      // ID extractor (UUID)
                User::setOrganization,            // Parent setter
                (existingUser, payloadUser) -> {  // Update function
                    if (payloadUser.getName() != null)
                        existingUser.setName(payloadUser.getName());
                    if (payloadUser.getEmail() != null)
                        existingUser.setEmail(payloadUser.getEmail());
                    if (payloadUser.getRole() != null)
                        existingUser.setRole(payloadUser.getRole());
                },
                (user) -> {                       // Initializer
                    if (user.getActive() == null) {
                        user.setActive(true);
                    }
                },
                userRepository
            );
        }

        return savedOrg;
    }
}
```

### 3. Order → OrderItem (Exemplo - E-commerce)

```java
@Service
@Transactional
public class OrderService {
    private final CascadeUpdateHelper cascadeUpdateHelper;
    private final OrderItemRepository orderItemRepository;

    public Order update(Long id, Order orderData) {
        Order savedOrder = repository.save(existing);

        if (orderData.getItems() != null) {
            Specification<OrderItem> spec = OrderItemSpecification.belongsToOrder(savedOrder.getId());
            List<OrderItem> existingItems = orderItemRepository.findAll(spec);

            cascadeUpdateHelper.updateChildrenWithInit(
                savedOrder,                       // Pai: Order
                orderData.getItems(),             // Filhos do payload
                existingItems,                    // Filhos existentes
                OrderItem::getId,                 // ID extractor
                OrderItem::setOrder,              // Parent setter
                (existingItem, payloadItem) -> {  // Update function
                    if (payloadItem.getQuantity() != null)
                        existingItem.setQuantity(payloadItem.getQuantity());
                    if (payloadItem.getPrice() != null)
                        existingItem.setPrice(payloadItem.getPrice());
                    // Recalcula subtotal
                    existingItem.setSubtotal(
                        existingItem.getQuantity() * existingItem.getPrice()
                    );
                },
                (item) -> {                       // Initializer
                    if (item.getQuantity() == null) {
                        item.setQuantity(1);
                    }
                    // Calcula subtotal inicial
                    item.setSubtotal(item.getQuantity() * item.getPrice());
                },
                orderItemRepository
            );
        }

        return savedOrder;
    }
}
```

### 4. Post → Comment (Exemplo - Blog/Rede Social)

```java
@Service
@Transactional
public class PostService {
    private final CascadeUpdateHelper cascadeUpdateHelper;
    private final CommentRepository commentRepository;

    public Post update(Long id, Post postData) {
        Post savedPost = repository.save(existing);

        if (postData.getComments() != null) {
            Specification<Comment> spec = CommentSpecification.belongsToPost(savedPost.getId());
            List<Comment> existingComments = commentRepository.findAll(spec);

            cascadeUpdateHelper.updateChildrenWithInit(
                savedPost,                        // Pai: Post
                postData.getComments(),           // Filhos do payload
                existingComments,                 // Filhos existentes
                Comment::getId,                   // ID extractor
                Comment::setPost,                 // Parent setter
                (existingComment, payloadComment) -> {  // Update function
                    if (payloadComment.getText() != null)
                        existingComment.setText(payloadComment.getText());
                    if (payloadComment.isApproved() != null)
                        existingComment.setApproved(payloadComment.isApproved());
                },
                (comment) -> {                    // Initializer
                    if (comment.getApproved() == null) {
                        comment.setApproved(false); // Comentários precisam aprovação
                    }
                    if (comment.getCreatedAt() == null) {
                        comment.setCreatedAt(LocalDateTime.now());
                    }
                },
                commentRepository
            );
        }

        return savedPost;
    }
}
```

### 5. Playlist → Song (Exemplo - Música)

```java
@Service
@Transactional
public class PlaylistService {
    private final CascadeUpdateHelper cascadeUpdateHelper;
    private final PlaylistSongRepository playlistSongRepository;

    public Playlist update(Long id, Playlist playlistData) {
        Playlist savedPlaylist = repository.save(existing);

        if (playlistData.getSongs() != null) {
            Specification<PlaylistSong> spec = PlaylistSongSpecification.belongsToPlaylist(savedPlaylist.getId());
            List<PlaylistSong> existingSongs = playlistSongRepository.findAll(spec);

            cascadeUpdateHelper.updateChildrenWithInit(
                savedPlaylist,                    // Pai: Playlist
                playlistData.getSongs(),          // Filhos do payload
                existingSongs,                    // Filhos existentes
                PlaylistSong::getId,              // ID extractor
                PlaylistSong::setPlaylist,        // Parent setter
                (existingSong, payloadSong) -> {  // Update function
                    if (payloadSong.getPosition() != null)
                        existingSong.setPosition(payloadSong.getPosition());
                    if (payloadSong.getSong() != null)
                        existingSong.setSong(payloadSong.getSong());
                },
                (song) -> {                       // Initializer
                    if (song.getPosition() == null) {
                        // Auto-incrementa posição
                        song.setPosition(existingSongs.size() + 1);
                    }
                },
                playlistSongRepository
            );
        }

        return savedPlaylist;
    }
}
```

## Variações do Helper

### Versão Simples (Sem Initializer)

Quando não precisa inicializar campos nos novos filhos:

```java
cascadeUpdateHelper.updateChildren(
    parent,
    payloadChildren,
    existingChildren,
    Child::getId,
    Child::setParent,
    (existing, payload) -> {
        // Update logic
    },
    repository
);
```

### Versão Minimalista (Sem Update Customizado)

Quando não precisa de lógica de update complexa:

```java
cascadeUpdateHelper.updateChildren(
    parent,
    payloadChildren,
    existingChildren,
    Child::getId,
    Child::setParent,
    repository
);
```

## Padrão de Implementação

### Passo 1: Injetar o Helper

```java
@Service
@Transactional
public class MyEntityService {
    private final CascadeUpdateHelper cascadeUpdateHelper;

    public MyEntityService(CascadeUpdateHelper cascadeUpdateHelper) {
        this.cascadeUpdateHelper = cascadeUpdateHelper;
    }
}
```

### Passo 2: Adicionar Lógica no Update

```java
public MyEntity update(Long id, MyEntity entityData) {
    // 1. Atualizar campos do pai
    MyEntity savedEntity = repository.save(existing);

    // 2. Atualizar filhos em cascata (se presentes no payload)
    if (entityData.getChildren() != null) {
        // 2.1. Buscar filhos existentes
        List<MyChild> existingChildren = childRepository.findByParentId(savedEntity.getId());

        // 2.2. Usar helper para cascade update
        cascadeUpdateHelper.updateChildrenWithInit(
            savedEntity,
            entityData.getChildren(),
            existingChildren,
            MyChild::getId,
            MyChild::setParent,
            (existing, payload) -> { /* update logic */ },
            (child) -> { /* init logic */ },
            childRepository
        );
    }

    // 3. Retornar entidade atualizada
    return repository.findById(savedEntity.getId())
        .orElseThrow(() -> new RuntimeException("Not found"));
}
```

## Logs de Debug

O helper imprime logs consistentes para todas as operações:

```
📦 CASCADE UPDATE - Existing children: 3
📦 CASCADE UPDATE - Payload IDs: [1, 2]
🗑️  CASCADE DELETE - Deleting 1 children
   🗑️  Deleting child ID: 3
✏️  CASCADE UPDATE - Updating child ID: 1
✏️  CASCADE UPDATE - Updating child ID: 2
➕ CASCADE INSERT - Creating new child
✅ CASCADE UPDATE - Complete
```

## Vantagens

| Aspecto              | Antes (Manual)                 | Depois (Helper)         |
| -------------------- | ------------------------------ | ----------------------- |
| **Linhas de código** | ~80 linhas                     | ~15 linhas              |
| **Reutilização**     | ❌ Duplicado em cada service   | ✅ Centralizado         |
| **Manutenção**       | ❌ Atualizar em vários lugares | ✅ Atualizar em 1 lugar |
| **Testes**           | ❌ Testar em cada service      | ✅ Testar só o helper   |
| **Consistência**     | ❌ Lógica pode divergir        | ✅ Sempre igual         |
| **Logs**             | ❌ Diferentes em cada lugar    | ✅ Sempre iguais        |

## Testes Unitários

Exemplo de teste para o helper:

```java
@ExtendWith(MockitoExtension.class)
class CascadeUpdateHelperTest {

    @Mock
    private JpaRepository<EventCategory, Long> repository;

    @InjectMocks
    private CascadeUpdateHelper helper;

    @Test
    void shouldInsertNewChildrenWithoutId() {
        Event parent = new Event();
        EventCategory newCategory = new EventCategory();
        newCategory.setName("New Category");

        helper.updateChildrenWithInit(
            parent,
            List.of(newCategory),
            List.of(),
            EventCategory::getId,
            EventCategory::setEvent,
            (e, p) -> {},
            (c) -> c.setCurrentParticipants(0),
            repository
        );

        verify(repository).save(newCategory);
        assertEquals(parent, newCategory.getEvent());
        assertEquals(0, newCategory.getCurrentParticipants());
    }

    @Test
    void shouldUpdateExistingChildrenWithId() {
        Event parent = new Event();
        EventCategory existing = new EventCategory();
        existing.setId(1L);
        existing.setName("Old Name");

        EventCategory payload = new EventCategory();
        payload.setId(1L);
        payload.setName("New Name");

        when(repository.findById(1L)).thenReturn(Optional.of(existing));

        helper.updateChildrenWithInit(
            parent,
            List.of(payload),
            List.of(existing),
            EventCategory::getId,
            EventCategory::setEvent,
            (e, p) -> e.setName(p.getName()),
            (c) -> {},
            repository
        );

        verify(repository).save(existing);
        assertEquals("New Name", existing.getName());
    }

    @Test
    void shouldDeleteChildrenNotInPayload() {
        Event parent = new Event();
        EventCategory toDelete = new EventCategory();
        toDelete.setId(2L);

        helper.updateChildrenWithInit(
            parent,
            List.of(), // Payload vazio
            List.of(toDelete), // Existe no banco
            EventCategory::getId,
            EventCategory::setEvent,
            (e, p) -> {},
            (c) -> {},
            repository
        );

        verify(repository).deleteAll(List.of(toDelete));
        verify(repository).flush();
    }
}
```

## Troubleshooting

### Problema: "Child not found with ID"

**Causa:** Payload enviou ID que não existe no banco.

**Solução:** Validar IDs no controller antes de chamar service, ou retornar erro 400.

### Problema: LazyInitializationException

**Causa:** Tentar acessar children fora da transação.

**Solução:** Garantir que método do service tem `@Transactional` e fazer force load:

```java
entity.getChildren().size(); // Force load
```

### Problema: ConcurrentModificationException

**Causa:** Modificar lista enquanto itera.

**Solução:** O helper já trata isso internamente com `Collectors.toList()`.

## Referências

- Código: `src/main/java/com/mvt/mvt_events/util/CascadeUpdateHelper.java`
- Uso: `src/main/java/com/mvt/mvt_events/service/EventService.java`
- Docs: `docs/implementation/CASCADE_UPDATE_1_N.md`
