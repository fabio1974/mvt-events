-- ============================================================================
-- V28: Criar tabela unified_payouts
-- ============================================================================
-- Repasses periódicos unificados para Couriers e ADMs
-- Agrupa múltiplos pagamentos em um único repasse
-- ============================================================================

CREATE TABLE unified_payouts (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Beneficiário
    beneficiary_id UUID NOT NULL,
    beneficiary_type VARCHAR(20) NOT NULL,
    
    -- Período
    period VARCHAR(7) NOT NULL, -- Formato: YYYY-MM
    start_date DATE,
    end_date DATE,
    
    -- Financeiro
    total_amount DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    item_count INTEGER DEFAULT 0,
    
    -- Status e pagamento
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    paid_at TIMESTAMP,
    payment_method VARCHAR(20),
    payment_reference VARCHAR(100),
    notes TEXT,
    
    -- Foreign Keys
    CONSTRAINT fk_payout_beneficiary FOREIGN KEY (beneficiary_id) REFERENCES users(id) ON DELETE RESTRICT,
    
    -- Constraints
    CONSTRAINT chk_payout_total_amount CHECK (total_amount >= 0),
    CONSTRAINT chk_payout_item_count CHECK (item_count >= 0),
    CONSTRAINT chk_payout_period CHECK (period ~ '^\d{4}-\d{2}$'),
    CONSTRAINT chk_payout_beneficiary_type CHECK (beneficiary_type IN ('COURIER', 'ADM')),
    CONSTRAINT chk_payout_status CHECK (status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED', 'CANCELLED')),
    CONSTRAINT chk_payout_payment_method CHECK (payment_method IS NULL OR payment_method IN ('PIX', 'BANK_TRANSFER', 'CASH', 'CHECK'))
);

-- Índices
CREATE INDEX idx_payout_beneficiary ON unified_payouts(beneficiary_id);
CREATE INDEX idx_payout_beneficiary_type ON unified_payouts(beneficiary_type);
CREATE INDEX idx_payout_period ON unified_payouts(period);
CREATE INDEX idx_payout_status ON unified_payouts(status);
CREATE INDEX idx_payout_paid_at ON unified_payouts(paid_at DESC) WHERE paid_at IS NOT NULL;

-- Índice composto para queries comuns
CREATE INDEX idx_payout_beneficiary_period ON unified_payouts(beneficiary_id, period);

-- Comentários
COMMENT ON TABLE unified_payouts IS 'Repasses periódicos unificados para Couriers e ADMs';
COMMENT ON COLUMN unified_payouts.period IS 'Período de referência no formato YYYY-MM (ex: 2025-10)';
COMMENT ON COLUMN unified_payouts.beneficiary_type IS 'Tipo: COURIER (repasse de entregas) ou ADM (comissões)';
COMMENT ON COLUMN unified_payouts.status IS 'Status: PENDING, PROCESSING, COMPLETED, FAILED, CANCELLED';
