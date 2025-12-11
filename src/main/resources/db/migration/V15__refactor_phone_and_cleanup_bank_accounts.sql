-- Migration V15: Refactor phone fields and cleanup duplicated data in bank_accounts
-- Author: System
-- Date: 2025-12-10
--
-- Changes:
-- 1. Add phone_ddd and phone_number to users
-- 2. Migrate phone data from bank_accounts to users
-- 3. Remove duplicated fields from bank_accounts (accountHolderName, accountHolderDocument, email, birthdate, phoneDdd, phoneNumber)
-- 4. These fields will be retrieved from User entity when needed for Pagar.me API

-- ============================================================================
-- 1. ADD PHONE FIELDS TO USERS
-- ============================================================================

ALTER TABLE users ADD COLUMN IF NOT EXISTS phone_ddd VARCHAR(2);
ALTER TABLE users ADD COLUMN IF NOT EXISTS phone_number VARCHAR(9);

-- ============================================================================
-- 2. MIGRATE PHONE DATA FROM BANK_ACCOUNTS TO USERS
-- ============================================================================

-- Update users with phone data from bank_accounts where available
UPDATE users u
SET 
    phone_ddd = ba.phone_ddd,
    phone_number = ba.phone_number
FROM bank_accounts ba
WHERE u.id = ba.user_id
    AND ba.phone_ddd IS NOT NULL
    AND ba.phone_number IS NOT NULL
    AND (u.phone_ddd IS NULL OR u.phone_number IS NULL);

-- ============================================================================
-- 3. MIGRATE LEGACY PHONE FIELD TO phone_ddd + phone_number
-- ============================================================================

-- For users with old 'phone' field but no phone_ddd/phone_number
-- Expected format: (11) 98765-4321 or 11987654321
UPDATE users
SET 
    phone_ddd = CASE 
        WHEN phone ~ '^\(?(\d{2})\)?' THEN 
            substring(regexp_replace(phone, '[^0-9]', '', 'g') from 1 for 2)
        ELSE NULL
    END,
    phone_number = CASE 
        WHEN phone ~ '^\(?(\d{2})\)?' THEN 
            substring(regexp_replace(phone, '[^0-9]', '', 'g') from 3)
        ELSE NULL
    END
WHERE phone IS NOT NULL
    AND (phone_ddd IS NULL OR phone_number IS NULL)
    AND length(regexp_replace(phone, '[^0-9]', '', 'g')) >= 10;

-- ============================================================================
-- 4. DROP DUPLICATED FIELDS FROM BANK_ACCOUNTS
-- ============================================================================

-- These fields are now retrieved from User entity:
-- - accountHolderName → user.name
-- - accountHolderDocument → user.cpf (document_number)
-- - email → user.username
-- - birthdate → user.dateOfBirth (date_of_birth)
-- - phoneDdd → user.phoneDdd (phone_ddd)
-- - phoneNumber → user.phoneNumber (phone_number)

ALTER TABLE bank_accounts DROP COLUMN IF EXISTS account_holder_name;
ALTER TABLE bank_accounts DROP COLUMN IF EXISTS account_holder_document;
ALTER TABLE bank_accounts DROP COLUMN IF EXISTS email;
ALTER TABLE bank_accounts DROP COLUMN IF EXISTS birthdate;
ALTER TABLE bank_accounts DROP COLUMN IF EXISTS phone_ddd;
ALTER TABLE bank_accounts DROP COLUMN IF EXISTS phone_number;

-- ============================================================================
-- 5. ADD INDEXES FOR PHONE FIELDS
-- ============================================================================

CREATE INDEX IF NOT EXISTS idx_users_phone_ddd ON users(phone_ddd);
CREATE INDEX IF NOT EXISTS idx_users_phone_number ON users(phone_number);

-- ============================================================================
-- ROLLBACK INSTRUCTIONS (if needed)
-- ============================================================================

-- To rollback this migration:
-- 
-- ALTER TABLE bank_accounts ADD COLUMN account_holder_name VARCHAR(200);
-- ALTER TABLE bank_accounts ADD COLUMN account_holder_document VARCHAR(14);
-- ALTER TABLE bank_accounts ADD COLUMN email VARCHAR(200);
-- ALTER TABLE bank_accounts ADD COLUMN birthdate VARCHAR(10);
-- ALTER TABLE bank_accounts ADD COLUMN phone_ddd VARCHAR(2);
-- ALTER TABLE bank_accounts ADD COLUMN phone_number VARCHAR(9);
-- 
-- UPDATE bank_accounts ba
-- SET 
--     account_holder_name = u.name,
--     account_holder_document = u.cpf,
--     email = u.username,
--     birthdate = TO_CHAR(u.date_of_birth, 'DD/MM/YYYY'),
--     phone_ddd = u.phone_ddd,
--     phone_number = u.phone_number
-- FROM users u
-- WHERE ba.user_id = u.id;
-- 
-- DROP INDEX IF EXISTS idx_users_phone_ddd;
-- DROP INDEX IF EXISTS idx_users_phone_number;
-- ALTER TABLE users DROP COLUMN IF EXISTS phone_ddd;
-- ALTER TABLE users DROP COLUMN IF EXISTS phone_number;
