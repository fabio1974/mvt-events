#!/bin/bash

# Script para verificar o estado do schema EventFinancials
echo "🔍 Verificando schema EventFinancials..."

psql $DATABASE_URL << EOF

-- Listar todas as colunas da tabela event_financials
SELECT 
    column_name, 
    data_type, 
    is_nullable, 
    column_default
FROM information_schema.columns 
WHERE table_name = 'event_financials' 
ORDER BY ordinal_position;

-- Verificar constraints
SELECT 
    conname as constraint_name,
    pg_get_constraintdef(oid) as definition
FROM pg_constraint 
WHERE conrelid = 'event_financials'::regclass;

-- Contar registros na tabela
SELECT COUNT(*) as total_records FROM event_financials;

EOF

echo "✅ Verificação completa!"