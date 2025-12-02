-- Add PayPal Account payment method
ALTER TABLE payments DROP CONSTRAINT IF EXISTS payments_payment_method_check;
ALTER TABLE payments ADD CONSTRAINT payments_payment_method_check CHECK (payment_method IN (
    'CREDIT_CARD', 'DEBIT_CARD', 'PIX', 'BANK_TRANSFER', 'PAYPAL_ACCOUNT', 'CASH', 'OTHER'
));