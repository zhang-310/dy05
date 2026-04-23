-- 话术生成配置预设表
CREATE TABLE IF NOT EXISTS live_generation_preset (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT NOT NULL,
    preset_name     VARCHAR(128) NOT NULL,
    gen_style       VARCHAR(64) DEFAULT 'professional',
    use_kb_ref      INTEGER DEFAULT 1,
    model_id        BIGINT,
    hot_keywords    TEXT,           -- JSON array of strings
    duration_mode   VARCHAR(32) DEFAULT 'standard', -- standard/short/custom
    is_default      INTEGER DEFAULT 0,  -- 1 = default preset for this user
    deleted         INTEGER DEFAULT 0,
    create_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_live_gen_preset_user ON live_generation_preset(user_id);
