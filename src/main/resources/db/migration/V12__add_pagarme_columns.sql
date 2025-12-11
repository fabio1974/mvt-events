-- ============================================================================
-- Migration V11: Add Pagar.me columns to database
-- ============================================================================
-- Description: Add Pagar.me-specific columns for payment gateway integration
-- Author: System Migration
-- Date: 2025-12-09
-- ============================================================================

-- Add Pagar.me columns to users table
ALTER TABLE users ADD COLUMN IF NOT EXISTS pagarme_recipient_id VARCHAR(100);
ALTER TABLE users ADD COLUMN IF NOT EXISTS pagarme_status VARCHAR(20);

-- Create index for faster lookups
CREATE INDEX IF NOT EXISTS idx_users_pagarme_recipient ON users(pagarme_recipient_id);

-- Add Pagar.me columns to payments table
ALTER TABLE payments ADD COLUMN IF NOT EXISTS pagarme_order_id VARCHAR(100);
ALTER TABLE payments ADD COLUMN IF NOT EXISTS pix_qr_code TEXT;
ALTER TABLE payments ADD COLUMN IF NOT EXISTS pix_qr_code_url TEXT;
ALTER TABLE payments ADD COLUMN IF NOT EXISTS split_rules JSONB;

-- Add unique constraint and index (check if exists first)
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_pagarme_order_id') THEN
        ALTER TABLE payments ADD CONSTRAINT uk_pagarme_order_id UNIQUE (pagarme_order_id);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_payments_pagarme_order ON payments(pagarme_order_id);

-- Log migration completion
DO $$
BEGIN
    RAISE NOTICE '✅ V11: Added all Pagar.me columns to database';
END $$;
