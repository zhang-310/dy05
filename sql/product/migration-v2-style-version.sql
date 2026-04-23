-- ================================================
-- 产品话术 v2.0：按 product_id + script_type + style 版本管理
-- 执行前请确认 dy_product_script 表已存在
-- ================================================

-- 复合索引：支持按风格维度的版本查询
CREATE INDEX IF NOT EXISTS idx_product_script_product_type_style
ON dy_product_script(product_id, script_type, COALESCE(style, ''));

-- 风格预设模板表（Phase 1 可选，供后续扩展）
CREATE TABLE IF NOT EXISTS style_preset (
    id BIGSERIAL PRIMARY KEY,
    preset_name VARCHAR(64) NOT NULL,
    preset_code VARCHAR(64) NOT NULL UNIQUE,
    style_value VARCHAR(128) NOT NULL,
    style_tags_json JSONB,
    category VARCHAR(32),
    description TEXT,
    prompt_template TEXT,
    word_count_min INTEGER DEFAULT 150,
    word_count_max INTEGER DEFAULT 300,
    sort_order INTEGER DEFAULT 0,
    is_enabled BOOLEAN DEFAULT TRUE,
    created_by BIGINT NOT NULL,
    deleted INTEGER NOT NULL DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 初始化核心预设数据
INSERT INTO style_preset (preset_name, preset_code, style_value, category, description, word_count_min, word_count_max, sort_order, created_by)
VALUES
('专业版', 'professional', 'professional', 'default', '专业严谨，逻辑清晰', 150, 300, 1, 0),
('亲切版', 'friendly', 'friendly', 'default', '温暖亲切，像朋友推荐', 150, 300, 2, 0),
('激情版', 'passionate', 'passionate', 'default', '热情洋溢，感染力强', 150, 300, 3, 0),
('种草版', 'seeding', 'seeding', 'default', '真实体验，强调感受', 150, 300, 4, 0),
('促销版', 'promotion', 'promotion', 'default', '突出优惠，营造紧迫感', 150, 300, 5, 0)
ON CONFLICT (preset_code) DO NOTHING;
