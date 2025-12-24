-- Migration: Add request and response columns to payments table
-- Purpose: Store the complete request sent to payment gateway and response received
-- Date: 2025-12-23

-- Add request column to store the complete request payload sent to create the payment
ALTER TABLE payments
ADD COLUMN request JSONB;

-- Add response column to store the complete response payload returned by the payment gateway
ALTER TABLE payments
ADD COLUMN response JSONB;

-- Add comments for documentation
COMMENT ON COLUMN payments.request IS 'Complete request payload sent to payment gateway (Pagar.me, Iugu, etc.) for auditing and debugging';
COMMENT ON COLUMN payments.response IS 'Complete response payload returned by payment gateway for auditing and debugging';
