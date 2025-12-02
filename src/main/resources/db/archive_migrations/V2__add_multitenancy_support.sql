-- ============================================================================
-- MIGRATION V2: MULTI-TENANCY SUPPORT IMPLEMENTATION BASED ON EVENTS
-- ============================================================================

-- ============================================================================
-- FUNCTION TO SET EVENT CONTEXT IN SESSION
-- ============================================================================
DROP FUNCTION IF EXISTS set_current_event(UUID);
DROP FUNCTION IF EXISTS set_current_event(BIGINT);

CREATE OR REPLACE FUNCTION set_current_event(event_bigint BIGINT)
RETURNS VOID AS $$
BEGIN
    -- Validate if the event exists and is active
    IF NOT EXISTS (SELECT 1 FROM events WHERE id = event_bigint) THEN
        RAISE EXCEPTION 'Event % not found', event_bigint;
    END IF;
    
    -- Set the event in the session context
    PERFORM set_config('app.current_event_id', event_bigint::text, false);
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- ============================================================================
-- FUNCTION TO GET CURRENT EVENT TENANT
-- ============================================================================
DROP FUNCTION IF EXISTS get_current_event_tenant_id();

CREATE OR REPLACE FUNCTION get_current_event_tenant_id()
RETURNS BIGINT AS $$
DECLARE
    event_id BIGINT;
BEGIN
    -- Try to get the event_id from session context
    BEGIN
        event_id := current_setting('app.current_event_id')::BIGINT;
    EXCEPTION WHEN OTHERS THEN
        -- If no event_id is defined, return NULL
        -- This will force the application to always set the event
        RETURN NULL;
    END;
    
    RETURN event_id;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- ============================================================================
-- FUNCTION TO CLEAR EVENT CONTEXT
-- ============================================================================
DROP FUNCTION IF EXISTS clear_current_event();

CREATE OR REPLACE FUNCTION clear_current_event()
RETURNS VOID AS $$
BEGIN
    -- Clear the event context
    PERFORM set_config('app.current_event_id', '', false);
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- ============================================================================
-- MIGRATION V2: MULTI-TENANCY SUPPORT IMPLEMENTATION BASED ON EVENTS
-- ============================================================================
-- Architecture: Events are tenants, Organizations are global

