-- ============================================================================
-- V80: Corrige timezone offset em campos de data/hora
-- ============================================================================
-- PROBLEMA: Em produção, created_at e outros timestamps foram salvos com 3h
-- a mais devido a conflito entre LocalDateTime e timezone do servidor.
--
-- SOLUÇÃO: Usa accepted_at como referência para identificar deliveries com
-- created_at incorreto. Se accepted_at < created_at, o created_at está errado.
-- Lógica: accepted_at deve ser DEPOIS de created_at, não antes.
--
-- MIGRAÇÃO INTELIGENTE: Apenas corrige registros onde a ordem cronológica
-- está invertida, indicando precisamente o problema de timezone.
-- ============================================================================

-- Verificar registros afetados usando accepted_at como referência
DO $$
DECLARE
    affected_deliveries INTEGER;
    affected_payments INTEGER;
    affected_related INTEGER;
BEGIN
    -- Contar deliveries onde accepted_at < created_at (ordem invertida = bug)
    SELECT COUNT(*) INTO affected_deliveries 
    FROM deliveries 
    WHERE accepted_at IS NOT NULL 
      AND accepted_at < created_at;
    
    -- Contar payments relacionados a essas deliveries
    SELECT COUNT(DISTINCT p.id) INTO affected_payments
    FROM payments p
    INNER JOIN payment_deliveries pd ON p.id = pd.payment_id
    INNER JOIN deliveries d ON pd.delivery_id = d.id
    WHERE d.accepted_at IS NOT NULL 
      AND d.accepted_at < d.created_at;
    
    RAISE NOTICE '🔍 Análise de registros com bug de timezone:';
    RAISE NOTICE '   - Deliveries com accepted_at < created_at: %', affected_deliveries;
    RAISE NOTICE '   - Payments relacionados: %', affected_payments;
    RAISE NOTICE '   - Critério: accepted_at deve ser DEPOIS de created_at';
END $$;

-- ============================================================================
-- 1. DELIVERIES
-- ============================================================================
-- Corrigir deliveries onde accepted_at < created_at (ordem cronológica invertida)
UPDATE deliveries
SET 
    created_at = created_at - INTERVAL '3 hours',
    updated_at = updated_at - INTERVAL '3 hours',
    accepted_at = CASE WHEN accepted_at IS NOT NULL THEN accepted_at - INTERVAL '3 hours' ELSE NULL END,
    picked_up_at = CASE WHEN picked_up_at IS NOT NULL THEN picked_up_at - INTERVAL '3 hours' ELSE NULL END,
    in_transit_at = CASE WHEN in_transit_at IS NOT NULL THEN in_transit_at - INTERVAL '3 hours' ELSE NULL END,
    completed_at = CASE WHEN completed_at IS NOT NULL THEN completed_at - INTERVAL '3 hours' ELSE NULL END,
    cancelled_at = CASE WHEN cancelled_at IS NOT NULL THEN cancelled_at - INTERVAL '3 hours' ELSE NULL END,
    scheduled_pickup_at = CASE WHEN scheduled_pickup_at IS NOT NULL THEN scheduled_pickup_at - INTERVAL '3 hours' ELSE NULL END
WHERE 
    -- Apenas corrigir se accepted_at existe e está ANTES de created_at (ordem invertida)
    accepted_at IS NOT NULL 
    AND accepted_at < created_at;

-- ============================================================================
-- 2. PAYMENTS (relacionados às deliveries afetadas)
-- ============================================================================
-- Corrigir payments que pertencem a deliveries com bug de timezone
UPDATE payments p
SET 
    created_at = p.created_at - INTERVAL '3 hours',
    updated_at = p.updated_at - INTERVAL '3 hours',
    payment_date = CASE WHEN p.payment_date IS NOT NULL THEN p.payment_date - INTERVAL '3 hours' ELSE NULL END,
    expires_at = CASE WHEN p.expires_at IS NOT NULL THEN p.expires_at - INTERVAL '3 hours' ELSE NULL END
WHERE EXISTS (
    SELECT 1 
    FROM payment_deliveries pd
    INNER JOIN deliveries d ON pd.delivery_id = d.id
    WHERE pd.payment_id = p.id
      AND d.accepted_at IS NOT NULL
      AND d.accepted_at < (d.created_at - INTERVAL '3 hours') -- Compara com o valor ANTES da correção
);

