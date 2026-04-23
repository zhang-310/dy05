-- 数字人合成任务表：跟踪 HeyGen/D-ID 异步任务状态
CREATE TABLE IF NOT EXISTS sv_digital_human_task (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    project_id BIGINT,
    provider VARCHAR(20) NOT NULL,          -- heygen | did
    external_task_id VARCHAR(255) NOT NULL,  -- HeyGen video_id or D-ID talk_id
    status VARCHAR(20) NOT NULL DEFAULT 'SUBMITTED',  -- SUBMITTED, PROCESSING, COMPLETED, FAILED
    video_url TEXT,
    error_message TEXT,
    params_json TEXT,                        -- original request params
    result_json TEXT,                        -- full API response
    retry_count INTEGER DEFAULT 0,
    max_retries INTEGER DEFAULT 10,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_sv_dh_task_status ON sv_digital_human_task(status) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_sv_dh_task_user ON sv_digital_human_task(user_id) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_sv_dh_task_external ON sv_digital_human_task(provider, external_task_id) WHERE deleted = 0;
