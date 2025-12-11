-- V16: Remove city_id, city_old e country da tabela users
-- Data: 2025-12-11
-- Motivo: Cidade agora está apenas em addresses (1:1 com User)
--         Country não é mais necessário (todos os dados são do Brasil)

-- 1. Remover constraint FK se existir
ALTER TABLE users DROP CONSTRAINT IF EXISTS fk_users_city;

-- 2. Remover índice se existir
DROP INDEX IF EXISTS idx_users_city_id;

-- 3. Remover colunas
ALTER TABLE users DROP COLUMN IF EXISTS city_id;
ALTER TABLE users DROP COLUMN IF EXISTS city_old;
ALTER TABLE users DROP COLUMN IF EXISTS country;
