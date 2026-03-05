-- V70: Adiciona referência ao cartão utilizado no pagamento (apenas CREDIT_CARD)
-- Permite rastrear qual CustomerCard foi usado em cada pagamento por cartão.

ALTER TABLE payments
    ADD COLUMN IF NOT EXISTS customer_card_id BIGINT;

ALTER TABLE payments
    ADD CONSTRAINT fk_payments_customer_card
    FOREIGN KEY (customer_card_id)
    REFERENCES customer_cards(id)
    ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_payments_customer_card_id
    ON payments(customer_card_id);

COMMENT ON COLUMN payments.customer_card_id IS
    'Referência ao CustomerCard utilizado (apenas quando payment_method = CREDIT_CARD). NULL para PIX.';
