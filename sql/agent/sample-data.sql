-- ============================================================
-- agent 模块 - 示例数据
-- ============================================================

INSERT INTO agent (user_id, agent_name, agent_type, description, system_prompt, model_id, temperature, max_tokens, status, deleted, create_time, update_time)
VALUES
  (1, '文案助手', 'copywriting', '帮助生成和优化短视频文案', '你是一个专业的短视频文案创作助手，擅长撰写吸引人的标题和描述。', 1, 0.7, 2000, 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (1, '数据分析师', 'analytics', '分析账号数据并给出运营建议', '你是一个抖音数据分析专家，能够根据数据给出专业的运营优化建议。', 1, 0.3, 4000, 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO agent_conversation (agent_id, user_id, title, deleted, create_time, update_time)
VALUES
  (1, 1, '帮我写一个美食探店视频标题', 0, CURRENT_TIMESTAMP - INTERVAL '1 day', CURRENT_TIMESTAMP),
  (2, 1, '分析上周账号数据', 0, CURRENT_TIMESTAMP - INTERVAL '2 hours', CURRENT_TIMESTAMP);

INSERT INTO agent_message (conversation_id, role, content, tokens_used, create_time)
VALUES
  (1, 'user', '帮我写一个关于日料探店的短视频标题，要吸引人', 15, CURRENT_TIMESTAMP - INTERVAL '1 day'),
  (1, 'assistant', '以下是几个日料探店标题建议：\n1. 人均50！藏在巷子里的宝藏日料店\n2. 这家日料我吹爆！三文鱼厚切到离谱\n3. 本地人私藏的日料小店，终于被我找到了', 68, CURRENT_TIMESTAMP - INTERVAL '1 day'),
  (2, 'user', '帮我分析一下上周的账号数据表现', 12, CURRENT_TIMESTAMP - INTERVAL '2 hours'),
  (2, 'assistant', '根据上周数据分析：\n- 总播放量 28.1万，环比增长 15%\n- 互动率 6.8%，高于行业平均\n- 建议：周三和周五发布效果最好，可以增加这两天的发布频率', 85, CURRENT_TIMESTAMP - INTERVAL '2 hours');
