-- 添加 AI 改进建议字段：低分话术归因后自动生成
ALTER TABLE live_script ADD COLUMN IF NOT EXISTS ai_suggestion TEXT;
COMMENT ON COLUMN live_script.ai_suggestion IS 'AI 自动改进建议（低分话术归因后自动生成）';
