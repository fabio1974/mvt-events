-- V126: Preços por canal de venda
-- Cada produto pode ter preço distinto no Zapi-Food (delivery próprio) e no iFood.
-- Se nulo, o fluxo usa `price` (preço de balcão/referência) como fallback.

ALTER TABLE products
    ADD COLUMN delivery_price DECIMAL(10,2),
    ADD COLUMN ifood_price    DECIMAL(10,2);

COMMENT ON COLUMN products.price IS 'Preço padrão/balcão. Fallback quando delivery_price ou ifood_price não estiverem preenchidos.';
COMMENT ON COLUMN products.delivery_price IS 'Preço no canal Zapi-Food (delivery próprio). NULL = usar price.';
COMMENT ON COLUMN products.ifood_price IS 'Preço no canal iFood. NULL = usar price.';
