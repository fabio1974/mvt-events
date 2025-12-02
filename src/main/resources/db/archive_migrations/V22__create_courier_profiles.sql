-- ============================================================================
-- V22: Criar tabela courier_profiles
-- ============================================================================
-- Perfil especializado para usuários COURIER (motoboy)
-- Relacionamento 1:1 com users
-- ============================================================================

CREATE TABLE courier_profiles (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Relacionamento 1:1 com User
    user_id UUID NOT NULL UNIQUE,
    
    -- Veículo
    vehicle_type VARCHAR(20),
    vehicle_plate VARCHAR(10),
    vehicle_model VARCHAR(50),
    vehicle_color VARCHAR(30),
    
    -- Métricas de performance
    rating DECIMAL(3, 2) DEFAULT 0.00,
    total_deliveries INTEGER DEFAULT 0,
    completed_deliveries INTEGER DEFAULT 0,
    cancelled_deliveries INTEGER DEFAULT 0,
    
    -- Status e localização
    status VARCHAR(20) NOT NULL DEFAULT 'OFFLINE',
    last_location_update TIMESTAMP,
    current_latitude DECIMAL(10, 7),
    current_longitude DECIMAL(10, 7),
    
    -- Foreign Keys
    CONSTRAINT fk_courier_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    
    -- Constraints
    CONSTRAINT chk_courier_rating CHECK (rating >= 0 AND rating <= 5),
    CONSTRAINT chk_courier_total_deliveries CHECK (total_deliveries >= 0),
    CONSTRAINT chk_courier_completed_deliveries CHECK (completed_deliveries >= 0),
    CONSTRAINT chk_courier_cancelled_deliveries CHECK (cancelled_deliveries >= 0),
    CONSTRAINT chk_courier_status CHECK (status IN ('AVAILABLE', 'ON_DELIVERY', 'OFFLINE', 'SUSPENDED'))
);

-- Índices
CREATE INDEX idx_courier_user ON courier_profiles(user_id);
CREATE INDEX idx_courier_status ON courier_profiles(status);
CREATE INDEX idx_courier_rating ON courier_profiles(rating DESC);
CREATE INDEX idx_courier_location ON courier_profiles(current_latitude, current_longitude) 
    WHERE current_latitude IS NOT NULL AND current_longitude IS NOT NULL;

-- Comentários
COMMENT ON TABLE courier_profiles IS 'Perfil especializado para usuários com role COURIER';
COMMENT ON COLUMN courier_profiles.user_id IS 'FK para users - deve ter role = COURIER';
COMMENT ON COLUMN courier_profiles.status IS 'Status do courier: AVAILABLE, ON_DELIVERY, OFFLINE, SUSPENDED';
COMMENT ON COLUMN courier_profiles.rating IS 'Avaliação média do courier (0 a 5)';
