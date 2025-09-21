-- ============================================================================
-- MIGRAÇÃO V2: ADIÇÃO DE SUPORTE MULTI-TENANCY BASEADO EM EVENTS
-- ============================================================================

-- ============================================================================
-- FUNÇÃO PARA DEFINIR EVENT NO CONTEXTO DA SESSÃO
-- ============================================================================
DROP FUNCTION IF EXISTS set_current_event(UUID);
DROP FUNCTION IF EXISTS set_current_event(BIGINT);

CREATE OR REPLACE FUNCTION set_current_event(event_bigint BIGINT)
RETURNS VOID AS $$
BEGIN
    -- Validar se o evento existe e está ativo
    IF NOT EXISTS (SELECT 1 FROM events WHERE id = event_bigint) THEN
        RAISE EXCEPTION 'Event % não encontrado', event_bigint;
    END IF;
    
    -- Definir o evento no contexto da sessão
    PERFORM set_config('app.current_event_id', event_bigint::text, false);
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- ============================================================================
-- FUNÇÃO PARA OBTER EVENT TENANT ATUAL
-- ============================================================================
DROP FUNCTION IF EXISTS get_current_event_tenant_id();

CREATE OR REPLACE FUNCTION get_current_event_tenant_id()
RETURNS BIGINT AS $$
DECLARE
    event_id BIGINT;
BEGIN
    -- Tentar obter o event_id do contexto da sessão
    BEGIN
        event_id := current_setting('app.current_event_id')::BIGINT;
    EXCEPTION WHEN OTHERS THEN
        -- Se não houver event_id definido, retornar NULL
        -- Isso forçará que a aplicação sempre defina o evento
        RETURN NULL;
    END;
    
    RETURN event_id;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- ============================================================================
-- FUNÇÃO PARA LIMPAR CONTEXTO DO EVENT
-- ============================================================================
DROP FUNCTION IF EXISTS clear_current_event();

CREATE OR REPLACE FUNCTION clear_current_event()
RETURNS VOID AS $$
BEGIN
    -- Limpar o contexto do event
    PERFORM set_config('app.current_event_id', '', false);
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- ============================================================================
-- MIGRAÇÃO V2: ADIÇÃO DE SUPORTE MULTI-TENANCY BASEADO EM EVENTS
-- ============================================================================
-- Arquitetura: Events são os tenants, Organizations são globais

-- ============================================================================
-- ADIÇÃO DE TENANT_ID NAS TABELAS (EXCETO ORGANIZATIONS E EVENTS)
-- ============================================================================
-- ============================================================================
-- EVENTS são os próprios tenants (não precisam de tenant_id)
-- ORGANIZATIONS são entidades globais (não precisam de tenant_id)

-- Adicionar tenant_id nas tabelas que devem ser isoladas por evento
ALTER TABLE athletes ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
ALTER TABLE registrations ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
ALTER TABLE payments ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
ALTER TABLE transfers ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
ALTER TABLE payment_events ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
ALTER TABLE event_financials ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
ALTER TABLE users ADD COLUMN IF NOT EXISTS tenant_id BIGINT;

-- ============================================================================
-- MIGRAÇÃO DE DADOS EXISTENTES
-- ============================================================================
DO $$
BEGIN
    -- Atualizar registros existentes com tenant_id baseado no event_id
    
    -- Athletes: usar o primeiro evento como tenant padrão se não tiver registrations
    UPDATE athletes SET tenant_id = (
        SELECT COALESCE(
            (SELECT r.event_id FROM registrations r WHERE r.athlete_id = athletes.id LIMIT 1),
            (SELECT id FROM events LIMIT 1)
        )
    ) WHERE tenant_id IS NULL;
    
    -- Registrations: tenant_id = event_id
    UPDATE registrations SET tenant_id = event_id WHERE tenant_id IS NULL;
    
    -- Payments: tenant_id baseado no event_id da registration
    UPDATE payments SET tenant_id = (
        SELECT r.event_id FROM registrations r WHERE r.id = payments.registration_id
    ) WHERE tenant_id IS NULL;
    
    -- Transfers: tenant_id = event_id
    UPDATE transfers SET tenant_id = event_id WHERE tenant_id IS NULL;
    
    -- Payment Events: tenant_id = event_id
    UPDATE payment_events SET tenant_id = event_id WHERE tenant_id IS NULL;
    
    -- Event Financials: tenant_id = event_id
    UPDATE event_financials SET tenant_id = event_id WHERE tenant_id IS NULL;
    
    -- Users: usar o primeiro evento como tenant padrão
    UPDATE users SET tenant_id = (
        SELECT id FROM events LIMIT 1
    ) WHERE tenant_id IS NULL;
