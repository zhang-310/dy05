-- PostgreSQL 版：V133 原脚本含 MySQL 语法，此处补列与索引
ALTER TABLE live_script_template
    ADD COLUMN IF NOT EXISTS industry_code VARCHAR(64);
ALTER TABLE live_script_template
    ADD COLUMN IF NOT EXISTS is_preset INTEGER NOT NULL DEFAULT 0;

CREATE INDEX IF NOT EXISTS idx_live_script_template_industry_code
    ON live_script_template (industry_code) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_live_script_template_is_preset
    ON live_script_template (is_preset) WHERE deleted = 0;

COMMENT ON COLUMN live_script_template.industry_code IS '行业编码（cosmetics/food 等），null=通用模板';
COMMENT ON COLUMN live_script_template.is_preset IS '是否系统预置模板（0=用户创建, 1=系统预置）';
