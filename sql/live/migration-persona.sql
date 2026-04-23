-- ============================================================
-- live 模块 - persona_id 字段迁移
-- ============================================================
ALTER TABLE live_session ADD COLUMN IF NOT EXISTS persona_id BIGINT;
COMMENT ON COLUMN live_session.persona_id IS '关联人设 ID（dy_persona.id）';
CREATE INDEX IF NOT EXISTS idx_live_session_persona ON live_session(persona_id) WHERE deleted = 0;

-- live_script 添加 script_type 和 style 字段
ALTER TABLE live_script ADD COLUMN IF NOT EXISTS script_type VARCHAR(32) DEFAULT 'custom';
ALTER TABLE live_script ADD COLUMN IF NOT EXISTS style VARCHAR(64);
ALTER TABLE live_script ADD COLUMN IF NOT EXISTS ai_generated INTEGER DEFAULT 0;
COMMENT ON COLUMN live_script.script_type IS '话术类型：opening/product/transition/closing/full/custom';
COMMENT ON COLUMN live_script.style IS '话术风格';
COMMENT ON COLUMN live_script.ai_generated IS '是否AI生成：0=否 1=是';
