# Solução para LazyInitializationException

## 🐛 Problema

Ao retornar objetos JPA com relacionamentos `@ManyToOne(fetch = FetchType.LAZY)` em DTOs, ocorre o erro:

```
Could not initialize proxy [com.mvt.mvt_events.jpa.City#1068] - no session
```

### Causa

1. O método do service retorna a entidade dentro de uma transação
2. A transação é encerrada quando o método retorna
3. O controller tenta acessar `organization.getCity()` no construtor do DTO
4. O relacionamento está lazy e a sessão Hibernate já foi fechada
5. **LazyInitializationException** é lançada

## ✅ Solução Implementada

### Inicializar Relacionamentos Lazy dentro da Transação

Use `Hibernate.initialize()` para forçar o carregamento do relacionamento **antes** da transação fechar:

```java
import org.hibernate.Hibernate;

@Service
@Transactional
public class OrganizationService {

    @Transactional(readOnly = true)
    public Page<Organization> list(Pageable pageable) {
        Page<Organization> organizations = repository.findAll(pageable);

        // Inicializar relacionamento city para evitar LazyInitializationException
        organizations.forEach(org -> Hibernate.initialize(org.getCity()));

        return organizations;
    }

    @Transactional(readOnly = true)
    public Organization get(Long id) {
        Organization organization = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Not found"));

        // Inicializar relacionamento city
        Hibernate.initialize(organization.getCity());

        return organization;
    }
}
```

## 🔧 Checklist de Implementação

Quando criar um novo service com relacionamentos lazy:

- [ ] Adicionar `import org.hibernate.Hibernate;`
- [ ] Adicionar `@Transactional(readOnly = true)` nos métodos de leitura
- [ ] Chamar `Hibernate.initialize(entity.getRelationship())` para cada relacionamento lazy
- [ ] Para Pages, usar `forEach`: `page.forEach(e -> Hibernate.initialize(e.getRel()))`

## 📋 Alternativas (Não Recomendadas)

### ❌ 1. Mudar para EAGER

```java
@ManyToOne(fetch = FetchType.EAGER) // NÃO RECOMENDADO
@JoinColumn(name = "city_id")
private City city;
```

**Problema:** Carrega sempre, mesmo quando não necessário, impactando performance.

### ❌ 2. Open Session In View

```properties
spring.jpa.open-in-view=true # NÃO RECOMENDADO
```

**Problema:** Mantém conexões abertas desnecessariamente, problemas de performance.

### ✅ 3. Hibernate.initialize() (RECOMENDADO)

```java
Hibernate.initialize(organization.getCity());
```

**Vantagem:** Controle explícito sobre o que carregar, melhor performance.

## 🎯 Padrão Estabelecido

### Service Layer

```java
@Service
@Transactional
public class MyEntityService {

    @Transactional(readOnly = true)
    public Page<MyEntity> list(Pageable pageable) {
        Page<MyEntity> entities = repository.findAll(pageable);

        // Inicializar todos os relacionamentos lazy que serão usados no DTO
        entities.forEach(entity -> {
            Hibernate.initialize(entity.getCity());
            Hibernate.initialize(entity.getOrganization());
            // ... outros relacionamentos lazy
        });

        return entities;
    }

    @Transactional(readOnly = true)
    public MyEntity get(Long id) {
        MyEntity entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Not found"));

        // Inicializar relacionamentos lazy
        Hibernate.initialize(entity.getCity());
        Hibernate.initialize(entity.getOrganization());

        return entity;
    }
}
```

### Controller Layer

```java
@GetMapping
public Page<MyEntityResponse> list(Pageable pageable) {
    Page<MyEntity> entities = service.list(pageable);

    // Os relacionamentos já estão inicializados, DTO pode acessá-los
    return entities.map(MyEntityResponse::new);
}
```

### DTO Layer

```java
@Data
@NoArgsConstructor
public static class MyEntityResponse {
    private Long id;
    private CityDTO city;

    public MyEntityResponse(MyEntity entity) {
        this.id = entity.getId();

        // Seguro porque Hibernate.initialize() foi chamado no service
        this.city = DTOMapper.toDTO(entity.getCity());
    }
}
```

## 🚀 Performance

### Consultas Geradas

**Sem Hibernate.initialize():**

```sql
-- Consulta principal
SELECT * FROM organizations LIMIT 10;

-- N+1 Problem: Uma consulta para cada city
SELECT * FROM cities WHERE id = 1068;
SELECT * FROM cities WHERE id = 1069;
-- ... mais consultas
```

**Com Hibernate.initialize():**

```sql
-- Consulta principal
SELECT * FROM organizations LIMIT 10;

-- Queries individuais otimizadas
SELECT * FROM cities WHERE id = 1068;
SELECT * FROM cities WHERE id = 1069;
-- ... (Hibernate pode fazer batch fetching)
```

### Otimização Adicional: JOIN FETCH

Para melhor performance, use `JOIN FETCH` em queries customizadas:

```java
@Query("SELECT o FROM Organization o LEFT JOIN FETCH o.city WHERE o.id = :id")
Optional<Organization> findByIdWithCity(@Param("id") Long id);

@Query("SELECT o FROM Organization o LEFT JOIN FETCH o.city")
List<Organization> findAllWithCity();
```

## 📚 Referências

- [Hibernate Documentation - Fetching](https://docs.jboss.org/hibernate/orm/current/userguide/html_single/Hibernate_User_Guide.html#fetching)
- [Spring Data JPA - @EntityGraph](https://docs.spring.io/spring-data/jpa/docs/current/reference/html/#jpa.entity-graph)
- [N+1 Problem Solutions](https://vladmihalcea.com/n-plus-1-query-problem/)

## ✅ Services Atualizados

- ✅ `OrganizationService` - Inicializa `city` em todos os métodos de leitura
- ⚠️ `UserService` - **Precisa ser atualizado** (se usar relacionamentos lazy)
- ⚠️ Outros services - Revisar e aplicar o padrão

---

**Última atualização:** Outubro 2025  
**Padrão estabelecido:** Inicialização explícita com `Hibernate.initialize()`
