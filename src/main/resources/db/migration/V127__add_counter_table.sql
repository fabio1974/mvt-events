-- Mesa "Balcão": uma RestaurantTable especial por client (number=0, is_counter=true).
-- Usada para vendas de balcão, distinguindo no relatório de caixa de vendas de mesa.

ALTER TABLE restaurant_tables
    ADD COLUMN is_counter BOOLEAN NOT NULL DEFAULT false;

-- Garante no máximo 1 balcão por client
CREATE UNIQUE INDEX uniq_counter_per_client
    ON restaurant_tables (client_id)
    WHERE is_counter = true;

-- Backfill: cria balcão (number=0) para todo CLIENT que ainda não tem
INSERT INTO restaurant_tables (client_id, number, seats, active, is_counter, created_at, updated_at)
SELECT u.id, 0, 0, true, true, NOW(), NOW()
FROM users u
WHERE u.role = 'CLIENT'
  AND NOT EXISTS (
      SELECT 1 FROM restaurant_tables rt
      WHERE rt.client_id = u.id AND rt.is_counter = true
  )
ON CONFLICT (client_id, number) DO NOTHING;
