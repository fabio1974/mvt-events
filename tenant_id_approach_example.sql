-- ============================================================================
-- ABORDAGEM SEM TABELA TENANTS (usando tenant_id das tabelas existentes)
-- ============================================================================
-- ATENÇÃO: Esta abordagem tem limitações de segurança e controle

-- Função para obter tenants existentes (baseado em events como "tabela principal")
CREATE OR REPLACE FUNCTION get_existing_tenants()
RETURNS TABLE(tenant_id UUID) AS $$
BEGIN
    RETURN QUERY
    SELECT DISTINCT e.tenant_id 
    FROM events e 
    WHERE e.tenant_id IS NOT NULL;
END;
$$ LANGUAGE plpgsql;

-- Função para validar tenant (usando events como referência)
CREATE OR REPLACE FUNCTION set_current_tenant_from_events(tenant_uuid UUID)
RETURNS VOID AS $$
BEGIN
    -- Validar se tenant existe em alguma tabela principal
    IF NOT EXISTS (
        SELECT 1 FROM events WHERE tenant_id = tenant_uuid
        UNION
        SELECT 1 FROM organizations WHERE tenant_id = tenant_uuid
    ) THEN
        RAISE EXCEPTION 'Tenant % não encontrado em nenhuma tabela', tenant_uuid;
    END IF;
    
    PERFORM set_config('app.current_tenant_id', tenant_uuid::text, false);
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- RLS continua igual
ALTER TABLE events ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_events ON events
    FOR ALL
    USING (tenant_id = current_setting('app.current_tenant_id')::UUID)
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id')::UUID);

-- Problemas desta abordagem:
-- 1. Como criar o primeiro evento se não existe tenant?
-- 2. Como garantir que tenant_id é único/válido?
-- 3. Como desativar um tenant?
-- 4. Como ter metadados do tenant (nome, email, limites)?