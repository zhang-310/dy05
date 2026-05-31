-- V209 完整版: AI 数字人任务表
DROP TABLE IF EXISTS digital_human_task CASCADE;
CREATE TABLE digital_human_task (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL DEFAULT 0,
    script_content TEXT,
    voice_type VARCHAR(32) DEFAULT 'default',
    avatar_id BIGINT,
    status VARCHAR(16) DEFAULT 'pending',
    output_url VARCHAR(512),
    error_message VARCHAR(512),
    progress INT DEFAULT 0,
    cost_credits BIGINT DEFAULT 0,
    deleted INT DEFAULT 0,
    create_time TIMESTAMP DEFAULT now(),
    update_time TIMESTAMP DEFAULT now()
);
CREATE INDEX idx_dh_user ON digital_human_task(user_id, create_time DESC) WHERE deleted = 0;
CREATE INDEX idx_dh_status ON digital_human_task(status) WHERE deleted = 0;
