-- ============================================================================
-- Migration V4: Iugu Payment Integration - Bank Accounts
-- ============================================================================
-- Descrição: 
--   - Cria tabela bank_accounts para armazenar dados bancários de couriers/organizers
--   - Adiciona campos Iugu em users (iugu_account_id, dados_bancarios_completos, auto_withdraw_ativo)
--   - Permite transferências automáticas via Iugu com auto_withdraw
-- 
-- Autor: System
-- Data: 2025-12-02
-- ============================================================================

-- ============================================================================
-- TABELA: bank_accounts
-- ============================================================================

CREATE TABLE IF NOT EXISTS bank_accounts (
    -- Primary Key
    id BIGSERIAL PRIMARY KEY,
    
    -- Timestamps (herdados de BaseEntity)
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    
    -- Foreign Key para users (1:1 relationship)
    user_id UUID NOT NULL UNIQUE,
    
    -- Dados Bancários
    bank_code VARCHAR(3) NOT NULL CHECK (bank_code ~ '^\d{3}$'),
    bank_name VARCHAR(100) NOT NULL,
    agency VARCHAR(10) NOT NULL CHECK (agency ~ '^\d+$'),
    account_number VARCHAR(20) NOT NULL CHECK (account_number ~ '^\d+-\d$'),
    account_type VARCHAR(10) NOT NULL CHECK (account_type IN ('CHECKING', 'SAVINGS')),
    
    -- Status e Validação
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING_VALIDATION' 
        CHECK (status IN ('PENDING_VALIDATION', 'ACTIVE', 'BLOCKED', 'CANCELLED')),
    validated_at TIMESTAMP WITHOUT TIME ZONE,
    
    -- Metadata
    notes TEXT,
    
    -- Constraints
    CONSTRAINT fk_bank_account_user FOREIGN KEY (user_id) 
        REFERENCES users(id) ON DELETE CASCADE
);

-- ============================================================================
-- ÍNDICES: bank_accounts
-- ============================================================================

-- Índice para buscar conta por usuário (1:1 - único)
CREATE UNIQUE INDEX idx_bank_accounts_user_id ON bank_accounts(user_id);

-- Índice para filtrar por status
CREATE INDEX idx_bank_accounts_status ON bank_accounts(status) WHERE status = 'ACTIVE';

-- Índice para buscar contas pendentes de validação
CREATE INDEX idx_bank_accounts_pending ON bank_accounts(created_at) 
    WHERE status = 'PENDING_VALIDATION';

-- ============================================================================
-- COMMENTS: bank_accounts
-- ============================================================================

COMMENT ON TABLE bank_accounts IS 
    'Dados bancários dos usuários (couriers/organizers) para recebimento via Iugu';

COMMENT ON COLUMN bank_accounts.user_id IS 
    'ID do usuário dono da conta bancária (relacionamento 1:1)';

COMMENT ON COLUMN bank_accounts.bank_code IS 
    'Código de 3 dígitos do banco (ex: 260=Nubank, 341=Itaú, 001=BB)';

COMMENT ON COLUMN bank_accounts.bank_name IS 
    'Nome do banco (ex: Nubank, Itaú, Banco do Brasil)';

COMMENT ON COLUMN bank_accounts.agency IS 
    'Agência sem dígito verificador (apenas números)';

COMMENT ON COLUMN bank_accounts.account_number IS 
    'Conta com dígito verificador no formato 12345-6 (com hífen)';

COMMENT ON COLUMN bank_accounts.account_type IS 
    'Tipo de conta: CHECKING (corrente) ou SAVINGS (poupança)';

COMMENT ON COLUMN bank_accounts.status IS 
    'Status da conta: PENDING_VALIDATION, ACTIVE, BLOCKED, CANCELLED';

COMMENT ON COLUMN bank_accounts.validated_at IS 
    'Data/hora em que a conta foi validada e ativada';

COMMENT ON COLUMN bank_accounts.notes IS 
    'Observações sobre a conta (ex: motivo de bloqueio)';

-- ============================================================================
-- ALTER TABLE: users (adicionar campos Iugu)
-- ============================================================================

-- ID da subconta Iugu para transferências automáticas
ALTER TABLE users 
    ADD COLUMN IF NOT EXISTS iugu_account_id VARCHAR(100);

-- Flag indicando se dados bancários estão completos e validados
ALTER TABLE users 
    ADD COLUMN IF NOT EXISTS bank_data_complete BOOLEAN NOT NULL DEFAULT false;

-- Flag indicando se auto_withdraw está ativo no Iugu
ALTER TABLE users 
    ADD COLUMN IF NOT EXISTS auto_withdraw_enabled BOOLEAN NOT NULL DEFAULT false;

-- ============================================================================
-- ÍNDICES: users (campos Iugu)
-- ============================================================================

-- Índice para buscar usuários por iugu_account_id
CREATE INDEX IF NOT EXISTS idx_users_iugu_account_id 
    ON users(iugu_account_id) WHERE iugu_account_id IS NOT NULL;

-- Índice para buscar usuários com dados bancários completos
CREATE INDEX IF NOT EXISTS idx_users_bank_data_complete 
    ON users(bank_data_complete) WHERE bank_data_complete = true;

-- Índice para buscar usuários com auto_withdraw ativo
CREATE INDEX IF NOT EXISTS idx_users_auto_withdraw_enabled 
    ON users(auto_withdraw_enabled) WHERE auto_withdraw_enabled = true;

-- Índice composto para buscar couriers/organizers com pagamentos habilitados
CREATE INDEX IF NOT EXISTS idx_users_payment_ready 
    ON users(role, bank_data_complete, auto_withdraw_enabled) 
    WHERE (role = 'COURIER' OR role = 'ORGANIZER') 
        AND bank_data_complete = true 
        AND auto_withdraw_enabled = true;

-- ============================================================================
-- COMMENTS: users (campos Iugu)
-- ============================================================================

COMMENT ON COLUMN users.iugu_account_id IS 
    'ID da subconta Iugu criada para este usuário (usado para splits e transferências automáticas)';

COMMENT ON COLUMN users.bank_data_complete IS 
    'Indica se o usuário completou cadastro de dados bancários e foram validados pelo Iugu';

COMMENT ON COLUMN users.auto_withdraw_enabled IS 
    'Indica se transferências automáticas (D+1) estão ativas no Iugu para este usuário';

-- ============================================================================
-- FIM DA MIGRATION V4
-- ============================================================================
