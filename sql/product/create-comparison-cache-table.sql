-- product_script_comparison_cache 表
-- 说明：缓存版本对比、风格对比、排行榜等计算结果，避免重复计算
CREATE TABLE IF NOT EXISTS product_script_comparison_cache (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL,
    comparison_type VARCHAR(64) NOT NULL,
    comparison_data JSONB NOT NULL,
    cached_at TIMESTAMP NOT NULL,
    ttl_minutes INTEGER NOT NULL DEFAULT 60,
    owner_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT uk_cache_composite UNIQUE (product_id, comparison_type, owner_id)
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
