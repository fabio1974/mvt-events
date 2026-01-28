-- ============================================================================
-- V43: Add password reset token fields to users table
-- ============================================================================
-- Adds reset_token and reset_token_expires_at columns for password recovery

ALTER TABLE users ADD COLUMN IF NOT EXISTS reset_token VARCHAR(255);
ALTER TABLE users ADD COLUMN IF NOT EXISTS reset_token_expires_at TIMESTAMP;

-- Create index for faster token lookup
CREATE INDEX IF NOT EXISTS idx_users_reset_token ON users(reset_token);

COMMENT ON COLUMN users.reset_token IS 'Token único para reset de senha, enviado por email';
COMMENT ON COLUMN users.reset_token_expires_at IS 'Data de expiração do token de reset (1 hora por padrão)';
