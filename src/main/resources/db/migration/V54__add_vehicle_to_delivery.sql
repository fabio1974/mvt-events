-- Adiciona coluna vehicle_id na tabela deliveries para registrar o veículo usado na entrega
-- O veículo é setado automaticamente no momento do aceite pelo courier

ALTER TABLE deliveries ADD COLUMN vehicle_id BIGINT;

-- Adiciona constraint de chave estrangeira para vehicles
ALTER TABLE deliveries 
ADD CONSTRAINT fk_deliveries_vehicle 
FOREIGN KEY (vehicle_id) 
REFERENCES vehicles(id);

-- Cria índice para melhorar performance de consultas
CREATE INDEX idx_deliveries_vehicle_id ON deliveries(vehicle_id);

-- Comentários explicativos
COMMENT ON COLUMN deliveries.vehicle_id IS 'Veículo usado na entrega, setado automaticamente no aceite do courier';
