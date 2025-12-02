# 🚀 Plano de Deploy - Migrações V1 e V2

## 📋 Pré-requisitos (Você fará manualmente)

### 1️⃣ Backup do banco de produção
```sql
-- Criar backup do schema público atual (se necessário)
CREATE SCHEMA IF NOT EXISTS backup_production_YYYYMMDD_HHMMSS;

-- Copiar todas as tabelas
CREATE TABLE backup_production_YYYYMMDD_HHMMSS.users AS SELECT * FROM public.users;
CREATE TABLE backup_production_YYYYMMDD_HHMMSS.organizations AS SELECT * FROM public.organizations;
-- ... (repetir para todas as tabelas)
```

### 2️⃣ Deletar schema public
```sql
-- ATENÇÃO: Isso apaga TODOS os dados!
DROP SCHEMA public CASCADE;
```

### 3️⃣ Recriar schema public vazio
```sql
CREATE SCHEMA public;
GRANT ALL ON SCHEMA public TO postgres;
GRANT ALL ON SCHEMA public TO public;
```

## 🎯 Deploy via Git (Copilot fará)

Após você recriar o schema público, me avise e eu farei:

### ✅ Arquivos prontos para deploy:
- ✓ `V1__baseline_initial_schema.sql` - Schema completo (DDL)
- ✓ `V2__initial_test_data.sql` - Dados de teste anonimizados
- ✓ `application.properties` - Configuração do Flyway
- ✓ `build.gradle` - Dependências corretas

### 🔄 Processo de deploy que executarei:

1. **Verificar status do Git**
   ```bash
   git status
   git diff
   ```

2. **Commit das migrações**
   ```bash
   git add src/main/resources/db/migration/V1__baseline_initial_schema.sql
   git add src/main/resources/db/migration/V2__initial_test_data.sql
   git add src/main/resources/application.properties
   git commit -m "feat: Add Flyway V1 (baseline schema) and V2 (test data) migrations"
   ```

3. **Push para o repositório**
   ```bash
   git push origin main
   ```

4. **Deploy na produção**
   - Pull do código no servidor
   - Executar build
   - Flyway aplicará V1 e V2 automaticamente
   - Aplicação startará com dados limpos

## 📊 Resultado esperado após deploy:

### Estrutura (V1):
- ✓ 15 tabelas criadas
- ✓ Todos os índices
- ✓ Todas as constraints
- ✓ Todos os triggers

### Dados (V2):
- ✓ 9 usuários de teste (senha: 123456)
  - 1 ADMIN
  - 2 ORGANIZERs
  - 4 CLIENTs
  - 2 COURIERs
- ✓ 2 organizações
- ✓ 5 client_contracts
- ✓ 4 employment_contracts
- ✓ 8 deliveries em diferentes status
- ✓ 5,570 cidades (carregadas no startup)

## 🔐 Credenciais de teste (V2):

| Username | Senha | Role |
|----------|-------|------|
| admin@mvt.com | 123456 | ADMIN |
| organizer1@mvt.com | 123456 | ORGANIZER |
| organizer2@mvt.com | 123456 | ORGANIZER |
| client1@mvt.com | 123456 | CLIENT |
| client2@mvt.com | 123456 | CLIENT |
| client3@mvt.com | 123456 | CLIENT |
| client4@mvt.com | 123456 | CLIENT |
| courier1@mvt.com | 123456 | COURIER |
| courier2@mvt.com | 123456 | COURIER |

## ⚠️ IMPORTANTE:

1. **Backup antes de tudo**: Certifique-se de ter backup dos dados de produção
2. **Schema vazio**: O public deve estar completamente vazio antes do deploy
3. **Flyway limpo**: A tabela `flyway_schema_history` será criada do zero
4. **Dados de teste**: V2 contém dados FICTÍCIOS, não de produção

## 🆘 Rollback (se necessário):

Se algo der errado:
```sql
-- Deletar public novamente
DROP SCHEMA public CASCADE;

-- Recriar
CREATE SCHEMA public;

-- Restaurar do backup
CREATE TABLE public.users AS SELECT * FROM backup_production_YYYYMMDD_HHMMSS.users;
-- ... (repetir para todas as tabelas)
```

---

## 📞 Quando estiver pronto:

**Me avise assim que:**
1. Deletar o schema public de produção
2. Recriar o schema public vazio
3. Confirmar que está pronto para o deploy

**Então eu farei:**
- Commit das migrações
- Push para o Git
- Instruções para deploy no servidor

---
**Data de criação**: 02/12/2025  
**Status**: Aguardando recriação manual do schema público
