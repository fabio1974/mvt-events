-- Rota planejada da fase de aproximação (courier → pickup).
-- Recalculada pelo PlannedRouteService enquanto status = ACCEPTED.
-- Separada de planned_route (rota principal pickup → paradas), que é
-- persistida na criação da corrida e recalculada apenas na fase IN_TRANSIT.
ALTER TABLE deliveries
    ADD COLUMN IF NOT EXISTS approach_planned_route geometry(LineString, 4326);

CREATE INDEX IF NOT EXISTS idx_deliveries_approach_planned_route
    ON deliveries USING gist (approach_planned_route);
