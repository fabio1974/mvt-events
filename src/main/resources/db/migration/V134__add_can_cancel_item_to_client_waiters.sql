-- Permissão do CLIENT sobre o garçom: pode cancelar item de pedido?
-- Default TRUE para manter compatibilidade (comportamento atual).
ALTER TABLE client_waiters
    ADD COLUMN can_cancel_item BOOLEAN NOT NULL DEFAULT TRUE;
