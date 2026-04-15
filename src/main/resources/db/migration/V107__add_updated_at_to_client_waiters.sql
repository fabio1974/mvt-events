-- Adicionar updated_at à client_waiters (requerido por BaseEntity)
ALTER TABLE client_waiters ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ DEFAULT NOW();
