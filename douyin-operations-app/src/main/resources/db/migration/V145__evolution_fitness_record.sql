-- 阶段 C：进化/实验适应度结构化记录与血缘（parent_task_id）；供代际对比与小流量评测接入
CREATE TABLE IF NOT EXISTS evolution_fitness_record (
    id              BIGSERIAL PRIMARY KEY,
    kb_id           BIGINT,
    task_id         VARCHAR(64) NOT NULL,
    parent_task_id  VARCHAR(64),
    metric_name     VARCHAR(64) NOT NULL,
    metric_value    DOUBLE PRECISION,
    payload_json    TEXT,
    create_time     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_evo_fit_task ON evolution_fitness_record(task_id);
CREATE INDEX IF NOT EXISTS idx_evo_fit_kb ON evolution_fitness_record(kb_id);
CREATE INDEX IF NOT EXISTS idx_evo_fit_create ON evolution_fitness_record(create_time DESC);

COMMENT ON TABLE evolution_fitness_record IS '进化/实验适应度快照（阶段 C）；业务写入见 EvolutionFitnessService';
