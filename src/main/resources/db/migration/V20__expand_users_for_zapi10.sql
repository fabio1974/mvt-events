-- ============================================================================
-- V20: Expandir tabela users para Zapi10
-- ============================================================================
-- Adiciona novos roles (CLIENT, COURIER, ADM) e campos de geolocalização
-- Mantém compatibilidade com roles existentes (USER, ORGANIZER, ADMIN)
-- ============================================================================

-- Adicionar campos de geolocalização
ALTER TABLE users ADD COLUMN IF NOT EXISTS latitude DECIMAL(10, 7);
ALTER TABLE users ADD COLUMN IF NOT EXISTS longitude DECIMAL(10, 7);

-- Comentários para documentação
COMMENT ON COLUMN users.latitude IS 'Latitude da localização do usuário (para CLIENT, COURIER, ADM)';
COMMENT ON COLUMN users.longitude IS 'Longitude da localização do usuário (para CLIENT, COURIER, ADM)';

-- Índices para queries geoespaciais (futuro)
CREATE INDEX IF NOT EXISTS idx_users_location ON users(latitude, longitude) WHERE latitude IS NOT NULL AND longitude IS NOT NULL;

-- Nota: Os novos valores de role (CLIENT, COURIER, ADM) serão aceitos automaticamente
-- pois a coluna role é VARCHAR(50) e o enum é validado apenas em nível de aplicação.
