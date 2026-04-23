-- ============================================================
-- 风格预设按话术分类细化
-- 为 style_preset 的 category 字段填充话术分类
-- 分类值：common/product_seed/product_promotion/product_formal/live_opening/live_product/live_transition/live_closing/live_emotional
-- ============================================================

-- 更新内置预设的 category（原 default 改为具体话术分类）
UPDATE style_preset SET category = 'product_formal'  WHERE preset_code = 'professional' AND deleted = 0;
UPDATE style_preset SET category = 'product_seed'     WHERE preset_code = 'friendly' AND deleted = 0;
UPDATE style_preset SET category = 'live_product'    WHERE preset_code = 'passionate' AND deleted = 0;
UPDATE style_preset SET category = 'product_seed'     WHERE preset_code = 'seeding' AND deleted = 0;
UPDATE style_preset SET category = 'product_promotion' WHERE preset_code = 'promotion' AND deleted = 0;

-- 将空 category 设为 common（通用）
UPDATE style_preset SET category = 'common' WHERE (category IS NULL OR category = '' OR category = 'default') AND deleted = 0;
