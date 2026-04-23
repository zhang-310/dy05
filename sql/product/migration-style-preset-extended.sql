-- ============================================================
-- 话术风格预设 - 扩展：鸡汤类、别具一格类
-- 执行：Get-Content -Encoding UTF8 "sql/product/migration-style-preset-extended.sql" | docker exec -i dy-postgres psql -U postgres -d douyin_operations
-- ============================================================

-- 鸡汤类（情绪话术，拉停留促共鸣）
INSERT INTO style_preset (
    preset_name, preset_code, style_value, category, description,
    word_count_min, word_count_max, sort_order, is_enabled, created_by, deleted
) VALUES
    ('高鸡汤', 'chicken_soup', 'chicken_soup', 'live_emotional', '励志治愈，心理共鸣，拉高停留', 80, 200, 6, TRUE, 0, 0),
    ('高歇后语', 'proverb', 'proverb', 'live_emotional', '俗语智慧，幽默接地气', 60, 150, 7, TRUE, 0, 0),
    ('高扎心', 'heart_piercing', 'heart_piercing', 'live_emotional', '直击痛点，现实共鸣', 80, 200, 8, TRUE, 0, 0),
    ('搞笑幽默', 'humorous', 'humorous', 'live_emotional', '轻松搞笑，拉近距离', 80, 200, 9, TRUE, 0, 0)
ON CONFLICT (preset_code) DO UPDATE SET
    preset_name=EXCLUDED.preset_name, style_value=EXCLUDED.style_value, category=EXCLUDED.category,
    description=EXCLUDED.description, word_count_min=EXCLUDED.word_count_min, word_count_max=EXCLUDED.word_count_max,
    sort_order=EXCLUDED.sort_order, is_enabled=EXCLUDED.is_enabled, update_time=CURRENT_TIMESTAMP;

-- 别具一格类（特殊风格，独树一帜）
INSERT INTO style_preset (
    preset_name, preset_code, style_value, category, description,
    word_count_min, word_count_max, sort_order, is_enabled, created_by, deleted
) VALUES
    ('人设特色', 'persona_flavor', 'persona_flavor', 'live_special', '强化人设，独树一帜', 100, 250, 10, TRUE, 0, 0),
    ('地方特色', 'local_flavor', 'local_flavor', 'live_special', '方言俚语，地域共鸣', 80, 200, 11, TRUE, 0, 0),
    ('高创意', 'creative', 'creative', 'live_special', '出其不意，记忆点强', 100, 300, 12, TRUE, 0, 0)
ON CONFLICT (preset_code) DO UPDATE SET
    preset_name=EXCLUDED.preset_name, style_value=EXCLUDED.style_value, category=EXCLUDED.category,
    description=EXCLUDED.description, word_count_min=EXCLUDED.word_count_min, word_count_max=EXCLUDED.word_count_max,
    sort_order=EXCLUDED.sort_order, is_enabled=EXCLUDED.is_enabled, update_time=CURRENT_TIMESTAMP;
