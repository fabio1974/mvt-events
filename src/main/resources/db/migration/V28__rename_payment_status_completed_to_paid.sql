-- Renomeia o status COMPLETED para PAID na tabela payments
-- Isso alinha o enum PaymentStatus com a nomenclatura correta
-- PAID = Pago (Payment), COMPLETED = Concluída (Delivery)

-- 1. Remove a constraint antiga que valida os status
ALTER TABLE payments DROP CONSTRAINT IF EXISTS payments_payment_status_check;

-- 2. Atualiza os valores no banco
UPDATE payments 
SET status = 'PAID' 
WHERE status = 'COMPLETED';

-- 3. Cria a nova constraint com os valores corretos (incluindo PAID)
ALTER TABLE payments ADD CONSTRAINT payments_payment_status_check 
    CHECK (status IN ('PENDING', 'PROCESSING', 'PAID', 'FAILED', 'REFUNDED', 'CANCELLED', 'EXPIRED'));
