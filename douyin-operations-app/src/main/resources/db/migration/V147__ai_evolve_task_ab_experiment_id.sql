-- Phase 2：进化任务可选 AB/实验 ID；与主题 category 约定 ab:{id} 或接口写入对齐；适应度 experiment_id 同源
ALTER TABLE ai_evolve_task
    ADD COLUMN IF NOT EXISTS ab_experiment_id VARCHAR(64);

CREATE INDEX IF NOT EXISTS idx_ai_evolve_task_ab_exp ON ai_evolve_task (ab_experiment_id)
    WHERE ab_experiment_id IS NOT NULL;

COMMENT ON COLUMN ai_evolve_task.ab_experiment_id IS '可选：AB/实验 ID，写入 evolution_fitness_record.experiment_id；可由主题 category 前缀 ab: 解析';
