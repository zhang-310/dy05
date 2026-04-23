-- E-4：进化任务 DAG 依赖（前置 task_no 列表，应用层拓扑与环检测）
ALTER TABLE ai_evolve_task
    ADD COLUMN IF NOT EXISTS depends_on_task_nos TEXT,
    ADD COLUMN IF NOT EXISTS blocked_reason VARCHAR(512);

COMMENT ON COLUMN ai_evolve_task.depends_on_task_nos IS '前置任务 task_no，逗号分隔；全部 completed 后解锁执行';
COMMENT ON COLUMN ai_evolve_task.blocked_reason IS 'status=blocked 时的说明（如等待前置任务）';

CREATE INDEX IF NOT EXISTS idx_ai_evolve_task_kb_blocked ON ai_evolve_task (kb_id, status, create_time DESC)
    WHERE status = 'blocked';
