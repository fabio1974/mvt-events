-- Campos de billing no payments para suportar pagamentos recorrentes
ALTER TABLE payments ADD COLUMN payment_type VARCHAR(30) NOT NULL DEFAULT 'DELIVERY';
ALTER TABLE payments ADD COLUMN subscription_id BIGINT REFERENCES client_subscriptions(id);
ALTER TABLE payments ADD COLUMN billing_period_start DATE;
ALTER TABLE payments ADD COLUMN billing_period_end DATE;
ALTER TABLE payments ADD COLUMN due_date DATE;
ALTER TABLE payments ADD COLUMN reference_month VARCHAR(7);
ALTER TABLE payments ADD COLUMN prorata BOOLEAN NOT NULL DEFAULT false;

-- Índice para busca rápida de faturas por subscription + mês
CREATE INDEX idx_payments_subscription_month
    ON payments (subscription_id, reference_month) WHERE subscription_id IS NOT NULL;
