w-- ============================================================================
-- Migration V18: Fix bank_accounts account_number check constraint
-- ============================================================================
-- Descrição: 
--   - Remove a constraint que validava account_number com hífen
--   - account_number e account_digit são armazenados separados no banco
--   - Não há validação de formato necessária (apenas campo numérico)
-- 
-- Status: Essencial para permitir qualquer formato de dígito verificador
-- Autor: System
-- Data: 2025-12-11
-- ============================================================================

-- ============================================================================
-- 1. REMOVE CONSTRAINT ANTIGA (esperava formato com hífen)
-- ============================================================================

ALTER TABLE bank_accounts 
DROP CONSTRAINT IF EXISTS "bank_accounts_account_number_check";
