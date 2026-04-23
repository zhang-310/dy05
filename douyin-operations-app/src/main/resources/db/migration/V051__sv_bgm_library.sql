-- V051: BGM 素材库（Phase 4.2）
CREATE TABLE IF NOT EXISTS sv_bgm_library (
    id BIGSERIAL PRIMARY KEY,
    bgm_name VARCHAR(200) NOT NULL,
    artist VARCHAR(100),
    style VARCHAR(64) NOT NULL,
    bpm INTEGER,
    mood VARCHAR(64),
    energy_level INTEGER,
    emotion_curve_match VARCHAR(200),
    usage_count INTEGER DEFAULT 0,
    avg_viral_score DOUBLE PRECISION,
    best_content_types JSONB DEFAULT '[]',
    license_type VARCHAR(32),
    source_platform VARCHAR(32),
    expire_date DATE,
    duration_seconds INTEGER,
    tags JSONB DEFAULT '[]',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_bgm_style ON sv_bgm_library(style) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_bgm_mood ON sv_bgm_library(mood) WHERE deleted = 0;
