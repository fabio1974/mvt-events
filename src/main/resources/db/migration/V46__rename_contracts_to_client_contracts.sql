-- ============================================================================
-- V46: Renomear tabela contracts para client_contracts
-- ============================================================================
-- Renomeia a tabela e todas as constraints/indices relacionados
-- ============================================================================

-- 1. Renomear a tabela
ALTER TABLE contracts RENAME TO client_contracts;

-- 2. Renomear constraints
ALTER TABLE client_contracts 
    RENAME CONSTRAINT fk_contract_client TO fk_client_contract_client;

ALTER TABLE client_contracts 
    RENAME CONSTRAINT fk_contract_organization TO fk_client_contract_organization;

ALTER TABLE client_contracts 
    RENAME CONSTRAINT uq_contract_client_org TO uq_client_contract_client_org;

ALTER TABLE client_contracts 
    RENAME CONSTRAINT chk_contract_status TO chk_client_contract_status;

ALTER TABLE client_contracts 
    RENAME CONSTRAINT chk_contract_dates TO chk_client_contract_dates;

-- 3. Renomear índices
ALTER INDEX idx_contract_client RENAME TO idx_client_contract_client;
ALTER INDEX idx_contract_organization RENAME TO idx_client_contract_organization;
ALTER INDEX idx_contract_status RENAME TO idx_client_contract_status;
ALTER INDEX idx_contract_primary RENAME TO idx_client_contract_primary;

-- 4. Atualizar trigger function (se ainda existir)
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_proc WHERE proname = 'check_primary_contract') THEN
        -- Recriar a função do trigger para referenciar a nova tabela
        CREATE OR REPLACE FUNCTION check_primary_client_contract()
        RETURNS TRIGGER AS $trigger$
        BEGIN
            -- Se está marcando como primário
            IF NEW.is_primary = TRUE THEN
                -- Desmarca todos os outros contratos deste cliente
                UPDATE client_contracts
                SET is_primary = FALSE
                WHERE client_id = NEW.client_id
                  AND id != NEW.id
                  AND is_primary = TRUE;
            END IF;
            RETURN NEW;
        END;
        $trigger$ LANGUAGE plpgsql;

        -- Remover o trigger antigo se existir
        DROP TRIGGER IF EXISTS enforce_single_primary_contract ON client_contracts;
        
        -- Criar novo trigger
        CREATE TRIGGER enforce_single_primary_client_contract
        BEFORE INSERT OR UPDATE ON client_contracts
        FOR EACH ROW
        EXECUTE FUNCTION check_primary_client_contract();
        
        -- Remover função antiga
        DROP FUNCTION IF EXISTS check_primary_contract();
        
        RAISE NOTICE 'Trigger atualizado para client_contracts';
    END IF;
END $$;
