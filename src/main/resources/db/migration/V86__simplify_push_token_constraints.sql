-- Remove a constraint composta (user_id, token) que causa conflito com o partial index.
-- A regra de negócio é: um token de dispositivo só pode estar ativo para UM usuário por vez.
-- O partial unique index ux_user_push_tokens_active_token já garante isso.
-- A constraint uk_user_push_tokens_user_token é redundante e impede o UPSERT
-- de transferir o token quando um user diferente faz login no mesmo dispositivo.

-- 1) Dropar a constraint composta (user_id, token)
ALTER TABLE user_push_tokens DROP CONSTRAINT IF EXISTS uk_user_push_tokens_user_token;

-- 2) Garantir que o partial unique index ainda existe (idempotente)
CREATE UNIQUE INDEX IF NOT EXISTS ux_user_push_tokens_active_token
    ON user_push_tokens(token)
    WHERE is_active = true;
