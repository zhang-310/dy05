-- huashu 主题池增量补充 4 条（地方特色/女性视角/人设特色/高创意）
-- 执行：docker cp sql/ai/migration-topic-pool-huashu-add4.sql dy-postgres:/tmp/; docker exec dy-postgres psql -U postgres -d douyin_operations -f /tmp/migration-topic-pool-huashu-add4.sql

INSERT INTO ai_evolve_topic (kb_id, account_id, topic, category, priority, source, status, deleted, create_time, update_time)
SELECT NULL, NULL, '地方特色话术 方言 接地气 地域共鸣', 'huashu_scenario', 100, 'initial', 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM ai_evolve_topic WHERE topic = '地方特色话术 方言 接地气 地域共鸣' AND deleted = 0);

INSERT INTO ai_evolve_topic (kb_id, account_id, topic, category, priority, source, status, deleted, create_time, update_time)
SELECT NULL, NULL, '女性视角话术 经济自主 自我价值 婚姻疲惫', 'huashu_scenario', 100, 'initial', 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM ai_evolve_topic WHERE topic = '女性视角话术 经济自主 自我价值 婚姻疲惫' AND deleted = 0);

INSERT INTO ai_evolve_topic (kb_id, account_id, topic, category, priority, source, status, deleted, create_time, update_time)
SELECT NULL, NULL, '人设特色话术 记忆点 标签 差异化', 'huashu_persuasion', 100, 'initial', 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM ai_evolve_topic WHERE topic = '人设特色话术 记忆点 标签 差异化' AND deleted = 0);

INSERT INTO ai_evolve_topic (kb_id, account_id, topic, category, priority, source, status, deleted, create_time, update_time)
SELECT NULL, NULL, '高创意话术 反转 意料之外 记忆点', 'huashu_literary', 100, 'initial', 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM ai_evolve_topic WHERE topic = '高创意话术 反转 意料之外 记忆点' AND deleted = 0);
