-- V62: Adicionar taxa mínima de frete separada para automóvel
-- O minimum_shipping_fee existente passa a ser exclusivamente para MOTO
-- car_minimum_shipping_fee é o novo campo para automóvel

ALTER TABLE site_configurations
    ADD COLUMN car_minimum_shipping_fee NUMERIC(10, 2) NOT NULL DEFAULT 8.00;

COMMENT ON COLUMN site_configurations.minimum_shipping_fee IS 'Valor mínimo do frete para MOTO';
COMMENT ON COLUMN site_configurations.car_minimum_shipping_fee IS 'Valor mínimo do frete para AUTOMÓVEL';