END
$$;

-- ============================================================================
-- TORNAR TENANT_ID OBRIGATÓRIO (EXCETO ORGANIZATIONS E EVENTS)
-- ============================================================================
ALTER TABLE athletes ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE registrations ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE payments ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE transfers ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE payment_events ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE event_financials ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE users ALTER COLUMN tenant_id SET NOT NULL;

-- ============================================================================
-- CHAVES ESTRANGEIRAS PARA EVENTS (COMO TENANTS)
-- ============================================================================
DO $$
BEGIN
    -- FK para athletes (tenant_id aponta para events.id)
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_athletes_event_tenant') THEN
        ALTER TABLE athletes ADD CONSTRAINT fk_athletes_event_tenant 
            FOREIGN KEY (tenant_id) REFERENCES events(id) ON DELETE CASCADE;
    END IF;
    
    -- FK para registrations (tenant_id aponta para events.id)
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_registrations_event_tenant') THEN
        ALTER TABLE registrations ADD CONSTRAINT fk_registrations_event_tenant 
            FOREIGN KEY (tenant_id) REFERENCES events(id) ON DELETE CASCADE;
    END IF;
    
    -- FK para payments (tenant_id aponta para events.id)
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_payments_event_tenant') THEN
        ALTER TABLE payments ADD CONSTRAINT fk_payments_event_tenant 
            FOREIGN KEY (tenant_id) REFERENCES events(id) ON DELETE CASCADE;
    END IF;
    
    -- FK para transfers (tenant_id aponta para events.id)
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_transfers_event_tenant') THEN
        ALTER TABLE transfers ADD CONSTRAINT fk_transfers_event_tenant 
            FOREIGN KEY (tenant_id) REFERENCES events(id) ON DELETE CASCADE;
    END IF;
    
    -- FK para payment_events (tenant_id aponta para events.id)
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_payment_events_event_tenant') THEN
        ALTER TABLE payment_events ADD CONSTRAINT fk_payment_events_event_tenant 
            FOREIGN KEY (tenant_id) REFERENCES events(id) ON DELETE CASCADE;
    END IF;
    
    -- FK para event_financials (tenant_id aponta para events.id)
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_event_financials_event_tenant') THEN
        ALTER TABLE event_financials ADD CONSTRAINT fk_event_financials_event_tenant 
            FOREIGN KEY (tenant_id) REFERENCES events(id) ON DELETE CASCADE;
    END IF;
    
    -- FK para users (tenant_id aponta para events.id)
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_users_event_tenant') THEN
        ALTER TABLE users ADD CONSTRAINT fk_users_event_tenant 
            FOREIGN KEY (tenant_id) REFERENCES events(id) ON DELETE CASCADE;
    END IF;
END
$$;

-- ============================================================================
-- ÍNDICES COMPOSTOS PARA PERFORMANCE MULTI-TENANT (BASEADO EM EVENTS)
-- ============================================================================

-- Organizations permanecem globais (sem tenant_id)
-- Events são os próprios tenants (sem tenant_id)

-- Índices para athletes
CREATE INDEX IF NOT EXISTS idx_athletes_tenant_id ON athletes(tenant_id);
CREATE INDEX IF NOT EXISTS idx_athletes_tenant_email ON athletes(tenant_id, email);
CREATE INDEX IF NOT EXISTS idx_athletes_tenant_document ON athletes(tenant_id, document_number);

-- Índices para registrations
CREATE INDEX IF NOT EXISTS idx_registrations_tenant_id ON registrations(tenant_id);
CREATE INDEX IF NOT EXISTS idx_registrations_tenant_event ON registrations(tenant_id, event_id);
CREATE INDEX IF NOT EXISTS idx_registrations_tenant_athlete ON registrations(tenant_id, athlete_id);
CREATE INDEX IF NOT EXISTS idx_registrations_tenant_status ON registrations(tenant_id, status);

-- Índices para payments
CREATE INDEX IF NOT EXISTS idx_payments_tenant_id ON payments(tenant_id);
CREATE INDEX IF NOT EXISTS idx_payments_tenant_registration ON payments(tenant_id, registration_id);
CREATE INDEX IF NOT EXISTS idx_payments_tenant_status ON payments(tenant_id, payment_status);

