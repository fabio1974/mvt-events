-- Campo table_number denormalizado no pedido (mantém histórico mesmo se mesa for deletada)
ALTER TABLE orders ADD COLUMN table_number INTEGER;

-- Preencher retroativamente com o número das mesas existentes
UPDATE orders o SET table_number = rt.number
FROM restaurant_tables rt WHERE o.table_id = rt.id AND o.table_number IS NULL;

-- Limpar FK de pedidos finalizados (liberar mesas para deleção)
UPDATE orders SET table_id = NULL
WHERE table_id IS NOT NULL AND status IN ('COMPLETED', 'CANCELLED');
