-- Adiciona campo minimum_shipping_fee na tabela site_configurations
ALTER TABLE site_configurations
ADD COLUMN minimum_shipping_fee NUMERIC(10, 2) NOT NULL DEFAULT 5.00 CHECK (minimum_shipping_fee >= 0);

-- Adicionar comentário
COMMENT ON COLUMN site_configurations.minimum_shipping_fee IS 'Valor mínimo do frete em Reais (ex: 5.00 = R$ 5,00 mínimo)';

-- Atualizar registros existentes com valor padrão
UPDATE site_configurations SET minimum_shipping_fee = 5.00 WHERE minimum_shipping_fee IS NULL;
