-- Cria tabela de configurações do site
CREATE TABLE site_configurations (
    id BIGSERIAL PRIMARY KEY,
    price_per_km NUMERIC(10, 2) NOT NULL CHECK (price_per_km > 0),
    organizer_percentage NUMERIC(5, 2) NOT NULL CHECK (organizer_percentage >= 0 AND organizer_percentage <= 100),
    platform_percentage NUMERIC(5, 2) NOT NULL CHECK (platform_percentage >= 0 AND platform_percentage <= 100),
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    updated_by VARCHAR(255),
    notes TEXT
);

-- Criar índice único para garantir apenas uma configuração ativa por vez
CREATE UNIQUE INDEX idx_site_configurations_single_active 
ON site_configurations(is_active) 
WHERE is_active = true;

-- Inserir configuração padrão
INSERT INTO site_configurations (
    price_per_km,
    organizer_percentage,
    platform_percentage,
    is_active,
    created_at,
    updated_at,
    updated_by,
    notes
) VALUES (
    1.00,           -- R$ 1,00 por km
    5.00,           -- 5% para o gerente
    10.00,          -- 10% para a plataforma
    true,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    'SYSTEM',
    'Configuração inicial padrão do sistema'
);

-- Adicionar comentários
COMMENT ON TABLE site_configurations IS 'Configurações globais do site/plataforma (histórico de mudanças)';
COMMENT ON COLUMN site_configurations.price_per_km IS 'Preço por km para cálculo do frete (em Reais)';
COMMENT ON COLUMN site_configurations.organizer_percentage IS 'Percentual de comissão para o gerente/organizador';
COMMENT ON COLUMN site_configurations.platform_percentage IS 'Percentual de comissão para a plataforma';
COMMENT ON COLUMN site_configurations.is_active IS 'Indica se esta é a configuração ativa (apenas uma por vez - garantido por índice único)';
