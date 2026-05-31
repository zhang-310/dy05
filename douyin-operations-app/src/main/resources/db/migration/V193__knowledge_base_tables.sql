-- V193: 知识库表 (关联 V184)
CREATE TABLE IF NOT EXISTS sys_knowledge_base (
    id BIGSERIAL PRIMARY KEY,
    tenant_id VARCHAR(128) NOT NULL DEFAULT 'default',
    kb_name VARCHAR(256) NOT NULL,
    description TEXT,
    doc_count INT DEFAULT 0,
    embedding_model VARCHAR(64) DEFAULT 'text-embedding-3-small',
    create_time TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS sys_knowledge_document (
    id BIGSERIAL PRIMARY KEY,
    kb_id BIGINT NOT NULL REFERENCES sys_knowledge_base(id),
    title VARCHAR(512) NOT NULL,
    content TEXT,
    source_type VARCHAR(32) DEFAULT 'upload',
    source_url VARCHAR(1024),
    chunk_count INT DEFAULT 0,
    status VARCHAR(16) DEFAULT 'pending',
    create_time TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_kb_doc ON sys_knowledge_document(kb_id, status);
