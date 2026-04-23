-- ============================================================
-- 分镜生成使用 DeepSeek-Chat（更快，减少超时）
-- 原 deepseek-reasoner 深度思考模型响应慢，易超时
-- 前置：ai_model 中需有 deepseek-chat 记录
-- ============================================================

-- 将 short_video_script 主模型改为 deepseek-chat
UPDATE ai_task_model_config
SET primary_model_id = (SELECT id FROM ai_model WHERE model_provider='deepseek' AND model_version='deepseek-chat' AND deleted=0 LIMIT 1),
    fallback_model_id = (SELECT id FROM ai_model WHERE model_provider='deepseek' AND model_version='deepseek-reasoner' AND deleted=0 LIMIT 1),
    fallback2_model_id = (SELECT id FROM ai_model WHERE model_provider='ollama' AND deleted=0 ORDER BY id LIMIT 1),
    timeout_seconds = 90,
    update_time = CURRENT_TIMESTAMP
WHERE task_code = 'short_video_script' AND deleted = 0
  AND EXISTS (SELECT 1 FROM ai_model WHERE model_provider='deepseek' AND model_version='deepseek-chat' AND deleted=0);
