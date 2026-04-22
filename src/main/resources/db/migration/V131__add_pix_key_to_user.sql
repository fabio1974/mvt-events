-- Chave PIX do usuário (motoboy principalmente, mas pode ser qualquer role).
-- Usado pelo PixOutProvider para settlement de PagarmeTransfer PENDING.

ALTER TABLE users
    ADD COLUMN pix_key VARCHAR(120),
    ADD COLUMN pix_key_type VARCHAR(10);

-- Tipo: CPF | CNPJ | EMAIL | PHONE | EVP (chave aleatória)
ALTER TABLE users
    ADD CONSTRAINT user_pix_key_type_chk
        CHECK (pix_key_type IS NULL OR pix_key_type IN ('CPF', 'CNPJ', 'EMAIL', 'PHONE', 'EVP'));

CREATE INDEX idx_users_pix_key ON users(pix_key) WHERE pix_key IS NOT NULL;
