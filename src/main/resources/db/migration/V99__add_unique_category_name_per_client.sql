-- V99: Impede categorias duplicadas por restaurante
CREATE UNIQUE INDEX idx_product_categories_client_name
ON product_categories(client_id, LOWER(name));
