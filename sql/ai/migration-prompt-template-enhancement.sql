-- Prompt 模板增强：添加最后使用时间和标签字段
-- 日期: 2026-04-22

-- 添加最后使用时间字段
ALTER TABLE ai_prompt_template ADD COLUMN IF NOT EXISTS last_used_at TIMESTAMP NULL;
COMMENT ON COLUMN ai_prompt_template.last_used_at IS '最后使用时间';

-- 添加标签字段
ALTER TABLE ai_prompt_template ADD COLUMN IF NOT EXISTS tags TEXT NULL;
COMMENT ON COLUMN ai_prompt_template.tags IS '标签（JSON 数组）';

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_ai_prompt_template_last_used ON ai_prompt_template(last_used_at);
CREATE INDEX IF NOT EXISTS idx_ai_prompt_template_usage_count ON ai_prompt_template(usage_count);

-- 验证
SELECT
    COUNT(*) as total_templates,
    COUNT(last_used_at) as has_last_used,
    COUNT(tags) as has_tags
FROM ai_prompt_template
WHERE deleted = 0;
