-- sv_viral_video 新增丰富元数据 + BOS 存储字段
-- 对应 Flyway V109__viral_video_enriched_metadata.sql

ALTER TABLE sv_viral_video ADD COLUMN IF NOT EXISTS favorite_count BIGINT DEFAULT 0;
ALTER TABLE sv_viral_video ADD COLUMN IF NOT EXISTS comment_count BIGINT DEFAULT 0;
ALTER TABLE sv_viral_video ADD COLUMN IF NOT EXISTS video_duration INTEGER DEFAULT 0;
ALTER TABLE sv_viral_video ADD COLUMN IF NOT EXISTS description TEXT;
ALTER TABLE sv_viral_video ADD COLUMN IF NOT EXISTS author_id VARCHAR(128);
ALTER TABLE sv_viral_video ADD COLUMN IF NOT EXISTS author_followers BIGINT;
ALTER TABLE sv_viral_video ADD COLUMN IF NOT EXISTS music_name VARCHAR(256);
ALTER TABLE sv_viral_video ADD COLUMN IF NOT EXISTS hashtags TEXT;
ALTER TABLE sv_viral_video ADD COLUMN IF NOT EXISTS video_bos_key VARCHAR(500);
ALTER TABLE sv_viral_video ADD COLUMN IF NOT EXISTS video_bos_url VARCHAR(500);
ALTER TABLE sv_viral_video ADD COLUMN IF NOT EXISTS keyframe_bos_keys TEXT;
ALTER TABLE sv_viral_video ADD COLUMN IF NOT EXISTS keyframe_bos_urls TEXT;
ALTER TABLE sv_viral_video ADD COLUMN IF NOT EXISTS cover_bos_key VARCHAR(500);
ALTER TABLE sv_viral_video ADD COLUMN IF NOT EXISTS cover_bos_url VARCHAR(500);
ALTER TABLE sv_viral_video ADD COLUMN IF NOT EXISTS metadata_json TEXT;

ALTER TABLE sv_comment ADD COLUMN IF NOT EXISTS video_source VARCHAR(16) DEFAULT 'sv_video';
CREATE INDEX IF NOT EXISTS idx_sv_comment_viral ON sv_comment(video_id, video_source) WHERE video_source = 'viral';
