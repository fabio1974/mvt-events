-- Data em que o módulo de mesas foi ativado
ALTER TABLE store_profiles ADD COLUMN table_orders_enabled_at TIMESTAMP WITH TIME ZONE;

-- Preencher retroativamente para quem já tem o módulo ativado
UPDATE store_profiles SET table_orders_enabled_at = created_at WHERE table_orders_enabled = true;
