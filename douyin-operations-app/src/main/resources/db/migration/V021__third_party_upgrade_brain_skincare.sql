-- 第三方评估融合升级：最强大脑 + 护肤品赛道
-- 1. ai_host_persona 扩展：target_category, target_gmv_tier, strategy_phase
-- 2. 护肤品合规词增强

-- ai_host_persona 扩展
ALTER TABLE ai_host_persona ADD COLUMN IF NOT EXISTS target_category VARCHAR(64);
ALTER TABLE ai_host_persona ADD COLUMN IF NOT EXISTS target_gmv_tier VARCHAR(32);
ALTER TABLE ai_host_persona ADD COLUMN IF NOT EXISTS strategy_phase INTEGER;

COMMENT ON COLUMN ai_host_persona.target_category IS '目标品类：护肤品/美妆/综合';
COMMENT ON COLUMN ai_host_persona.target_gmv_tier IS 'GMV档位：新锐/腰部/头部';
COMMENT ON COLUMN ai_host_persona.strategy_phase IS '转型阶段：1=品类测试 2=品牌深度 3=头部竞争';

-- 护肤品功效宣称合规词（医疗违禁、绝对化）
INSERT INTO sc_compliance_word (word_type, word_value, replacement, is_enabled, deleted)
SELECT 'medical', '绝对美白', NULL, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM sc_compliance_word WHERE word_type='medical' AND word_value='绝对美白' AND deleted=0);
INSERT INTO sc_compliance_word (word_type, word_value, replacement, is_enabled, deleted)
SELECT 'medical', '100%祛斑', NULL, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM sc_compliance_word WHERE word_type='medical' AND word_value='100%祛斑' AND deleted=0);
INSERT INTO sc_compliance_word (word_type, word_value, replacement, is_enabled, deleted)
SELECT 'medical', '医学级', NULL, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM sc_compliance_word WHERE word_type='medical' AND word_value='医学级' AND deleted=0);
INSERT INTO sc_compliance_word (word_type, word_value, replacement, is_enabled, deleted)
SELECT 'medical', '药到病除', NULL, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM sc_compliance_word WHERE word_type='medical' AND word_value='药到病除' AND deleted=0);
INSERT INTO sc_compliance_word (word_type, word_value, replacement, is_enabled, deleted)
SELECT 'absolute', '绝对有效', '有效', 1, 0
WHERE NOT EXISTS (SELECT 1 FROM sc_compliance_word WHERE word_type='absolute' AND word_value='绝对有效' AND deleted=0);
INSERT INTO sc_compliance_word (word_type, word_value, replacement, is_enabled, deleted)
SELECT 'absolute', '彻底祛除', '改善', 1, 0
WHERE NOT EXISTS (SELECT 1 FROM sc_compliance_word WHERE word_type='absolute' AND word_value='彻底祛除' AND deleted=0);
