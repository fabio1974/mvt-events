-- V60: Adicionar preço por km para automóvel e taxa de cartão de crédito
-- O price_per_km existente passa a ser exclusivamente para MOTORCYCLE (moto)
-- car_price_per_km é o novo campo para veículos do tipo CAR (automóvel)
-- credit_card_fee_percentage é a taxa cobrada quando o pagamento é via cartão de crédito

ALTER TABLE site_configurations
    ADD COLUMN car_price_per_km NUMERIC(10, 2) NOT NULL DEFAULT 2.00,
    ADD COLUMN credit_card_fee_percentage NUMERIC(5, 2) NOT NULL DEFAULT 0.00;

COMMENT ON COLUMN site_configurations.price_per_km IS 'Preço por km para MOTO (motorcycle)';
COMMENT ON COLUMN site_configurations.car_price_per_km IS 'Preço por km para AUTOMÓVEL (car)';
COMMENT ON COLUMN site_configurations.credit_card_fee_percentage IS 'Taxa percentual cobrada quando pagamento é via cartão de crédito (0-100)';
