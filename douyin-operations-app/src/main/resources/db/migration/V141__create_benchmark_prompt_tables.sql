-- =====================================================
-- Benchmark 模块 - Prompt 管理与质量脚本知识库
-- V101__create_benchmark_prompt_tables.sql
-- =====================================================

-- 1. Prompt 模板表
CREATE TABLE benchmark_prompt_template (
    id BIGSERIAL PRIMARY KEY,
    owner_id BIGINT NOT NULL,
    template_name VARCHAR(100) NOT NULL,
    template_code VARCHAR(50) NOT NULL,
    template_content TEXT NOT NULL,
    template_variables JSONB,
    scene_type VARCHAR(50),
    industry VARCHAR(50),
    is_active BOOLEAN DEFAULT TRUE,
    version INTEGER DEFAULT 1,
    description TEXT,
    usage_count INTEGER DEFAULT 0,
    avg_score DECIMAL(5,2) DEFAULT 0.00,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER DEFAULT 0,
    CONSTRAINT uk_template_code UNIQUE (template_code, deleted)
);

CREATE INDEX idx_prompt_template_owner ON benchmark_prompt_template(owner_id, deleted);
CREATE INDEX idx_prompt_template_scene ON benchmark_prompt_template(scene_type, deleted);
CREATE INDEX idx_prompt_template_active ON benchmark_prompt_template(is_active, deleted);

COMMENT ON TABLE benchmark_prompt_template IS 'Prompt 模板表';
COMMENT ON COLUMN benchmark_prompt_template.template_code IS '模板唯一编码';
COMMENT ON COLUMN benchmark_prompt_template.template_content IS 'Prompt 内容（支持变量占位符）';
COMMENT ON COLUMN benchmark_prompt_template.template_variables IS '变量定义（JSON 格式）';
COMMENT ON COLUMN benchmark_prompt_template.scene_type IS '场景类型（创意分析/爆款因素/竞品对比等）';
COMMENT ON COLUMN benchmark_prompt_template.industry IS '行业分类';
COMMENT ON COLUMN benchmark_prompt_template.usage_count IS '使用次数';
COMMENT ON COLUMN benchmark_prompt_template.avg_score IS '平均评分';

-- 2. Prompt 使用日志表
CREATE TABLE benchmark_prompt_usage_log (
    id BIGSERIAL PRIMARY KEY,
    owner_id BIGINT NOT NULL,
    template_id BIGINT NOT NULL,
    video_id BIGINT,
    analysis_id BIGINT,
    prompt_content TEXT NOT NULL,
    response_content TEXT,
    token_usage INTEGER,
    response_time_ms INTEGER,
    user_rating INTEGER,
    user_feedback TEXT,
    ab_test_group VARCHAR(20),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER DEFAULT 0
);

CREATE INDEX idx_usage_log_template ON benchmark_prompt_usage_log(template_id, deleted);
CREATE INDEX idx_usage_log_video ON benchmark_prompt_usage_log(video_id, deleted);
CREATE INDEX idx_usage_log_time ON benchmark_prompt_usage_log(create_time DESC);
CREATE INDEX idx_usage_log_ab_test ON benchmark_prompt_usage_log(ab_test_group, deleted);

COMMENT ON TABLE benchmark_prompt_usage_log IS 'Prompt 使用日志表';
COMMENT ON COLUMN benchmark_prompt_usage_log.prompt_content IS '实际使用的 Prompt（变量已替换）';
COMMENT ON COLUMN benchmark_prompt_usage_log.response_content IS 'AI 响应内容';
COMMENT ON COLUMN benchmark_prompt_usage_log.token_usage IS 'Token 消耗量';
COMMENT ON COLUMN benchmark_prompt_usage_log.response_time_ms IS '响应时间（毫秒）';
COMMENT ON COLUMN benchmark_prompt_usage_log.user_rating IS '用户评分（1-5）';
COMMENT ON COLUMN benchmark_prompt_usage_log.ab_test_group IS 'A/B 测试分组';

-- 3. Prompt A/B 测试表
CREATE TABLE benchmark_prompt_ab_test (
    id BIGSERIAL PRIMARY KEY,
    owner_id BIGINT NOT NULL,
    test_name VARCHAR(100) NOT NULL,
    test_code VARCHAR(50) NOT NULL,
    template_a_id BIGINT NOT NULL,
    template_b_id BIGINT NOT NULL,
    traffic_split INTEGER DEFAULT 50,
    start_time TIMESTAMP,
    end_time TIMESTAMP,
    status VARCHAR(20) DEFAULT 'draft',
    winner_template_id BIGINT,
    total_usage_a INTEGER DEFAULT 0,
    total_usage_b INTEGER DEFAULT 0,
    avg_score_a DECIMAL(5,2) DEFAULT 0.00,
    avg_score_b DECIMAL(5,2) DEFAULT 0.00,
    confidence_level DECIMAL(5,2),
    conclusion TEXT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER DEFAULT 0,
    CONSTRAINT uk_test_code UNIQUE (test_code, deleted)
);

CREATE INDEX idx_benchmark_prompt_ab_test_owner ON benchmark_prompt_ab_test(owner_id, deleted);
CREATE INDEX idx_benchmark_prompt_ab_test_status ON benchmark_prompt_ab_test(status, deleted);
CREATE INDEX idx_benchmark_prompt_ab_test_time ON benchmark_prompt_ab_test(start_time, end_time);

