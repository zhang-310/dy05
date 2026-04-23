-- A/B 测试集成：live_script 添加实验追踪字段
-- 执行时机：Phase 3.3

ALTER TABLE live_script ADD COLUMN IF NOT EXISTS ab_experiment_id BIGINT;
ALTER TABLE live_script ADD COLUMN IF NOT EXISTS ab_variant_id BIGINT;

COMMENT ON COLUMN live_script.ab_experiment_id IS 'A/B 实验 ID（生成时分配）';
COMMENT ON COLUMN live_script.ab_variant_id IS 'A/B 变体 ID（生成时分配）';

CREATE INDEX IF NOT EXISTS idx_live_script_ab ON live_script(ab_experiment_id) WHERE deleted = 0 AND ab_experiment_id IS NOT NULL;
