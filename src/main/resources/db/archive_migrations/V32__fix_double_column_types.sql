-- ============================================================================
-- V32: Corrigir tipos de colunas DOUBLE (latitude/longitude)
-- ============================================================================
-- Converter colunas DECIMAL para DOUBLE PRECISION (PostgreSQL Float)
-- Para resolver conflito entre JPA entity (Double) e schema (DECIMAL)
-- ============================================================================

-- ============================================================================
-- TABELA: USERS (latitude/longitude)
-- ============================================================================

ALTER TABLE users 
ALTER COLUMN latitude TYPE DOUBLE PRECISION,
ALTER COLUMN longitude TYPE DOUBLE PRECISION;

-- ============================================================================
-- TABELA: COURIER_PROFILES (current_latitude/current_longitude)
-- ============================================================================

ALTER TABLE courier_profiles 
ALTER COLUMN current_latitude TYPE DOUBLE PRECISION,
ALTER COLUMN current_longitude TYPE DOUBLE PRECISION;

-- ============================================================================
-- TABELA: DELIVERIES (from_lat/from_lng/to_lat/to_lng)
-- ============================================================================

ALTER TABLE deliveries 
ALTER COLUMN from_lat TYPE DOUBLE PRECISION,
ALTER COLUMN from_lng TYPE DOUBLE PRECISION,
ALTER COLUMN to_lat TYPE DOUBLE PRECISION,
ALTER COLUMN to_lng TYPE DOUBLE PRECISION;

-- ============================================================================
-- COMENTÁRIOS
-- ============================================================================

COMMENT ON COLUMN users.latitude IS 'Latitude da localização do usuário (DOUBLE PRECISION)';
COMMENT ON COLUMN users.longitude IS 'Longitude da localização do usuário (DOUBLE PRECISION)';

COMMENT ON COLUMN courier_profiles.current_latitude IS 'Latitude atual do motoboy (DOUBLE PRECISION)';
COMMENT ON COLUMN courier_profiles.current_longitude IS 'Longitude atual do motoboy (DOUBLE PRECISION)';

COMMENT ON COLUMN deliveries.from_lat IS 'Latitude do endereço de origem (DOUBLE PRECISION)';
COMMENT ON COLUMN deliveries.from_lng IS 'Longitude do endereço de origem (DOUBLE PRECISION)';
COMMENT ON COLUMN deliveries.to_lat IS 'Latitude do endereço de destino (DOUBLE PRECISION)';
COMMENT ON COLUMN deliveries.to_lng IS 'Longitude do endereço de destino (DOUBLE PRECISION)';
