-- P2 R-4：提词备注（不混入话术正文）；P2 G-2：一键生成异步队列载荷与执行模式
ALTER TABLE live_script ADD COLUMN IF NOT EXISTS presenter_notes TEXT;

ALTER TABLE live_generation_task ADD COLUMN IF NOT EXISTS request_payload TEXT;
ALTER TABLE live_generation_task ADD COLUMN IF NOT EXISTS execution_mode VARCHAR(16);

CREATE INDEX IF NOT EXISTS idx_live_gen_task_queued ON live_generation_task(status) WHERE deleted = 0 AND status = 'queued';

COMMENT ON COLUMN live_script.presenter_notes IS '演讲者提词备注（R-4），独立字段';
COMMENT ON COLUMN live_generation_task.request_payload IS '异步一键生成：LiveAiGenerateVO JSON（G-2）';
COMMENT ON COLUMN live_generation_task.execution_mode IS 'inline|sse|rabbit；rabbit=队列执行';
