-- V39: Remove tenant_id column from organizations
-- Multi-tenancy uses organization.id directly (no separate tenant_id field needed)

-- Drop index first
DROP INDEX IF EXISTS idx_organizations_tenant_id;

-- Remove tenant_id column
ALTER TABLE organizations DROP COLUMN IF EXISTS tenant_id;
