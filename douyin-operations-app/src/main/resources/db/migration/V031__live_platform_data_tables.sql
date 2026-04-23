-- V031: live_platform, live_violation_rule, live_session_data, live_product_data（与 Entity 对齐）

-- 1. live_platform 平台配置表
CREATE TABLE IF NOT EXISTS live_platform (
    id                BIGSERIAL PRIMARY KEY,
    platform_code     VARCHAR(32)  NOT NULL,
    platform_name     VARCHAR(64)  NOT NULL,
    icon_url          VARCHAR(512),
    prompt_template   TEXT,
    max_script_length INTEGER DEFAULT 0,
    forbidden_topics  TEXT,
    active            INTEGER      NOT NULL DEFAULT 1,
    deleted           INTEGER      NOT NULL DEFAULT 0,
    create_time       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_live_platform_code ON live_platform(platform_code) WHERE deleted = 0;

-- 2. live_violation_rule 平台级违禁词规则表
CREATE TABLE IF NOT EXISTS live_violation_rule (
    id          BIGSERIAL PRIMARY KEY,
    platform_id BIGINT       NOT NULL,
    word        VARCHAR(128) NOT NULL,
    level       VARCHAR(16)  NOT NULL DEFAULT 'warning',
    reason      VARCHAR(256),
    replacement VARCHAR(256),
    category    VARCHAR(64),
    active      INTEGER      NOT NULL DEFAULT 1,
    deleted     INTEGER      NOT NULL DEFAULT 0,
    create_time TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_violation_rule_platform ON live_violation_rule(platform_id) WHERE deleted = 0 AND active = 1;
CREATE INDEX IF NOT EXISTS idx_violation_rule_word ON live_violation_rule(word) WHERE deleted = 0;

-- 3. live_session_data 直播场次数据汇总表
CREATE TABLE IF NOT EXISTS live_session_data (
    id              BIGSERIAL       PRIMARY KEY,
    session_id      BIGINT          NOT NULL,
    total_viewers   INTEGER         NOT NULL DEFAULT 0,
    peak_viewers    INTEGER         NOT NULL DEFAULT 0,
    total_likes     BIGINT          NOT NULL DEFAULT 0,
    total_comments  INTEGER         NOT NULL DEFAULT 0,
    total_shares    INTEGER         NOT NULL DEFAULT 0,
    total_revenue   NUMERIC(14,2)   NOT NULL DEFAULT 0,
    total_orders    INTEGER         NOT NULL DEFAULT 0,
    avg_stay_time   INTEGER         NOT NULL DEFAULT 0,
    new_followers   INTEGER         NOT NULL DEFAULT 0,
    sync_time       TIMESTAMP,
    ai_analysis     TEXT,
    ai_review_id    BIGINT,
    deleted         INTEGER         NOT NULL DEFAULT 0,
    create_time     TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    update_time     TIMESTAMP       DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_lsd_session_id ON live_session_data(session_id) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_lsd_sync_time ON live_session_data(sync_time) WHERE deleted = 0;

-- 4. live_product_data 直播商品数据汇总表
CREATE TABLE IF NOT EXISTS live_product_data (
    id               BIGSERIAL       PRIMARY KEY,
    session_id        BIGINT          NOT NULL,
    product_id       BIGINT          NOT NULL,
    impressions      INTEGER         NOT NULL DEFAULT 0,
    clicks           INTEGER         NOT NULL DEFAULT 0,
    orders           INTEGER         NOT NULL DEFAULT 0,
    sale_quantity    INTEGER         NOT NULL DEFAULT 0,
    revenue          NUMERIC(12,2)   NOT NULL DEFAULT 0,
    refund_quantity  INTEGER         NOT NULL DEFAULT 0,
    conversion_rate  NUMERIC(5,4)    NOT NULL DEFAULT 0,
    sync_time        TIMESTAMP,
    create_time      TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    update_time      TIMESTAMP       DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_lpd_session_product ON live_product_data(session_id, product_id);
CREATE INDEX IF NOT EXISTS idx_lpd_session_id ON live_product_data(session_id);
CREATE INDEX IF NOT EXISTS idx_lpd_product_id ON live_product_data(product_id);
