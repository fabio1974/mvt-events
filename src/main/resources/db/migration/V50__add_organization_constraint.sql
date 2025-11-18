-- Migration V50: Add constraint to ensure only ORGANIZER can have organization_id
-- Adicionar constraint que garante que apenas ORGANIZERs podem ter organization_id

-- Adicionar constraint apenas se ela não existir
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints 
        WHERE constraint_name = 'chk_organization_only_for_organizer' 
        AND table_name = 'users'
    ) THEN
        ALTER TABLE users 
        ADD CONSTRAINT chk_organization_only_for_organizer
        CHECK (organization_id IS NULL OR role = 'ORGANIZER');
        
        RAISE NOTICE 'Constraint chk_organization_only_for_organizer adicionada';
    ELSE
        RAISE NOTICE 'Constraint chk_organization_only_for_organizer já existe, pulando';
    END IF;
END $$;

-- Comentário:
-- Esta constraint garante que:
-- 1. ORGANIZER: pode ter organization_id (NULL ou não NULL)  
-- 2. COURIER: sempre organization_id = NULL (obtém via employment_contracts)
-- 3. CLIENT: sempre organization_id = NULL (organização vem do contexto)
-- 4. USER: sempre organization_id = NULL (usuários gerais)