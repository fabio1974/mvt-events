-- Migration V5: Adicionar campos Iugu em Payment
-- Adiciona campos para integração com Iugu PIX payments

-- 1. Adicionar campos Iugu
ALTER TABLE payments
    ADD COLUMN IF NOT EXISTS iugu_invoice_id VARCHAR(100),
    ADD COLUMN IF NOT EXISTS pix_qr_code TEXT,
    ADD COLUMN IF NOT EXISTS pix_qr_code_url TEXT,
    ADD COLUMN IF NOT EXISTS expires_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS split_rules JSONB;

-- 2. Comentários nos campos
COMMENT ON COLUMN payments.iugu_invoice_id IS 'ID da invoice no Iugu (ex: 1234567890ABCDEF1234567890ABCDEF)';
COMMENT ON COLUMN payments.pix_qr_code IS 'Código PIX Copia e Cola (texto para pagamento)';
COMMENT ON COLUMN payments.pix_qr_code_url IS 'URL da imagem do QR Code PIX';
COMMENT ON COLUMN payments.expires_at IS 'Data/hora de vencimento da invoice PIX';
COMMENT ON COLUMN payments.split_rules IS 'JSON com regras de split (87% motoboy, 5% gerente, 8% plataforma)';

-- 3. Criar índice para busca rápida por iugu_invoice_id
CREATE INDEX IF NOT EXISTS idx_payments_iugu_invoice_id 
    ON payments(iugu_invoice_id) 
    WHERE iugu_invoice_id IS NOT NULL;

-- 4. Criar índice para busca por invoices expiradas
CREATE INDEX IF NOT EXISTS idx_payments_expires_at 
    ON payments(expires_at) 
    WHERE expires_at IS NOT NULL AND status = 'PENDING';

-- 5. Adicionar constraint para garantir iugu_invoice_id único
ALTER TABLE payments
    ADD CONSTRAINT uk_payments_iugu_invoice_id 
    UNIQUE (iugu_invoice_id);

-- 6. Verificar estrutura final
DO $$
BEGIN
    RAISE NOTICE '✅ Migration V5 aplicada com sucesso!';
    RAISE NOTICE '   ├─ Campos adicionados: iugu_invoice_id, pix_qr_code, pix_qr_code_url, expires_at, split_rules';
    RAISE NOTICE '   ├─ Índices criados: idx_payments_iugu_invoice_id, idx_payments_expires_at';
    RAISE NOTICE '   └─ Constraint: uk_payments_iugu_invoice_id';
END $$;
