-- Remove duplicate active push tokens, keeping only the most recently updated one per token value.
-- These duplicates could have been created by the buggy deactivate-then-insert flow
-- before the UPSERT order fix.

WITH ranked AS (
    SELECT id,
           ROW_NUMBER() OVER (
               PARTITION BY token
               ORDER BY updated_at DESC
           ) AS rn
    FROM user_push_tokens
    WHERE is_active = true
)
UPDATE user_push_tokens
SET is_active = false,
    updated_at = CURRENT_TIMESTAMP
WHERE id IN (
    SELECT id FROM ranked WHERE rn > 1
);
