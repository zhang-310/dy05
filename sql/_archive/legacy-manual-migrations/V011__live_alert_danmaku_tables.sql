-- V017: 直播预警规则 + 弹幕意图日志

-- 预警规则配置表
CREATE TABLE IF NOT EXISTS live_alert_rule (
    id BIGSERIAL PRIMARY KEY,
    owner_id BIGINT NOT NULL,
    rule_name VARCHAR(128) NOT NULL,
    rule_code VARCHAR(64) NOT NULL,
    metric_name VARCHAR(64) NOT NULL,
    condition_type VARCHAR(32) NOT NULL DEFAULT 'threshold',
    threshold_value DECIMAL(10,2),
    comparison VARCHAR(16) DEFAULT 'lt',
    alert_level VARCHAR(16) DEFAULT 'warning',
    message_template TEXT,
    suggestion TEXT,
    enabled INTEGER DEFAULT 1,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_live_alert_rule_owner ON live_alert_rule(owner_id);

-- 弹幕意图分析日志表
CREATE TABLE IF NOT EXISTS live_danmaku_intent_log (
    id BIGSERIAL PRIMARY KEY,
    session_id BIGINT NOT NULL,
    batch_texts TEXT,
    intent_result JSONB,
    suggestion_result JSONB,
    analyzed_count INTEGER DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_danmaku_intent_session ON live_danmaku_intent_log(session_id);

-- sv_script 字段补齐（与 Entity 对齐）
ALTER TABLE sv_script ADD COLUMN IF NOT EXISTS persona_id BIGINT;
ALTER TABLE sv_script ADD COLUMN IF NOT EXISTS use_kb_ref BOOLEAN DEFAULT FALSE;
ALTER TABLE sv_script ADD COLUMN IF NOT EXISTS persona_check_score INTEGER;

-- sv_viral_video 自动采集字段
ALTER TABLE sv_viral_video ADD COLUMN IF NOT EXISTS auto_collected BOOLEAN DEFAULT FALSE;
ALTER TABLE sv_viral_video ADD COLUMN IF NOT EXISTS collect_source VARCHAR(64);
ALTER TABLE sv_viral_video ADD COLUMN IF NOT EXISTS last_synced_at TIMESTAMP;
