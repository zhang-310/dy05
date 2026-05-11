-- ============================================
-- 抖音违规知识库 - 数据库表结构
-- 生成日期: 2026-05-10
-- 用途: 存储抖音直播、短视频、素材违规规则
-- ============================================

-- 1. 违规规则表
CREATE TABLE IF NOT EXISTS compliance_rule (
    id BIGSERIAL PRIMARY KEY,
    rule_code VARCHAR(64) NOT NULL UNIQUE,        -- 规则编码（如 LIVE_CONTENT_POLITICAL）
    category VARCHAR(32) NOT NULL,                -- 分类（live/video/material）
    sub_category VARCHAR(32),                     -- 子分类（content/behavior/product）
    rule_name VARCHAR(128) NOT NULL,              -- 规则名称
    description TEXT NOT NULL,                    -- 规则描述
    severity VARCHAR(16) NOT NULL,                -- 严重程度（critical/high/medium/low）
    punishment TEXT,                              -- 处罚措施
    examples TEXT,                                -- 违规示例（JSON 数组）
    keywords TEXT,                                -- 关键词（JSON 数组）
    patterns TEXT,                                -- 正则表达式（JSON 数组）
    embedding VECTOR(1536),                       -- 向量化（用于语义检索）
    source_url TEXT,                              -- 来源 URL
    effective_date DATE,                          -- 生效日期
    status INTEGER DEFAULT 1,                     -- 状态（1=启用 0=禁用）
    deleted INTEGER DEFAULT 0,                    -- 逻辑删除（0=未删除 1=已删除）
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 索引
CREATE INDEX idx_compliance_rule_category ON compliance_rule(category, sub_category) WHERE deleted = 0;
CREATE INDEX idx_compliance_rule_severity ON compliance_rule(severity) WHERE deleted = 0;
CREATE INDEX idx_compliance_rule_status ON compliance_rule(status, deleted);

-- 向量索引（需要 pgvector 扩展）
-- CREATE INDEX idx_compliance_rule_embedding ON compliance_rule USING ivfflat(embedding vector_cosine_ops) WHERE deleted = 0;

COMMENT ON TABLE compliance_rule IS '违规规则表';
COMMENT ON COLUMN compliance_rule.rule_code IS '规则编码（唯一标识）';
COMMENT ON COLUMN compliance_rule.category IS '分类：live=直播, video=短视频, material=素材';
COMMENT ON COLUMN compliance_rule.sub_category IS '子分类：content=内容, behavior=行为, product=商品';
COMMENT ON COLUMN compliance_rule.severity IS '严重程度：critical=严重, high=高, medium=中, low=低';
COMMENT ON COLUMN compliance_rule.examples IS '违规示例（JSON 数组）';
COMMENT ON COLUMN compliance_rule.keywords IS '关键词列表（JSON 数组）';
COMMENT ON COLUMN compliance_rule.patterns IS '正则表达式列表（JSON 数组）';
COMMENT ON COLUMN compliance_rule.embedding IS '规则描述的向量表示（1536 维）';

-- 2. 违规检测记录表
CREATE TABLE IF NOT EXISTS compliance_check_log (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,                      -- 用户 ID
    content_type VARCHAR(32) NOT NULL,            -- 内容类型（script/video/material）
    content_id BIGINT,                            -- 内容 ID
    content_text TEXT,                            -- 检测内容
    check_result VARCHAR(16) NOT NULL,            -- 检测结果（pass/warning/reject）
    matched_rules TEXT,                           -- 匹配的规则（JSON 数组）
    risk_score DECIMAL(5,2),                      -- 风险评分（0-100）
    suggestions TEXT,                             -- 修改建议
    check_duration_ms INTEGER,                    -- 检测耗时（毫秒）
    check_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 索引
CREATE INDEX idx_compliance_check_log_user ON compliance_check_log(user_id, check_time);
CREATE INDEX idx_compliance_check_log_content ON compliance_check_log(content_type, content_id);
CREATE INDEX idx_compliance_check_log_result ON compliance_check_log(check_result, check_time);

COMMENT ON TABLE compliance_check_log IS '违规检测记录表';
COMMENT ON COLUMN compliance_check_log.content_type IS '内容类型：script=话术, video=短视频, material=素材';
COMMENT ON COLUMN compliance_check_log.check_result IS '检测结果：pass=通过, warning=警告, reject=拒绝';
COMMENT ON COLUMN compliance_check_log.matched_rules IS '匹配的规则列表（JSON 数组）';
COMMENT ON COLUMN compliance_check_log.risk_score IS '风险评分（0-100，越高越危险）';

-- 3. 敏感词库表
CREATE TABLE IF NOT EXISTS compliance_keyword (
    id BIGSERIAL PRIMARY KEY,
    keyword VARCHAR(128) NOT NULL,                -- 敏感词
    category VARCHAR(32) NOT NULL,                -- 分类（political/porn/violence/fraud）
    severity VARCHAR(16) NOT NULL,                -- 严重程度（critical/high/medium/low）
    replacement VARCHAR(128),                     -- 替换词（可选）
    status INTEGER DEFAULT 1,                     -- 状态（1=启用 0=禁用）
    deleted INTEGER DEFAULT 0,                    -- 逻辑删除
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 索引
CREATE INDEX idx_compliance_keyword_category ON compliance_keyword(category, severity) WHERE deleted = 0;
CREATE INDEX idx_compliance_keyword_keyword ON compliance_keyword(keyword) WHERE deleted = 0;

COMMENT ON TABLE compliance_keyword IS '敏感词库表';
COMMENT ON COLUMN compliance_keyword.category IS '分类：political=政治, porn=色情, violence=暴力, fraud=欺诈';

-- 4. 违规案例库表
CREATE TABLE IF NOT EXISTS compliance_case (
    id BIGSERIAL PRIMARY KEY,
    rule_id BIGINT NOT NULL,                      -- 关联规则 ID
    case_title VARCHAR(256) NOT NULL,             -- 案例标题
    case_content TEXT NOT NULL,                   -- 案例内容
    violation_reason TEXT,                        -- 违规原因
    punishment_result TEXT,                       -- 处罚结果
    case_source VARCHAR(128),                     -- 案例来源
    case_date DATE,                               -- 案例日期
    status INTEGER DEFAULT 1,                     -- 状态
    deleted INTEGER DEFAULT 0,                    -- 逻辑删除
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (rule_id) REFERENCES compliance_rule(id)
);

-- 索引
CREATE INDEX idx_compliance_case_rule ON compliance_case(rule_id) WHERE deleted = 0;
CREATE INDEX idx_compliance_case_date ON compliance_case(case_date) WHERE deleted = 0;

COMMENT ON TABLE compliance_case IS '违规案例库表';
COMMENT ON COLUMN compliance_case.case_content IS '违规内容示例';
COMMENT ON COLUMN compliance_case.violation_reason IS '违规原因分析';
COMMENT ON COLUMN compliance_case.punishment_result IS '实际处罚结果';
