-- V110: Remove table_orders_enabled from store_profiles (moved to users in V109)
ALTER TABLE store_profiles DROP COLUMN table_orders_enabled;
