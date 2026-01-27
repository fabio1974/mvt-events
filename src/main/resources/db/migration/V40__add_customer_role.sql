-- V40: Adicionar role CUSTOMER à constraint de roles na tabela users
-- CUSTOMER: Cliente avulso/consumidor final (sem vínculo com Organization)

-- Remover constraint antiga
ALTER TABLE users DROP CONSTRAINT IF EXISTS users_role_check;

-- Adicionar nova constraint com CUSTOMER
ALTER TABLE users ADD CONSTRAINT users_role_check 
CHECK (role IN ('USER', 'ORGANIZER', 'ADMIN', 'CLIENT', 'COURIER', 'ADM', 'CUSTOMER'));

-- Comentário atualizado
COMMENT ON CONSTRAINT users_role_check ON users IS 'Valores válidos: USER (atleta legado), ORGANIZER (dono da Organization), ADMIN (admin sistema), CLIENT (cliente corporativo), COURIER (entregador), ADM (gerente local), CUSTOMER (cliente avulso)';
