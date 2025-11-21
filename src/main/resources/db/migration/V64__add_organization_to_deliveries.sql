-- Adiciona coluna organization_id na tabela deliveries
-- Esta organização é definida quando o courier aceita a entrega
-- Representa a organização através da qual o courier está trabalhando
-- e que também tem contrato com o cliente

ALTER TABLE deliveries 
ADD COLUMN organization_id BIGINT;

-- Adiciona foreign key para organizations
ALTER TABLE deliveries 
ADD CONSTRAINT fk_deliveries_organization 
FOREIGN KEY (organization_id) 
REFERENCES organizations(id);

-- Criar índice para melhor performance nas queries
CREATE INDEX idx_deliveries_organization_id ON deliveries(organization_id);

-- Comentários para documentação
COMMENT ON COLUMN deliveries.organization_id IS 'Organização que conecta o courier e o client desta entrega. Setado no aceite, removido no cancelamento.';
