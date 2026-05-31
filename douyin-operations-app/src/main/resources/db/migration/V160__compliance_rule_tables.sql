-- V160: Compliance rule knowledge base tables
-- Ownership: common compliance runtime used by content/live checks; writes through compliance services only.
-- Note: embedding is stored as serialized TEXT. pgvector can be introduced later with an explicit extension migration.

CREATE TABLE IF NOT EXISTS compliance_rule (
    id BIGSERIAL PRIMARY KEY,
    rule_code VARCHAR(64) NOT NULL UNIQUE,
    category VARCHAR(32) NOT NULL,
    sub_category VARCHAR(32),
    rule_name VARCHAR(128) NOT NULL,
    description TEXT NOT NULL,
    severity VARCHAR(16) NOT NULL,
    punishment TEXT,
    examples TEXT,
    keywords TEXT,
    patterns TEXT,
    embedding TEXT,
    source_url TEXT,
    effective_date DATE,
    status INTEGER NOT NULL DEFAULT 1,
    deleted INTEGER NOT NULL DEFAULT 0,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_compliance_rule_category ON compliance_rule(category, sub_category) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_compliance_rule_severity ON compliance_rule(severity) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_compliance_rule_status ON compliance_rule(status, deleted);

COMMENT ON TABLE compliance_rule IS '违规规则表';
COMMENT ON COLUMN compliance_rule.rule_code IS '规则编码（唯一标识）';
COMMENT ON COLUMN compliance_rule.category IS '分类：live=直播, video=短视频, material=素材';
COMMENT ON COLUMN compliance_rule.sub_category IS '子分类：content=内容, behavior=行为, product=商品';
COMMENT ON COLUMN compliance_rule.severity IS '严重程度：critical=严重, high=高, medium=中, low=低';
COMMENT ON COLUMN compliance_rule.examples IS '违规示例（JSON 数组）';
COMMENT ON COLUMN compliance_rule.keywords IS '关键词列表（JSON 数组）';
COMMENT ON COLUMN compliance_rule.patterns IS '正则表达式列表（JSON 数组）';
COMMENT ON COLUMN compliance_rule.embedding IS '规则描述的序列化向量表示';

CREATE TABLE IF NOT EXISTS compliance_keyword (
    id BIGSERIAL PRIMARY KEY,
    keyword VARCHAR(128) NOT NULL,
    category VARCHAR(32) NOT NULL,
    severity VARCHAR(16) NOT NULL,
    replacement VARCHAR(128),
    status INTEGER NOT NULL DEFAULT 1,
    deleted INTEGER NOT NULL DEFAULT 0,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_compliance_keyword_category ON compliance_keyword(category, severity) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_compliance_keyword_keyword ON compliance_keyword(keyword) WHERE deleted = 0;

COMMENT ON TABLE compliance_keyword IS '敏感词库表';
COMMENT ON COLUMN compliance_keyword.category IS '分类：political=政治, porn=色情, violence=暴力, fraud=欺诈';

CREATE TABLE IF NOT EXISTS compliance_check_log (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    content_type VARCHAR(32) NOT NULL,
    content_id BIGINT,
    content_text TEXT,
    check_result VARCHAR(16) NOT NULL,
    matched_rules TEXT,
    risk_score DECIMAL(5,2),
    suggestions TEXT,
    check_duration_ms INTEGER,
    check_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_compliance_check_log_user ON compliance_check_log(user_id, check_time);
CREATE INDEX IF NOT EXISTS idx_compliance_check_log_content ON compliance_check_log(content_type, content_id);
CREATE INDEX IF NOT EXISTS idx_compliance_check_log_result ON compliance_check_log(check_result, check_time);

COMMENT ON TABLE compliance_check_log IS '违规检测记录表';
COMMENT ON COLUMN compliance_check_log.content_type IS '内容类型：script=话术, video=短视频, material=素材';
COMMENT ON COLUMN compliance_check_log.check_result IS '检测结果：pass=通过, warning=警告, reject=拒绝';
COMMENT ON COLUMN compliance_check_log.matched_rules IS '匹配的规则列表（JSON 数组）';
COMMENT ON COLUMN compliance_check_log.risk_score IS '风险评分（0-100，越高越危险）';

CREATE TABLE IF NOT EXISTS compliance_case (
    id BIGSERIAL PRIMARY KEY,
    rule_id BIGINT NOT NULL,
    case_title VARCHAR(256) NOT NULL,
    case_content TEXT NOT NULL,
    violation_reason TEXT,
    punishment_result TEXT,
    case_source VARCHAR(128),
    case_date DATE,
    status INTEGER NOT NULL DEFAULT 1,
    deleted INTEGER NOT NULL DEFAULT 0,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_compliance_case_rule FOREIGN KEY (rule_id) REFERENCES compliance_rule(id)
);

CREATE INDEX IF NOT EXISTS idx_compliance_case_rule ON compliance_case(rule_id) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_compliance_case_date ON compliance_case(case_date) WHERE deleted = 0;

COMMENT ON TABLE compliance_case IS '违规案例库表';
COMMENT ON COLUMN compliance_case.case_content IS '违规内容示例';
COMMENT ON COLUMN compliance_case.violation_reason IS '违规原因分析';
COMMENT ON COLUMN compliance_case.punishment_result IS '实际处罚结果';
