-- Remove event_time column
ALTER TABLE events DROP COLUMN IF EXISTS event_time;

-- Change event_date from DATE to TIMESTAMP
ALTER TABLE events ALTER COLUMN event_date TYPE TIMESTAMP USING event_date::timestamp;

-- Change registration_start_date from TIMESTAMP to DATE  
ALTER TABLE events ALTER COLUMN registration_start_date TYPE DATE USING registration_start_date::date;

-- Change registration_end_date from TIMESTAMP to DATE
ALTER TABLE events ALTER COLUMN registration_end_date TYPE DATE USING registration_end_date::date;
