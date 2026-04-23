-- product_script_snapshot 表
-- 说明：记录直播场次中引用的话术快照
CREATE TABLE IF NOT EXISTS product_script_snapshot (
    id BIGSERIAL PRIMARY KEY,
    live_session_id BIGINT NOT NULL,
    product_script_version_id BIGINT NOT NULL,
    content_snapshot TEXT NOT NULL,
    referenced_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    owner_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_product_script_snapshot_live_session_id
    ON product_script_snapshot(live_session_id) WHERE deleted = 0;

CREATE INDEX IF NOT EXISTS idx_product_script_snapshot_product_script_version_id
    ON product_script_snapshot(product_script_version_id) WHERE deleted = 0;
