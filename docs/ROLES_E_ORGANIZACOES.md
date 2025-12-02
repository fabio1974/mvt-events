# 📚 Documentação do Sistema de Roles e Organizações - Zapi10

## 🎯 Visão Geral

O Zapi10 é um sistema de gerenciamento de entregas que utiliza um modelo de multi-tenancy baseado em **organizações** (empresas de motoboys/entregadores). O sistema possui 4 roles principais, cada um com regras específicas sobre seu relacionamento com organizações.

---

## 👥 Roles do Sistema

### 1. 🔧 ADMIN (Administrador do Sistema)

**Descrição:** Usuário com acesso total ao sistema, responsável pela administração global.

#### Características:
- ❌ **NÃO possui** `organization_id`
- ✅ **Acesso irrestrito** a todas as organizações e entregas
- ✅ **Sem filtro de tenant** - vê TODOS os dados do sistema
- ✅ Pode gerenciar usuários, organizações e configurações globais

#### Regras de Banco de Dados:
```sql
-- Constraint no banco
ALTER TABLE users ADD CONSTRAINT chk_admin_no_organization
CHECK (role != 'ADMIN' OR organization_id IS NULL);
```

#### Exemplo de Token JWT:
```json
{
  "role": "ADMIN",
  "userId": "5a9ec5f8-6a5f-44d4-bb76-82ff3e872d57",
  "email": "admin@zapi10.com",
  "name": "Administrador Sistema",
  "authorities": ["ROLE_ADMIN"]
  // ⚠️ Nota: NÃO contém organizationId
}
```

#### Casos de Uso:
- Visualizar todas as entregas do sistema
- Gerenciar todas as organizações
- Criar/editar/excluir usuários de qualquer tipo
- Acessar relatórios globais
- Configurar integrações e parâmetros do sistema

---

### 2. 🏍️ COURIER (Motoboy/Entregador)

**Descrição:** Profissional que realiza as entregas. Pode trabalhar para uma ou múltiplas organizações.

#### Características:
- ❌ **NÃO possui** `organization_id` direto na tabela `users`
- ✅ **Obtém organizações** através da tabela `employment_contracts` (contratos de trabalho)
- ✅ Vê apenas entregas das organizações onde possui contrato ativo
- ✅ Pode ter múltiplos contratos simultâneos (trabalha para várias empresas)

#### Regras de Banco de Dados:
```sql
-- Constraint no banco
ALTER TABLE users ADD CONSTRAINT chk_courier_no_organization
CHECK (role != 'COURIER' OR organization_id IS NULL);

-- Relacionamento via employment_contracts
-- Um COURIER pode ter N contratos ativos
SELECT ec.organization_id, o.name
FROM employment_contracts ec
JOIN organizations o ON o.id = ec.organization_id
WHERE ec.courier_id = 'courier-uuid-here'
  AND ec.is_active = true;
```

#### Modelo de Dados - Employment Contract:
```sql
CREATE TABLE employment_contracts (
    id BIGSERIAL PRIMARY KEY,
    courier_id UUID NOT NULL REFERENCES users(id),
    organization_id BIGINT NOT NULL REFERENCES organizations(id),
    is_active BOOLEAN DEFAULT true,
    hired_at TIMESTAMP NOT NULL,
    terminated_at TIMESTAMP,
    -- outros campos...
);
```

#### Exemplo de Token JWT:
```json
{
  "role": "COURIER",
  "userId": "courier-uuid-123",
  "email": "joao@motoboy.com",
  "name": "João Silva",
  "authorities": ["ROLE_COURIER"]
  // ⚠️ Nota: NÃO contém organizationId
  // As organizações são buscadas via employment_contracts
}
```

#### Casos de Uso:
- Ver entregas disponíveis das organizações onde trabalha
- Aceitar/rejeitar entregas
- Atualizar status das entregas (coletada, em trânsito, entregue)
- Visualizar histórico das suas entregas
- Receber notificações de novas entregas

