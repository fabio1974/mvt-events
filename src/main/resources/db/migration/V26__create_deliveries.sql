-- ============================================================================
-- V26: Criar tabela deliveries
-- ============================================================================
-- Entidade CORE do Zapi10 - representa uma entrega
-- Substitui a entidade registrations do MVT Events
-- TENANT: ADM (todas queries devem filtrar por adm_id)
-- ============================================================================

CREATE TABLE deliveries (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Atores (TENANT via adm_id)
    client_id UUID NOT NULL,
    courier_id UUID,
    adm_id UUID, -- Denormalizado do ADM principal do courier
    
    -- Origem
    from_address TEXT NOT NULL,
    from_lat DECIMAL(10, 7),
    from_lng DECIMAL(10, 7),
    
    -- Destino
    to_address TEXT NOT NULL,
    to_lat DECIMAL(10, 7),
    to_lng DECIMAL(10, 7),
    
    -- Detalhes da entrega
    distance_km DECIMAL(6, 2),
    estimated_time_minutes INTEGER,
    item_description VARCHAR(500),
    recipient_name VARCHAR(150),
    recipient_phone VARCHAR(20),
    
    -- Preço
    total_amount DECIMAL(10, 2) NOT NULL,
    
    -- Status e timestamps
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    accepted_at TIMESTAMP,
    picked_up_at TIMESTAMP,
    completed_at TIMESTAMP,
    cancelled_at TIMESTAMP,
    cancellation_reason TEXT,
    
    -- Parceria municipal (opcional)
    partnership_id BIGINT,
    
    -- Foreign Keys
    CONSTRAINT fk_delivery_client FOREIGN KEY (client_id) REFERENCES users(id) ON DELETE RESTRICT,
    CONSTRAINT fk_delivery_courier FOREIGN KEY (courier_id) REFERENCES users(id) ON DELETE RESTRICT,
    CONSTRAINT fk_delivery_adm FOREIGN KEY (adm_id) REFERENCES users(id) ON DELETE RESTRICT,
    CONSTRAINT fk_delivery_partnership FOREIGN KEY (partnership_id) REFERENCES municipal_partnerships(id) ON DELETE SET NULL,
    
    -- Constraints
    CONSTRAINT chk_delivery_total_amount CHECK (total_amount >= 0.01),
    CONSTRAINT chk_delivery_distance CHECK (distance_km IS NULL OR distance_km >= 0),
    CONSTRAINT chk_delivery_estimated_time CHECK (estimated_time_minutes IS NULL OR estimated_time_minutes >= 0),
    CONSTRAINT chk_delivery_status CHECK (status IN ('PENDING', 'ACCEPTED', 'PICKED_UP', 'IN_TRANSIT', 'COMPLETED', 'CANCELLED'))
);

-- Índices (TENANT e queries)
CREATE INDEX idx_delivery_client ON deliveries(client_id);
CREATE INDEX idx_delivery_courier ON deliveries(courier_id);
CREATE INDEX idx_delivery_adm ON deliveries(adm_id); -- TENANT
CREATE INDEX idx_delivery_status ON deliveries(status);
CREATE INDEX idx_delivery_created_at ON deliveries(created_at DESC);
CREATE INDEX idx_delivery_completed_at ON deliveries(completed_at DESC) WHERE completed_at IS NOT NULL;
CREATE INDEX idx_delivery_partnership ON deliveries(partnership_id);

-- Índice composto para queries por ADM + status (TENANT)
CREATE INDEX idx_delivery_adm_status ON deliveries(adm_id, status);

-- Comentários
COMMENT ON TABLE deliveries IS 'Entidade CORE do Zapi10 - entregas/deliveries (TENANT via adm_id)';
COMMENT ON COLUMN deliveries.adm_id IS 'ADM responsável (denormalizado do ADM principal do courier) - usado para TENANT';
COMMENT ON COLUMN deliveries.status IS 'Status: PENDING, ACCEPTED, PICKED_UP, IN_TRANSIT, COMPLETED, CANCELLED';
