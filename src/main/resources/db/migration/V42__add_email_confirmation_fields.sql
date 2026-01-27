-- V42: Adicionar campos de confirmação de email
-- confirmed: indica se o usuário confirmou o email
-- confirmation_token: token único para confirmação
-- confirmation_token_expires_at: data de expiração do token

ALTER TABLE users 
ADD COLUMN IF NOT EXISTS confirmed BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE users 
ADD COLUMN IF NOT EXISTS confirmation_token VARCHAR(255);

ALTER TABLE users 
ADD COLUMN IF NOT EXISTS confirmation_token_expires_at TIMESTAMP;

-- Índice para busca rápida por token
CREATE INDEX IF NOT EXISTS idx_users_confirmation_token ON users(confirmation_token);

-- Comentário explicativo
COMMENT ON COLUMN users.confirmed IS 'Indica se o usuário confirmou o email. TRUE = pode fazer login';
COMMENT ON COLUMN users.confirmation_token IS 'Token único enviado por email para confirmação';
COMMENT ON COLUMN users.confirmation_token_expires_at IS 'Data/hora de expiração do token de confirmação';

-- Usuários existentes são considerados confirmados (legacy)
UPDATE users SET confirmed = TRUE WHERE confirmed = FALSE;
