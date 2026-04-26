-- Raio (em km) usado para listar estabelecimentos próximos no GET /api/stores
-- quando o customer abre o app pra fazer um pedido Zapi-Food.
-- Default 150km — admin pode ajustar em runtime via CRUD de SiteConfiguration.
ALTER TABLE site_configurations
    ADD COLUMN establishment_search_radius_km INTEGER NOT NULL DEFAULT 150;
