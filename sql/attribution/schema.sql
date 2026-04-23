-- ============================================================
-- 效果归因模块表结构
-- ============================================================

-- 归因分析表
CREATE TABLE IF NOT EXISTS attribution (
    id BIGSERIAL PRIMARY KEY,
    session_id BIGINT NOT NULL,
    owner_id BIGINT NOT NULL,

    -- 归因类型: script_sales(话术→销售) / product_gmv(商品→GMV) / overall(综合)
    attribution_type VARCHAR(32) NOT NULL,

    -- 关联ID
    script_id BIGINT,
    product_id BIGINT,

    -- 关联内容快照
    script_content TEXT,
    product_name VARCHAR(256),

    -- 贡献指标
    contributed_gmv DECIMAL(12, 2) DEFAULT 0.00,
    contributed_sales INTEGER DEFAULT 0,
    conversion_rate DECIMAL(5, 4) DEFAULT 0.0000,
    contribution_ratio DECIMAL(5, 4) DEFAULT 0.0000,

    -- 效果评分（0-100）
    effect_score INTEGER DEFAULT 0,

    -- AI 分析
    analysis TEXT,
    model_used VARCHAR(64),
    tokens_used BIGINT DEFAULT 0,

    -- 状态：0=计算中 1=完成 2=失败
    status INTEGER NOT NULL DEFAULT 0,

    deleted INTEGER NOT NULL DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 索引
CREATE INDEX idx_attribution_session ON attribution(session_id, deleted);
CREATE INDEX idx_attribution_owner ON attribution(owner_id, deleted);
CREATE INDEX idx_attribution_type ON attribution(attribution_type, deleted);
CREATE INDEX idx_attribution_script ON attribution(script_id) WHERE script_id IS NOT NULL;
CREATE INDEX idx_attribution_product ON attribution(product_id) WHERE product_id IS NOT NULL;
CREATE INDEX idx_attribution_status ON attribution(status, deleted);

-- 注释
COMMENT ON TABLE attribution IS '效果归因分析表';
COMMENT ON COLUMN attribution.attribution_type IS '归因类型: script_sales / product_gmv / overall';
COMMENT ON COLUMN attribution.contributed_gmv IS '贡献的销售额（元）';
COMMENT ON COLUMN attribution.contributed_sales IS '贡献的销量（件）';
COMMENT ON COLUMN attribution.conversion_rate IS '转化率（0-1）';
COMMENT ON COLUMN attribution.contribution_ratio IS '贡献占比（0-1）';
COMMENT ON COLUMN attribution.effect_score IS '效果评分（0-100）';
COMMENT ON COLUMN attribution.analysis IS 'AI 综合归因分析报告';
COMMENT ON COLUMN attribution.status IS '状态：0=计算中 1=完成 2=失败';
