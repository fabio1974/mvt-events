#!/bin/bash

# Script de correção emergencial para produção
# Execute este script no servidor de produção

echo "🚨 Aplicando correção emergencial para EventFinancials..."

# Conectar ao banco e aplicar correções
psql $DATABASE_URL << EOF

-- Renomear coluna para corresponder à entidade Java
ALTER TABLE event_financials RENAME COLUMN last_transfer_at TO last_transfer_date;

-- Adicionar colunas faltantes
ALTER TABLE event_financials ADD COLUMN IF NOT EXISTS next_transfer_date TIMESTAMP;
ALTER TABLE event_financials ADD COLUMN IF NOT EXISTS transfer_frequency VARCHAR(20) DEFAULT 'WEEKLY' NOT NULL;
ALTER TABLE event_financials ADD COLUMN IF NOT EXISTS total_payments INTEGER DEFAULT 0 NOT NULL;
ALTER TABLE event_financials ADD COLUMN IF NOT EXISTS pending_transfer_amount DECIMAL(12,2) DEFAULT 0 NOT NULL;

-- Verificar estrutura da tabela
\d event_financials

EOF

echo "✅ Correção aplicada! A aplicação deve funcionar agora."