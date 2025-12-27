-- Renomeia o status COMPLETED para PAID na tabela payments
-- Isso alinha o enum PaymentStatus com a nomenclatura correta
-- PAID = Pago (Payment), COMPLETED = Concluída (Delivery)

UPDATE payments 
SET status = 'PAID' 
WHERE status = 'COMPLETED';