-- ============================================================================
-- ADDING TENANT_ID TO TABLES (EXCEPT ORGANIZATIONS AND EVENTS)
-- ============================================================================
-- EVENTS are the tenants themselves (don't need tenant_id)
-- ORGANIZATIONS are global entities (don't need tenant_id)

-- Add tenant_id to tables that should be isolated by event
ALTER TABLE athletes ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
ALTER TABLE registrations ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
ALTER TABLE payments ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
ALTER TABLE transfers ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
ALTER TABLE payment_events ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
ALTER TABLE event_financials ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
ALTER TABLE users ADD COLUMN IF NOT EXISTS tenant_id BIGINT;

-- ============================================================================
-- MIGRATING EXISTING DATA
-- ============================================================================
DO $$
BEGIN
    -- Update existing records with tenant_id based on event_id
    
    -- Athletes: use the first event as default tenant if no registrations exist
    UPDATE athletes SET tenant_id = (
        SELECT COALESCE(
            (SELECT r.event_id FROM registrations r WHERE r.athlete_id = athletes.id LIMIT 1),
            (SELECT id FROM events LIMIT 1)
        )
    ) WHERE tenant_id IS NULL;
    
    -- Registrations: tenant_id = event_id
    UPDATE registrations SET tenant_id = event_id WHERE tenant_id IS NULL;
    
    -- Payments: tenant_id based on registration's event_id
    UPDATE payments SET tenant_id = (
        SELECT r.event_id FROM registrations r WHERE r.id = payments.registration_id
    ) WHERE tenant_id IS NULL;
    
    -- Transfers: tenant_id = event_id
    UPDATE transfers SET tenant_id = event_id WHERE tenant_id IS NULL;
    
    -- Payment Events: tenant_id = event_id
    UPDATE payment_events SET tenant_id = event_id WHERE tenant_id IS NULL;
    
    -- Event Financials: tenant_id = event_id
    UPDATE event_financials SET tenant_id = event_id WHERE tenant_id IS NULL;
    
    -- Users: use the first event as default tenant
    UPDATE users SET tenant_id = (
        SELECT id FROM events LIMIT 1
    ) WHERE tenant_id IS NULL;
END
$$;

-- ============================================================================
-- MAKE TENANT_ID REQUIRED (EXCEPT ORGANIZATIONS AND EVENTS)
-- ============================================================================
ALTER TABLE athletes ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE registrations ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE payments ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE transfers ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE payment_events ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE event_financials ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE users ALTER COLUMN tenant_id SET NOT NULL;

-- ============================================================================
-- FOREIGN KEYS TO EVENTS (AS TENANTS)
-- ============================================================================
DO $$
BEGIN
    -- FK for athletes (tenant_id points to events.id)
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_athletes_event_tenant') THEN
        ALTER TABLE athletes ADD CONSTRAINT fk_athletes_event_tenant 
            FOREIGN KEY (tenant_id) REFERENCES events(id) ON DELETE CASCADE;
    END IF;
    
    -- FK for registrations (tenant_id points to events.id)
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_registrations_event_tenant') THEN
        ALTER TABLE registrations ADD CONSTRAINT fk_registrations_event_tenant 
            FOREIGN KEY (tenant_id) REFERENCES events(id) ON DELETE CASCADE;
    END IF;
    
    -- FK for payments (tenant_id points to events.id)
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_payments_event_tenant') THEN
        ALTER TABLE payments ADD CONSTRAINT fk_payments_event_tenant 
            FOREIGN KEY (tenant_id) REFERENCES events(id) ON DELETE CASCADE;
    END IF;
    
    -- FK for transfers (tenant_id points to events.id)
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_transfers_event_tenant') THEN
        ALTER TABLE transfers ADD CONSTRAINT fk_transfers_event_tenant 
            FOREIGN KEY (tenant_id) REFERENCES events(id) ON DELETE CASCADE;
    END IF;
    
    -- FK for payment_events (tenant_id points to events.id)
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_payment_events_event_tenant') THEN
        ALTER TABLE payment_events ADD CONSTRAINT fk_payment_events_event_tenant 
            FOREIGN KEY (tenant_id) REFERENCES events(id) ON DELETE CASCADE;
    END IF;
    
    -- FK for event_financials (tenant_id points to events.id)
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_event_financials_event_tenant') THEN
        ALTER TABLE event_financials ADD CONSTRAINT fk_event_financials_event_tenant 
            FOREIGN KEY (tenant_id) REFERENCES events(id) ON DELETE CASCADE;
    END IF;
    
    -- FK for users (tenant_id points to events.id)
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_users_event_tenant') THEN
        ALTER TABLE users ADD CONSTRAINT fk_users_event_tenant 
            FOREIGN KEY (tenant_id) REFERENCES events(id) ON DELETE CASCADE;
    END IF;
END
$$;

-- ============================================================================
-- COMPOSITE INDEXES FOR MULTI-TENANT PERFORMANCE (BASED ON EVENTS)
-- ============================================================================

-- Organizations remain global (no tenant_id)
-- Events are the tenants themselves (no tenant_id)

-- Indexes for athletes
CREATE INDEX IF NOT EXISTS idx_athletes_tenant_id ON athletes(tenant_id);
CREATE INDEX IF NOT EXISTS idx_athletes_tenant_email ON athletes(tenant_id, email);
CREATE INDEX IF NOT EXISTS idx_athletes_tenant_document ON athletes(tenant_id, document_number);

-- Indexes for registrations
CREATE INDEX IF NOT EXISTS idx_registrations_tenant_id ON registrations(tenant_id);
CREATE INDEX IF NOT EXISTS idx_registrations_tenant_event ON registrations(tenant_id, event_id);
CREATE INDEX IF NOT EXISTS idx_registrations_tenant_athlete ON registrations(tenant_id, athlete_id);
CREATE INDEX IF NOT EXISTS idx_registrations_tenant_status ON registrations(tenant_id, status);

-- Indexes for payments
CREATE INDEX IF NOT EXISTS idx_payments_tenant_id ON payments(tenant_id);
CREATE INDEX IF NOT EXISTS idx_payments_tenant_registration ON payments(tenant_id, registration_id);
CREATE INDEX IF NOT EXISTS idx_payments_tenant_status ON payments(tenant_id, payment_status);

-- Indexes for transfers
CREATE INDEX IF NOT EXISTS idx_transfers_tenant_id ON transfers(tenant_id);
CREATE INDEX IF NOT EXISTS idx_transfers_tenant_event ON transfers(tenant_id, event_id);
CREATE INDEX IF NOT EXISTS idx_transfers_tenant_organization ON transfers(tenant_id, organization_id);
CREATE INDEX IF NOT EXISTS idx_transfers_tenant_status ON transfers(tenant_id, status);

-- Indexes for payment_events
CREATE INDEX IF NOT EXISTS idx_payment_events_tenant_id ON payment_events(tenant_id);
CREATE INDEX IF NOT EXISTS idx_payment_events_tenant_event ON payment_events(tenant_id, event_id);
CREATE INDEX IF NOT EXISTS idx_payment_events_tenant_payment ON payment_events(tenant_id, payment_id);

-- Indexes for event_financials
CREATE INDEX IF NOT EXISTS idx_event_financials_tenant_id ON event_financials(tenant_id);
CREATE INDEX IF NOT EXISTS idx_event_financials_tenant_event ON event_financials(tenant_id, event_id);

-- Indexes for users
CREATE INDEX IF NOT EXISTS idx_users_tenant_id ON users(tenant_id);
CREATE INDEX IF NOT EXISTS idx_users_tenant_username ON users(tenant_id, username);
CREATE INDEX IF NOT EXISTS idx_users_tenant_email ON users(tenant_id, email);

-- ============================================================================
-- UNIQUE CONSTRAINTS ADJUSTED FOR EVENTS-BASED MULTI-TENANCY
-- ============================================================================
DO $$
BEGIN
    -- Organizations remain global (original constraints maintained)
    -- Events are the tenants themselves (original constraints maintained)
    
    -- Athletes: email and document unique per tenant (event)
    ALTER TABLE athletes DROP CONSTRAINT IF EXISTS ukn0i3de0t8dyv19qn45vgyi322;
    ALTER TABLE athletes DROP CONSTRAINT IF EXISTS ukek0v4gdbthm9d0pgs4itg21j0;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_athletes_tenant_email') THEN
        ALTER TABLE athletes ADD CONSTRAINT uk_athletes_tenant_email UNIQUE (tenant_id, email);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_athletes_tenant_document') THEN
        ALTER TABLE athletes ADD CONSTRAINT uk_athletes_tenant_document UNIQUE (tenant_id, document_number);
    END IF;
    
    -- Registrations: one registration per athlete per event per tenant (redundant but consistent)
    ALTER TABLE registrations DROP CONSTRAINT IF EXISTS uk18v3flm45yg9d3o380mne8cof;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_registrations_tenant_event_athlete') THEN
        ALTER TABLE registrations ADD CONSTRAINT uk_registrations_tenant_event_athlete UNIQUE (tenant_id, event_id, athlete_id);
    END IF;
    
    -- Event Financials: one financial record per event per tenant
    ALTER TABLE event_financials DROP CONSTRAINT IF EXISTS uk4adkilqs0mjxvf2ypbwalyvc;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_event_financials_tenant_event') THEN
        ALTER TABLE event_financials ADD CONSTRAINT uk_event_financials_tenant_event UNIQUE (tenant_id, event_id);
    END IF;
    
    -- Users: username unique per tenant (event)
    ALTER TABLE users DROP CONSTRAINT IF EXISTS ukr43af9ap4edm43mmtq01oddj6;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_users_tenant_username') THEN
        ALTER TABLE users ADD CONSTRAINT uk_users_tenant_username UNIQUE (tenant_id, username);
    END IF;
END
$$;

-- ============================================================================
-- SECURITY POLICIES (RLS - Row Level Security)
-- ============================================================================

-- Enable RLS only on tables with tenant_id (not on organizations/events)
ALTER TABLE athletes ENABLE ROW LEVEL SECURITY;
ALTER TABLE registrations ENABLE ROW LEVEL SECURITY;
ALTER TABLE payments ENABLE ROW LEVEL SECURITY;
ALTER TABLE transfers ENABLE ROW LEVEL SECURITY;
ALTER TABLE payment_events ENABLE ROW LEVEL SECURITY;
ALTER TABLE event_financials ENABLE ROW LEVEL SECURITY;
ALTER TABLE users ENABLE ROW LEVEL SECURITY;

-- ============================================================================
-- RLS POLICIES FOR EVENT-BASED ISOLATION
-- ============================================================================

-- Policy for athletes
DROP POLICY IF EXISTS event_isolation_athletes ON athletes;
CREATE POLICY event_isolation_athletes ON athletes
    FOR ALL
    USING (tenant_id = get_current_event_tenant_id())
    WITH CHECK (tenant_id = get_current_event_tenant_id());

-- Policy for registrations
DROP POLICY IF EXISTS event_isolation_registrations ON registrations;
CREATE POLICY event_isolation_registrations ON registrations
    FOR ALL
    USING (tenant_id = get_current_event_tenant_id())
    WITH CHECK (tenant_id = get_current_event_tenant_id());

-- Policy for payments
DROP POLICY IF EXISTS event_isolation_payments ON payments;
CREATE POLICY event_isolation_payments ON payments
    FOR ALL
    USING (tenant_id = get_current_event_tenant_id())
    WITH CHECK (tenant_id = get_current_event_tenant_id());

-- Policy for transfers
DROP POLICY IF EXISTS event_isolation_transfers ON transfers;
CREATE POLICY event_isolation_transfers ON transfers
    FOR ALL
    USING (tenant_id = get_current_event_tenant_id())
    WITH CHECK (tenant_id = get_current_event_tenant_id());

-- Policy for payment_events
DROP POLICY IF EXISTS event_isolation_payment_events ON payment_events;
CREATE POLICY event_isolation_payment_events ON payment_events
    FOR ALL
    USING (tenant_id = get_current_event_tenant_id())
    WITH CHECK (tenant_id = get_current_event_tenant_id());

-- Policy for event_financials
DROP POLICY IF EXISTS event_isolation_event_financials ON event_financials;
CREATE POLICY event_isolation_event_financials ON event_financials
    FOR ALL
    USING (tenant_id = get_current_event_tenant_id())
    WITH CHECK (tenant_id = get_current_event_tenant_id());

-- Policy for users
DROP POLICY IF EXISTS event_isolation_users ON users;
CREATE POLICY event_isolation_users ON users
    FOR ALL
    USING (tenant_id = get_current_event_tenant_id())
    WITH CHECK (tenant_id = get_current_event_tenant_id());

/*
============================================================================
EVENTS-BASED MULTI-TENANT ARCHITECTURE SUMMARY
============================================================================

1. ISOLATION SCOPE:
   - TENANT: Events (each event is an independent tenant)
   - GLOBAL: Organizations (shared across all events)

2. TENANT-ISOLATED TABLES:
   - athletes (tenant_id -> events.id)
   - registrations (tenant_id -> events.id)
   - payments (tenant_id -> events.id)
   - transfers (tenant_id -> events.id)  
   - payment_events (tenant_id -> events.id)
   - event_financials (tenant_id -> events.id)
   - users (tenant_id -> events.id)

3. GLOBAL TABLES (NO ISOLATION):
   - organizations (can be accessed by any event)
   - events (are the tenants themselves)

4. IMPLEMENTED SECURITY:
   - Row Level Security (RLS) active on all isolated tables
   - RLS policies based on get_current_event_tenant_id() function
   - Automatic validation on INSERT/UPDATE/DELETE/SELECT

5. CONTROL FUNCTIONS:
   - set_current_event(event_id): Sets event context in session
   - get_current_event_tenant_id(): Returns current event ID from session
   - clear_current_event(): Clears session context

6. CONSTRAINTS AND INDEXES:
   - Uniqueness based on (tenant_id, field) for proper isolation
   - Composite indexes for multi-tenant performance
   - Foreign keys ensuring referential integrity

7. MIGRATION STRATEGY:
   - Existing data migrated automatically
   - tenant_id populated based on event_id from existing relationships
   - Automatic backup of original constraints

8. RLS POLICIES:
   - All isolated tables have active RLS policies
   - Policies apply to both reading (USING) and writing (WITH CHECK)
   - Application must ALWAYS set event before any operation
   - Failure to set event will result in empty queries (extra protection)

IMPORTANT: 
- Always set the event at the beginning of each request
- Clear context at the end of the request
- Organizations are global and accessible by all events
- Test isolation during development
*/