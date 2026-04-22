-- Módulo de Caixa
-- Sessão de caixa = "abertura → movimentações → fechamento" de um turno.
-- No máximo 1 sessão OPEN por client (índice parcial único).

CREATE TABLE cash_register_sessions (
    id BIGSERIAL PRIMARY KEY,
    client_id UUID NOT NULL REFERENCES users(id),
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',         -- OPEN | CLOSED
    opening_balance NUMERIC(10,2) NOT NULL DEFAULT 0,   -- fundo de caixa inicial
    closing_balance_actual NUMERIC(10,2),               -- contagem manual no fechamento
    closing_balance_expected NUMERIC(10,2),             -- esperado pelo sistema (snapshot)
    opened_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    closed_at TIMESTAMPTZ,
    opened_by UUID REFERENCES users(id),
    closed_by UUID REFERENCES users(id),
    notes TEXT,
    CONSTRAINT cash_session_status_chk CHECK (status IN ('OPEN', 'CLOSED'))
);

CREATE INDEX idx_cash_session_client ON cash_register_sessions(client_id);
CREATE INDEX idx_cash_session_opened_at ON cash_register_sessions(opened_at);

-- Garante no máximo 1 sessão OPEN por client
CREATE UNIQUE INDEX uniq_cash_session_open_per_client
    ON cash_register_sessions(client_id)
    WHERE status = 'OPEN';

-- Movimentações dentro da sessão
CREATE TABLE cash_register_movements (
    id BIGSERIAL PRIMARY KEY,
    session_id BIGINT NOT NULL REFERENCES cash_register_sessions(id) ON DELETE CASCADE,
    type VARCHAR(20) NOT NULL,                          -- ADDITION | WITHDRAWAL | SANGRIA
    amount NUMERIC(10,2) NOT NULL,
    reason TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID REFERENCES users(id),
    CONSTRAINT cash_movement_type_chk CHECK (type IN ('ADDITION', 'WITHDRAWAL', 'SANGRIA')),
    CONSTRAINT cash_movement_amount_chk CHECK (amount > 0)
);

CREATE INDEX idx_cash_movement_session ON cash_register_movements(session_id);
