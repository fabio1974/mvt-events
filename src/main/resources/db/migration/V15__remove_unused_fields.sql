-- V15: Remove unused fields from events and event_categories tables
-- Author: System
-- Date: 2025-10-09

-- Remove startsAt and bannerUrl from events table
ALTER TABLE events DROP COLUMN IF EXISTS starts_at;
ALTER TABLE events DROP COLUMN IF EXISTS banner_url;

-- Remove isActive from event_categories table
ALTER TABLE event_categories DROP COLUMN IF EXISTS is_active;
