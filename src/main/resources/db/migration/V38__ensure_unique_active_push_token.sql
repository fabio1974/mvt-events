-- Ensure only one active row per token across all users
-- 1) Deactivate duplicates, keeping the most recently updated active row
WITH ranked AS (
    SELECT id,
           token,
           updated_at,
           ROW_NUMBER() OVER (PARTITION BY token ORDER BY updated_at DESC, id DESC) AS rn
    FROM user_push_tokens
    WHERE is_active = true
)
UPDATE user_push_tokens u
SET is_active = false
FROM ranked r
WHERE u.id = r.id
  AND r.rn > 1;

-- 2) Create a partial unique index to enforce uniqueness of active tokens
CREATE UNIQUE INDEX IF NOT EXISTS ux_user_push_tokens_active_token
    ON user_push_tokens(token)
    WHERE is_active = true;
