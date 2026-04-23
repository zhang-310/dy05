-- V133: 直播话术模板新增行业编码和预置标记字段（P3-04 行业模板扩展）
-- 支持按行业（护肤/彩妆/食品/服饰等）筛选和预置系统模板
-- PostgreSQL：列注释使用 COMMENT ON COLUMN，不支持 MySQL 行内 COMMENT

ALTER TABLE live_script_template
    ADD COLUMN IF NOT EXISTS industry_code VARCHAR(64) DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS is_preset INTEGER DEFAULT 0 NOT NULL;

COMMENT ON COLUMN live_script_template.industry_code IS '行业编码（cosmetics/food/clothing/jewelry/digital等），null=通用模板';
COMMENT ON COLUMN live_script_template.is_preset IS '是否系统预置模板（0=用户创建, 1=系统预置）';

-- 为行业编码创建索引（方便按行业筛选）
CREATE INDEX IF NOT EXISTS idx_live_script_template_industry_code
    ON live_script_template (industry_code) WHERE deleted = 0;

-- 为预置模板创建索引
CREATE INDEX IF NOT EXISTS idx_live_script_template_is_preset
    ON live_script_template (is_preset) WHERE deleted = 0;
