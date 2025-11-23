-- Substitui organization_id por organizer_id na tabela deliveries
-- O organizer é o User que é owner da Organization comum entre courier e client

-- Primeiro, criar nova coluna organizer_id
ALTER TABLE deliveries 
ADD COLUMN organizer_id UUID;

-- Atualizar dados existentes: buscar o owner da organization e setar como organizer
UPDATE deliveries d
SET organizer_id = (
    SELECT o.owner_id
    FROM organizations o
    WHERE o.id = d.organization_id
)
WHERE d.organization_id IS NOT NULL;

-- Remover constraint antiga (se existir)
ALTER TABLE deliveries 
DROP CONSTRAINT IF EXISTS fk_deliveries_organization;

-- Remover coluna antiga organization_id
ALTER TABLE deliveries 
DROP COLUMN organization_id;

-- Adicionar nova constraint para organizer (FK para users)
ALTER TABLE deliveries
ADD CONSTRAINT fk_deliveries_organizer 
FOREIGN KEY (organizer_id) REFERENCES users(id);

-- Criar índice para melhor performance
CREATE INDEX IF NOT EXISTS idx_deliveries_organizer_id ON deliveries(organizer_id);

-- Adicionar comentário
COMMENT ON COLUMN deliveries.organizer_id IS 'User que é owner da organização comum entre courier e client';
