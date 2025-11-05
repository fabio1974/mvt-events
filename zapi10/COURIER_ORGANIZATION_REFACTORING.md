# Refatoração: Relacionamento COURIER ↔ Organization (N:M)

**Data:** 22 de outubro de 2025  
**Status:** 🔄 Em Implementação  
**Versão:** 2.0

---

## 📋 Mudança de Modelo

### ❌ **ANTES (INCORRETO)**

```
Gerente ADM ↔ COURIER (N:M)
    └── courier_adm_links (tabela intermediária)
```

### ✅ **DEPOIS (CORRETO)**

```
Gerente ADM → Organization (1:1 / N:1)
    └── organization.adm_id (FK)

COURIER → Organization (N:M)
    └── courier_organizations (tabela intermediária)
```

---

## 🎯 Novo Modelo de Negócio

### Relacionamentos

1. **Gerente ADM ↔ Organization**

   - Tipo: **N:1** (vários gerentes ADM podem estar na mesma organização)
   - Implementação: Coluna `organization_id` na tabela `users`
   - Status: ✅ **JÁ EXISTE**

2. **COURIER ↔ Organization**

   - Tipo: **N:M** (um motoboy pode trabalhar em várias organizações)
   - Implementação: Tabela intermediária `courier_organizations`
   - Status: 🔄 **A IMPLEMENTAR**

3. **CLIENT ↔ Organization**
   - Tipo: **N:M** (um cliente pode pertencer a vários grupos)
   - Implementação: Tabela intermediária `client_organizations`
   - Status: 📋 **PLANEJADO PARA FUTURO**

---

## 🗄️ Nova Estrutura do Banco de Dados

### Tabela: `courier_organizations`

```sql
CREATE TABLE courier_organizations (
    id BIGSERIAL PRIMARY KEY,
    courier_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    organization_id BIGINT NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,

    -- Metadados
    linked_at TIMESTAMP NOT NULL DEFAULT NOW(),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,

    -- Audit
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),

    -- Constraints
    UNIQUE(courier_id, organization_id)
);

CREATE INDEX idx_courier_orgs_courier ON courier_organizations(courier_id);
CREATE INDEX idx_courier_orgs_organization ON courier_organizations(organization_id);
CREATE INDEX idx_courier_orgs_active ON courier_organizations(is_active);
```

### Remover Tabela Antiga

```sql
-- Remover relacionamento antigo COURIER ↔ ADM
DROP TABLE IF EXISTS courier_adm_links CASCADE;
```

---

## 💻 Implementação no Código

### 1. Criar Entidade: `CourierOrganization`

```java
@Entity
@Table(name = "courier_organizations",
       uniqueConstraints = @UniqueConstraint(columnNames = {"courier_id", "organization_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class CourierOrganization extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "courier_id", nullable = false)
    private User courier;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Column(name = "linked_at", nullable = false)
    private LocalDateTime linkedAt = LocalDateTime.now();

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;
}
```

### 2. Atualizar Entidade: `User`

```java
// Adicionar relacionamento N:M com Organizations (apenas para COURIER)
@ManyToMany
@JoinTable(
    name = "courier_organizations",
    joinColumns = @JoinColumn(name = "courier_id"),
    inverseJoinColumns = @JoinColumn(name = "organization_id")
)
@JsonIgnore
private Set<Organization> courierOrganizations = new HashSet<>();
```

### 3. Atualizar Entidade: `Organization`

```java
// Adicionar relacionamento N:M com Couriers
@ManyToMany(mappedBy = "courierOrganizations")
@JsonIgnore
private Set<User> couriers = new HashSet<>();
```

---

## 🔄 Migração de Dados

### Script SQL

```sql
-- 1. Criar nova tabela
-- (veja SQL acima)

-- 2. Migrar dados de courier_adm_links para courier_organizations
INSERT INTO courier_organizations (courier_id, organization_id, linked_at, is_active, created_at, updated_at)
SELECT
    cal.courier_id,
    u.organization_id,
    cal.linked_at,
    cal.is_active,
    cal.created_at,
    cal.updated_at
FROM courier_adm_links cal
INNER JOIN users u ON u.id = cal.adm_id
WHERE u.organization_id IS NOT NULL
ON CONFLICT (courier_id, organization_id) DO NOTHING;

-- 3. Remover tabela antiga
DROP TABLE courier_adm_links CASCADE;
```

---

## 📝 Mudanças no Código

### Arquivos a Remover

- ❌ `CourierADMLink.java`
- ❌ `CourierADMLinkRepository.java`
- ❌ `CourierADMLinkService.java`
- ❌ `CourierADMLinkController.java`

### Arquivos a Criar

- ✅ `CourierOrganization.java`
- ✅ `CourierOrganizationRepository.java`
- ✅ `CourierOrganizationService.java`
- ✅ `CourierOrganizationController.java`

### Arquivos a Atualizar

- 🔄 `User.java` - Adicionar `Set<Organization> courierOrganizations`
- 🔄 `Organization.java` - Adicionar `Set<User> couriers`
- 🔄 `DeliveryService.java` - Atualizar lógica de validação
- 🔄 `CourierProfile.java` - Remover referências a `courier_adm_links`

---

## ✅ Checklist de Implementação

### Fase 1: Criação

- [ ] Criar `CourierOrganization.java`
- [ ] Criar `CourierOrganizationRepository.java`
- [ ] Atualizar `User.java` (adicionar relacionamento N:M)
- [ ] Atualizar `Organization.java` (adicionar relacionamento N:M)
- [ ] Criar migration SQL

### Fase 2: Migração de Dados

- [ ] Executar script de migração
- [ ] Validar dados migrados
- [ ] Backup dos dados antigos

### Fase 3: Refatoração

- [ ] Atualizar `DeliveryService.java`
- [ ] Atualizar `CourierProfile.java`
- [ ] Remover `CourierADMLink.java`
- [ ] Remover `CourierADMLinkRepository.java`
- [ ] Remover services e controllers relacionados

### Fase 4: Testes

- [ ] Testar criação de courier com múltiplas organizations
- [ ] Testar listagem de couriers por organization
- [ ] Testar listagem de organizations por courier
- [ ] Testar remoção de vínculo

### Fase 5: Documentação

- [ ] Atualizar documentação de API
- [ ] Atualizar diagramas MER
- [ ] Atualizar guias de uso

---

## 🎯 Benefícios

1. **Modelo mais limpo**: Relacionamento direto COURIER ↔ Organization
2. **Eliminação de redundância**: Não precisa mais de ADM como intermediário
3. **Escalabilidade**: Fácil adicionar CLIENT ↔ Organization no futuro
4. **Consistência**: Todos os roles se relacionam com Organization da mesma forma

---

## 📚 Próximos Passos

1. **Implementar CourierOrganization** (N:M)
2. **Migrar dados** da tabela antiga
3. **Remover código legado**
4. **Implementar ClientOrganization** (N:M) - futuro

---

**Quer que eu implemente essa refatoração agora?** 🚀
