-- Migration: Add UNIQUE constraint for UPSERT on user_push_tokens
-- Issue: ON CONFLICT requires unique constraint to work
-- Date: 2026-03-22

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint 
        WHERE conname = 'uk_user_push_tokens_user_token'
    ) THEN
        ALTER TABLE user_push_tokens 
        ADD CONSTRAINT uk_user_push_tokens_user_token UNIQUE (user_id, token);
    END IF;
END $$;
