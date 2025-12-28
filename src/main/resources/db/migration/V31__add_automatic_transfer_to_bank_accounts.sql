-- V10: Adiciona coluna automatic_transfer em bank_accounts
-- Permite controlar se transferências automáticas estão habilitadas no Pagar.me

ALTER TABLE bank_accounts
ADD COLUMN automatic_transfer BOOLEAN NOT NULL DEFAULT TRUE;

COMMENT ON COLUMN bank_accounts.automatic_transfer IS 'Flag para habilitar transferências automáticas diárias no Pagar.me (default: true)';