---

### 3. 🏢 ORGANIZER (Dono/Gerente da Organização)

**Descrição:** Proprietário ou gerente de uma empresa de entregas (organização).

#### Características:
- ✅ **POSSUI** `organization_id` obrigatório na tabela `users`
- ✅ **Vê apenas entregas** da sua organização (filtro de tenant)
- ✅ Gerencia COURIERs da sua organização via `employment_contracts`
- ✅ Gerencia CLIENTs da sua organização via `service_contracts`
- ✅ **Um ORGANIZER = Uma Organização** (relacionamento 1:1)

#### Regras de Banco de Dados:
```sql
-- Constraint no banco - ORGANIZER DEVE ter organization_id
ALTER TABLE users ADD CONSTRAINT chk_organizer_must_have_organization
CHECK (role != 'ORGANIZER' OR organization_id IS NOT NULL);
```

#### Exemplo de Token JWT:
```json
{
  "role": "ORGANIZER",
  "userId": "organizer-uuid-456",
  "email": "gerente@expressentregas.com",
  "name": "Maria Santos",
  "organizationId": 10,  // ✅ OBRIGATÓRIO
  "authorities": ["ROLE_ORGANIZER"]
}
```

#### Casos de Uso:
- Visualizar todas as entregas da sua organização
- Atribuir entregas aos COURIERs da sua equipe
- Contratar/demitir COURIERs (criar/desativar employment_contracts)
- Criar contratos com CLIENTs (service_contracts)
- Gerenciar configurações da organização
- Ver relatórios e métricas da sua organização

---

### 4. 📦 CLIENT (Cliente/Solicitante de Entregas)

**Descrição:** Pessoa ou empresa que solicita entregas.

#### Características:
- ❌ **NÃO possui** `organization_id` direto na tabela `users`
- ✅ **Obtém organizações** através da tabela `service_contracts` (contratos de serviço)
- ✅ Pode solicitar entregas de organizações com quem tem contrato
- ✅ Pode ter contratos com múltiplas organizações

#### Regras de Banco de Dados:
```sql
-- Constraint no banco
ALTER TABLE users ADD CONSTRAINT chk_client_no_organization
CHECK (role != 'CLIENT' OR organization_id IS NULL);

-- Relacionamento via service_contracts
-- Um CLIENT pode ter N contratos ativos
SELECT sc.organization_id, o.name
FROM service_contracts sc
JOIN organizations o ON o.id = sc.organization_id
WHERE sc.client_id = 'client-uuid-here'
  AND sc.is_active = true;
```

#### Modelo de Dados - Service Contract:
```sql
CREATE TABLE service_contracts (
    id BIGSERIAL PRIMARY KEY,
    client_id UUID NOT NULL REFERENCES users(id),
    organization_id BIGINT NOT NULL REFERENCES organizations(id),
    is_active BOOLEAN DEFAULT true,
    started_at TIMESTAMP NOT NULL,
    ended_at TIMESTAMP,
    -- outros campos...
);
```

#### Exemplo de Token JWT:
```json
{
  "role": "CLIENT",
  "userId": "client-uuid-789",
  "email": "contato@lojaxyz.com",
  "name": "Loja XYZ",
  "authorities": ["ROLE_CLIENT"]
  // ⚠️ Nota: NÃO contém organizationId
  // As organizações são buscadas via service_contracts
}
```

#### Casos de Uso:
- Solicitar novas entregas
- Ver histórico das suas entregas
- Avaliar entregas realizadas
- Gerenciar endereços de coleta/entrega
- Acompanhar status em tempo real

---

## 🔐 Constraints de Banco de Dados

Todas as regras são garantidas a nível de banco através da migração **V54**:

