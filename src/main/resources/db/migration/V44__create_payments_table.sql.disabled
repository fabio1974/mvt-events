-- ============================================================================
-- Migration V44: Create payments table for deliveries
-- ============================================================================
-- Description: Creates payments table to track delivery payments
-- Author: System
-- Date: 2025-10-23
-- ============================================================================

-- ============================================================================
-- CREATE TABLE: payments
-- ============================================================================
CREATE TABLE IF NOT EXISTS payments (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Relationships
    delivery_id BIGINT NOT NULL,
    payer_id UUID NOT NULL,
    organization_id BIGINT,

    -- Payment Info
    transaction_id VARCHAR(100) UNIQUE,
    amount DECIMAL(10, 2) NOT NULL,
    payment_method VARCHAR(20),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    payment_date TIMESTAMP,

    -- Provider Info
    provider VARCHAR(50),
    provider_payment_id VARCHAR(100),

    -- Metadata
    notes TEXT,
    metadata JSONB,

    -- Foreign Keys
    CONSTRAINT fk_payment_delivery FOREIGN KEY (delivery_id)
        REFERENCES deliveries(id) ON DELETE CASCADE,
    CONSTRAINT fk_payment_payer FOREIGN KEY (payer_id)
        REFERENCES users(id) ON DELETE RESTRICT,
    CONSTRAINT fk_payment_organization FOREIGN KEY (organization_id)
        REFERENCES organizations(id) ON DELETE SET NULL,

    -- Constraints
    CONSTRAINT chk_payment_amount CHECK (amount > 0),
    CONSTRAINT chk_payment_status CHECK (status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED', 'REFUNDED', 'CANCELLED')),
    CONSTRAINT chk_payment_method CHECK (payment_method IN ('CREDIT_CARD', 'DEBIT_CARD', 'PIX', 'BANK_SLIP', 'CASH', 'WALLET'))
);

-- ============================================================================
-- CREATE INDEXES
-- ============================================================================
CREATE INDEX IF NOT EXISTS idx_payment_delivery ON payments(delivery_id);
CREATE INDEX IF NOT EXISTS idx_payment_payer ON payments(payer_id);
CREATE INDEX IF NOT EXISTS idx_payment_organization ON payments(organization_id);
CREATE INDEX IF NOT EXISTS idx_payment_status ON payments(status);
CREATE INDEX IF NOT EXISTS idx_payment_provider ON payments(provider);
CREATE INDEX IF NOT EXISTS idx_payment_date ON payments(payment_date);
CREATE INDEX IF NOT EXISTS idx_payment_transaction ON payments(transaction_id);

-- ============================================================================
-- COMMENTS
-- ============================================================================
COMMENT ON TABLE payments IS 'Pagamentos de entregas';
COMMENT ON COLUMN payments.delivery_id IS 'ID da entrega relacionada';
COMMENT ON COLUMN payments.payer_id IS 'ID do usuário que está pagando';
COMMENT ON COLUMN payments.organization_id IS 'ID da organização que receberá o pagamento';
COMMENT ON COLUMN payments.transaction_id IS 'ID único da transação (gerado internamente)';
COMMENT ON COLUMN payments.amount IS 'Valor do pagamento';
COMMENT ON COLUMN payments.payment_method IS 'Método de pagamento utilizado';
COMMENT ON COLUMN payments.status IS 'Status do pagamento';
COMMENT ON COLUMN payments.payment_date IS 'Data/hora em que o pagamento foi concluído';
COMMENT ON COLUMN payments.provider IS 'Provedor de pagamento (stripe, mercadopago, etc)';
COMMENT ON COLUMN payments.provider_payment_id IS 'ID do pagamento no provedor externo';
COMMENT ON COLUMN payments.metadata IS 'Dados adicionais em formato JSON';
