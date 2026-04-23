-- V052: 竞品监测（Phase 4.3）
CREATE TABLE IF NOT EXISTS sv_competitor (
    id BIGSERIAL PRIMARY KEY,
    owner_id BIGINT NOT NULL,
    competitor_name VARCHAR(100) NOT NULL,
    platform VARCHAR(32) DEFAULT 'douyin',
    account_id VARCHAR(100),
    account_url VARCHAR(500),
    category VARCHAR(64),
    fan_count BIGINT DEFAULT 0,
    notes TEXT,
    is_active BOOLEAN DEFAULT true,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_competitor_owner ON sv_competitor(owner_id) WHERE deleted = 0;

CREATE TABLE IF NOT EXISTS sv_competitor_snapshot (
    id BIGSERIAL PRIMARY KEY,
    competitor_id BIGINT NOT NULL,
    snapshot_date DATE NOT NULL,
    fan_count BIGINT,
    fan_delta INTEGER,
    video_count INTEGER,
    avg_view_count BIGINT,
    avg_like_rate DOUBLE PRECISION,
    avg_completion_rate DOUBLE PRECISION,
    top_video_titles JSONB DEFAULT '[]',
    content_strategy_summary TEXT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_competitor_snapshot ON sv_competitor_snapshot(competitor_id, snapshot_date);
