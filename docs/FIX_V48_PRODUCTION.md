# 🚨 Correção de Erro em Produção - Migration V48

## ❌ Problema

Migration V48 está falhando em produção com erro:
```
ERROR: new row for relation "deliveries" violates check constraint "chk_delivery_type"
```

**Causa:** A constraint `chk_delivery_type` foi criada antes de atualizar os dados existentes, causando violação.

---

## ✅ Solução Implementada

### Arquivos Corrigidos

1. **V48__add_delivery_type_and_payment_fields.sql** (corrigida)
   - Reorganizada para: adicionar coluna → atualizar dados → aplicar constraint
   - Adicionado UPDATE final para garantir nenhum NULL

2. **V55__fix_delivery_type_constraint.sql** (NOVA)
   - Migration de correção para ambientes que já falharam
   - Remove constraint → limpa dados → recria constraint

---

## 📋 Passos para Corrigir Produção

### Opção 1: Deploy com V55 (RECOMENDADO)

Se a aplicação não está rodando:

```bash
# 1. Pull do código corrigido
git pull origin main

# 2. Build
./gradlew build

# 3. Deploy
# A V55 será executada automaticamente e corrigirá o problema
```

A migration V55 fará:
1. ✅ Remover constraint problemática
2. ✅ Garantir todos os registros têm `delivery_type = 'DELIVERY'`
3. ✅ Recriar constraint corretamente
4. ✅ Definir NOT NULL

---

### Opção 2: Correção Manual no Banco (SE URGENTE)

Se precisar corrigir AGORA sem deploy:

```sql
-- 1. Conectar ao banco de produção
psql -h SEU_HOST -U SEU_USER -d mvt-events

-- 2. Remover constraint problemática
ALTER TABLE deliveries DROP CONSTRAINT IF EXISTS chk_delivery_type;

-- 3. Garantir dados válidos
UPDATE deliveries 
SET delivery_type = 'DELIVERY'
WHERE delivery_type IS NULL 
   OR delivery_type NOT IN ('DELIVERY', 'RIDE', 'CONTRACT');

-- 4. Recriar constraint
ALTER TABLE deliveries 
ADD CONSTRAINT chk_delivery_type 
CHECK (delivery_type IN ('DELIVERY', 'RIDE', 'CONTRACT'));

-- 5. Garantir NOT NULL
ALTER TABLE deliveries 
ALTER COLUMN delivery_type SET NOT NULL;

-- 6. Marcar V48 como executada no Flyway
-- (se ainda não está marcada)
INSERT INTO flyway_schema_history 
(installed_rank, version, description, type, script, checksum, installed_by, execution_time, success)
SELECT 
    (SELECT COALESCE(MAX(installed_rank), 0) + 1 FROM flyway_schema_history),
    '48',
    'add delivery type and payment fields',
    'SQL',
    'V48__add_delivery_type_and_payment_fields.sql',
    NULL,
    current_user,
    0,
    true
WHERE NOT EXISTS (
    SELECT 1 FROM flyway_schema_history WHERE version = '48'
);

-- 7. Verificar
SELECT version, description, installed_on, success 
FROM flyway_schema_history 
WHERE version IN ('48', '55')
ORDER BY installed_rank;
```

Depois faça o deploy normalmente (a V55 será pulada pois os dados já estarão corretos).

---

## 🔍 Verificação

Após correção, verificar:

```sql
-- 1. Ver constraint
SELECT constraint_name, check_clause 
FROM information_schema.check_constraints 
WHERE constraint_name = 'chk_delivery_type';

-- 2. Verificar dados
SELECT delivery_type, COUNT(*) 
FROM deliveries 
GROUP BY delivery_type;

-- 3. Verificar NULLs (não deve retornar nada)
SELECT id, delivery_type 
FROM deliveries 
WHERE delivery_type IS NULL;

-- 4. Ver migrations aplicadas
SELECT version, description, installed_on, success 
FROM flyway_schema_history 
WHERE version >= '48'
ORDER BY installed_rank;
```

---

## 🎯 Resultado Esperado

Após correção:
- ✅ Constraint `chk_delivery_type` criada corretamente
- ✅ Todas entregas com `delivery_type IN ('DELIVERY', 'RIDE', 'CONTRACT')`
- ✅ Nenhum valor NULL
- ✅ V48 marcada como success no flyway_schema_history
- ✅ V55 aplicada (se fez deploy) ou dados corrigidos manualmente

---

## 📊 Status das Migrations

Versões envolvidas:
- **V48**: Adiciona delivery_type, payment_completed, payment_captured (CORRIGIDA)
- **V55**: Correção para ambientes que falharam na V48 (NOVA)

---

## 🚀 Deploy Seguro

Para evitar problemas futuros em novos ambientes:

1. ✅ Sempre testar migrations em staging primeiro
2. ✅ Fazer backup antes de deploy em produção
3. ✅ Verificar flyway_schema_history após deploy
4. ✅ Monitorar logs durante startup

---

## 💡 Lições Aprendidas

**Ordem correta para migrations com constraints:**
1. Adicionar coluna (sem constraint)
2. Popular/atualizar dados
3. Adicionar constraint
4. Definir NOT NULL/DEFAULT

**Evitar:**
- ❌ Adicionar coluna com NOT NULL e constraint juntos
- ❌ Criar constraint antes de popular dados
- ❌ Assumir que DEFAULT funciona antes de constraint
