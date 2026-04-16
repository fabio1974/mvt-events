-- V115: Remove label column from restaurant_tables (replaced by status enum)
ALTER TABLE restaurant_tables DROP COLUMN IF EXISTS label;
