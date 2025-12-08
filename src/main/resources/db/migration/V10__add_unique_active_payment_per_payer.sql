-- Adiciona constraint para garantir apenas um pagamento ativo por cliente (payer)
-- Um pagamento ativo é considerado PENDING ou COMPLETED
-- Esta migration cria um índice único parcial que impede que um cliente tenha múltiplos pagamentos ativos simultaneamente

-- IMPORTANTE: Esta constraint usa um índice único parcial (partial unique index)
-- que só é aplicado quando o pagamento está PENDING ou COMPLETED

-- PASSO 1: Identificar e cancelar payments duplicados (manter apenas o mais recente por payer)
-- Para cada payer que tem múltiplos payments PENDING ou PAID, vamos:
-- 1. Manter o payment mais recente (maior id ou created_at)
-- 2. Marcar os outros como CANCELLED

WITH duplicate_payments AS (
    SELECT 
        id,
        payer_id,
        status,
        created_at,
        ROW_NUMBER() OVER (
            PARTITION BY payer_id 
            ORDER BY created_at DESC, id DESC
        ) as rn
    FROM payments
    WHERE status IN ('PENDING', 'COMPLETED')
)
UPDATE payments
SET 
    status = 'CANCELLED',
    updated_at = NOW()
WHERE id IN (
    SELECT id FROM duplicate_payments WHERE rn > 1
)
RETURNING id, payer_id, status;

-- PASSO 2: Criar índice único parcial na tabela payments
-- Este índice garante que cada payer_id apareça apenas UMA VEZ
-- quando o pagamento está ATIVO (PENDING ou COMPLETED)
CREATE UNIQUE INDEX idx_unique_active_payment_per_payer 
ON payments (payer_id)
WHERE status IN ('PENDING', 'COMPLETED');

-- Adicionar comentário explicativo
COMMENT ON INDEX idx_unique_active_payment_per_payer IS 
'Garante que cada cliente (payer) tenha no máximo UM pagamento ativo (PENDING ou COMPLETED) por vez. 
Permite múltiplos pagamentos FAILED, CANCELLED ou REFUNDED para o mesmo cliente.
Isso previne que o cliente crie múltiplas faturas (invoices) ativas simultaneamente.';

