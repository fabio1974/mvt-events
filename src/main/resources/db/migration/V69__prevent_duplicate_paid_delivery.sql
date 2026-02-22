-- ============================================================================
-- V69: Impedir que uma delivery seja paga duas vezes
-- ============================================================================

-- 1. Dropar o índice antigo que só cobria PENDING e COMPLETED
DROP INDEX IF EXISTS idx_unique_active_payment_per_delivery;

-- 2. Criar novo partial unique index incluindo PAID
--    Garante que uma delivery só pode ter UM pagamento ativo (PENDING, PAID ou COMPLETED)
CREATE UNIQUE INDEX idx_unique_active_payment_per_delivery
    ON payment_deliveries (delivery_id)
    WHERE payment_status IN ('PENDING', 'PAID', 'COMPLETED');

-- 3. Atualizar o trigger V64 para também bloquear INSERT quando já existe PAID
CREATE OR REPLACE FUNCTION check_pending_payment_for_delivery()
RETURNS TRIGGER AS $$
DECLARE
    v_payment_status VARCHAR(20);
    v_existing_payment_id BIGINT;
    v_existing_status VARCHAR(20);
BEGIN
    -- Get the status of the payment being inserted
    SELECT status INTO v_payment_status
    FROM payments
    WHERE id = NEW.payment_id;

    -- Check if there's already a PENDING or PAID payment for this delivery
    SELECT p.id, p.status
    INTO v_existing_payment_id, v_existing_status
    FROM payment_deliveries pd
    INNER JOIN payments p ON p.id = pd.payment_id
    WHERE pd.delivery_id = NEW.delivery_id
      AND p.status IN ('PENDING', 'PAID')
      AND p.id != NEW.payment_id
    LIMIT 1;

    IF FOUND THEN
        RAISE EXCEPTION 'Delivery % já possui pagamento % (ID: %). Não é possível criar outro pagamento.',
            NEW.delivery_id,
            v_existing_status,
            v_existing_payment_id
        USING ERRCODE = '23505',
              HINT = 'A corrida já está paga ou com pagamento pendente.';
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;
