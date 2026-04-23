-- V059: AI 进化 P3 - 进化执行记录与报告表（Entity 补建）
-- 若表已由 create-knowledge-evolution-tables.sql 创建，则统一时间字段命名

-- ============================================================
-- 1. ai_knowledge_evolution_execution
-- ============================================================
CREATE TABLE IF NOT EXISTS ai_knowledge_evolution_execution (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    execution_period VARCHAR(50),
    execution_date DATE NOT NULL,
    rule_type VARCHAR(50),
    included_count INTEGER DEFAULT 0,
    updated_count INTEGER DEFAULT 0,
    merged_count INTEGER DEFAULT 0,
    archived_count INTEGER DEFAULT 0,
    quality_improvement DECIMAL(5, 2),
    execution_status VARCHAR(50) DEFAULT 'COMPLETED',
    error_message TEXT,
    executed_by VARCHAR(255) DEFAULT 'system',
    notes TEXT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER DEFAULT 0
);

-- 若表已存在且为 created_at/updated_at，统一重命名
DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='public' AND table_name='ai_knowledge_evolution_execution' AND column_name='created_at') THEN
    ALTER TABLE ai_knowledge_evolution_execution RENAME COLUMN created_at TO create_time;
  END IF;
  IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='public' AND table_name='ai_knowledge_evolution_execution' AND column_name='updated_at') THEN
    ALTER TABLE ai_knowledge_evolution_execution RENAME COLUMN updated_at TO update_time;
  END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_evolution_execution_user_id ON ai_knowledge_evolution_execution (user_id) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_evolution_execution_date ON ai_knowledge_evolution_execution (execution_date) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_evolution_execution_status ON ai_knowledge_evolution_execution (execution_status) WHERE deleted = 0;

-- ============================================================
-- 2. ai_knowledge_evolution_report
-- ============================================================
CREATE TABLE IF NOT EXISTS ai_knowledge_evolution_report (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    report_type VARCHAR(50) NOT NULL,
    period_start DATE NOT NULL,
    period_end DATE NOT NULL,
    total_in_library INTEGER DEFAULT 0,
    new_added_count INTEGER DEFAULT 0,
    archived_count INTEGER DEFAULT 0,
    deduplication_count INTEGER DEFAULT 0,
    average_quality_score DECIMAL(5, 2),
    trend VARCHAR(20),
    report_content JSONB,
    generated_by VARCHAR(255) DEFAULT 'system',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER DEFAULT 0
);

DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='public' AND table_name='ai_knowledge_evolution_report' AND column_name='created_at') THEN
    ALTER TABLE ai_knowledge_evolution_report RENAME COLUMN created_at TO create_time;
  END IF;
  IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='public' AND table_name='ai_knowledge_evolution_report' AND column_name='updated_at') THEN
    ALTER TABLE ai_knowledge_evolution_report RENAME COLUMN updated_at TO update_time;
  END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_evolution_report_user_id ON ai_knowledge_evolution_report (user_id) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_evolution_report_type ON ai_knowledge_evolution_report (report_type) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_evolution_report_period ON ai_knowledge_evolution_report (period_start, period_end) WHERE deleted = 0;
