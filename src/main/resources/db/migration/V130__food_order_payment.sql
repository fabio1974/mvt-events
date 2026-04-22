-- Pagamento de FoodOrder (Zapi-Food)
-- Fase 1: apenas PIX, com timing AT_CHECKOUT ou ON_DELIVERY.
-- Split no checkout (3 recipients fixos): client 87% comida / organizer 5% total / plataforma resto.
-- 87% do frete fica retido com plataforma e transferido ao courier no accept.

ALTER TABLE orders
    ADD COLUMN customer_payment_method VARCHAR(20),                 -- PIX (unico na fase 1)
    ADD COLUMN payment_timing VARCHAR(20),                          -- AT_CHECKOUT | ON_DELIVERY
    ADD COLUMN pagarme_order_id VARCHAR(100),                       -- order_xxx no pagar.me
    ADD COLUMN customer_payment_status VARCHAR(20),                 -- PENDING | PAID | FAILED | CANCELLED
    ADD COLUMN customer_paid_at TIMESTAMPTZ,
    ADD COLUMN pix_qr_code TEXT,                                    -- conteudo do QR (copy-paste)
    ADD COLUMN pix_qr_code_url TEXT,                                -- URL da imagem do QR
    ADD COLUMN pix_expires_at TIMESTAMPTZ;

CREATE INDEX idx_orders_pagarme_order ON orders(pagarme_order_id) WHERE pagarme_order_id IS NOT NULL;
CREATE INDEX idx_orders_customer_payment_status ON orders(customer_payment_status) WHERE customer_payment_status IS NOT NULL;

-- Transfers pagar.me pro courier (auditoria do debito interno plataforma → courier)
CREATE TABLE pagarme_transfers (
    id BIGSERIAL PRIMARY KEY,
    food_order_id BIGINT REFERENCES orders(id),
    delivery_id BIGINT,                                             -- FK solta, deliveries pode nao existir ainda
    recipient_user_id UUID NOT NULL REFERENCES users(id),
    recipient_pagarme_id VARCHAR(100) NOT NULL,
    amount_cents BIGINT NOT NULL CHECK (amount_cents > 0),
    pagarme_transfer_id VARCHAR(100),                               -- id retornado pelo pagar.me
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',                  -- PENDING | SUCCEEDED | FAILED
    error_message TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    executed_at TIMESTAMPTZ,
    CONSTRAINT pagarme_transfer_status_chk CHECK (status IN ('PENDING', 'SUCCEEDED', 'FAILED'))
);

CREATE INDEX idx_pagarme_transfers_food_order ON pagarme_transfers(food_order_id);
CREATE INDEX idx_pagarme_transfers_recipient ON pagarme_transfers(recipient_user_id);
CREATE INDEX idx_pagarme_transfers_status ON pagarme_transfers(status);
