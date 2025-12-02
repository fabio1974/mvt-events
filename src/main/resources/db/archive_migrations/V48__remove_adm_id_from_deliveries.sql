-- Migration V48: Remove adm_id column from deliveries table
-- A coluna adm_id é redundante pois o ADM pode ser determinado através do cliente

-- Remove a coluna adm_id da tabela deliveries
ALTER TABLE deliveries DROP COLUMN IF EXISTS adm_id;

-- Comentário: 
-- A partir desta versão, para determinar o ADM de uma delivery, 
-- deve-se usar delivery.client.organization (cliente -> organização)
-- ou buscar os ADMs da organização do cliente através da relação User.organization