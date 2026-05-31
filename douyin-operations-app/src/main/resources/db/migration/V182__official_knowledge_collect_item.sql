CREATE TABLE IF NOT EXISTS ai_official_knowledge_collect_item (
    id BIGSERIAL PRIMARY KEY,
    source_type VARCHAR(64) NOT NULL DEFAULT 'douyin_school_official',
    source_site VARCHAR(128) NOT NULL DEFAULT 'school.jinritemai.com',
    source_url TEXT NOT NULL,
    source_url_hash VARCHAR(64) NOT NULL,
    source_id VARCHAR(128),
    title VARCHAR(512),
    category VARCHAR(128),
    topic_code VARCHAR(64),
    target_kb_name VARCHAR(128),
    target_kb_id BIGINT,
    doc_id BIGINT,
    is_violation BOOLEAN NOT NULL DEFAULT FALSE,
    collect_status VARCHAR(32) NOT NULL DEFAULT 'DISCOVERED',
    index_status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    ocr_status VARCHAR(32) NOT NULL DEFAULT 'NOT_REQUIRED',
    asr_status VARCHAR(32) NOT NULL DEFAULT 'NOT_REQUIRED',
    image_count INTEGER NOT NULL DEFAULT 0,
    video_count INTEGER NOT NULL DEFAULT 0,
    image_text_count INTEGER NOT NULL DEFAULT 0,
    video_text_count INTEGER NOT NULL DEFAULT 0,
    retry_count INTEGER NOT NULL DEFAULT 0,
    last_error TEXT,
    last_discovered_at TIMESTAMP,
    last_collected_at TIMESTAMP,
    last_indexed_at TIMESTAMP,
    last_media_extract_at TIMESTAMP,
    next_retry_at TIMESTAMP,
    last_recollect_at TIMESTAMP,
    official_update_timestamp BIGINT,
    metadata TEXT,
    deleted INTEGER NOT NULL DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_ai_official_collect_source_url
    ON ai_official_knowledge_collect_item (source_type, source_url_hash)
    WHERE deleted = 0;

CREATE INDEX IF NOT EXISTS idx_ai_official_collect_status
    ON ai_official_knowledge_collect_item (collect_status, update_time DESC)
    WHERE deleted = 0;

CREATE INDEX IF NOT EXISTS idx_ai_official_collect_kb
    ON ai_official_knowledge_collect_item (target_kb_name, is_violation)
    WHERE deleted = 0;

CREATE INDEX IF NOT EXISTS idx_ai_official_collect_media
    ON ai_official_knowledge_collect_item (ocr_status, asr_status)
    WHERE deleted = 0;

INSERT INTO ai_official_knowledge_collect_item (
    source_type,
    source_site,
    source_url,
    source_url_hash,
    source_id,
    title,
    category,
    target_kb_name,
    target_kb_id,
    doc_id,
    is_violation,
    collect_status,
    index_status,
    ocr_status,
    asr_status,
    image_count,
    video_count,
    image_text_count,
    video_text_count,
    last_discovered_at,
    last_collected_at,
    last_indexed_at,
    last_media_extract_at,
    official_update_timestamp,
    metadata,
    create_time,
    update_time
)
SELECT
    d.source_type,
    'school.jinritemai.com',
    COALESCE(d.metadata::jsonb ->> 'sourceUrl', 'doc:' || d.id::text),
    md5(COALESCE(d.metadata::jsonb ->> 'sourceUrl', 'doc:' || d.id::text)),
    NULLIF(d.metadata::jsonb ->> 'sourceId', ''),
    d.title,
    NULLIF(d.metadata::jsonb ->> 'category', ''),
    kb.kb_name,
    kb.id,
    d.id,
    kb.kb_name = 'douyin_weigui' OR COALESCE(d.metadata::jsonb ->> 'contentType', '') = 'official_rule',
    'INDEXED',
    CASE WHEN d.status = 1 THEN 'INDEXED' ELSE 'PENDING' END,
    CASE
        WHEN COALESCE(NULLIF(d.metadata::jsonb ->> 'imageCount', ''), '0')::integer > 0
             AND COALESCE(NULLIF(d.metadata::jsonb ->> 'imageTextCount', ''), '0')::integer > 0 THEN 'DONE'
        WHEN COALESCE(NULLIF(d.metadata::jsonb ->> 'imageCount', ''), '0')::integer > 0 THEN 'PENDING'
        ELSE 'NOT_REQUIRED'
    END,
    CASE
        WHEN COALESCE(NULLIF(d.metadata::jsonb ->> 'videoCount', ''), '0')::integer > 0
             AND COALESCE(NULLIF(d.metadata::jsonb ->> 'videoTextCount', ''), '0')::integer > 0 THEN 'DONE'
        WHEN COALESCE(NULLIF(d.metadata::jsonb ->> 'videoCount', ''), '0')::integer > 0 THEN 'PENDING'
        ELSE 'NOT_REQUIRED'
    END,
    COALESCE(NULLIF(d.metadata::jsonb ->> 'imageCount', ''), '0')::integer,
    COALESCE(NULLIF(d.metadata::jsonb ->> 'videoCount', ''), '0')::integer,
    COALESCE(NULLIF(d.metadata::jsonb ->> 'imageTextCount', ''), '0')::integer,
    COALESCE(NULLIF(d.metadata::jsonb ->> 'videoTextCount', ''), '0')::integer,
    d.create_time,
    d.create_time,
    CASE WHEN d.status = 1 THEN d.update_time ELSE NULL END,
    d.update_time,
    NULLIF(d.metadata::jsonb ->> 'officialUpdateTimestamp', '')::bigint,
    d.metadata,
    d.create_time,
    d.update_time
FROM ai_kb_document d
JOIN ai_knowledge_base kb ON kb.id = d.kb_id
WHERE d.deleted = 0
  AND d.source_type = 'douyin_school_official'
  AND d.metadata IS NOT NULL
  AND d.metadata <> ''
  AND d.metadata::jsonb ? 'sourceUrl'
ON CONFLICT DO NOTHING;
