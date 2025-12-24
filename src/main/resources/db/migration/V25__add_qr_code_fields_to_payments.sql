-- Migration V25: Adicionar campos qr_code e qr_code_url na tabela payments
-- Esses campos armazenam os valores de charges[0].last_transaction do response do Pagar.me

-- Adicionar coluna qr_code (texto do QR code para pagamento PIX)
ALTER TABLE payments ADD COLUMN IF NOT EXISTS qr_code TEXT;

-- Adicionar coluna qr_code_url (URL da imagem do QR code)
ALTER TABLE payments ADD COLUMN IF NOT EXISTS qr_code_url TEXT;

-- Comentários explicativos
COMMENT ON COLUMN payments.qr_code IS 'QR code do PIX extraído de charges[0].last_transaction.qr_code do response Pagar.me';
COMMENT ON COLUMN payments.qr_code_url IS 'URL do QR code do PIX extraído de charges[0].last_transaction.qr_code_url do response Pagar.me';
