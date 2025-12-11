-- Migration V14: Create addresses table and migrate data from users and bank_accounts
-- Author: System
-- Date: 2025-12-10
--
-- IMPORTANT: This migration creates the addresses table for FIXED addresses.
-- The users table keeps gps_latitude and gps_longitude for REAL-TIME GPS tracking.

-- ============================================================================
-- 1. CREATE ADDRESSES TABLE
-- ============================================================================

CREATE TABLE addresses (
    id BIGSERIAL PRIMARY KEY,
    user_id UUID NOT NULL UNIQUE,
    city_id BIGINT, -- Nullable temporariamente para permitir migração
    street VARCHAR(200) NOT NULL,
    number VARCHAR(10) NOT NULL,
    complement VARCHAR(100),
    neighborhood VARCHAR(100) NOT NULL,
    reference_point VARCHAR(200),
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_addresses_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_addresses_city FOREIGN KEY (city_id) REFERENCES cities(id)
);

-- Index para busca por usuário
CREATE INDEX idx_addresses_user_id ON addresses(user_id);

-- Index para busca por cidade
CREATE INDEX idx_addresses_city_id ON addresses(city_id);

-- ============================================================================
-- 2. MIGRATE DATA FROM BANK_ACCOUNTS TO ADDRESSES
-- ============================================================================

-- Inserir endereços a partir de bank_accounts que possuem dados completos
INSERT INTO addresses (
    user_id,
    city_id,
    street,
    number,
    complement,
    neighborhood,
    reference_point,
    latitude,
    longitude,
    created_at,
    updated_at
)
SELECT 
    ba.user_id,
    -- Buscar city_id pelo nome da cidade e estado (LEFT JOIN para permitir NULL)
    c.id as city_id,
    ba.address_street,
    ba.address_number,
    ba.address_complement,
    ba.address_neighborhood,
    ba.address_reference_point,
    NULL as latitude, -- Será preenchido depois se existir em users
    NULL as longitude,
    ba.created_at,
    ba.updated_at
FROM bank_accounts ba
LEFT JOIN cities c ON LOWER(c.name) = LOWER(ba.address_city) 
    AND UPPER(c.state) = UPPER(ba.address_state)
WHERE ba.address_street IS NOT NULL 
    AND ba.address_number IS NOT NULL
    AND ba.address_neighborhood IS NOT NULL
ON CONFLICT (user_id) DO NOTHING;

-- ============================================================================
-- 3. UPDATE ADDRESSES WITH LATITUDE/LONGITUDE FROM USERS (if exists)
-- ============================================================================

-- Note: Users may have old "latitude" and "longitude" columns from previous schema
-- If they exist, migrate them to addresses. Otherwise, skip this step.
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns 
               WHERE table_name='users' AND column_name='latitude') THEN
        UPDATE addresses a
        SET 
            latitude = u.latitude,
            longitude = u.longitude,
            updated_at = CURRENT_TIMESTAMP
        FROM users u
        WHERE a.user_id = u.id
            AND u.latitude IS NOT NULL
            AND u.longitude IS NOT NULL;
    END IF;
END $$;

-- ============================================================================
-- 4. MIGRATE DATA FROM USERS (for users without bank_account data)
-- ============================================================================

-- Para usuários que têm address/latitude/longitude mas não têm endereço em bank_account
-- Criar endereço básico baseado no campo "address" de users (se existir)
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns 
               WHERE table_name='users' AND column_name='address') THEN
        INSERT INTO addresses (
            user_id,
            city_id,
            street,
            number,
            complement,
            neighborhood,
            reference_point,
            latitude,
            longitude,
            created_at,
            updated_at
        )
        SELECT 
            u.id as user_id,
            -- Usar city_id do usuário, ou cidade padrão de SP
            COALESCE(
                u.city_id,
                (SELECT id FROM cities WHERE state = 'SP' LIMIT 1)
            ) as city_id,
            COALESCE(u.address, 'Endereço não informado') as street,
            'S/N' as number,
            NULL as complement,
            'Centro' as neighborhood,
            NULL as reference_point,
            u.latitude,
            u.longitude,
            u.created_at,
            u.updated_at
        FROM users u
        WHERE NOT EXISTS (SELECT 1 FROM addresses a WHERE a.user_id = u.id)
            AND (u.address IS NOT NULL OR u.latitude IS NOT NULL OR u.longitude IS NOT NULL)
        ON CONFLICT (user_id) DO NOTHING;
    END IF;
END $$;

-- ============================================================================
-- 5. DROP ADDRESS COLUMNS FROM BANK_ACCOUNTS
-- ============================================================================

ALTER TABLE bank_accounts DROP COLUMN IF EXISTS address_street;
ALTER TABLE bank_accounts DROP COLUMN IF EXISTS address_number;
ALTER TABLE bank_accounts DROP COLUMN IF EXISTS address_complement;
ALTER TABLE bank_accounts DROP COLUMN IF EXISTS address_neighborhood;
ALTER TABLE bank_accounts DROP COLUMN IF EXISTS address_city;
ALTER TABLE bank_accounts DROP COLUMN IF EXISTS address_state;
ALTER TABLE bank_accounts DROP COLUMN IF EXISTS address_zip_code;
ALTER TABLE bank_accounts DROP COLUMN IF EXISTS address_reference_point;

-- ============================================================================
-- 6. DROP OLD ADDRESS COLUMNS FROM USERS (keep GPS columns!)
-- ============================================================================

-- IMPORTANT: We keep gps_latitude and gps_longitude for real-time tracking
-- We only remove the old FIXED address fields
ALTER TABLE users DROP COLUMN IF EXISTS address;
ALTER TABLE users DROP COLUMN IF EXISTS latitude;
ALTER TABLE users DROP COLUMN IF EXISTS longitude;

-- ============================================================================
-- ROLLBACK INSTRUCTIONS (if needed)
-- ============================================================================

-- To rollback this migration:
-- 1. Re-add columns to users and bank_accounts
-- 2. Copy data back from addresses table
-- 3. Drop addresses table
-- 
-- ALTER TABLE users ADD COLUMN address VARCHAR(500);
-- ALTER TABLE users ADD COLUMN latitude DECIMAL(10, 7);
-- ALTER TABLE users ADD COLUMN longitude DECIMAL(10, 7);
-- 
-- ALTER TABLE bank_accounts ADD COLUMN address_street VARCHAR(200);
-- ALTER TABLE bank_accounts ADD COLUMN address_number VARCHAR(10);
-- ... (add all other columns)
-- 
-- UPDATE users u SET address = a.street, latitude = a.latitude, longitude = a.longitude
-- FROM addresses a WHERE u.id = a.user_id;
-- 
-- UPDATE bank_accounts ba SET address_street = a.street, address_number = a.number, ...
-- FROM addresses a WHERE ba.user_id = a.user_id;
-- 
-- DROP TABLE addresses;
