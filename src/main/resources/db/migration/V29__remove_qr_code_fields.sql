-- V29: Remove campos qr_code e qr_code_url (manter apenas pix_qr_code e pix_qr_code_url)
-- Autor: Sistema
-- Data: 2025-12-28
-- Descrição: Remove campos duplicados qr_code e qr_code_url da tabela payments,
--            mantendo apenas os campos pix_qr_code e pix_qr_code_url que são usados pelas APIs

-- Drop colunas qr_code e qr_code_url
ALTER TABLE payments DROP COLUMN IF EXISTS qr_code;
ALTER TABLE payments DROP COLUMN IF EXISTS qr_code_url;
