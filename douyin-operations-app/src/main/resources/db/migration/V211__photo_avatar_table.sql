-- V211 完整版: 照片转视频任务表
DROP TABLE IF EXISTS photo_avatar_task CASCADE;
CREATE TABLE photo_avatar_task (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL DEFAULT 0,
    photo_url VARCHAR(512),
    outfit_style VARCHAR(32) DEFAULT 'casual',
    background VARCHAR(32) DEFAULT 'studio',
    status VARCHAR(16) DEFAULT 'pending',
    output_url VARCHAR(512),
    error_message VARCHAR(512),
    progress INT DEFAULT 0,
    cost_credits BIGINT DEFAULT 0,
    deleted INT DEFAULT 0,
    create_time TIMESTAMP DEFAULT now(),
    update_time TIMESTAMP DEFAULT now()
);
CREATE INDEX idx_pa_user ON photo_avatar_task(user_id, create_time DESC) WHERE deleted = 0;
CREATE INDEX idx_pa_status ON photo_avatar_task(status) WHERE deleted = 0;
