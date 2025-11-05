# Controller Response Pattern - Objetos Aninhados

## 📋 Visão Geral

Este documento define o padrão para respostas de API em controllers, garantindo que **todas as entidades relacionadas sejam retornadas como objetos aninhados** em vez de campos flat.

## 🎯 Objetivo

- ✅ Evitar campos flat como `cityId`, `cityName`, `organizationId`
- ✅ Retornar objetos completos aninhados: `city: {id, name, state}`
- ✅ Prevenir problemas de lazy loading
- ✅ Evitar referências cíclicas
- ✅ Padronizar todas as respostas de API

## 📦 Estrutura de Pastas

```
src/main/java/com/mvt/mvt_events/
├── dto/
│   ├── BaseDTO.java                    # Classe base para DTOs
│   ├── common/                         # DTOs reutilizáveis
│   │   ├── CityDTO.java               # DTO para City
│   │   ├── OrganizationDTO.java       # DTO para Organization
│   │   └── ...                        # Outros DTOs comuns
│   └── mapper/
│       └── DTOMapper.java             # Utilitário de mapeamento
```

## 🔧 Como Implementar em um Novo Controller

### Passo 1: Criar o Response DTO

```java
@Data
@NoArgsConstructor
public static class MyEntityResponse {
    private Long id;
    private String name;

    // Use DTOs compartilhados para relacionamentos
    private CityDTO city;
    private OrganizationDTO organization;

    public MyEntityResponse(MyEntity entity) {
        this.id = entity.getId();
        this.name = entity.getName();

        // Use DTOMapper para mapear relacionamentos
        this.city = DTOMapper.toDTO(entity.getCity());
        this.organization = DTOMapper.toDTO(entity.getOrganization());
    }
}
```

### Passo 2: Endpoints de Listagem Paginada

```java
@GetMapping
@Operation(summary = "Listar entidades")
public Page<MyEntityResponse> list(Pageable pageable) {
    Page<MyEntity> entities = service.list(pageable);
    return entities.map(MyEntityResponse::new);
}
```

### Passo 3: Endpoints de Busca Individual

```java
@GetMapping("/{id}")
@Operation(summary = "Buscar entidade por ID")
public MyEntityResponse get(@PathVariable Long id) {
    MyEntity entity = service.get(id);
    return new MyEntityResponse(entity);
}
```

### Passo 4: Endpoints de Criação/Atualização

```java
@PostMapping
@ResponseStatus(HttpStatus.CREATED)
public MyEntityResponse create(@RequestBody @Valid MyEntityCreateRequest request) {
    MyEntity entity = service.create(request);
    return new MyEntityResponse(entity);
}

@PutMapping("/{id}")
public MyEntityResponse update(@PathVariable Long id, @RequestBody @Valid MyEntityUpdateRequest request) {
    MyEntity entity = service.update(id, request);
    return new MyEntityResponse(entity);
}
```

## 📝 Template Completo de Controller

```java
@RestController
@RequestMapping("/api/my-entities")
@Tag(name = "My Entities", description = "Gerenciamento de My Entities")
@SecurityRequirement(name = "bearerAuth")
public class MyEntityController {

    private final MyEntityService service;

    public MyEntityController(MyEntityService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Listar entidades")
    public Page<MyEntityResponse> list(
            @RequestParam(required = false) String search,
            Pageable pageable) {
        Page<MyEntity> entities = service.list(search, pageable);
        return entities.map(MyEntityResponse::new);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar entidade por ID")
    public MyEntityResponse get(@PathVariable Long id) {
        MyEntity entity = service.get(id);
        return new MyEntityResponse(entity);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MyEntityResponse create(@RequestBody @Valid MyEntityCreateRequest request) {
        MyEntity entity = service.create(request);
        return new MyEntityResponse(entity);
    }

    @PutMapping("/{id}")
    public MyEntityResponse update(@PathVariable Long id, @RequestBody @Valid MyEntityUpdateRequest request) {
        MyEntity entity = service.update(id, request);
        return new MyEntityResponse(entity);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }

    // ========================================================================
    // DTOs
    // ========================================================================

    @Data
    public static class MyEntityCreateRequest {
        @NotBlank(message = "Nome é obrigatório")
        private String name;

        private Long cityId;
        private CityIdWrapper city;

        public Long getCityIdResolved() {
            if (cityId != null) return cityId;
            if (city != null && city.getId() != null) return city.getId();
            return null;
        }
    }

    @Data
    public static class MyEntityUpdateRequest {
        private String name;
        private Long cityId;
        private CityIdWrapper city;

        public Long getCityIdResolved() {
            if (cityId != null) return cityId;
            if (city != null && city.getId() != null) return city.getId();
            return null;
        }
    }

    @Data
    @NoArgsConstructor
    public static class MyEntityResponse {
        private Long id;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private String name;
        private CityDTO city;
        private OrganizationDTO organization;

        public MyEntityResponse(MyEntity entity) {
            this.id = entity.getId();
            this.createdAt = entity.getCreatedAt();
            this.updatedAt = entity.getUpdatedAt();
            this.name = entity.getName();

            // Use DTOMapper para relacionamentos
            this.city = DTOMapper.toDTO(entity.getCity());
            this.organization = DTOMapper.toDTO(entity.getOrganization());
        }
    }

    @Data
    public static class CityIdWrapper {
        private Long id;
    }
}
```

