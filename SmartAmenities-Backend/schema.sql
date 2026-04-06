-- ============================================================
-- Smart Amenities — MySQL Schema
-- Run this file in DBeaver against your RDS MySQL instance
-- ============================================================

CREATE DATABASE IF NOT EXISTS smartamenities;
USE smartamenities;

-- ─────────────────────────────────────────────────────────────
-- amenities
-- id must exactly match node IDs in GraphConstants.kt
-- (REST_D6, REST_D10, FAM_D18, etc.) — routing breaks if these differ
-- ─────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS amenities (
    id                       VARCHAR(50)  NOT NULL,
    name                     VARCHAR(255) NOT NULL,
    type                     ENUM('RESTROOM','FAMILY_RESTROOM','LACTATION_ROOM',
                                  'GENDER_NEUTRAL_RESTROOM','WATER_FOUNTAIN') NOT NULL,
    floor                    INT          NOT NULL,
    location_x               FLOAT        NOT NULL,
    location_y               FLOAT        NOT NULL,
    status                   ENUM('OPEN','CLOSED','OUT_OF_SERVICE','UNKNOWN')
                                          NOT NULL DEFAULT 'UNKNOWN',
    crowd_level              ENUM('EMPTY','SHORT','MEDIUM','LONG','UNKNOWN')
                                          NOT NULL DEFAULT 'UNKNOWN',
    avg_usage_minutes        INT          NOT NULL DEFAULT 6,
    is_wheelchair_accessible BOOLEAN      NOT NULL DEFAULT FALSE,
    is_step_free_route       BOOLEAN      NOT NULL DEFAULT FALSE,
    is_family_restroom       BOOLEAN      NOT NULL DEFAULT FALSE,
    is_gender_neutral        BOOLEAN      NOT NULL DEFAULT FALSE,
    confidence_score         FLOAT        NOT NULL DEFAULT 0.0,
    gate_proximity           VARCHAR(100) NOT NULL,
    last_updated             BIGINT       NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ─────────────────────────────────────────────────────────────
-- users — auth table (Caleb's APIs write here on register/login)
-- matches all fields in User.kt data class
-- ─────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS users (
    id            VARCHAR(36)  NOT NULL,           -- UUID from Android
    first_name    VARCHAR(100) NOT NULL,
    last_name     VARCHAR(100) NOT NULL,
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,           -- SHA-256 hex, hashed on device
    phone         VARCHAR(50)  NOT NULL DEFAULT '',
    is_guest      BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at    BIGINT       NOT NULL,           -- Unix ms from Android System.currentTimeMillis()
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ─────────────────────────────────────────────────────────────
-- crowd_readings — historical crowd log for admin panel
-- one row inserted every time crowd level changes on any amenity
-- (DynamoDB mirrors this table for real-time high-write queries)
-- ─────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS crowd_readings (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    amenity_id  VARCHAR(50)  NOT NULL,
    crowd_level ENUM('EMPTY','SHORT','MEDIUM','LONG','UNKNOWN') NOT NULL,
    recorded_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_crowd_amenity
        FOREIGN KEY (amenity_id) REFERENCES amenities(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ─────────────────────────────────────────────────────────────
-- simulation_scenarios — preset crowd scenarios for admin panel
-- Shreyas's panel calls an API to load these and apply overrides
-- ─────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS simulation_scenarios (
    id          INT          NOT NULL AUTO_INCREMENT,
    name        VARCHAR(100) NOT NULL UNIQUE,
    description TEXT         NOT NULL,
    config_json JSON         NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
