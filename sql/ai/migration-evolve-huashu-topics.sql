-- 进化主题池：新增 huashu 高停留话术主题（供已有主题池的实例增量补充）
-- 执行前需确保 ai_evolve_topic 已存在

INSERT INTO ai_evolve_topic (kb_id, topic, category, priority, source, status, deleted, create_time, update_time)
SELECT NULL, '高扎心话术 人生真相 现实共鸣 情感冲击', 'huashu', 100, 'manual', 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM ai_evolve_topic WHERE topic = '高扎心话术 人生真相 现实共鸣 情感冲击' AND deleted = 0);

INSERT INTO ai_evolve_topic (kb_id, topic, category, priority, source, status, deleted, create_time, update_time)
SELECT NULL, '搞笑幽默话术 段子 梗 轻松调侃', 'huashu', 100, 'manual', 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM ai_evolve_topic WHERE topic = '搞笑幽默话术 段子 梗 轻松调侃' AND deleted = 0);

INSERT INTO ai_evolve_topic (kb_id, topic, category, priority, source, status, deleted, create_time, update_time)
SELECT NULL, '高鸡汤话术 正能量 励志 情感价值', 'huashu', 100, 'manual', 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM ai_evolve_topic WHERE topic = '高鸡汤话术 正能量 励志 情感价值' AND deleted = 0);

INSERT INTO ai_evolve_topic (kb_id, topic, category, priority, source, status, deleted, create_time, update_time)
SELECT NULL, '歇后语俗语话术 接地气 民间智慧', 'huashu', 100, 'manual', 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM ai_evolve_topic WHERE topic = '歇后语俗语话术 接地气 民间智慧' AND deleted = 0);

INSERT INTO ai_evolve_topic (kb_id, topic, category, priority, source, status, deleted, create_time, update_time)
SELECT NULL, '家庭夫妻话术 婆媳 育儿 婚姻共鸣', 'huashu', 100, 'manual', 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM ai_evolve_topic WHERE topic = '家庭夫妻话术 婆媳 育儿 婚姻共鸣' AND deleted = 0);

INSERT INTO ai_evolve_topic (kb_id, topic, category, priority, source, status, deleted, create_time, update_time)
SELECT NULL, '爱情情感话术 夫妻关系 恋爱观', 'huashu', 100, 'manual', 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM ai_evolve_topic WHERE topic = '爱情情感话术 夫妻关系 恋爱观' AND deleted = 0);

INSERT INTO ai_evolve_topic (kb_id, topic, category, priority, source, status, deleted, create_time, update_time)
SELECT NULL, '高抒情话术 诗意 排比 意境', 'huashu', 100, 'manual', 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM ai_evolve_topic WHERE topic = '高抒情话术 诗意 排比 意境' AND deleted = 0);

INSERT INTO ai_evolve_topic (kb_id, topic, category, priority, source, status, deleted, create_time, update_time)
SELECT NULL, '人生语录话术 金句 哲理 记忆点', 'huashu', 100, 'manual', 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM ai_evolve_topic WHERE topic = '人生语录话术 金句 哲理 记忆点' AND deleted = 0);

-- 大数据分析补充（10 类）
INSERT INTO ai_evolve_topic (kb_id, topic, category, priority, source, status, deleted, create_time, update_time)
SELECT NULL, '毒鸡汤话术 反讽 夸张 生活热点 绝绝子', 'huashu', 100, 'manual', 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM ai_evolve_topic WHERE topic = '毒鸡汤话术 反讽 夸张 生活热点 绝绝子' AND deleted = 0);

INSERT INTO ai_evolve_topic (kb_id, topic, category, priority, source, status, deleted, create_time, update_time)
SELECT NULL, '痛点共鸣话术 直接命中痛点 缺失感', 'huashu', 100, 'manual', 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM ai_evolve_topic WHERE topic = '痛点共鸣话术 直接命中痛点 缺失感' AND deleted = 0);

INSERT INTO ai_evolve_topic (kb_id, topic, category, priority, source, status, deleted, create_time, update_time)
SELECT NULL, '治愈系话术 轻生活 高压人群 治愈', 'huashu', 100, 'manual', 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM ai_evolve_topic WHERE topic = '治愈系话术 轻生活 高压人群 治愈' AND deleted = 0);

INSERT INTO ai_evolve_topic (kb_id, topic, category, priority, source, status, deleted, create_time, update_time)
SELECT NULL, '成长逆袭话术 时间延续 强对比 逆袭故事', 'huashu', 100, 'manual', 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM ai_evolve_topic WHERE topic = '成长逆袭话术 时间延续 强对比 逆袭故事' AND deleted = 0);

INSERT INTO ai_evolve_topic (kb_id, topic, category, priority, source, status, deleted, create_time, update_time)
SELECT NULL, '自嘲幽默话术 承认缺点 转折亮点 亲和力', 'huashu', 100, 'manual', 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM ai_evolve_topic WHERE topic = '自嘲幽默话术 承认缺点 转折亮点 亲和力' AND deleted = 0);

INSERT INTO ai_evolve_topic (kb_id, topic, category, priority, source, status, deleted, create_time, update_time)
SELECT NULL, '反焦虑反转话术 预期铺垫 意外转折 缓解压力', 'huashu', 100, 'manual', 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM ai_evolve_topic WHERE topic = '反焦虑反转话术 预期铺垫 意外转折 缓解压力' AND deleted = 0);

INSERT INTO ai_evolve_topic (kb_id, topic, category, priority, source, status, deleted, create_time, update_time)
SELECT NULL, '顺口溜押韵话术 节奏感 记忆点 高情商回复', 'huashu', 100, 'manual', 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM ai_evolve_topic WHERE topic = '顺口溜押韵话术 节奏感 记忆点 高情商回复' AND deleted = 0);

INSERT INTO ai_evolve_topic (kb_id, topic, category, priority, source, status, deleted, create_time, update_time)
SELECT NULL, '身份锚定留人话术 精准标签 降低决策门槛', 'huashu', 100, 'manual', 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM ai_evolve_topic WHERE topic = '身份锚定留人话术 精准标签 降低决策门槛' AND deleted = 0);

INSERT INTO ai_evolve_topic (kb_id, topic, category, priority, source, status, deleted, create_time, update_time)
SELECT NULL, '损失厌恶促单话术 紧迫感 稀缺感 库存回收', 'huashu', 100, 'manual', 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM ai_evolve_topic WHERE topic = '损失厌恶促单话术 紧迫感 稀缺感 库存回收' AND deleted = 0);

INSERT INTO ai_evolve_topic (kb_id, topic, category, priority, source, status, deleted, create_time, update_time)
SELECT NULL, '打工人职场话术 职场共鸣 摸鱼 加班心酸', 'huashu', 100, 'manual', 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM ai_evolve_topic WHERE topic = '打工人职场话术 职场共鸣 摸鱼 加班心酸' AND deleted = 0);
