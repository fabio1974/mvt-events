-- Configuração de mesas no perfil do cliente
ALTER TABLE store_profiles ADD COLUMN total_tables INTEGER;
ALTER TABLE store_profiles ADD COLUMN default_seats INTEGER;
