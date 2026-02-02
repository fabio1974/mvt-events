-- ============================================================================
-- V45: Criar tabela customer_cards para cartões tokenizados
-- ============================================================================
-- Descrição: Tabela para armazenar cartões de crédito tokenizados dos clientes.
--            Segue padrões PCI Compliance: não armazena número completo nem CVV.
--            Apenas token do Pagar.me + últimos 4 dígitos + bandeira.
-- 
-- Funcionalidades:
-- - Múltiplos cartões por cliente
-- - Um cartão padrão (is_default)
-- - Soft delete (mantém histórico)
-- - Verificação de expiração
-- ============================================================================

CREATE TABLE customer_cards (
    -- Primary Key
    id BIGSERIAL PRIMARY KEY,
    
    -- Timestamps (herança de BaseEntity)
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Foreign Keys
    customer_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    
    -- Pagar.me Token (PCI Compliance)
    pagarme_card_id VARCHAR(100) NOT NULL UNIQUE,
    
    -- Informações para Exibição (não sensíveis)
    last_four_digits VARCHAR(4) NOT NULL,
    brand VARCHAR(20) NOT NULL CHECK (brand IN ('VISA', 'MASTERCARD', 'AMEX', 'ELO', 'HIPERCARD', 'DINERS', 'DISCOVER', 'JCB', 'OTHER')),
    holder_name VARCHAR(100),
    exp_month INTEGER CHECK (exp_month BETWEEN 1 AND 12),
    exp_year INTEGER CHECK (exp_year >= 2020),
    
    -- Controle e Status
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    is_verified BOOLEAN NOT NULL DEFAULT FALSE,
    verified_at TIMESTAMP(6),
    last_used_at TIMESTAMP(6),
    deleted_at TIMESTAMP(6),
    
    -- Constraints
    CONSTRAINT uk_customer_pagarme_card UNIQUE (customer_id, pagarme_card_id)
);

-- Índices para performance
CREATE INDEX idx_customer_cards_customer_id ON customer_cards(customer_id);
CREATE INDEX idx_customer_cards_is_default ON customer_cards(customer_id, is_default) WHERE is_default = TRUE AND deleted_at IS NULL;
CREATE INDEX idx_customer_cards_active ON customer_cards(customer_id, is_active) WHERE is_active = TRUE AND deleted_at IS NULL;
CREATE INDEX idx_customer_cards_deleted_at ON customer_cards(deleted_at) WHERE deleted_at IS NOT NULL;

-- Comentários
COMMENT ON TABLE customer_cards IS 'Cartões de crédito tokenizados dos clientes (PCI compliant)';
COMMENT ON COLUMN customer_cards.pagarme_card_id IS 'ID do cartão tokenizado no Pagar.me (card_xxxxx) - NUNCA armazena número real';
COMMENT ON COLUMN customer_cards.last_four_digits IS 'Últimos 4 dígitos para exibição ao usuário';
COMMENT ON COLUMN customer_cards.brand IS 'Bandeira do cartão (Visa, Mastercard, Elo, etc)';
COMMENT ON COLUMN customer_cards.is_default IS 'Se é o cartão padrão do cliente (usado por default)';
COMMENT ON COLUMN customer_cards.deleted_at IS 'Soft delete - mantém histórico para auditoria';
