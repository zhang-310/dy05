-- ============================================================
-- script 模块 - 增量迁移：为违规词表增加 scope 字段
-- scope: all=全场景 / live_only=仅直播 / video_only=仅短视频
-- ============================================================

-- 1. violation_word 增加 scope
ALTER TABLE violation_word ADD COLUMN IF NOT EXISTS scope VARCHAR(16) NOT NULL DEFAULT 'all';
COMMENT ON COLUMN violation_word.scope IS '适用范围：all=全场景 live_only=仅直播 video_only=仅短视频';

-- 2. sc_user_violation_word 增加 scope
ALTER TABLE sc_user_violation_word ADD COLUMN IF NOT EXISTS scope VARCHAR(16) NOT NULL DEFAULT 'all';
COMMENT ON COLUMN sc_user_violation_word.scope IS '适用范围：all=全场景 live_only=仅直播 video_only=仅短视频';
