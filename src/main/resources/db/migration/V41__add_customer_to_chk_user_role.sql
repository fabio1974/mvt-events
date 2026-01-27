-- V41: Adicionar role CUSTOMER à constraint chk_user_role
-- Este constraint estava faltando o CUSTOMER que foi adicionado em V40

-- Remover constraint antiga
ALTER TABLE users DROP CONSTRAINT IF EXISTS chk_user_role;

-- Adicionar nova constraint com CUSTOMER
ALTER TABLE users ADD CONSTRAINT chk_user_role 
CHECK (role IN ('USER', 'ORGANIZER', 'ADMIN', 'CLIENT', 'COURIER', 'ADM', 'CUSTOMER'));
