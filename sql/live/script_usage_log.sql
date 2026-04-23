-- ================================================
-- 话术效果归因闭环 #26：记录话术使用，反哺评分
-- ================================================
CREATE TABLE IF NOT EXISTS script_usage_log (
    id BIGSERIAL PRIMARY KEY,
    script_source VARCHAR(16) NOT NULL,
    script_id BIGINT NOT NULL,
    session_id BIGINT,
    product_id BIGINT,
    script_type VARCHAR(32),
    used_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    user_id BIGINT,
    deleted INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_script_usage_log_script ON script_usage_log(script_source, script_id);
CREATE INDEX IF NOT EXISTS idx_script_usage_log_session ON script_usage_log(session_id) WHERE session_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_script_usage_log_used_at ON script_usage_log(used_at DESC);

COMMENT ON TABLE script_usage_log IS '话术使用日志（效果归因反哺评分）';
COMMENT ON COLUMN script_usage_log.script_source IS '来源：product=产品话术 live=直播话术';
COMMENT ON COLUMN script_usage_log.script_id IS '话术 ID';
