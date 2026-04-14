-- Módulo de Pedidos de Mesa (Table Orders)
-- WAITER role, mesas, vínculo N:N garçom↔cliente, order_type na order

-- 1. Tabela de mesas do estabelecimento
CREATE TABLE restaurant_tables (
    id BIGSERIAL PRIMARY KEY,
    client_id UUID NOT NULL REFERENCES users(id),
    number INT NOT NULL,
    label VARCHAR(50),
    seats INT,
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(client_id, number)
);
CREATE INDEX idx_restaurant_tables_client ON restaurant_tables(client_id);

-- 2. Vínculo N:N entre garçom (WAITER) e estabelecimento (CLIENT)
CREATE TABLE client_waiters (
    id BIGSERIAL PRIMARY KEY,
    client_id UUID NOT NULL REFERENCES users(id),
    waiter_id UUID NOT NULL REFERENCES users(id),
    pin VARCHAR(6),
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(client_id, waiter_id),
    UNIQUE(client_id, pin)
);
CREATE INDEX idx_client_waiters_client ON client_waiters(client_id);
CREATE INDEX idx_client_waiters_waiter ON client_waiters(waiter_id);

-- 3. Novos campos na tabela orders
ALTER TABLE orders ADD COLUMN order_type VARCHAR(20) NOT NULL DEFAULT 'DELIVERY';
ALTER TABLE orders ADD COLUMN waiter_id UUID REFERENCES users(id);
ALTER TABLE orders ADD COLUMN table_id BIGINT REFERENCES restaurant_tables(id);
CREATE INDEX idx_orders_type ON orders(order_type);
CREATE INDEX idx_orders_waiter ON orders(waiter_id) WHERE waiter_id IS NOT NULL;
CREATE INDEX idx_orders_table ON orders(table_id) WHERE table_id IS NOT NULL;

-- 4. Feature flag no store_profile
ALTER TABLE store_profiles ADD COLUMN table_orders_enabled BOOLEAN NOT NULL DEFAULT false;
