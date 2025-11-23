-- Adiciona campos de taxas adicionais na tabela site_configurations

-- Adicionar coluna de taxa de periculosidade
ALTER TABLE site_configurations 
ADD COLUMN danger_fee_percentage NUMERIC(5, 2) NOT NULL DEFAULT 0.00 
CHECK (danger_fee_percentage >= 0 AND danger_fee_percentage <= 100);

-- Adicionar coluna de taxa de renda alta
ALTER TABLE site_configurations 
ADD COLUMN high_income_fee_percentage NUMERIC(5, 2) NOT NULL DEFAULT 0.00 
CHECK (high_income_fee_percentage >= 0 AND high_income_fee_percentage <= 100);

-- Atualizar registro existente com valores padrão (0%)
UPDATE site_configurations 
SET danger_fee_percentage = 0.00, 
    high_income_fee_percentage = 0.00
WHERE danger_fee_percentage IS NULL OR high_income_fee_percentage IS NULL;

-- Adicionar comentários
COMMENT ON COLUMN site_configurations.danger_fee_percentage IS 'Taxa de periculosidade: percentual de acréscimo para áreas perigosas';
COMMENT ON COLUMN site_configurations.high_income_fee_percentage IS 'Taxa de renda alta: percentual de acréscimo para bairros de alta renda';
