-- Campos para guardar a previsão (Google Directions) antes do recálculo pela rota real
ALTER TABLE deliveries ADD COLUMN estimated_distance_km NUMERIC(6,2);
ALTER TABLE deliveries ADD COLUMN estimated_shipping_fee NUMERIC(10,2);

-- Preencher entregas existentes com os valores atuais (já são estimativas do Google)
UPDATE deliveries SET estimated_distance_km = distance_km WHERE distance_km IS NOT NULL;
UPDATE deliveries SET estimated_shipping_fee = shipping_fee WHERE shipping_fee IS NOT NULL;
