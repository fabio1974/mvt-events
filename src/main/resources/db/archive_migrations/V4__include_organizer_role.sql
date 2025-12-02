-- ============================================================================
-- MIGRATION V4: MERGE USERS AND ATHLETES + CLEANUP UNNECESSARY FIELDS
-- ============================================================================
-- This migration merges users and athletes tables and removes unnecessary fields
-- ORGANIZER users can also register for events as athletes

-- ============================================================================
-- STEP 1: ADD ATHLETE FIELDS TO USERS TABLE
-- ============================================================================

-- Add athlete-specific fields to users table
ALTER TABLE users ADD COLUMN IF NOT EXISTS address TEXT;
ALTER TABLE users ADD COLUMN IF NOT EXISTS city VARCHAR(100);
ALTER TABLE users ADD COLUMN IF NOT EXISTS country VARCHAR(100);
ALTER TABLE users ADD COLUMN IF NOT EXISTS date_of_birth DATE;
ALTER TABLE users ADD COLUMN IF NOT EXISTS document_number VARCHAR(20);
ALTER TABLE users ADD COLUMN IF NOT EXISTS emergency_contact VARCHAR(255);
ALTER TABLE users ADD COLUMN IF NOT EXISTS gender VARCHAR(10);
ALTER TABLE users ADD COLUMN IF NOT EXISTS name VARCHAR(255);
ALTER TABLE users ADD COLUMN IF NOT EXISTS phone VARCHAR(255);
ALTER TABLE users ADD COLUMN IF NOT EXISTS state VARCHAR(100);

-- Add organization relationship for organizers
ALTER TABLE users ADD COLUMN IF NOT EXISTS organization_id BIGINT;

-- Remove tenant_id from users as it's not needed (users are global)
ALTER TABLE users DROP COLUMN IF EXISTS tenant_id;

-- ============================================================================
-- STEP 2: UPDATE USER ROLE ENUM TO INCLUDE ORGANIZER
-- ============================================================================

-- Drop existing role constraint
ALTER TABLE users DROP CONSTRAINT IF EXISTS users_role_check;

-- Add new role constraint with ORGANIZER
ALTER TABLE users ADD CONSTRAINT users_role_check 
    CHECK (role IN ('USER', 'ORGANIZER', 'ADMIN'));

-- Add gender constraint
ALTER TABLE users ADD CONSTRAINT users_gender_check 
    CHECK (gender IN ('MALE', 'FEMALE', 'OTHER'));

-- ============================================================================
-- STEP 3: CREATE ATHLETE-USER MAPPING FOR DATA MIGRATION
-- ============================================================================

-- Create a mapping table for athlete_id to user_id conversion
CREATE TEMPORARY TABLE athlete_user_mapping AS
SELECT 
    a.id as athlete_id,
    COALESCE(u.id, gen_random_uuid()) as user_id,
    CASE WHEN u.id IS NULL THEN true ELSE false END as needs_insert
FROM athletes a
LEFT JOIN users u ON u.email = a.email;

-- ============================================================================
-- STEP 4: MIGRATE DATA FROM ATHLETES TO USERS
-- ============================================================================

-- Insert new users for athletes that don't exist as users yet
INSERT INTO users (
    id, created_at, updated_at, email, enabled, password, role, username,
    address, city, country, date_of_birth, document_number, 
    emergency_contact, gender, name, phone, state
)
SELECT 
    aum.user_id,
    COALESCE(a.created_at, NOW()),
    COALESCE(a.updated_at, NOW()),
    a.email,
    true, -- enabled
    '$2a$10$2X8wNBhT5p5Nw8aKzE3/xOQh7L.jF9yV2sL8mJ6qN3pR4tY7uZ9wG', -- Default password hash for "password123"
    'USER', -- role - all athletes become USER role
    a.email, -- username = email
    a.address,
    a.city,
    a.country,
    a.date_of_birth,
    a.document_number,
    a.emergency_contact,
    a.gender,
    a.name,
    a.phone,
    a.state
FROM athletes a
JOIN athlete_user_mapping aum ON a.id = aum.athlete_id
WHERE aum.needs_insert = true;

-- Update existing users with athlete data (merge data for users that were also athletes)
UPDATE users u SET
    address = COALESCE(u.address, a.address),
    city = COALESCE(u.city, a.city),
    country = COALESCE(u.country, a.country),
    date_of_birth = COALESCE(u.date_of_birth, a.date_of_birth),
    document_number = COALESCE(u.document_number, a.document_number),
    emergency_contact = COALESCE(u.emergency_contact, a.emergency_contact),
    gender = COALESCE(u.gender, a.gender),
    name = COALESCE(u.name, a.name, u.username),
    phone = COALESCE(u.phone, a.phone),
    state = COALESCE(u.state, a.state),
    updated_at = GREATEST(u.updated_at, COALESCE(a.updated_at, u.updated_at))
FROM athletes a
JOIN athlete_user_mapping aum ON a.id = aum.athlete_id
WHERE u.id = aum.user_id AND aum.needs_insert = false;

