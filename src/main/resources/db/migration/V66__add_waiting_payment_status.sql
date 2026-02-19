-- ============================================================================
-- V60: Adicionar status WAITING_PAYMENT ao enum DeliveryStatus
-- ============================================================================
-- Descrição: Adiciona novo status WAITING_PAYMENT para entregas/viagens que
--            estão aguardando pagamento PIX do customer antes de serem aceitas.
--
-- Fluxo com PIX do customer:
-- PENDING → (courier aceita) → WAITING_PAYMENT → (customer paga PIX) → ACCEPTED
--
-- Fluxo sem PIX (cartão/outro):
-- PENDING → (courier aceita) → ACCEPTED (direto)
-- ============================================================================

-- 1. Dropar constraint antiga
ALTER TABLE deliveries DROP CONSTRAINT IF EXISTS chk_delivery_status;

-- 2. Criar constraint nova com WAITING_PAYMENT
ALTER TABLE deliveries ADD CONSTRAINT chk_delivery_status 
    CHECK (status::text = ANY (ARRAY[
        'PENDING'::character varying,
        'WAITING_PAYMENT'::character varying,
        'ACCEPTED'::character varying, 
        'IN_TRANSIT'::character varying, 
        'COMPLETED'::character varying, 
        'CANCELLED'::character varying
    ]::text[]));

-- 3. Comentário explicativo
COMMENT ON COLUMN deliveries.status IS 'Status da entrega: PENDING (aguardando aceitação), WAITING_PAYMENT (aguardando pagamento PIX do customer), ACCEPTED (aceita), IN_TRANSIT (em trânsito/coletada), COMPLETED (finalizada), CANCELLED (cancelada)';
