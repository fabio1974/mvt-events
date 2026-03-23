-- Adiciona rota planejada (calculada no wizard, persistida uma única vez)
-- Elimina necessidade de chamar Google Directions durante corridas ativas
ALTER TABLE deliveries ADD COLUMN IF NOT EXISTS planned_route geometry(LineString, 4326);

CREATE INDEX IF NOT EXISTS idx_deliveries_planned_route ON deliveries USING GIST (planned_route)
    WHERE planned_route IS NOT NULL;
