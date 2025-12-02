-- V8__migrate_email_to_username_and_drop_email_column.sql
-- 1. Move all email data to username if username is not already an email
UPDATE users SET username = email WHERE username IS DISTINCT FROM email;
-- 2. Drop unique constraint on email if exists
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ukr43af9ap4edm43mmtq01oddj6') THEN
        ALTER TABLE users DROP CONSTRAINT ukr43af9ap4edm43mmtq01oddj6;
    END IF;
END $$;
-- 3. Drop email column
ALTER TABLE users DROP COLUMN IF EXISTS email;
-- 4. Add unique constraint to username (if not exists)
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ukr43af9ap4edm43mmtq01oddj6') THEN
        ALTER TABLE users ADD CONSTRAINT ukr43af9ap4edm43mmtq01oddj6 UNIQUE (username);
    END IF;
END $$;