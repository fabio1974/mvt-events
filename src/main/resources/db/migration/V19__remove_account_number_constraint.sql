-- ============================================================================
-- Migration V19: Remove bank_accounts account_number check constraint
-- ============================================================================
-- Descrição: 
--   - Remove a constraint bank_accounts_account_number_check que estava na V4
--   - account_number e account_digit são armazenados separados no banco
--   - Sem necessidade de validação de formato
-- 
-- Status: Essencial para permitir qualquer formato de dígito verificador
-- Autor: System
-- Data: 2025-12-11
-- ============================================================================

-- Remove a constraint antiga que esperava formato "número-dígito"
ALTER TABLE bank_accounts 
DROP CONSTRAINT IF EXISTS "bank_accounts_account_number_check";

-- Já que account_number apenas armazena números, adicionar constraint simples se necessário
ALTER TABLE bank_accounts 
ADD CONSTRAINT bank_accounts_account_number_check 
CHECK (account_number ~ '^\d+$');
