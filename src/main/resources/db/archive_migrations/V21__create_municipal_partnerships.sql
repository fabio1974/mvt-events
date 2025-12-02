-- ============================================================================
-- V21: Criar tabela municipal_partnerships
-- ============================================================================
-- Parcerias institucionais com prefeituras municipais
-- Permite ADMs serem vinculados a convênios com órgãos públicos
-- ============================================================================

CREATE TABLE municipal_partnerships (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Identificação
    name VARCHAR(200) NOT NULL,
    city VARCHAR(100) NOT NULL,
    state VARCHAR(2) NOT NULL,
    cnpj VARCHAR(18) NOT NULL UNIQUE,
    
    -- Contato
    contact_name VARCHAR(150),
    contact_email VARCHAR(150),
    contact_phone VARCHAR(20),
    
    -- Convênio
    agreement_number VARCHAR(50),
    start_date DATE,
    end_date DATE,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    notes TEXT,
    
    -- Constraints
    CONSTRAINT chk_partnership_status CHECK (status IN ('PENDING', 'ACTIVE', 'SUSPENDED', 'EXPIRED')),
    CONSTRAINT chk_partnership_dates CHECK (end_date IS NULL OR end_date >= start_date)
);

-- Índices
CREATE INDEX idx_partnership_city ON municipal_partnerships(city);
CREATE INDEX idx_partnership_state ON municipal_partnerships(state);
CREATE INDEX idx_partnership_status ON municipal_partnerships(status);
CREATE INDEX idx_partnership_cnpj ON municipal_partnerships(cnpj);

-- Comentários
COMMENT ON TABLE municipal_partnerships IS 'Parcerias com prefeituras municipais para entregas institucionais';
COMMENT ON COLUMN municipal_partnerships.cnpj IS 'CNPJ da prefeitura/órgão público (único)';
COMMENT ON COLUMN municipal_partnerships.status IS 'Status da parceria: PENDING, ACTIVE, SUSPENDED, EXPIRED';
