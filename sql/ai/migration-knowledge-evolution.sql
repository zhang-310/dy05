-- ========================================
-- Knowledge Library Self-Evolution Module
-- Migration Script
-- ========================================

-- ─────────────────────────────────────────
-- 1. Evolution Log Table
-- ─────────────────────────────────────────
CREATE TABLE IF NOT EXISTS ai_knowledge_evolution_log (
    id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    user_id BIGINT NOT NULL,
    script_version_id BIGINT NOT NULL,
    action VARCHAR(50) NOT NULL COMMENT 'auto_import, version_update, auto_archive, merge, quality_score_update',
    rule_type VARCHAR(50) COMMENT 'INCLUSION_RULE, UPDATE_RULE, ARCHIVAL_RULE, DEDUP_RULE',
    old_value TEXT COMMENT 'Previous state (JSON)',
    new_value TEXT COMMENT 'New state (JSON)',
    reason TEXT COMMENT 'Action reason',
    executed_by VARCHAR(255) DEFAULT 'system' COMMENT 'Executor: system or user ID',
    status VARCHAR(50) DEFAULT 'COMPLETED' COMMENT 'COMPLETED, FAILED, PENDING',
    error_message TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER DEFAULT 0,
    INDEX idx_user_id (user_id),
    INDEX idx_script_version_id (script_version_id),
    INDEX idx_action (action),
    INDEX idx_rule_type (rule_type),
    INDEX idx_created_at (created_at)
);

-- ─────────────────────────────────────────
-- 2. Quality Score Table
-- ─────────────────────────────────────────
CREATE TABLE IF NOT EXISTS ai_knowledge_quality_score (
    id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    user_id BIGINT NOT NULL,
    script_version_id BIGINT NOT NULL,
    period_start DATE NOT NULL COMMENT 'Period start date',
    period_end DATE NOT NULL COMMENT 'Period end date',
    quality_score DECIMAL(5, 2) NOT NULL COMMENT 'Quality score 0-100',
    effectiveness_score DECIMAL(5, 2) COMMENT 'Effectiveness score',
    usage_count INTEGER DEFAULT 0 COMMENT 'Usage count in period',
    adoption_rate DECIMAL(5, 2) COMMENT 'Adoption rate 0-100',
    engagement_rate DECIMAL(5, 2) COMMENT 'Engagement rate',
    conversion_rate DECIMAL(5, 2) COMMENT 'Conversion rate',
    avg_sentiment_score DECIMAL(5, 2) COMMENT 'Average sentiment score',
    consecutive_low_scores INTEGER DEFAULT 0 COMMENT 'Consecutive periods with low score',
    trend VARCHAR(20) COMMENT 'UP, DOWN, STABLE',
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER DEFAULT 0,
    INDEX idx_user_id (user_id),
    INDEX idx_script_version_id (script_version_id),
    INDEX idx_period (period_start, period_end),
    INDEX idx_quality_score (quality_score),
    UNIQUE KEY uk_script_period (script_version_id, period_start, period_end, deleted)
);

-- ─────────────────────────────────────────
-- 3. Deduplication Group Table
-- ─────────────────────────────────────────
CREATE TABLE IF NOT EXISTS ai_knowledge_deduplication_group (
    id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    user_id BIGINT NOT NULL,
    master_script_id BIGINT NOT NULL COMMENT 'Master (primary) script version ID',
    duplicate_script_id BIGINT NOT NULL COMMENT 'Duplicate (variant) script version ID',
    similarity_score DECIMAL(3, 2) NOT NULL COMMENT 'Vector similarity 0-1',
    merge_status VARCHAR(50) DEFAULT 'DETECTED' COMMENT 'DETECTED, MERGED, IGNORED, PENDING_REVIEW',
    merge_reason VARCHAR(255),
    merged_at TIMESTAMP COMMENT 'Merge completion time',
    variant_type VARCHAR(50) DEFAULT 'VARIANT' COMMENT 'VARIANT, ARCHIVED, PENDING',
    is_active INTEGER DEFAULT 1 COMMENT '1=active, 0=archived',
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER DEFAULT 0,
    INDEX idx_user_id (user_id),
    INDEX idx_master_script_id (master_script_id),
    INDEX idx_duplicate_script_id (duplicate_script_id),
    INDEX idx_merge_status (merge_status),
    INDEX idx_similarity_score (similarity_score),
    UNIQUE KEY uk_master_duplicate (master_script_id, duplicate_script_id, deleted)
);

