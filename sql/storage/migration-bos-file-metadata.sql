-- ============================================================
-- BOS 文件元数据表
-- 版本: v1.0
-- 日期: 2026-03-01
-- 说明: 追溯每个文件的成本、用途、使用次数
-- 参考: docs/design/BOS-SHORTVIDEO-INTEGRATION-ANALYSIS.md
-- ============================================================

CREATE TABLE IF NOT EXISTS bos_file_metadata (
    id                   BIGSERIAL    PRIMARY KEY,
    bos_key              VARCHAR(500) NOT NULL,
    user_id              BIGINT       NOT NULL,
    task_id              BIGINT,                                      -- 项目 ID (sv_project)
    category             VARCHAR(50),                                -- keyframe/video/audio/thumbnail/reference
    file_size            BIGINT       DEFAULT 0,
    storage_cost_monthly DECIMAL(10, 4) DEFAULT 0,
    usage_count          INTEGER      DEFAULT 0,
    shot_id              BIGINT,                                      -- 关联分镜 ID
    source_url           VARCHAR(500),                                -- 来源 URL（回源拉取时记录）
    create_time          TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    update_time          TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    deleted              INTEGER      NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_bos_file_metadata_key ON bos_file_metadata (bos_key) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_bos_file_metadata_user ON bos_file_metadata (user_id, create_time DESC);
CREATE INDEX IF NOT EXISTS idx_bos_file_metadata_task ON bos_file_metadata (task_id, category) WHERE task_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_bos_file_metadata_user_task ON bos_file_metadata (user_id, task_id) WHERE deleted = 0;

COMMENT ON TABLE bos_file_metadata IS 'BOS 文件元数据表（成本追溯、用途统计）';
COMMENT ON COLUMN bos_file_metadata.bos_key IS 'BOS 对象 Key';
COMMENT ON COLUMN bos_file_metadata.task_id IS '项目 ID (sv_project.id)';
COMMENT ON COLUMN bos_file_metadata.category IS 'keyframe/video/audio/thumbnail/reference';
COMMENT ON COLUMN bos_file_metadata.usage_count IS '被引用次数';
