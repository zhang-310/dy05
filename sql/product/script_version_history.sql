-- ================================================
-- 产品话术版本历史表（#16 话术版本历史 + 回滚）
-- ================================================
CREATE TABLE IF NOT EXISTS script_version_history (
    id BIGSERIAL PRIMARY KEY,
    script_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    script_type VARCHAR(32) NOT NULL,
    style VARCHAR(64),
    version INTEGER NOT NULL,
    script_content TEXT NOT NULL,
    persona_id BIGINT,
    duration INTEGER,
    created_by BIGINT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_script_version_history_script_id ON script_version_history(script_id);
CREATE INDEX IF NOT EXISTS idx_script_version_history_product_id ON script_version_history(product_id);
CREATE INDEX IF NOT EXISTS idx_script_version_history_create_time ON script_version_history(create_time DESC);

COMMENT ON TABLE script_version_history IS '产品话术版本历史表';
COMMENT ON COLUMN script_version_history.script_id IS '话术 ID（dy_product_script.id）';
COMMENT ON COLUMN script_version_history.product_id IS '产品 ID';
COMMENT ON COLUMN script_version_history.script_type IS '话术类型：seed/promotion/formal';
COMMENT ON COLUMN script_version_history.script_content IS '内容快照';
