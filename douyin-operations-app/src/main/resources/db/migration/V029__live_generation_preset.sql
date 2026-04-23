-- V029: live_generation_preset 生成配置预设表（与 Entity 对齐）
CREATE TABLE IF NOT EXISTS live_generation_preset (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(100) NOT NULL,
    description     TEXT,
    style           VARCHAR(50) DEFAULT 'standard',
    model_id        BIGINT,
    use_kb_ref      BOOLEAN DEFAULT true,
    duration_mode   VARCHAR(20) DEFAULT 'standard',
    hot_keywords    TEXT,
    is_default      BOOLEAN DEFAULT false,
    owner_id        BIGINT,
    deleted         INTEGER NOT NULL DEFAULT 0,
    create_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_live_gen_preset_owner ON live_generation_preset(owner_id) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_live_gen_preset_default ON live_generation_preset(owner_id, is_default) WHERE deleted = 0;
