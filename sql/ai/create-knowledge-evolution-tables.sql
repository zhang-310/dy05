-- ========================================
-- Create Knowledge Evolution Module Tables
-- PostgreSQL compatible version
-- ========================================

-- ─────────────────────────────────────────
-- 1. Evolution Log Table
-- ─────────────────────────────────────────
CREATE TABLE IF NOT EXISTS ai_knowledge_evolution_log (
    id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    user_id BIGINT NOT NULL,
    script_version_id BIGINT NOT NULL,
    action VARCHAR(50) NOT NULL,
    rule_type VARCHAR(50),
    old_value TEXT,
    new_value TEXT,
    reason TEXT,
    executed_by VARCHAR(255) DEFAULT 'system',
    status VARCHAR(50) DEFAULT 'COMPLETED',
    error_message TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_evolution_log_user_id ON ai_knowledge_evolution_log (user_id) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_evolution_log_script_version_id ON ai_knowledge_evolution_log (script_version_id) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_evolution_log_action ON ai_knowledge_evolution_log (action) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_evolution_log_rule_type ON ai_knowledge_evolution_log (rule_type) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_evolution_log_created_at ON ai_knowledge_evolution_log (created_at) WHERE deleted = 0;

COMMENT ON TABLE ai_knowledge_evolution_log IS 'Knowledge evolution log table - Records all evolution actions';
COMMENT ON COLUMN ai_knowledge_evolution_log.action IS 'auto_import, version_update, auto_archive, merge, quality_score_update';
COMMENT ON COLUMN ai_knowledge_evolution_log.rule_type IS 'INCLUSION_RULE, UPDATE_RULE, ARCHIVAL_RULE, DEDUP_RULE';
COMMENT ON COLUMN ai_knowledge_evolution_log.old_value IS 'Previous state (JSON)';
COMMENT ON COLUMN ai_knowledge_evolution_log.new_value IS 'New state (JSON)';
COMMENT ON COLUMN ai_knowledge_evolution_log.executed_by IS 'Executor: system or user ID';
COMMENT ON COLUMN ai_knowledge_evolution_log.status IS 'COMPLETED, FAILED, PENDING';

-- ─────────────────────────────────────────
-- 2. Quality Score Table
-- ─────────────────────────────────────────
CREATE TABLE IF NOT EXISTS ai_knowledge_quality_score (
    id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    user_id BIGINT NOT NULL,
    script_version_id BIGINT NOT NULL,
    period_start DATE NOT NULL,
    period_end DATE NOT NULL,
    quality_score DECIMAL(5, 2) NOT NULL,
    effectiveness_score DECIMAL(5, 2),
    usage_count INTEGER DEFAULT 0,
    adoption_rate DECIMAL(5, 2),
    engagement_rate DECIMAL(5, 2),
    conversion_rate DECIMAL(5, 2),
    avg_sentiment_score DECIMAL(5, 2),
    consecutive_low_scores INTEGER DEFAULT 0,
    trend VARCHAR(20),
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER DEFAULT 0,
    CONSTRAINT uk_script_period UNIQUE (script_version_id, period_start, period_end, deleted)
);

CREATE INDEX IF NOT EXISTS idx_quality_score_user_id ON ai_knowledge_quality_score (user_id) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_quality_score_script_version_id ON ai_knowledge_quality_score (script_version_id) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_quality_score_period ON ai_knowledge_quality_score (period_start, period_end) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_quality_score_score ON ai_knowledge_quality_score (quality_score) WHERE deleted = 0;

COMMENT ON TABLE ai_knowledge_quality_score IS 'Knowledge quality score table - Tracks quality scores over time periods';
COMMENT ON COLUMN ai_knowledge_quality_score.period_start IS 'Period start date';
COMMENT ON COLUMN ai_knowledge_quality_score.period_end IS 'Period end date';
COMMENT ON COLUMN ai_knowledge_quality_score.quality_score IS 'Quality score 0-100';
COMMENT ON COLUMN ai_knowledge_quality_score.trend IS 'UP, DOWN, STABLE';

-- ─────────────────────────────────────────
-- 3. Deduplication Group Table (already created, but include for completeness)
-- ─────────────────────────────────────────
CREATE TABLE IF NOT EXISTS ai_knowledge_deduplication_group (
    id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    user_id BIGINT NOT NULL,
    master_script_id BIGINT NOT NULL,
    duplicate_script_id BIGINT NOT NULL,
    similarity_score DECIMAL(3, 2) NOT NULL,
    merge_status VARCHAR(50) DEFAULT 'DETECTED',
    merge_reason VARCHAR(255),
    merged_at TIMESTAMP,
    variant_type VARCHAR(50) DEFAULT 'VARIANT',
    is_active INTEGER DEFAULT 1,
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER DEFAULT 0,
    CONSTRAINT uk_master_duplicate UNIQUE (master_script_id, duplicate_script_id, deleted)
);

CREATE INDEX IF NOT EXISTS idx_dedup_group_user_id ON ai_knowledge_deduplication_group (user_id) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_dedup_group_master_script_id ON ai_knowledge_deduplication_group (master_script_id) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_dedup_group_duplicate_script_id ON ai_knowledge_deduplication_group (duplicate_script_id) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_dedup_group_merge_status ON ai_knowledge_deduplication_group (merge_status) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_dedup_group_similarity_score ON ai_knowledge_deduplication_group (similarity_score) WHERE deleted = 0;

COMMENT ON TABLE ai_knowledge_deduplication_group IS 'Knowledge deduplication group table - Groups duplicate/variant scripts together with similarity scores';
COMMENT ON COLUMN ai_knowledge_deduplication_group.master_script_id IS 'Master (primary) script version ID';
COMMENT ON COLUMN ai_knowledge_deduplication_group.duplicate_script_id IS 'Duplicate (variant) script version ID';
COMMENT ON COLUMN ai_knowledge_deduplication_group.similarity_score IS 'Vector similarity 0-1';
COMMENT ON COLUMN ai_knowledge_deduplication_group.merge_status IS 'DETECTED, MERGED, IGNORED, PENDING_REVIEW';
COMMENT ON COLUMN ai_knowledge_deduplication_group.variant_type IS 'VARIANT, ARCHIVED, PENDING';
COMMENT ON COLUMN ai_knowledge_deduplication_group.is_active IS '1=active, 0=archived';

-- ─────────────────────────────────────────
-- 4. Evolution Rule Configuration Table
-- ─────────────────────────────────────────
CREATE TABLE IF NOT EXISTS ai_knowledge_evolution_rule (
    id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    rule_type VARCHAR(50) NOT NULL UNIQUE,
    rule_name VARCHAR(100) NOT NULL,
    description TEXT,
    is_enabled INTEGER DEFAULT 1,
    config JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_evolution_rule_type ON ai_knowledge_evolution_rule (rule_type) WHERE deleted = 0;

COMMENT ON TABLE ai_knowledge_evolution_rule IS 'Evolution rule configuration table';
COMMENT ON COLUMN ai_knowledge_evolution_rule.rule_type IS 'INCLUSION_RULE, UPDATE_RULE, ARCHIVAL_RULE, DEDUP_RULE';
COMMENT ON COLUMN ai_knowledge_evolution_rule.config IS 'Rule configuration parameters (JSON)';

-- ─────────────────────────────────────────
-- 5. Evolution Execution Record Table
-- ─────────────────────────────────────────
CREATE TABLE IF NOT EXISTS ai_knowledge_evolution_execution (
    id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
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
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_evolution_execution_user_id ON ai_knowledge_evolution_execution (user_id) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_evolution_execution_date ON ai_knowledge_evolution_execution (execution_date) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_evolution_execution_status ON ai_knowledge_evolution_execution (execution_status) WHERE deleted = 0;

COMMENT ON TABLE ai_knowledge_evolution_execution IS 'Evolution execution record table - Records each execution run';
COMMENT ON COLUMN ai_knowledge_evolution_execution.execution_period IS 'DAILY, WEEKLY, MONTHLY';
COMMENT ON COLUMN ai_knowledge_evolution_execution.rule_type IS 'Which rule(s) triggered';
COMMENT ON COLUMN ai_knowledge_evolution_execution.included_count IS 'Scripts added to library';
COMMENT ON COLUMN ai_knowledge_evolution_execution.updated_count IS 'Scripts updated';
COMMENT ON COLUMN ai_knowledge_evolution_execution.merged_count IS 'Scripts merged';
COMMENT ON COLUMN ai_knowledge_evolution_execution.archived_count IS 'Scripts archived';
COMMENT ON COLUMN ai_knowledge_evolution_execution.execution_status IS 'COMPLETED, FAILED, PENDING';

-- ─────────────────────────────────────────
-- 6. Evolution Report Table
-- ─────────────────────────────────────────
CREATE TABLE IF NOT EXISTS ai_knowledge_evolution_report (
    id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
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
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_evolution_report_user_id ON ai_knowledge_evolution_report (user_id) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_evolution_report_type ON ai_knowledge_evolution_report (report_type) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_evolution_report_period ON ai_knowledge_evolution_report (period_start, period_end) WHERE deleted = 0;

COMMENT ON TABLE ai_knowledge_evolution_report IS 'Evolution report table - Stores evolution reports';
COMMENT ON COLUMN ai_knowledge_evolution_report.report_type IS 'WEEKLY, MONTHLY, CUSTOM';
COMMENT ON COLUMN ai_knowledge_evolution_report.report_content IS 'Detailed report data (JSON)';
COMMENT ON COLUMN ai_knowledge_evolution_report.trend IS 'UP, DOWN, STABLE';

-- ─────────────────────────────────────────
-- Default Rule Configuration Data
-- ─────────────────────────────────────────
INSERT INTO ai_knowledge_evolution_rule (rule_type, rule_name, description, is_enabled, config) VALUES
('INCLUSION_RULE', 'High-Performance Script Auto-Import', 'Automatically import scripts with score >= 80 and usage >= 5', 1,
 '{"score_threshold": 80, "min_usage_count": 5, "min_creation_days_ago": 30, "max_daily_import": 100}')
ON CONFLICT (rule_type) DO NOTHING;

INSERT INTO ai_knowledge_evolution_rule (rule_type, rule_name, description, is_enabled, config) VALUES
('UPDATE_RULE', 'Script Version Auto-Update', 'Automatically update script version if new score is significantly higher', 1,
 '{"score_improvement_threshold": 5, "min_new_version_score": 75, "stability_days": 14, "rollback_threshold": 10}')
ON CONFLICT (rule_type) DO NOTHING;

INSERT INTO ai_knowledge_evolution_rule (rule_type, rule_name, description, is_enabled, config) VALUES
('ARCHIVAL_RULE', 'Low-Performance Script Auto-Archive', 'Automatically archive scripts with 3+ consecutive low scores', 1,
 '{"consecutive_low_score_periods": 3, "low_score_threshold": 40, "archive_days": 90}')
ON CONFLICT (rule_type) DO NOTHING;

INSERT INTO ai_knowledge_evolution_rule (rule_type, rule_name, description, is_enabled, config) VALUES
('DEDUP_RULE', 'Duplicate Script Deduplication', 'Automatically merge duplicate scripts based on vector similarity', 1,
 '{"similarity_threshold": 0.85, "require_manual_approval": false, "preserve_variants": true}')
ON CONFLICT (rule_type) DO NOTHING;
