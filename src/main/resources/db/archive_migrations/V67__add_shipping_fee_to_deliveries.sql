-- Adiciona coluna shipping_fee à tabela deliveries
-- Campo para armazenar o valor do frete da entrega

ALTER TABLE deliveries 
ADD COLUMN IF NOT EXISTS shipping_fee NUMERIC(10, 2);

-- Adicionar comentário
COMMENT ON COLUMN deliveries.shipping_fee IS 'Valor do frete da entrega';

-- Para entregas existentes, pode-se copiar o valor de total_amount se necessário
-- UPDATE deliveries SET shipping_fee = total_amount WHERE shipping_fee IS NULL;
