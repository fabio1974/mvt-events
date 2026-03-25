-- completion_order: registra a ordem de visita de cada parada.
-- Para paradas COMPLETED: valor incremental (1, 2, 3...) atribuído no momento da conclusão. Imutável.
-- Para paradas SKIPPED: 0. Imutável.
-- Para paradas PENDING: sequência planejada pelo algoritmo nearest-neighbor; atualizada a cada recálculo de rota.
ALTER TABLE delivery_stops ADD COLUMN completion_order INTEGER;
