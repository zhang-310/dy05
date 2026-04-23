-- Phase 2.2 异步任务队列 - 数据库迁移
-- 依据: docs/IMPLEMENTATION_CHECKLIST.md 2.2

CREATE TABLE IF NOT EXISTS sv_video_generation_task (
    id BIGSERIAL PRIMARY KEY,
    owner_id BIGINT NOT NULL,
    project_id BIGINT,
    shot_list_id BIGINT,
    request_json TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'pending',
    progress_current INT DEFAULT 0,
    progress_total INT DEFAULT 0,
    result_json TEXT,
    error_message TEXT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_sv_video_task_owner ON sv_video_generation_task(owner_id);
CREATE INDEX IF NOT EXISTS idx_sv_video_task_status ON sv_video_generation_task(status);
CREATE INDEX IF NOT EXISTS idx_sv_video_task_create ON sv_video_generation_task(create_time);

COMMENT ON TABLE sv_video_generation_task IS '图生视频异步任务 (Phase 2.2)';
COMMENT ON COLUMN sv_video_generation_task.request_json IS 'Img2VideoInput 列表 JSON';
COMMENT ON COLUMN sv_video_generation_task.result_json IS 'VideoResult 列表 JSON';
COMMENT ON COLUMN sv_video_generation_task.status IS 'pending/processing/completed/failed/cancelled';
