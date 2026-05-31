-- V196: 短视频扩展字段
ALTER TABLE douyin_video ADD COLUMN IF NOT EXISTS script_id BIGINT;
ALTER TABLE douyin_video ADD COLUMN IF NOT EXISTS campaign_id BIGINT;
ALTER TABLE douyin_video ADD COLUMN IF NOT EXISTS ai_analysis_json TEXT;
ALTER TABLE douyin_video ADD COLUMN IF NOT EXISTS viral_score DOUBLE PRECISION;

CREATE INDEX IF NOT EXISTS idx_video_script ON douyin_video(script_id) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_video_viral ON douyin_video(viral_score DESC) WHERE deleted = 0 AND viral_score IS NOT NULL;
