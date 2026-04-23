-- product_script_effectiveness_record 表
-- 说明：记录每个话术版本的效果评分历史和快照数据
CREATE TABLE IF NOT EXISTS product_script_effectiveness_record (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL,
    script_version_id BIGINT NOT NULL,
    calculated_at TIMESTAMP NOT NULL,
    score_value DECIMAL(5, 2) NOT NULL,
    score_level VARCHAR(10),
    usage_count_snapshot INTEGER NOT NULL DEFAULT 0,
    conversion_rate_snapshot DECIMAL(5, 2),
    likes_snapshot INTEGER NOT NULL DEFAULT 0,
    comments_snapshot INTEGER NOT NULL DEFAULT 0,
    owner_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER NOT NULL DEFAULT 0
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
