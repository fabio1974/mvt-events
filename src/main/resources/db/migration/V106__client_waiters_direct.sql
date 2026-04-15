-- Refatoração: vínculo WAITER↔CLIENT direto (N:N)
-- Remove waiter_contracts (via Organization) e recria client_waiters

-- 1. Recriar client_waiters (relação direta CLIENT↔WAITER)
CREATE TABLE IF NOT EXISTS client_waiters (
    id BIGSERIAL PRIMARY KEY,
    client_id UUID NOT NULL REFERENCES users(id),
    waiter_id UUID NOT NULL REFERENCES users(id),
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(client_id, waiter_id)
);
CREATE INDEX IF NOT EXISTS idx_client_waiters_client ON client_waiters(client_id);
CREATE INDEX IF NOT EXISTS idx_client_waiters_waiter ON client_waiters(waiter_id);

-- 2. Migrar dados: waiter_contracts → client_waiters
--    Para cada waiter_contract, vincula o waiter a todos os CLIENTs da org
INSERT INTO client_waiters (client_id, waiter_id, active, created_at)
SELECT DISTINCT cc.client_id, wc.waiter_id, wc.is_active, wc.created_at
FROM waiter_contracts wc
JOIN client_contracts cc ON cc.organization_id = wc.organization_id AND cc.status = 'ACTIVE'
ON CONFLICT (client_id, waiter_id) DO NOTHING;

-- 3. Dropar waiter_contracts (não mais necessário)
DROP TABLE IF EXISTS waiter_contracts;
