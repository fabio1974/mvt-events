-- ============================================================================
-- MIGRATION V17: REVERT TO ORGANIZATION-BASED MULTI-TENANCY
-- ============================================================================
-- Purpose: Remove event-based tenancy (V2) and use organization-based tenancy
-- Architecture: Organizations are tenants, Events belong to organizations
-- Author: System
-- Date: 2025-10-11
-- ============================================================================

-- ============================================================================
-- STEP 1: DROP ROW LEVEL SECURITY POLICIES (DEPENDENCIES)
-- ============================================================================
-- These policies depend on get_current_event_tenant_id() function
DO $$
BEGIN
    -- Drop policies if they exist
    IF EXISTS (SELECT 1 FROM pg_policies WHERE policyname = 'event_isolation_registrations') THEN
        DROP POLICY event_isolation_registrations ON registrations;
    END IF;
    
    IF EXISTS (SELECT 1 FROM pg_policies WHERE policyname = 'event_isolation_payments') THEN
        DROP POLICY event_isolation_payments ON payments;
    END IF;
    
    IF EXISTS (SELECT 1 FROM pg_policies WHERE policyname = 'event_isolation_transfers') THEN
        DROP POLICY event_isolation_transfers ON transfers;
    END IF;
    
    IF EXISTS (SELECT 1 FROM pg_policies WHERE policyname = 'event_isolation_payment_events') THEN
        DROP POLICY event_isolation_payment_events ON payment_events;
    END IF;
    
    IF EXISTS (SELECT 1 FROM pg_policies WHERE policyname = 'event_isolation_event_financials') THEN
        DROP POLICY event_isolation_event_financials ON event_financials;
    END IF;
    
    -- Note: athletes table was dropped in V4, so we skip it
    
    IF EXISTS (SELECT 1 FROM pg_policies WHERE policyname = 'event_isolation_users') THEN
        DROP POLICY event_isolation_users ON users;
    END IF;
END
$$;

