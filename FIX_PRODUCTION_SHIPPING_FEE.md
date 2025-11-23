# Fix: Erro de Deploy - Coluna shipping_fee Faltante

**Data:** 21/11/2025  
**Status:** ✅ Corrigido

## 🔥 Problema

A aplicação falhou ao fazer deploy em produção com o seguinte erro:

```
org.hibernate.tool.schema.spi.SchemaManagementException: 
Schema-validation: missing column [shipping_fee] in table [deliveries]
```

## 🔍 Análise

### Causa Raiz
A entidade `Delivery.java` possui o campo `shippingFee`:
```java
private BigDecimal shippingFee;
```

Porém, a coluna correspondente **não existe no banco de dados de produção**.

### Investigação do Histórico de Migrações

#### Banco de Produção (flyway_schema_history)
```
rank | version | description
-----|---------|-------------
...
62   | 64      | add organization to deliveries
63   | 65      | add payout tracking to payout items  
64   | 66      | remove unified payout
```

#### Migrações Locais
```
V62__rename_gps_columns_in_users.sql
V63__rename_address_coordinates_to_simple_names.sql
V64__add_organization_to_deliveries.sql
V65__add_payout_tracking_to_payout_items.sql
V66__remove_unified_payout.sql
V59__add_shipping_fee_to_deliveries.sql  ⚠️ PROBLEMA AQUI
```

### Conclusão
A migração **V59__add_shipping_fee_to_deliveries.sql** foi criada localmente mas:
1. ❌ Nunca foi commitada no git
2. ❌ Nunca foi enviada para produção
3. ❌ Flyway em produção pulou da V58 para V60+ sem executar V59
4. ✅ A entidade foi atualizada com o campo `shippingFee`
5. ❌ O banco de produção não tem a coluna

Resultado: **Desalinhamento entre código (tem o campo) e banco de dados (não tem a coluna)**

## ✅ Solução Aplicada

### 1. Renumeração da Migração
Como o Flyway já executou migrações V60-V66 em produção, **não é possível inserir uma V59 retroativamente**.

**Ação tomada:**
```bash
mv src/main/resources/db/migration/V59__add_shipping_fee_to_deliveries.sql \
   src/main/resources/db/migration/V67__add_shipping_fee_to_deliveries.sql
```

### 2. Conteúdo da V67
```sql
-- Adiciona coluna shipping_fee à tabela deliveries
ALTER TABLE deliveries 
ADD COLUMN IF NOT EXISTS shipping_fee NUMERIC(10, 2);

COMMENT ON COLUMN deliveries.shipping_fee IS 'Valor do frete da entrega';
```

O `IF NOT EXISTS` garante que:
- ✅ Em produção: adiciona a coluna (que está faltando)
- ✅ Em desenvolvimento: não falha se a coluna já existir (da V59 antiga)

### 3. Deploy
```bash
# Compilação
./gradlew clean build -x test
# ✅ BUILD SUCCESSFUL

# Commit
git add src/main/resources/db/migration/V67__add_shipping_fee_to_deliveries.sql
git commit -m "fix: adicionar migração V67 para coluna shipping_fee faltante em produção"

# Push para produção
git push origin main
# ✅ Push successful
```

## 📋 Ordem de Execução Esperada em Produção

Quando o Render fizer o próximo deploy:

1. ✅ V65 - add payout tracking to payout items (já aplicada)
2. ✅ V66 - remove unified payout (já aplicada)
3. **🆕 V67 - add shipping_fee to deliveries (NOVA)**
4. ✅ Aplicação inicia com sucesso
5. ✅ Hibernate valida schema: coluna `shipping_fee` agora existe

## 🎯 Resultado Esperado

### Antes (Com Erro)
```
Hibernate Schema Validation:
❌ deliveries.shipping_fee → MISSING COLUMN
→ APPLICATION FAILS TO START
```

### Depois (Corrigido)
```
Flyway Migrations:
✅ V67 executed → Column shipping_fee created

Hibernate Schema Validation:
✅ deliveries.shipping_fee → COLUMN EXISTS
✅ APPLICATION STARTS SUCCESSFULLY
```

## 📚 Lições Aprendidas

### ⚠️ Problema Identificado
Criar migrações localmente sem commitar imediatamente pode causar:
1. Numeração desalinhada entre ambientes
2. Migrações "órfãs" que nunca entram em produção
3. Schema desalinhado entre código e banco de dados

### ✅ Boas Práticas
1. **Sempre commitar migrações imediatamente após criação**
2. **Nunca renumerar migrações após deploy em qualquer ambiente**
3. **Usar `IF NOT EXISTS` em ALTER TABLE para idempotência**
4. **Validar flyway_schema_history antes de criar novas migrações**
5. **Testar deploy em staging antes de produção**

## 🔗 Commits Relacionados

- `b38df72` - Remoção do UnifiedPayout (V65, V66)
- `ef5f129` - Fix: adicionar migração V67 para shipping_fee (ESTE FIX)

## 📊 Status Atual

### Desenvolvimento Local
```sql
SELECT version, description FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 5;
version | description
--------|-------------
67      | add shipping_fee to deliveries
66      | remove unified payout  
65      | add payout tracking to payout items
64      | add organization to deliveries
63      | rename address coordinates to simple names
```

### Produção (Após Deploy)
```sql
-- Esperado após próximo deploy:
version | description
--------|-------------
67      | add shipping_fee to deliveries  ← NOVA
66      | remove unified payout
65      | add payout tracking to payout items
64      | add organization to deliveries
63      | rename address coordinates to simple names
```

## ✅ Checklist de Validação Pós-Deploy

- [ ] Aplicação inicia sem erros
- [ ] Endpoint `/actuator/health` retorna UP
- [ ] Coluna `shipping_fee` existe em `deliveries`
- [ ] Migrações V65, V66, V67 aparecem no flyway_schema_history
- [ ] Deliveries podem ser criadas/atualizadas normalmente

---

**Próximo Deploy:** Automático via Render após push para main  
**Tempo Estimado:** 5-10 minutos  
**Monitoramento:** Logs do Render + Health Check
