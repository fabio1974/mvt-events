-- Remove registration_id column and related indexes from payments table
-- This migration removes the legacy registration_id field that is no longer used

-- Drop indexes that reference registration_id
DROP INDEX IF EXISTS idx_payment_registration_method;
DROP INDEX IF EXISTS idx_payments_registration_id;

-- Drop the foreign key constraint if it exists
ALTER TABLE payments DROP CONSTRAINT IF EXISTS fk_payments_registration;

-- Drop the registration_id column
ALTER TABLE payments DROP COLUMN IF EXISTS registration_id;
