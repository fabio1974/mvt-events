-- V8: Add remaining missing columns to EventFinancials to match Java entity

-- Add missing revenue tracking columns that weren't in V7
DO $$ 
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name = 'event_financials' 
                   AND column_name = 'total_revenue') THEN
        ALTER TABLE event_financials ADD COLUMN total_revenue DECIMAL(12,2) DEFAULT 0 NOT NULL;
    END IF;
END $$;

DO $$ 
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name = 'event_financials' 
                   AND column_name = 'platform_fees') THEN
        ALTER TABLE event_financials ADD COLUMN platform_fees DECIMAL(12,2) DEFAULT 0 NOT NULL;
    END IF;
END $$;

DO $$ 
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name = 'event_financials' 
                   AND column_name = 'net_revenue') THEN
        ALTER TABLE event_financials ADD COLUMN net_revenue DECIMAL(12,2) DEFAULT 0 NOT NULL;
    END IF;
END $$;

DO $$ 
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name = 'event_financials' 
                   AND column_name = 'total_transfer_fees') THEN
        ALTER TABLE event_financials ADD COLUMN total_transfer_fees DECIMAL(12,2) DEFAULT 0 NOT NULL;
    END IF;
END $$;

-- Update existing data to map old columns to new structure
UPDATE event_financials SET 
    total_revenue = COALESCE(total_collected, 0),
    platform_fees = COALESCE(platform_fee_amount, 0),
    net_revenue = COALESCE(organizer_net_amount, 0)
WHERE total_revenue = 0 OR platform_fees = 0;

-- Add comment for documentation
COMMENT ON COLUMN event_financials.total_revenue IS 'Total revenue collected for this event';
COMMENT ON COLUMN event_financials.platform_fees IS 'Total platform fees charged';
COMMENT ON COLUMN event_financials.net_revenue IS 'Net revenue after platform fees';
COMMENT ON COLUMN event_financials.total_transfer_fees IS 'Total fees charged for transfers';