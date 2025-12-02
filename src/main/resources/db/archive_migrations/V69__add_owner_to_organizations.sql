-- Adiciona coluna owner_id à tabela organizations
-- Owner é o User (ORGANIZER) responsável pela organização

ALTER TABLE organizations
ADD COLUMN owner_id UUID;

-- Adicionar foreign key
ALTER TABLE organizations
ADD CONSTRAINT fk_organizations_owner
FOREIGN KEY (owner_id) REFERENCES users(id);

-- Criar índice
CREATE INDEX idx_organizations_owner_id ON organizations(owner_id);

-- Comentário
COMMENT ON COLUMN organizations.owner_id IS 'User (ORGANIZER) responsável pela organização';

-- Popular owner_id com o primeiro ORGANIZER da organização (se existir)
UPDATE organizations o
SET owner_id = (
    SELECT u.id
    FROM users u
    WHERE u.organization_id = o.id
    AND u.role = 'ORGANIZER'
    LIMIT 1
);
