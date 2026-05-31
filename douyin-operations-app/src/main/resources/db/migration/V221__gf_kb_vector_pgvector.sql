-- Gaifan 知识库 pgvector（可选扩展；未安装 vector 时由 apply 脚本 ON_ERROR_STOP=0 跳过）
CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE IF NOT EXISTS gf_kb_chunk_embedding (
    chunk_id bigint PRIMARY KEY,
    kb_id bigint NOT NULL,
    tenant_id varchar(64),
    embedding vector(768),
    metadata jsonb,
    created_at timestamp with time zone NOT NULL DEFAULT current_timestamp
);

CREATE INDEX IF NOT EXISTS idx_gf_kb_chunk_kb ON gf_kb_chunk_embedding (kb_id);

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_am WHERE amname = 'hnsw') THEN
        EXECUTE 'CREATE INDEX IF NOT EXISTS idx_gf_kb_chunk_embedding_hnsw ON gf_kb_chunk_embedding USING hnsw (embedding vector_cosine_ops)';
    END IF;
EXCEPTION WHEN OTHERS THEN
    RAISE NOTICE 'hnsw index skipped: %', SQLERRM;
END $$;
