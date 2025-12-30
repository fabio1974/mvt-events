-- Remove state column from users table
ALTER TABLE users DROP COLUMN IF EXISTS state;
