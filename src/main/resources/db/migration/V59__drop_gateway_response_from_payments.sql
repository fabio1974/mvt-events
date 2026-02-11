-- Migration V59: Remove coluna gateway_response redundante
-- A coluna 'response' (JSONB) já contém o gateway_response completo dentro dela

-- 1. Dropar coluna gateway_response (redundante)
ALTER TABLE payments
    DROP COLUMN IF EXISTS gateway_response;

-- 2. Comentário atualizado na coluna response
COMMENT ON COLUMN payments.response IS 'Response completo retornado pelo gateway de pagamento (Pagar.me, Iugu, etc.). Inclui todos os dados: order ID, status, charges, PIX data, timestamps, gateway_response, antifraud, acquirer, etc.';

-- 3. Verificar estrutura final
DO $$
BEGIN
    RAISE NOTICE '✅ Migration V59 aplicada com sucesso!';
    RAISE NOTICE '   ├─ Coluna removida: gateway_response (redundante)';
    RAISE NOTICE '   └─ Uso: response JSONB contém o response completo do Pagar.me';
END $$;
