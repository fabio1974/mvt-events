-- V72: Adiciona campo para limitar histórico de deliveries exibido no FE/Mobile
-- O frontend/mobile pode usar recent=true para filtrar; este campo define o número de dias.

ALTER TABLE site_configurations
    ADD COLUMN IF NOT EXISTS delivery_history_days INTEGER NOT NULL DEFAULT 7;

COMMENT ON COLUMN site_configurations.delivery_history_days IS
    'Número de dias padrão para filtrar histórico de deliveries no FE/Mobile (ex: 7, 14, 30). '
    'Aplicado quando o parâmetro recent=true é passado na query.';
