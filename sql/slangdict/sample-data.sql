-- ============================================================
-- 话术梗库模块 (SlangDict) — 示例数据
-- 依赖: sd_entry, sd_product_mapping 表已创建
-- ============================================================

INSERT INTO sd_entry (user_id, phrase, meaning, category, usage_scene, example, source, use_count, status)
VALUES
(1, '给小弟准备一套别墅', '推荐高端男士内裤', 'product_alias', '带货', '家人们，今天给小弟准备一套别墅，住进去舒服得不得了！', '同行学习', 12, 1),
(1, '贴身管家', '形容内裤舒适贴合', 'catchphrase', '带货', '这款就是你的贴身管家，24小时守护你的舒适！', '自创', 8, 1),
(1, '脸上的空调', '形容护肤品清爽感', 'product_alias', '带货', '抹上去就像脸上装了空调，清清凉凉的~', 'AI生成', 5, 1),
(1, '熬夜急救包', '指修复型面膜', 'product_alias', '带货', '昨晚又熬夜了？没关系，熬夜急救包安排上！', '自创', 15, 1),
(1, '一秒变白富美', '美白产品效果夸张表达', 'slang', '带货', '用完这个，一秒变白富美，不是我吹！', '同行学习', 3, 1),
(1, '姐妹们冲鸭', '号召粉丝下单', 'catchphrase', '互动', '最后50单了，姐妹们冲鸭！', '自创', 20, 1);

-- 关联梗与产品（假设产品 ID 1, 2 已存在）
INSERT INTO sd_product_mapping (entry_id, product_id, user_id)
SELECT e.id, 1, 1 FROM sd_entry e WHERE e.phrase = '给小弟准备一套别墅' AND e.user_id = 1
UNION ALL
SELECT e.id, 1, 1 FROM sd_entry e WHERE e.phrase = '贴身管家' AND e.user_id = 1;
