-- V4: Multi-tenant schema with Event as Tenant - BIGINT IDs
-- Architecture: Event-centric with RLS (Row Level Security)

-- Drop existing functions that might have different return types
DROP FUNCTION IF EXISTS get_current_event();

-- =====================================================
-- 1. ORGANIZATIONS (Simple entity, not tenant)
-- =====================================================
CREATE TABLE organizations (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    slug VARCHAR(100) UNIQUE NOT NULL,
    contact_email VARCHAR(255) NOT NULL,
    phone VARCHAR(20),
    website VARCHAR(255),
    description TEXT,
    logo_url VARCHAR(255),
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Index for efficient lookups
CREATE INDEX idx_organizations_slug ON organizations(slug);
CREATE INDEX idx_organizations_email ON organizations(contact_email);

-- =====================================================
-- 2. EXTEND EVENTS TABLE (Make it tenant + entity)
-- =====================================================
-- Add new columns to existing events table
ALTER TABLE events 
ADD COLUMN organization_id BIGINT,
ADD COLUMN slug VARCHAR(100) UNIQUE,
ADD COLUMN description TEXT,
ADD COLUMN event_type VARCHAR(50),
ADD COLUMN event_date DATE,
ADD COLUMN event_time TIME,
ADD COLUMN location VARCHAR(255),
ADD COLUMN address TEXT,
ADD COLUMN max_participants INTEGER,
ADD COLUMN registration_open BOOLEAN DEFAULT true,
ADD COLUMN registration_start_date TIMESTAMP,
ADD COLUMN registration_end_date TIMESTAMP,
ADD COLUMN price DECIMAL(10,2),
ADD COLUMN currency VARCHAR(3) DEFAULT 'BRL',
ADD COLUMN terms_and_conditions TEXT,
ADD COLUMN banner_url VARCHAR(255),
ADD COLUMN status VARCHAR(20) DEFAULT 'DRAFT',
ADD COLUMN updated_at TIMESTAMP DEFAULT NOW();

-- Create default organization for existing events
INSERT INTO organizations (name, slug, contact_email, description) 
VALUES (
    'Default Organization',
    'default-org',
    'admin@mvt-events.com',
    'Default organization for existing events'
);

-- Set organization_id for existing events
UPDATE events 
SET organization_id = (SELECT id FROM organizations WHERE slug = 'default-org' LIMIT 1)
WHERE organization_id IS NULL;

-- Add foreign key constraint
ALTER TABLE events 
ADD CONSTRAINT fk_events_organization 
FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE;

-- Make organization_id NOT NULL
ALTER TABLE events ALTER COLUMN organization_id SET NOT NULL;

-- Generate slugs for existing events based on name
UPDATE events 
SET slug = LOWER(REGEXP_REPLACE(name, '[^a-zA-Z0-9]+', '-', 'g'))
WHERE slug IS NULL;

-- Add constraints
ALTER TABLE events ADD CONSTRAINT events_type_check 
CHECK (event_type IN ('RUNNING', 'CYCLING', 'TRIATHLON', 'SWIMMING', 'WALKING', 
                     'TRAIL_RUNNING', 'MOUNTAIN_BIKING', 'ROAD_CYCLING', 'MARATHON', 
                     'HALF_MARATHON', 'ULTRA_MARATHON', 'OBSTACLE_RACE', 'DUATHLON', 'OTHER'));

ALTER TABLE events ADD CONSTRAINT events_status_check 
CHECK (status IN ('DRAFT', 'PUBLISHED', 'CANCELLED', 'COMPLETED'));

-- Indexes for events
CREATE INDEX idx_events_organization ON events(organization_id);
CREATE INDEX idx_events_slug ON events(slug);
CREATE INDEX idx_events_date ON events(event_date);
CREATE INDEX idx_events_type ON events(event_type);
CREATE INDEX idx_events_status ON events(status);

-- =====================================================
-- 3. ATHLETES (Cross-tenant, global)
-- =====================================================
CREATE TABLE athletes (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    name VARCHAR(255) NOT NULL,
    phone VARCHAR(20),
    birth_date DATE,
    gender VARCHAR(10), -- 'M', 'F', 'OTHER'
    document_number VARCHAR(20), -- CPF, passport, etc
    emergency_contact_name VARCHAR(255),
    emergency_contact_phone VARCHAR(20),
    address TEXT,
    city VARCHAR(100),
    state VARCHAR(100),
    country VARCHAR(100),
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Indexes for athletes
CREATE INDEX idx_athletes_email ON athletes(email);
CREATE INDEX idx_athletes_document ON athletes(document_number);

-- =====================================================
-- 4. REGISTRATIONS (Event-tenant isolated)
-- =====================================================
CREATE TABLE registrations (
    id BIGSERIAL PRIMARY KEY,
    event_id BIGINT NOT NULL REFERENCES events(id) ON DELETE CASCADE,
    athlete_id BIGINT NOT NULL REFERENCES athletes(id) ON DELETE CASCADE,
    category VARCHAR(100), -- 'Male 18-29', 'Female 30-39', etc
    team_name VARCHAR(255),
    bib_number INTEGER,
    registration_date TIMESTAMP DEFAULT NOW(),
    payment_status VARCHAR(20) DEFAULT 'PENDING', -- 'PENDING', 'PAID', 'CANCELLED', 'REFUNDED'
    amount_paid DECIMAL(10,2),
    payment_method VARCHAR(50),
    special_needs TEXT,
    t_shirt_size VARCHAR(10),
    status VARCHAR(20) DEFAULT 'ACTIVE', -- 'ACTIVE', 'CANCELLED', 'DNS', 'DNF'
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    
    -- Ensure athlete can only register once per event
    UNIQUE(event_id, athlete_id)
);

-- Indexes for registrations
CREATE INDEX idx_registrations_event ON registrations(event_id);
CREATE INDEX idx_registrations_athlete ON registrations(athlete_id);
CREATE INDEX idx_registrations_status ON registrations(payment_status);
CREATE INDEX idx_registrations_bib ON registrations(event_id, bib_number);

-- =====================================================
-- 5. RESULTS (Event-tenant isolated)
-- =====================================================
CREATE TABLE results (
    id BIGSERIAL PRIMARY KEY,
    event_id BIGINT NOT NULL REFERENCES events(id) ON DELETE CASCADE,
    registration_id BIGINT NOT NULL REFERENCES registrations(id) ON DELETE CASCADE,
    athlete_id BIGINT NOT NULL REFERENCES athletes(id) ON DELETE CASCADE,
    category VARCHAR(100),
    start_time TIMESTAMP,
    finish_time TIMESTAMP,
    elapsed_time INTERVAL,
    overall_position INTEGER,
    category_position INTEGER,
    gender_position INTEGER,
    pace VARCHAR(20), -- For running events: min/km
    speed DECIMAL(5,2), -- For cycling: km/h
    distance_completed DECIMAL(8,2), -- In case of DNF
    status VARCHAR(20) DEFAULT 'FINISHED', -- 'FINISHED', 'DNF', 'DQ'
    notes TEXT,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    
    -- Ensure one result per registration
    UNIQUE(event_id, registration_id)
);

-- Indexes for results
CREATE INDEX idx_results_event ON results(event_id);
CREATE INDEX idx_results_athlete ON results(athlete_id);
CREATE INDEX idx_results_overall_position ON results(event_id, overall_position);
CREATE INDEX idx_results_category ON results(event_id, category, category_position);

-- =====================================================
-- 6. PAYMENTS (Event-tenant isolated)
-- =====================================================
CREATE TABLE payments (
    id BIGSERIAL PRIMARY KEY,
    event_id BIGINT NOT NULL REFERENCES events(id) ON DELETE CASCADE,
    registration_id BIGINT NOT NULL REFERENCES registrations(id) ON DELETE CASCADE,
    athlete_id BIGINT NOT NULL REFERENCES athletes(id) ON DELETE CASCADE,
    amount DECIMAL(10,2) NOT NULL,
    currency VARCHAR(3) DEFAULT 'BRL',
    payment_method VARCHAR(50) NOT NULL, -- 'CREDIT_CARD', 'PIX', 'BANK_TRANSFER'
    payment_gateway VARCHAR(50), -- 'STRIPE', 'MERCADO_PAGO', 'PAGSEGURO'
    gateway_transaction_id VARCHAR(255),
    gateway_payment_id VARCHAR(255),
    status VARCHAR(20) DEFAULT 'PENDING', -- 'PENDING', 'PROCESSING', 'COMPLETED', 'FAILED', 'REFUNDED'
    processed_at TIMESTAMP,
    refunded_at TIMESTAMP,
    refund_amount DECIMAL(10,2),
    refund_reason TEXT,
    platform_fee DECIMAL(10,2), -- Fee charged by platform
    organizer_amount DECIMAL(10,2), -- Amount that goes to organizer
    metadata JSONB, -- Store gateway-specific data
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Indexes for payments
CREATE INDEX idx_payments_event ON payments(event_id);
CREATE INDEX idx_payments_registration ON payments(registration_id);
CREATE INDEX idx_payments_status ON payments(status);
CREATE INDEX idx_payments_gateway_id ON payments(gateway_transaction_id);
CREATE INDEX idx_payments_processed_at ON payments(processed_at);

-- =====================================================
-- 7. EVENT CATEGORIES (Event-tenant isolated)
-- =====================================================
CREATE TABLE event_categories (
    id BIGSERIAL PRIMARY KEY,
    event_id BIGINT NOT NULL REFERENCES events(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    min_age INTEGER,
    max_age INTEGER,
    gender VARCHAR(10), -- 'M', 'F', 'MIXED'
    price DECIMAL(10,2),
    max_participants INTEGER,
    distance DECIMAL(8,2), -- In kilometers
    created_at TIMESTAMP DEFAULT NOW(),
    
    -- Ensure unique category names per event
    UNIQUE(event_id, name)
);

-- Index for categories
CREATE INDEX idx_event_categories_event ON event_categories(event_id);

-- =====================================================
-- 8. ROW LEVEL SECURITY (RLS) POLICIES
-- =====================================================

-- Enable RLS on tenant-isolated tables
ALTER TABLE registrations ENABLE ROW LEVEL SECURITY;
ALTER TABLE results ENABLE ROW LEVEL SECURITY;
ALTER TABLE payments ENABLE ROW LEVEL SECURITY;
ALTER TABLE event_categories ENABLE ROW LEVEL SECURITY;

-- Create roles for different access patterns
DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'event_organizer') THEN
        CREATE ROLE event_organizer;
    END IF;
    IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'platform_admin') THEN
        CREATE ROLE platform_admin;
    END IF;
    IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'athlete_user') THEN
        CREATE ROLE athlete_user;
    END IF;
END
$$;

-- RLS Policies for REGISTRATIONS
CREATE POLICY event_registrations_isolation ON registrations
    FOR ALL TO event_organizer
    USING (event_id = current_setting('app.current_event_id', true)::bigint);

CREATE POLICY platform_admin_registrations ON registrations
    FOR ALL TO platform_admin
    USING (true); -- Platform admin sees everything

CREATE POLICY athlete_own_registrations ON registrations
    FOR SELECT TO athlete_user
    USING (athlete_id = current_setting('app.current_athlete_id', true)::bigint);

-- RLS Policies for RESULTS
CREATE POLICY event_results_isolation ON results
    FOR ALL TO event_organizer
    USING (event_id = current_setting('app.current_event_id', true)::bigint);

CREATE POLICY platform_admin_results ON results
    FOR ALL TO platform_admin
    USING (true);

CREATE POLICY athlete_own_results ON results
    FOR SELECT TO athlete_user
    USING (athlete_id = current_setting('app.current_athlete_id', true)::bigint);

-- RLS Policies for PAYMENTS
CREATE POLICY event_payments_isolation ON payments
    FOR ALL TO event_organizer
    USING (event_id = current_setting('app.current_event_id', true)::bigint);

CREATE POLICY platform_admin_payments ON payments
    FOR ALL TO platform_admin
    USING (true);

CREATE POLICY athlete_own_payments ON payments
    FOR SELECT TO athlete_user
    USING (athlete_id = current_setting('app.current_athlete_id', true)::bigint);

-- RLS Policies for EVENT_CATEGORIES
CREATE POLICY event_categories_isolation ON event_categories
    FOR ALL TO event_organizer
    USING (event_id = current_setting('app.current_event_id', true)::bigint);

CREATE POLICY platform_admin_categories ON event_categories
    FOR ALL TO platform_admin
    USING (true);

-- Public read access to categories for athletes browsing events
CREATE POLICY public_read_categories ON event_categories
    FOR SELECT TO athlete_user
    USING (true);

-- =====================================================
-- 9. HELPER FUNCTIONS
-- =====================================================

-- Function to set current event context
CREATE OR REPLACE FUNCTION set_current_event(event_id BIGINT)
RETURNS VOID AS $$
BEGIN
    PERFORM set_config('app.current_event_id', event_id::text, true);
END;
$$ LANGUAGE plpgsql;

-- Function to set current athlete context
CREATE OR REPLACE FUNCTION set_current_athlete(athlete_id BIGINT)
RETURNS VOID AS $$
BEGIN
    PERFORM set_config('app.current_athlete_id', athlete_id::text, true);
END;
$$ LANGUAGE plpgsql;

-- Function to get current event context
CREATE OR REPLACE FUNCTION get_current_event()
RETURNS BIGINT AS $$
BEGIN
    RETURN current_setting('app.current_event_id', true)::bigint;
EXCEPTION
    WHEN OTHERS THEN
        RETURN NULL;
END;
$$ LANGUAGE plpgsql;

-- =====================================================
-- 10. GRANTS AND PERMISSIONS
-- =====================================================

-- Grant usage to roles
GRANT USAGE ON SCHEMA public TO event_organizer, platform_admin, athlete_user;

-- Grant table permissions
GRANT SELECT, INSERT, UPDATE, DELETE ON organizations TO platform_admin;
GRANT SELECT ON organizations TO event_organizer, athlete_user;

GRANT SELECT, INSERT, UPDATE, DELETE ON events TO platform_admin;
GRANT SELECT, INSERT, UPDATE, DELETE ON events TO event_organizer;
GRANT SELECT ON events TO athlete_user;

GRANT SELECT, INSERT, UPDATE, DELETE ON athletes TO platform_admin;
GRANT SELECT, UPDATE ON athletes TO athlete_user;
GRANT SELECT ON athletes TO event_organizer;

GRANT ALL ON registrations TO platform_admin, event_organizer;
GRANT SELECT, INSERT ON registrations TO athlete_user;

GRANT ALL ON results TO platform_admin, event_organizer;
GRANT SELECT ON results TO athlete_user;

GRANT ALL ON payments TO platform_admin, event_organizer;
GRANT SELECT ON payments TO athlete_user;

GRANT ALL ON event_categories TO platform_admin, event_organizer;
GRANT SELECT ON event_categories TO athlete_user;

-- Grant sequence permissions
GRANT USAGE ON ALL SEQUENCES IN SCHEMA public TO event_organizer, platform_admin, athlete_user;

-- Comments
COMMENT ON TABLE organizations IS 'Organizations that create and manage events';
COMMENT ON TABLE events IS 'Events - each event is a tenant with isolated data';
COMMENT ON TABLE athletes IS 'Athletes - global users who can participate in multiple events';
COMMENT ON TABLE registrations IS 'Event registrations - isolated per event via RLS';
COMMENT ON TABLE results IS 'Event results - isolated per event via RLS';
COMMENT ON TABLE payments IS 'Payment transactions - isolated per event via RLS';
COMMENT ON TABLE event_categories IS 'Event categories/divisions - isolated per event via RLS';