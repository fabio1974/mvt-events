# 🚀 Como Executar as Migrations

## Opção 1: Usar o script automatizado

```bash
cd /Users/jose.barros.br/Documents/projects/mvt-events
./run-migrations.sh
```

Esse script irá:

- ✅ Iniciar a aplicação
- ✅ Filtrar apenas logs relevantes das migrations
- ✅ Mostrar progresso das migrations V40 e V41

## Opção 2: Executar manualmente

```bash
cd /Users/jose.barros.br/Documents/projects/mvt-events
./gradlew bootRun
```

## O que vai acontecer?

### Migration V40

```sql
✅ Criar tabela employment_contracts
✅ Criar tabela contracts
✅ Criar trigger check_primary_contract()
✅ Criar índices para performance
```

### Migration V41

```sql
✅ Migrar courier_organizations → employment_contracts (se existir)
✅ Migrar courier_adm_links → employment_contracts (se existir)
✅ Migrar client_manager_links → contracts (se existir)
✅ Remover tabelas antigas
✅ Validar contratos primários
```

## Como verificar se funcionou?

### 1. Verificar logs da aplicação

Procure por:

```
Migrating schema `public` to version "40"
Migrating schema `public` to version "41"
Successfully applied 2 migrations
```

### 2. Conectar ao banco e verificar

```bash
psql -U postgres -d mvt_events
```

```sql
-- Verificar tabelas criadas
\dt employment_contracts
\dt contracts

-- Verificar dados
SELECT COUNT(*) as total_employment FROM employment_contracts;
SELECT COUNT(*) as total_contracts FROM contracts;
SELECT COUNT(*) as total_primary FROM contracts WHERE is_primary = TRUE;

-- Verificar trigger
SELECT trigger_name, event_manipulation
FROM information_schema.triggers
WHERE trigger_name = 'enforce_single_primary_contract';

-- Ver estrutura das tabelas
\d employment_contracts
\d contracts
```

## Próximos passos após migrations

1. ✅ Verificar se as tabelas foram criadas
2. ✅ Verificar se os dados foram migrados (se houver)
3. ✅ Testar trigger de is_primary
4. ⏳ Criar Repositories
5. ⏳ Criar Services
6. ⏳ Criar Controllers
7. ⏳ Testar APIs

## Troubleshooting

### Erro: "Migration checksum mismatch"

```bash
# Limpar histórico do Flyway e recriar
psql -U postgres -d mvt_events -c "DELETE FROM flyway_schema_history WHERE version IN ('40', '41');"
./gradlew bootRun
```

### Erro: "Table already exists"

```bash
# As migrations já foram executadas anteriormente
# Verificar no banco:
psql -U postgres -d mvt_events -c "SELECT * FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 5;"
```

### Ver logs completos

```bash
./gradlew bootRun > full.log 2>&1
tail -f full.log
```

## Status Atual

- ✅ Código refatorado
- ✅ Migrations criadas (V40, V41)
- ✅ Script de execução pronto
- ⏳ **PRÓXIMO PASSO: Executar `./run-migrations.sh`**
