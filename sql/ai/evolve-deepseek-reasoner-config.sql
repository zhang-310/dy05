-- 进化引擎使用 DeepSeek Reasoner（思考模型）为主模型
-- 主模型: deepseek-reasoner, 备1: deepseek-chat, 备2: ollama 首个

-- 若已有配置则更新
UPDATE ai_task_model_config
SET primary_model_id = (SELECT id FROM ai_model WHERE model_provider='deepseek' AND model_version='deepseek-reasoner' AND deleted=0 LIMIT 1),
    fallback_model_id = (SELECT id FROM ai_model WHERE model_provider='deepseek' AND model_version='deepseek-chat' AND deleted=0 LIMIT 1),
    fallback2_model_id = (SELECT id FROM ai_model WHERE model_provider='ollama' AND deleted=0 ORDER BY id LIMIT 1),
    timeout_seconds = 360,
    update_time = CURRENT_TIMESTAMP
WHERE task_code = 'knowledge_evolve' AND deleted = 0;

-- 若无配置则插入
INSERT INTO ai_task_model_config (task_code, task_name, task_group, primary_model_id, fallback_model_id, fallback2_model_id, timeout_seconds, status, deleted, create_time, update_time)
SELECT 'knowledge_evolve', '知识进化', 'evolve',
  (SELECT id FROM ai_model WHERE model_provider='deepseek' AND model_version='deepseek-reasoner' AND deleted=0 LIMIT 1),
  (SELECT id FROM ai_model WHERE model_provider='deepseek' AND model_version='deepseek-chat' AND deleted=0 LIMIT 1),
  (SELECT id FROM ai_model WHERE model_provider='ollama' AND deleted=0 ORDER BY id LIMIT 1),
  360, 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM ai_task_model_config WHERE task_code='knowledge_evolve' AND deleted=0)
  AND EXISTS (SELECT 1 FROM ai_model WHERE model_provider='deepseek' AND model_version='deepseek-reasoner' AND deleted=0);
