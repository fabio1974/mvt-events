-- V26: Garantir que a coluna pagarme_recipient_id existe na tabela site_configurations
-- Esta migration usa DO $$ para verificar se a coluna já existe antes de tentar adicioná-la
-- Útil para casos onde a migration V10 pode ter falhado ou não ter sido aplicada corretamente

DO $$
BEGIN
    -- Verifica se a coluna pagarme_recipient_id existe
    IF NOT EXISTS (
        SELECT 1 
        FROM information_schema.columns 
        WHERE table_name = 'site_configurations' 
        AND column_name = 'pagarme_recipient_id'
    ) THEN
        -- Se não existe, adiciona a coluna
        ALTER TABLE site_configurations
        ADD COLUMN pagarme_recipient_id VARCHAR(100);
        
        RAISE NOTICE 'Coluna pagarme_recipient_id adicionada à tabela site_configurations';
    ELSE
        RAISE NOTICE 'Coluna pagarme_recipient_id já existe na tabela site_configurations';
    END IF;
END $$;

-- Adiciona ou atualiza o comentário da coluna
COMMENT ON COLUMN site_configurations.pagarme_recipient_id IS 'ID do recipient Pagar.me da plataforma/empresa para receber comissão nos splits';
