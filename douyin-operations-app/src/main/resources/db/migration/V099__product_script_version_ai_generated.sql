-- P1 D4-02：话术版本是否由 AI 生成（效果排行榜来源筛选）
ALTER TABLE product_script_version
    ADD COLUMN IF NOT EXISTS ai_generated BOOLEAN NOT NULL DEFAULT FALSE;

COMMENT ON COLUMN product_script_version.ai_generated IS '是否由 AI 生成（人工保存默认 false）';

CREATE INDEX IF NOT EXISTS idx_product_script_version_ai_generated
    ON product_script_version (ai_generated)
    WHERE deleted = 0;
