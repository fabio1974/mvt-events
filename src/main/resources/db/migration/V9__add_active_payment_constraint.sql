-- Adiciona constraint para garantir apenas um pagamento ativo por delivery
-- Um pagamento ativo é considerado PENDING ou COMPLETED
-- Esta migration cria um índice único parcial que impede múltiplos pagamentos ativos

-- IMPORTANTE: Esta constraint usa um índice único parcial (partial unique index)
-- que só é aplicado quando o pagamento está PENDING ou COMPLETED

-- Primeiro, criar uma função helper para verificar se o pagamento está ativo
CREATE OR REPLACE FUNCTION is_payment_active(payment_id BIGINT) 
RETURNS BOOLEAN AS $$
BEGIN
    RETURN EXISTS (
        SELECT 1 FROM payments 
        WHERE id = payment_id 
        AND status IN ('PENDING', 'COMPLETED')
    );
END;
$$ LANGUAGE plpgsql IMMUTABLE;

-- Criar índice único parcial na tabela payment_deliveries
-- Este índice garante que cada delivery_id apareça apenas UMA VEZ
-- quando associado a um pagamento ATIVO (PENDING ou COMPLETED)
CREATE UNIQUE INDEX idx_unique_active_payment_per_delivery 
ON payment_deliveries (delivery_id)
WHERE is_payment_active(payment_id);

-- Adicionar comentário explicativo
COMMENT ON INDEX idx_unique_active_payment_per_delivery IS 
'Garante que cada delivery tenha no máximo UM pagamento ativo (PENDING ou COMPLETED). 
Permite múltiplos pagamentos FAILED, CANCELLED ou REFUNDED para a mesma delivery.';

COMMENT ON FUNCTION is_payment_active(BIGINT) IS 
'Função helper que retorna TRUE se o pagamento está com status PENDING ou COMPLETED (ativo).';
