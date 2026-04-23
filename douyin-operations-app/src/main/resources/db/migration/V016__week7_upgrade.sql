-- V016: Week 7-8 升级迁移（合并 sql/migrations V010）

-- 竞品监控表
CREATE TABLE IF NOT EXISTS sv_competitor_account (
    id BIGSERIAL PRIMARY KEY,
    owner_id BIGINT NOT NULL,
    account_id VARCHAR(128),
    account_name VARCHAR(256) NOT NULL,
    platform VARCHAR(64) DEFAULT 'douyin',
    status INTEGER DEFAULT 1,
    last_analysis_time TIMESTAMP,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_sv_competitor_owner ON sv_competitor_account(owner_id);

-- 竞品分析报告表
CREATE TABLE IF NOT EXISTS sv_competitor_report (
    id BIGSERIAL PRIMARY KEY,
    owner_id BIGINT NOT NULL,
    competitor_id BIGINT,
    report_type VARCHAR(32) DEFAULT 'single',
    content TEXT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER DEFAULT 0
);

-- 效果预测记录表
CREATE TABLE IF NOT EXISTS sv_effect_prediction (
    id BIGSERIAL PRIMARY KEY,
    owner_id BIGINT NOT NULL,
    title VARCHAR(512),
    script_content TEXT,
    publish_time VARCHAR(64),
    prediction_result JSONB,
    confidence DECIMAL(3,2),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER DEFAULT 0
);

-- 行业合规检测记录表
CREATE TABLE IF NOT EXISTS sc_compliance_check_log (
    id BIGSERIAL PRIMARY KEY,
    owner_id BIGINT NOT NULL,
    text_content TEXT,
    industry_code VARCHAR(32),
    violation_count INTEGER DEFAULT 0,
    result JSONB,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER DEFAULT 0
);

-- 通知模板表
CREATE TABLE IF NOT EXISTS wc_notification_template (
    id BIGSERIAL PRIMARY KEY,
    owner_id BIGINT NOT NULL,
    template_code VARCHAR(64) NOT NULL UNIQUE,
    template_name VARCHAR(128),
    msg_type VARCHAR(32) DEFAULT 'markdown',
    content_template TEXT,
    enabled INTEGER DEFAULT 1,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER DEFAULT 0
);

-- 数字人生成记录表
CREATE TABLE IF NOT EXISTS ai_digital_human_task (
    id BIGSERIAL PRIMARY KEY,
    owner_id BIGINT NOT NULL,
    avatar_id VARCHAR(128),
    voice_id VARCHAR(128),
    script_text TEXT,
    video_url TEXT,
    provider VARCHAR(64),
    status VARCHAR(32) DEFAULT 'pending',
    error_message TEXT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_ai_dh_task_owner ON ai_digital_human_task(owner_id);
