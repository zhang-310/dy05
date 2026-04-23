-- ============================================================
-- ai_call_log 效果归因字段迁移
-- 版本：1.1 | 更新日期：2026-02-28
-- ============================================================

ALTER TABLE ai_call_log ADD COLUMN IF NOT EXISTS referenced_chunk_ids TEXT;
ALTER TABLE ai_call_log ADD COLUMN IF NOT EXISTS linked_video_id BIGINT;
ALTER TABLE ai_call_log ADD COLUMN IF NOT EXISTS linked_session_id BIGINT;
ALTER TABLE ai_call_log ADD COLUMN IF NOT EXISTS content_effect VARCHAR(32);
ALTER TABLE ai_call_log ADD COLUMN IF NOT EXISTS effect_score DECIMAL(8,2);

CREATE INDEX IF NOT EXISTS idx_ai_call_log_linked_video ON ai_call_log (linked_video_id) WHERE linked_video_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_ai_call_log_attribution ON ai_call_log (content_effect, create_time DESC) WHERE content_effect IS NOT NULL;

COMMENT ON COLUMN ai_call_log.referenced_chunk_ids IS '本次生成引用的知识条目 ID（JSON 数组）';
COMMENT ON COLUMN ai_call_log.linked_video_id IS '关联的短视频 ID（用户发布后回填）';
COMMENT ON COLUMN ai_call_log.linked_session_id IS '关联的直播场次 ID（用户发布后回填）';
COMMENT ON COLUMN ai_call_log.content_effect IS '内容效果：high_perform/normal/low_perform';
COMMENT ON COLUMN ai_call_log.effect_score IS '效果评分（播放量/均值的比值）';
