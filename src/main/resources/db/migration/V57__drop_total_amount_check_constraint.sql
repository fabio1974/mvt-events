-- V57: Remover CHECK constraint de total_amount
-- RIDE (viagem) pode ter total_amount = 0 ou null
ALTER TABLE deliveries DROP CONSTRAINT IF EXISTS chk_delivery_total_amount;
