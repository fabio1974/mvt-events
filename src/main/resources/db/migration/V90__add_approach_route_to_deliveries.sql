-- Rota de aproximação do entregador (fase ACCEPTED → PICKUP).
-- Separada da actual_route (fase IN_TRANSIT → COMPLETED) para eliminar o algoritmo de trim.
ALTER TABLE deliveries ADD COLUMN IF NOT EXISTS approach_route geometry(LineString, 4326);

CREATE INDEX IF NOT EXISTS idx_deliveries_approach_route
    ON deliveries USING GIST (approach_route)
    WHERE approach_route IS NOT NULL;
