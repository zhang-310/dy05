-- V210 完整版: 短剧项目表
DROP TABLE IF EXISTS drama_project CASCADE;
CREATE TABLE drama_project (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL DEFAULT 0,
    title VARCHAR(256),
    description TEXT,
    genre VARCHAR(32) DEFAULT 'other',
    status VARCHAR(32) DEFAULT 'draft',
    script TEXT,
    episode_count INT DEFAULT 0,
    visibility VARCHAR(16) DEFAULT 'private',
    cost_credits BIGINT DEFAULT 0,
    deleted INT DEFAULT 0,
    create_time TIMESTAMP DEFAULT now(),
    update_time TIMESTAMP DEFAULT now()
);
CREATE INDEX idx_drama_user ON drama_project(user_id, create_time DESC) WHERE deleted = 0;
CREATE INDEX idx_drama_status ON drama_project(status) WHERE deleted = 0;
