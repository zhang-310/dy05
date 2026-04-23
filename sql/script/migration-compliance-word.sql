-- ============================================================
-- 合规词库表 sc_compliance_word
-- 用途：绝对化用语、医疗功效词等，支持运营后台动态管理
-- 替代 ComplianceServiceImpl 中的硬编码词对
-- ============================================================

CREATE TABLE IF NOT EXISTS sc_compliance_word (
    id              BIGSERIAL       PRIMARY KEY,
    word_type       VARCHAR(16)     NOT NULL,                           -- absolute=绝对化用语(可自动修复) medical=医疗功效(不可修复)
    word_value      VARCHAR(128)   NOT NULL,                           -- 待检测词
    replacement     VARCHAR(256),                                       -- 替换建议（absolute 有值，medical 为空）
    is_enabled      INTEGER         NOT NULL DEFAULT 1,                 -- 1=启用 0=禁用
    deleted         INTEGER         NOT NULL DEFAULT 0,                 -- 逻辑删除
    create_time     TIMESTAMP                DEFAULT CURRENT_TIMESTAMP,
    update_time     TIMESTAMP                DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_sc_compliance_word_type ON sc_compliance_word (word_type) WHERE deleted = 0 AND is_enabled = 1;
CREATE UNIQUE INDEX IF NOT EXISTS uk_sc_compliance_word ON sc_compliance_word (word_type, word_value) WHERE deleted = 0;

COMMENT ON TABLE  sc_compliance_word           IS '合规词库：绝对化用语、医疗功效词';
COMMENT ON COLUMN sc_compliance_word.word_type IS 'absolute=可自动修复 medical=不可修复';
COMMENT ON COLUMN sc_compliance_word.word_value IS '待检测词';
COMMENT ON COLUMN sc_compliance_word.replacement IS '替换建议（仅 absolute 类型）';
COMMENT ON COLUMN sc_compliance_word.is_enabled IS '1=启用 0=禁用';

-- 初始化默认数据（与 ComplianceServiceImpl 原硬编码一致，仅当表为空时插入）
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM sc_compliance_word LIMIT 1) THEN
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
        ('medical', '处方', NULL, 1, 0);
  END IF;
END $$;
