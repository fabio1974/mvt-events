-- Migration: Convert request and response columns from JSONB to TEXT
-- Purpose: Fix issue where Hibernate @JdbcTypeCode was double-serializing JSON strings
-- The fields are already serialized as JSON strings by the application, so they should be TEXT not JSONB
-- Date: 2026-02-18

-- Convert request column from JSONB to TEXT
-- The USING clause preserves existing data by casting JSONB to TEXT
ALTER TABLE payments
ALTER COLUMN request TYPE TEXT USING request::TEXT;

-- Convert response column from JSONB to TEXT  
-- The USING clause preserves existing data by casting JSONB to TEXT
ALTER TABLE payments
ALTER COLUMN response TYPE TEXT USING response::TEXT;

-- Update comments to reflect the change
COMMENT ON COLUMN payments.request IS 'Complete request payload (JSON string already serialized) sent to payment gateway for auditing';
COMMENT ON COLUMN payments.response IS 'Complete response payload (JSON string already serialized) returned by payment gateway for auditing';
