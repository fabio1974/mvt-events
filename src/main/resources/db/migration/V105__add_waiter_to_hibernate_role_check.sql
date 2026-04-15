-- Adicionar WAITER à constraint gerada pelo Hibernate (users_role_check)

ALTER TABLE users DROP CONSTRAINT IF EXISTS users_role_check;

ALTER TABLE users ADD CONSTRAINT users_role_check
    CHECK (role::text = ANY (ARRAY[
        'USER', 'ORGANIZER', 'ADMIN', 'CLIENT', 'COURIER', 'ADM', 'CUSTOMER', 'WAITER'
    ]::text[]));
