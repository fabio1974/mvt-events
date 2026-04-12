-- V95: Zapi-Food — Perfil da loja (1:1 com users)
-- Apenas CLIENTs com service_type RESTAURANT/PHARMACY/MARKET/LODGING

CREATE TABLE store_profiles (
    id BIGSERIAL PRIMARY KEY,
    user_id UUID NOT NULL UNIQUE REFERENCES users(id),
    is_open BOOLEAN NOT NULL DEFAULT false,
    opening_hours JSONB,
    min_order NUMERIC(10,2),
    avg_preparation_minutes INT,
    logo_url VARCHAR(500),
    cover_url VARCHAR(500),
    description TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_store_profiles_user ON store_profiles(user_id);
CREATE INDEX idx_store_profiles_open ON store_profiles(is_open) WHERE is_open = true;

COMMENT ON TABLE store_profiles IS 'Perfil da loja — módulo Zapi-Food. Relação 1:1 com users (apenas CLIENTs com catálogo).';
