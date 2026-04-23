-- Seed sc_compliance_word (run only when table is empty or missing rows)
INSERT INTO sc_compliance_word (word_type, word_value, replacement, is_enabled, deleted)
VALUES
    ('absolute', '最好', '优质', 1, 0),
    ('absolute', '第一', '领先', 1, 0),
    ('absolute', '唯一', '优选', 1, 0),
    ('absolute', '国家级', '高品质', 1, 0),
    ('absolute', '最高级', '高级', 1, 0),
    ('absolute', '顶级', '高端', 1, 0),
    ('absolute', '极致', '出色', 1, 0),
    ('absolute', '100%', '高', 1, 0),
    ('absolute', '百分百', '高', 1, 0),
    ('medical', '治疗', NULL, 1, 0),
    ('medical', '根治', NULL, 1, 0),
    ('medical', '药效', NULL, 1, 0),
    ('medical', '疗效', NULL, 1, 0),
    ('medical', '治愈', NULL, 1, 0),
    ('medical', '药用', NULL, 1, 0),
    ('medical', '处方', NULL, 1, 0)
ON CONFLICT (word_type, word_value) WHERE deleted = 0 DO NOTHING;
