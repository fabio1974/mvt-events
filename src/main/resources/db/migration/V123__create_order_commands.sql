-- V123: Comandas dentro de pedido de mesa (split de conta por pessoa/comanda)
-- Cada comanda tem display_number local ao pedido (1, 2, 3...) e opcionalmente um nome.
-- Items podem ser atribuídos a uma comanda (null = compartilhado entre todos).

CREATE TABLE order_commands (
    id              BIGSERIAL PRIMARY KEY,
    order_id        BIGINT NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    display_number  INTEGER NOT NULL,
    name            VARCHAR(50),
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),

    CONSTRAINT uk_order_commands_order_display UNIQUE (order_id, display_number)
);

CREATE INDEX idx_order_commands_order ON order_commands(order_id);

ALTER TABLE order_items
    ADD COLUMN command_id BIGINT REFERENCES order_commands(id) ON DELETE SET NULL;

CREATE INDEX idx_order_items_command ON order_items(command_id);

COMMENT ON TABLE order_commands IS 'Comandas dentro de um pedido de mesa — permite split por pessoa';
COMMENT ON COLUMN order_commands.display_number IS 'Número local no pedido (1, 2, 3...) — estável, não renumera em delete';
COMMENT ON COLUMN order_commands.name IS 'Nome opcional da pessoa (ex: Pedro); null → mostrar "Comanda #N"';
COMMENT ON COLUMN order_items.command_id IS 'Comanda à qual o item pertence; null = compartilhado entre todos';
