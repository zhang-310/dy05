-- ============================================================
-- 商品话术版本效果评分系统 (W-04)
-- 包含：效果评分历史、对比缓存、相关索引
-- ============================================================

-- product_script_effectiveness_record 表
-- 说明：记录每个话术版本的效果评分历史和快照数据
CREATE TABLE IF NOT EXISTS product_script_effectiveness_record (
    id                              BIGSERIAL           PRIMARY KEY,
    product_id                      BIGINT              NOT NULL,                      -- 所属产品 ID
    script_version_id               BIGINT              NOT NULL,                      -- 话术版本 ID（product_script_version.id）
    calculated_at                   TIMESTAMP           NOT NULL,                      -- 计算时间
    score_value                     DECIMAL(5, 2)       NOT NULL,                      -- 评分值（0-100）
    score_level                     VARCHAR(10),                                        -- 评分等级（A/B/C/D/F）
    usage_count_snapshot            INTEGER             NOT NULL DEFAULT 0,            -- 快照：使用次数
    conversion_rate_snapshot        DECIMAL(5, 2),                                     -- 快照：转化率（百分比）
    likes_snapshot                  INTEGER             NOT NULL DEFAULT 0,            -- 快照：点赞数
    comments_snapshot               INTEGER             NOT NULL DEFAULT 0,            -- 快照：评论数
    owner_id                        BIGINT              NOT NULL,                      -- 所有者 ID（数据隔离）
    created_at                      TIMESTAMP           NOT NULL DEFAULT CURRENT_TIMESTAMP, -- 创建时间
    updated_at                      TIMESTAMP           NOT NULL DEFAULT CURRENT_TIMESTAMP, -- 更新时间
    deleted                         INTEGER             NOT NULL DEFAULT 0,            -- 逻辑删除：0=正常 1=已删除
    CONSTRAINT fk_record_product    FOREIGN KEY (product_id) REFERENCES dy_product (id),
    CONSTRAINT fk_record_version    FOREIGN KEY (script_version_id) REFERENCES product_script_version (id)
);

CREATE INDEX IF NOT EXISTS idx_effectiveness_record_product_id
    ON product_script_effectiveness_record (product_id);
CREATE INDEX IF NOT EXISTS idx_effectiveness_record_script_version_id
    ON product_script_effectiveness_record (script_version_id);
CREATE INDEX IF NOT EXISTS idx_effectiveness_record_calculated_at
    ON product_script_effectiveness_record (calculated_at DESC);
CREATE INDEX IF NOT EXISTS idx_effectiveness_record_score_value
    ON product_script_effectiveness_record (score_value DESC);
CREATE INDEX IF NOT EXISTS idx_effectiveness_record_owner_id
    ON product_script_effectiveness_record (owner_id);
CREATE INDEX IF NOT EXISTS idx_effectiveness_record_composite
    ON product_script_effectiveness_record (product_id, script_version_id, calculated_at DESC);

COMMENT ON TABLE  product_script_effectiveness_record
    IS '商品话术效果评分历史表（记录每次计算的评分和快照数据）';
COMMENT ON COLUMN product_script_effectiveness_record.product_id
    IS '所属产品 ID';
COMMENT ON COLUMN product_script_effectiveness_record.script_version_id
    IS '话术版本 ID';
COMMENT ON COLUMN product_script_effectiveness_record.calculated_at
    IS '评分计算时间';
COMMENT ON COLUMN product_script_effectiveness_record.score_value
    IS '评分值（0-100 分）';
COMMENT ON COLUMN product_script_effectiveness_record.score_level
    IS '评分等级（A/B/C/D/F）';
COMMENT ON COLUMN product_script_effectiveness_record.usage_count_snapshot
    IS '计算时的使用次数快照';
COMMENT ON COLUMN product_script_effectiveness_record.conversion_rate_snapshot
    IS '计算时的转化率快照';
COMMENT ON COLUMN product_script_effectiveness_record.likes_snapshot
    IS '计算时的点赞数快照';
COMMENT ON COLUMN product_script_effectiveness_record.comments_snapshot
    IS '计算时的评论数快照';
COMMENT ON COLUMN product_script_effectiveness_record.owner_id
    IS '所有者 ID（用于数据隔离）';


-- product_script_comparison_cache 表
-- 说明：缓存版本对比、风格对比、排行榜等计算结果，避免重复计算
CREATE TABLE IF NOT EXISTS product_script_comparison_cache (
    id                              BIGSERIAL           PRIMARY KEY,
    product_id                      BIGINT              NOT NULL,                      -- 所属产品 ID
    comparison_type                 VARCHAR(64)         NOT NULL,                      -- 对比类型（version_compare/style_compare/ranking）
    comparison_data                 JSONB               NOT NULL,                      -- 对比结果（JSONB 格式）
    cached_at                       TIMESTAMP           NOT NULL,                      -- 缓存时间
    ttl_minutes                     INTEGER             NOT NULL DEFAULT 60,           -- 缓存 TTL（分钟）
    owner_id                        BIGINT              NOT NULL,                      -- 所有者 ID（数据隔离）
    created_at                      TIMESTAMP           NOT NULL DEFAULT CURRENT_TIMESTAMP, -- 创建时间
    updated_at                      TIMESTAMP           NOT NULL DEFAULT CURRENT_TIMESTAMP, -- 更新时间
    deleted                         INTEGER             NOT NULL DEFAULT 0,            -- 逻辑删除：0=正常 1=已删除
    CONSTRAINT fk_cache_product     FOREIGN KEY (product_id) REFERENCES dy_product (id),
    CONSTRAINT uk_cache_composite   UNIQUE (product_id, comparison_type, owner_id)
);

CREATE INDEX IF NOT EXISTS idx_comparison_cache_product_id
    ON product_script_comparison_cache (product_id);
CREATE INDEX IF NOT EXISTS idx_comparison_cache_type
    ON product_script_comparison_cache (comparison_type);
CREATE INDEX IF NOT EXISTS idx_comparison_cache_owner_id
    ON product_script_comparison_cache (owner_id);
CREATE INDEX IF NOT EXISTS idx_comparison_cache_cached_at
    ON product_script_comparison_cache (cached_at DESC);
CREATE INDEX IF NOT EXISTS idx_comparison_cache_composite
    ON product_script_comparison_cache (product_id, comparison_type, cached_at DESC);

COMMENT ON TABLE  product_script_comparison_cache
    IS '商品话术对比缓存表（存储版本对比、风格对比、排行榜等计算结果）';
COMMENT ON COLUMN product_script_comparison_cache.product_id
    IS '所属产品 ID';
COMMENT ON COLUMN product_script_comparison_cache.comparison_type
    IS '对比类型（version_compare/style_compare/ranking 等）';
COMMENT ON COLUMN product_script_comparison_cache.comparison_data
    IS '对比结果数据（JSONB 格式存储）';
COMMENT ON COLUMN product_script_comparison_cache.cached_at
    IS '缓存生成时间';
COMMENT ON COLUMN product_script_comparison_cache.ttl_minutes
    IS '缓存有效期（分钟）';
COMMENT ON COLUMN product_script_comparison_cache.owner_id
    IS '所有者 ID（用于数据隔离）';
