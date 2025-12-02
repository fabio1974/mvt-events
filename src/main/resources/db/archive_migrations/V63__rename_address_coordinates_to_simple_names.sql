-- Renomear colunas de coordenadas de endereço para nomes mais simples
-- client_lat -> latitude
-- client_lng -> longitude

ALTER TABLE users RENAME COLUMN client_lat TO latitude;
ALTER TABLE users RENAME COLUMN client_lng TO longitude;
