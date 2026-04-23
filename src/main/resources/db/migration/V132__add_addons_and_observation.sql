-- Fase 2 do refactor de mesas: adicionais por item + observação por item.

-- Flag pra marcar Products que podem ser usados como adicionais.
-- Products com is_addon=true continuam no cardápio geral, mas o mobile filtra
-- pra mostrá-los separadamente na seção "Adicionais" do ProductDetailScreen.
ALTER TABLE products
    ADD COLUMN is_addon BOOLEAN NOT NULL DEFAULT FALSE;

-- Observação por item (coexiste com `notes` legado).
-- Em pedidos de mesa, `observation` é o campo canônico per-item a partir da fase 2.
-- `notes` continua existindo pra compatibilidade com pedidos antigos e uso futuro
-- (ex: observação do cliente final em Zapi-Food delivery).
ALTER TABLE order_items
    ADD COLUMN observation TEXT;

-- Adicionais pendurados em um OrderItem (N por item).
-- unit_price é snapshot no momento do pedido (mesmo padrão de order_items.unit_price).
CREATE TABLE order_item_addons (
    id BIGSERIAL PRIMARY KEY,
    order_item_id BIGINT NOT NULL REFERENCES order_items(id) ON DELETE CASCADE,
    product_id BIGINT NOT NULL REFERENCES products(id),
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    unit_price NUMERIC(10, 2) NOT NULL
);

CREATE INDEX idx_order_item_addons_order_item ON order_item_addons(order_item_id);
CREATE INDEX idx_order_item_addons_product ON order_item_addons(product_id);
