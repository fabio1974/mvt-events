-- ============================================================================
-- Migration V20: Refactor Address relationship from 1:1 to 1:N per User
-- Add isDefault field to mark primary address
-- ============================================================================

-- Step 1: Remove unique constraint on user_id in addresses table
-- This allows multiple addresses per user
ALTER TABLE addresses 
DROP CONSTRAINT IF EXISTS addresses_user_id_key;

-- Step 2: Add is_default column to addresses table
ALTER TABLE addresses 
ADD COLUMN IF NOT EXISTS is_default BOOLEAN NOT NULL DEFAULT false;

-- Step 3: Set the first address (by creation date) for each user as default
UPDATE addresses 
SET is_default = true 
WHERE id IN (
  SELECT id FROM addresses a
  WHERE a.id = (
    SELECT id FROM addresses a2 
    WHERE a2.user_id = a.user_id 
    ORDER BY a2.created_at ASC 
    LIMIT 1
  )
);

-- Create index on user_id for better query performance
CREATE INDEX IF NOT EXISTS idx_addresses_user_id ON addresses(user_id);

-- Create index on is_default for filtering default addresses
CREATE INDEX IF NOT EXISTS idx_addresses_is_default ON addresses(is_default);

-- Create combined index for finding default address per user
CREATE INDEX IF NOT EXISTS idx_addresses_user_default ON addresses(user_id, is_default);
