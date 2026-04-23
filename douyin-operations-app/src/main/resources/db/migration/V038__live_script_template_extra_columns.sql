-- V038: live_script_template 增加 industry_tags、auto_collected、template_content、owner_id（与 Entity 对齐）
ALTER TABLE live_script_template ADD COLUMN IF NOT EXISTS industry_tags VARCHAR(512);
ALTER TABLE live_script_template ADD COLUMN IF NOT EXISTS auto_collected INTEGER NOT NULL DEFAULT 0;
ALTER TABLE live_script_template ADD COLUMN IF NOT EXISTS template_content TEXT;
ALTER TABLE live_script_template ADD COLUMN IF NOT EXISTS owner_id BIGINT;
