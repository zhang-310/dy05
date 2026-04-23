-- ============================================================
-- 短视频模块 - 工作流任务持久化表
-- 版本: v1.0
-- 日期: 2026-03-02
-- 说明: 工作流任务状态持久化，Redis 不可用或重启后可从 DB 恢复
-- 依据: docs/shortvideo/SHORTVIDEO_ITERATION_EVALUATION_2026-03.md P0
-- ============================================================

CREATE TABLE IF NOT EXISTS sv_workflow_task (
    id BIGSERIAL PRIMARY KEY,
    task_id VARCHAR(128) NOT NULL UNIQUE,
    project_id BIGINT,
    owner_id BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'processing',
    current_step VARCHAR(64) DEFAULT 'script',
    progress INT DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_sv_workflow_task_task_id ON sv_workflow_task(task_id);
CREATE INDEX IF NOT EXISTS idx_sv_workflow_task_owner ON sv_workflow_task(owner_id);
CREATE INDEX IF NOT EXISTS idx_sv_workflow_task_project ON sv_workflow_task(project_id);
CREATE INDEX IF NOT EXISTS idx_sv_workflow_task_create ON sv_workflow_task(create_time);

COMMENT ON TABLE sv_workflow_task IS '工作流任务状态（script→shotList→keyframe→videoGen→compose→publish）';
COMMENT ON COLUMN sv_workflow_task.task_id IS '任务 ID，如 wf_xxx';
COMMENT ON COLUMN sv_workflow_task.status IS 'processing/completed/failed';
COMMENT ON COLUMN sv_workflow_task.current_step IS '当前步骤节点 ID';
