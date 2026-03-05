-- Adiciona campo 'blocked' para controle de segurança (login).
-- blocked = true impede login via Spring Security.
-- O campo 'enabled' existente passa a ser usado apenas para regras de preenchimento mínimo por role.
ALTER TABLE users ADD COLUMN blocked BOOLEAN NOT NULL DEFAULT false;
