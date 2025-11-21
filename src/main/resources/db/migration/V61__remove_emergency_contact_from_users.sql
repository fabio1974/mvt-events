-- Remove a coluna emergency_contact da tabela users
ALTER TABLE users
DROP COLUMN IF EXISTS emergency_contact;
