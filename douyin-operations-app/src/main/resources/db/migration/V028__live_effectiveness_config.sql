-- V028: live_effectiveness_config 直播效果评分公式权重配置表（与 Entity 对齐）
CREATE TABLE IF NOT EXISTS live_effectiveness_config (
    id                  BIGSERIAL PRIMARY KEY,
    user_id             BIGINT        NOT NULL,
    owner_id            BIGINT,
    config_name         VARCHAR(128)  NOT NULL DEFAULT '默认配置',
    name                VARCHAR(128),
    conversion_weight   DECIMAL(5,2)  NOT NULL DEFAULT 0.30,
    interaction_weight  DECIMAL(5,2)  NOT NULL DEFAULT 0.25,
    retention_weight    DECIMAL(5,2)  NOT NULL DEFAULT 0.25,
    gmv_weight          DECIMAL(5,2)  NOT NULL DEFAULT 0.20,
    viewer_weight       DECIMAL(5,2)  DEFAULT 0.30,
    is_default          INTEGER       DEFAULT 0,
    deleted             INTEGER       NOT NULL DEFAULT 0,
    create_time         TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    update_time         TIMESTAMP     DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_live_effectiveness_config_owner ON live_effectiveness_config(owner_id) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_live_eff_config_user ON live_effectiveness_config(user_id) WHERE deleted = 0;
