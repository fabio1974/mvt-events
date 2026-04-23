-- Adiciona flag para desabilitar impressões automáticas (rodada de mesa, aceite de pedido food).
-- Default TRUE mantém comportamento atual.
ALTER TABLE store_profiles
    ADD COLUMN auto_print_enabled BOOLEAN NOT NULL DEFAULT TRUE;
