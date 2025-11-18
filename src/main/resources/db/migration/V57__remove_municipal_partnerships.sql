-- ============================================================================
-- Migration V57: Remover conceito de Municipal Partnership
-- ============================================================================
-- Description: Remove tabela municipal_partnerships e campo partnership_id.
--              Prefeituras serão tratadas como ORGANIZERs comuns no sistema.
--              Simplifica o modelo: apenas service_contracts entre CLIENTs
--              e ORGANIZERs (sejam privados ou públicos).
-- Author: System
-- Date: 2025-11-05
-- ============================================================================

-- ============================================================================
-- 1. Remover foreign keys que referenciam municipal_partnerships
-- ============================================================================

DO $$
BEGIN
    -- FK de deliveries
    IF EXISTS (
        SELECT 1 FROM information_schema.table_constraints 
        WHERE constraint_name = 'fk_delivery_partnership' 
        AND table_name = 'deliveries'
    ) THEN
        ALTER TABLE deliveries DROP CONSTRAINT fk_delivery_partnership;
        RAISE NOTICE '✅ FK fk_delivery_partnership removida de deliveries';
    END IF;

    -- FK de adm_profiles (se existir)
    IF EXISTS (
        SELECT 1 FROM information_schema.table_constraints 
        WHERE constraint_name = 'fk_adm_partnership' 
        AND table_name = 'adm_profiles'
    ) THEN
        ALTER TABLE adm_profiles DROP CONSTRAINT fk_adm_partnership;
        RAISE NOTICE '✅ FK fk_adm_partnership removida de adm_profiles';
    END IF;
END $$;

-- ============================================================================
-- 2. Remover coluna partnership_id de deliveries
-- ============================================================================

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'deliveries' 
        AND column_name = 'partnership_id'
    ) THEN
        ALTER TABLE deliveries DROP COLUMN partnership_id;
        RAISE NOTICE '✅ Coluna partnership_id removida de deliveries';
    ELSE
        RAISE NOTICE 'ℹ️  Coluna partnership_id não existe em deliveries';
    END IF;
END $$;

-- ============================================================================
-- 3. Remover coluna partnership_id de adm_profiles (se existir)
-- ============================================================================

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'adm_profiles' 
        AND column_name = 'partnership_id'
    ) THEN
        ALTER TABLE adm_profiles DROP COLUMN partnership_id;
        RAISE NOTICE '✅ Coluna partnership_id removida de adm_profiles';
    END IF;
END $$;

-- ============================================================================
-- 4. Remover índices relacionados a partnership
-- ============================================================================

DROP INDEX IF EXISTS idx_delivery_partnership;
DROP INDEX IF EXISTS idx_delivery_partnership_completed;
DROP INDEX IF EXISTS idx_partnership_city;
DROP INDEX IF EXISTS idx_partnership_status;
DROP INDEX IF EXISTS idx_partnership_cnpj;

-- ============================================================================
-- 5. Remover view que usa partnership
-- ============================================================================

DROP VIEW IF EXISTS available_on_demand_deliveries;

-- Recriar view SEM partnership
CREATE OR REPLACE VIEW available_on_demand_deliveries AS
SELECT 
    d.id,
    d.client_id,
    d.from_address,
    d.from_lat,
    d.from_lng,
    d.to_address,
    d.to_lat,
    d.to_lng,
    d.distance_km,
    d.total_amount,
    d.item_description,
    d.created_at,
    u.username as client_email,
    u.name as client_name,
    u.phone as client_phone
FROM deliveries d
JOIN users u ON u.id = d.client_id
WHERE d.delivery_type = 'ON_DEMAND'
  AND d.status = 'PENDING'
  AND d.courier_id IS NULL
ORDER BY d.created_at ASC;

COMMENT ON VIEW available_on_demand_deliveries IS 
'Entregas ON_DEMAND disponíveis para COURIERs aceitarem';

-- ============================================================================
-- 6. Remover tabela municipal_partnerships
-- ============================================================================

DO $$
BEGIN
    DROP TABLE IF EXISTS municipal_partnerships CASCADE;
    RAISE NOTICE '✅ Tabela municipal_partnerships removida';
END $$;

-- ============================================================================
-- 7. Atualizar comentário do delivery_type
-- ============================================================================

COMMENT ON COLUMN deliveries.delivery_type IS 
'Tipo de entrega:
- CONTRACT: Cliente possui service_contract com ORGANIZER (público ou privado)
- ON_DEMAND: Cliente sem service_contract, entrega avulsa';

-- ============================================================================
-- Resumo
-- ============================================================================
DO $$
DECLARE
    contract_count INTEGER;
    on_demand_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO contract_count FROM deliveries WHERE delivery_type = 'CONTRACT';
    SELECT COUNT(*) INTO on_demand_count FROM deliveries WHERE delivery_type = 'ON_DEMAND';
    
    RAISE NOTICE '============================================';
    RAISE NOTICE 'MUNICIPAL PARTNERSHIPS REMOVIDO';
    RAISE NOTICE '============================================';
    RAISE NOTICE '';
    RAISE NOTICE '✅ MODELO SIMPLIFICADO:';
    RAISE NOTICE '';
    RAISE NOTICE '1️⃣  CONTRACT (Com service_contract):';
    RAISE NOTICE '   - CLIENT tem service_contract com ORGANIZER';
    RAISE NOTICE '   - ORGANIZER pode ser privado OU público (prefeitura)';
    RAISE NOTICE '   - Total: % entregas', contract_count;
    RAISE NOTICE '';
    RAISE NOTICE '2️⃣  ON_DEMAND (Sem contrato):';
    RAISE NOTICE '   - CLIENT sem service_contract';
    RAISE NOTICE '   - Disponível para todos os COURIERs no raio';
    RAISE NOTICE '   - Total: % entregas', on_demand_count;
    RAISE NOTICE '';
    RAISE NOTICE '🗑️  Removido:';
    RAISE NOTICE '   - Tabela: municipal_partnerships';
    RAISE NOTICE '   - Coluna: deliveries.partnership_id';
    RAISE NOTICE '   - Coluna: adm_profiles.partnership_id';
    RAISE NOTICE '   - Constraints: fk_delivery_partnership, fk_adm_partnership';
    RAISE NOTICE '   - Índices: idx_delivery_partnership, idx_partnership_*';
    RAISE NOTICE '';
    RAISE NOTICE '💡 Agora: Prefeituras = ORGANIZERs comuns';
    RAISE NOTICE '============================================';
END $$;
