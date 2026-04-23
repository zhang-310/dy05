-- douyin_shortvideo 主题增量补充
INSERT INTO ai_evolve_topic (kb_id, account_id, topic, category, priority, source, status, deleted, create_time, update_time)
SELECT NULL, NULL, '爆款拆解 脚本结构 分镜 黄金3秒', 'douyin_shortvideo', 100, 'initial', 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM ai_evolve_topic WHERE topic = '爆款拆解 脚本结构 分镜 黄金3秒' AND deleted = 0);
