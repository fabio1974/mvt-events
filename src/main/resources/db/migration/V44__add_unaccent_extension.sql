-- Migration V44: Enable unaccent extension for accent-insensitive search
-- This allows searching "joao" to find "João", "Grilo" to find "Grílò", etc.

-- Try to enable the unaccent extension (may fail on managed databases without superuser privileges)
-- If it fails, we'll create a fallback function that does manual accent removal
DO $$
BEGIN
    CREATE EXTENSION IF NOT EXISTS unaccent;
EXCEPTION WHEN OTHERS THEN
    RAISE NOTICE 'unaccent extension not available, will use manual accent removal';
END $$;

-- Create an immutable version of unaccent for use in indexes
-- Uses manual translate() for accent removal (works everywhere, no extension needed)
CREATE OR REPLACE FUNCTION immutable_unaccent(text)
RETURNS text AS $$
    SELECT translate($1, 
        'áàâãäåÁÀÂÃÄÅéèêëÉÈÊËíìîïÍÌÎÏóòôõöÓÒÔÕÖúùûüÚÙÛÜçÇñÑýÝ',
        'aaaaaaAAAAAAeeeeEEEEiiiiIIIIoooooOOOOOuuuuUUUUcCnNyY')
$$ LANGUAGE SQL IMMUTABLE PARALLEL SAFE;

-- Create indexes for faster accent-insensitive searches on commonly searched fields

-- Users table - name and username
CREATE INDEX IF NOT EXISTS idx_users_name_unaccent ON users (immutable_unaccent(lower(name)));
CREATE INDEX IF NOT EXISTS idx_users_username_unaccent ON users (immutable_unaccent(lower(username)));

-- Cities table - name (heavily searched for typeahead)
CREATE INDEX IF NOT EXISTS idx_cities_name_unaccent ON cities (immutable_unaccent(lower(name)));

-- Organizations table - name
CREATE INDEX IF NOT EXISTS idx_organizations_name_unaccent ON organizations (immutable_unaccent(lower(name)));

COMMENT ON FUNCTION immutable_unaccent(text) IS 'Immutable function for accent-insensitive searches using translate()';
