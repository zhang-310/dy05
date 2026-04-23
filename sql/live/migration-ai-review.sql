-- live_session_data 增加 ai_review_id 字段
-- 关联 AI 模块 ai_live_review 表，用于复盘报告查询
-- 执行日期：2026-03-03

ALTER TABLE live_session_data ADD COLUMN IF NOT EXISTS ai_review_id BIGINT;
COMMENT ON COLUMN live_session_data.ai_review_id IS '关联 AI 复盘报告 ID（ai_live_review.id）';
