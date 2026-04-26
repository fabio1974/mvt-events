-- Chat de suporte assíncrono entre usuários (CLIENT/CUSTOMER/COURIER/ORGANIZER/WAITER) e admins.
-- v1.0: 1 thread por usuário (não por ticket). Só user inicia. Admin responde.
-- Push notification ao admin quando user envia, e ao user quando admin responde.

CREATE TABLE support_messages (
    id          BIGSERIAL PRIMARY KEY,
    user_id     UUID NOT NULL REFERENCES users(id),
    from_admin  BOOLEAN NOT NULL DEFAULT false,
    text        TEXT NOT NULL,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    read_at     TIMESTAMP WITH TIME ZONE,
    resolved_at TIMESTAMP WITH TIME ZONE
);

-- Listagem cronológica do thread por usuário (UI pega do mais recente)
CREATE INDEX idx_support_messages_user ON support_messages(user_id, created_at DESC);

-- Badge de não-lidas: queries do tipo "unread por user"
-- Partial index — só linhas não lidas (mais leve, mais rápido)
CREATE INDEX idx_support_messages_unread
    ON support_messages(user_id, from_admin)
    WHERE read_at IS NULL;
