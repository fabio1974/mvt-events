-- V56: Tornar total_amount opcional (nullable)
-- RIDE (viagem) pode não ter valor definido na criação
ALTER TABLE deliveries ALTER COLUMN total_amount DROP NOT NULL;
