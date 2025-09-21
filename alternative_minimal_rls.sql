-- ============================================================================
-- ALTERNATIVA MINIMALISTA: RLS SEM TABELA TENANTS COMPLEXA
-- ============================================================================
-- ATENÇÃO: Esta é uma versão simplificada apenas para demonstração
-- Para produção, recomendamos a versão completa com tabela tenants

-- Tabela minimalista apenas para validação
CREATE TABLE IF NOT EXISTS tenant_registry (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP
);

-- Função simples para validar tenant
CREATE OR REPLACE FUNCTION set_current_tenant_simple(tenant_uuid UUID)
RETURNS VOID AS $$
BEGIN
    -- Validação mínima
    IF NOT EXISTS (SELECT 1 FROM tenant_registry WHERE id = tenant_uuid AND active = true) THEN
        RAISE EXCEPTION 'Tenant % inválido', tenant_uuid;
    END IF;
    
    PERFORM set_config('app.current_tenant_id', tenant_uuid::text, false);
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- RLS nas tabelas principais (mesmo código)
ALTER TABLE organizations ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_organizations ON organizations
    FOR ALL
    USING (tenant_id = current_setting('app.current_tenant_id')::UUID);

-- E assim por diante...