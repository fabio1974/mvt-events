-- Remove contact_email column from organizations table
ALTER TABLE organizations DROP COLUMN IF EXISTS contact_email;
