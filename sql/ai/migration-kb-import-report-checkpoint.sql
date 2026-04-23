-- ============================================================
-- Phase 4：导入报告与增量导入 checkpoint
-- ============================================================

CREATE TABLE IF NOT EXISTS kb_import_report (
    id BIGSERIAL PRIMARY KEY,
    kb_id BIGINT NOT NULL,
    source_path VARCHAR(500),
    import_type VARCHAR(16),
    total_files INT DEFAULT 0,
    success_count INT DEFAULT 0,
    failed_count INT DEFAULT 0,
    skipped_count INT DEFAULT 0,
    dedup_skipped INT DEFAULT 0,
    dedup_downweighted INT DEFAULT 0,
    new_chunks INT DEFAULT 0,
    content_type VARCHAR(16),
    errors JSONB,
    duration_ms BIGINT,
    user_id BIGINT NOT NULL,
    deleted INTEGER NOT NULL DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE kb_import_report IS '知识库导入报告（目录/文件上传/增量）';
CREATE INDEX IF NOT EXISTS idx_import_report_kb ON kb_import_report(kb_id, create_time DESC);

CREATE TABLE IF NOT EXISTS kb_import_checkpoint (
    id BIGSERIAL PRIMARY KEY,
    kb_id BIGINT NOT NULL,
    source_path VARCHAR(500) NOT NULL,
    last_import_time TIMESTAMP NOT NULL,
    file_count INT DEFAULT 0,
    deleted INTEGER NOT NULL DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(kb_id, source_path)
);

COMMENT ON TABLE kb_import_checkpoint IS '增量导入时间戳（按路径只处理新/改文件）';
