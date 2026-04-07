-- Migra actual_route e approach_route de LineString para LineStringM (com dimensão M = epoch seconds).
-- Permite armazenar o timestamp de captura de cada ponto GPS.
-- planned_route e approach_planned_route permanecem LineString (rotas teóricas, sem GPS real).

-- 1. Converter actual_route: LineString → LineStringM (M = 0 para pontos existentes)
ALTER TABLE deliveries
    ALTER COLUMN actual_route TYPE geometry(LineStringM, 4326)
    USING CASE
        WHEN actual_route IS NOT NULL THEN ST_Force3DM(actual_route)
        ELSE NULL
    END;

-- 2. Converter approach_route: LineString → LineStringM
ALTER TABLE deliveries
    ALTER COLUMN approach_route TYPE geometry(LineStringM, 4326)
    USING CASE
        WHEN approach_route IS NOT NULL THEN ST_Force3DM(approach_route)
        ELSE NULL
    END;

-- Recriar índices (tipo mudou)
DROP INDEX IF EXISTS idx_deliveries_actual_route;
CREATE INDEX idx_deliveries_actual_route ON deliveries USING GIST (actual_route) WHERE actual_route IS NOT NULL;

DROP INDEX IF EXISTS idx_deliveries_approach_route;
CREATE INDEX idx_deliveries_approach_route ON deliveries USING GIST (approach_route) WHERE approach_route IS NOT NULL;
