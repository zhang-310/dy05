-- Prompt 自优化日志表
-- 记录每次 meta-prompt 分析与优化结果
CREATE TABLE IF NOT EXISTS ai_prompt_optimization_log (
    id          BIGSERIAL PRIMARY KEY,
    task_type   VARCHAR(50)  NOT NULL,
    original_prompt_hash VARCHAR(64),
    optimized_prompt_hash VARCHAR(64),
    improvement_pct DECIMAL(5,2),
    trigger_reason VARCHAR(100),
    details_json TEXT,
    user_id     BIGINT,
    deleted     INTEGER      NOT NULL DEFAULT 0,
    create_time TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_prompt_opt_log_task_type ON ai_prompt_optimization_log(task_type);
CREATE INDEX IF NOT EXISTS idx_prompt_opt_log_user_id ON ai_prompt_optimization_log(user_id);