## 🔍 Formato de Resposta Esperado

### ❌ ERRADO (Campos Flat)

```json
{
  "content": [
    {
      "id": 1,
      "name": "Entity Name",
      "cityId": 1068,
      "cityName": "Ubajara",
      "cityState": "Ceará",
      "organizationId": 6,
      "organizationName": "Moveltrack"
    }
  ]
}
```

### ✅ CORRETO (Objetos Aninhados)

```json
{
  "content": [
    {
      "id": 1,
      "name": "Entity Name",
      "city": {
        "id": 1068,
        "name": "Ubajara",
        "state": "Ceará"
      },
      "organization": {
        "id": 6,
        "name": "Moveltrack"
      }
    }
  ]
}
```

## 🛠️ Service Layer - Transactional

Sempre adicione `@Transactional(readOnly = true)` nos métodos de leitura e **inicialize relacionamentos lazy**:

```java
import org.hibernate.Hibernate;

@Service
@Transactional
public class MyEntityService {

    @Transactional(readOnly = true)
    public Page<MyEntity> list(Pageable pageable) {
        Page<MyEntity> entities = repository.findAll(pageable);

        // ⚠️ IMPORTANTE: Inicializar relacionamentos lazy para evitar LazyInitializationException
        entities.forEach(entity -> {
            Hibernate.initialize(entity.getCity());
            Hibernate.initialize(entity.getOrganization());
        });

        return entities;
    }

    @Transactional(readOnly = true)
    public MyEntity get(Long id) {
        MyEntity entity = repository.findById(id)
            .orElseThrow(() -> new RuntimeException("Entity not found"));

        // ⚠️ IMPORTANTE: Inicializar relacionamentos lazy
        Hibernate.initialize(entity.getCity());
        Hibernate.initialize(entity.getOrganization());

        return entity;
    }
}
```

**Por que isso é necessário?**

Quando o método do service retorna, a transação é fechada. Se o DTO tentar acessar `entity.getCity()` fora da transação, ocorrerá `LazyInitializationException`. Usando `Hibernate.initialize()`, forçamos o carregamento dentro da transação.

📖 **Leia mais:** [LAZY_LOADING_SOLUTION.md](./LAZY_LOADING_SOLUTION.md)

## 📚 DTOs Compartilhados

Use sempre os DTOs compartilhados em `dto/common/`:

- `CityDTO` - para relacionamentos com City
- `OrganizationDTO` - para relacionamentos com Organization

### Como adicionar um novo DTO compartilhado:

1. Crie a classe em `dto/common/`:

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategoryDTO {
    private Long id;
    private String name;
}
```

2. Adicione o método no `DTOMapper`:

```java
public static CategoryDTO toDTO(Category category) {
    if (category == null) return null;
    return new CategoryDTO(category.getId(), category.getName());
}
```

## ✅ Checklist para Novo Controller

- [ ] Criar `MyEntityResponse` com objetos aninhados
- [ ] Usar `DTOMapper.toDTO()` para relacionamentos
- [ ] Todos os endpoints retornam DTOs (não entidades)
- [ ] Service tem `@Transactional(readOnly = true)` nos métodos de leitura
- [ ] Request DTOs aceitam tanto `cityId` quanto `city: {id}`
- [ ] Testar resposta JSON para garantir objetos aninhados

## 📖 Exemplos Implementados

Veja os seguintes controllers como referência:

- `UserController` - exemplo completo com City e Organization
- `OrganizationController` - exemplo com City

## 🎓 Boas Práticas

1. **Sempre use DTOs nas respostas** - nunca retorne entidades JPA diretamente
2. **Reutilize DTOs comuns** - evite duplicação de código
3. **Use DTOMapper** - centralize a lógica de mapeamento
4. **Adicione @Transactional** - evite lazy loading exceptions
5. **Documente com Swagger** - use `@Operation` nos endpoints
6. **Valide entrada** - use `@Valid` nos requests

## 🚀 Migração de Controllers Existentes

Para atualizar um controller existente:

1. Crie o `MyEntityResponse` DTO
2. Atualize todos os endpoints para retornar o DTO
3. Use `entities.map(MyEntityResponse::new)` para paginação
4. Adicione `@Transactional(readOnly = true)` no service
5. Teste a resposta JSON

---

**Última atualização**: Outubro 2025  
**Responsável**: Equipe de Desenvolvimento MVT Events
