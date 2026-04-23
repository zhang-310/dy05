-- P1 D4-03：进化主题规则配置（条件+动作，JSON）
ALTER TABLE ai_evolve_topic
    ADD COLUMN IF NOT EXISTS rule_config JSONB;

COMMENT ON COLUMN ai_evolve_topic.rule_config IS '规则配置 JSON：如 qualityThreshold、actions 等';
