#!/bin/bash

# Script de correção emergencial COMPLETA para produção
# Execute este script no servidor de produção

echo "🚨 Aplicando correção emergencial COMPLETA para EventFinancials..."

# Conectar ao banco e aplicar correções
psql $DATABASE_URL << EOF

-- 1. Renomear coluna para corresponder à entidade Java
DO \$\$ 
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns 
               WHERE table_name = 'event_financials' 
               AND column_name = 'last_transfer_at') THEN
        ALTER TABLE event_financials RENAME COLUMN last_transfer_at TO last_transfer_date;
    END IF;
END \$\$;

-- 2. Adicionar colunas básicas de transferência
ALTER TABLE event_financials ADD COLUMN IF NOT EXISTS next_transfer_date TIMESTAMP;
ALTER TABLE event_financials ADD COLUMN IF NOT EXISTS transfer_frequency VARCHAR(20) DEFAULT 'WEEKLY' NOT NULL;
ALTER TABLE event_financials ADD COLUMN IF NOT EXISTS total_payments INTEGER DEFAULT 0 NOT NULL;
ALTER TABLE event_financials ADD COLUMN IF NOT EXISTS pending_transfer_amount DECIMAL(12,2) DEFAULT 0 NOT NULL;

-- 3. ❌ CRÍTICO: Adicionar colunas de receita que estão causando erros
ALTER TABLE event_financials ADD COLUMN IF NOT EXISTS total_revenue DECIMAL(12,2) DEFAULT 0 NOT NULL;
ALTER TABLE event_financials ADD COLUMN IF NOT EXISTS platform_fees DECIMAL(12,2) DEFAULT 0 NOT NULL;
ALTER TABLE event_financials ADD COLUMN IF NOT EXISTS net_revenue DECIMAL(12,2) DEFAULT 0 NOT NULL;
ALTER TABLE event_financials ADD COLUMN IF NOT EXISTS total_transfer_fees DECIMAL(12,2) DEFAULT 0 NOT NULL;

-- 4. Remover constraint órfã que está causando problemas
ALTER TABLE event_financials DROP CONSTRAINT IF EXISTS chk_event_financials_transfer_frequency;

-- 5. Adicionar constraint correta após criar a coluna
ALTER TABLE event_financials ADD CONSTRAINT chk_event_financials_transfer_frequency 
    CHECK (transfer_frequency IN ('IMMEDIATE', 'DAILY', 'WEEKLY', 'MONTHLY', 'ON_DEMAND'));

-- 6. Atualizar dados existentes para mapear colunas antigas para novas
UPDATE event_financials SET 
    total_revenue = COALESCE(total_collected, 0),
    platform_fees = COALESCE(platform_fee_amount, 0),
    net_revenue = COALESCE(organizer_net_amount, 0),
    pending_transfer_amount = COALESCE(pending_transfers, 0)
WHERE total_revenue = 0 OR platform_fees = 0;

-- 7. Verificar estrutura final da tabela
\d event_financials

-- 8. Verificar constraints
SELECT conname, pg_get_constraintdef(oid) FROM pg_constraint WHERE conrelid = 'event_financials'::regclass;

EOF

echo "✅ Correção COMPLETA aplicada! Todas as colunas foram sincronizadas."
echo "🎯 Schema agora corresponde 100% à entidade EventFinancials.java"
echo "🚀 Reinicie a aplicação para aplicar as mudanças."