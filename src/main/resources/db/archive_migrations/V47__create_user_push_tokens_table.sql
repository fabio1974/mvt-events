-- Migration V47: Criar tabela user_push_tokens para sistema de notificações push
-- Esta tabela armazena tokens de dispositivos para envio de notificações push

CREATE TABLE user_push_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    token VARCHAR(500) NOT NULL,
    platform VARCHAR(20) NOT NULL CHECK (platform IN ('ios', 'android', 'web')),
    device_type VARCHAR(20) NOT NULL CHECK (device_type IN ('mobile', 'web', 'tablet')),
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Foreign key para tabela users
    CONSTRAINT fk_user_push_tokens_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    
    -- Constraint única para evitar tokens duplicados por usuário
    CONSTRAINT uk_user_push_tokens_user_token UNIQUE(user_id, token)
);

-- Índices adicionais para performance
CREATE INDEX idx_user_push_tokens_user_id ON user_push_tokens(user_id);
CREATE INDEX idx_user_push_tokens_is_active ON user_push_tokens(is_active);
CREATE INDEX idx_user_push_tokens_platform ON user_push_tokens(platform);
CREATE INDEX idx_user_push_tokens_device_type ON user_push_tokens(device_type);
CREATE INDEX idx_user_push_tokens_token ON user_push_tokens(token);

-- Comentários para documentação
COMMENT ON TABLE user_push_tokens IS 'Armazena tokens de dispositivos para notificações push';
COMMENT ON COLUMN user_push_tokens.id IS 'Identificador único do token';
COMMENT ON COLUMN user_push_tokens.user_id IS 'ID do usuário proprietário do token';
COMMENT ON COLUMN user_push_tokens.token IS 'Token do dispositivo (Expo Push Token)';
COMMENT ON COLUMN user_push_tokens.platform IS 'Plataforma do dispositivo (ios, android, web)';
COMMENT ON COLUMN user_push_tokens.device_type IS 'Tipo do dispositivo (mobile, web, tablet)';
COMMENT ON COLUMN user_push_tokens.is_active IS 'Indica se o token está ativo';
COMMENT ON COLUMN user_push_tokens.created_at IS 'Data/hora de criação do registro';
COMMENT ON COLUMN user_push_tokens.updated_at IS 'Data/hora da última atualização';