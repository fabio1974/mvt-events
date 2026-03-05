-- V71: Adiciona campo para limitar histórico de pagamentos exibido no FE/Mobile
-- O frontend/mobile pode usar recentDays para filtrar; este campo define o valor padrão.

ALTER TABLE site_configurations
    ADD COLUMN IF NOT EXISTS payment_history_days INTEGER NOT NULL DEFAULT 7;

COMMENT ON COLUMN site_configurations.payment_history_days IS
    'Número de dias padrão para filtrar histórico de pagamentos no FE/Mobile (ex: 7, 14, 30). '
    'O FE pode sobrescrever via parâmetro recentDays na query.';
