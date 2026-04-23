-- V046: live_script_ab_test 话术版本级 A/B 测试表（Phase 2.5）
CREATE TABLE IF NOT EXISTS live_script_ab_test (
    id BIGSERIAL PRIMARY KEY,
    owner_id BIGINT NOT NULL,
    script_id BIGINT NOT NULL,
    version_a_id BIGINT NOT NULL,
    version_b_id BIGINT NOT NULL,
    status VARCHAR(16) DEFAULT 'running',
    traffic_split INTEGER DEFAULT 50,
    a_impressions INTEGER DEFAULT 0,
    b_impressions INTEGER DEFAULT 0,
    a_conversion_rate DOUBLE PRECISION,
    b_conversion_rate DOUBLE PRECISION,
    a_retention_rate DOUBLE PRECISION,
    b_retention_rate DOUBLE PRECISION,
    a_interaction_rate DOUBLE PRECISION,
    b_interaction_rate DOUBLE PRECISION,
    p_value DOUBLE PRECISION,
    confidence_level DOUBLE PRECISION,
    winner VARCHAR(1),
    start_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    end_time TIMESTAMP,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_ab_test_owner ON live_script_ab_test(owner_id, status) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_ab_test_script ON live_script_ab_test(script_id) WHERE deleted = 0;
