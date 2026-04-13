-- V100: Zapi-Food — Endereço de entrega no pedido
ALTER TABLE orders ADD COLUMN delivery_address TEXT;
ALTER TABLE orders ADD COLUMN delivery_latitude DOUBLE PRECISION;
ALTER TABLE orders ADD COLUMN delivery_longitude DOUBLE PRECISION;
