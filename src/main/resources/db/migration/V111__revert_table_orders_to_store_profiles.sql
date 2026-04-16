-- V111: Revert table_orders_enabled back to store_profiles only
-- V109 added it to users, V110 dropped from store_profiles — undo both

-- Restore column in store_profiles (dropped in V110)
ALTER TABLE store_profiles ADD COLUMN table_orders_enabled BOOLEAN NOT NULL DEFAULT false;

-- Copy data back from users
UPDATE store_profiles sp
SET table_orders_enabled = u.table_orders_enabled
FROM users u
WHERE sp.user_id = u.id
  AND u.table_orders_enabled = true;

-- Drop column from users (added in V109)
ALTER TABLE users DROP COLUMN table_orders_enabled;
