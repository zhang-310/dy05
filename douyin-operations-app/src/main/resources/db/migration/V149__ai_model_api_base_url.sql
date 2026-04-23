-- 单模型可选自定义 OpenAI 兼容 API 根地址；留空则仍按 provider 使用系统配置
ALTER TABLE ai_model ADD COLUMN IF NOT EXISTS api_base_url VARCHAR(512);

COMMENT ON COLUMN ai_model.api_base_url IS '可选；自定义 Base URL（不含 /v1/chat/completions 路径），留空则按 model_provider 读系统配置';
