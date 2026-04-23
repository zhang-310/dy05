-- ============================================================
-- 创作中心（短视频脚本/文案/分镜/标题）使用 DeepSeek 深度思考模型
-- 主模型: deepseek-reasoner（深度思考）, 备1: deepseek-chat, 备2: ollama 首个
-- 前置：1. 执行 ai-model-data.sql 确保有 DeepSeek-Reasoner 记录
--       2. 在 .env 或 sys_config 中配置 DEEPSEEK_API_KEY / ai.deepseek.api_key
-- ============================================================

-- 若已有 short_video_script 配置则更新
UPDATE ai_task_model_config
SET primary_model_id = (SELECT id FROM ai_model WHERE model_provider='deepseek' AND model_version='deepseek-reasoner' AND deleted=0 LIMIT 1),
    fallback_model_id = (SELECT id FROM ai_model WHERE model_provider='deepseek' AND model_version='deepseek-chat' AND deleted=0 LIMIT 1),
    fallback2_model_id = (SELECT id FROM ai_model WHERE model_provider='ollama' AND deleted=0 ORDER BY id LIMIT 1),
    timeout_seconds = 120,
    update_time = CURRENT_TIMESTAMP
WHERE task_code = 'short_video_script' AND deleted = 0
  AND EXISTS (SELECT 1 FROM ai_model WHERE model_provider='deepseek' AND model_version='deepseek-reasoner' AND deleted=0);

-- 若无 short_video_script 配置则插入
INSERT INTO ai_task_model_config (task_code, task_name, task_group, primary_model_id, fallback_model_id, fallback2_model_id, timeout_seconds, max_retries, sort_order, status, deleted, create_time, update_time)
SELECT 'short_video_script', '短视频脚本生成', 'shortvideo',
  (SELECT id FROM ai_model WHERE model_provider='deepseek' AND model_version='deepseek-reasoner' AND deleted=0 LIMIT 1),
  (SELECT id FROM ai_model WHERE model_provider='deepseek' AND model_version='deepseek-chat' AND deleted=0 LIMIT 1),
  (SELECT id FROM ai_model WHERE model_provider='ollama' AND deleted=0 ORDER BY id LIMIT 1),
  120, 1, 0, 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM ai_task_model_config WHERE task_code='short_video_script' AND deleted=0)
  AND EXISTS (SELECT 1 FROM ai_model WHERE model_provider='deepseek' AND model_version='deepseek-reasoner' AND deleted=0);