-- Índices para transfers
CREATE INDEX IF NOT EXISTS idx_transfers_tenant_id ON transfers(tenant_id);
CREATE INDEX IF NOT EXISTS idx_transfers_tenant_event ON transfers(tenant_id, event_id);
CREATE INDEX IF NOT EXISTS idx_transfers_tenant_organization ON transfers(tenant_id, organization_id);
CREATE INDEX IF NOT EXISTS idx_transfers_tenant_status ON transfers(tenant_id, status);

-- Índices para payment_events
CREATE INDEX IF NOT EXISTS idx_payment_events_tenant_id ON payment_events(tenant_id);
CREATE INDEX IF NOT EXISTS idx_payment_events_tenant_event ON payment_events(tenant_id, event_id);
CREATE INDEX IF NOT EXISTS idx_payment_events_tenant_payment ON payment_events(tenant_id, payment_id);

-- Índices para event_financials
CREATE INDEX IF NOT EXISTS idx_event_financials_tenant_id ON event_financials(tenant_id);
CREATE INDEX IF NOT EXISTS idx_event_financials_tenant_event ON event_financials(tenant_id, event_id);

-- Índices para users
CREATE INDEX IF NOT EXISTS idx_users_tenant_id ON users(tenant_id);
CREATE INDEX IF NOT EXISTS idx_users_tenant_username ON users(tenant_id, username);
CREATE INDEX IF NOT EXISTS idx_users_tenant_email ON users(tenant_id, email);

-- ============================================================================
-- CONSTRAINTS ÚNICAS AJUSTADAS PARA MULTI-TENANCY BASEADO EM EVENTS
-- ============================================================================
DO $$
BEGIN
    -- Organizations permanecem globais (constraints originais mantidos)
    -- Events são os próprios tenants (constraints originais mantidos)
    
    -- Athletes: email e document únicos por tenant (evento)
    ALTER TABLE athletes DROP CONSTRAINT IF EXISTS ukn0i3de0t8dyv19qn45vgyi322;
    ALTER TABLE athletes DROP CONSTRAINT IF EXISTS ukek0v4gdbthm9d0pgs4itg21j0;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_athletes_tenant_email') THEN
        ALTER TABLE athletes ADD CONSTRAINT uk_athletes_tenant_email UNIQUE (tenant_id, email);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_athletes_tenant_document') THEN
        ALTER TABLE athletes ADD CONSTRAINT uk_athletes_tenant_document UNIQUE (tenant_id, document_number);
    END IF;
    
    -- Registrations: uma inscrição por atleta por evento por tenant (redundante mas consistente)
    ALTER TABLE registrations DROP CONSTRAINT IF EXISTS uk18v3flm45yg9d3o380mne8cof;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_registrations_tenant_event_athlete') THEN
        ALTER TABLE registrations ADD CONSTRAINT uk_registrations_tenant_event_athlete UNIQUE (tenant_id, event_id, athlete_id);
    END IF;
    
    -- Event Financials: um registro financeiro por evento por tenant (redundante mas consistente)
    -- Event Financials: um registro financeiro por evento por tenant
    ALTER TABLE event_financials DROP CONSTRAINT IF EXISTS uk4adkilqs0mjxvf2ypbwalyvc;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_event_financials_tenant_event') THEN
        ALTER TABLE event_financials ADD CONSTRAINT uk_event_financials_tenant_event UNIQUE (tenant_id, event_id);
    END IF;
    
    -- Users: username único por tenant (evento)
    ALTER TABLE users DROP CONSTRAINT IF EXISTS ukr43af9ap4edm43mmtq01oddj6;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_users_tenant_username') THEN
        ALTER TABLE users ADD CONSTRAINT uk_users_tenant_username UNIQUE (tenant_id, username);
    END IF;
END
$$;

-- ============================================================================
-- POLÍTICAS DE SEGURANÇA (RLS - Row Level Security)
-- ============================================================================

-- Habilitar RLS apenas nas tabelas com tenant_id (não em organizations/events)
ALTER TABLE athletes ENABLE ROW LEVEL SECURITY;
ALTER TABLE registrations ENABLE ROW LEVEL SECURITY;
ALTER TABLE payments ENABLE ROW LEVEL SECURITY;
ALTER TABLE transfers ENABLE ROW LEVEL SECURITY;
ALTER TABLE payment_events ENABLE ROW LEVEL SECURITY;
ALTER TABLE event_financials ENABLE ROW LEVEL SECURITY;
ALTER TABLE users ENABLE ROW LEVEL SECURITY;

