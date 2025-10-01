-- V9: Create cities table with correct schema
-- Description: Creates the cities table to store Brazilian cities data
-- Data will be populated automatically on application startup via IBGE API

-- ============================================================================
-- CREATE CITIES TABLE
-- ============================================================================

-- Drop table if it exists to ensure clean state
DROP TABLE IF EXISTS cities CASCADE;


CREATE TABLE cities (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    state VARCHAR(50) NOT NULL,
    state_code VARCHAR(2) NOT NULL,
    ibge_code VARCHAR(10) UNIQUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
-- ============================================================================
-- CREATE INDEXES FOR PERFORMANCE
-- ============================================================================

-- Index for city name searches (case-insensitive)
CREATE INDEX idx_cities_name_lower ON cities (LOWER(name));

-- Index for state filtering
CREATE INDEX idx_cities_state_code ON cities (state_code);

-- Index for IBGE code lookups
CREATE INDEX idx_cities_ibge_code ON cities (ibge_code);

-- Composite index for state + city searches
CREATE INDEX idx_cities_state_name ON cities (state_code, LOWER(name));

-- ============================================================================
-- ADD COMMENTS FOR DOCUMENTATION
-- ============================================================================

COMMENT ON TABLE cities IS 'Brazilian cities data populated automatically from IBGE API on startup';
COMMENT ON COLUMN cities.name IS 'City name';
COMMENT ON COLUMN cities.state IS 'Full state name';
COMMENT ON COLUMN cities.state_code IS 'Two-letter state code (UF)';
COMMENT ON COLUMN cities.ibge_code IS 'Official IBGE municipality code';
COMMENT ON COLUMN cities.created_at IS 'Record creation timestamp';
COMMENT ON COLUMN cities.updated_at IS 'Last update timestamp';