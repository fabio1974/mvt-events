-- V6: Consolidate athlete emergency contact fields
-- Merges emergency_contact_name and emergency_contact_phone into single emergency_contact field

-- Add new emergency_contact column
ALTER TABLE athletes ADD COLUMN emergency_contact TEXT;

-- Migrate existing data: combine name and phone
UPDATE athletes 
SET emergency_contact = 
    CASE 
        WHEN emergency_contact_name IS NOT NULL AND emergency_contact_phone IS NOT NULL THEN
            emergency_contact_name || ' - ' || emergency_contact_phone
        WHEN emergency_contact_name IS NOT NULL THEN
            emergency_contact_name
        WHEN emergency_contact_phone IS NOT NULL THEN
            emergency_contact_phone
        ELSE NULL
    END;

-- Drop old columns
ALTER TABLE athletes DROP COLUMN emergency_contact_name;
ALTER TABLE athletes DROP COLUMN emergency_contact_phone;

-- Update Athlete birth_date to date_of_birth to match entity
ALTER TABLE athletes RENAME COLUMN birth_date TO date_of_birth;