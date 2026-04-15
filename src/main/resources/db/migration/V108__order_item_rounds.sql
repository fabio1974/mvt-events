-- Adicionar rodada (round) e timestamp de envio à cozinha nos itens do pedido
ALTER TABLE order_items ADD COLUMN IF NOT EXISTS round INT NOT NULL DEFAULT 1;
ALTER TABLE order_items ADD COLUMN IF NOT EXISTS sent_at TIMESTAMPTZ DEFAULT NOW();
