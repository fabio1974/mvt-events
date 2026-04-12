-- V98: Zapi-Food — Pedidos e itens de pedido

CREATE TABLE orders (
    id BIGSERIAL PRIMARY KEY,
    customer_id UUID NOT NULL REFERENCES users(id),
    client_id UUID NOT NULL REFERENCES users(id),
    delivery_id BIGINT REFERENCES deliveries(id),
    status VARCHAR(20) NOT NULL DEFAULT 'PLACED',
    subtotal NUMERIC(10,2) NOT NULL,
    delivery_fee NUMERIC(10,2),
    total NUMERIC(10,2) NOT NULL,
    notes TEXT,
    estimated_preparation_minutes INT,
    accepted_at TIMESTAMPTZ,
    preparing_at TIMESTAMPTZ,
    ready_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    cancelled_at TIMESTAMPTZ,
    cancellation_reason TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_orders_customer ON orders(customer_id);
CREATE INDEX idx_orders_client ON orders(client_id);
CREATE INDEX idx_orders_status ON orders(status);
CREATE INDEX idx_orders_delivery ON orders(delivery_id) WHERE delivery_id IS NOT NULL;

CREATE TABLE order_items (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    product_id BIGINT NOT NULL REFERENCES products(id),
    quantity INT NOT NULL DEFAULT 1,
    unit_price NUMERIC(10,2) NOT NULL,
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_order_items_order ON order_items(order_id);

COMMENT ON TABLE orders IS 'Pedidos do módulo Zapi-Food. Status: PLACED → ACCEPTED → PREPARING → READY → DELIVERING → COMPLETED / CANCELLED.';
COMMENT ON TABLE order_items IS 'Itens do pedido — snapshot do preço no momento da compra.';
