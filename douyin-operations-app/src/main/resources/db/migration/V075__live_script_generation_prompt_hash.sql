-- G-3：AI 生成时最终请求用的 system+user prompt 指纹（SHA-256 十六进制，64 字符）
ALTER TABLE live_script ADD COLUMN IF NOT EXISTS generation_prompt_hash VARCHAR(64);
