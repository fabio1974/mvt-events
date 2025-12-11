-- ============================================================================
-- Migration V12: Add KYC fields to bank_accounts table for Pagar.me
-- ============================================================================
-- Description: Add all required KYC (Know Your Customer) fields for Pagar.me recipient creation
-- Author: System Migration
-- Date: 2025-12-10
-- ============================================================================

-- Add account holder information
ALTER TABLE bank_accounts ADD COLUMN IF NOT EXISTS agency_digit VARCHAR(2);
ALTER TABLE bank_accounts ADD COLUMN IF NOT EXISTS account_digit VARCHAR(2);
ALTER TABLE bank_accounts ADD COLUMN IF NOT EXISTS account_holder_name VARCHAR(200);
ALTER TABLE bank_accounts ADD COLUMN IF NOT EXISTS account_holder_document VARCHAR(14);

-- Add personal information (KYC)
ALTER TABLE bank_accounts ADD COLUMN IF NOT EXISTS email VARCHAR(200);
ALTER TABLE bank_accounts ADD COLUMN IF NOT EXISTS mother_name VARCHAR(200);
ALTER TABLE bank_accounts ADD COLUMN IF NOT EXISTS birthdate VARCHAR(10); -- DD/MM/YYYY
ALTER TABLE bank_accounts ADD COLUMN IF NOT EXISTS monthly_income VARCHAR(20);
ALTER TABLE bank_accounts ADD COLUMN IF NOT EXISTS professional_occupation VARCHAR(100);

-- Add contact information
ALTER TABLE bank_accounts ADD COLUMN IF NOT EXISTS phone_ddd VARCHAR(2);
ALTER TABLE bank_accounts ADD COLUMN IF NOT EXISTS phone_number VARCHAR(9);

-- Add address information
ALTER TABLE bank_accounts ADD COLUMN IF NOT EXISTS address_street VARCHAR(200);
ALTER TABLE bank_accounts ADD COLUMN IF NOT EXISTS address_number VARCHAR(10);
ALTER TABLE bank_accounts ADD COLUMN IF NOT EXISTS address_complement VARCHAR(100);
ALTER TABLE bank_accounts ADD COLUMN IF NOT EXISTS address_neighborhood VARCHAR(100);
ALTER TABLE bank_accounts ADD COLUMN IF NOT EXISTS address_city VARCHAR(100);
ALTER TABLE bank_accounts ADD COLUMN IF NOT EXISTS address_state VARCHAR(2);
ALTER TABLE bank_accounts ADD COLUMN IF NOT EXISTS address_zip_code VARCHAR(8);
ALTER TABLE bank_accounts ADD COLUMN IF NOT EXISTS address_reference_point VARCHAR(200);

-- Create indexes for frequently queried fields
CREATE INDEX IF NOT EXISTS idx_bank_accounts_email ON bank_accounts(email);
CREATE INDEX IF NOT EXISTS idx_bank_accounts_document ON bank_accounts(account_holder_document);

-- Log migration completion
DO $$
BEGIN
    RAISE NOTICE '✅ V12: Added all KYC fields to bank_accounts table';
END $$;
