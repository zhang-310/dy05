-- V108：依据抖音协议/社区自律公约/电商规则中心等公开材料归纳的补充词（非爬取正文；运营请以官网最新版为准）
-- 与 V067、IndustryComplianceServiceImpl.DOUYIN_PUBLIC_RULE_PATTERNS 互补

INSERT INTO sc_compliance_word (word_type, word_value, replacement, is_enabled, deleted)
SELECT 'medical', '包治百病', NULL, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM sc_compliance_word WHERE word_type = 'medical' AND word_value = '包治百病' AND deleted = 0);

INSERT INTO sc_compliance_word (word_type, word_value, replacement, is_enabled, deleted)
SELECT 'medical', '人肉搜索', NULL, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM sc_compliance_word WHERE word_type = 'medical' AND word_value = '人肉搜索' AND deleted = 0);

INSERT INTO sc_compliance_word (word_type, word_value, replacement, is_enabled, deleted)
SELECT 'medical', '开盒挂人', NULL, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM sc_compliance_word WHERE word_type = 'medical' AND word_value = '开盒挂人' AND deleted = 0);

INSERT INTO sc_compliance_word (word_type, word_value, replacement, is_enabled, deleted)
SELECT 'medical', '内部特供', NULL, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM sc_compliance_word WHERE word_type = 'medical' AND word_value = '内部特供' AND deleted = 0);

INSERT INTO sc_compliance_word (word_type, word_value, replacement, is_enabled, deleted)
SELECT 'medical', '机关专供', NULL, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM sc_compliance_word WHERE word_type = 'medical' AND word_value = '机关专供' AND deleted = 0);

INSERT INTO sc_compliance_word (word_type, word_value, replacement, is_enabled, deleted)
SELECT 'medical', '原单尾货', NULL, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM sc_compliance_word WHERE word_type = 'medical' AND word_value = '原单尾货' AND deleted = 0);

INSERT INTO sc_compliance_word (word_type, word_value, replacement, is_enabled, deleted)
SELECT 'medical', 'A货', NULL, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM sc_compliance_word WHERE word_type = 'medical' AND word_value = 'A货' AND deleted = 0);

INSERT INTO sc_compliance_word (word_type, word_value, replacement, is_enabled, deleted)
SELECT 'medical', '高仿', NULL, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM sc_compliance_word WHERE word_type = 'medical' AND word_value = '高仿' AND deleted = 0);

INSERT INTO sc_compliance_word (word_type, word_value, replacement, is_enabled, deleted)
SELECT 'medical', '刷赞', NULL, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM sc_compliance_word WHERE word_type = 'medical' AND word_value = '刷赞' AND deleted = 0);

INSERT INTO sc_compliance_word (word_type, word_value, replacement, is_enabled, deleted)
SELECT 'medical', '刷粉', NULL, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM sc_compliance_word WHERE word_type = 'medical' AND word_value = '刷粉' AND deleted = 0);

INSERT INTO sc_compliance_word (word_type, word_value, replacement, is_enabled, deleted)
SELECT 'medical', '买粉', NULL, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM sc_compliance_word WHERE word_type = 'medical' AND word_value = '买粉' AND deleted = 0);

INSERT INTO sc_compliance_word (word_type, word_value, replacement, is_enabled, deleted)
SELECT 'medical', '虚构原价', NULL, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM sc_compliance_word WHERE word_type = 'medical' AND word_value = '虚构原价' AND deleted = 0);

INSERT INTO sc_compliance_word (word_type, word_value, replacement, is_enabled, deleted)
SELECT 'medical', '虚假折扣', NULL, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM sc_compliance_word WHERE word_type = 'medical' AND word_value = '虚假折扣' AND deleted = 0);

INSERT INTO sc_compliance_word (word_type, word_value, replacement, is_enabled, deleted)
SELECT 'medical', '偏方祛斑', NULL, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM sc_compliance_word WHERE word_type = 'medical' AND word_value = '偏方祛斑' AND deleted = 0);

INSERT INTO sc_compliance_word (word_type, word_value, replacement, is_enabled, deleted)
SELECT 'absolute', '亏本甩卖', '促销', 1, 0
WHERE NOT EXISTS (SELECT 1 FROM sc_compliance_word WHERE word_type = 'absolute' AND word_value = '亏本甩卖' AND deleted = 0);

INSERT INTO sc_compliance_word (word_type, word_value, replacement, is_enabled, deleted)
SELECT 'absolute', '跳楼价', '优惠价', 1, 0
WHERE NOT EXISTS (SELECT 1 FROM sc_compliance_word WHERE word_type = 'absolute' AND word_value = '跳楼价' AND deleted = 0);
