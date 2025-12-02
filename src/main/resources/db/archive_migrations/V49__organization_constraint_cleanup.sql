-- Migration V49: Clean organization data and update constraint
-- Limpar dados e ajustar constraint existente

-- Dropar constraint existente que pode estar causando problemas
ALTER TABLE users DROP CONSTRAINT IF EXISTS chk_only_organizer_has_organization;

-- Limpar organization_id de todos exceto ORGANIZER
UPDATE users 
SET organization_id = NULL 
WHERE role != 'ORGANIZER' AND organization_id IS NOT NULL;

-- Aplicar constraint nova e flexível
ALTER TABLE users 
ADD CONSTRAINT chk_organization_only_for_organizer
CHECK (organization_id IS NULL OR role = 'ORGANIZER');

-- Comentário:
-- Permite apenas ORGANIZERs terem organization_id
-- Outros roles sempre NULL