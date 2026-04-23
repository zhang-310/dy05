-- V034: live_script 增加 ab_variant_id、prompt_template_id、platform、actual_execution_time（与 Entity 对齐）
ALTER TABLE live_script ADD COLUMN IF NOT EXISTS ab_variant_id BIGINT;
ALTER TABLE live_script ADD COLUMN IF NOT EXISTS prompt_template_id BIGINT;
ALTER TABLE live_script ADD COLUMN IF NOT EXISTS platform VARCHAR(32);
ALTER TABLE live_script ADD COLUMN IF NOT EXISTS actual_execution_time TIMESTAMP;
