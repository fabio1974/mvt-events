-- ============================================================================
-- Migration V49: Create customer_payment_preferences table
-- ============================================================================
-- Armazena as preferências de pagamento de cada cliente:
-- - Tipo preferido: PIX ou CREDIT_CARD
-- - Cartão padrão (quando tipo é CREDIT_CARD)
-- ============================================================================

CREATE TABLE customer_payment_preferences (
    id BIGSERIAL PRIMARY KEY,
    user_id UUID NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    preferred_payment_type VARCHAR(20) NOT NULL DEFAULT 'PIX',
    default_card_id BIGINT REFERENCES customer_cards(id) ON DELETE SET NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    
    -- Constraint: tipo deve ser PIX ou CREDIT_CARD
    CONSTRAINT chk_preferred_payment_type CHECK (preferred_payment_type IN ('PIX', 'CREDIT_CARD'))
);

-- Index para busca rápida por user_id
CREATE INDEX idx_customer_payment_preferences_user_id ON customer_payment_preferences(user_id);

-- Comment
COMMENT ON TABLE customer_payment_preferences IS 'Preferências de pagamento dos clientes (PIX ou cartão de crédito)';
COMMENT ON COLUMN customer_payment_preferences.preferred_payment_type IS 'Tipo de pagamento preferido: PIX ou CREDIT_CARD';
COMMENT ON COLUMN customer_payment_preferences.default_card_id IS 'Cartão padrão quando tipo é CREDIT_CARD (pode ser null para PIX)';
