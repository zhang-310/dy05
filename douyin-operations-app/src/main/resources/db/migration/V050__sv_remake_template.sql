-- V050: 爆款二创模板库（Phase 4.1）
CREATE TABLE IF NOT EXISTS sv_remake_template (
    id BIGSERIAL PRIMARY KEY,
    owner_id BIGINT NOT NULL DEFAULT 0,
    template_name VARCHAR(100) NOT NULL,
    remake_type VARCHAR(32) NOT NULL,
    source_viral_id BIGINT,
    structure_template JSONB NOT NULL,
    emotion_curve VARCHAR(200),
    bgm_style VARCHAR(64),
    duration_range VARCHAR(32),
    adaptation_guide TEXT,
    variable_slots JSONB DEFAULT '[]',
    usage_count INTEGER DEFAULT 0,
    avg_viral_score DOUBLE PRECISION,
    content_types JSONB DEFAULT '[]',
    tags JSONB DEFAULT '[]',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_remake_type ON sv_remake_template(remake_type) WHERE deleted = 0;
