-- Remove redundant payment fields from registrations table
-- These fields are now handled by the Payment entity

ALTER TABLE registrations 
DROP COLUMN IF EXISTS payment_status,
DROP COLUMN IF EXISTS payment_amount,
DROP COLUMN IF EXISTS payment_date,
DROP COLUMN IF EXISTS payment_reference;