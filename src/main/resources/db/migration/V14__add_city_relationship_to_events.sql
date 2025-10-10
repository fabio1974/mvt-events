-- Adiciona relacionamento com City e remove campo address redundante

-- 1. Adiciona a coluna city_id (nullable inicialmente)
ALTER TABLE events
ADD COLUMN city_id BIGINT;

-- 2. Adiciona a foreign key constraint
ALTER TABLE events
ADD CONSTRAINT fk_events_city
FOREIGN KEY (city_id) REFERENCES cities(id);

-- 3. Cria índice para melhor performance nas consultas
CREATE INDEX idx_events_city_id ON events(city_id);

-- 4. Remove a coluna address (redundante com city + location)
ALTER TABLE events
DROP COLUMN IF EXISTS address;

-- Comentários:
-- - O campo city_id é opcional (nullable) porque:
--   * Eventos existentes ainda não têm cidade associada
--   * Alguns eventos podem ser online/virtuais sem cidade específica
-- - O campo 'location' (texto livre) continua disponível para detalhes específicos do local
--   (ex: "Parque Ibirapuera - Portão 3", "Praia de Copacabana - Posto 6")
-- - O campo 'address' foi removido pois é redundante: City já tem nome, estado e país estruturados
