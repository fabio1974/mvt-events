-- V75: Adicionar campo radius_meters à tabela special_zones
-- Permite que cada zona tenha seu próprio raio de cobertura em metros

-- Adicionar coluna radius_meters com valor padrão de 300 metros
ALTER TABLE special_zones
ADD COLUMN radius_meters DOUBLE PRECISION NOT NULL DEFAULT 300.0;

-- Adicionar constraint para garantir que o raio seja positivo
ALTER TABLE special_zones
ADD CONSTRAINT check_positive_radius CHECK (radius_meters > 0);

-- Comentário da coluna
COMMENT ON COLUMN special_zones.radius_meters IS 'Raio de cobertura da zona em metros. Define a distância máxima do ponto central para aplicar a taxa especial.';
