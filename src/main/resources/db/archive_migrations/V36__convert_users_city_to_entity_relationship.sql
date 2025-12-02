-- ============================================================================
-- V36: Converter campo city em users para relacionamento com tabela cities
-- ============================================================================
-- Altera o campo city de VARCHAR para foreign key, mantendo os dados existentes

-- Passo 1: Adicionar nova coluna city_id
ALTER TABLE users ADD COLUMN city_id BIGINT;

-- Passo 2: Tentar mapear cidades existentes (melhor esforço)
-- Atualiza city_id baseado no nome da cidade existente no campo city
UPDATE users 
SET city_id = (
    SELECT c.id 
    FROM cities c 
    WHERE LOWER(TRIM(c.name)) = LOWER(TRIM(users.city))
    LIMIT 1
)
WHERE users.city IS NOT NULL AND users.city != '';

-- Passo 3: Para cidades não encontradas, tenta mapear por similitude de nome (sem acentos - usando LIKE)
UPDATE users 
SET city_id = (
    SELECT c.id 
    FROM cities c 
    WHERE LOWER(TRIM(c.name)) LIKE '%' || LOWER(TRIM(users.city)) || '%'
       OR LOWER(TRIM(users.city)) LIKE '%' || LOWER(TRIM(c.name)) || '%'
    LIMIT 1
)
WHERE users.city_id IS NULL 
  AND users.city IS NOT NULL 
  AND users.city != '';

-- Passo 4: Para registros ainda sem city_id, define uma cidade padrão (São Paulo - maior cidade)
UPDATE users 
SET city_id = (
    SELECT id FROM cities WHERE name = 'São Paulo' AND state_code = 'SP' LIMIT 1
)
WHERE users.city_id IS NULL 
  AND users.city IS NOT NULL 
  AND users.city != '';

-- Passo 5: Adicionar foreign key constraint
ALTER TABLE users 
ADD CONSTRAINT fk_users_city 
FOREIGN KEY (city_id) REFERENCES cities(id);

-- Passo 6: Renomear coluna antiga para backup (não remover ainda por segurança)
ALTER TABLE users RENAME COLUMN city TO city_old;

-- Passo 7: Adicionar índice para performance
CREATE INDEX IF NOT EXISTS idx_users_city_id ON users(city_id);

-- Passo 8: Comentários para documentação
COMMENT ON COLUMN users.city_id IS 'FK para cities - cidade do usuário';
COMMENT ON COLUMN users.city_old IS 'BACKUP - campo city antigo (será removido em migração futura)';

-- Passo 9: Estatísticas da migração
DO $$
DECLARE
    total_users INTEGER;
    users_with_city INTEGER;
    users_migrated INTEGER;
    users_with_default INTEGER;
BEGIN
    SELECT COUNT(*) INTO total_users FROM users;
    SELECT COUNT(*) INTO users_with_city FROM users WHERE city_old IS NOT NULL AND city_old != '';
    SELECT COUNT(*) INTO users_migrated FROM users WHERE users.city_id IS NOT NULL;
    SELECT COUNT(*) INTO users_with_default FROM users 
    WHERE users.city_id = (SELECT id FROM cities WHERE name = 'São Paulo' AND state_code = 'SP' LIMIT 1);
    
    RAISE NOTICE 'Migração de city concluída:';
    RAISE NOTICE '- Total de usuários: %', total_users;
    RAISE NOTICE '- Usuários com cidade definida: %', users_with_city;
    RAISE NOTICE '- Usuários migrados com sucesso: %', users_migrated;
    RAISE NOTICE '- Usuários com cidade padrão (São Paulo): %', users_with_default;
END $$;