-- Adiciona colunas de latitude e longitude do endereço do cliente na tabela users
ALTER TABLE users
ADD COLUMN client_lat DOUBLE PRECISION,
ADD COLUMN client_lng DOUBLE PRECISION;
