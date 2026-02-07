-- ============================================================================
-- Migration V50: Add UNIQUE constraint to ensure only one default address per user
-- ============================================================================

-- Step 1: First, ensure no user has multiple default addresses
-- Keep only the most recent address as default for users with multiple defaults
UPDATE addresses SET is_default = false 
WHERE id IN (
    SELECT a.id 
    FROM addresses a
    INNER JOIN (
        SELECT user_id, MAX(id) as max_id
        FROM addresses 
        WHERE is_default = true
        GROUP BY user_id
        HAVING COUNT(*) > 1
    ) duplicates ON a.user_id = duplicates.user_id
    WHERE a.is_default = true 
    AND a.id != duplicates.max_id
);

-- Step 2: Create partial unique index to ensure only one default address per user
-- This constraint only applies when is_default = true
CREATE UNIQUE INDEX IF NOT EXISTS idx_unique_default_per_user 
ON addresses (user_id) 
WHERE is_default = true;

-- Comment explaining the constraint
COMMENT ON INDEX idx_unique_default_per_user IS 
'Ensures each user can have only one default address. Partial index applies only to rows where is_default = true.';
