-- 为 live_script_template 添加 template_content 字段
-- 用于存储从场次创建的复合模板结构（JSON）
ALTER TABLE live_script_template ADD COLUMN IF NOT EXISTS template_content TEXT;

COMMENT ON COLUMN live_script_template.template_content IS '模板结构化内容（JSON），用于从场次保存的完整多段话术模板结构';
