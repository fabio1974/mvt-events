-- Migration V24: Remove coluna pagarme_order_id da tabela payments
-- Os valores agora são salvos no campo provider_payment_id que já existe

-- Remove a coluna pagarme_order_id
ALTER TABLE payments DROP COLUMN IF EXISTS pagarme_order_id;

-- Comentário explicativo
COMMENT ON COLUMN payments.provider_payment_id IS 'ID do pagamento no gateway de pagamento (Pagar.me Order ID, etc)';
