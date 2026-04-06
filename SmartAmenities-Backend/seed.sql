-- ============================================================
-- Smart Amenities — Seed Data
-- Run after schema.sql
-- ============================================================

USE smartamenities;

-- ─────────────────────────────────────────────────────────────
-- amenities — 14 rows translated from MockAmenityDataSource.kt
-- last_updated offsets match Kotlin: now - X_000L (X seconds ago)
-- id values must exactly match GraphConstants.kt amenityNodeIds
-- ─────────────────────────────────────────────────────────────
INSERT INTO amenities
    (id, name, type, floor, location_x, location_y,
     status, crowd_level, avg_usage_minutes,
     is_wheelchair_accessible, is_step_free_route, is_family_restroom, is_gender_neutral,
     confidence_score, gate_proximity, last_updated)
VALUES
-- Level 3 Restrooms — Top Wall (Gates D5–D22)
('REST_D6',  'Restroom – Near Gate D6',  'RESTROOM', 3, 0.10, 0.35,
 'OPEN',          'SHORT',   6, TRUE, TRUE, FALSE, FALSE, 0.92, 'Near Gate D6',
 FLOOR((UNIX_TIMESTAMP() - 90)   * 1000)),

('REST_D10', 'Restroom – Near Gate D10', 'RESTROOM', 3, 0.34, 0.35,
 'OPEN',          'MEDIUM',  6, TRUE, TRUE, FALSE, FALSE, 0.88, 'Near Gate D10',
 FLOOR((UNIX_TIMESTAMP() - 180)  * 1000)),

('REST_D17', 'Restroom – Near Gate D17', 'RESTROOM', 3, 0.70, 0.35,
 'OPEN',          'SHORT',   6, TRUE, TRUE, FALSE, FALSE, 0.95, 'Near Gate D17',
 FLOOR((UNIX_TIMESTAMP() - 60)   * 1000)),

('REST_D20', 'Restroom – Near Gate D20', 'RESTROOM', 3, 0.88, 0.35,
 'OUT_OF_SERVICE','UNKNOWN', 6, TRUE, TRUE, FALSE, FALSE, 0.70, 'Near Gate D20',
 FLOOR((UNIX_TIMESTAMP() - 600)  * 1000)),

('REST_D22', 'Restroom – Near Gate D22', 'RESTROOM', 3, 0.96, 0.35,
 'OPEN',          'LONG',    6, TRUE, TRUE, FALSE, FALSE, 0.97, 'Near Gate D22',
 FLOOR((UNIX_TIMESTAMP() - 30)   * 1000)),

-- Level 3 Restrooms — Bottom Wall (Gates D23–D40)
('REST_D24', 'Restroom – Near Gate D24', 'RESTROOM', 3, 0.90, 0.65,
 'OPEN',          'MEDIUM',  6, TRUE, TRUE, FALSE, FALSE, 0.90, 'Near Gate D24',
 FLOOR((UNIX_TIMESTAMP() - 120)  * 1000)),

('REST_D27', 'Restroom – Near Gate D27', 'RESTROOM', 3, 0.72, 0.65,
 'CLOSED',        'UNKNOWN', 6, TRUE, TRUE, FALSE, FALSE, 0.60, 'Near Gate D27',
 FLOOR((UNIX_TIMESTAMP() - 1200) * 1000)),

('REST_D29', 'Restroom – Near Gate D29', 'RESTROOM', 3, 0.60, 0.65,
 'OPEN',          'SHORT',   6, TRUE, TRUE, FALSE, FALSE, 0.87, 'Near Gate D29',
 FLOOR((UNIX_TIMESTAMP() - 150)  * 1000)),

('REST_D36', 'Restroom – Near Gate D36', 'RESTROOM', 3, 0.30, 0.65,
 'OPEN',          'SHORT',   6, TRUE, TRUE, FALSE, FALSE, 0.83, 'Near Gate D36',
 FLOOR((UNIX_TIMESTAMP() - 240)  * 1000)),

('REST_D40', 'Restroom – Near Gate D40', 'RESTROOM', 3, 0.06, 0.65,
 'OPEN',          'MEDIUM',  6, TRUE, TRUE, FALSE, FALSE, 0.80, 'Near Gate D40',
 FLOOR((UNIX_TIMESTAMP() - 300)  * 1000)),

-- Level 3 Family Restrooms
('FAM_D18',  'Family Restroom – Near Gate D18', 'FAMILY_RESTROOM', 3, 0.76, 0.35,
 'OPEN',          'SHORT',   6, TRUE, TRUE, TRUE,  FALSE, 0.94, 'Near Gate D18',
 FLOOR((UNIX_TIMESTAMP() - 45)   * 1000)),

('FAM_D25',  'Family Restroom – Near Gate D25', 'FAMILY_RESTROOM', 3, 0.84, 0.65,
 'OPEN',          'SHORT',   6, TRUE, TRUE, TRUE,  FALSE, 0.93, 'Near Gate D25',
 FLOOR((UNIX_TIMESTAMP() - 75)   * 1000)),

('FAM_D28',  'Family Restroom – Near Gate D28', 'FAMILY_RESTROOM', 3, 0.66, 0.65,
 'OUT_OF_SERVICE','UNKNOWN', 6, TRUE, TRUE, TRUE,  FALSE, 0.55, 'Near Gate D28',
 FLOOR((UNIX_TIMESTAMP() - 900)  * 1000)),

-- Level 3 Lactation Room
('LAC_D22',  'Lactation Room – Near Gate D22',  'LACTATION_ROOM',  3, 0.96, 0.38,
 'OPEN',          'SHORT',   6, TRUE, TRUE, FALSE, FALSE, 0.96, 'Near Gate D22',
 FLOOR((UNIX_TIMESTAMP() - 60)   * 1000));

-- ─────────────────────────────────────────────────────────────
-- simulation_scenarios — 3 presets for Shreyas's admin panel
-- ─────────────────────────────────────────────────────────────
INSERT INTO simulation_scenarios (name, description, config_json) VALUES

('Peak Arrival Surge',
 'Restrooms near Gate D22 are all crowded — simulates a large flight just deplaned at D22.',
 '{
    "overrides": {
        "REST_D22": {"crowd_level": "LONG"},
        "REST_D24": {"crowd_level": "LONG"},
        "LAC_D22":  {"crowd_level": "LONG"}
    }
 }'),

('D22 Closed',
 'REST_D22 is marked closed — simulates a maintenance closure at the D22 restroom.',
 '{
    "overrides": {
        "REST_D22": {"status": "CLOSED"}
    }
 }'),

('Quiet Period',
 'All amenities have empty crowd levels — simulates an off-peak lull across Terminal D.',
 '{
    "overrides": {
        "REST_D6":  {"crowd_level": "EMPTY"},
        "REST_D10": {"crowd_level": "EMPTY"},
        "REST_D17": {"crowd_level": "EMPTY"},
        "REST_D20": {"crowd_level": "EMPTY"},
        "REST_D22": {"crowd_level": "EMPTY"},
        "REST_D24": {"crowd_level": "EMPTY"},
        "REST_D27": {"crowd_level": "EMPTY"},
        "REST_D29": {"crowd_level": "EMPTY"},
        "REST_D36": {"crowd_level": "EMPTY"},
        "REST_D40": {"crowd_level": "EMPTY"},
        "FAM_D18":  {"crowd_level": "EMPTY"},
        "FAM_D25":  {"crowd_level": "EMPTY"},
        "FAM_D28":  {"crowd_level": "EMPTY"},
        "LAC_D22":  {"crowd_level": "EMPTY"}
    }
 }');
