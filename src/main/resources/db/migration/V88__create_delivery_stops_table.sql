-- Migration V88: Create delivery_stops table for multi-stop deliveries
-- Extracts destination data from deliveries into a separate 1:N table.
-- CRITICAL: Migrates ALL existing deliveries to the new model without data loss.
-- The old columns in deliveries are kept intact for backward compatibility.

-- 1. Create the delivery_stops table
CREATE TABLE delivery_stops (
    id BIGSERIAL PRIMARY KEY,
    delivery_id BIGINT NOT NULL REFERENCES deliveries(id) ON DELETE CASCADE,
    stop_order INTEGER NOT NULL DEFAULT 1,
    address TEXT NOT NULL,
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    recipient_name VARCHAR(150),
    recipient_phone VARCHAR(20),
    item_description VARCHAR(500),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    completed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_stop_status CHECK (status IN ('PENDING', 'COMPLETED', 'SKIPPED')),
    CONSTRAINT chk_stop_order_positive CHECK (stop_order > 0)
);

CREATE INDEX idx_delivery_stops_delivery_id ON delivery_stops(delivery_id);
CREATE UNIQUE INDEX ux_delivery_stops_order ON delivery_stops(delivery_id, stop_order);

-- 2. Migrate ALL existing deliveries into delivery_stops (1 stop per delivery).
--    Status mapping:
--      COMPLETED → stop COMPLETED (entrega concluída)
--      CANCELLED → stop SKIPPED  (entrega cancelada, parada nunca foi executada)
--      Outros    → stop PENDING  (entrega em andamento ou aguardando)
--    Para deliveries com to_address NULL (dados inconsistentes), usa string vazia.
INSERT INTO delivery_stops (
    delivery_id, stop_order, address, latitude, longitude,
    recipient_name, recipient_phone, item_description,
    status, completed_at, created_at, updated_at
)
SELECT
    d.id,
    1,
    COALESCE(d.to_address, '(endereço não informado)'),
    d.to_lat,
    d.to_lng,
    d.recipient_name,
    d.recipient_phone,
    d.item_description,
    CASE
        WHEN d.status = 'COMPLETED'  THEN 'COMPLETED'
        WHEN d.status = 'CANCELLED'  THEN 'SKIPPED'
        ELSE 'PENDING'
    END,
    CASE WHEN d.status = 'COMPLETED' THEN d.completed_at ELSE NULL END,
    d.created_at,
    COALESCE(d.updated_at, d.created_at, CURRENT_TIMESTAMP)
FROM deliveries d;

-- 3. Verify migration: every delivery should have exactly 1 stop
DO $$
DECLARE
    total_deliveries INTEGER;
    total_stops INTEGER;
    orphan_deliveries INTEGER;
BEGIN
    SELECT COUNT(*) INTO total_deliveries FROM deliveries;
    SELECT COUNT(*) INTO total_stops FROM delivery_stops;
    SELECT COUNT(*) INTO orphan_deliveries
        FROM deliveries d
        WHERE NOT EXISTS (SELECT 1 FROM delivery_stops ds WHERE ds.delivery_id = d.id);

    RAISE NOTICE 'Migration V88 verification: % deliveries, % stops created, % without stops',
        total_deliveries, total_stops, orphan_deliveries;

    IF orphan_deliveries > 0 THEN
        RAISE EXCEPTION 'Migration V88 FAILED: % deliveries have no corresponding stop!', orphan_deliveries;
    END IF;
END $$;

-- 4. Add additional_stop_fee to site_configurations (default R$ 2.00)
ALTER TABLE site_configurations
    ADD COLUMN IF NOT EXISTS additional_stop_fee DECIMAL(10,2) NOT NULL DEFAULT 2.00;

COMMENT ON COLUMN site_configurations.additional_stop_fee IS 'Valor fixo em reais cobrado por cada parada adicional além da primeira';

-- NOTE: Old columns (to_address, to_lat, to_lng, recipient_name, recipient_phone, item_description)
-- are intentionally kept in deliveries for backward compatibility. They continue to be populated
-- by the application for the first stop. The canonical source for destination data is now delivery_stops.
