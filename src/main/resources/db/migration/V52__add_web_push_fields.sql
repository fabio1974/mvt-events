-- Migration V52: Add Web Push fields to user_push_tokens table
-- Adiciona campos necessários para suportar Web Push Notifications

-- Adicionar campos para Web Push Notifications
ALTER TABLE user_push_tokens
ADD COLUMN web_endpoint VARCHAR(1000),
ADD COLUMN web_p256dh VARCHAR(500),
ADD COLUMN web_auth VARCHAR(500);

-- Comentário sobre os novos campos:
-- web_endpoint: URL do endpoint FCM/Mozilla para envio
-- web_p256dh: Chave pública para criptografia (base64)
-- web_auth: Chave de autenticação para verificação (base64)

-- Criar índice para busca eficiente por endpoint
CREATE INDEX IF NOT EXISTS idx_user_push_tokens_web_endpoint 
ON user_push_tokens(web_endpoint) 
WHERE web_endpoint IS NOT NULL;