-- ================================================
-- A/B 测试框架 #28：话术风格实验（随机分配风格，对比转化率）
-- ================================================

-- 实验类型增加 script_style
ALTER TABLE ab_experiment DROP CONSTRAINT IF EXISTS chk_experiment_type;
ALTER TABLE ab_experiment ADD CONSTRAINT chk_experiment_type
  CHECK (experiment_type IN ('video', 'live', 'copy', 'script_style'));

-- 实验关联目标（话术风格实验时：product_id 或 session_id）
ALTER TABLE ab_experiment ADD COLUMN IF NOT EXISTS target_entity_type VARCHAR(32);
ALTER TABLE ab_experiment ADD COLUMN IF NOT EXISTS target_entity_id BIGINT;
COMMENT ON COLUMN ab_experiment.target_entity_type IS '目标实体类型：product=产品话术 live_session=直播场次';
COMMENT ON COLUMN ab_experiment.target_entity_id IS '目标实体 ID';

-- 变体存储风格编码（script_style 实验时 A/B 各对应一个 style）
ALTER TABLE ab_variant ADD COLUMN IF NOT EXISTS style_code VARCHAR(64);
COMMENT ON COLUMN ab_variant.style_code IS '话术风格编码（script_style 实验时：professional/friendly 等）';

CREATE INDEX IF NOT EXISTS idx_ab_experiment_target ON ab_experiment(target_entity_type, target_entity_id) WHERE target_entity_type IS NOT NULL;
