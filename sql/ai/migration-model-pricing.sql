-- AI 模型价格矩阵表（区分 input/output token 计费）
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
