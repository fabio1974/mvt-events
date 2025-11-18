-- ============================================================================
-- Migration V56: Corrigir constraints de delivery_type
-- ============================================================================
-- Description: Remove constraint incorreta que força partnership_id para
--              entregas CONTRACT. Partnership é apenas para prefeituras,
--              não para contratos privados (service_contracts).
-- Author: System
-- Date: 2025-11-05
-- ============================================================================

-- ============================================================================
-- 1. Remover constraints incorretas
-- ============================================================================

DO $$
BEGIN
    -- Remover constraint que força partnership_id em entregas CONTRACT
    IF EXISTS (
        SELECT 1 FROM information_schema.table_constraints 
        WHERE constraint_name = 'chk_contract_has_partnership' 
        AND table_name = 'deliveries'
    ) THEN
        ALTER TABLE deliveries DROP CONSTRAINT chk_contract_has_partnership;
        RAISE NOTICE '✅ Constraint chk_contract_has_partnership removida';
    ELSE
        RAISE NOTICE 'ℹ️  Constraint chk_contract_has_partnership não existe';
    END IF;

    -- Remover constraint que impede partnership_id em ON_DEMAND
    IF EXISTS (
        SELECT 1 FROM information_schema.table_constraints 
        WHERE constraint_name = 'chk_on_demand_no_partnership' 
        AND table_name = 'deliveries'
    ) THEN
        ALTER TABLE deliveries DROP CONSTRAINT chk_on_demand_no_partnership;
        RAISE NOTICE '✅ Constraint chk_on_demand_no_partnership removida';
    ELSE
        RAISE NOTICE 'ℹ️  Constraint chk_on_demand_no_partnership não existe';
    END IF;
END $$;

-- ============================================================================
-- 2. Adicionar constraint correta (apenas validação de tipo)
-- ============================================================================

-- Manter apenas validação do enum delivery_type
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints 
        WHERE constraint_name = 'chk_delivery_type' 
        AND table_name = 'deliveries'
    ) THEN
        ALTER TABLE deliveries 
        ADD CONSTRAINT chk_delivery_type
        CHECK (delivery_type IN ('CONTRACT', 'ON_DEMAND'));
        
        RAISE NOTICE '✅ Constraint chk_delivery_type criada';
    ELSE
        RAISE NOTICE 'ℹ️  Constraint chk_delivery_type já existe';
    END IF;
END $$;

-- ============================================================================
-- 3. Atualizar comentários para esclarecer
-- ============================================================================

COMMENT ON COLUMN deliveries.delivery_type IS 
'Tipo de entrega:
- CONTRACT: Cliente possui service_contract com ORGANIZER (partnership_id geralmente NULL)
- ON_DEMAND: Cliente sem service_contract, entrega avulsa (partnership_id sempre NULL)';

COMMENT ON COLUMN deliveries.partnership_id IS 
'ID da parceria municipal (APENAS para entregas de prefeituras/órgãos públicos).
Para contratos privados (service_contract entre CLIENT e ORGANIZER), este campo fica NULL.
Uso RARO - apenas quando prefeitura é o cliente.';

-- ============================================================================
-- Resumo
-- ============================================================================
DO $$
DECLARE
    contract_count INTEGER;
    on_demand_count INTEGER;
    with_partnership INTEGER;
    without_partnership INTEGER;
BEGIN
    SELECT COUNT(*) INTO contract_count FROM deliveries WHERE delivery_type = 'CONTRACT';
    SELECT COUNT(*) INTO on_demand_count FROM deliveries WHERE delivery_type = 'ON_DEMAND';
    SELECT COUNT(*) INTO with_partnership FROM deliveries WHERE partnership_id IS NOT NULL;
    SELECT COUNT(*) INTO without_partnership FROM deliveries WHERE partnership_id IS NULL;
    
    RAISE NOTICE '============================================';
    RAISE NOTICE 'CONSTRAINTS CORRIGIDAS';
    RAISE NOTICE '============================================';
    RAISE NOTICE '';
    RAISE NOTICE '📦 MODELO CORRETO:';
    RAISE NOTICE '';
    RAISE NOTICE '1️⃣  CONTRACT (Com service_contract):';
    RAISE NOTICE '   - CLIENT tem service_contract com ORGANIZER';
    RAISE NOTICE '   - partnership_id: NULL (contrato privado)';
    RAISE NOTICE '   - Total: % entregas', contract_count;
    RAISE NOTICE '';
    RAISE NOTICE '2️⃣  ON_DEMAND (Sem contrato):';
    RAISE NOTICE '   - CLIENT sem service_contract';
    RAISE NOTICE '   - partnership_id: NULL';
    RAISE NOTICE '   - Total: % entregas', on_demand_count;
    RAISE NOTICE '';
    RAISE NOTICE '3️⃣  Contrato Público (RARO):';
    RAISE NOTICE '   - Apenas quando PREFEITURA/ÓRGÃO PÚBLICO é cliente';
    RAISE NOTICE '   - partnership_id: NOT NULL';
    RAISE NOTICE '   - Total: % entregas', with_partnership;
    RAISE NOTICE '';
    RAISE NOTICE '✅ Constraints removidas:';
    RAISE NOTICE '   - chk_contract_has_partnership (INCORRETA)';
    RAISE NOTICE '   - chk_on_demand_no_partnership (INCORRETA)';
    RAISE NOTICE '';
    RAISE NOTICE '✅ Constraint mantida:';
    RAISE NOTICE '   - chk_delivery_type (valida enum)';
    RAISE NOTICE '============================================';
END $$;
