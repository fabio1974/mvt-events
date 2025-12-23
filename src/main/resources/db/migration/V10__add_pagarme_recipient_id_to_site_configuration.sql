-- V10: Adiciona coluna pagarme_recipient_id na tabela site_configurations
-- Usado para armazenar o ID do recipient Pagar.me da plataforma/empresa
-- Esse ID será usado no split de pagamentos para receber a comissão da plataforma

ALTER TABLE site_configurations
ADD COLUMN pagarme_recipient_id VARCHAR(100);

COMMENT ON COLUMN site_configurations.pagarme_recipient_id IS 'ID do recipient Pagar.me da plataforma/empresa para receber comissão nos splits';
