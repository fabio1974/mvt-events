-- ============================================================================
-- V46: Adicionar pagarme_customer_id ao users (para CUSTOMER/CLIENT)
-- ============================================================================
-- Descrição: Campo para armazenar ID do customer no Pagar.me (cus_xxxxx).
--            Necessário para gerenciar cartões tokenizados via API Pagar.me.
--            Separado de pagarme_recipient_id (usado para COURIER receber pagamentos).
-- 
-- Uso:
-- - CUSTOMER/CLIENT → pagarme_customer_id (para pagar)
-- - COURIER → pagarme_recipient_id (para receber)
-- ============================================================================

ALTER TABLE users ADD COLUMN pagarme_customer_id VARCHAR(100);

-- Índice para busca rápida
CREATE INDEX idx_users_pagarme_customer_id ON users(pagarme_customer_id) WHERE pagarme_customer_id IS NOT NULL;

-- Comentário
COMMENT ON COLUMN users.pagarme_customer_id IS 'ID do customer no Pagar.me (cus_xxxxx) - usado para gerenciar cartões e pagamentos';
