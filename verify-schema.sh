#!/bin/bash

# Script para verificar o estado do schema EventFinancials e Events
echo "🔍 Verificando schema EventFinancials e Events..."

psql $DATABASE_URL << EOF

-- ============================================================================
-- VERIFICAÇÃO DA TABELA EVENT_FINANCIALS
-- ============================================================================

echo "📊 Estrutura da tabela EVENT_FINANCIALS:"
SELECT 
    column_name, 
    data_type, 
    is_nullable, 
    column_default
FROM information_schema.columns 
WHERE table_name = 'event_financials' 
ORDER BY ordinal_position;

-- ============================================================================
-- VERIFICAÇÃO DA TABELA EVENTS
-- ============================================================================

echo "📊 Estrutura da tabela EVENTS:"
SELECT 
    column_name, 
    data_type, 
    is_nullable, 
    column_default
FROM information_schema.columns 
WHERE table_name = 'events' 
ORDER BY ordinal_position;

-- ============================================================================
-- VERIFICAÇÃO DE CONSTRAINTS
-- ============================================================================

echo "🔒 Constraints das tabelas:"
SELECT 
    tc.table_name, 
    tc.constraint_name, 
    tc.constraint_type,
    pg_get_constraintdef(pgc.oid) as definition
FROM information_schema.table_constraints tc
JOIN pg_constraint pgc ON tc.constraint_name = pgc.conname
WHERE tc.table_name IN ('event_financials', 'events')
AND tc.constraint_type IN ('CHECK', 'FOREIGN KEY')
ORDER BY tc.table_name, tc.constraint_type, tc.constraint_name;

-- ============================================================================
-- CONTAGEM DE REGISTROS
-- ============================================================================

echo "📈 Contagem de registros:"
SELECT 'event_financials' as tabela, COUNT(*) as total_records FROM event_financials
UNION ALL
SELECT 'events' as tabela, COUNT(*) as total_records FROM events
ORDER BY tabela;

-- ============================================================================
-- VERIFICAÇÃO DE COLUNAS CRÍTICAS
-- ============================================================================

echo "🎯 Verificação de colunas críticas:"

-- Verificar se as colunas críticas existem
SELECT 
    'event_financials' as tabela,
    'net_revenue' as coluna_critica,
    CASE WHEN EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'event_financials' AND column_name = 'net_revenue'
    ) THEN '✅ EXISTS' ELSE '❌ MISSING' END as status
UNION ALL
SELECT 
    'events' as tabela,
    'platform_fee_percentage' as coluna_critica,
    CASE WHEN EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'events' AND column_name = 'platform_fee_percentage'
    ) THEN '✅ EXISTS' ELSE '❌ MISSING' END as status;

EOF

echo "✅ Verificação completa!"