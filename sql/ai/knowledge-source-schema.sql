-- ============================================================
-- ai_knowledge_source 知识源管理表
-- 版本：1.0 | 更新日期：2026-02-28
-- 用途：知识源目录、扫描、索引状态管理
-- ============================================================

CREATE TABLE IF NOT EXISTS ai_knowledge_source (
    id                  BIGSERIAL       PRIMARY KEY,
    source_name         VARCHAR(128)    NOT NULL,                           -- 知识源名称
    source_path         VARCHAR(512)    NOT NULL,                           -- 知识源路径（文件目录）
    source_type         VARCHAR(32)     NOT NULL DEFAULT 'local',           -- 类型：local / remote
    file_count          INTEGER         NOT NULL DEFAULT 0,                 -- 文件数量
    index_count         INTEGER         NOT NULL DEFAULT 0,                 -- 已索引数量
    last_index_time     TIMESTAMP,                                           -- 最后索引时间
    status              INTEGER         NOT NULL DEFAULT 1,                 -- 1=启用 0=禁用
    deleted             INTEGER         NOT NULL DEFAULT 0,                 -- 逻辑删除
    create_time         TIMESTAMP                DEFAULT CURRENT_TIMESTAMP,
    update_time         TIMESTAMP                DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_ai_knowledge_source_status ON ai_knowledge_source (status) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_ai_knowledge_source_type ON ai_knowledge_source (source_type) WHERE deleted = 0;

COMMENT ON TABLE ai_knowledge_source IS '知识源管理：目录、扫描、索引状态';
COMMENT ON COLUMN ai_knowledge_source.source_name IS '知识源名称';
COMMENT ON COLUMN ai_knowledge_source.source_path IS '知识源路径（文件目录）';
COMMENT ON COLUMN ai_knowledge_source.source_type IS '类型：local（本地）/ remote（远程）';
COMMENT ON COLUMN ai_knowledge_source.file_count IS '文件数量';
COMMENT ON COLUMN ai_knowledge_source.index_count IS '已索引数量';
COMMENT ON COLUMN ai_knowledge_source.last_index_time IS '最后索引时间';
