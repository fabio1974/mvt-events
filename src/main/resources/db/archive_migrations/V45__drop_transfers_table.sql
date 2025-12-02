-- ============================================================================
-- Migration V45: Drop transfers table (related to removed Events system)
-- ============================================================================
-- Description: Removes transfers table as it was only used for Event-based
--              financial transfers. The Delivery system uses Payment entity instead.
-- Author: System
-- Date: 2025-10-23
-- ============================================================================

-- Drop indexes first
DROP INDEX IF EXISTS idx_transfers_event_id;
DROP INDEX IF EXISTS idx_transfers_tenant_id;
DROP INDEX IF EXISTS idx_transfers_tenant_event;

-- Drop table
DROP TABLE IF EXISTS transfers CASCADE;

-- Comments
COMMENT ON SCHEMA public IS 'transfers table removed - was related to Events system (now removed)';
