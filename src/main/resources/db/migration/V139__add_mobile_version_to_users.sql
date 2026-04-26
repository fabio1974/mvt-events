-- Versão e plataforma do app mobile que o usuário está usando.
-- Atualizado a cada login. Útil pra suporte e métricas de adoção de release.

ALTER TABLE users
    ADD COLUMN mobile_app_version        VARCHAR(20),
    ADD COLUMN mobile_platform           VARCHAR(10),
    ADD COLUMN mobile_version_updated_at TIMESTAMPTZ;
