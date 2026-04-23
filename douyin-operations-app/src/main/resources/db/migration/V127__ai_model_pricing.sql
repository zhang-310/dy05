-- V127: AI 模型价格矩阵表（区分 input/output token 计费）
CREATE TABLE IF NOT EXISTS ai_model_pricing (
    id              BIGSERIAL PRIMARY KEY,
    model_name      VARCHAR(100) NOT NULL UNIQUE,
    provider        VARCHAR(50),
    input_price_per_1k  DECIMAL(10,6) NOT NULL DEFAULT 0,
    output_price_per_1k DECIMAL(10,6) NOT NULL DEFAULT 0,
    currency        VARCHAR(10) NOT NULL DEFAULT 'CNY',
    effective_from  TIMESTAMP DEFAULT NOW(),
    create_time     TIMESTAMP DEFAULT NOW(),
    update_time     TIMESTAMP DEFAULT NOW(),
    deleted         INTEGER NOT NULL DEFAULT 0
);

COMMENT ON TABLE ai_model_pricing IS 'AI 模型 token 级价格矩阵';
COMMENT ON COLUMN ai_model_pricing.input_price_per_1k IS '每 1000 输入 token 价格';
COMMENT ON COLUMN ai_model_pricing.output_price_per_1k IS '每 1000 输出 token 价格';

-- 默认价格数据
INSERT INTO ai_model_pricing (model_name, provider, input_price_per_1k, output_price_per_1k, currency)
VALUES
    ('deepseek-chat', 'deepseek', 0.001, 0.002, 'CNY'),
    ('deepseek-reasoner', 'deepseek', 0.004, 0.016, 'CNY'),
    ('gpt-4o', 'openai', 0.075, 0.300, 'CNY'),
    ('gpt-4o-mini', 'openai', 0.001, 0.004, 'CNY'),
    ('qwen-max', 'qwen', 0.020, 0.060, 'CNY'),
    ('qwen-plus', 'qwen', 0.004, 0.012, 'CNY'),
    ('glm-4-flash', 'glm', 0.001, 0.001, 'CNY'),
    ('glm-4-plus', 'glm', 0.050, 0.050, 'CNY'),
    ('doubao-pro-32k', 'volcengine', 0.008, 0.008, 'CNY'),
    ('doubao-lite-32k', 'volcengine', 0.003, 0.003, 'CNY')
ON CONFLICT (model_name) DO NOTHING;
