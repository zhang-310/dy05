-- ============================================================
-- 话术风格预设管理 - 基础信息迁移
-- 建表 + 初始化 5 个默认预设（中文、话术分类、字数范围等）
-- 执行：psql -h localhost -p 5433 -U postgres -d douyin_operations -f sql/product/migration-style-preset-init.sql
-- ============================================================

-- 1. 建表（与 StylePreset 实体一致）
CREATE TABLE IF NOT EXISTS style_preset (
    id               BIGSERIAL       PRIMARY KEY,
    preset_name      VARCHAR(64)     NOT NULL,
    preset_code      VARCHAR(64)     NOT NULL,
    style_value      VARCHAR(128)     NOT NULL,
    style_tags_json  JSONB,
    category         VARCHAR(32),
    description      TEXT,
    prompt_template  TEXT,
    word_count_min   INTEGER         DEFAULT 150,
    word_count_max   INTEGER         DEFAULT 300,
    sort_order       INTEGER         DEFAULT 0,
    is_enabled       BOOLEAN         DEFAULT TRUE,
    created_by       BIGINT          DEFAULT 0,
    deleted          INTEGER         NOT NULL DEFAULT 0,
    create_time      TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    update_time      TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_style_preset_code UNIQUE (preset_code)
);

-- 2. 索引
CREATE INDEX IF NOT EXISTS idx_style_preset_enabled_sort ON style_preset (is_enabled, sort_order) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_style_preset_category ON style_preset (category) WHERE deleted = 0;

-- 3. 注释
COMMENT ON TABLE  style_preset             IS '话术风格预设表（产品/直播话术生成用）';
COMMENT ON COLUMN style_preset.preset_name IS '预设名称';
COMMENT ON COLUMN style_preset.preset_code IS '预设编码（唯一）';
COMMENT ON COLUMN style_preset.style_value IS '风格值（生成时使用）';
COMMENT ON COLUMN style_preset.category    IS '话术分类：common/product_seed/product_promotion/product_formal/live_opening/live_product/live_transition/live_closing/live_emotional';
COMMENT ON COLUMN style_preset.description IS '风格说明';
COMMENT ON COLUMN style_preset.word_count_min IS '最小字数';
COMMENT ON COLUMN style_preset.word_count_max IS '最大字数';
COMMENT ON COLUMN style_preset.is_enabled  IS '是否启用';
COMMENT ON COLUMN style_preset.deleted     IS '逻辑删除：0=正常 1=已删除';

-- 4. 插入/更新 5 个默认预设（基础信息完整，含话术分类）
INSERT INTO style_preset (
    preset_name, preset_code, style_value, category, description,
    word_count_min, word_count_max, sort_order, is_enabled, created_by, deleted
) VALUES
    ('专业版', 'professional', 'professional', 'product_formal',  '专业严谨，逻辑清晰', 150, 300, 1, TRUE, 0, 0),
    ('亲切版', 'friendly',     'friendly',     'product_seed',    '温暖亲切，像朋友推荐', 150, 300, 2, TRUE, 0, 0),
    ('激情版', 'passionate',   'passionate',   'live_product',    '热情洋溢，感染力强', 150, 300, 3, TRUE, 0, 0),
    ('种草版', 'seeding',      'seeding',      'product_seed',    '真实体验，强调感受', 150, 300, 4, TRUE, 0, 0),
    ('促销版', 'promotion',    'promotion',    'product_promotion', '突出优惠，营造紧迫感', 150, 300, 5, TRUE, 0, 0)
ON CONFLICT (preset_code) DO UPDATE SET
    preset_name      = EXCLUDED.preset_name,
    style_value      = EXCLUDED.style_value,
    category         = EXCLUDED.category,
    description      = EXCLUDED.description,
    word_count_min   = EXCLUDED.word_count_min,
    word_count_max   = EXCLUDED.word_count_max,
    sort_order       = EXCLUDED.sort_order,
    is_enabled       = EXCLUDED.is_enabled,
    update_time      = CURRENT_TIMESTAMP;
