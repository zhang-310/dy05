CREATE TABLE IF NOT EXISTS ai_competitor_insight (
    id BIGSERIAL PRIMARY KEY,
    source VARCHAR(50) NOT NULL,
    category VARCHAR(50),
    competitor_name VARCHAR(100),
    insight_type VARCHAR(50),
    content TEXT,
    collected_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    quality_score DOUBLE PRECISION,
    ingested_to_kb BOOLEAN DEFAULT false,
    deleted INTEGER DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_ai_comp_insight_cat ON ai_competitor_insight(category) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_ai_comp_insight_ingested ON ai_competitor_insight(ingested_to_kb) WHERE deleted = 0;
