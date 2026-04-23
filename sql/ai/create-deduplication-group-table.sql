-- ========================================
-- Create ai_knowledge_deduplication_group table
-- PostgreSQL compatible version
-- ========================================

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

-- Create indexes
CREATE INDEX IF NOT EXISTS idx_dedup_group_user_id ON ai_knowledge_deduplication_group (user_id) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_dedup_group_master_script_id ON ai_knowledge_deduplication_group (master_script_id) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_dedup_group_duplicate_script_id ON ai_knowledge_deduplication_group (duplicate_script_id) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_dedup_group_merge_status ON ai_knowledge_deduplication_group (merge_status) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_dedup_group_similarity_score ON ai_knowledge_deduplication_group (similarity_score) WHERE deleted = 0;

-- Add comments
COMMENT ON TABLE ai_knowledge_deduplication_group IS 'Knowledge deduplication group table - Groups duplicate/variant scripts together with similarity scores';
COMMENT ON COLUMN ai_knowledge_deduplication_group.master_script_id IS 'Master (primary) script version ID';
COMMENT ON COLUMN ai_knowledge_deduplication_group.duplicate_script_id IS 'Duplicate (variant) script version ID';
COMMENT ON COLUMN ai_knowledge_deduplication_group.similarity_score IS 'Vector similarity 0-1';
COMMENT ON COLUMN ai_knowledge_deduplication_group.merge_status IS 'DETECTED, MERGED, IGNORED, PENDING_REVIEW';
COMMENT ON COLUMN ai_knowledge_deduplication_group.merged_at IS 'Merge completion time';
COMMENT ON COLUMN ai_knowledge_deduplication_group.variant_type IS 'VARIANT, ARCHIVED, PENDING';
COMMENT ON COLUMN ai_knowledge_deduplication_group.is_active IS '1=active, 0=archived';
