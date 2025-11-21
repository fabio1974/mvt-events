-- Renomeia as colunas latitude e longitude para gps_latitude e gps_longitude na tabela users
ALTER TABLE users
RENAME COLUMN latitude TO gps_latitude;

ALTER TABLE users
RENAME COLUMN longitude TO gps_longitude;
