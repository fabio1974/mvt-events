-- Migration to drop courier_profiles table
-- The courier profile is no longer needed as a separate entity
-- All courier data is now managed through the users table with role COURIER

DROP TABLE IF EXISTS courier_profiles CASCADE;
