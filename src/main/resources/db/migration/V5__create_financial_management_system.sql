-- V5: Financial Management and Transfer System
-- Flexible transfer scheduling with organizer-configurable frequencies

-- =====================================================
-- 1. ADD FINANCIAL COLUMNS TO ORGANIZATIONS
-- =====================================================

-- Transfer settings
ALTER TABLE organizations ADD COLUMN auto_transfer BOOLEAN DEFAULT true;
ALTER TABLE organizations ADD COLUMN transfer_frequency VARCHAR(20) DEFAULT 'IMMEDIATE';
ALTER TABLE organizations ADD COLUMN transfer_day_of_week INTEGER; -- 1=Monday, 7=Sunday
ALTER TABLE organizations ADD COLUMN transfer_day_of_month INTEGER; -- 1-28 for monthly
ALTER TABLE organizations ADD COLUMN min_transfer_amount DECIMAL(10,2) DEFAULT 0;

-- Payment information
ALTER TABLE organizations ADD COLUMN pix_key VARCHAR(255);
ALTER TABLE organizations ADD COLUMN bank_name VARCHAR(100);
ALTER TABLE organizations ADD COLUMN bank_agency VARCHAR(20);
ALTER TABLE organizations ADD COLUMN bank_account VARCHAR(20);
ALTER TABLE organizations ADD COLUMN bank_account_type VARCHAR(20); -- 'CHECKING', 'SAVINGS'

-- Platform fee settings (can be customized per organization)
ALTER TABLE organizations ADD COLUMN platform_fee_rate DECIMAL(5,4) DEFAULT 0.05; -- 5% default

-- Add constraints for transfer frequency
ALTER TABLE organizations ADD CONSTRAINT chk_transfer_frequency 
    CHECK (transfer_frequency IN ('IMMEDIATE', 'DAILY', 'WEEKLY', 'MONTHLY', 'ON_DEMAND'));

ALTER TABLE organizations ADD CONSTRAINT chk_transfer_day_of_week 
    CHECK (transfer_day_of_week IS NULL OR (transfer_day_of_week >= 1 AND transfer_day_of_week <= 7));

ALTER TABLE organizations ADD CONSTRAINT chk_transfer_day_of_month 
    CHECK (transfer_day_of_month IS NULL OR (transfer_day_of_month >= 1 AND transfer_day_of_month <= 28));

ALTER TABLE organizations ADD CONSTRAINT chk_bank_account_type 
    CHECK (bank_account_type IS NULL OR bank_account_type IN ('CHECKING', 'SAVINGS'));

-- =====================================================
-- 2. EVENT FINANCIALS (Consolidation per event)
-- =====================================================

CREATE TABLE event_financials (
    id BIGSERIAL PRIMARY KEY,
    event_id BIGINT NOT NULL REFERENCES events(id) ON DELETE CASCADE,
    
    -- Collection totals
    total_collected DECIMAL(12,2) DEFAULT 0 NOT NULL,
    total_registrations INTEGER DEFAULT 0 NOT NULL,
    
    -- Platform fee calculations
    platform_fee_rate DECIMAL(5,4) NOT NULL,
    platform_fee_amount DECIMAL(12,2) DEFAULT 0 NOT NULL,
    
    -- Organizer amounts
    organizer_gross_amount DECIMAL(12,2) DEFAULT 0 NOT NULL, -- Before fees
    organizer_net_amount DECIMAL(12,2) DEFAULT 0 NOT NULL,   -- After fees
    
    -- Transfer tracking
    pending_transfers DECIMAL(12,2) DEFAULT 0 NOT NULL,
    transferred_amount DECIMAL(12,2) DEFAULT 0 NOT NULL,
    last_transfer_at TIMESTAMP,
    
    -- Metadata
    created_at TIMESTAMP DEFAULT NOW() NOT NULL,
    updated_at TIMESTAMP DEFAULT NOW() NOT NULL,
    
    -- Ensure one financial record per event
    UNIQUE(event_id)
);

-- Indexes for event_financials
CREATE INDEX idx_event_financials_event ON event_financials(event_id);
CREATE INDEX idx_event_financials_pending ON event_financials(pending_transfers);
CREATE INDEX idx_event_financials_updated ON event_financials(updated_at);

-- =====================================================
-- 3. TRANSFERS (Money transfers to organizers)
-- =====================================================

