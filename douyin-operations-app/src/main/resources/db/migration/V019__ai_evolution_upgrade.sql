-- V019: AI 进化模块全面升级
-- 1. ai_evolve_pending_deepen 补充 deleted + kb_id
-- 2. ai_evolve_topic 补充 account_id（若 V011 已加则跳过）
-- 3. ai_evolve_task 补充 kb_id 索引
-- 4. ai_knowledge_base 补充 kb_type
-- 5. knowledge_evolution 系列表时间字段统一为 create_time/update_time

-- ============================================================
-- 1. ai_evolve_pending_deepen
-- ============================================================
ALTER TABLE ai_evolve_pending_deepen ADD COLUMN IF NOT EXISTS deleted INTEGER NOT NULL DEFAULT 0;
ALTER TABLE ai_evolve_pending_deepen ADD COLUMN IF NOT EXISTS kb_id BIGINT;

CREATE INDEX IF NOT EXISTS idx_pending_deepen_kb_status
    ON ai_evolve_pending_deepen (kb_id, status, priority_level) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_pending_deepen_report
    ON ai_evolve_pending_deepen (report_id);

-- ============================================================
-- 2. ai_evolve_topic account_id
-- ============================================================
ALTER TABLE ai_evolve_topic ADD COLUMN IF NOT EXISTS account_id BIGINT;
CREATE INDEX IF NOT EXISTS idx_ai_evolve_topic_account
    ON ai_evolve_topic (account_id) WHERE deleted = 0;

-- ============================================================
-- 3. ai_evolve_task kb_id 索引
-- ============================================================
CREATE INDEX IF NOT EXISTS idx_ai_evolve_task_kb
    ON ai_evolve_task (kb_id, create_time DESC);

-- ============================================================
-- 4. ai_knowledge_base kb_type
-- ============================================================
ALTER TABLE ai_knowledge_base ADD COLUMN IF NOT EXISTS kb_type VARCHAR(16) DEFAULT 'general';

UPDATE ai_knowledge_base SET kb_type = 'huashu' WHERE kb_type = 'general' AND (kb_name LIKE '%huashu%' OR kb_name LIKE '%话术%');
UPDATE ai_knowledge_base SET kb_type = 'zhishi' WHERE kb_type = 'general' AND (kb_name LIKE '%zhishi%' OR kb_name LIKE '%知识%');
UPDATE ai_knowledge_base SET kb_type = 'douyin' WHERE kb_type = 'general' AND (kb_name LIKE '%douyin%' OR kb_name LIKE '%抖音%');

-- ============================================================
-- 5. knowledge_evolution 系列表时间字段统一
-- ============================================================
DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='public' AND table_name='ai_knowledge_evolution_log' AND column_name='created_at') THEN
    ALTER TABLE ai_knowledge_evolution_log RENAME COLUMN created_at TO create_time;
  END IF;
  IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='public' AND table_name='ai_knowledge_evolution_log' AND column_name='updated_at') THEN
    ALTER TABLE ai_knowledge_evolution_log RENAME COLUMN updated_at TO update_time;
  END IF;
END $$;

DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='public' AND table_name='ai_knowledge_evolution_rule' AND column_name='created_at') THEN
    ALTER TABLE ai_knowledge_evolution_rule RENAME COLUMN created_at TO create_time;
  END IF;
  IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='public' AND table_name='ai_knowledge_evolution_rule' AND column_name='updated_at') THEN
    ALTER TABLE ai_knowledge_evolution_rule RENAME COLUMN updated_at TO update_time;
  END IF;
END $$;

DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='public' AND table_name='ai_knowledge_quality_score' AND column_name='created_at') THEN
    ALTER TABLE ai_knowledge_quality_score RENAME COLUMN created_at TO create_time;
  END IF;
  IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='public' AND table_name='ai_knowledge_quality_score' AND column_name='updated_at') THEN
    ALTER TABLE ai_knowledge_quality_score RENAME COLUMN updated_at TO update_time;
  END IF;
END $$;

DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='public' AND table_name='ai_knowledge_deduplication_group' AND column_name='created_at') THEN
    ALTER TABLE ai_knowledge_deduplication_group RENAME COLUMN created_at TO create_time;
  END IF;
  IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='public' AND table_name='ai_knowledge_deduplication_group' AND column_name='updated_at') THEN
    ALTER TABLE ai_knowledge_deduplication_group RENAME COLUMN updated_at TO update_time;
  END IF;
END $$;
