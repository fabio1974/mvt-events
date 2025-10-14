-- V18: Drop tenant_id from event_categories
-- Complete the migration to organization-based tenancy by removing the tenant_id column
-- from event_categories table, which was missed in V17

-- Drop the foreign key constraint first
ALTER TABLE event_categories DROP CONSTRAINT IF EXISTS fk_event_category_tenant;

-- Drop the index
DROP INDEX IF EXISTS idx_event_categories_tenant_id;

-- Drop the tenant_id column
ALTER TABLE event_categories DROP COLUMN IF EXISTS tenant_id;
