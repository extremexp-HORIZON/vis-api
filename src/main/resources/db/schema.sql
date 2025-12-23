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
    rectangle JSONB,
    PRIMARY KEY (id, file_name),
    CONSTRAINT zones_file_name_id_unique UNIQUE (file_name, id)
);

-- Create indexes for better query performance
CREATE INDEX IF NOT EXISTS idx_zones_file_name ON zones(file_name);
CREATE INDEX IF NOT EXISTS idx_zones_type ON zones(type);
CREATE INDEX IF NOT EXISTS idx_zones_status ON zones(status);
CREATE INDEX IF NOT EXISTS idx_zones_created_at ON zones(created_at);

-- GIN indexes for JSONB columns to enable efficient JSON queries
CREATE INDEX IF NOT EXISTS idx_zones_heights_gin ON zones USING GIN (heights);
CREATE INDEX IF NOT EXISTS idx_zones_geohashes_gin ON zones USING GIN (geohashes);
CREATE INDEX IF NOT EXISTS idx_zones_rectangle_gin ON zones USING GIN (rectangle);

-- ============================================
-- COMMENTS FOR DOCUMENTATION
-- ============================================
COMMENT ON TABLE zones IS 'Stores zone information with JSONB fields for complex data types';

COMMENT ON COLUMN zones.id IS 'Unique identifier for the zone (UUID format)';
COMMENT ON COLUMN zones.file_name IS 'Name of the file that the zone was created from';
COMMENT ON COLUMN zones.name IS 'Name of the zone';
COMMENT ON COLUMN zones.heights IS 'Array of heights stored as JSONB';
COMMENT ON COLUMN zones.geohashes IS 'Array of geohashes stored as JSONB';
COMMENT ON COLUMN zones.rectangle IS 'Rectangle object stored as JSONB';
