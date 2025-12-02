-- Update registrations table structure to match current entity
-- This migration aligns the database schema with the current Registration entity

-- Rename amount_paid to payment_amount
ALTER TABLE registrations 
RENAME COLUMN amount_paid TO payment_amount;

-- Rename athlete_id to user_id (if exists)
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns 
              WHERE table_name = 'registrations' AND column_name = 'athlete_id') THEN
        ALTER TABLE registrations RENAME COLUMN athlete_id TO user_id;
    END IF;
END $$;

-- Add missing columns if they don't exist
ALTER TABLE registrations 
ADD COLUMN IF NOT EXISTS payment_date TIMESTAMP(6),
ADD COLUMN IF NOT EXISTS payment_reference VARCHAR(255);

-- Update payment_status values to match enum in entity
UPDATE registrations SET payment_status = 'PENDING' WHERE payment_status IS NULL;
UPDATE registrations SET payment_status = 'FAILED' WHERE payment_status = 'CANCELLED';

-- Update status values to match enum in entity  
UPDATE registrations SET status = 'PENDING' WHERE status IS NULL;
UPDATE registrations SET status = 'ACTIVE' WHERE status = 'ACTIVE';
UPDATE registrations SET status = 'CANCELLED' WHERE status IN ('DNS', 'DNF', 'CANCELLED');

-- Update constraints to match new enum values
ALTER TABLE registrations DROP CONSTRAINT IF EXISTS registrations_payment_status_check;
ALTER TABLE registrations ADD CONSTRAINT registrations_payment_status_check 
CHECK (payment_status IN ('PENDING', 'PAID', 'FAILED', 'REFUNDED'));

ALTER TABLE registrations DROP CONSTRAINT IF EXISTS registrations_status_check;
ALTER TABLE registrations ADD CONSTRAINT registrations_status_check 
CHECK (status IN ('PENDING', 'ACTIVE', 'CANCELLED', 'COMPLETED'));

-- Remove columns that are no longer needed
ALTER TABLE registrations 
DROP COLUMN IF EXISTS bib_number,
DROP COLUMN IF EXISTS category, 
DROP COLUMN IF EXISTS payment_method,
DROP COLUMN IF EXISTS special_needs,
DROP COLUMN IF EXISTS t_shirt_size,
DROP COLUMN IF EXISTS team_name;