-- ============================================================================
-- V24: Criar tabela courier_adm_links
-- ============================================================================
-- Tabela N:M entre Courier e ADM
-- Permite que um courier trabalhe com múltiplos ADMs (regiões)
-- ============================================================================

CREATE TABLE courier_adm_links (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Relacionamentos
    courier_id UUID NOT NULL,
    adm_id UUID NOT NULL,
    
    -- Metadata do vínculo
    linked_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_primary BOOLEAN NOT NULL DEFAULT false,
    is_active BOOLEAN NOT NULL DEFAULT true,
    
    -- Foreign Keys
    CONSTRAINT fk_courier_adm_link_courier FOREIGN KEY (courier_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_courier_adm_link_adm FOREIGN KEY (adm_id) REFERENCES users(id) ON DELETE CASCADE,
    
    -- Constraints
    CONSTRAINT uq_courier_adm_link UNIQUE (courier_id, adm_id)
);

-- Índice único para garantir apenas 1 ADM principal ativo por courier
CREATE UNIQUE INDEX idx_courier_primary_adm 
ON courier_adm_links(courier_id) 
WHERE is_primary = true AND is_active = true;

-- Índices para queries
CREATE INDEX idx_courier_link_courier ON courier_adm_links(courier_id);
CREATE INDEX idx_courier_link_adm ON courier_adm_links(adm_id);
CREATE INDEX idx_courier_link_primary ON courier_adm_links(is_primary) WHERE is_primary = true;
CREATE INDEX idx_courier_link_active ON courier_adm_links(is_active);

-- Comentários
COMMENT ON TABLE courier_adm_links IS 'Relacionamento N:M entre Couriers e ADMs';
COMMENT ON COLUMN courier_adm_links.is_primary IS 'ADM principal responsável pelo courier (apenas 1 ativo por courier)';
COMMENT ON COLUMN courier_adm_links.is_active IS 'Link ativo (courier pode desativar associação temporariamente)';