```sql
-- Migration: V54__add_organization_role_constraints.sql

-- 1. ADMIN não pode ter organization_id
ALTER TABLE users ADD CONSTRAINT chk_admin_no_organization
CHECK (role != 'ADMIN' OR organization_id IS NULL);

-- 2. COURIER não pode ter organization_id
ALTER TABLE users ADD CONSTRAINT chk_courier_no_organization
CHECK (role != 'COURIER' OR organization_id IS NULL);

-- 3. ORGANIZER DEVE ter organization_id
ALTER TABLE users ADD CONSTRAINT chk_organizer_must_have_organization
CHECK (role != 'ORGANIZER' OR organization_id IS NOT NULL);

-- 4. CLIENT não pode ter organization_id
ALTER TABLE users ADD CONSTRAINT chk_client_no_organization
CHECK (role != 'CLIENT' OR organization_id IS NULL);
```

---

## 📊 Diagrama de Relacionamentos

```
┌─────────────────────────────────────────────────────────────────┐
│                        ORGANIZATIONS                             │
│                  (Empresas de Entregas)                          │
└────────────────┬────────────────────────────────┬────────────────┘
                 │                                 │
                 │ 1:1                             │ 1:N
                 │                                 │
                 ▼                                 ▼
        ┌────────────────┐              ┌──────────────────────┐
        │   ORGANIZER    │              │  EMPLOYMENT_         │
        │                │              │  CONTRACTS           │
        │ - TEM orgId    │              │                      │
        │ - Gerencia a   │              │ - courier_id         │
        │   organização  │              │ - organization_id    │
        └────────────────┘              │ - is_active          │
                                        └──────────┬───────────┘
                                                   │ N:1
                                                   │
                                                   ▼
                                        ┌──────────────────────┐
                                        │     COURIER          │
                                        │                      │
                                        │ - NÃO tem orgId      │
                                        │ - Pode trabalhar     │
                                        │   para várias orgs   │
                                        └──────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                        ORGANIZATIONS                             │
└────────────────────────────────────────┬────────────────────────┘
                                         │ 1:N
                                         │
                                         ▼
                              ┌──────────────────────┐
                              │  SERVICE_            │
                              │  CONTRACTS           │
                              │                      │
                              │ - client_id          │
                              │ - organization_id    │
                              │ - is_active          │
                              └──────────┬───────────┘
                                         │ N:1
                                         │
                                         ▼
                              ┌──────────────────────┐
                              │      CLIENT          │
                              │                      │
                              │ - NÃO tem orgId      │
                              │ - Solicita entregas  │
                              └──────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                           ADMIN                                  │
│                                                                  │
│ - NÃO tem organization_id                                       │
│ - Acesso a TODAS as organizações                                │
│ - Gerencia o sistema globalmente                                │
└──────────────────────────────────────────────────────────────────┘
```

---

## 🔄 Fluxos de Negócio

### Fluxo 1: ORGANIZER contrata COURIER

```sql
-- 1. ORGANIZER cria contrato de trabalho
INSERT INTO employment_contracts (
    courier_id, 
    organization_id,  -- organizationId do ORGANIZER
    is_active,
    hired_at
) VALUES (
    'courier-uuid',
    10,  -- organização do ORGANIZER
    true,
    NOW()
);

-- 2. COURIER agora pode ver entregas desta organização
SELECT d.* 
FROM deliveries d
JOIN employment_contracts ec ON ec.organization_id = d.partnership_id
WHERE ec.courier_id = 'courier-uuid'
  AND ec.is_active = true;
```

### Fluxo 2: ORGANIZER cria contrato com CLIENT

```sql
-- 1. ORGANIZER cria contrato de serviço
INSERT INTO service_contracts (
    client_id,
    organization_id,  -- organizationId do ORGANIZER
    is_active,
    started_at
) VALUES (
    'client-uuid',
    10,  -- organização do ORGANIZER
    true,
    NOW()
);

-- 2. CLIENT pode solicitar entregas desta organização
INSERT INTO deliveries (
    client_id,
    partnership_id,  -- organization_id do contrato
    from_address,
    to_address,
    total_amount,
    status
) VALUES (
    'client-uuid',
    10,
    'Endereço origem',
    'Endereço destino',
    25.00,
    'PENDING'
);
```

