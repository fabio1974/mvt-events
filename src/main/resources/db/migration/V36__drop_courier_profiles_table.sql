-- Migration V36: Remove courier_profiles table
-- The CourierProfile entity is no longer used in the system
-- All courier information is now managed through the User entity with role COURIER

DROP TABLE IF EXISTS courier_profiles CASCADE;
