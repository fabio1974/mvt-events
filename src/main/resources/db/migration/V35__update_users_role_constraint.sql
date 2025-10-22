-- ============================================================================
-- V35: Atualizar constraint de roles para incluir CLIENT, COURIER, ADM
-- ============================================================================
-- Remove o constraint antigo e adiciona um novo que inclui os roles do Zapi10

-- Drop existing role constraint
ALTER TABLE users DROP CONSTRAINT IF EXISTS users_role_check;

-- Add new role constraint with all valid roles
ALTER TABLE users ADD CONSTRAINT users_role_check 
    CHECK (role IN ('USER', 'ORGANIZER', 'ADMIN', 'CLIENT', 'COURIER', 'ADM'));

-- Add comment for documentation
COMMENT ON CONSTRAINT users_role_check ON users IS 'Valores válidos: USER (atleta), ORGANIZER (organizador), ADMIN (administrador sistema), CLIENT (cliente Zapi10), COURIER (entregador), ADM (gerente local Zapi10)';