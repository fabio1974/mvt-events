-- Migra relacionamento Order↔Delivery de OneToOne (orders.delivery_id)
-- para OneToMany (deliveries.order_id), permitindo histórico de tentativas.

-- 1. Adicionar order_id na tabela deliveries
ALTER TABLE deliveries ADD COLUMN order_id BIGINT REFERENCES orders(id);
CREATE INDEX idx_deliveries_order ON deliveries(order_id) WHERE order_id IS NOT NULL;

-- 2. Popular order_id a partir dos dados existentes
UPDATE deliveries d
SET order_id = o.id
FROM orders o
WHERE o.delivery_id = d.id;

-- 3. Remover FK e coluna delivery_id da tabela orders
ALTER TABLE orders DROP CONSTRAINT IF EXISTS orders_delivery_id_fkey;
DROP INDEX IF EXISTS idx_orders_delivery;
ALTER TABLE orders DROP COLUMN delivery_id;
