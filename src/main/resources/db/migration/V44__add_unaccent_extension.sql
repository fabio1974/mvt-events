-- Migration V44: Enable unaccent extension for accent-insensitive search
-- This allows searching "joao" to find "João", "Grilo" to find "Grílò", etc.

-- Enable the unaccent extension (requires superuser or extension creation privileges)
CREATE EXTENSION IF NOT EXISTS unaccent;

-- Create an immutable version of unaccent for use in indexes
-- This is needed because the default unaccent function is not IMMUTABLE
CREATE OR REPLACE FUNCTION immutable_unaccent(text)
RETURNS text AS $$
    SELECT unaccent('unaccent', $1)
$$ LANGUAGE SQL IMMUTABLE PARALLEL SAFE;

-- Create indexes for faster accent-insensitive searches on commonly searched fields

-- Users table - name and username
CREATE INDEX IF NOT EXISTS idx_users_name_unaccent ON users (immutable_unaccent(lower(name)));
CREATE INDEX IF NOT EXISTS idx_users_username_unaccent ON users (immutable_unaccent(lower(username)));

-- Cities table - name (heavily searched for typeahead)
CREATE INDEX IF NOT EXISTS idx_cities_name_unaccent ON cities (immutable_unaccent(lower(name)));

-- Organizations table - name
CREATE INDEX IF NOT EXISTS idx_organizations_name_unaccent ON organizations (immutable_unaccent(lower(name)));

COMMENT ON FUNCTION immutable_unaccent(text) IS 'Immutable version of unaccent for use in indexes and accent-insensitive searches';
