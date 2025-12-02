-- Create indexes to improve search performance
CREATE INDEX IF NOT EXISTS idx_cities_name_lower ON cities (LOWER(name));
CREATE INDEX IF NOT EXISTS idx_cities_state_code ON cities (state_code);

-- Add trigram extension for better text search (if available)
-- CREATE EXTENSION IF NOT EXISTS pg_trgm;
-- CREATE INDEX IF NOT EXISTS idx_cities_name_gin ON cities USING gin (name gin_trgm_ops);