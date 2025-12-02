-- V38: Convert ADM role to ORGANIZER
-- ADM was meant to be "Gerente" but will be consolidated into ORGANIZER role

-- Convert all ADM users to ORGANIZER
UPDATE users SET role = 'ORGANIZER' WHERE role = 'ADM';

-- Update role check constraint to remove ADM
ALTER TABLE users DROP CONSTRAINT IF EXISTS chk_user_role;
ALTER TABLE users ADD CONSTRAINT chk_user_role
    CHECK (role IN ('USER', 'ORGANIZER', 'ADMIN', 'CLIENT', 'COURIER'));
