-- ============================================================================
-- V76: Add current_delivery_id to users (courier active delivery tracking)
-- ============================================================================
-- Tracks which delivery the courier is currently handling.
-- Set when courier accepts a delivery, cleared on complete/cancel.
-- ============================================================================

ALTER TABLE users ADD COLUMN current_delivery_id BIGINT;

ALTER TABLE users ADD CONSTRAINT fk_users_current_delivery
    FOREIGN KEY (current_delivery_id) REFERENCES deliveries(id)
    ON DELETE SET NULL;
