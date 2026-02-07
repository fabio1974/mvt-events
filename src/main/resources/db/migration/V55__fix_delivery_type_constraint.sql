-- ============================================================================
-- V55: Corrigir constraint delivery_type e garantir dados válidos
-- ============================================================================

-- 1. Remover constraint existente se houver problema
ALTER TABLE deliveries DROP CONSTRAINT IF EXISTS chk_delivery_type;

-- 2. Garantir que todos os registros têm delivery_type válido
-- Converter qualquer valor NULL ou inválido para 'DELIVERY'
UPDATE deliveries 
SET delivery_type = 'DELIVERY'
WHERE delivery_type IS NULL 
   OR delivery_type NOT IN ('DELIVERY', 'RIDE', 'CONTRACT');

-- 3. Recriar constraint com validação correta
ALTER TABLE deliveries 
ADD CONSTRAINT chk_delivery_type 
CHECK (delivery_type IN ('DELIVERY', 'RIDE', 'CONTRACT'));

-- 4. Confirmar que coluna não aceita NULL
ALTER TABLE deliveries 
ALTER COLUMN delivery_type SET NOT NULL;

-- 5. Verificar índice
CREATE INDEX IF NOT EXISTS idx_deliveries_type_status 
ON deliveries(delivery_type, status);
