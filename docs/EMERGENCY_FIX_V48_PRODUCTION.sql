-- ============================================================================
-- CORREÇÃO EMERGENCIAL PARA PRODUÇÃO
-- Execute este script MANUALMENTE no banco de produção
-- ============================================================================

-- 1. Marcar V48 como falha no histórico (se ainda não está)
-- Isso permite que ela seja executada novamente após correção
UPDATE flyway_schema_history 
SET success = false 
WHERE version = '48' AND success = true;

-- 2. Remover constraint problemática se existir
ALTER TABLE deliveries DROP CONSTRAINT IF EXISTS chk_delivery_type;

-- 3. Adicionar coluna se não existir (sem constraint)
ALTER TABLE deliveries 
ADD COLUMN IF NOT EXISTS delivery_type VARCHAR(20);

-- 4. Adicionar colunas de pagamento se não existirem
ALTER TABLE deliveries 
ADD COLUMN IF NOT EXISTS payment_completed BOOLEAN;

ALTER TABLE deliveries 
ADD COLUMN IF NOT EXISTS payment_captured BOOLEAN;

-- 5. Atualizar TODOS os registros existentes
UPDATE deliveries 
SET delivery_type = 'DELIVERY'
WHERE delivery_type IS NULL OR delivery_type = '';

UPDATE deliveries 
SET payment_completed = CASE 
    WHEN status IN ('ACCEPTED', 'IN_TRANSIT', 'COMPLETED') THEN true 
    ELSE false 
END
WHERE payment_completed IS NULL;

UPDATE deliveries 
SET payment_captured = CASE 
    WHEN status IN ('ACCEPTED', 'IN_TRANSIT', 'COMPLETED') THEN true 
    ELSE false 
END
WHERE payment_captured IS NULL;

-- 6. Definir NOT NULL e DEFAULT
ALTER TABLE deliveries 
ALTER COLUMN delivery_type SET NOT NULL;

ALTER TABLE deliveries 
ALTER COLUMN delivery_type SET DEFAULT 'DELIVERY';

ALTER TABLE deliveries 
ALTER COLUMN payment_completed SET NOT NULL;

ALTER TABLE deliveries 
ALTER COLUMN payment_completed SET DEFAULT false;

ALTER TABLE deliveries 
ALTER COLUMN payment_captured SET NOT NULL;

ALTER TABLE deliveries 
ALTER COLUMN payment_captured SET DEFAULT false;

-- 7. Agora sim, adicionar constraint
ALTER TABLE deliveries 
ADD CONSTRAINT chk_delivery_type 
CHECK (delivery_type IN ('DELIVERY', 'RIDE', 'CONTRACT'));

-- 8. Criar índices
CREATE INDEX IF NOT EXISTS idx_deliveries_type_status 
ON deliveries(delivery_type, status);

CREATE INDEX IF NOT EXISTS idx_deliveries_payment_status 
ON deliveries(payment_completed, payment_captured);

-- 9. Remover entrada falha da V48 do histórico
DELETE FROM flyway_schema_history WHERE version = '48' AND success = false;

-- 10. Marcar V48 como executada com sucesso
INSERT INTO flyway_schema_history 
(installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success)
SELECT 
    (SELECT COALESCE(MAX(installed_rank), 0) + 1 FROM flyway_schema_history),
    '48',
    'add delivery type and payment fields',
    'SQL',
    'V48__add_delivery_type_and_payment_fields.sql',
    NULL,
    current_user,
    NOW(),
    0,
    true
WHERE NOT EXISTS (
    SELECT 1 FROM flyway_schema_history WHERE version = '48' AND success = true
);

-- 11. Verificar resultado
SELECT version, description, installed_on, success 
FROM flyway_schema_history 
WHERE version IN ('48', '55')
ORDER BY installed_rank;

-- 12. Verificar dados
SELECT delivery_type, COUNT(*) 
FROM deliveries 
GROUP BY delivery_type;

-- ============================================================================
-- Após executar este script, reinicie a aplicação
-- A V55 será pulada automaticamente pois os dados já estarão corretos
-- ============================================================================
