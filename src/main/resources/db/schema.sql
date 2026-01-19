-- PostgreSQL Schema for Zones
-- This file contains the DDL statements to create the required tables

-- ============================================
-- ZONES TABLE
-- ============================================
CREATE TABLE IF NOT EXISTS zones (
    id VARCHAR(36) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    type VARCHAR(100),
    description TEXT,
    status VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    heights JSONB,
    geohashes JSONB,
    coordinates JSONB,
    radius double precision,
    center JSONB,
    PRIMARY KEY (id, file_name),
    CONSTRAINT zones_file_name_id_unique UNIQUE (file_name, id)
);

-- Create indexes for better query performance
CREATE INDEX IF NOT EXISTS idx_zones_file_name ON zones(file_name);
CREATE INDEX IF NOT EXISTS idx_zones_type ON zones(type);
CREATE INDEX IF NOT EXISTS idx_zones_status ON zones(status);
CREATE INDEX IF NOT EXISTS idx_zones_created_at ON zones(created_at);
CREATE INDEX IF NOT EXISTS idx_zones_radius ON zones(radius);

-- GIN indexes for JSONB columns to enable efficient JSON queries
CREATE INDEX IF NOT EXISTS idx_zones_heights_gin ON zones USING GIN (heights);
CREATE INDEX IF NOT EXISTS idx_zones_geohashes_gin ON zones USING GIN (geohashes);
CREATE INDEX IF NOT EXISTS idx_zones_coordinates_gin ON zones USING GIN (coordinates);
CREATE INDEX IF NOT EXISTS idx_zones_center_gin ON zones USING GIN (center);

-- ============================================
-- DRONE TELEMETRY TABLE
-- ============================================
CREATE TABLE IF NOT EXISTS drone_telemetry (
    "time" timestamp without time zone NOT NULL,
    drone_id VARCHAR(50) NOT NULL,
    host VARCHAR(255),
    id VARCHAR(36) NOT NULL DEFAULT gen_random_uuid()::VARCHAR,
    topic VARCHAR(255),
    alt double precision,
    nr5g_band VARCHAR(50),
    lte_band VARCHAR(50),
    lte_dlbw_mhz double precision,
    nr5g_earfcn double precision,
    lte_earfcn double precision,
    gps_fix boolean,
    lat double precision,
    lon double precision,
    lte_mccmnc VARCHAR(50),
    lte_op_mode VARCHAR(50),
    nr5g_pcell_id VARCHAR(50),
    lte_pcell_id VARCHAR(50),
    nr5g_rat VARCHAR(50),
    lte_rat VARCHAR(50),
    nr5g_rsrp_dbm double precision,
    lte_rsrp_dbm double precision,
    nr5g_rsrq_db double precision,
    lte_rsrq_db double precision,
    lte_rssi_dbm double precision,
    nr5g_rssnr_db double precision,
    lte_rssnr_db double precision,
    satellites double precision,
    lte_scell_id VARCHAR(50),
    session_id VARCHAR(100) NOT NULL,
    speed_kmh double precision,
    lte_tac VARCHAR(50),
    lte_ulbw_mhz double precision,
    PRIMARY KEY (id),
    CONSTRAINT drone_telemetry_id_unique UNIQUE (id)
);

-- Create indexes for better query performance
CREATE INDEX IF NOT EXISTS idx_drone_telemetry_drone_id ON drone_telemetry(drone_id);
CREATE INDEX IF NOT EXISTS idx_drone_telemetry_time ON drone_telemetry("time");
CREATE INDEX IF NOT EXISTS idx_drone_telemetry_location ON drone_telemetry(lat, lon);
CREATE INDEX IF NOT EXISTS idx_drone_telemetry_session_id ON drone_telemetry(session_id);
CREATE UNIQUE INDEX IF NOT EXISTS idx_drone_telemetry_id ON drone_telemetry(id);

-- ============================================
-- DUPLICATE PREVENTION FOR DRONE TELEMETRY
-- ============================================
-- Unique constraint to prevent duplicate messages based on message characteristics
-- The combination of drone_id, time, session_id, lat, lon, and alt should uniquely identify a telemetry reading
CREATE UNIQUE INDEX IF NOT EXISTS idx_drone_telemetry_dedupe 
ON drone_telemetry(drone_id, "time", session_id, 
    COALESCE(lat::text, 'NULL'), COALESCE(lon::text, 'NULL'), COALESCE(alt::text, 'NULL'));

-- Trigger function to silently ignore duplicate inserts
-- This prevents Telegraf from seeing constraint violation errors
CREATE OR REPLACE FUNCTION prevent_duplicate_drone_telemetry()
RETURNS TRIGGER AS $$
BEGIN
    -- Check if a duplicate record already exists
    IF EXISTS (
        SELECT 1 FROM drone_telemetry 
        WHERE drone_id = NEW.drone_id 
        AND "time" = NEW."time"
        AND session_id = NEW.session_id
        AND (
            (lat IS NOT NULL AND lon IS NOT NULL AND NEW.lat IS NOT NULL AND NEW.lon IS NOT NULL 
             AND lat = NEW.lat AND lon = NEW.lon AND COALESCE(alt, 0) = COALESCE(NEW.alt, 0))
            OR
            (lat IS NULL AND lon IS NULL AND NEW.lat IS NULL AND NEW.lon IS NULL)
        )
    ) THEN
        -- Duplicate found, skip the insert
        RETURN NULL;
    END IF;
    -- Not a duplicate, proceed with insert
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Create the trigger
DROP TRIGGER IF EXISTS prevent_duplicate_telemetry ON drone_telemetry;
CREATE TRIGGER prevent_duplicate_telemetry
BEFORE INSERT ON drone_telemetry
FOR EACH ROW
EXECUTE FUNCTION prevent_duplicate_drone_telemetry();

-- ============================================
-- COMMENTS FOR DOCUMENTATION
-- ============================================
COMMENT ON TABLE zones IS 'Stores zone information with JSONB fields for complex data types';

COMMENT ON COLUMN zones.id IS 'Unique identifier for the zone (UUID format)';
COMMENT ON COLUMN zones.file_name IS 'Name of the file that the zone was created from';
COMMENT ON COLUMN zones.name IS 'Name of the zone';
COMMENT ON COLUMN zones.heights IS 'Array of heights stored as JSONB';
COMMENT ON COLUMN zones.geohashes IS 'Array of geohashes stored as JSONB';
COMMENT ON COLUMN zones.coordinates IS 'Array of coordinates stored as JSONB';
COMMENT ON COLUMN zones.radius IS 'Radius of the zone in meters';
COMMENT ON COLUMN zones.center IS 'Center of the zone stored as JSONB';

COMMENT ON TABLE drone_telemetry IS 'Stores drone telemetry data';

COMMENT ON COLUMN drone_telemetry.id IS 'Unique identifier for the drone telemetry data (UUID format) generated by Telegraf';
COMMENT ON COLUMN drone_telemetry.session_id IS 'Session identifier for grouping telemetry data';
COMMENT ON COLUMN drone_telemetry.drone_id IS 'Identifier for the drone';
COMMENT ON COLUMN drone_telemetry.time IS 'Timestamp of the telemetry data';
COMMENT ON COLUMN drone_telemetry.host IS 'Host of the telemetry data';
COMMENT ON COLUMN drone_telemetry.topic IS 'Topic of the telemetry data';