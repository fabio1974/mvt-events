-- V17: Add zip_code column to addresses table
-- This field stores the CEP (Brazilian postal code) without formatting (8 digits)

ALTER TABLE addresses ADD COLUMN IF NOT EXISTS zip_code VARCHAR(8);

COMMENT ON COLUMN addresses.zip_code IS 'CEP sem pontuação (8 dígitos). Usado para integração com Pagar.me.';
