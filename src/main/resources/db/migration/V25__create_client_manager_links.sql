-- ============================================================================
-- V25: Criar tabela client_manager_links
-- ============================================================================
-- Tabela N:M entre Client e ADM
-- Permite que um cliente seja gerenciado por múltiplos ADMs
-- Usado para multi-tenant (cada ADM vê apenas seus clientes)
-- ============================================================================

CREATE TABLE client_manager_links (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Relacionamentos
    client_id UUID NOT NULL,
    adm_id UUID NOT NULL,
    
    -- Metadata do vínculo
    linked_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Foreign Keys
    CONSTRAINT fk_client_manager_link_client FOREIGN KEY (client_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_client_manager_link_adm FOREIGN KEY (adm_id) REFERENCES users(id) ON DELETE CASCADE,
    
    -- Constraints
    CONSTRAINT uq_client_manager_link UNIQUE (client_id, adm_id)
);

-- Índices para queries
CREATE INDEX idx_client_link_client ON client_manager_links(client_id);
CREATE INDEX idx_client_link_adm ON client_manager_links(adm_id);

-- Comentários
COMMENT ON TABLE client_manager_links IS 'Relacionamento N:M entre Clients e ADMs para multi-tenancy';
COMMENT ON COLUMN client_manager_links.client_id IS 'FK para users com role = CLIENT';
COMMENT ON COLUMN client_manager_links.adm_id IS 'FK para users com role = ADM';
