-- V58: Adiciona campo in_transit_at e outros timestamps à tabela deliveries
-- Também adiciona o campo cancellation_reason se não existir

-- Adicionar coluna in_transit_at
ALTER TABLE deliveries 
ADD COLUMN IF NOT EXISTS in_transit_at TIMESTAMP WITHOUT TIME ZONE;

-- Adicionar coluna accepted_at se não existir
ALTER TABLE deliveries 
ADD COLUMN IF NOT EXISTS accepted_at TIMESTAMP WITHOUT TIME ZONE;

-- Adicionar coluna cancellation_reason se não existir
ALTER TABLE deliveries 
ADD COLUMN IF NOT EXISTS cancellation_reason TEXT;

-- Comentários nas colunas
COMMENT ON COLUMN deliveries.accepted_at IS 'Data/hora em que o courier aceitou a delivery';
COMMENT ON COLUMN deliveries.in_transit_at IS 'Data/hora em que o courier iniciou o transporte';
COMMENT ON COLUMN deliveries.cancellation_reason IS 'Motivo do cancelamento da delivery';
