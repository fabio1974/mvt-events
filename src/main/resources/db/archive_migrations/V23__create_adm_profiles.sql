-- ============================================================================
-- V23: Criar tabela adm_profiles
-- ============================================================================
-- Perfil especializado para usuários ADM (gerente local - TENANT)
-- Relacionamento 1:1 com users
-- ============================================================================

CREATE TABLE adm_profiles (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Relacionamento 1:1 com User
    user_id UUID NOT NULL UNIQUE,
    
    -- Região de atuação (TENANT)
    region VARCHAR(100) NOT NULL,
    region_code VARCHAR(20),
    
    -- Comissão e financeiro
    commission_percentage DECIMAL(5, 2) NOT NULL DEFAULT 10.00,
    total_commission DECIMAL(12, 2) DEFAULT 0.00,
    
    -- Métricas de gestão
    total_clients_managed INTEGER DEFAULT 0,
    total_couriers_managed INTEGER DEFAULT 0,
    total_deliveries_managed INTEGER DEFAULT 0,
    
    -- Parceria municipal (opcional)
    partnership_id BIGINT,
    
    -- Status
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    
    -- Foreign Keys
    CONSTRAINT fk_adm_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_adm_partnership FOREIGN KEY (partnership_id) REFERENCES municipal_partnerships(id) ON DELETE SET NULL,
    
    -- Constraints
    CONSTRAINT chk_adm_commission CHECK (commission_percentage >= 0 AND commission_percentage <= 100),
    CONSTRAINT chk_adm_total_commission CHECK (total_commission >= 0),
    CONSTRAINT chk_adm_total_clients CHECK (total_clients_managed >= 0),
    CONSTRAINT chk_adm_total_couriers CHECK (total_couriers_managed >= 0),
    CONSTRAINT chk_adm_total_deliveries CHECK (total_deliveries_managed >= 0),
    CONSTRAINT chk_adm_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'SUSPENDED'))
);

-- Índices
CREATE INDEX idx_adm_user ON adm_profiles(user_id);
CREATE INDEX idx_adm_region ON adm_profiles(region);
CREATE INDEX idx_adm_region_code ON adm_profiles(region_code);
CREATE INDEX idx_adm_partnership ON adm_profiles(partnership_id);
CREATE INDEX idx_adm_status ON adm_profiles(status);

-- Comentários
COMMENT ON TABLE adm_profiles IS 'Perfil especializado para usuários com role ADM (gerente local - TENANT)';
COMMENT ON COLUMN adm_profiles.user_id IS 'FK para users - deve ter role = ADM';
COMMENT ON COLUMN adm_profiles.region IS 'Região de atuação do ADM - usado para multi-tenancy';
COMMENT ON COLUMN adm_profiles.commission_percentage IS 'Percentual de comissão sobre entregas (0 a 100)';
COMMENT ON COLUMN adm_profiles.status IS 'Status do ADM: ACTIVE, INACTIVE, SUSPENDED';
