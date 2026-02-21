-- Limpa deliveries com delivery_type = 'CONTRACT' que não é um valor válido no enum Java.
-- Converte para 'DELIVERY' para manter a integridade dos dados.
UPDATE deliveries 
SET delivery_type = 'DELIVERY' 
WHERE delivery_type = 'CONTRACT';
