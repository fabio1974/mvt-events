-- V19: Adicionar categoria à inscrição
-- Permite que cada inscrição esteja vinculada a uma categoria específica do evento

-- Adicionar coluna category_id
ALTER TABLE registrations
ADD COLUMN category_id BIGINT;

-- Adicionar foreign key para event_categories
ALTER TABLE registrations
ADD CONSTRAINT fk_registration_category
    FOREIGN KEY (category_id)
    REFERENCES event_categories(id)
    ON DELETE SET NULL;

-- Criar índice para melhor performance em queries
CREATE INDEX idx_registration_category_id ON registrations(category_id);

-- Comentários para documentação
COMMENT ON COLUMN registrations.category_id IS 'Categoria do evento na qual o usuário se inscreveu';
