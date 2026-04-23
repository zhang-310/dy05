-- V032: live_session_script_slot, live_session_realtime_data（与 Entity 对齐）

-- 1. live_session_script_slot 直播话术段落表
CREATE TABLE IF NOT EXISTS live_session_script_slot (
    id                BIGSERIAL PRIMARY KEY,
    live_session_id   BIGINT    NOT NULL,
    slot_index        INTEGER   NOT NULL,
    script_version_id BIGINT,
    content           TEXT      NOT NULL,
    duration_seconds   INTEGER   DEFAULT 120,
    script_type       VARCHAR(32),
    style             VARCHAR(64),
    is_current        BOOLEAN   DEFAULT false,
    is_completed      BOOLEAN   DEFAULT false,
    started_at        TIMESTAMP,
    completed_at      TIMESTAMP,
    owner_id          BIGINT    NOT NULL,
    created_at        TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted           INTEGER   NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_live_session_script_slot_session_id ON live_session_script_slot(live_session_id) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_live_session_script_slot_session_slot ON live_session_script_slot(live_session_id, slot_index) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_live_session_script_slot_is_current ON live_session_script_slot(live_session_id, is_current) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_live_session_script_slot_owner_id ON live_session_script_slot(owner_id) WHERE deleted = 0;

-- 2. live_session_realtime_data 直播实时数据表
CREATE TABLE IF NOT EXISTS live_session_realtime_data (
    id                      BIGSERIAL PRIMARY KEY,
    live_session_id         BIGINT    NOT NULL,
    watched_count           INTEGER   DEFAULT 0,
    viewer_count            INTEGER   DEFAULT 0,
    like_count              INTEGER   DEFAULT 0,
    comment_count           INTEGER   DEFAULT 0,
    share_count             INTEGER   DEFAULT 0,
    follow_count            INTEGER   DEFAULT 0,
    gift_amount             DECIMAL(10,2) DEFAULT 0.00,
    product_click_count     INTEGER   DEFAULT 0,
    product_purchase_count  INTEGER   DEFAULT 0,
    product_purchase_amount DECIMAL(10,2) DEFAULT 0.00,
    current_slot_index      INTEGER,
    created_at              TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted                 INTEGER   NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_live_session_realtime_data_session_id ON live_session_realtime_data(live_session_id) WHERE deleted = 0;
