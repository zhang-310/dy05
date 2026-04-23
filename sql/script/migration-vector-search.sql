-- Vector Search System Migration for Script Module
-- Creates tables for vector embeddings, search results, and search suggestions
-- Created: 2026-03-06

-- 1. ScriptVectorEmbedding: 向量嵌入存储表
CREATE TABLE IF NOT EXISTS sc_script_vector_embedding (
    id BIGSERIAL PRIMARY KEY,
    script_id BIGINT NOT NULL,
    owner_id BIGINT NOT NULL,
    title VARCHAR(256),
    content_text TEXT,
    vector_embedding BYTEA NOT NULL,                    -- 1024D 向量，存为 FLOAT32 数组 (4096 字节)
    vector_dimension INTEGER DEFAULT 1024,
    embedding_model VARCHAR(100) DEFAULT 'BGE-M3',     -- 模型名称
    embedding_time_ms INTEGER DEFAULT 0,                -- 向量生成耗时 (ms)
    is_indexed_milvus BOOLEAN DEFAULT FALSE,            -- 是否已导入 Milvus
    milvus_collection_id BIGINT,                        -- Milvus 集合 ID
    last_indexed_at TIMESTAMP,                          -- 最后索引时间
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    deleted INTEGER DEFAULT 0,
    CONSTRAINT fk_script_vector_embedding_script FOREIGN KEY (script_id)
        REFERENCES script_library(id) ON DELETE CASCADE
);

CREATE INDEX idx_script_vector_embedding_script_id ON sc_script_vector_embedding(script_id);
CREATE INDEX idx_script_vector_embedding_owner_id ON sc_script_vector_embedding(owner_id);
CREATE INDEX idx_script_vector_embedding_is_indexed ON sc_script_vector_embedding(is_indexed_milvus);
CREATE INDEX idx_script_vector_embedding_created_at ON sc_script_vector_embedding(created_at DESC);


-- 2. SearchSuggestion: 搜索建议词库
CREATE TABLE IF NOT EXISTS sc_search_suggestion (
    id BIGSERIAL PRIMARY KEY,
    owner_id BIGINT,                                     -- NULL 表示全局建议
    suggestion_text VARCHAR(256) NOT NULL,
    suggestion_type VARCHAR(50) NOT NULL,               -- HISTORY / HOT_TOPIC / RECOMMENDED / SYSTEM
    search_count INTEGER DEFAULT 0,                     -- 被搜索的次数
    trending_score DECIMAL(4,3) DEFAULT 0.000,          -- 热度评分 [0-1]
    result_count INTEGER DEFAULT 0,                     -- 匹配的脚本数
    last_searched_at TIMESTAMP,                         -- 最后被搜索的时间
    last_updated TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER DEFAULT 0,
    UNIQUE(owner_id, suggestion_text, suggestion_type)
);

CREATE INDEX idx_search_suggestion_text ON sc_search_suggestion(suggestion_text);
CREATE INDEX idx_search_suggestion_type ON sc_search_suggestion(suggestion_type);
CREATE INDEX idx_search_suggestion_trending ON sc_search_suggestion(trending_score DESC);
CREATE INDEX idx_search_suggestion_owner_id ON sc_search_suggestion(owner_id);


-- 3. SearchResult: 搜索结果记录（用于分析）
CREATE TABLE IF NOT EXISTS sc_search_result (
    id BIGSERIAL PRIMARY KEY,
    owner_id BIGINT NOT NULL,
    query_text TEXT NOT NULL,
    search_type VARCHAR(50) NOT NULL,                   -- HYBRID / SEMANTIC / LEXICAL
    total_results INTEGER DEFAULT 0,
    top_result_id BIGINT,
    execution_time_ms INTEGER DEFAULT 0,
    vector_weight DECIMAL(3,2) DEFAULT 0.50,            -- 向量搜索权重
    lexical_weight DECIMAL(3,2) DEFAULT 0.50,           -- BM25 权重
    clicked_result_id BIGINT,                           -- 用户点击的结果 ID
    clicked_at TIMESTAMP,
    is_satisfied BOOLEAN DEFAULT NULL,                  -- NULL=未反馈, TRUE=满意, FALSE=不满意
    feedback_at TIMESTAMP,
    user_agent VARCHAR(512),
    ip_address VARCHAR(45),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER DEFAULT 0
);

CREATE INDEX idx_search_result_owner_id ON sc_search_result(owner_id);
CREATE INDEX idx_search_result_query_text ON sc_search_result USING gin(to_tsvector('english', query_text));
CREATE INDEX idx_search_result_search_type ON sc_search_result(search_type);
CREATE INDEX idx_search_result_created_at ON sc_search_result(created_at DESC);


-- 4. SearchAnalytics: 搜索分析数据（按日期聚合）
CREATE TABLE IF NOT EXISTS sc_search_analytics (
    id BIGSERIAL PRIMARY KEY,
    owner_id BIGINT,                                     -- NULL 表示全局统计
    analytics_date DATE NOT NULL,
    search_query VARCHAR(512),
    search_type VARCHAR(50),                            -- HYBRID / SEMANTIC / LEXICAL
    search_count INTEGER DEFAULT 0,
    avg_execution_time_ms DECIMAL(10,2) DEFAULT 0,
    click_through_rate DECIMAL(5,2) DEFAULT 0,          -- 点击率 [0-100]
    satisfaction_score DECIMAL(4,2) DEFAULT 0,          -- 满意度 [0-1]
    top_result_id BIGINT,
    top_result_click_count INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    deleted INTEGER DEFAULT 0,
    UNIQUE(owner_id, analytics_date, search_query, search_type)
);

CREATE INDEX idx_search_analytics_owner_id ON sc_search_analytics(owner_id);
CREATE INDEX idx_search_analytics_date ON sc_search_analytics(analytics_date DESC);
CREATE INDEX idx_search_analytics_query ON sc_search_analytics(search_query);


-- 5. VectorEmbeddingTask: 定时任务执行日志
CREATE TABLE IF NOT EXISTS sc_vector_embedding_task (
    id BIGSERIAL PRIMARY KEY,
    task_type VARCHAR(50) NOT NULL,                     -- EMBEDDING / INDEXING / RERANKING
    batch_size INTEGER DEFAULT 100,
    start_time TIMESTAMP,
    end_time TIMESTAMP,
    duration_ms INTEGER,
    processed_count INTEGER DEFAULT 0,
    failed_count INTEGER DEFAULT 0,
    error_message TEXT,
    status VARCHAR(50) DEFAULT 'PENDING',               -- PENDING / RUNNING / SUCCESS / FAILED
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_vector_embedding_task_type ON sc_vector_embedding_task(task_type);
CREATE INDEX idx_vector_embedding_task_status ON sc_vector_embedding_task(status);
CREATE INDEX idx_vector_embedding_task_created_at ON sc_vector_embedding_task(created_at DESC);


-- 6. Add columns to script_library for vector search optimization
ALTER TABLE script_library ADD COLUMN IF NOT EXISTS has_vector_embedding BOOLEAN DEFAULT FALSE;
ALTER TABLE script_library ADD COLUMN IF NOT EXISTS vector_embedding_score DECIMAL(4,3) DEFAULT NULL;
CREATE INDEX idx_script_library_has_vector ON script_library(has_vector_embedding) WHERE deleted = 0;

COMMIT;
