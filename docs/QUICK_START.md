# 🚀 QUICK START - Iniciar Sistema AGORA

**Tempo estimado:** 2 minutos

---

## ✅ PASSO 1: Dar Permissão

```bash
chmod +x start-complete.sh
```

---

## ✅ PASSO 2: Iniciar

```bash
./start-complete.sh
```

**O que acontece:**

1. 🧹 Limpa build anterior
2. 🔨 Compila código
3. 📊 Executa Migration V44 (cria tabela payments)
4. 🚀 Inicia aplicação na porta 8080

---

## ✅ PASSO 3: Verificar

```bash
# Health check
curl http://localhost:8080/actuator/health
```

**Resposta esperada:**

```json
{ "status": "UP" }
```

---

## ✅ PASSO 4: Acessar Swagger

Abra no navegador:

```
http://localhost:8080/swagger-ui.html
```

---

## 🎯 ENDPOINTS PRINCIPAIS

### Payments

- `POST /api/payments` - Criar pagamento
- `GET /api/payments/{id}` - Buscar pagamento
- `GET /api/payments/delivery/{deliveryId}` - Pagamentos de uma entrega
- `PATCH /api/payments/{id}/complete` - Marcar como pago
- `PATCH /api/payments/{id}/refund` - Reembolsar

### Deliveries

- `POST /api/deliveries` - Criar entrega
- `GET /api/deliveries` - Listar entregas
- `GET /api/deliveries/{id}` - Buscar entrega
- `PATCH /api/deliveries/{id}/status` - Atualizar status

### Payouts

- `POST /api/unified-payouts` - Criar payout
- `GET /api/unified-payouts` - Listar payouts
- `GET /api/unified-payouts/{id}` - Buscar payout

---

## 📊 BANCO DE DADOS

**Conexão:**

```
Host: localhost
Port: 5435
Database: mvt-events
User: postgres
Password: postgres
```

**Versão atual:** 44

**Tabelas principais:**

- `payments` ← NOVA
- `deliveries`
- `payout_items`
- `unified_payouts`
- `employment_contracts` ← NOVA
- `contracts` ← NOVA

---

## 🔍 VERIFICAÇÕES

### 1. Aplicação rodando?

```bash
curl http://localhost:8080/actuator/health
```

### 2. Migrations aplicadas?

```bash
psql -h localhost -p 5435 -U postgres -d mvt-events -c "SELECT version FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 5;"
```

### 3. Tabela payments existe?

```bash
psql -h localhost -p 5435 -U postgres -d mvt-events -c "\d payments"
```

---

## ❓ PROBLEMAS?

### Erro de compilação

```bash
./gradlew clean build -x test
```

### Porta 8080 ocupada

```bash
# Matar processo
lsof -ti:8080 | xargs kill -9

# Ou usar outra porta
./gradlew bootRun --args='--server.port=8081'
```

### Banco não conecta

```bash
# Verificar se Docker está rodando
docker ps | grep postgres

# Subir banco
docker-compose up -d postgres
```

---

## 📚 DOCUMENTAÇÃO COMPLETA

Veja `PAYMENT_SYSTEM_COMPLETE.md` para:

- Arquitetura completa
- Fluxos de negócio
- Exemplos de uso
- Próximos passos

---

**RESUMO:** Execute `./start-complete.sh` e pronto! 🎉
