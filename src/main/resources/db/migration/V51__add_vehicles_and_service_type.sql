-- V51: Adiciona tabela vehicles e campo service_type em users

-- Criar tabela vehicles
CREATE TABLE vehicles (
    id BIGSERIAL PRIMARY KEY,
    owner_id UUID NOT NULL,
    type VARCHAR(20) NOT NULL CHECK (type IN ('MOTORCYCLE', 'CAR')),
    plate VARCHAR(10) NOT NULL UNIQUE,
    brand VARCHAR(50) NOT NULL,
    model VARCHAR(50) NOT NULL,
    color VARCHAR(30) NOT NULL,
    year VARCHAR(4),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_vehicles_owner FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Índices para performance
CREATE INDEX idx_vehicles_owner_id ON vehicles(owner_id);
CREATE INDEX idx_vehicles_type ON vehicles(type);
CREATE INDEX idx_vehicles_plate ON vehicles(plate);
CREATE INDEX idx_vehicles_is_active ON vehicles(is_active);

-- Adicionar campo service_type em users (apenas para COURIER)
ALTER TABLE users ADD COLUMN service_type VARCHAR(30) CHECK (service_type IN ('DELIVERY', 'PASSENGER_TRANSPORT', 'BOTH'));

-- Comentários
COMMENT ON TABLE vehicles IS 'Veículos dos motoristas/entregadores';
COMMENT ON COLUMN vehicles.owner_id IS 'Proprietário do veículo (USER)';
COMMENT ON COLUMN vehicles.type IS 'Tipo do veículo: MOTORCYCLE (Moto) ou CAR (Automóvel)';
COMMENT ON COLUMN vehicles.plate IS 'Placa do veículo (única)';
COMMENT ON COLUMN vehicles.is_active IS 'Indica se o veículo está ativo';
COMMENT ON COLUMN users.service_type IS 'Tipo de serviço prestado pelo COURIER: DELIVERY (Entrega), PASSENGER_TRANSPORT (Transporte de Passageiro), BOTH (Ambos)';
