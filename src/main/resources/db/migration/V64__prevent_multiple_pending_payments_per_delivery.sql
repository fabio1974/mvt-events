-- Migration: Prevent multiple PENDING payments for the same delivery
-- Purpose: Ensure only one PENDING payment can exist per delivery at any time
-- This prevents duplicate payment attempts being sent to Pagar.me
-- Date: 2026-02-18

-- Create function to check for existing pending payments
CREATE OR REPLACE FUNCTION check_pending_payment_for_delivery()
RETURNS TRIGGER AS $$
DECLARE
    v_payment_status VARCHAR(20);
    v_existing_payment_id BIGINT;
    v_existing_provider_id VARCHAR(255);
BEGIN
    -- Get the status of the payment being inserted
    SELECT status INTO v_payment_status
    FROM payments
    WHERE id = NEW.payment_id;
    
    -- If the new payment is PENDING, check if there's already a PENDING payment for this delivery
    IF v_payment_status = 'PENDING' THEN
        SELECT p.id, p.provider_payment_id
        INTO v_existing_payment_id, v_existing_provider_id
        FROM payment_deliveries pd
        INNER JOIN payments p ON p.id = pd.payment_id
        WHERE pd.delivery_id = NEW.delivery_id
          AND p.status = 'PENDING'
          AND p.id != NEW.payment_id  -- Exclude the current payment
        LIMIT 1;
        
        -- If found, raise an exception to block the insert
        IF FOUND THEN
            RAISE EXCEPTION 'Já existe um pagamento PENDENTE (ID: %, Provider ID: %) para esta entrega (Delivery ID: %). Aguarde a conclusão ou cancele o pagamento anterior.',
                v_existing_payment_id,
                COALESCE(v_existing_provider_id, 'N/A'),
                NEW.delivery_id
            USING ERRCODE = '23505',  -- unique_violation error code
                  HINT = 'Cancele ou aguarde a conclusão do pagamento existente antes de criar um novo.';
        END IF;
    END IF;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Create trigger to enforce the constraint
CREATE TRIGGER trg_prevent_duplicate_pending_payments
    BEFORE INSERT ON payment_deliveries
    FOR EACH ROW
    EXECUTE FUNCTION check_pending_payment_for_delivery();

-- Add comment for documentation
COMMENT ON FUNCTION check_pending_payment_for_delivery() IS 
'Prevents multiple PENDING payments for the same delivery. Raises exception (23505) if a PENDING payment already exists.';

COMMENT ON TRIGGER trg_prevent_duplicate_pending_payments ON payment_deliveries IS 
'Enforces business rule: only one PENDING payment per delivery at any time. Prevents duplicate payment attempts to Pagar.me.';
