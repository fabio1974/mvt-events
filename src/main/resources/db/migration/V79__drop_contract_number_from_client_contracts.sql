-- Remove a coluna contract_number da tabela client_contracts
ALTER TABLE client_contracts DROP COLUMN IF EXISTS contract_number;
