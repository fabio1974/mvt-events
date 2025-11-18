# Permissões para Criar Entregas

## 📋 Resumo das Permissões

| Role | Pode Criar Entrega? | Restrições |
|------|---------------------|------------|
| **ADMIN** | ✅ SIM | Pode criar para **qualquer cliente** |
| **CLIENT** | ✅ SIM | Pode criar **apenas para si mesmo** |
| **ORGANIZER** | ❌ NÃO | Apenas **gerencia** entregas (assign, cancel, list) |
| **COURIER** | ❌ NÃO | Apenas **executa** entregas (pickup, transit, complete) |

---

## 🔐 Validações Implementadas

### 1. ADMIN pode criar para qualquer cliente

```java
if (creatorRole == User.Role.ADMIN) {
    // ADMIN pode criar entregas para qualquer cliente (sem restrições)
}
```

**Exemplo:**
```bash
# ADMIN cria entrega para CLIENT uuid: 189c7d79-cb21-40c1-9b7c-006ebaa3289a
curl -X POST http://localhost:8080/api/deliveries \
  -H "Authorization: Bearer <ADMIN_TOKEN>" \
  -d '{"client": {"id": "189c7d79-cb21-40c1-9b7c-006ebaa3289a"}, ...}'
```

✅ **Permitido** - ADMIN tem acesso total

---

### 2. CLIENT só pode criar para si mesmo

```java
if (creatorRole == User.Role.CLIENT) {
    // CLIENT só pode criar entregas para si mesmo
    if (!creator.getId().equals(client.getId())) {
        throw new RuntimeException("CLIENT só pode criar entregas para si mesmo");
    }
}
```

**Exemplo válido:**
```bash
# CLIENT cria entrega para si mesmo
curl -X POST http://localhost:8080/api/deliveries \
  -H "Authorization: Bearer <CLIENT_TOKEN>" \  # userId: abc-123
  -d '{"client": {"id": "abc-123"}, ...}'      # mesmo userId
```

✅ **Permitido**

**Exemplo inválido:**
```bash
# CLIENT tenta criar entrega para outro cliente
curl -X POST http://localhost:8080/api/deliveries \
  -H "Authorization: Bearer <CLIENT_TOKEN>" \  # userId: abc-123
  -d '{"client": {"id": "xyz-999"}, ...}'      # userId diferente
```

❌ **Erro:** "CLIENT só pode criar entregas para si mesmo"

---

### 3. ORGANIZER NÃO pode criar entregas

```java
if (creatorRole == User.Role.ORGANIZER) {
    // ORGANIZER não pode criar entregas, apenas gerenciar
    throw new RuntimeException("ORGANIZER não pode criar entregas, apenas gerenciar as existentes");
}
```

**Exemplo:**
```bash
# ORGANIZER tenta criar entrega
curl -X POST http://localhost:8080/api/deliveries \
  -H "Authorization: Bearer <ORGANIZER_TOKEN>" \
  -d '{"client": {...}, ...}'
```

❌ **Erro:** "ORGANIZER não pode criar entregas, apenas gerenciar as existentes"

**Justificativa:**
- ORGANIZER é responsável por **gerenciar** (atribuir, cancelar, listar)
- Entregas são criadas pelos próprios CLIENTs ou pelo ADMIN
- Evita conflito de responsabilidades

---

### 4. COURIER NÃO pode criar entregas

```java
if (creatorRole == User.Role.COURIER) {
    // COURIER não pode criar entregas
    throw new RuntimeException("COURIER não pode criar entregas");
}
```

**Exemplo:**
```bash
# COURIER tenta criar entrega
curl -X POST http://localhost:8080/api/deliveries \
  -H "Authorization: Bearer <COURIER_TOKEN>" \
  -d '{"client": {...}, ...}'
```

❌ **Erro:** "COURIER não pode criar entregas"

**Justificativa:**
- COURIER é responsável por **executar** entregas (pickup, transit, complete)
- Não faz sentido criar entregas para entregar

---

## 🎯 Validação do Destinatário

**Regra:** O campo `client` no request **DEVE** ser um usuário com role `CLIENT`.

```java
if (client.getRole() != User.Role.CLIENT) {
    throw new RuntimeException("O destinatário da entrega deve ser um CLIENT (role atual: " + client.getRole() + ")");
}
```

**Exemplo inválido:**
```bash
# Tentar criar entrega com destinatário COURIER
curl -X POST http://localhost:8080/api/deliveries \
  -H "Authorization: Bearer <ADMIN_TOKEN>" \
  -d '{"client": {"id": "courier-uuid-123"}, ...}'  # Este UUID é um COURIER
```

❌ **Erro:** "O destinatário da entrega deve ser um CLIENT (role atual: COURIER)"

