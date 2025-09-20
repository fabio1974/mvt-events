-- V7: Fix EventFinancials column naming to match Java entity

-- Rename last_transfer_at to last_transfer_date for consistency
ALTER TABLE event_financials RENAME COLUMN last_transfer_at TO last_transfer_date;

-- Add next_transfer_date column to match entity
ALTER TABLE event_financials ADD COLUMN next_transfer_date TIMESTAMP;

-- Add transfer_frequency column to match entity (if not exists)
DO $$ 
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name = 'event_financials' 
                   AND column_name = 'transfer_frequency') THEN
        ALTER TABLE event_financials ADD COLUMN transfer_frequency VARCHAR(20) DEFAULT 'WEEKLY' NOT NULL;
    END IF;
END $$;

-- Add total_payments column to match entity (if not exists)
DO $$ 
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name = 'event_financials' 
                   AND column_name = 'total_payments') THEN
        ALTER TABLE event_financials ADD COLUMN total_payments INTEGER DEFAULT 0 NOT NULL;
    END IF;
END $$;

-- Add pending_transfer_amount column to match entity (if not exists)
DO $$ 
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name = 'event_financials' 
                   AND column_name = 'pending_transfer_amount') THEN
        ALTER TABLE event_financials ADD COLUMN pending_transfer_amount DECIMAL(12,2) DEFAULT 0 NOT NULL;
    END IF;
END $$;

-- Add constraint for transfer_frequency
ALTER TABLE event_financials ADD CONSTRAINT chk_event_financials_transfer_frequency 
    CHECK (transfer_frequency IN ('IMMEDIATE', 'DAILY', 'WEEKLY', 'MONTHLY', 'ON_DEMAND'));