-- ============================================================================
-- V34: Adicionar campo scheduled_pickup_at na tabela deliveries
-- ============================================================================
-- Campo para armazenar quando a entrega está agendada para ser coletada
-- Usado para ordenação e filtros nas consultas do DeliveryRepository
-- ============================================================================

-- ============================================================================
-- TABELA: DELIVERIES - Adicionar scheduled_pickup_at
-- ============================================================================

-- Adicionar coluna scheduled_pickup_at (nullable para deliveries existentes)
ALTER TABLE deliveries ADD COLUMN scheduled_pickup_at TIMESTAMP(6);

-- Criar índice para performance nas consultas ordenadas por scheduled_pickup_at
CREATE INDEX idx_delivery_scheduled_pickup ON deliveries(scheduled_pickup_at) 
    WHERE scheduled_pickup_at IS NOT NULL;

-- Criar índice composto para consultas por courier + data agendada
CREATE INDEX idx_delivery_courier_scheduled ON deliveries(courier_id, scheduled_pickup_at) 
    WHERE courier_id IS NOT NULL AND scheduled_pickup_at IS NOT NULL;

-- ============================================================================
-- COMENTÁRIOS
-- ============================================================================

COMMENT ON COLUMN deliveries.scheduled_pickup_at IS 'Data e hora agendada para coleta da entrega';
COMMENT ON INDEX idx_delivery_scheduled_pickup IS 'Índice para consultas ordenadas por data agendada';
COMMENT ON INDEX idx_delivery_courier_scheduled IS 'Índice para consultas do courier por data agendada';