---

## 📊 Matriz de Permissões Completa

| Ação | ADMIN | CLIENT | ORGANIZER | COURIER |
|------|-------|--------|-----------|---------|
| **Criar entrega** | ✅ Qualquer cliente | ✅ Si mesmo | ❌ | ❌ |
| **Listar entregas** | ✅ Todas | ✅ Suas | ✅ Da org | ✅ Disponíveis |
| **Ver detalhes** | ✅ Todas | ✅ Suas | ✅ Da org | ✅ Atribuídas |
| **Atribuir courier** | ✅ | ❌ | ✅ | ❌ |
| **Cancelar** | ✅ | ✅ Suas | ✅ Da org | ❌ |
| **Aceitar** | ❌ | ❌ | ❌ | ✅ |
| **Pickup** | ❌ | ❌ | ❌ | ✅ |
| **Complete** | ❌ | ❌ | ❌ | ✅ |

---

## 🔍 Fluxo Típico

### Cenário 1: Cliente Solicita Entrega

```
1. CLIENT faz login
2. CLIENT cria entrega (POST /api/deliveries)
   → client.id = seu próprio UUID ✅
3. Sistema valida: creatorId == clientId ✅
4. Entrega criada com status PENDING
5. Notificação enviada para COURIERs disponíveis
```

### Cenário 2: Admin Cria Entrega para Cliente

```
1. ADMIN faz login
2. ADMIN seleciona cliente (qualquer CLIENT)
3. ADMIN cria entrega (POST /api/deliveries)
   → client.id = UUID do cliente escolhido ✅
4. Sistema valida: role == ADMIN ✅
5. Entrega criada com status PENDING
6. Notificação enviada para COURIERs disponíveis
```

### Cenário 3: Organizer Tenta Criar (BLOQUEADO)

```
1. ORGANIZER faz login
2. ORGANIZER tenta criar entrega (POST /api/deliveries)
3. Sistema valida: role == ORGANIZER ❌
4. Erro: "ORGANIZER não pode criar entregas, apenas gerenciar as existentes"
5. Request rejeitado com HTTP 500
```

---

## 🧪 Testes de Validação

### Teste 1: ADMIN cria para qualquer cliente ✅

```bash
curl -X POST http://localhost:8080/api/deliveries \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..." \  # ADMIN token
  -H "Content-Type: application/json" \
  -d '{
    "client": {"id": "189c7d79-cb21-40c1-9b7c-006ebaa3289a"},
    "fromAddress": "...",
    "toAddress": "...",
    "totalAmount": "123"
  }'
```

**Resultado esperado:** HTTP 201 Created

### Teste 2: CLIENT cria para si mesmo ✅

```bash
curl -X POST http://localhost:8080/api/deliveries \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..." \  # CLIENT token (userId: abc-123)
  -H "Content-Type: application/json" \
  -d '{
    "client": {"id": "abc-123"},  # Mesmo userId do token
    "fromAddress": "...",
    "toAddress": "...",
    "totalAmount": "100"
  }'
```

**Resultado esperado:** HTTP 201 Created

### Teste 3: CLIENT tenta criar para outro ❌

```bash
curl -X POST http://localhost:8080/api/deliveries \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..." \  # CLIENT token (userId: abc-123)
  -H "Content-Type: application/json" \
  -d '{
    "client": {"id": "xyz-999"},  # Outro userId
    "fromAddress": "...",
    "toAddress": "...",
    "totalAmount": "100"
  }'
```

**Resultado esperado:** HTTP 500 - "CLIENT só pode criar entregas para si mesmo"

### Teste 4: ORGANIZER tenta criar ❌

```bash
curl -X POST http://localhost:8080/api/deliveries \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..." \  # ORGANIZER token
  -H "Content-Type: application/json" \
  -d '{
    "client": {"id": "abc-123"},
    "fromAddress": "...",
    "toAddress": "...",
    "totalAmount": "100"
  }'
```

**Resultado esperado:** HTTP 500 - "ORGANIZER não pode criar entregas, apenas gerenciar as existentes"

---

## 📝 Changelog

### v1.0 (2025-11-05)
- ✅ Implementado validação de roles ao criar entregas
- ✅ ADMIN pode criar para qualquer cliente
- ✅ CLIENT só pode criar para si mesmo
- ❌ ORGANIZER bloqueado de criar entregas
- ❌ COURIER bloqueado de criar entregas
- ✅ Validação de destinatário (deve ser CLIENT)

---

## 🔗 Referências

- **Código:** `DeliveryService.java` → método `create()`
- **Documentação de Roles:** `ROLES_E_ORGANIZACOES.md`
- **Entregas On-Demand:** `ENTREGAS_ON_DEMAND.md`
