-- ============================================================================
-- V48: Adicionar delivery_type e campos de controle de pagamento
-- ============================================================================

-- 1. Adicionar coluna delivery_type (se não existir)
ALTER TABLE deliveries 
ADD COLUMN IF NOT EXISTS delivery_type VARCHAR(20) NOT NULL DEFAULT 'DELIVERY';

-- 2. Adicionar coluna payment_completed (se não existir)
ALTER TABLE deliveries 
ADD COLUMN IF NOT EXISTS payment_completed BOOLEAN NOT NULL DEFAULT false;

-- 3. Adicionar coluna payment_captured (se não existir)
ALTER TABLE deliveries 
ADD COLUMN IF NOT EXISTS payment_captured BOOLEAN NOT NULL DEFAULT false;

-- 4. Atualizar entregas existentes
-- Entregas já aceitas/completadas são marcadas como DELIVERY e pagas
UPDATE deliveries 
SET delivery_type = 'DELIVERY',
    payment_completed = true, 
    payment_captured = true
WHERE status IN ('ACCEPTED', 'IN_TRANSIT', 'COMPLETED');

-- Entregas PENDING não têm pagamento ainda (comportamento novo)
UPDATE deliveries 
SET delivery_type = 'DELIVERY',
    payment_completed = false, 
    payment_captured = false
WHERE status = 'PENDING';

-- 5. Adicionar constraint CHECK para delivery_type (se não existir)
-- Valores: DELIVERY (entrega), RIDE (viagem), CONTRACT (contrato/organizer)
DO $$ 
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints 
        WHERE constraint_name = 'chk_delivery_type' AND table_name = 'deliveries'
    ) THEN
        ALTER TABLE deliveries ADD CONSTRAINT chk_delivery_type 
            CHECK (delivery_type IN ('DELIVERY', 'RIDE', 'CONTRACT'));
    END IF;
END $$;

-- 6. Criar índice para queries por tipo e status (se não existir)
CREATE INDEX IF NOT EXISTS idx_deliveries_type_status 
ON deliveries(delivery_type, status);

-- 7. Criar índice para queries por status de pagamento (se não existir)
CREATE INDEX IF NOT EXISTS idx_deliveries_payment_status 
ON deliveries(payment_completed, payment_captured);

-- 8. Comentários
COMMENT ON COLUMN deliveries.delivery_type IS 
    'Tipo: DELIVERY (entrega de objeto, paga ao aceitar) ou RIDE (viagem, paga ao iniciar)';
COMMENT ON COLUMN deliveries.payment_completed IS 
    'Se o pagamento foi realizado (momento varia por tipo)';
COMMENT ON COLUMN deliveries.payment_captured IS 
    'Se o pagamento foi capturado/confirmado pelo gateway (ex: cartão autorizado)';
