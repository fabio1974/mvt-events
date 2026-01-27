-- V39: Adicionar campos de configuração de transferência na tabela bank_accounts
-- transferInterval: intervalo de saque (Daily, Weekly, Monthly)
-- transferDay: dia do saque (0 para Daily, 0-6 para Weekly, 1-31 para Monthly)

ALTER TABLE bank_accounts
ADD COLUMN IF NOT EXISTS transfer_interval VARCHAR(10) DEFAULT 'Daily';

ALTER TABLE bank_accounts
ADD COLUMN IF NOT EXISTS transfer_day INTEGER DEFAULT 0;

-- Comentários para documentação
COMMENT ON COLUMN bank_accounts.transfer_interval IS 'Intervalo de transferência automática: Daily, Weekly ou Monthly';
COMMENT ON COLUMN bank_accounts.transfer_day IS 'Dia da transferência: 0 para Daily, 0-6 para Weekly (0=Dom), 1-31 para Monthly';
