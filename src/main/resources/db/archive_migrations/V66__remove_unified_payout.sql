-- Remove UnifiedPayout e suas referências
-- O sistema agora usa apenas PayoutItem para rastrear repasses individuais

-- 1. Remover constraint de unique que envolve payout_id
ALTER TABLE payout_items DROP CONSTRAINT IF EXISTS payout_items_payout_id_payment_id_key;

-- 2. Remover coluna payout_id da tabela payout_items
ALTER TABLE payout_items DROP COLUMN IF EXISTS payout_id;

-- 3. Dropar tabela unified_payouts (se existir)
DROP TABLE IF EXISTS unified_payouts CASCADE;

-- 4. Comentários sobre a mudança
COMMENT ON TABLE payout_items IS 'Items de repasse individual. Cada item representa um repasse específico para um beneficiário (courier, ADM, sistema, etc)';
COMMENT ON COLUMN payout_items.beneficiary_id IS 'Beneficiário que receberá este repasse';
COMMENT ON COLUMN payout_items.payment_id IS 'Pagamento de origem deste repasse';
COMMENT ON COLUMN payout_items.status IS 'Status do repasse: PENDING, PROCESSING, PAID, FAILED, CANCELLED';
