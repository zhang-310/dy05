-- ============================================================
-- Create dy_optimization_suggestion table
-- PostgreSQL compatible version
-- ============================================================

CREATE TABLE IF NOT EXISTS dy_optimization_suggestion (
    id                      BIGSERIAL           PRIMARY KEY,
    script_version_id       BIGINT              NOT NULL,
    analysis_result_id      BIGINT              NOT NULL,
    owner_id                BIGINT              NOT NULL,
    category                VARCHAR(64)         NOT NULL,
    priority                VARCHAR(20)         NOT NULL,
    suggestion_content      TEXT                NOT NULL,
    related_weak_point      VARCHAR(256),
    expected_improvement    JSONB,
    adoption_status         VARCHAR(20)         DEFAULT 'PENDING',
    adopted_at              TIMESTAMP,
    adoption_notes          TEXT,
    created_at              TIMESTAMP           NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP           NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at              TIMESTAMP,
    deleted                 INTEGER             NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_optimization_suggestion_script_version_id
    ON dy_optimization_suggestion (script_version_id) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_optimization_suggestion_analysis_result_id
    ON dy_optimization_suggestion (analysis_result_id) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_optimization_suggestion_owner_id
    ON dy_optimization_suggestion (owner_id) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_optimization_suggestion_category
    ON dy_optimization_suggestion (category) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_optimization_suggestion_priority
    ON dy_optimization_suggestion (priority) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_optimization_suggestion_adoption_status
    ON dy_optimization_suggestion (adoption_status) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_optimization_suggestion_created_at_desc
    ON dy_optimization_suggestion (created_at DESC) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_optimization_suggestion_deleted
    ON dy_optimization_suggestion (deleted);

COMMENT ON TABLE dy_optimization_suggestion IS 'Optimization suggestion table';
COMMENT ON COLUMN dy_optimization_suggestion.script_version_id IS 'Related script version ID';
COMMENT ON COLUMN dy_optimization_suggestion.analysis_result_id IS 'Related analysis result ID';
COMMENT ON COLUMN dy_optimization_suggestion.owner_id IS 'Owner ID for data isolation';
COMMENT ON COLUMN dy_optimization_suggestion.category IS 'Suggestion category (CONTENT/PACING/STYLE/TOPIC)';
COMMENT ON COLUMN dy_optimization_suggestion.priority IS 'Priority (LOW/MEDIUM/HIGH/CRITICAL)';
COMMENT ON COLUMN dy_optimization_suggestion.suggestion_content IS 'Suggestion content description';
COMMENT ON COLUMN dy_optimization_suggestion.related_weak_point IS 'Related weak point type';
COMMENT ON COLUMN dy_optimization_suggestion.expected_improvement IS 'Expected improvement JSON';
COMMENT ON COLUMN dy_optimization_suggestion.adoption_status IS 'Adoption status (PENDING/ACCEPTED/REJECTED/APPLIED)';
COMMENT ON COLUMN dy_optimization_suggestion.adopted_at IS 'Adoption time';
COMMENT ON COLUMN dy_optimization_suggestion.adoption_notes IS 'Adoption notes';
COMMENT ON COLUMN dy_optimization_suggestion.created_at IS 'Creation time';
COMMENT ON COLUMN dy_optimization_suggestion.updated_at IS 'Update time';
COMMENT ON COLUMN dy_optimization_suggestion.deleted_at IS 'Deletion time';
COMMENT ON COLUMN dy_optimization_suggestion.deleted IS 'Logical deletion flag (0=normal 1=deleted)';
