-- 话术使用日志关联 A/B 实验（归因用）
ALTER TABLE script_usage_log ADD COLUMN IF NOT EXISTS ab_experiment_id BIGINT;
ALTER TABLE script_usage_log ADD COLUMN IF NOT EXISTS ab_variant_id BIGINT;
COMMENT ON COLUMN script_usage_log.ab_experiment_id IS 'A/B 实验 ID（若该次使用属于某实验）';
COMMENT ON COLUMN script_usage_log.ab_variant_id IS 'A/B 变体 ID（若该次使用属于某实验）';
CREATE INDEX IF NOT EXISTS idx_script_usage_log_ab ON script_usage_log(ab_experiment_id, ab_variant_id) WHERE ab_experiment_id IS NOT NULL;