-- ============================================================================
-- POLÍTICAS RLS PARA ISOLAMENTO POR EVENTO
-- ============================================================================

-- Política para athletes
DROP POLICY IF EXISTS event_isolation_athletes ON athletes;
CREATE POLICY event_isolation_athletes ON athletes
    FOR ALL
    USING (tenant_id = get_current_event_tenant_id())
    WITH CHECK (tenant_id = get_current_event_tenant_id());

-- Política para registrations
DROP POLICY IF EXISTS event_isolation_registrations ON registrations;
CREATE POLICY event_isolation_registrations ON registrations
    FOR ALL
    USING (tenant_id = get_current_event_tenant_id())
    WITH CHECK (tenant_id = get_current_event_tenant_id());

-- Política para payments
DROP POLICY IF EXISTS event_isolation_payments ON payments;
CREATE POLICY event_isolation_payments ON payments
    FOR ALL
    USING (tenant_id = get_current_event_tenant_id())
    WITH CHECK (tenant_id = get_current_event_tenant_id());

-- Política para transfers
DROP POLICY IF EXISTS event_isolation_transfers ON transfers;
CREATE POLICY event_isolation_transfers ON transfers
    FOR ALL
    USING (tenant_id = get_current_event_tenant_id())
    WITH CHECK (tenant_id = get_current_event_tenant_id());

-- Política para payment_events
DROP POLICY IF EXISTS event_isolation_payment_events ON payment_events;
CREATE POLICY event_isolation_payment_events ON payment_events
    FOR ALL
    USING (tenant_id = get_current_event_tenant_id())
    WITH CHECK (tenant_id = get_current_event_tenant_id());

-- Política para event_financials
DROP POLICY IF EXISTS event_isolation_event_financials ON event_financials;
CREATE POLICY event_isolation_event_financials ON event_financials
    FOR ALL
    USING (tenant_id = get_current_event_tenant_id())
    WITH CHECK (tenant_id = get_current_event_tenant_id());

-- Política para users
DROP POLICY IF EXISTS event_isolation_users ON users;
CREATE POLICY event_isolation_users ON users
    FOR ALL
    USING (tenant_id = get_current_event_tenant_id())
    WITH CHECK (tenant_id = get_current_event_tenant_id());

/*
============================================================================
RESUMO DA ARQUITETURA MULTI-TENANT BASEADA EM EVENTS
============================================================================

1. ISOLATION SCOPE:
   - TENANT: Events (cada evento é um tenant independente)
   - GLOBAL: Organizations (compartilhadas entre todos os eventos)

2. TABELAS ISOLADAS POR TENANT:
   - athletes (tenant_id -> events.id)
   - registrations (tenant_id -> events.id)
   - payments (tenant_id -> events.id)
   - transfers (tenant_id -> events.id)  
   - payment_events (tenant_id -> events.id)
   - event_financials (tenant_id -> events.id)
   - users (tenant_id -> events.id)

3. TABELAS GLOBAIS (SEM ISOLAMENTO):
   - organizations (podem ser acessadas por qualquer evento)
   - events (são os próprios tenants)

4. SEGURANÇA IMPLEMENTADA:
   - Row Level Security (RLS) ativo em todas as tabelas isoladas
   - Políticas RLS baseadas na função get_current_event_tenant_id()
   - Validação automática em INSERT/UPDATE/DELETE/SELECT

5. FUNÇÕES DE CONTROLE:
   - set_current_event(event_id): Define contexto do evento na sessão
   - get_current_event_tenant_id(): Retorna ID do evento atual da sessão
   - clear_current_event(): Limpa contexto da sessão

6. CONSTRAINTS E ÍNDICES:
   - Unicidade baseada em (tenant_id, campo) para isolation adequado
   - Índices compostos para performance multi-tenant
   - Foreign keys garantindo integridade referencial

7. MIGRATION STRATEGY:
   - Dados existentes migrados automaticamente
   - tenant_id populado baseado em event_id das relationships existentes
   - Backup automático de constraints originais

IMPORTANTE: 
- Sempre definir o evento no início de cada request
- Limpar o contexto no final do request
- Organizations são globais e acessíveis por todos os eventos
- Testar isolamento durante desenvolvimento
*/