-- ─────────────────────────────────────────
-- 4. Evolution Rule Configuration Table
-- ─────────────────────────────────────────
CREATE TABLE IF NOT EXISTS ai_knowledge_evolution_rule (
    id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    rule_type VARCHAR(50) NOT NULL UNIQUE COMMENT 'INCLUSION_RULE, UPDATE_RULE, ARCHIVAL_RULE, DEDUP_RULE',
    rule_name VARCHAR(100) NOT NULL,
    description TEXT,
    is_enabled INTEGER DEFAULT 1,
    config JSONB COMMENT 'Rule configuration parameters',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER DEFAULT 0,
    INDEX idx_rule_type (rule_type)
);

-- ─────────────────────────────────────────
-- 5. Evolution Execution Record Table
-- ─────────────────────────────────────────
CREATE TABLE IF NOT EXISTS ai_knowledge_evolution_execution (
    id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    user_id BIGINT NOT NULL,
    execution_period VARCHAR(50) COMMENT 'DAILY, WEEKLY, MONTHLY',
    execution_date DATE NOT NULL,
    rule_type VARCHAR(50) COMMENT 'Which rule(s) triggered',
    included_count INTEGER DEFAULT 0 COMMENT 'Scripts added to library',
    updated_count INTEGER DEFAULT 0 COMMENT 'Scripts updated',
    merged_count INTEGER DEFAULT 0 COMMENT 'Scripts merged',
    archived_count INTEGER DEFAULT 0 COMMENT 'Scripts archived',
    quality_improvement DECIMAL(5, 2) COMMENT 'Quality improvement percentage',
    execution_status VARCHAR(50) DEFAULT 'COMPLETED' COMMENT 'COMPLETED, FAILED, PENDING',
    error_message TEXT,
    executed_by VARCHAR(255) DEFAULT 'system',
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER DEFAULT 0,
    INDEX idx_user_id (user_id),
    INDEX idx_execution_date (execution_date),
    INDEX idx_execution_status (execution_status)
);

-- ─────────────────────────────────────────
-- 6. Evolution Report Table
-- ─────────────────────────────────────────
CREATE TABLE IF NOT EXISTS ai_knowledge_evolution_report (
    id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    user_id BIGINT NOT NULL,
    report_type VARCHAR(50) NOT NULL COMMENT 'WEEKLY, MONTHLY, CUSTOM',
    period_start DATE NOT NULL,
    period_end DATE NOT NULL,
    total_in_library INTEGER DEFAULT 0,
    new_added_count INTEGER DEFAULT 0,
    archived_count INTEGER DEFAULT 0,
    deduplication_count INTEGER DEFAULT 0,
    average_quality_score DECIMAL(5, 2),
    trend VARCHAR(20) COMMENT 'UP, DOWN, STABLE',
    report_content JSONB COMMENT 'Detailed report data',
    generated_by VARCHAR(255) DEFAULT 'system',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER DEFAULT 0,
    INDEX idx_user_id (user_id),
    INDEX idx_report_type (report_type),
    INDEX idx_period (period_start, period_end)
);

-- ─────────────────────────────────────────
-- Default Rule Configuration Data
-- ─────────────────────────────────────────
INSERT INTO ai_knowledge_evolution_rule (rule_type, rule_name, description, is_enabled, config) VALUES
('INCLUSION_RULE', 'High-Performance Script Auto-Import', 'Automatically import scripts with score >= 80 and usage >= 5', 1,
 '{"score_threshold": 80, "min_usage_count": 5, "min_creation_days_ago": 30, "max_daily_import": 100}'),

('UPDATE_RULE', 'Script Version Auto-Update', 'Automatically update script version if new score is significantly higher', 1,
 '{"score_improvement_threshold": 5, "min_new_version_score": 75, "stability_days": 14, "rollback_threshold": 10}'),

('ARCHIVAL_RULE', 'Low-Performance Script Auto-Archive', 'Automatically archive scripts with 3+ consecutive low scores', 1,
 '{"consecutive_low_score_periods": 3, "low_score_threshold": 40, "archive_days": 90}'),

('DEDUP_RULE', 'Duplicate Script Deduplication', 'Automatically merge duplicate scripts based on vector similarity', 1,
 '{"similarity_threshold": 0.85, "require_manual_approval": false, "preserve_variants": true}');
