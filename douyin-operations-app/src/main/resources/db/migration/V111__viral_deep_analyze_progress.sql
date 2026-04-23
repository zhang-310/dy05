-- LF-05 细粒度深度分析进度（JSON），与 deep_analyze_steps 摘要并存

ALTER TABLE sv_viral_video ADD COLUMN IF NOT EXISTS deep_analyze_progress TEXT;
COMMENT ON COLUMN sv_viral_video.deep_analyze_progress IS '深度分析管线细粒度进度 JSON（步骤状态/耗时/详情）';
