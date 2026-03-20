-- Enable PostGIS extension for spatial/geographic data types
-- Required for storing delivery routes as LineString geometries
CREATE EXTENSION IF NOT EXISTS postgis;
