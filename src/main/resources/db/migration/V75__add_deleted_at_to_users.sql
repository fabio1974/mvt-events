-- ============================================================================
-- V75: Add deleted_at column to users for soft-delete (Apple review requirement)
-- ============================================================================
-- Enables account deletion via DELETE /api/users/me
-- When deleted_at IS NOT NULL, the account is considered soft-deleted:
--   - blocked = true (prevents login)
--   - username anonymized to 'deleted_<uuid>@removed.com'
--   - name set to 'Usuário Removido'
-- ============================================================================

ALTER TABLE users ADD COLUMN deleted_at TIMESTAMP(6);

CREATE INDEX idx_users_deleted_at ON users(deleted_at) WHERE deleted_at IS NOT NULL;

COMMENT ON COLUMN users.deleted_at IS 'Soft-delete timestamp. Non-null means account was deleted by user.';
