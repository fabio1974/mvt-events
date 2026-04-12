-- V97: Zapi-Food — Produtos/itens do cardápio

CREATE TABLE products (
    id BIGSERIAL PRIMARY KEY,
    client_id UUID NOT NULL REFERENCES users(id),
    category_id BIGINT REFERENCES product_categories(id) ON DELETE SET NULL,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    price NUMERIC(10,2) NOT NULL,
    image_url VARCHAR(500),
    available BOOLEAN NOT NULL DEFAULT true,
    preparation_time_minutes INT,
    display_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_products_client ON products(client_id);
CREATE INDEX idx_products_category ON products(category_id);
CREATE INDEX idx_products_available ON products(client_id, available) WHERE available = true;

COMMENT ON TABLE products IS 'Produtos do cardápio — módulo Zapi-Food (ex: Pizza Margherita R$ 29,90).';
