-- ============================================================================
-- Migration V10: Remove Iugu columns from database
-- ============================================================================
-- Description: Remove all Iugu-specific columns as we migrate to Pagar.me
-- Author: System Migration
-- Date: 2025-12-09
-- ============================================================================

-- Remove Iugu columns from users table
ALTER TABLE users DROP COLUMN IF EXISTS iugu_account_id;
ALTER TABLE users DROP COLUMN IF EXISTS bank_data_complete;
ALTER TABLE users DROP COLUMN IF EXISTS auto_withdraw_enabled;

-- Remove Iugu columns from payments table
ALTER TABLE payments DROP COLUMN IF EXISTS iugu_invoice_id;
ALTER TABLE payments DROP COLUMN IF EXISTS pix_qr_code;
ALTER TABLE payments DROP COLUMN IF EXISTS pix_qr_code_url;
ALTER TABLE payments DROP COLUMN IF EXISTS split_rules;

-- Log migration completion
DO $$
BEGIN
    RAISE NOTICE '✅ V10: Removed all Iugu columns from database';
END $$;
