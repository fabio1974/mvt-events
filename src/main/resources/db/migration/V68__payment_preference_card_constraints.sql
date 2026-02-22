-- ============================================================================
-- V68: Constraints de integridade para preferência de pagamento e cartão default
-- ============================================================================

-- 0. LIMPEZA: Remover preferências inválidas antes de criar constraints
--    (ex: CREDIT_CARD sem default_card_id, ou default_card_id apontando para cartão deletado)
DELETE FROM customer_payment_preferences
WHERE preferred_payment_type = 'CREDIT_CARD'
  AND (
      default_card_id IS NULL
      OR default_card_id NOT IN (
          SELECT id FROM customer_cards WHERE deleted_at IS NULL AND is_active = TRUE
      )
  );

-- 1. CHECK: Se preferência = CREDIT_CARD, obrigatoriamente ter default_card_id
--    Impede o estado inválido: "prefere cartão mas não tem cartão default"
ALTER TABLE customer_payment_preferences
    ADD CONSTRAINT chk_credit_card_requires_default_card
    CHECK (
        preferred_payment_type != 'CREDIT_CARD' OR default_card_id IS NOT NULL
    );

-- 2. UNIQUE PARTIAL INDEX: Apenas UM cartão default por customer
--    Impede dois cartões com is_default = true para o mesmo customer
CREATE UNIQUE INDEX idx_one_default_card_per_customer
    ON customer_cards (customer_id)
    WHERE is_default = TRUE AND deleted_at IS NULL;

-- 3. FK: Garantir que default_card_id referencia um cartão real
--    (Se já existe a FK, este ALTER será ignorado pelo IF NOT EXISTS)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_name = 'fk_preference_default_card'
        AND table_name = 'customer_payment_preferences'
    ) THEN
        ALTER TABLE customer_payment_preferences
            ADD CONSTRAINT fk_preference_default_card
            FOREIGN KEY (default_card_id) REFERENCES customer_cards(id);
    END IF;
END $$;