-- ============================================================================
-- STEP 5: UPDATE REGISTRATIONS TO USE USER_ID INSTEAD OF ATHLETE_ID
-- ============================================================================

-- Add user_id column to registrations
ALTER TABLE registrations ADD COLUMN IF NOT EXISTS user_id UUID;

-- Populate user_id based on athlete_id mapping
UPDATE registrations r 
SET user_id = aum.user_id
FROM athlete_user_mapping aum
WHERE r.athlete_id = aum.athlete_id;

-- Verify all registrations have user_id populated
DO $$
DECLARE
    missing_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO missing_count FROM registrations WHERE user_id IS NULL;
    IF missing_count > 0 THEN
        RAISE EXCEPTION 'Migration failed: % registrations without user_id', missing_count;
    END IF;
END $$;

-- Make user_id NOT NULL after population and verification
ALTER TABLE registrations ALTER COLUMN user_id SET NOT NULL;

-- ============================================================================
-- STEP 6: ADD NEW CONSTRAINTS AND INDEXES
-- ============================================================================

-- Add foreign key constraints
ALTER TABLE users ADD CONSTRAINT fk_users_organization 
    FOREIGN KEY (organization_id) REFERENCES organizations(id);

ALTER TABLE registrations ADD CONSTRAINT fk_registrations_user 
    FOREIGN KEY (user_id) REFERENCES users(id);

-- Add unique constraints
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_users_email') THEN
        ALTER TABLE users ADD CONSTRAINT uk_users_email UNIQUE (email);
    END IF;
END $$;

-- Add unique constraint for document_number (only when not null)
CREATE UNIQUE INDEX IF NOT EXISTS uk_users_document_number 
    ON users (document_number) 
    WHERE document_number IS NOT NULL;

-- Add unique constraint for user per event registration (replaces athlete+event unique)
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_registrations_user_event') THEN
        ALTER TABLE registrations ADD CONSTRAINT uk_registrations_user_event 
            UNIQUE (user_id, event_id);
    END IF;
END $$;

-- ============================================================================
-- STEP 7: CREATE PERFORMANCE INDEXES
-- ============================================================================

-- Indexes for faster lookups
CREATE INDEX IF NOT EXISTS idx_users_organization_id ON users (organization_id);
CREATE INDEX IF NOT EXISTS idx_users_role ON users (role);
CREATE INDEX IF NOT EXISTS idx_users_email ON users (email);
CREATE INDEX IF NOT EXISTS idx_users_document_number ON users (document_number) WHERE document_number IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_registrations_user_id ON registrations (user_id);

-- ============================================================================
-- STEP 8: DROP OLD CONSTRAINTS AND REFERENCES
-- ============================================================================

-- Drop old foreign key from registrations to athletes
ALTER TABLE registrations DROP CONSTRAINT IF EXISTS fkn59hfc6jmae6j23flb53nqqvd;

-- Drop old unique constraint from registrations (athlete + event)
ALTER TABLE registrations DROP CONSTRAINT IF EXISTS uk18v3flm45yg9d3o380mne8cof;

-- Drop old unique constraints from athletes (will be dropped with table)
ALTER TABLE athletes DROP CONSTRAINT IF EXISTS ukek0v4gdbthm9d0pgs4itg21j0;
ALTER TABLE athletes DROP CONSTRAINT IF EXISTS ukn0i3de0t8dyv19qn45vgyi322;

-- ============================================================================
-- STEP 9: DROP UNNECESSARY COLUMNS AND TABLES
-- ============================================================================

-- Drop athlete_id column from registrations (data already migrated to user_id)
ALTER TABLE registrations DROP COLUMN IF EXISTS athlete_id;

-- Drop the entire athletes table (data already migrated to users)
DROP TABLE IF EXISTS athletes CASCADE;

-- Drop temporary mapping table
DROP TABLE IF EXISTS athlete_user_mapping;

-- ============================================================================
-- STEP 10: ADD COMMENTS FOR DOCUMENTATION
-- ============================================================================

COMMENT ON TABLE users IS 'Unified table for system users (athletes, organizers, admins)';
COMMENT ON COLUMN users.organization_id IS 'Organization that the user belongs to (for ORGANIZER role only)';
COMMENT ON COLUMN users.role IS 'User role: USER (athlete), ORGANIZER (can create events and register as athlete), ADMIN';
COMMENT ON COLUMN users.name IS 'Full name of the user/athlete';
COMMENT ON COLUMN users.date_of_birth IS 'Date of birth for athletes';
COMMENT ON COLUMN users.document_number IS 'Document number (CPF, passport, etc.) - unique when not null';
COMMENT ON COLUMN users.gender IS 'Gender: MALE, FEMALE, or OTHER';

COMMENT ON COLUMN registrations.user_id IS 'Reference to the user who registered (replaces athlete_id) - can be USER or ORGANIZER';

-- ============================================================================
-- MIGRATION COMPLETE
-- ============================================================================