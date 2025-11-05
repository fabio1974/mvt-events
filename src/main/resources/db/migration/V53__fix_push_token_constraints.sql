-- Migration V53: Fix device_type constraint to accept uppercase values
-- Corrigir constraint para aceitar valores em maiúscula conforme enum Java

-- Remover constraint existente
ALTER TABLE user_push_tokens DROP CONSTRAINT IF EXISTS user_push_tokens_device_type_check;

-- Adicionar nova constraint que aceita valores em maiúscula
ALTER TABLE user_push_tokens 
ADD CONSTRAINT user_push_tokens_device_type_check 
CHECK (device_type IN ('MOBILE', 'WEB', 'TABLET'));

-- Atualizar constraint de platform também para consistência
ALTER TABLE user_push_tokens DROP CONSTRAINT IF EXISTS user_push_tokens_platform_check;
ALTER TABLE user_push_tokens 
ADD CONSTRAINT user_push_tokens_platform_check 
CHECK (platform IN ('IOS', 'ANDROID', 'WEB'));

-- Comentário
COMMENT ON CONSTRAINT user_push_tokens_device_type_check ON user_push_tokens 
IS 'Valores válidos: MOBILE, WEB, TABLET (em maiúscula conforme enum Java)';

COMMENT ON CONSTRAINT user_push_tokens_platform_check ON user_push_tokens 
IS 'Valores válidos: IOS, ANDROID, WEB (em maiúscula conforme enum Java)';