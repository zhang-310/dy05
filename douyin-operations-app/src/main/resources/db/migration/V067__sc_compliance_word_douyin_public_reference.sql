-- 依据抖音/抖音电商等「公开规则说明」中常见违规类型归纳的补充词库（非爬虫实时页；运营请以官网最新版为准）
-- 与 RiskWarningServiceImpl、ComplianceServiceImpl、ComplianceWordService 共用 sc_compliance_word

-- 引导私下交易 / 脱离平台（medical 类型在此表示「高风险不可自动替换」，与医疗词共用检测通道）
INSERT INTO sc_compliance_word (word_type, word_value, replacement, is_enabled, deleted)
SELECT 'medical', '私下交易', NULL, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM sc_compliance_word WHERE word_type = 'medical' AND word_value = '私下交易' AND deleted = 0);
INSERT INTO sc_compliance_word (word_type, word_value, replacement, is_enabled, deleted)
SELECT 'medical', '线下付款', NULL, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM sc_compliance_word WHERE word_type = 'medical' AND word_value = '线下付款' AND deleted = 0);
INSERT INTO sc_compliance_word (word_type, word_value, replacement, is_enabled, deleted)
SELECT 'medical', '私信转账', NULL, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM sc_compliance_word WHERE word_type = 'medical' AND word_value = '私信转账' AND deleted = 0);
INSERT INTO sc_compliance_word (word_type, word_value, replacement, is_enabled, deleted)
SELECT 'medical', '加微信下单', NULL, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM sc_compliance_word WHERE word_type = 'medical' AND word_value = '加微信下单' AND deleted = 0);
INSERT INTO sc_compliance_word (word_type, word_value, replacement, is_enabled, deleted)
SELECT 'medical', '加微信购买', NULL, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM sc_compliance_word WHERE word_type = 'medical' AND word_value = '加微信购买' AND deleted = 0);
INSERT INTO sc_compliance_word (word_type, word_value, replacement, is_enabled, deleted)
SELECT 'medical', '脱离平台', NULL, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM sc_compliance_word WHERE word_type = 'medical' AND word_value = '脱离平台' AND deleted = 0);
INSERT INTO sc_compliance_word (word_type, word_value, replacement, is_enabled, deleted)
SELECT 'medical', '走私单', NULL, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM sc_compliance_word WHERE word_type = 'medical' AND word_value = '走私单' AND deleted = 0);

-- 虚假宣传 / 夸张承诺（常见公开通报类型）
INSERT INTO sc_compliance_word (word_type, word_value, replacement, is_enabled, deleted)
SELECT 'medical', '无效退款', NULL, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM sc_compliance_word WHERE word_type = 'medical' AND word_value = '无效退款' AND deleted = 0);
INSERT INTO sc_compliance_word (word_type, word_value, replacement, is_enabled, deleted)
SELECT 'absolute', '永久低价', '活动价', 1, 0
WHERE NOT EXISTS (SELECT 1 FROM sc_compliance_word WHERE word_type = 'absolute' AND word_value = '永久低价' AND deleted = 0);
INSERT INTO sc_compliance_word (word_type, word_value, replacement, is_enabled, deleted)
SELECT 'absolute', '史上最低', '优惠价', 1, 0
WHERE NOT EXISTS (SELECT 1 FROM sc_compliance_word WHERE word_type = 'absolute' AND word_value = '史上最低' AND deleted = 0);
INSERT INTO sc_compliance_word (word_type, word_value, replacement, is_enabled, deleted)
SELECT 'absolute', '全网首发', '新品上架', 1, 0
WHERE NOT EXISTS (SELECT 1 FROM sc_compliance_word WHERE word_type = 'absolute' AND word_value = '全网首发' AND deleted = 0);
INSERT INTO sc_compliance_word (word_type, word_value, replacement, is_enabled, deleted)
SELECT 'medical', '虚假功效', NULL, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM sc_compliance_word WHERE word_type = 'medical' AND word_value = '虚假功效' AND deleted = 0);

-- 美妆直播常见违禁宣称（公开广告法/化妆品条例归纳）
INSERT INTO sc_compliance_word (word_type, word_value, replacement, is_enabled, deleted)
SELECT 'medical', '干细胞美容', NULL, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM sc_compliance_word WHERE word_type = 'medical' AND word_value = '干细胞美容' AND deleted = 0);
INSERT INTO sc_compliance_word (word_type, word_value, replacement, is_enabled, deleted)
SELECT 'medical', '换脸级', NULL, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM sc_compliance_word WHERE word_type = 'medical' AND word_value = '换脸级' AND deleted = 0);
INSERT INTO sc_compliance_word (word_type, word_value, replacement, is_enabled, deleted)
SELECT 'absolute', '立竿见影', '使用后可见变化', 1, 0
WHERE NOT EXISTS (SELECT 1 FROM sc_compliance_word WHERE word_type = 'absolute' AND word_value = '立竿见影' AND deleted = 0);
