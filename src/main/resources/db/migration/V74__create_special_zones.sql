-- Cria tabela de zonas especiais (periculosidade e alta renda)
CREATE TABLE special_zones (
    id BIGSERIAL PRIMARY KEY,
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    address TEXT NOT NULL,
    zone_type VARCHAR(20) NOT NULL CHECK (zone_type IN ('DANGER', 'HIGH_INCOME')),
    is_active BOOLEAN NOT NULL DEFAULT true,
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT check_valid_coordinates CHECK (
        latitude BETWEEN -90 AND 90 AND
        longitude BETWEEN -180 AND 180
    )
);

-- Índices para performance de busca geográfica
CREATE INDEX idx_special_zones_coordinates ON special_zones(latitude, longitude);
CREATE INDEX idx_special_zones_type ON special_zones(zone_type);
CREATE INDEX idx_special_zones_active ON special_zones(is_active);

-- Índice composto para consultas filtradas
CREATE INDEX idx_special_zones_active_type ON special_zones(is_active, zone_type) WHERE is_active = true;

-- Comentários
COMMENT ON TABLE special_zones IS 'Zonas especiais que afetam o cálculo do frete (periculosidade ou alta renda)';
COMMENT ON COLUMN special_zones.latitude IS 'Latitude do ponto central da zona';
COMMENT ON COLUMN special_zones.longitude IS 'Longitude do ponto central da zona';
COMMENT ON COLUMN special_zones.address IS 'Endereço descritivo da zona';
COMMENT ON COLUMN special_zones.zone_type IS 'DANGER (periculosidade) ou HIGH_INCOME (alta renda)';
COMMENT ON COLUMN special_zones.is_active IS 'Indica se a zona está ativa para cálculos';
COMMENT ON COLUMN special_zones.notes IS 'Observações sobre a zona';

-- Exemplos de dados (opcional - podem ser removidos em produção)
-- INSERT INTO special_zones (latitude, longitude, address, zone_type, is_active, notes)
-- VALUES 
--     (-23.5505, -46.6333, 'Centro - São Paulo, SP', 'DANGER', true, 'Região central com alta incidência de assaltos'),
--     (-23.5629, -46.6544, 'Jardins - São Paulo, SP', 'HIGH_INCOME', true, 'Bairro nobre da zona sul');
