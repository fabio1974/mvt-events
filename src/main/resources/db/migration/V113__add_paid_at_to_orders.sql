-- V113: Add paid_at timestamp to orders (tracks when payment was confirmed)
ALTER TABLE orders ADD COLUMN paid_at TIMESTAMP WITH TIME ZONE;