COMMENT ON TABLE benchmark_prompt_ab_test IS 'Prompt A/B 测试表';
COMMENT ON COLUMN benchmark_prompt_ab_test.traffic_split IS '流量分配比例（A 组百分比）';
COMMENT ON COLUMN benchmark_prompt_ab_test.status IS '测试状态（draft/running/completed/cancelled）';
COMMENT ON COLUMN benchmark_prompt_ab_test.confidence_level IS '置信度';

-- 4. 质量脚本知识库表
CREATE TABLE benchmark_quality_script (
    id BIGSERIAL PRIMARY KEY,
    owner_id BIGINT NOT NULL,
    video_id BIGINT NOT NULL,
    analysis_id BIGINT NOT NULL,
    script_content TEXT NOT NULL,
    script_type VARCHAR(50),
    industry VARCHAR(50),
    scene_type VARCHAR(50),
    quality_score DECIMAL(5,2) NOT NULL,
    engagement_rate DECIMAL(5,2),
    viral_score DECIMAL(5,2),
    completion_rate DECIMAL(5,2),
    ai_rating DECIMAL(5,2),
    likes_count INTEGER DEFAULT 0,
    comments_count INTEGER DEFAULT 0,
    shares_count INTEGER DEFAULT 0,
    collections_count INTEGER DEFAULT 0,
    views_count INTEGER DEFAULT 0,
    video_duration INTEGER,
    key_features JSONB,
    creative_elements JSONB,
    hook_strategy TEXT,
    content_structure TEXT,
    embedding_vector TEXT,
    reference_count INTEGER DEFAULT 0,
    last_referenced_at TIMESTAMP,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER DEFAULT 0
);

CREATE INDEX idx_quality_script_owner ON benchmark_quality_script(owner_id, deleted);
CREATE INDEX idx_quality_script_video ON benchmark_quality_script(video_id, deleted);
CREATE INDEX idx_quality_script_score ON benchmark_quality_script(quality_score DESC, deleted);
CREATE INDEX idx_quality_script_industry ON benchmark_quality_script(industry, deleted);
CREATE INDEX idx_quality_script_scene ON benchmark_quality_script(scene_type, deleted);
CREATE INDEX idx_quality_script_engagement ON benchmark_quality_script(engagement_rate DESC);

COMMENT ON TABLE benchmark_quality_script IS '质量脚本知识库表';
COMMENT ON COLUMN benchmark_quality_script.quality_score IS '综合质量评分（0-100）';
COMMENT ON COLUMN benchmark_quality_script.engagement_rate IS '互动率（%）';
COMMENT ON COLUMN benchmark_quality_script.viral_score IS '传播力评分';
COMMENT ON COLUMN benchmark_quality_script.completion_rate IS '完播率（%）';
COMMENT ON COLUMN benchmark_quality_script.ai_rating IS 'AI 评分';
COMMENT ON COLUMN benchmark_quality_script.key_features IS '关键特征（JSON）';
COMMENT ON COLUMN benchmark_quality_script.creative_elements IS '创意元素（JSON）';
COMMENT ON COLUMN benchmark_quality_script.embedding_vector IS '向量表示（TEXT 存序列化向量；生产可迁移为 pgvector VECTOR(1536)）';
COMMENT ON COLUMN benchmark_quality_script.reference_count IS '被引用次数';

-- 5. 脚本相似度索引表
CREATE TABLE benchmark_script_similarity (
    id BIGSERIAL PRIMARY KEY,
    owner_id BIGINT NOT NULL,
    source_script_id BIGINT NOT NULL,
    target_script_id BIGINT NOT NULL,
    similarity_score DECIMAL(5,4) NOT NULL,
    similarity_type VARCHAR(20) DEFAULT 'semantic',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER DEFAULT 0,
    CONSTRAINT uk_script_similarity UNIQUE (source_script_id, target_script_id, deleted)
);

CREATE INDEX idx_similarity_source ON benchmark_script_similarity(source_script_id, similarity_score DESC);
CREATE INDEX idx_similarity_target ON benchmark_script_similarity(target_script_id, deleted);
CREATE INDEX idx_similarity_score ON benchmark_script_similarity(similarity_score DESC);

COMMENT ON TABLE benchmark_script_similarity IS '脚本相似度索引表';
COMMENT ON COLUMN benchmark_script_similarity.similarity_score IS '相似度分数（0-1）';
COMMENT ON COLUMN benchmark_script_similarity.similarity_type IS '相似度类型（semantic/structural/keyword）';

-- 6. 脚本使用效果表
CREATE TABLE benchmark_script_usage_effect (
    id BIGSERIAL PRIMARY KEY,
    owner_id BIGINT NOT NULL,
    quality_script_id BIGINT NOT NULL,
    usage_video_id BIGINT NOT NULL,
    usage_analysis_id BIGINT NOT NULL,
    usage_context TEXT,
    effect_rating INTEGER,
    effect_feedback TEXT,
    improvement_suggestions TEXT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER DEFAULT 0
);

CREATE INDEX idx_usage_effect_script ON benchmark_script_usage_effect(quality_script_id, deleted);
CREATE INDEX idx_usage_effect_video ON benchmark_script_usage_effect(usage_video_id, deleted);
CREATE INDEX idx_usage_effect_rating ON benchmark_script_usage_effect(effect_rating DESC);

COMMENT ON TABLE benchmark_script_usage_effect IS '脚本使用效果表';
COMMENT ON COLUMN benchmark_script_usage_effect.usage_context IS '使用场景描述';
COMMENT ON COLUMN benchmark_script_usage_effect.effect_rating IS '效果评分（1-5）';
COMMENT ON COLUMN benchmark_script_usage_effect.improvement_suggestions IS '改进建议';
