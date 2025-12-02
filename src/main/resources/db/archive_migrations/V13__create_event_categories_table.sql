-- V13: Create event_categories table
-- Categories define different groups within an event (e.g., age groups, gender divisions, distance categories)
-- Simplified structure with only essential fields for age/gender/distance/pricing/limits
-- tenant_id references the event (events are tenants in this multi-tenant system)

-- Drop table if exists (useful for development/testing)
DROP TABLE IF EXISTS event_categories CASCADE;

CREATE TABLE event_categories (
    id BIGSERIAL PRIMARY KEY,
    event_id BIGINT NOT NULL,
    tenant_id BIGINT NOT NULL, -- References event_id (events are tenants)
    name VARCHAR(100) NOT NULL,
    min_age INTEGER,
    max_age INTEGER,
    gender VARCHAR(20),
    distance DECIMAL(10,2),
    distance_unit VARCHAR(10),
    price DECIMAL(10,2) NOT NULL,
    max_participants INTEGER,
    current_participants INTEGER DEFAULT 0,
    is_active BOOLEAN DEFAULT true,
    observations TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_event_category_event FOREIGN KEY (event_id) REFERENCES events(id) ON DELETE CASCADE,
    CONSTRAINT fk_event_category_tenant FOREIGN KEY (tenant_id) REFERENCES events(id) ON DELETE CASCADE,
    CONSTRAINT chk_age_range CHECK (min_age IS NULL OR max_age IS NULL OR min_age <= max_age),
    CONSTRAINT chk_current_participants CHECK (current_participants >= 0),
    CONSTRAINT chk_max_participants CHECK (max_participants IS NULL OR max_participants > 0)
);

CREATE INDEX idx_event_categories_event_id ON event_categories(event_id);
CREATE INDEX idx_event_categories_tenant_id ON event_categories(tenant_id);
CREATE INDEX idx_event_categories_is_active ON event_categories(is_active);
