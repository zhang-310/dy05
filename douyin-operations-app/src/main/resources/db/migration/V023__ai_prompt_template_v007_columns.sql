-- V023: ai_prompt_template 表补充 V007 其余扩展列（与 Entity 对齐）
ALTER TABLE ai_prompt_template ADD COLUMN IF NOT EXISTS template_code VARCHAR(64);
ALTER TABLE ai_prompt_template ADD COLUMN IF NOT EXISTS variant_name VARCHAR(64) DEFAULT 'default';
ALTER TABLE ai_prompt_template ADD COLUMN IF NOT EXISTS version INTEGER DEFAULT 1;
ALTER TABLE ai_prompt_template ADD COLUMN IF NOT EXISTS system_prompt TEXT;
ALTER TABLE ai_prompt_template ADD COLUMN IF NOT EXISTS user_prompt_tpl TEXT;
ALTER TABLE ai_prompt_template ADD COLUMN IF NOT EXISTS model_hint VARCHAR(64);
ALTER TABLE ai_prompt_template ADD COLUMN IF NOT EXISTS temperature FLOAT DEFAULT 0.7;
ALTER TABLE ai_prompt_template ADD COLUMN IF NOT EXISTS max_tokens INTEGER DEFAULT 2000;
ALTER TABLE ai_prompt_template ADD COLUMN IF NOT EXISTS is_active BOOLEAN DEFAULT TRUE;
ALTER TABLE ai_prompt_template ADD COLUMN IF NOT EXISTS is_default BOOLEAN DEFAULT FALSE;
ALTER TABLE ai_prompt_template ADD COLUMN IF NOT EXISTS usage_count INTEGER DEFAULT 0;
ALTER TABLE ai_prompt_template ADD COLUMN IF NOT EXISTS owner_id BIGINT DEFAULT 0;

CREATE UNIQUE INDEX IF NOT EXISTS idx_prompt_tpl_code_variant_ver
    ON ai_prompt_template(template_code, variant_name, version) WHERE deleted = 0 AND template_code IS NOT NULL;
