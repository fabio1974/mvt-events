-- V61: Adicionar preferência de veículo na delivery
-- O cliente pode escolher MOTORCYCLE, CAR ou ANY (sem preferência)

ALTER TABLE deliveries
    ADD COLUMN preferred_vehicle_type VARCHAR(20) NOT NULL DEFAULT 'ANY';

-- Constraint para validar valores permitidos
ALTER TABLE deliveries
    ADD CONSTRAINT chk_preferred_vehicle_type
    CHECK (preferred_vehicle_type IN ('MOTORCYCLE', 'CAR', 'ANY'));

COMMENT ON COLUMN deliveries.preferred_vehicle_type IS 'Preferência de veículo: MOTORCYCLE (moto), CAR (automóvel), ANY (sem preferência)';
