-- Adiciona campos de controle de repasse ao PayoutItem
-- Permite rastrear status, pagamento e beneficiário de cada repasse individual

-- Adicionar coluna de beneficiário
ALTER TABLE payout_items 
ADD COLUMN beneficiary_id UUID;

-- Adicionar foreign key para users
ALTER TABLE payout_items 
ADD CONSTRAINT fk_payout_items_beneficiary 
FOREIGN KEY (beneficiary_id) 
REFERENCES users(id);

-- Adicionar coluna de status
ALTER TABLE payout_items 
ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'PENDING';

-- Adicionar coluna de data de pagamento
ALTER TABLE payout_items 
ADD COLUMN paid_at TIMESTAMP;

-- Adicionar coluna de referência de pagamento
ALTER TABLE payout_items 
ADD COLUMN payment_reference VARCHAR(100);

-- Adicionar coluna de método de pagamento
ALTER TABLE payout_items 
ADD COLUMN payment_method VARCHAR(20);

-- Adicionar coluna de observações
ALTER TABLE payout_items 
ADD COLUMN notes TEXT;

-- Adicionar novos tipos de valor
COMMENT ON COLUMN payout_items.value_type IS 'Tipos: COURIER_AMOUNT, ADM_COMMISSION, SYSTEM_FEE, PLATFORM_FEE, OTHER';
COMMENT ON COLUMN payout_items.status IS 'Status: PENDING, PROCESSING, PAID, FAILED, CANCELLED';
COMMENT ON COLUMN payout_items.payment_method IS 'Métodos: PIX, BANK_TRANSFER, CASH, WALLET, OTHER';

-- Criar índices para melhor performance
CREATE INDEX idx_payout_items_beneficiary_id ON payout_items(beneficiary_id);
CREATE INDEX idx_payout_items_status ON payout_items(status);
CREATE INDEX idx_payout_items_value_type ON payout_items(value_type);
CREATE INDEX idx_payout_items_paid_at ON payout_items(paid_at);

-- Comentários sobre os novos campos
COMMENT ON COLUMN payout_items.beneficiary_id IS 'Beneficiário que receberá este repasse. Deve ser preenchido manualmente ao criar o PayoutItem.';
COMMENT ON COLUMN payout_items.status IS 'Status: PENDING, PROCESSING, PAID, FAILED, CANCELLED';
COMMENT ON COLUMN payout_items.payment_method IS 'Métodos: PIX, BANK_TRANSFER, CASH, WALLET, OTHER';