CREATE TABLE transfers (
    id BIGSERIAL PRIMARY KEY,
    event_id BIGINT NOT NULL REFERENCES events(id) ON DELETE CASCADE,
    organization_id BIGINT NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    
    -- Transfer details
    amount DECIMAL(12,2) NOT NULL,
    currency VARCHAR(3) DEFAULT 'BRL' NOT NULL,
    transfer_type VARCHAR(20) DEFAULT 'AUTOMATIC' NOT NULL, -- 'AUTOMATIC', 'MANUAL', 'SCHEDULED'
    
    -- Transfer method and destination
    transfer_method VARCHAR(50) NOT NULL, -- 'PIX', 'BANK_TRANSFER', 'TED'
    destination_key VARCHAR(255), -- PIX key or bank details
    destination_bank VARCHAR(100),
    destination_agency VARCHAR(20),
    destination_account VARCHAR(20),
    
    -- Status tracking
    status VARCHAR(20) DEFAULT 'PENDING' NOT NULL,
    
    -- Gateway integration
    gateway_provider VARCHAR(50), -- 'MERCADO_PAGO', 'STRIPE', 'MANUAL'
    gateway_transfer_id VARCHAR(255),
    gateway_fee DECIMAL(10,2) DEFAULT 0,
    gateway_response JSONB,
    
    -- Timestamps
    requested_at TIMESTAMP DEFAULT NOW() NOT NULL,
    processed_at TIMESTAMP,
    completed_at TIMESTAMP,
    failed_at TIMESTAMP,
    
    -- Failure tracking
    failure_reason TEXT,
    retry_count INTEGER DEFAULT 0,
    
    -- Metadata
    created_at TIMESTAMP DEFAULT NOW() NOT NULL,
    updated_at TIMESTAMP DEFAULT NOW() NOT NULL
);

-- Add constraints for transfers
ALTER TABLE transfers ADD CONSTRAINT chk_transfer_type 
    CHECK (transfer_type IN ('AUTOMATIC', 'MANUAL', 'SCHEDULED'));

ALTER TABLE transfers ADD CONSTRAINT chk_transfer_method 
    CHECK (transfer_method IN ('PIX', 'BANK_TRANSFER', 'TED', 'MANUAL'));

