-- Add actual_route column to store the real GPS route traced during delivery
-- LineString geometry in SRID 4326 (WGS 84 / GPS standard)
-- Points are appended as the courier moves between pickup and completion
ALTER TABLE deliveries ADD COLUMN actual_route geometry(LineString, 4326);

-- Spatial index for efficient geo queries (bounding box, intersections, etc.)
CREATE INDEX idx_deliveries_actual_route ON deliveries USING GIST (actual_route);
