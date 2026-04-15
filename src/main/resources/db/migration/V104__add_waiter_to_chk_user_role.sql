-- Adicionar role WAITER à constraint chk_user_role
-- (faltou na V102 que criou o módulo de table orders)

ALTER TABLE users DROP CONSTRAINT IF EXISTS chk_user_role;

ALTER TABLE users ADD CONSTRAINT chk_user_role
    CHECK (role::text = ANY (ARRAY[
        'USER', 'ORGANIZER', 'ADMIN', 'CLIENT', 'COURIER', 'CUSTOMER', 'WAITER'
    ]::text[]));
