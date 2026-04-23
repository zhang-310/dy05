-- 模板库增强：添加行业标签、自动采集标记
-- 执行时机：Phase 3.2

ALTER TABLE live_script_template ADD COLUMN IF NOT EXISTS industry_tags VARCHAR(512);
ALTER TABLE live_script_template ADD COLUMN IF NOT EXISTS auto_collected INTEGER NOT NULL DEFAULT 0;
ALTER TABLE live_script_template ADD COLUMN IF NOT EXISTS owner_id BIGINT;

COMMENT ON COLUMN live_script_template.industry_tags IS '行业标签（逗号分隔，如 美妆,护肤）';
COMMENT ON COLUMN live_script_template.auto_collected IS '是否自动采集: 0=手动, 1=自动（高效话术沉淀）';
COMMENT ON COLUMN live_script_template.owner_id IS '所属用户 ID（NULL=系统模板，所有人可见）';

CREATE INDEX IF NOT EXISTS idx_lst_owner ON live_script_template(owner_id) WHERE deleted = 0;
