-- Q3-5: 效果评分公式权重配置表
CREATE TABLE IF NOT EXISTS live_effectiveness_config (
    id                  BIGSERIAL PRIMARY KEY,
    user_id             BIGINT NOT NULL,
    config_name         VARCHAR(128) NOT NULL DEFAULT '默认配置',
    viewer_weight       NUMERIC(5,2) NOT NULL DEFAULT 0.30,
    interaction_weight  NUMERIC(5,2) NOT NULL DEFAULT 0.30,
    conversion_weight   NUMERIC(5,2) NOT NULL DEFAULT 0.40,
    is_default          INTEGER DEFAULT 1,
    deleted             INTEGER DEFAULT 0,
    create_time         TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time         TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_live_eff_config_user ON live_effectiveness_config(user_id);

COMMENT ON TABLE live_effectiveness_config IS '直播效果评分公式权重配置';
COMMENT ON COLUMN live_effectiveness_config.user_id IS '用户ID（数据隔离）';
COMMENT ON COLUMN live_effectiveness_config.config_name IS '配置名称';
COMMENT ON COLUMN live_effectiveness_config.viewer_weight IS '观看量权重';
COMMENT ON COLUMN live_effectiveness_config.interaction_weight IS '互动量权重';
COMMENT ON COLUMN live_effectiveness_config.conversion_weight IS '转化率权重';
COMMENT ON COLUMN live_effectiveness_config.is_default IS '是否默认配置 0=否 1=是';
