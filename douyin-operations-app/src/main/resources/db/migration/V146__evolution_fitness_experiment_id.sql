-- Wave3：AB/实验维度可选外键（与 payload 中 abExperimentId 可同时存在，列便于筛选）
ALTER TABLE evolution_fitness_record
    ADD COLUMN IF NOT EXISTS experiment_id VARCHAR(64);

CREATE INDEX IF NOT EXISTS idx_evo_fit_experiment ON evolution_fitness_record (experiment_id)
    WHERE experiment_id IS NOT NULL;

COMMENT ON COLUMN evolution_fitness_record.experiment_id IS '可选：AB/实验 ID（与 payload_json.abExperimentId 对齐）';
