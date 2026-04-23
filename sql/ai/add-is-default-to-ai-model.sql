-- Add is_default column to ai_model table
ALTER TABLE ai_model ADD COLUMN IF NOT EXISTS is_default INTEGER NOT NULL DEFAULT 0;

COMMENT ON COLUMN ai_model.is_default IS '是否默认模型：0=否 1=是';

-- Create index for is_default
CREATE INDEX IF NOT EXISTS idx_ai_model_is_default ON ai_model (is_default) WHERE deleted = 0;
