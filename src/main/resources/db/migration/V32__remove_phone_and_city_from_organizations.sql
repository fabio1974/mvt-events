-- Remove phone and city_id fields from organizations table
ALTER TABLE organizations DROP COLUMN IF EXISTS phone;
ALTER TABLE organizations DROP COLUMN IF EXISTS city_id;
