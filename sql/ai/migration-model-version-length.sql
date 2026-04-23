-- 扩展 model_version 以支持较长模型标识（如 claude-sonnet-4-20250514-thinking）
ALTER TABLE ai_model ALTER COLUMN model_version TYPE VARCHAR(128);
