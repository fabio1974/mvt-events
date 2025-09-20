-- Extensões úteis
CREATE EXTENSION IF NOT EXISTS pgcrypto; -- gen_random_uuid()

-- Tabela simples para começar: events
CREATE TABLE IF NOT EXISTS events (
  id          uuid PRIMARY KEY,
  name        text        NOT NULL,
  starts_at   timestamptz NOT NULL,
  created_at  timestamptz NOT NULL DEFAULT now()
);