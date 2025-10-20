-- ============================================================================
-- V29: Criar tabela payout_items
-- ============================================================================
-- Tabela intermediária entre payments e unified_payouts
-- Permite rastrear quais pagamentos compõem cada repasse
-- ============================================================================

CREATE TABLE payout_items (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Relacionamentos
    payout_id BIGINT NOT NULL,
    payment_id BIGINT NOT NULL,
    
    -- Valor e tipo
    item_value DECIMAL(12, 2) NOT NULL,
    value_type VARCHAR(20) NOT NULL,
    
    -- Foreign Keys
    CONSTRAINT fk_item_payout FOREIGN KEY (payout_id) REFERENCES unified_payouts(id) ON DELETE CASCADE,
    CONSTRAINT fk_item_payment FOREIGN KEY (payment_id) REFERENCES payments(id) ON DELETE RESTRICT,
    
    -- Constraints
    CONSTRAINT uk_payout_payment UNIQUE (payout_id, payment_id),
    CONSTRAINT chk_item_value CHECK (item_value >= 0),
    CONSTRAINT chk_value_type CHECK (value_type IN ('COURIER_AMOUNT', 'ADM_COMMISSION', 'PLATFORM_AMOUNT'))
);

-- Índices
CREATE INDEX idx_payout_item_payout ON payout_items(payout_id);
CREATE INDEX idx_payout_item_payment ON payout_items(payment_id);
CREATE INDEX idx_payout_item_value_type ON payout_items(value_type);

-- Índice para prevenir duplicatas e queries
CREATE UNIQUE INDEX uk_payment_value_type ON payout_items(payment_id, value_type);

-- Comentários
COMMENT ON TABLE payout_items IS 'Tabela intermediária entre payments e unified_payouts';
COMMENT ON COLUMN payout_items.value_type IS 'Tipo do valor: COURIER_AMOUNT (valor do entregador), ADM_COMMISSION (comissão do ADM), PLATFORM_AMOUNT (taxa da plataforma)';
COMMENT ON COLUMN payout_items.item_value IS 'Valor específico deste item (parte do payment total)';
