-- ============================================================
-- live 模块 - AI 分析字段
-- ============================================================

ALTER TABLE live_session_data ADD COLUMN IF NOT EXISTS ai_analysis TEXT;
COMMENT ON COLUMN live_session_data.ai_analysis IS 'AI 分析报告（JSON）';
