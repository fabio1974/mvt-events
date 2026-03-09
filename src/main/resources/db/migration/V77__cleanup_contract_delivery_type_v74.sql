-- V77: Limpa deliveries com delivery_type = 'CONTRACT' reintroduzidas pela V74 (Apple Review demo)
-- e corrige a constraint para aceitar apenas DELIVERY e RIDE (valores válidos no enum Java).

-- 1. Converte CONTRACT → DELIVERY
UPDATE deliveries
SET delivery_type = 'DELIVERY'
WHERE delivery_type = 'CONTRACT';

-- 2. Remove a constraint antiga (criada em V55 que ainda permitia CONTRACT)
ALTER TABLE deliveries DROP CONSTRAINT IF EXISTS chk_delivery_type;

-- 3. Recria a constraint permitindo apenas os valores válidos do enum Java
ALTER TABLE deliveries
    ADD CONSTRAINT chk_delivery_type
    CHECK (delivery_type IN ('DELIVERY', 'RIDE'));
