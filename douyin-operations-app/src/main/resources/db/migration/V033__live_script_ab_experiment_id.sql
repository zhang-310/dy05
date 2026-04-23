-- V033: live_script 增加 ab_experiment_id 列（A/B 实验关联）
ALTER TABLE live_script ADD COLUMN IF NOT EXISTS ab_experiment_id BIGINT;
