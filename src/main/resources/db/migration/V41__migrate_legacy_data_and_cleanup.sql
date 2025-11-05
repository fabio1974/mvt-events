-- ============================================================================
-- V41: Migrar dados antigos e remover tabelas legacy
-- ============================================================================
-- Migra dados das tabelas antigas para as novas e remove as tabelas obsoletas
-- ============================================================================

-- ============================================================================
-- 1. MIGRAÇÃO DE DADOS (se as tabelas antigas existirem)
-- ============================================================================

-- Migrar courier_organizations → employment_contracts (se existir)
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'courier_organizations') THEN
        INSERT INTO employment_contracts (id, created_at, updated_at, courier_id, organization_id, linked_at, is_active)
        SELECT 
            id,
            created_at,
            updated_at,
            courier_id,
            organization_id,
            COALESCE(linked_at, created_at) as linked_at,
            COALESCE(is_active, true) as is_active
        FROM courier_organizations
        ON CONFLICT (courier_id, organization_id) DO NOTHING;
        
        RAISE NOTICE 'Migrados % registros de courier_organizations para employment_contracts', 
            (SELECT COUNT(*) FROM courier_organizations);
    ELSE
        RAISE NOTICE 'Tabela courier_organizations não existe, pulando migração';
    END IF;
END $$;

-- Migrar courier_adm_links → employment_contracts (se existir)
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'courier_adm_links') THEN
        -- Migra links antigos para a nova estrutura
        -- Busca o organization_id do usuário ADM (que é do tipo BIGINT)
        INSERT INTO employment_contracts (courier_id, organization_id, linked_at, is_active)
        SELECT 
            cal.courier_id,
            u.organization_id,  -- Pega o organization_id do ADM (BIGINT)
            cal.created_at as linked_at,
            COALESCE(cal.is_active, true) as is_active
        FROM courier_adm_links cal
        INNER JOIN users u ON u.id = cal.adm_id
        -- Apenas migra se o ADM tem uma organization vinculada
        WHERE u.organization_id IS NOT NULL
        ON CONFLICT (courier_id, organization_id) DO NOTHING;
        
        RAISE NOTICE 'Migrados % registros de courier_adm_links para employment_contracts', 
            (SELECT COUNT(*) FROM courier_adm_links cal
             INNER JOIN users u ON u.id = cal.adm_id
             WHERE u.organization_id IS NOT NULL);
    ELSE
        RAISE NOTICE 'Tabela courier_adm_links não existe, pulando migração';
    END IF;
END $$;

-- Migrar client_manager_links → contracts (se existir)
DO $$
DECLARE
    table_exists boolean;
    has_manager_id boolean := false;
BEGIN
    -- Verificar se a tabela existe
    SELECT EXISTS (
        SELECT 1 FROM information_schema.tables 
        WHERE table_name = 'client_manager_links'
    ) INTO table_exists;
    
    IF table_exists THEN
        -- Verificar se a coluna manager_id existe
        SELECT EXISTS (
            SELECT 1 FROM information_schema.columns 
            WHERE table_name = 'client_manager_links' 
            AND column_name = 'manager_id'
        ) INTO has_manager_id;
        
        IF has_manager_id THEN
            -- Migra links antigos para contratos de serviço
            -- O primeiro link de cada cliente será marcado como primário
            WITH ranked_links AS (
                SELECT 
                    client_id,
                    manager_id,
                    created_at,
                    COALESCE(is_active, true) as is_active,
                    ROW_NUMBER() OVER (PARTITION BY client_id ORDER BY created_at) as rn
                FROM client_manager_links
                WHERE EXISTS (
                    SELECT 1 FROM users u 
                    WHERE u.id = manager_id 
                    AND u.organization_id IS NOT NULL
                )
            )
            INSERT INTO contracts (
                client_id, 
                organization_id, 
                contract_date, 
                start_date, 
                status, 
                is_primary
            )
            SELECT 
                rl.client_id,
                u.organization_id,
                rl.created_at::DATE as contract_date,
                rl.created_at::DATE as start_date,
                CASE WHEN rl.is_active THEN 'ACTIVE' ELSE 'CANCELLED' END as status,
                (rl.rn = 1) as is_primary
            FROM ranked_links rl
            INNER JOIN users u ON u.id = rl.manager_id
            WHERE u.organization_id IS NOT NULL
            ON CONFLICT (client_id, organization_id) DO NOTHING;
            
            RAISE NOTICE 'Migrados registros de client_manager_links para contracts';
        ELSE
            RAISE NOTICE 'Tabela client_manager_links existe mas sem coluna manager_id, pulando migração';
        END IF;
    ELSE
        RAISE NOTICE 'Tabela client_manager_links não existe, pulando migração';
    END IF;
END $$;

-- ============================================================================
-- 2. REMOVER TABELAS ANTIGAS (Legacy)
-- ============================================================================

-- Remover courier_adm_links (se existir)
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'courier_adm_links') THEN
        DROP TABLE IF EXISTS courier_adm_links CASCADE;
        RAISE NOTICE 'Tabela courier_adm_links removida';
    END IF;
END $$;

-- Remover client_manager_links (se existir)
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'client_manager_links') THEN
        DROP TABLE IF EXISTS client_manager_links CASCADE;
        RAISE NOTICE 'Tabela client_manager_links removida';
    END IF;
END $$;

-- Remover courier_organizations antiga (se existir e foi migrada)
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'courier_organizations') THEN
        -- Só remove se já temos dados migrados
        IF EXISTS (SELECT 1 FROM employment_contracts) THEN
            DROP TABLE IF EXISTS courier_organizations CASCADE;
            RAISE NOTICE 'Tabela courier_organizations antiga removida';
        ELSE
            RAISE NOTICE 'Tabela courier_organizations existe mas sem dados migrados, mantendo por segurança';
        END IF;
    END IF;
END $$;

-- ============================================================================
-- 3. VALIDAÇÕES PÓS-MIGRAÇÃO
-- ============================================================================

-- Verificar se há clientes sem contrato primário e criar um
DO $$
DECLARE
    client_record RECORD;
    first_contract_id UUID;
BEGIN
    FOR client_record IN 
        SELECT u.id, u.username
        FROM users u
        WHERE u.role = 'CLIENT'
        AND NOT EXISTS (
            SELECT 1 FROM contracts c 
            WHERE c.client_id = u.id AND c.is_primary = TRUE
        )
        AND EXISTS (
            SELECT 1 FROM contracts c 
            WHERE c.client_id = u.id
        )
    LOOP
        -- Pega o primeiro contrato deste cliente
        SELECT id INTO first_contract_id
        FROM contracts
        WHERE client_id = client_record.id
        ORDER BY created_at
        LIMIT 1;
        
        -- Marca como primário
        UPDATE contracts
        SET is_primary = TRUE
        WHERE id = first_contract_id;
        
        RAISE NOTICE 'Cliente % - Contrato % marcado como primário', 
            client_record.username, first_contract_id;
    END LOOP;
END $$;

-- Log final
DO $$
BEGIN
    RAISE NOTICE '============================================';
    RAISE NOTICE 'RESUMO DA MIGRAÇÃO:';
    RAISE NOTICE '- Employment Contracts: % registros', (SELECT COUNT(*) FROM employment_contracts);
    RAISE NOTICE '- Service Contracts: % registros', (SELECT COUNT(*) FROM contracts);
    RAISE NOTICE '- Contratos Primários: % registros', (SELECT COUNT(*) FROM contracts WHERE is_primary = TRUE);
    RAISE NOTICE '============================================';
END $$;
