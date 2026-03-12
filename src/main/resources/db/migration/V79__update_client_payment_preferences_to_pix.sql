-- ============================================================================
-- Migration V79: Update payment preferences for CLIENTs to PIX only
-- ============================================================================
-- CLIENTs de estabelecimento não devem usar cartão de crédito.
-- Atualiza todas as preferências de CLIENTs para PIX e remove cartão padrão.
-- CUSTOMERs (clientes finais) mantêm suas preferências atuais.
-- ============================================================================

-- 1. Criar preferências PIX para CLIENTs que ainda não têm preferência
INSERT INTO customer_payment_preferences (user_id, preferred_payment_type, default_card_id, created_at, updated_at)
SELECT 
    u.id,
    'PIX',
    NULL,
    NOW(),
    NOW()
FROM users u
WHERE u.role = 'CLIENT'
  AND u.id NOT IN (SELECT user_id FROM customer_payment_preferences);

-- 2. Atualizar preferências existentes de CLIENTs para PIX (remover cartão padrão)
UPDATE customer_payment_preferences cpp
SET 
    preferred_payment_type = 'PIX',
    default_card_id = NULL,
    updated_at = NOW()
FROM users u
WHERE cpp.user_id = u.id
  AND u.role = 'CLIENT'
  AND (cpp.preferred_payment_type != 'PIX' OR cpp.default_card_id IS NOT NULL);

-- Comentário explicativo
COMMENT ON TABLE customer_payment_preferences IS 'Preferências de pagamento: CLIENTs (estabelecimentos) usam apenas PIX, CUSTOMERs (clientes finais) podem usar PIX ou CREDIT_CARD';
