-- Distância mínima (em metros) entre origem e destino para validar
-- pedidos Zapi-Food, corridas de entrega e corridas de passageiro.
-- Serve pra filtrar ruído de GPS (oscilação ~30-50m em smartphone).
-- Default 50m — admin pode ajustar em runtime via CRUD de SiteConfiguration.
-- Zero desliga a validação por completo.
ALTER TABLE site_configurations
    ADD COLUMN min_order_distance_meters INTEGER NOT NULL DEFAULT 50;
