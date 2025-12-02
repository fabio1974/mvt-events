-- ============================================================================
-- V30: Refatorar tabela payments para split de valores
-- ============================================================================
-- Adiciona colunas para divisão de valores:
-- - courier_amount: valor do entregador
-- - adm_amount: comissão do ADM
-- - platform_amount: taxa da plataforma
-- Total deve sempre = courier_amount + adm_amount + platform_amount
-- ============================================================================

-- Adicionar novas colunas
ALTER TABLE payments ADD COLUMN courier_amount DECIMAL(12, 2);
ALTER TABLE payments ADD COLUMN adm_amount DECIMAL(12, 2);
ALTER TABLE payments ADD COLUMN platform_amount DECIMAL(12, 2);

-- Adicionar constraints
ALTER TABLE payments ADD CONSTRAINT chk_payment_courier_amount 
    CHECK (courier_amount IS NULL OR courier_amount >= 0);

ALTER TABLE payments ADD CONSTRAINT chk_payment_adm_amount 
    CHECK (adm_amount IS NULL OR adm_amount >= 0);

ALTER TABLE payments ADD CONSTRAINT chk_payment_platform_amount 
    CHECK (platform_amount IS NULL OR platform_amount >= 0);

-- Constraint para validar split (quando presente, deve somar o total)
ALTER TABLE payments ADD CONSTRAINT chk_payment_split_sum
    CHECK (
        (courier_amount IS NULL AND adm_amount IS NULL AND platform_amount IS NULL)
        OR
        (amount = COALESCE(courier_amount, 0) + COALESCE(adm_amount, 0) + COALESCE(platform_amount, 0))
    );

-- Índices para otimizar queries de repasse
CREATE INDEX idx_payment_courier_amount ON payments(courier_amount) WHERE courier_amount IS NOT NULL;
CREATE INDEX idx_payment_adm_amount ON payments(adm_amount) WHERE adm_amount IS NOT NULL;
CREATE INDEX idx_payment_platform_amount ON payments(platform_amount) WHERE platform_amount IS NOT NULL;

-- Comentários
COMMENT ON COLUMN payments.courier_amount IS 'Valor destinado ao entregador (parte do amount total)';
COMMENT ON COLUMN payments.adm_amount IS 'Comissão do ADM (parte do amount total)';
COMMENT ON COLUMN payments.platform_amount IS 'Taxa da plataforma (parte do amount total)';

-- ============================================================================
-- Migração de dados existentes (se necessário)
-- ============================================================================
-- Os dados existentes permanecerão com as novas colunas NULL
-- Ao criar novos payments, o sistema deve preencher o split automaticamente
-- Pagamentos antigos podem ser migrados posteriormente por script específico
-- ============================================================================
