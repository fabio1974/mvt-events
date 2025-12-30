-- Remove contract_date column from client_contracts table
ALTER TABLE client_contracts DROP COLUMN IF EXISTS contract_date;
