-- Flyway V124: 竞品洞察增加 reliability_score 列
ALTER TABLE ai_competitor_insight ADD COLUMN IF NOT EXISTS reliability_score DECIMAL(3,2) DEFAULT 0.50;
ALTER TABLE ai_competitor_insight ADD COLUMN IF NOT EXISTS data_source VARCHAR(50);
ALTER TABLE ai_competitor_insight ADD COLUMN IF NOT EXISTS raw_data TEXT;

CREATE INDEX IF NOT EXISTS idx_competitor_insight_reliability ON ai_competitor_insight(reliability_score);
CREATE INDEX IF NOT EXISTS idx_competitor_insight_source ON ai_competitor_insight(data_source);
