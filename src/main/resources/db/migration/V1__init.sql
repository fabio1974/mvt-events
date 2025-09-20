-- Extensões úteis
CREATE EXTENSION IF NOT EXISTS pgcrypto; -- Para criptografia se necessário

-- Tabela simples para começar: events
CREATE TABLE IF NOT EXISTS events (
  id          BIGSERIAL PRIMARY KEY,
  name        text        NOT NULL,
  starts_at   timestamptz NOT NULL,
  created_at  timestamptz NOT NULL DEFAULT now()
);