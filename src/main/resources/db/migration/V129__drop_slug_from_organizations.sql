-- V128: Remove o campo `slug` de organizations.
-- Motivo: campo não usado em produto; causava DataIntegrityViolationException
-- na criação automática de Organization para ORGANIZER (constraint NOT NULL
-- nunca era satisfeita pois o código não setava valor).

ALTER TABLE organizations DROP COLUMN IF EXISTS slug;
