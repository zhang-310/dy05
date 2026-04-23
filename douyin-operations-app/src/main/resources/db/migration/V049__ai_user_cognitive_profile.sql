-- V049: 用户认知画像（Phase 3.3）
CREATE TABLE IF NOT EXISTS ai_user_cognitive_profile (
    id BIGSERIAL PRIMARY KEY,
    owner_id BIGINT NOT NULL UNIQUE,
    preferred_script_types JSONB DEFAULT '{}',
    preferred_styles JSONB DEFAULT '{}',
    preferred_emotion_curves JSONB DEFAULT '{}',
    avg_edit_ratio DOUBLE PRECISION DEFAULT 0.5,
    generation_frequency INTEGER DEFAULT 0,
    preferred_length VARCHAR(16) DEFAULT 'medium',
    optimization_focus VARCHAR(32) DEFAULT 'balanced',
    risk_tolerance DOUBLE PRECISION DEFAULT 0.5,
    profile_version INTEGER DEFAULT 1,
    last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER DEFAULT 0
);
