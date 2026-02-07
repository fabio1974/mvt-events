-- Remove coluna is_active_vehicle e simplifica para usar apenas is_active
-- is_active agora indica tanto se o veículo está ativo quanto se é o principal

-- Remove constraint antiga
DROP INDEX IF EXISTS idx_vehicles_unique_active_per_owner;

-- Garante que cada usuário tenha apenas um veículo ativo
-- Mantém apenas o primeiro veículo (menor ID) como ativo para cada usuário
UPDATE vehicles v1
SET is_active = false
WHERE is_active = true
  AND EXISTS (
    SELECT 1 FROM vehicles v2
    WHERE v2.owner_id = v1.owner_id
      AND v2.is_active = true
      AND v2.id < v1.id
  );

-- Ou marca como ativo os veículos que tinham is_active_vehicle = true
UPDATE vehicles SET is_active = true WHERE is_active_vehicle = true;

-- Remove a coluna is_active_vehicle
ALTER TABLE vehicles DROP COLUMN IF EXISTS is_active_vehicle;

-- Cria nova constraint: apenas 1 veículo ativo (principal) por usuário
CREATE UNIQUE INDEX idx_vehicles_unique_active_per_owner 
ON vehicles(owner_id) WHERE is_active = true;

-- Comentário explicativo
COMMENT ON INDEX idx_vehicles_unique_active_per_owner IS 'Garante que cada usuário tenha apenas um veículo ativo (principal)';
