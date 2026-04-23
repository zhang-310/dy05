-- V022: ai_prompt_template 表补充 avg_score、p50_score、p90_score 列（与 Entity 对齐）
ALTER TABLE ai_prompt_template ADD COLUMN IF NOT EXISTS avg_score FLOAT;
ALTER TABLE ai_prompt_template ADD COLUMN IF NOT EXISTS p50_score FLOAT;
ALTER TABLE ai_prompt_template ADD COLUMN IF NOT EXISTS p90_score FLOAT;
