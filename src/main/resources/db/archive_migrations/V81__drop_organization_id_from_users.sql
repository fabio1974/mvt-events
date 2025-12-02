-- ============================================================================
-- Migration V81: Drop organization_id column from users table
-- ============================================================================
-- 
-- Description:
--   Remove the organization_id column from users table. The relationship
--   between User and Organization is now reversed - Organization has an
--   owner_id field that references users(id).
--
-- Changes:
--   1. Drop the foreign key constraint if it exists
--   2. Drop the index on organization_id
--   3. Drop the organization_id column from users table
--
-- Author: System
-- Date: 2025-12-01
-- ============================================================================

-- Drop foreign key constraint if exists
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 
        FROM information_schema.table_constraints 
        WHERE constraint_name = 'fk_users_organization' 
        AND table_name = 'users'
    ) THEN
        ALTER TABLE users DROP CONSTRAINT fk_users_organization;
    END IF;
END $$;

-- Drop index if exists
DROP INDEX IF EXISTS idx_users_organization_id;

-- Drop column organization_id from users table
ALTER TABLE users DROP COLUMN IF EXISTS organization_id;

-- Add comment to document the change
COMMENT ON TABLE users IS 'Users table. Organization relationship is now through organizations.owner_id instead of users.organization_id';
