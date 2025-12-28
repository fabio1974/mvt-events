-- V30: Remove campo split_rules não utilizado
-- Autor: Sistema
-- Data: 2025-12-28
-- Descrição: Remove campo split_rules da tabela payments que não está sendo usado pelo código

-- Drop coluna split_rules
ALTER TABLE payments DROP COLUMN IF EXISTS split_rules;
