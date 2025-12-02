-- ============================================================================
-- V42: Remover tabelas antigas de eventos
-- ============================================================================
-- Remove todas as tabelas relacionadas ao sistema antigo de eventos
-- O Zapi10 é focado em entregas, não em gerenciamento de eventos
-- ============================================================================

-- ============================================================================
-- 1. BACKUP DOS DADOS (LOGS)
-- ============================================================================

DO $$
BEGIN
    RAISE NOTICE '============================================';
    RAISE NOTICE 'REMOVENDO TABELAS DE EVENTOS';
    RAISE NOTICE '============================================';
    RAISE NOTICE 'Events: % registros', (SELECT COUNT(*) FROM events);
    RAISE NOTICE 'Registrations: % registros', (SELECT COUNT(*) FROM registrations);
    RAISE NOTICE 'Event Categories: % registros', (SELECT COUNT(*) FROM event_categories);
    RAISE NOTICE 'Event Financials: % registros', (SELECT COUNT(*) FROM event_financials);
    RAISE NOTICE 'Payment Events: % registros', (SELECT COUNT(*) FROM payment_events);
    RAISE NOTICE '============================================';
END $$;

-- ============================================================================
-- 2. REMOVER TABELAS (ordem inversa de dependências)
-- ============================================================================

-- Tabelas dependentes primeiro
DROP TABLE IF EXISTS registrations CASCADE;
DROP TABLE IF EXISTS event_financials CASCADE;
DROP TABLE IF EXISTS payment_events CASCADE;

-- Tabela principal
DROP TABLE IF EXISTS events CASCADE;

-- Tabela de categorias
DROP TABLE IF EXISTS event_categories CASCADE;

-- ============================================================================
-- 3. CONFIRMAÇÃO
-- ============================================================================

DO $$
BEGIN
    RAISE NOTICE '============================================';
    RAISE NOTICE 'TABELAS DE EVENTOS REMOVIDAS COM SUCESSO';
    RAISE NOTICE '============================================';
    RAISE NOTICE 'O sistema Zapi10 está focado em entregas';
    RAISE NOTICE 'Tabelas removidas:';
    RAISE NOTICE '  - events';
    RAISE NOTICE '  - registrations';
    RAISE NOTICE '  - event_categories';
    RAISE NOTICE '  - event_financials';
    RAISE NOTICE '  - payment_events';
    RAISE NOTICE '============================================';
END $$;
