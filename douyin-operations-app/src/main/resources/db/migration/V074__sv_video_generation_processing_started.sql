-- 图生视频任务：进入 processing 的时间点，用于超时检测与可观测性
ALTER TABLE sv_video_generation_task
    ADD COLUMN IF NOT EXISTS processing_started_at TIMESTAMP;

COMMENT ON COLUMN sv_video_generation_task.processing_started_at IS '进入 processing 状态的时间，NULL 表示未开始或已结束';
