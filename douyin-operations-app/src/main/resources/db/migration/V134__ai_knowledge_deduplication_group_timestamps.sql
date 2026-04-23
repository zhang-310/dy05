-- 与 Entity 对齐：历史库可能缺 created_at / updated_at
ALTER TABLE ai_knowledge_deduplication_group
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMP;
ALTER TABLE ai_knowledge_deduplication_group
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP;

ALTER TABLE ai_knowledge_evolution_log
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMP;
ALTER TABLE ai_knowledge_evolution_log
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP;

ALTER TABLE ai_knowledge_evolution_rule
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMP;
ALTER TABLE ai_knowledge_evolution_rule
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP;

ALTER TABLE ai_knowledge_quality_score
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMP;
ALTER TABLE ai_knowledge_quality_score
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP;
