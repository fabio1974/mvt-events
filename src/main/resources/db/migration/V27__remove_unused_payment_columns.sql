-- Remove colunas não utilizadas da tabela payments
-- Campos: courier_amount, adm_amount, platform_amount, organization_id

-- Remove constraints relacionadas aos campos de split
ALTER TABLE payments DROP CONSTRAINT IF EXISTS chk_payment_courier_amount;
ALTER TABLE payments DROP CONSTRAINT IF EXISTS chk_payment_adm_amount;
ALTER TABLE payments DROP CONSTRAINT IF EXISTS chk_payment_platform_amount;
ALTER TABLE payments DROP CONSTRAINT IF EXISTS chk_payment_split_sum;

-- Remove índices
DROP INDEX IF EXISTS idx_payment_courier_amount;
DROP INDEX IF EXISTS idx_payment_adm_amount;
DROP INDEX IF EXISTS idx_payment_platform_amount;

-- Remove as colunas
ALTER TABLE payments DROP COLUMN IF EXISTS courier_amount;
ALTER TABLE payments DROP COLUMN IF EXISTS adm_amount;
ALTER TABLE payments DROP COLUMN IF EXISTS platform_amount;
ALTER TABLE payments DROP COLUMN IF EXISTS organization_id;
