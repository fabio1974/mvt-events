-- V109: Move table_orders_enabled flag from store_profiles to users table
-- Allows metadata-driven form to auto-render checkbox for admin to toggle per CLIENT

ALTER TABLE users ADD COLUMN table_orders_enabled BOOLEAN NOT NULL DEFAULT false;

-- Migrate existing data from store_profiles
UPDATE users u
SET table_orders_enabled = sp.table_orders_enabled
FROM store_profiles sp
WHERE sp.user_id = u.id
  AND sp.table_orders_enabled = true;
