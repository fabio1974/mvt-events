-- V94: Adiciona token de rastreamento público para compartilhamento via WhatsApp
ALTER TABLE deliveries ADD COLUMN tracking_token UUID;
ALTER TABLE deliveries ADD COLUMN tracking_token_expires_at TIMESTAMPTZ;

CREATE UNIQUE INDEX idx_deliveries_tracking_token ON deliveries(tracking_token) WHERE tracking_token IS NOT NULL;

COMMENT ON COLUMN deliveries.tracking_token IS 'UUID para rastreamento público (sem autenticação). Gerado ao criar a delivery.';
COMMENT ON COLUMN deliveries.tracking_token_expires_at IS 'Expiração do token de rastreamento (48h após criação).';