### Fluxo 3: ADMIN visualiza tudo

```java
// No controller, para ADMIN:
if ("ADMIN".equals(role)) {
    // organizationId = null = SEM FILTRO
    deliveries = deliveryService.findAll(null, ...);
    // Retorna TODAS as entregas do sistema
}
```

---

## 🛡️ Validações no Código

### Controller (DeliveryController.java)

```java
@GetMapping
public Page<DeliveryResponse> list(...) {
    String role = jwtUtil.getRoleFromToken(token);
    
    if ("ADMIN".equals(role)) {
        // ADMIN: sem filtro
        deliveries = deliveryService.findAll(null, ...);
        
    } else if ("COURIER".equals(role)) {
        // COURIER: buscar via contratos
        UUID courierUserId = jwtUtil.getUserIdFromToken(token);
        deliveries = findDeliveriesForCourier(courierUserId, ...);
        
    } else if ("ORGANIZER".equals(role)) {
        // ORGANIZER: filtrar por sua organização
        Long organizationId = jwtUtil.getOrganizationIdFromToken(token);
        if (organizationId == null) {
            throw new RuntimeException("ORGANIZER deve ter organizationId");
        }
        deliveries = deliveryService.findAll(organizationId, ...);
        
    } else if ("CLIENT".equals(role)) {
        // CLIENT: buscar via contratos de serviço
        UUID clientUserId = jwtUtil.getUserIdFromToken(token);
        deliveries = findDeliveriesForClient(clientUserId, ...);
    }
    
    return deliveries.map(this::mapToResponse);
}
```

---

## 📝 Resumo das Regras

| Role      | organizationId | Como obtém organizações        | Acesso                     |
|-----------|----------------|--------------------------------|----------------------------|
| ADMIN     | ❌ NULL        | -                              | Tudo (sem filtro)          |
| COURIER   | ❌ NULL        | `employment_contracts`         | Entregas das orgs onde trabalha |
| ORGANIZER | ✅ OBRIGATÓRIO | Direto na tabela `users`       | Apenas sua organização     |
| CLIENT    | ❌ NULL        | `service_contracts`            | Entregas que solicitou     |

---

## 🚀 Implementação

### Arquivos Principais:

1. **Migração de Banco:** `V54__add_organization_role_constraints.sql`
2. **Controller:** `DeliveryController.java`
3. **Service:** `DeliveryService.java`
4. **Repositories:** 
   - `EmploymentContractRepository.java`
   - `ServiceContractRepository.java`

### Testes Recomendados:

```sql
-- Teste 1: Tentar inserir ADMIN com organization_id (deve falhar)
INSERT INTO users (username, password, role, organization_id)
VALUES ('admin@test.com', 'hash', 'ADMIN', 10);
-- ❌ ERROR: chk_admin_no_organization

-- Teste 2: Tentar inserir ORGANIZER sem organization_id (deve falhar)
INSERT INTO users (username, password, role, organization_id)
VALUES ('organizer@test.com', 'hash', 'ORGANIZER', NULL);
-- ❌ ERROR: chk_organizer_must_have_organization

-- Teste 3: Inserir ORGANIZER com organization_id (deve funcionar)
INSERT INTO users (username, password, role, organization_id)
VALUES ('organizer@test.com', 'hash', 'ORGANIZER', 10);
-- ✅ OK
```

---

## 📞 Contato e Suporte

Para dúvidas sobre o modelo de dados ou implementação:
- Documentação técnica: Este arquivo
- Código fonte: `/src/main/java/com/mvt/mvt_events/`
- Migrações: `/src/main/resources/db/migration/`

---

**Última atualização:** 2025-11-05  
**Versão:** 1.0  
**Sistema:** Zapi10 - Gerenciamento de Entregas