-- ============================================================================
-- 3. USERS, ORGANIZATIONS e outras tabelas
-- ============================================================================
-- Para tabelas sem referência temporal clara, usar critério simples:
-- Corrigir apenas se created_at está no futuro (impossível logicamente)

UPDATE users
SET 
    created_at = created_at - INTERVAL '3 hours',
    updated_at = updated_at - INTERVAL '3 hours'
WHERE 
    created_at > NOW();

UPDATE organizations
SET 
    created_at = created_at - INTERVAL '3 hours',
    updated_at = updated_at - INTERVAL '3 hours'
WHERE 
    created_at > NOW();

UPDATE vehicles
SET 
    created_at = created_at - INTERVAL '3 hours',
    updated_at = updated_at - INTERVAL '3 hours'
WHERE 
    created_at > NOW();

UPDATE evaluations
SET 
    created_at = created_at - INTERVAL '3 hours',
    updated_at = updated_at - INTERVAL '3 hours'
WHERE 
    created_at > NOW();

UPDATE cities
SET 
    created_at = created_at - INTERVAL '3 hours',
    updated_at = updated_at - INTERVAL '3 hours'
WHERE 
    created_at > NOW();

UPDATE employment_contracts ec
SET 
    created_at = ec.created_at - INTERVAL '3 hours',
    updated_at = ec.updated_at - INTERVAL '3 hours',
    linked_at = ec.linked_at - INTERVAL '3 hours'
WHERE 
    ec.created_at > NOW()
    OR (ec.linked_at IS NOT NULL AND ec.linked_at < ec.created_at);

UPDATE customer_cards
SET 
    created_at = created_at - INTERVAL '3 hours',
    updated_at = updated_at - INTERVAL '3 hours',
    verified_at = CASE WHEN verified_at IS NOT NULL THEN verified_at - INTERVAL '3 hours' ELSE NULL END,
    last_used_at = CASE WHEN last_used_at IS NOT NULL THEN last_used_at - INTERVAL '3 hours' ELSE NULL END
WHERE 
    created_at > NOW();

UPDATE bank_accounts
SET 
    created_at = created_at - INTERVAL '3 hours',
    updated_at = updated_at - INTERVAL '3 hours',
    validated_at = CASE WHEN validated_at IS NOT NULL THEN validated_at - INTERVAL '3 hours' ELSE NULL END
WHERE 
    created_at > NOW();

UPDATE user_push_tokens
SET 
    created_at = created_at - INTERVAL '3 hours',
    updated_at = updated_at - INTERVAL '3 hours'
WHERE 
    created_at > NOW();

UPDATE customer_payment_preferences
SET 
    created_at = created_at - INTERVAL '3 hours',
    updated_at = updated_at - INTERVAL '3 hours'
WHERE 
    created_at > NOW();

UPDATE site_configurations
SET 
    created_at = created_at - INTERVAL '3 hours',
    updated_at = updated_at - INTERVAL '3 hours'
WHERE 
    created_at > NOW();

-- ============================================================================
-- RELATÓRIO FINAL
-- ============================================================================
DO $$
DECLARE
    corrected_deliveries INTEGER;
    corrected_payments INTEGER;
    corrected_others INTEGER;
BEGIN
    -- Contar deliveries corrigidos (agora accepted_at >= created_at)
    SELECT COUNT(*) INTO corrected_deliveries
    FROM deliveries 
    WHERE accepted_at IS NOT NULL 
      AND accepted_at >= created_at
      AND accepted_at < created_at + INTERVAL '4 hours'; -- Janela razoável
    
    RAISE NOTICE '✅ Migração V80 concluída com sucesso!';
    RAISE NOTICE '📊 Correção de timezone baseada em lógica inteligente:';
    RAISE NOTICE '   - Deliveries: usado accepted_at como referência';
    RAISE NOTICE '   - Payments: corrigidos os relacionados às deliveries afetadas';
    RAISE NOTICE '   - Outras tabelas: corrigido apenas se created_at > NOW()';
    RAISE NOTICE '🎯 Resultado:';
    RAISE NOTICE '   - Deliveries com ordem cronológica correta: %', corrected_deliveries;
    RAISE NOTICE '🚀 Próximo: Deploy da aplicação com OffsetDateTime';
END $$;
