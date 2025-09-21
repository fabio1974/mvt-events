-- ============================================================================
-- MIGRATION V3: MAKE USERS TENANT_ID NULLABLE
-- ============================================================================
-- Users are global entities that can manage multiple events/tenants
-- They should not be restricted to a single tenant

-- Make tenant_id nullable for users table
ALTER TABLE users ALTER COLUMN tenant_id DROP NOT NULL;

-- Update any existing users with NULL tenant_id (they are global)
-- This is safe because users manage events, they're not scoped to events
UPDATE users SET tenant_id = NULL WHERE tenant_id IS NOT NULL;

-- Remove RLS policy for users since they are global entities
DROP POLICY IF EXISTS event_isolation_users ON users;

-- Users can access all data regardless of tenant context
-- They manage events and should not be restricted by tenant isolation
CREATE POLICY allow_all_users ON users
    FOR ALL
    USING (true)
    WITH CHECK (true);