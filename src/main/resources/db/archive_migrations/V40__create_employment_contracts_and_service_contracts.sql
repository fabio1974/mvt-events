-- ============================================================================
-- V40: Criar tabelas para N:M relationships (COURIER e CLIENT)
-- ============================================================================
-- Cria duas tabelas:
-- 1. employment_contracts - Relação empregado-empresa (COURIER ↔ Organization)
-- 2. contracts - Relação cliente-fornecedor (CLIENT ↔ Organization)
-- ============================================================================

-- ============================================================================
-- 1. TABELA: employment_contracts (Contratos de Trabalho)
-- ============================================================================
-- Representa a relação de trabalho entre um COURIER e uma Organization
-- Um motoboy pode trabalhar para múltiplas organizações

CREATE TABLE employment_contracts (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Relacionamentos (tipos compatíveis com as tabelas originais)
    courier_id UUID NOT NULL,           -- users.id é UUID
    organization_id BIGINT NOT NULL,    -- organizations.id é BIGINT
    
    -- Metadados do contrato de trabalho
    linked_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    
    -- Foreign Keys
    CONSTRAINT fk_employment_courier FOREIGN KEY (courier_id) 
        REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_employment_organization FOREIGN KEY (organization_id) 
        REFERENCES organizations(id) ON DELETE CASCADE,
    
    -- Constraints
    CONSTRAINT uq_employment_courier_org UNIQUE (courier_id, organization_id)
);

-- Índices para performance
CREATE INDEX idx_employment_courier ON employment_contracts(courier_id);
CREATE INDEX idx_employment_organization ON employment_contracts(organization_id);
CREATE INDEX idx_employment_active ON employment_contracts(is_active);

-- ============================================================================
-- 2. TABELA: contracts (Contratos de Serviço)
-- ============================================================================
-- Representa a relação de contratação de serviços entre um CLIENT e uma Organization
-- Um cliente pode ter múltiplos contratos, mas apenas 1 pode ser titular (is_primary)

CREATE TABLE contracts (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Relacionamentos (tipos compatíveis com as tabelas originais)
    client_id UUID NOT NULL,            -- users.id é UUID
    organization_id BIGINT NOT NULL,    -- organizations.id é BIGINT
    
    -- Metadados do contrato de serviço
    contract_number VARCHAR(50) UNIQUE,
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    
    -- Datas
    contract_date DATE NOT NULL DEFAULT CURRENT_DATE,
    start_date DATE NOT NULL,
    end_date DATE,
    
    -- Foreign Keys
    CONSTRAINT fk_contract_client FOREIGN KEY (client_id) 
        REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_contract_organization FOREIGN KEY (organization_id) 
        REFERENCES organizations(id) ON DELETE CASCADE,
    
    -- Constraints
    CONSTRAINT uq_contract_client_org UNIQUE (client_id, organization_id),
    CONSTRAINT chk_contract_status CHECK (status IN ('ACTIVE', 'SUSPENDED', 'CANCELLED')),
    CONSTRAINT chk_contract_dates CHECK (end_date IS NULL OR end_date >= start_date)
);

-- Índices para performance
CREATE INDEX idx_contract_client ON contracts(client_id);
CREATE INDEX idx_contract_organization ON contracts(organization_id);
CREATE INDEX idx_contract_status ON contracts(status);
CREATE INDEX idx_contract_primary ON contracts(is_primary);

-- ============================================================================
-- 3. TRIGGER: Garantir apenas 1 contrato primário por cliente
-- ============================================================================
-- Este trigger garante que apenas 1 contrato pode ter is_primary = TRUE por cliente

CREATE OR REPLACE FUNCTION check_primary_contract()
RETURNS TRIGGER AS $$
BEGIN
    -- Se está marcando como primário
    IF NEW.is_primary = TRUE THEN
        -- Desmarca todos os outros contratos deste cliente
        UPDATE contracts
        SET is_primary = FALSE
        WHERE client_id = NEW.client_id
          AND id != NEW.id
          AND is_primary = TRUE;
    END IF;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER enforce_single_primary_contract
    BEFORE INSERT OR UPDATE ON contracts
    FOR EACH ROW
    EXECUTE FUNCTION check_primary_contract();

-- ============================================================================
-- 4. COMENTÁRIOS NAS TABELAS
-- ============================================================================

COMMENT ON TABLE employment_contracts IS 'Contratos de trabalho entre COURIER e Organization (relação empregado-empresa)';
COMMENT ON TABLE contracts IS 'Contratos de serviço entre CLIENT e Organization (relação cliente-fornecedor)';

COMMENT ON COLUMN employment_contracts.is_active IS 'Se o contrato de trabalho está ativo';
COMMENT ON COLUMN employment_contracts.linked_at IS 'Data/hora que o courier foi contratado';

COMMENT ON COLUMN contracts.is_primary IS 'Se este é o contrato titular do cliente (apenas 1 por cliente)';
COMMENT ON COLUMN contracts.status IS 'Status do contrato: ACTIVE, SUSPENDED, CANCELLED';
COMMENT ON COLUMN contracts.contract_number IS 'Número único do contrato';
COMMENT ON COLUMN contracts.start_date IS 'Data de início da vigência do contrato';
COMMENT ON COLUMN contracts.end_date IS 'Data de fim da vigência do contrato (NULL = indeterminado)';
