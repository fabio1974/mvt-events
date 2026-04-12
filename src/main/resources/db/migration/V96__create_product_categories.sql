-- V96: Zapi-Food — Categorias do cardápio

CREATE TABLE product_categories (
    id BIGSERIAL PRIMARY KEY,
    client_id UUID NOT NULL REFERENCES users(id),
    name VARCHAR(100) NOT NULL,
    description TEXT,
    display_order INT NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT true,
    image_url VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_product_categories_client ON product_categories(client_id);

COMMENT ON TABLE product_categories IS 'Categorias do cardápio — módulo Zapi-Food (ex: Pizzas, Bebidas, Sobremesas).';
