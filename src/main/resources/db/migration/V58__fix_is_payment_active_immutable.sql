-- =====================================================================
-- FIX CRÍTICO: Índice parcial com função IMMUTABLE que consulta outra tabela
-- 
-- PROBLEMA: is_payment_active() é IMMUTABLE mas consulta a tabela payments.
-- O PostgreSQL exige IMMUTABLE para partial indexes, mas como a função
-- consulta dados mutáveis, o índice fica "stale" — não reflete mudanças
-- de status dos payments (ex: PENDING → FAILED).
-- Resultado: "duplicate key" mesmo quando todos os payments são FAILED.
--
-- SOLUÇÃO: Adicionar coluna payment_status na payment_deliveries (mantida
-- em sincronia via trigger) e usar partial index baseado na coluna local.
-- =====================================================================

-- 1. Dropar o índice problemático
DROP INDEX IF EXISTS idx_unique_active_payment_per_delivery;??

-- 2. Dropar a função problemática  
DROP FUNCTION IF EXISTS is_payment_active(BIGINT);

-- 3. Adicionar coluna de status na tabela de junção
ALTER TABLE payment_deliveries 
ADD COLUMN IF NOT EXISTS payment_status VARCHAR(20) DEFAULT 'PENDING';

-- 4. Sincronizar coluna com dados atuais
UPDATE payment_deliveries pd
SET payment_status = p.status
FROM payments p
WHERE pd.payment_id = p.id;

-- 5. Criar índice parcial baseado na coluna local (funciona sem IMMUTABLE)
CREATE UNIQUE INDEX idx_unique_active_payment_per_delivery 
ON payment_deliveries (delivery_id)
WHERE payment_status IN ('PENDING', 'COMPLETED');

-- 6. Trigger para manter a coluna sincronizada quando o payment muda de status
CREATE OR REPLACE FUNCTION sync_payment_deliveries_status()
RETURNS TRIGGER AS $$
BEGIN
    IF OLD.status IS DISTINCT FROM NEW.status THEN
        UPDATE payment_deliveries 
        SET payment_status = NEW.status
        WHERE payment_id = NEW.id;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_sync_payment_status
AFTER UPDATE OF status ON payments
FOR EACH ROW
EXECUTE FUNCTION sync_payment_deliveries_status();

-- 7. Trigger para setar status inicial no INSERT na payment_deliveries
CREATE OR REPLACE FUNCTION set_payment_deliveries_status()
RETURNS TRIGGER AS $$
BEGIN
    NEW.payment_status := (SELECT status FROM payments WHERE id = NEW.payment_id);
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_set_payment_deliveries_status
BEFORE INSERT ON payment_deliveries
FOR EACH ROW
EXECUTE FUNCTION set_payment_deliveries_status();

-- 8. Comentários
COMMENT ON COLUMN payment_deliveries.payment_status IS 
'Status espelhado do payment, mantido em sincronia via trigger. Usado no partial index.';

COMMENT ON INDEX idx_unique_active_payment_per_delivery IS 
'Garante que cada delivery tenha no máximo UM pagamento ativo (PENDING ou COMPLETED). 
Permite múltiplos pagamentos FAILED, CANCELLED ou REFUNDED para a mesma delivery.';

