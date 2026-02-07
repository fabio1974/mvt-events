-- V52: Adiciona campo is_active_vehicle e atualiza campo color para enum
-- Data: 2026-02-06
-- Descrição: Permite que o motorista defina qual veículo está usando ativamente
--            e padroniza cores com enum

-- Adiciona coluna is_active_vehicle
ALTER TABLE vehicles 
ADD COLUMN is_active_vehicle BOOLEAN DEFAULT FALSE NOT NULL;

-- Adiciona comentário na coluna
COMMENT ON COLUMN vehicles.is_active_vehicle IS 'Indica se este é o veículo em uso ativo pelo motorista';

-- Cria constraint UNIQUE para garantir apenas 1 veículo ativo por usuário
-- Usando partial unique index (WHERE clause) pois queremos uniqueness apenas quando is_active_vehicle = true
CREATE UNIQUE INDEX idx_vehicles_unique_active_per_owner 
ON vehicles(owner_id) 
WHERE is_active = true AND is_active_vehicle = true;

-- Cria índice para melhorar performance de busca
CREATE INDEX idx_vehicles_owner_active_vehicle 
ON vehicles(owner_id, is_active_vehicle) 
WHERE is_active = true AND is_active_vehicle = true;

-- Atualiza coluna color para enum
-- Primeiro, mapeia cores existentes para o enum (se houver dados)
UPDATE vehicles SET color = 'BRANCO' WHERE UPPER(color) IN ('BRANCO', 'WHITE', 'BRANCA');
UPDATE vehicles SET color = 'PRETO' WHERE UPPER(color) IN ('PRETO', 'BLACK', 'PRETA');
UPDATE vehicles SET color = 'PRATA' WHERE UPPER(color) IN ('PRATA', 'SILVER');
UPDATE vehicles SET color = 'CINZA' WHERE UPPER(color) IN ('CINZA', 'GRAY', 'GREY', 'CINZENTO');
UPDATE vehicles SET color = 'VERMELHO' WHERE UPPER(color) IN ('VERMELHO', 'RED', 'VERMELHA');
UPDATE vehicles SET color = 'AZUL' WHERE UPPER(color) IN ('AZUL', 'BLUE');
UPDATE vehicles SET color = 'VERDE' WHERE UPPER(color) IN ('VERDE', 'GREEN');
UPDATE vehicles SET color = 'AMARELO' WHERE UPPER(color) IN ('AMARELO', 'YELLOW', 'AMARELA');
UPDATE vehicles SET color = 'LARANJA' WHERE UPPER(color) IN ('LARANJA', 'ORANGE');
UPDATE vehicles SET color = 'MARROM' WHERE UPPER(color) IN ('MARROM', 'BROWN');
UPDATE vehicles SET color = 'BEGE' WHERE UPPER(color) IN ('BEGE', 'BEIGE');
UPDATE vehicles SET color = 'DOURADO' WHERE UPPER(color) IN ('DOURADO', 'GOLD', 'DOURADA');
UPDATE vehicles SET color = 'ROSA' WHERE UPPER(color) IN ('ROSA', 'PINK');
UPDATE vehicles SET color = 'ROXO' WHERE UPPER(color) IN ('ROXO', 'PURPLE', 'ROXA');
UPDATE vehicles SET color = 'VINHO' WHERE UPPER(color) IN ('VINHO', 'WINE', 'BURGUNDY');
-- Mapeia cores não reconhecidas para OUTROS
UPDATE vehicles SET color = 'OUTROS' 
WHERE color NOT IN ('BRANCO', 'PRETO', 'PRATA', 'CINZA', 'VERMELHO', 'AZUL', 'VERDE', 
                     'AMARELO', 'LARANJA', 'MARROM', 'BEGE', 'DOURADO', 'ROSA', 'ROXO', 'VINHO', 'FANTASIA');

-- Altera tipo da coluna e adiciona constraint
ALTER TABLE vehicles 
ALTER COLUMN color TYPE VARCHAR(20),
ADD CONSTRAINT vehicles_color_check 
CHECK (color IN ('BRANCO', 'PRETO', 'PRATA', 'CINZA', 'VERMELHO', 'AZUL', 'VERDE', 
                 'AMARELO', 'LARANJA', 'MARROM', 'BEGE', 'DOURADO', 'ROSA', 'ROXO', 'VINHO', 'FANTASIA', 'OUTROS'));

