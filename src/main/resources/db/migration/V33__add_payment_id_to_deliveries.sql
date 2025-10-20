-- ============================================================================
-- V33: Adicionar coluna payment_id na tabela deliveries
-- ============================================================================
-- Permite relacionamento entre Delivery e Payment
-- Para suportar o mapeamento @OneToOne com @JoinColumn em Delivery.payment
-- ============================================================================

-- ============================================================================
-- TABELA: DELIVERIES - Adicionar payment_id
-- ============================================================================

-- Adicionar coluna payment_id (nullable para deliveries existentes)
ALTER TABLE deliveries ADD COLUMN payment_id BIGINT;

-- Adicionar foreign key constraint
ALTER TABLE deliveries ADD CONSTRAINT fk_delivery_payment 
    FOREIGN KEY (payment_id) REFERENCES payments(id) ON DELETE SET NULL;

-- Criar índice para performance
CREATE INDEX idx_delivery_payment ON deliveries(payment_id);

-- ============================================================================
-- COMENTÁRIOS
-- ============================================================================

COMMENT ON COLUMN deliveries.payment_id IS 'Referência ao pagamento desta entrega (pode ser NULL)';
COMMENT ON CONSTRAINT fk_delivery_payment ON deliveries IS 'Foreign key para pagamento da entrega';