-- ============================================================
-- 修复 Ollama 模型名：deepseek-v3:7b 在 Ollama 中不存在
-- 执行：已有错误配置的数据库执行此脚本
-- ============================================================

-- 将 deepseek-v3:7b 更新为 deepseek-llm:7b（Ollama 官方支持）
UPDATE ai_model
SET model_name = 'DeepSeek-LLM-7B', model_version = 'deepseek-llm:7b', update_time = CURRENT_TIMESTAMP
WHERE model_provider = 'ollama' AND model_version = 'deepseek-v3:7b' AND deleted = 0;
