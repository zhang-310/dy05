-- Gaifan KB demo：staging/prod verify RAG（user_id=1，pgvector 768 维 unit 向量）
-- 与 DeterministicEmbeddingGenerator.unitVector(768) 一致

INSERT INTO ai_knowledge_base (id, user_id, kb_name, description, total_documents, total_tokens, embedding_model, status, deleted)
SELECT 90001, 1, 'Gaifan Verify KB', 'Staging verify knowledge base', 1, 100, 'verify-stub', 1, 0
WHERE NOT EXISTS (SELECT 1 FROM ai_knowledge_base WHERE id = 90001 AND deleted = 0);

INSERT INTO ai_kb_document (id, kb_id, title, content, file_type, file_size, chunk_count, token_count, status, deleted)
SELECT 900001, 90001, 'Verify RAG Doc', 'verify rag content for gaifan staging smoke test', 'txt', 128, 1, 50, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM ai_kb_document WHERE id = 900001 AND deleted = 0);

-- unit vector: each component = 1/sqrt(768)
INSERT INTO gf_kb_chunk_embedding (chunk_id, kb_id, tenant_id, embedding, metadata)
SELECT
    9000001,
    90001,
    'demo-tenant',
    (SELECT ('[' || string_agg((1.0 / sqrt(768.0))::text, ',') || ']')::vector FROM generate_series(1, 768)),
    '{"doc_id":900001,"chunk_index":0,"title":"Verify RAG Doc","text":"verify rag content for gaifan staging smoke test"}'::jsonb
WHERE NOT EXISTS (SELECT 1 FROM gf_kb_chunk_embedding WHERE chunk_id = 9000001);

INSERT INTO gf_feature (feature_code, product_code, feature_name, quota_unit, monthly_limit, enabled)
SELECT 'knowledge-base.rag', 'knowledge-base', 'RAG 检索', 'query', 5000, true
WHERE NOT EXISTS (SELECT 1 FROM gf_feature WHERE feature_code = 'knowledge-base.rag');

INSERT INTO gf_feature (feature_code, product_code, feature_name, quota_unit, monthly_limit, enabled)
SELECT 'knowledge-base.document', 'knowledge-base', '文档上传', 'document', 5000, true
WHERE NOT EXISTS (SELECT 1 FROM gf_feature WHERE feature_code = 'knowledge-base.document');
