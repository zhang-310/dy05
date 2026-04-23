-- Q3-5: 效果评分公式可配置
-- Configurable Effectiveness Score Formula

CREATE TABLE IF NOT EXISTS live_effectiveness_config (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(128) NOT NULL DEFAULT '默认配置',
    conversion_weight DECIMAL(3,2) NOT NULL DEFAULT 0.30,
    interaction_weight DECIMAL(3,2) NOT NULL DEFAULT 0.25,
    retention_weight  DECIMAL(3,2) NOT NULL DEFAULT 0.25,
    gmv_weight       DECIMAL(3,2) NOT NULL DEFAULT 0.20,
    is_default       BOOLEAN DEFAULT false,
    owner_id         BIGINT,
    deleted          INTEGER DEFAULT 0,
    create_time      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_live_effectiveness_config_owner
    ON live_effectiveness_config (owner_id) WHERE deleted = 0;

COMMENT ON TABLE live_effectiveness_config IS '直播效果评分公式权重配置';
COMMENT ON COLUMN live_effectiveness_config.name IS '配置名称';
COMMENT ON COLUMN live_effectiveness_config.conversion_weight IS '转化率权重';
COMMENT ON COLUMN live_effectiveness_config.interaction_weight IS '互动量权重';
COMMENT ON COLUMN live_effectiveness_config.retention_weight IS '留存率权重';
COMMENT ON COLUMN live_effectiveness_config.gmv_weight IS 'GMV 权重';
COMMENT ON COLUMN live_effectiveness_config.is_default IS '是否为默认配置';
COMMENT ON COLUMN live_effectiveness_config.owner_id IS '所属用户 ID（数据隔离）';
