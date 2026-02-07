-- ============================================================================
-- V47: Remover status PICKED_UP e unificar com IN_TRANSIT
-- ============================================================================
-- Descrição: Remove o status PICKED_UP do enum DeliveryStatus.
--            Todas as entregas que estavam PICKED_UP passam para IN_TRANSIT.
--            Simplifica o fluxo: PENDING → ACCEPTED → IN_TRANSIT → COMPLETED
--
-- Motivo:
-- - PICKED_UP e IN_TRANSIT são redundantes
-- - Quando motoboy coleta, já está em trânsito automaticamente
-- - Para viagem de passageiro: botão "coletar" = início da viagem
-- ============================================================================

-- 1. Atualizar todas as entregas que estavam PICKED_UP para IN_TRANSIT
UPDATE deliveries
SET status = 'IN_TRANSIT'
WHERE status = 'PICKED_UP';

-- 2. Atualizar in_transit_at para entregas que não tinham (tinham apenas picked_up_at)
UPDATE deliveries
SET in_transit_at = picked_up_at
WHERE status = 'IN_TRANSIT'
  AND in_transit_at IS NULL
  AND picked_up_at IS NOT NULL;

-- 3. Dropar constraint antiga que inclui PICKED_UP
ALTER TABLE deliveries DROP CONSTRAINT IF EXISTS chk_delivery_status;

-- 4. Criar constraint nova sem PICKED_UP
ALTER TABLE deliveries ADD CONSTRAINT chk_delivery_status 
    CHECK (status::text = ANY (ARRAY[
        'PENDING'::character varying, 
        'ACCEPTED'::character varying, 
        'IN_TRANSIT'::character varying, 
        'COMPLETED'::character varying, 
        'CANCELLED'::character varying
    ]::text[]));

-- 5. Comentário explicativo
COMMENT ON COLUMN deliveries.status IS 'Status da entrega: PENDING (aguardando), ACCEPTED (aceita), IN_TRANSIT (em trânsito/coletada), COMPLETED (finalizada), CANCELLED (cancelada)';
