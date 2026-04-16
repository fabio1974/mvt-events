-- V114: Add independent status to restaurant_tables
ALTER TABLE restaurant_tables ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE';
