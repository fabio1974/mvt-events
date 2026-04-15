-- Cria tabela waiter_contracts (simétrica a employment_contracts)
CREATE TABLE IF NOT EXISTS waiter_contracts (
    id BIGSERIAL PRIMARY KEY,
    waiter_id UUID NOT NULL REFERENCES users(id),
    organization_id BIGINT NOT NULL REFERENCES organizations(id),
    linked_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(waiter_id, organization_id)
);

-- Migrar dados de client_waiters para waiter_contracts (se houver)
-- O client_waiter ligava WAITER→CLIENT, agora precisa ligar WAITER→ORGANIZATION
-- Encontra a organization do CLIENT via client_contracts
INSERT INTO waiter_contracts (waiter_id, organization_id, linked_at, is_active)
SELECT DISTINCT cw.waiter_id, cc.organization_id, cw.created_at, cw.active
FROM client_waiters cw
JOIN client_contracts cc ON cc.client_id = cw.client_id AND cc.status = 'ACTIVE'
ON CONFLICT (waiter_id, organization_id) DO NOTHING;

-- Dropar tabela client_waiters (obsoleta)
DROP TABLE IF EXISTS client_waiters;
