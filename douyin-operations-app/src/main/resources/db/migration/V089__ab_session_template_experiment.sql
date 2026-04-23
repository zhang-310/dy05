-- M-6：直播场次槽位模板 A/B 实验类型 session_template（变体 content JSON 含 templateId）
ALTER TABLE ab_experiment DROP CONSTRAINT IF EXISTS chk_experiment_type;
ALTER TABLE ab_experiment ADD CONSTRAINT chk_experiment_type CHECK (experiment_type IN (
    'video', 'live', 'copy', 'script_style', 'session_template'));

COMMENT ON COLUMN ab_experiment.experiment_type IS '实验类型：含 session_template=直播场次模板结构 A/B（target 常为 live_account）';
