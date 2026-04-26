-- Converte colunas timestamp da tabela deliveries para timestamp with time zone.
--
-- Motivação: o BE escreve OffsetDateTime (com TZ) com config hibernate.jdbc.time_zone=UTC.
-- Mas as colunas eram timestamp without time zone, que perde o offset. Resultado: o JSON
-- vai pro FE sem TZ marker, e o JS faz `new Date(iso)` interpretando como hora local
-- do dispositivo. Em produção, alguns campos viraram Fortaleza local, outros UTC, e a
-- timeline do app exibia horários inconsistentes (ex: "Coletado: 26/04 01:25" quando o
-- evento real foi às 22:25 do dia 25 em Fortaleza).
--
-- A conversão assume que valores existentes estão em UTC (corresponde à config do
-- hibernate.jdbc.time_zone). Os campos populados via Hibernate (picked_up_at,
-- in_transit_at, completed_at, cancelled_at) ficam corretos automaticamente.
--
-- O campo accepted_at, porém, era populado pela native query
-- `UPDATE deliveries SET accepted_at = NOW()` em initializeApproachRoute, e o NOW()
-- usava a session TZ do Postgres herdada da JVM (Fortaleza). Por isso accepted_at
-- históricos estão em Fortaleza local em vez de UTC. A correção do código foi feita
-- (passar OffsetDateTime explícito), mas os dados antigos precisam ser corrigidos
-- aqui — re-interpretando como Fortaleza antes de converter pra UTC.

-- Postgres bloqueia ALTER COLUMN em colunas referenciadas por views.
-- A view available_on_demand_deliveries seleciona created_at — drop e recria após o ALTER.
DROP VIEW IF EXISTS available_on_demand_deliveries;

ALTER TABLE deliveries
    ALTER COLUMN created_at          TYPE timestamp with time zone USING created_at          AT TIME ZONE 'UTC',
    ALTER COLUMN updated_at          TYPE timestamp with time zone USING updated_at          AT TIME ZONE 'UTC',
    ALTER COLUMN accepted_at         TYPE timestamp with time zone USING accepted_at         AT TIME ZONE 'America/Fortaleza',
    ALTER COLUMN picked_up_at        TYPE timestamp with time zone USING picked_up_at        AT TIME ZONE 'UTC',
    ALTER COLUMN in_transit_at       TYPE timestamp with time zone USING in_transit_at       AT TIME ZONE 'UTC',
    ALTER COLUMN completed_at        TYPE timestamp with time zone USING completed_at        AT TIME ZONE 'UTC',
    ALTER COLUMN cancelled_at        TYPE timestamp with time zone USING cancelled_at        AT TIME ZONE 'UTC',
    ALTER COLUMN scheduled_pickup_at TYPE timestamp with time zone USING scheduled_pickup_at AT TIME ZONE 'UTC';

-- Recria a view com a definição original.
CREATE OR REPLACE VIEW available_on_demand_deliveries AS
SELECT d.id,
       d.client_id,
       d.from_address,
       d.from_lat,
       d.from_lng,
       d.to_address,
       d.to_lat,
       d.to_lng,
       d.distance_km,
       d.total_amount,
       d.item_description,
       d.created_at,
       u.username AS client_email,
       u.name AS client_name,
       CASE
           WHEN u.phone_ddd IS NOT NULL AND u.phone_number IS NOT NULL THEN
               CASE
                   WHEN length(u.phone_number::text) = 9 THEN concat('(', u.phone_ddd, ') ', substring(u.phone_number::text, 1, 5), '-', substring(u.phone_number::text, 6))
                   WHEN length(u.phone_number::text) = 8 THEN concat('(', u.phone_ddd, ') ', substring(u.phone_number::text, 1, 4), '-', substring(u.phone_number::text, 5))
                   ELSE concat('(', u.phone_ddd, ') ', u.phone_number)
               END
           ELSE NULL::text
       END AS client_phone
FROM deliveries d
JOIN users u ON u.id = d.client_id
WHERE d.delivery_type::text = 'ON_DEMAND'::text
  AND d.status::text = 'PENDING'::text
  AND d.courier_id IS NULL
ORDER BY d.created_at;

COMMENT ON COLUMN deliveries.created_at          IS 'UTC (timestamp with time zone)';
COMMENT ON COLUMN deliveries.updated_at          IS 'UTC (timestamp with time zone)';
COMMENT ON COLUMN deliveries.accepted_at         IS 'UTC (timestamp with time zone)';
COMMENT ON COLUMN deliveries.picked_up_at        IS 'UTC (timestamp with time zone)';
COMMENT ON COLUMN deliveries.in_transit_at       IS 'UTC (timestamp with time zone)';
COMMENT ON COLUMN deliveries.completed_at        IS 'UTC (timestamp with time zone)';
COMMENT ON COLUMN deliveries.cancelled_at        IS 'UTC (timestamp with time zone)';
