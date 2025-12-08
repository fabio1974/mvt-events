-- ============================================================================
-- Migration V7: Remove PayoutItem (não necessário com Iugu split automático)
-- ============================================================================
-- Data: 2025-12-03
-- Motivo: Com Iugu split, os repasses são automáticos (D+1)
--         splitRules JSON em Payment + payment_deliveries N:M são suficientes
-- ============================================================================

-- STEP 1: Drop constraints e índices
DROP INDEX IF EXISTS idx_payout_item_payout;
DROP INDEX IF EXISTS idx_payout_item_payment;

-- STEP 2: Drop tabela payout_items
DROP TABLE IF EXISTS payout_items CASCADE;

-- STEP 3: Drop sequence
DROP SEQUENCE IF EXISTS payout_items_id_seq CASCADE;

-- ============================================================================
-- SUMÁRIO
-- ============================================================================
DO $$
BEGIN
    RAISE NOTICE '🗑️ MIGRATION V7 COMPLETA:';
    RAISE NOTICE '   ├─ Tabela payout_items removida';
    RAISE NOTICE '   ├─ Índices removidos';
    RAISE NOTICE '   ├─ Sequence removida';
    RAISE NOTICE '   └─ Motivo: Iugu faz split automático, não precisamos rastrear manualmente';
END $$;