-- Disable RLS on these tables (we'll use Hibernate filters instead)
ALTER TABLE IF EXISTS registrations DISABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS payments DISABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS transfers DISABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS payment_events DISABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS event_financials DISABLE ROW LEVEL SECURITY;
-- athletes was dropped in V4
ALTER TABLE IF EXISTS users DISABLE ROW LEVEL SECURITY;

-- ============================================================================
-- STEP 2: DROP FUNCTIONS CREATED IN V2 (EVENT-BASED TENANCY)
-- ============================================================================
DROP FUNCTION IF EXISTS set_current_event(BIGINT) CASCADE;
DROP FUNCTION IF EXISTS get_current_event_tenant_id() CASCADE;
DROP FUNCTION IF EXISTS clear_current_event() CASCADE;

-- ============================================================================
-- STEP 3: DROP FOREIGN KEY CONSTRAINTS RELATED TO TENANT_ID -> EVENTS
-- ============================================================================
DO $$
BEGIN
    -- Drop FK constraints that point tenant_id to events.id
    -- Note: athletes table was dropped in V4
    
    IF EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_registrations_event_tenant') THEN
        ALTER TABLE registrations DROP CONSTRAINT fk_registrations_event_tenant;
    END IF;
    
    IF EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_payments_event_tenant') THEN
        ALTER TABLE payments DROP CONSTRAINT fk_payments_event_tenant;
    END IF;
    
    IF EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_transfers_event_tenant') THEN
        ALTER TABLE transfers DROP CONSTRAINT fk_transfers_event_tenant;
    END IF;
    
    IF EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_payment_events_event_tenant') THEN
        ALTER TABLE payment_events DROP CONSTRAINT fk_payment_events_event_tenant;
    END IF;
    
    IF EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_event_financials_event_tenant') THEN
        ALTER TABLE event_financials DROP CONSTRAINT fk_event_financials_event_tenant;
    END IF;
    
    IF EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_users_event_tenant') THEN
        ALTER TABLE users DROP CONSTRAINT fk_users_event_tenant;
    END IF;
END
$$;

-- ============================================================================
-- STEP 4: DROP INDEXES RELATED TO TENANT_ID
-- ============================================================================
-- Note: athletes table was dropped in V4
DROP INDEX IF EXISTS idx_registrations_tenant_id;
DROP INDEX IF EXISTS idx_registrations_tenant_event;
DROP INDEX IF EXISTS idx_registrations_tenant_athlete;
DROP INDEX IF EXISTS idx_payments_tenant_id;
DROP INDEX IF EXISTS idx_payments_tenant_registration;
DROP INDEX IF EXISTS idx_transfers_tenant_id;
DROP INDEX IF EXISTS idx_transfers_tenant_event;
DROP INDEX IF EXISTS idx_payment_events_tenant_id;
DROP INDEX IF EXISTS idx_payment_events_tenant_event;
DROP INDEX IF EXISTS idx_payment_events_tenant_payment;
DROP INDEX IF EXISTS idx_event_financials_tenant_id;
DROP INDEX IF EXISTS idx_event_financials_tenant_event;
DROP INDEX IF EXISTS idx_users_tenant_id;

-- ============================================================================
-- STEP 5: DROP TENANT_ID COLUMNS FROM ALL TABLES
-- ============================================================================
-- Remove tenant_id columns since we're using organization-based tenancy
-- Events already have organization_id (the real tenant)
-- Other tables have relationships to events, which have organization_id
-- Note: athletes table was dropped in V4

ALTER TABLE registrations DROP COLUMN IF EXISTS tenant_id;
ALTER TABLE payments DROP COLUMN IF EXISTS tenant_id;
ALTER TABLE transfers DROP COLUMN IF EXISTS tenant_id;
ALTER TABLE payment_events DROP COLUMN IF EXISTS tenant_id;
ALTER TABLE event_financials DROP COLUMN IF EXISTS tenant_id;
ALTER TABLE users DROP COLUMN IF EXISTS tenant_id;

-- ============================================================================
-- STEP 6: CREATE INDEXES FOR ORGANIZATION-BASED QUERIES
-- ============================================================================
-- Since Events have organization_id, we can filter all related entities
-- through their relationship with events

-- Index on events.organization_id for fast tenant filtering
CREATE INDEX IF NOT EXISTS idx_events_organization_id ON events(organization_id);

-- Indexes on relationships to events (for efficient joins)
CREATE INDEX IF NOT EXISTS idx_registrations_event_id ON registrations(event_id);
CREATE INDEX IF NOT EXISTS idx_payments_registration_id ON payments(registration_id);
CREATE INDEX IF NOT EXISTS idx_transfers_event_id ON transfers(event_id);
CREATE INDEX IF NOT EXISTS idx_payment_events_event_id ON payment_events(event_id);
CREATE INDEX IF NOT EXISTS idx_event_financials_event_id ON event_financials(event_id);

-- Index on users.organization_id for direct tenant filtering
CREATE INDEX IF NOT EXISTS idx_users_organization_id ON users(organization_id);

-- ============================================================================
-- VERIFICATION QUERIES (FOR DOCUMENTATION)
-- ============================================================================
-- To verify organization-based tenancy is working:
-- 
-- 1. Get all events for organization_id=5:
--    SELECT * FROM events WHERE organization_id = 5;
--
-- 2. Get all registrations for organization_id=5:
--    SELECT r.* FROM registrations r
--    JOIN events e ON r.event_id = e.id
--    WHERE e.organization_id = 5;
--
-- 3. Get all payments for organization_id=5:
--    SELECT p.* FROM payments p
--    JOIN registrations r ON p.registration_id = r.id
--    JOIN events e ON r.event_id = e.id
--    WHERE e.organization_id = 5;
-- ============================================================================

-- ============================================================================
-- SUMMARY OF CHANGES
-- ============================================================================
-- ✅ Removed event-based tenancy functions (set_current_event, etc.)
-- ✅ Dropped foreign keys pointing tenant_id to events
-- ✅ Dropped all tenant_id indexes
-- ✅ Removed tenant_id columns from all tables
-- ✅ Created indexes for organization-based queries
-- 
-- NEW ARCHITECTURE:
-- - TENANT: Organizations (organization_id in events, users)
-- - Events belong to organizations (events.organization_id)
-- - Users belong to organizations (users.organization_id)
-- - Other entities (registrations, payments, etc.) belong to events
-- - Tenant filtering happens via Hibernate @Filter on Event.organization_id
-- 
-- TENANT ISOLATION:
-- - Events: Filtered by organization_id (Hibernate @Filter)
-- - Registrations: Filtered via event relationship
-- - Payments: Filtered via registration -> event relationship
-- - Users: Filtered by organization_id (direct)
-- ============================================================================
