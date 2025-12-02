-- ============================================================================
-- V31: Índices finais, constraints e otimizações
-- ============================================================================
-- Índices compostos para queries frequentes
-- Triggers para timestamps automáticos
-- Validações finais
-- ============================================================================

-- ============================================================================
-- ÍNDICES COMPOSTOS PARA DELIVERIES (queries de tenant + filtros)
-- ============================================================================

-- Queries por ADM (tenant) + status
CREATE INDEX IF NOT EXISTS idx_delivery_adm_status ON deliveries(adm_id, status) WHERE status != 'CANCELLED';

-- Queries por ADM + data de criação (dashboard de ADM)
CREATE INDEX IF NOT EXISTS idx_delivery_adm_created ON deliveries(adm_id, created_at DESC);

-- Queries por Courier + status (app do entregador)
CREATE INDEX IF NOT EXISTS idx_delivery_courier_status ON deliveries(courier_id, status) 
    WHERE courier_id IS NOT NULL AND status IN ('ASSIGNED', 'PICKED_UP', 'IN_TRANSIT');

-- Queries por Client + data (histórico do cliente)
CREATE INDEX IF NOT EXISTS idx_delivery_client_created ON deliveries(client_id, created_at DESC);

-- Queries por Partnership (relatórios institucionais)
CREATE INDEX IF NOT EXISTS idx_delivery_partnership_completed ON deliveries(partnership_id, completed_at DESC) 
    WHERE partnership_id IS NOT NULL AND completed_at IS NOT NULL;

-- ============================================================================
-- ÍNDICES PARA PERFORMANCE EM COURIER_ADM_LINKS
-- ============================================================================

-- Query rápida do ADM primário ativo do courier
CREATE INDEX IF NOT EXISTS idx_courier_link_primary_active ON courier_adm_links(courier_id) 
    WHERE is_primary = true AND is_active = true;

-- Queries de ADMs por região (via courier)
CREATE INDEX IF NOT EXISTS idx_courier_link_adm_active ON courier_adm_links(adm_id, is_active);

-- ============================================================================
-- ÍNDICES PARA MÉTRICAS E RELATÓRIOS
-- ============================================================================

-- Avaliações por rating (estatísticas)
CREATE INDEX IF NOT EXISTS idx_evaluation_rating ON evaluations(rating);

-- Payouts por período e status (relatórios financeiros)
CREATE INDEX IF NOT EXISTS idx_payout_period_status ON unified_payouts(period, status);

-- Pagamentos por registro e método (análise financeira)
CREATE INDEX IF NOT EXISTS idx_payment_registration_method ON payments(registration_id, payment_method) 
    WHERE registration_id IS NOT NULL;

-- ============================================================================
-- TRIGGER: Auto-update timestamps
-- ============================================================================

-- Função genérica para atualizar updated_at
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Aplicar trigger em todas as tabelas do Zapi10
DROP TRIGGER IF EXISTS update_municipal_partnerships_updated_at ON municipal_partnerships;
CREATE TRIGGER update_municipal_partnerships_updated_at BEFORE UPDATE ON municipal_partnerships
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

DROP TRIGGER IF EXISTS update_courier_profiles_updated_at ON courier_profiles;
CREATE TRIGGER update_courier_profiles_updated_at BEFORE UPDATE ON courier_profiles
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

DROP TRIGGER IF EXISTS update_adm_profiles_updated_at ON adm_profiles;
CREATE TRIGGER update_adm_profiles_updated_at BEFORE UPDATE ON adm_profiles
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

DROP TRIGGER IF EXISTS update_courier_adm_links_updated_at ON courier_adm_links;
CREATE TRIGGER update_courier_adm_links_updated_at BEFORE UPDATE ON courier_adm_links
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

DROP TRIGGER IF EXISTS update_client_manager_links_updated_at ON client_manager_links;
CREATE TRIGGER update_client_manager_links_updated_at BEFORE UPDATE ON client_manager_links
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

DROP TRIGGER IF EXISTS update_deliveries_updated_at ON deliveries;
CREATE TRIGGER update_deliveries_updated_at BEFORE UPDATE ON deliveries
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

DROP TRIGGER IF EXISTS update_evaluations_updated_at ON evaluations;
CREATE TRIGGER update_evaluations_updated_at BEFORE UPDATE ON evaluations
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

DROP TRIGGER IF EXISTS update_unified_payouts_updated_at ON unified_payouts;
CREATE TRIGGER update_unified_payouts_updated_at BEFORE UPDATE ON unified_payouts
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

DROP TRIGGER IF EXISTS update_payout_items_updated_at ON payout_items;
CREATE TRIGGER update_payout_items_updated_at BEFORE UPDATE ON payout_items
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================================================
-- VALIDAÇÕES FINAIS
-- ============================================================================

-- Comentários finais
COMMENT ON TRIGGER update_municipal_partnerships_updated_at ON municipal_partnerships IS 'Auto-update updated_at on row modification';
COMMENT ON TRIGGER update_courier_profiles_updated_at ON courier_profiles IS 'Auto-update updated_at on row modification';
COMMENT ON TRIGGER update_adm_profiles_updated_at ON adm_profiles IS 'Auto-update updated_at on row modification';
COMMENT ON TRIGGER update_courier_adm_links_updated_at ON courier_adm_links IS 'Auto-update updated_at on row modification';
COMMENT ON TRIGGER update_client_manager_links_updated_at ON client_manager_links IS 'Auto-update updated_at on row modification';
COMMENT ON TRIGGER update_deliveries_updated_at ON deliveries IS 'Auto-update updated_at on row modification';
COMMENT ON TRIGGER update_evaluations_updated_at ON evaluations IS 'Auto-update updated_at on row modification';
COMMENT ON TRIGGER update_unified_payouts_updated_at ON unified_payouts IS 'Auto-update updated_at on row modification';
COMMENT ON TRIGGER update_payout_items_updated_at ON payout_items IS 'Auto-update updated_at on row modification';

-- ============================================================================
-- ANÁLISE DE PERFORMANCE
-- ============================================================================
-- Para análise de uso dos índices:
-- SELECT schemaname, tablename, indexname, idx_scan 
-- FROM pg_stat_user_indexes 
-- WHERE schemaname = 'public' 
-- ORDER BY idx_scan ASC;
-- ============================================================================
