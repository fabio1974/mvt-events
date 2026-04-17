-- Assinaturas de serviços recorrentes dos clientes
CREATE TABLE client_subscriptions (
    id              BIGSERIAL PRIMARY KEY,
    client_id       UUID           NOT NULL REFERENCES users(id),
    service_id      BIGINT         NOT NULL REFERENCES platform_services(id),
    monthly_price   DECIMAL(10,2)  NOT NULL,
    billing_due_day INTEGER        NOT NULL CHECK (billing_due_day IN (1, 5, 10, 15, 20, 25)),
    started_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    cancelled_at    TIMESTAMP WITH TIME ZONE,
    active          BOOLEAN        NOT NULL DEFAULT true,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

-- Apenas uma assinatura ativa por serviço por cliente
CREATE UNIQUE INDEX uq_client_subscription_active
    ON client_subscriptions (client_id, service_id) WHERE active = true;
