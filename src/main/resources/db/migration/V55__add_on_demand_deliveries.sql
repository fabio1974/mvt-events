-- ============================================================================
-- Migration V55: Add support for on-demand deliveries (without contract)
-- ============================================================================
-- Description: Adiciona suporte para entregas "avulsas" onde CLIENTs podem
--              solicitar entregas sem ter contrato de serviço com uma organização.
--              Neste caso, a notificação é enviada para todos os COURIERs
--              disponíveis no raio de ação (algoritmo nível 3).
-- Author: System
-- Date: 2025-11-05
-- ============================================================================

-- ============================================================================
-- 1. Adicionar campo delivery_type para diferenciar tipos de entrega
-- ============================================================================

-- Adicionar coluna se não existir
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'deliveries' 
        AND column_name = 'delivery_type'
    ) THEN
        ALTER TABLE deliveries 
        ADD COLUMN delivery_type VARCHAR(20) DEFAULT 'CONTRACT' NOT NULL;
        
        RAISE NOTICE '✅ Coluna delivery_type adicionada';
    ELSE
        RAISE NOTICE 'ℹ️  Coluna delivery_type já existe';
    END IF;
END $$;

-- Adicionar constraint para valores válidos
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

COMMENT ON COLUMN deliveries.delivery_type IS 
'Tipo de entrega: CONTRACT (com contrato de serviço) ou ON_DEMAND (avulsa, sem contrato)';

-- ============================================================================
-- 2. Regras de negócio para delivery_type
-- ============================================================================

-- Constraint: CONTRACT deve ter partnership_id
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints 
        WHERE constraint_name = 'chk_contract_has_partnership' 
        AND table_name = 'deliveries'
    ) THEN
        ALTER TABLE deliveries 
        ADD CONSTRAINT chk_contract_has_partnership
        CHECK (
            delivery_type != 'CONTRACT' OR 
            (delivery_type = 'CONTRACT' AND partnership_id IS NOT NULL)
        );
        
        RAISE NOTICE '✅ Constraint chk_contract_has_partnership criada';
    ELSE
        RAISE NOTICE 'ℹ️  Constraint chk_contract_has_partnership já existe';
    END IF;
END $$;

COMMENT ON CONSTRAINT chk_contract_has_partnership ON deliveries IS 
'Entregas do tipo CONTRACT devem ter partnership_id (organização associada)';

-- Constraint: ON_DEMAND não deve ter partnership_id
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints 
        WHERE constraint_name = 'chk_on_demand_no_partnership' 
        AND table_name = 'deliveries'
    ) THEN
        ALTER TABLE deliveries 
        ADD CONSTRAINT chk_on_demand_no_partnership
        CHECK (
            delivery_type != 'ON_DEMAND' OR 
            (delivery_type = 'ON_DEMAND' AND partnership_id IS NULL)
        );
        
        RAISE NOTICE '✅ Constraint chk_on_demand_no_partnership criada';
    ELSE
        RAISE NOTICE 'ℹ️  Constraint chk_on_demand_no_partnership já existe';
    END IF;
END $$;

COMMENT ON CONSTRAINT chk_on_demand_no_partnership ON deliveries IS 
'Entregas do tipo ON_DEMAND não devem ter partnership_id (disponível para todos os COURIERs no raio)';

-- ============================================================================
-- 3. Adicionar índice para busca eficiente de entregas ON_DEMAND
-- ============================================================================

CREATE INDEX IF NOT EXISTS idx_delivery_on_demand_status 
ON deliveries(delivery_type, status, created_at DESC) 
WHERE delivery_type = 'ON_DEMAND' AND status IN ('PENDING', 'ACCEPTED');

COMMENT ON INDEX idx_delivery_on_demand_status IS 
'Índice para busca rápida de entregas ON_DEMAND disponíveis para COURIERs';

-- ============================================================================
-- 4. Adicionar campos de geolocalização para raio de ação (se não existirem)
-- ============================================================================

-- Nota: from_lat, from_lng, to_lat, to_lng já existem na tabela deliveries
-- Este comentário serve apenas para documentar que são usados no algoritmo de raio

COMMENT ON COLUMN deliveries.from_lat IS 
'Latitude do ponto de coleta - usado no algoritmo de raio para entregas ON_DEMAND';

COMMENT ON COLUMN deliveries.from_lng IS 
'Longitude do ponto de coleta - usado no algoritmo de raio para entregas ON_DEMAND';

-- ============================================================================
-- 5. Atualizar entregas existentes para tipo CONTRACT
-- ============================================================================

UPDATE deliveries 
SET delivery_type = 'CONTRACT'
WHERE delivery_type IS NULL AND partnership_id IS NOT NULL;

-- ============================================================================
-- 6. Criar view para entregas ON_DEMAND disponíveis
-- ============================================================================

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
'View de entregas ON_DEMAND disponíveis para COURIERs aceitarem (ordenadas por data de criação)';

-- ============================================================================
-- Resumo
-- ============================================================================
DO $$
DECLARE
    contract_count INTEGER;
    on_demand_count INTEGER;
    pending_on_demand INTEGER;
BEGIN
    SELECT COUNT(*) INTO contract_count FROM deliveries WHERE delivery_type = 'CONTRACT';
    SELECT COUNT(*) INTO on_demand_count FROM deliveries WHERE delivery_type = 'ON_DEMAND';
    SELECT COUNT(*) INTO pending_on_demand FROM deliveries WHERE delivery_type = 'ON_DEMAND' AND status = 'PENDING';
    
    RAISE NOTICE '============================================';
    RAISE NOTICE 'ENTREGAS ON-DEMAND HABILITADAS';
    RAISE NOTICE '============================================';
    RAISE NOTICE '';
    RAISE NOTICE '📦 TIPOS DE ENTREGA:';
    RAISE NOTICE '';
    RAISE NOTICE '1️⃣  CONTRACT (Com Contrato):';
    RAISE NOTICE '   - CLIENT possui service_contract com ORGANIZER';
    RAISE NOTICE '   - Delivery tem partnership_id (organização)';
    RAISE NOTICE '   - Notificação vai para COURIERs da organização';
    RAISE NOTICE '   - Total: % entregas', contract_count;
    RAISE NOTICE '';
    RAISE NOTICE '2️⃣  ON_DEMAND (Avulsa/Sem Contrato):';
    RAISE NOTICE '   - CLIENT não precisa ter contrato';
    RAISE NOTICE '   - Delivery SEM partnership_id';
    RAISE NOTICE '   - Notificação vai para TODOS os COURIERs no raio';
    RAISE NOTICE '   - Usa algoritmo de raio (nível 3)';
    RAISE NOTICE '   - Total: % entregas (% pendentes)', on_demand_count, pending_on_demand;
    RAISE NOTICE '';
    RAISE NOTICE '✅ Constraints criadas:';
    RAISE NOTICE '   - chk_delivery_type';
    RAISE NOTICE '   - chk_contract_has_partnership';
    RAISE NOTICE '   - chk_on_demand_no_partnership';
    RAISE NOTICE '';
    RAISE NOTICE '✅ View criada: available_on_demand_deliveries';
    RAISE NOTICE '============================================';
END $$;
