-- V124: Status de pagamento por comanda (split de pagamento)
-- Cada comanda pode ser fechada/paga independentemente.
-- A "Mesa" (items com command_id NULL) tem campos análogos em orders.

ALTER TABLE order_commands
    ADD COLUMN status          VARCHAR(30)               NOT NULL DEFAULT 'OPEN',
    ADD COLUMN payment_method  VARCHAR(20),
    ADD COLUMN paid_at         TIMESTAMP WITH TIME ZONE;

CREATE INDEX idx_order_commands_status ON order_commands(order_id, status);

-- Status da "Mesa" (items compartilhados com command_id NULL)
ALTER TABLE orders
    ADD COLUMN mesa_status          VARCHAR(30)               NOT NULL DEFAULT 'OPEN',
    ADD COLUMN mesa_payment_method  VARCHAR(20),
    ADD COLUMN mesa_paid_at         TIMESTAMP WITH TIME ZONE;

COMMENT ON COLUMN order_commands.status IS 'OPEN ou PAID';
COMMENT ON COLUMN orders.mesa_status IS 'Status dos items compartilhados (command_id NULL): OPEN ou PAID';
