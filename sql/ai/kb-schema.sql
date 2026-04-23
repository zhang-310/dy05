-- AI 知识库文档表
CREATE TABLE IF NOT EXISTS ai_kb_document (
    id BIGSERIAL PRIMARY KEY,
    kb_id BIGINT NOT NULL,
    title VARCHAR(256) NOT NULL,
    content TEXT,
    file_type VARCHAR(32),
    file_size BIGINT,
    chunk_count INTEGER NOT NULL DEFAULT 0,
    token_count INTEGER NOT NULL DEFAULT 0,
    status INTEGER NOT NULL DEFAULT 0,
    boost_factor DECIMAL(5,2) DEFAULT 1.00,
    deleted INTEGER NOT NULL DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_kb_document_kb_id ON ai_kb_document(kb_id);
CREATE INDEX idx_kb_document_deleted ON ai_kb_document(deleted);

COMMENT ON TABLE ai_kb_document IS 'AI 知识库文档表';
COMMENT ON COLUMN ai_kb_document.kb_id IS '知识库ID';
COMMENT ON COLUMN ai_kb_document.title IS '文档标题';
COMMENT ON COLUMN ai_kb_document.content IS '文档内容';
COMMENT ON COLUMN ai_kb_document.file_type IS '文件类型';
COMMENT ON COLUMN ai_kb_document.file_size IS '文件大小';
COMMENT ON COLUMN ai_kb_document.chunk_count IS '分块数量';
COMMENT ON COLUMN ai_kb_document.token_count IS 'Token 数量';
COMMENT ON COLUMN ai_kb_document.status IS '状态：0=处理中 1=已索引';
COMMENT ON COLUMN ai_kb_document.deleted IS '软删除标记';
