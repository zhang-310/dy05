-- LF-05：深度分析异步任务状态（与 Flyway V106 对齐）

ALTER TABLE sv_viral_video ADD COLUMN IF NOT EXISTS deep_analyze_status VARCHAR(32);
COMMENT ON COLUMN sv_viral_video.deep_analyze_status IS 'idle/queued/processing/completed/failed';

ALTER TABLE sv_viral_video ADD COLUMN IF NOT EXISTS deep_analyze_error TEXT;
COMMENT ON COLUMN sv_viral_video.deep_analyze_error IS '深度分析失败原因';

ALTER TABLE sv_viral_video ADD COLUMN IF NOT EXISTS deep_analyze_started_at TIMESTAMP;
COMMENT ON COLUMN sv_viral_video.deep_analyze_started_at IS '深度分析开始时间';

ALTER TABLE sv_viral_video ADD COLUMN IF NOT EXISTS deep_analyze_finished_at TIMESTAMP;
COMMENT ON COLUMN sv_viral_video.deep_analyze_finished_at IS '深度分析结束时间';
