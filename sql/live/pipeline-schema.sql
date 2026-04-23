-- 一键全自动流水线表
-- Task Q4-2: Generate → QC → Auto-Refine → Save pipeline

CREATE TABLE IF NOT EXISTS live_script_pipeline (
    id              BIGSERIAL PRIMARY KEY,
    session_id      BIGINT       NOT NULL,
    status          VARCHAR(20)  NOT NULL DEFAULT 'pending',  -- pending/generating/checking/refining/completed/failed
    total_scripts   INTEGER      DEFAULT 0,
    completed_scripts INTEGER    DEFAULT 0,
    refined_scripts INTEGER      DEFAULT 0,
    failed_scripts  INTEGER      DEFAULT 0,
    config_json     TEXT,
    owner_id        BIGINT       NOT NULL,
    deleted         INTEGER      NOT NULL DEFAULT 0,
    create_time     TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    update_time     TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE  live_script_pipeline IS '直播话术全自动生成流水线';
COMMENT ON COLUMN live_script_pipeline.status IS '流水线状态: pending/generating/checking/refining/completed/failed';
COMMENT ON COLUMN live_script_pipeline.config_json IS '流水线配置 JSON（modelId, style, useKbRef 等）';

CREATE INDEX IF NOT EXISTS idx_pipeline_session ON live_script_pipeline(session_id) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_pipeline_owner   ON live_script_pipeline(owner_id)   WHERE deleted = 0;
