-- ============================================================================
-- Migration V78: Drop is_default column from customer_cards
-- ============================================================================
-- Remover coluna is_default redundante da tabela customer_cards.
-- O cartão padrão agora é identificado por customer_payment_preferences.default_card_id
-- 
-- MOTIVO:
-- - Redundância: is_default duplica informação já presente em customer_payment_preferences
-- - Race conditions: Múltiplos UPDATEs podiam deixar estado inconsistente
-- - Fonte única da verdade: customer_payment_preferences.default_card_id
--
-- COMPATIBILIDADE:
-- - API continua retornando campo isDefault (calculado dinamicamente via @Transient)
-- - Mobile/Frontend não precisa de mudanças
-- ============================================================================

-- Dropar coluna is_default
ALTER TABLE customer_cards DROP COLUMN IF EXISTS is_default;

-- Comentário explicativo
COMMENT ON TABLE customer_cards IS 'Cartões de crédito tokenizados. Cartão padrão identificado por customer_payment_preferences.default_card_id';
