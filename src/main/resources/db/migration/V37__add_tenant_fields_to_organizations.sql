-- V37: Add city, commission and status to organizations
-- Part of consolidating ADM role into ORGANIZER + Organization model
-- Note: Multi-tenancy uses organization.id directly (no separate tenant_id field)

-- Add city relationship (from old ADMProfile.region)
ALTER TABLE organizations ADD COLUMN city_id BIGINT;
ALTER TABLE organizations ADD CONSTRAINT fk_organizations_city
    FOREIGN KEY (city_id) REFERENCES cities(id);

-- Add commission percentage (from old ADMProfile.commission_percentage, default 5%)
ALTER TABLE organizations ADD COLUMN commission_percentage DECIMAL(5,2) NOT NULL DEFAULT 5.00;

-- Add status (from old ADMProfile.status, default ACTIVE)
ALTER TABLE organizations ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE';
ALTER TABLE organizations ADD CONSTRAINT chk_organization_status
    CHECK (status IN ('ACTIVE', 'INACTIVE', 'SUSPENDED'));

-- Create index for status filtering
CREATE INDEX idx_organizations_status ON organizations(status);
