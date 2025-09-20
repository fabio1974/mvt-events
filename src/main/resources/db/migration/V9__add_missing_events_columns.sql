-- V9: Add missing columns to events table to match Java entity

-- Add platform_fee_percentage column
DO $$ 
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name = 'events' 
                   AND column_name = 'platform_fee_percentage') THEN
        ALTER TABLE events ADD COLUMN platform_fee_percentage DECIMAL(5,4);
    END IF;
END $$;

-- Add transfer_frequency column
DO $$ 
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name = 'events' 
                   AND column_name = 'transfer_frequency') THEN
        ALTER TABLE events ADD COLUMN transfer_frequency VARCHAR(20) DEFAULT 'WEEKLY';
    END IF;
END $$;

-- Add constraint for transfer_frequency if column was added
DO $$ 
BEGIN
    -- First check if constraint already exists
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_events_transfer_frequency') THEN
        -- Add constraint for transfer_frequency
        ALTER TABLE events ADD CONSTRAINT chk_events_transfer_frequency 
            CHECK (transfer_frequency IS NULL OR transfer_frequency IN ('IMMEDIATE', 'DAILY', 'WEEKLY', 'MONTHLY', 'ON_DEMAND'));
    END IF;
END $$;

-- Add comments for documentation
COMMENT ON COLUMN events.platform_fee_percentage IS 'Platform fee percentage for this event (overrides organization default)';
COMMENT ON COLUMN events.transfer_frequency IS 'Transfer frequency for this event (overrides organization default)';