ALTER TABLE transfers ADD CONSTRAINT chk_transfer_status 
    CHECK (status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED', 'CANCELLED'));

ALTER TABLE transfers ADD CONSTRAINT chk_positive_amount 
    CHECK (amount > 0);

-- Indexes for transfers
CREATE INDEX idx_transfers_event ON transfers(event_id);
CREATE INDEX idx_transfers_organization ON transfers(organization_id);
CREATE INDEX idx_transfers_status ON transfers(status);
CREATE INDEX idx_transfers_gateway_id ON transfers(gateway_transfer_id);
CREATE INDEX idx_transfers_processed_at ON transfers(processed_at);
CREATE INDEX idx_transfers_requested_at ON transfers(requested_at);

-- =====================================================
-- 4. PAYMENT EVENTS LOG (Audit trail)
-- =====================================================

CREATE TABLE payment_events (
    id BIGSERIAL PRIMARY KEY,
    event_id BIGINT NOT NULL REFERENCES events(id) ON DELETE CASCADE,
    payment_id BIGINT NOT NULL REFERENCES payments(id) ON DELETE CASCADE,
    
    -- Event details
    event_type VARCHAR(50) NOT NULL, -- 'PAYMENT_COMPLETED', 'PAYMENT_REFUNDED', 'TRANSFER_CREATED'
    amount DECIMAL(12,2) NOT NULL,
    
    -- Financial breakdown
    platform_fee DECIMAL(10,2) NOT NULL,
    organizer_amount DECIMAL(10,2) NOT NULL,
    
    -- Related transfer (if created)
    transfer_id BIGINT REFERENCES transfers(id),
    
    -- Metadata
    metadata JSONB,
    created_at TIMESTAMP DEFAULT NOW() NOT NULL
);

-- Indexes for payment events
CREATE INDEX idx_payment_events_event ON payment_events(event_id);
CREATE INDEX idx_payment_events_payment ON payment_events(payment_id);
CREATE INDEX idx_payment_events_transfer ON payment_events(transfer_id);
CREATE INDEX idx_payment_events_type ON payment_events(event_type);
CREATE INDEX idx_payment_events_created ON payment_events(created_at);

-- =====================================================
-- 5. RLS POLICIES FOR FINANCIAL TABLES
-- =====================================================

-- Enable RLS on financial tables
ALTER TABLE event_financials ENABLE ROW LEVEL SECURITY;
ALTER TABLE transfers ENABLE ROW LEVEL SECURITY;
ALTER TABLE payment_events ENABLE ROW LEVEL SECURITY;

-- RLS Policies for EVENT_FINANCIALS
CREATE POLICY event_financials_isolation ON event_financials
    FOR ALL TO event_organizer
    USING (event_id = current_setting('app.current_event_id', true)::bigint);

CREATE POLICY platform_admin_event_financials ON event_financials
    FOR ALL TO platform_admin
    USING (true);

-- RLS Policies for TRANSFERS
CREATE POLICY transfers_isolation ON transfers
    FOR ALL TO event_organizer
    USING (event_id = current_setting('app.current_event_id', true)::bigint);

CREATE POLICY platform_admin_transfers ON transfers
    FOR ALL TO platform_admin
    USING (true);

-- RLS Policies for PAYMENT_EVENTS
CREATE POLICY payment_events_isolation ON payment_events
    FOR ALL TO event_organizer
    USING (event_id = current_setting('app.current_event_id', true)::bigint);

CREATE POLICY platform_admin_payment_events ON payment_events
    FOR ALL TO platform_admin
    USING (true);

-- =====================================================
-- 6. HELPER FUNCTIONS FOR FINANCIAL OPERATIONS
-- =====================================================

-- Function to initialize event financials
CREATE OR REPLACE FUNCTION initialize_event_financials(p_event_id BIGINT)
RETURNS VOID AS $$
DECLARE
    v_org_fee_rate DECIMAL(5,4);
BEGIN
    -- Get organization's platform fee rate
    SELECT platform_fee_rate INTO v_org_fee_rate
    FROM organizations o
    JOIN events e ON e.organization_id = o.id
    WHERE e.id = p_event_id;
    
    -- Insert if not exists
    INSERT INTO event_financials (event_id, platform_fee_rate)
    VALUES (p_event_id, v_org_fee_rate)
    ON CONFLICT (event_id) DO NOTHING;
END;
$$ LANGUAGE plpgsql;

-- Function to update financials after payment
CREATE OR REPLACE FUNCTION update_event_financials_on_payment(
    p_event_id BIGINT,
    p_payment_amount DECIMAL(12,2)
)
RETURNS VOID AS $$
DECLARE
    v_platform_fee DECIMAL(12,2);
    v_organizer_amount DECIMAL(12,2);
    v_fee_rate DECIMAL(5,4);
BEGIN
    -- Ensure financials record exists
    PERFORM initialize_event_financials(p_event_id);
    
    -- Get fee rate
    SELECT platform_fee_rate INTO v_fee_rate
    FROM event_financials
    WHERE event_id = p_event_id;
    
    -- Calculate amounts
    v_platform_fee := p_payment_amount * v_fee_rate;
    v_organizer_amount := p_payment_amount - v_platform_fee;
    
    -- Update financials
    UPDATE event_financials SET
        total_collected = total_collected + p_payment_amount,
        total_registrations = total_registrations + 1,
        platform_fee_amount = platform_fee_amount + v_platform_fee,
        organizer_gross_amount = organizer_gross_amount + p_payment_amount,
        organizer_net_amount = organizer_net_amount + v_organizer_amount,
        pending_transfers = pending_transfers + v_organizer_amount,
        updated_at = NOW()
    WHERE event_id = p_event_id;
END;
$$ LANGUAGE plpgsql;

-- Function to process transfer completion
CREATE OR REPLACE FUNCTION complete_transfer(p_transfer_id BIGINT)
RETURNS VOID AS $$
DECLARE
    v_event_id BIGINT;
    v_amount DECIMAL(12,2);
BEGIN
    -- Get transfer details
    SELECT event_id, amount INTO v_event_id, v_amount
    FROM transfers
    WHERE id = p_transfer_id;
    
    -- Update transfer status
    UPDATE transfers SET
        status = 'COMPLETED',
        completed_at = NOW(),
        updated_at = NOW()
    WHERE id = p_transfer_id;
    
    -- Update event financials
    UPDATE event_financials SET
        pending_transfers = pending_transfers - v_amount,
        transferred_amount = transferred_amount + v_amount,
        last_transfer_at = NOW(),
        updated_at = NOW()
    WHERE event_id = v_event_id;
END;
$$ LANGUAGE plpgsql;

-- =====================================================
-- 7. GRANTS AND PERMISSIONS
-- =====================================================

-- Grant permissions on new tables
GRANT ALL ON event_financials TO platform_admin, event_organizer;
GRANT ALL ON transfers TO platform_admin, event_organizer;
GRANT ALL ON payment_events TO platform_admin, event_organizer;

GRANT SELECT ON event_financials TO athlete_user;
GRANT SELECT ON transfers TO athlete_user;
GRANT SELECT ON payment_events TO athlete_user;

-- Grant sequence permissions
GRANT USAGE ON ALL SEQUENCES IN SCHEMA public TO event_organizer, platform_admin, athlete_user;

-- =====================================================
-- 8. SAMPLE DATA FOR TESTING
-- =====================================================

-- Update sample organization with transfer settings
UPDATE organizations SET 
    auto_transfer = false,
    transfer_frequency = 'WEEKLY',
    transfer_day_of_week = 5, -- Friday
    min_transfer_amount = 100.00,
    pix_key = 'sample@org.com',
    platform_fee_rate = 0.05
WHERE name = 'Sample Sports Events';

COMMENT ON TABLE event_financials IS 'Financial consolidation per event with transfer tracking';
COMMENT ON TABLE transfers IS 'Money transfers from platform to event organizers';
COMMENT ON TABLE payment_events IS 'Audit trail for all payment and transfer events';