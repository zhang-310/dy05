-- V109: sv_viral_video 新增丰富元数据 + BOS 存储 + 评论来源字段
-- 支持深度分析时的全量数据入库（互动数据、作者信息、BOS 持久化、全量评论）

-- 互动数据
ALTER TABLE sv_viral_video ADD COLUMN IF NOT EXISTS favorite_count BIGINT DEFAULT 0;
ALTER TABLE sv_viral_video ADD COLUMN IF NOT EXISTS comment_count BIGINT DEFAULT 0;

-- 视频元信息
ALTER TABLE sv_viral_video ADD COLUMN IF NOT EXISTS video_duration INTEGER DEFAULT 0;
ALTER TABLE sv_viral_video ADD COLUMN IF NOT EXISTS description TEXT;

-- 作者信息
ALTER TABLE sv_viral_video ADD COLUMN IF NOT EXISTS author_id VARCHAR(128);
ALTER TABLE sv_viral_video ADD COLUMN IF NOT EXISTS author_followers BIGINT;

-- 音乐与标签
ALTER TABLE sv_viral_video ADD COLUMN IF NOT EXISTS music_name VARCHAR(256);
ALTER TABLE sv_viral_video ADD COLUMN IF NOT EXISTS hashtags TEXT;

-- BOS 持久化
ALTER TABLE sv_viral_video ADD COLUMN IF NOT EXISTS video_bos_key VARCHAR(500);
ALTER TABLE sv_viral_video ADD COLUMN IF NOT EXISTS video_bos_url VARCHAR(500);
ALTER TABLE sv_viral_video ADD COLUMN IF NOT EXISTS keyframe_bos_keys TEXT;
ALTER TABLE sv_viral_video ADD COLUMN IF NOT EXISTS keyframe_bos_urls TEXT;
ALTER TABLE sv_viral_video ADD COLUMN IF NOT EXISTS cover_bos_key VARCHAR(500);
ALTER TABLE sv_viral_video ADD COLUMN IF NOT EXISTS cover_bos_url VARCHAR(500);

-- 原始元数据 JSON（yt-dlp / Playwright 提取的完整 dump）
ALTER TABLE sv_viral_video ADD COLUMN IF NOT EXISTS metadata_json TEXT;

-- sv_comment 新增 video_source 区分爆款 vs 普通视频评论
ALTER TABLE sv_comment ADD COLUMN IF NOT EXISTS video_source VARCHAR(16) DEFAULT 'sv_video';
CREATE INDEX IF NOT EXISTS idx_sv_comment_viral ON sv_comment(video_id, video_source) WHERE video_source = 'viral';
