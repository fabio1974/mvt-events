-- V81: Convert token expiration fields from timestamp to timestamptz
-- Parte da conversão LocalDateTime → OffsetDateTime para tokens de confirmação e reset de senha

-- Alterar confirmation_token_expires_at de timestamp para timestamptz
ALTER TABLE users 
ALTER COLUMN confirmation_token_expires_at TYPE timestamptz 
USING confirmation_token_expires_at AT TIME ZONE 'America/Fortaleza';

-- Alterar reset_token_expires_at de timestamp para timestamptz
ALTER TABLE users 
ALTER COLUMN reset_token_expires_at TYPE timestamptz 
USING reset_token_expires_at AT TIME ZONE 'America/Fortaleza';
