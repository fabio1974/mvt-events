-- Migration: Create delivery_stops table for multi-stop deliveries
-- Extracts destination data from deliveries into a separate 1:N table.
-- Only CLIENT role users can create multi-stop deliveries; CUSTOMER stays single-stop.

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

-- 2. Migrate existing destination data from deliveries into delivery_stops (1 stop per existing delivery)
INSERT INTO delivery_stops (delivery_id, stop_order, address, latitude, longitude, recipient_name, recipient_phone, item_description, status, completed_at, created_at, updated_at)
SELECT
    d.id,
    1,
    d.to_address,
    d.to_lat,
    d.to_lng,
    d.recipient_name,
    d.recipient_phone,
    d.item_description,
    CASE WHEN d.status = 'COMPLETED' THEN 'COMPLETED' ELSE 'PENDING' END,
    d.completed_at,
    d.created_at,
    d.updated_at
FROM deliveries d
WHERE d.to_address IS NOT NULL;

-- 3. Add additional_stop_fee to site_configurations (default R$ 2.00)
ALTER TABLE site_configurations
    ADD COLUMN IF NOT EXISTS additional_stop_fee DECIMAL(10,2) NOT NULL DEFAULT 2.00;

COMMENT ON COLUMN site_configurations.additional_stop_fee IS 'Valor fixo em reais cobrado por cada parada adicional além da primeira';

-- Note: we keep the old columns (to_address, to_lat, to_lng, recipient_name, recipient_phone, item_description)
-- in deliveries for now to avoid breaking existing queries. They will be deprecated gradually.
-- The source of truth for destination data is now delivery_stops.
