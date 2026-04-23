-- ============================================================
-- 智能推荐优化 - 机器学习模型相关表
-- 创建时间: 2026-04-04
-- 说明: 支持基于机器学习的风格推荐系统
-- ============================================================

-- 1. 风格推荐模型表
CREATE TABLE IF NOT EXISTS style_recommendation_model (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    model_type VARCHAR(50) NOT NULL,  -- 'collaborative_filtering', 'rule_based', 'hybrid'
    model_version VARCHAR(20) NOT NULL,
    model_data TEXT,  -- JSON格式存储模型参数（相似度矩阵、权重等）
    training_samples INTEGER DEFAULT 0,  -- 训练样本数量
    accuracy DECIMAL(5,4),  -- 准确率
    precision_score DECIMAL(5,4),  -- 精确率
    recall_score DECIMAL(5,4),  -- 召回率
    f1_score DECIMAL(5,4),  -- F1分数
    top3_hit_rate DECIMAL(5,4),  -- Top-3命中率
    avg_score_improvement DECIMAL(5,2),  -- 平均评分提升
    trained_at TIMESTAMP NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER DEFAULT 0
);

CREATE INDEX idx_srm_user_active ON style_recommendation_model(user_id, is_active, deleted);
CREATE INDEX idx_srm_type_version ON style_recommendation_model(model_type, model_version, deleted);

COMMENT ON TABLE style_recommendation_model IS '风格推荐模型表';
COMMENT ON COLUMN style_recommendation_model.model_type IS '模型类型：collaborative_filtering(协同过滤), rule_based(规则引擎), hybrid(混合)';
COMMENT ON COLUMN style_recommendation_model.model_data IS 'JSON格式存储模型参数，如相似度矩阵、特征权重等';
COMMENT ON COLUMN style_recommendation_model.training_samples IS '训练样本数量';
COMMENT ON COLUMN style_recommendation_model.top3_hit_rate IS 'Top-3命中率：推荐的前3个风格中包含最终高分风格的比例';
COMMENT ON COLUMN style_recommendation_model.avg_score_improvement IS '平均评分提升：使用ML推荐后话术评分的平均提升幅度';

-- 2. 风格推荐反馈表
CREATE TABLE IF NOT EXISTS style_recommendation_feedback (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL,
    recommended_styles TEXT,  -- JSON数组，推荐的风格列表
    selected_styles TEXT,  -- JSON数组，用户实际选择的风格
    recommendation_source VARCHAR(20),  -- 'rule_based', 'ml_model', 'hybrid'
    model_version VARCHAR(20),  -- 使用的模型版本
    effectiveness_scores TEXT,  -- JSON对象，各风格的最终效果评分 {style: score}
    best_style VARCHAR(50),  -- 最终效果最好的风格
    best_score DECIMAL(5,2),  -- 最佳风格的评分
    is_top3_hit BOOLEAN,  -- 最佳风格是否在推荐的Top-3中
    user_id BIGINT NOT NULL,
    feedback_type VARCHAR(20) DEFAULT 'implicit',  -- 'implicit'(隐式反馈), 'explicit'(显式反馈)
    user_rating INTEGER,  -- 用户显式评分（1-5星，可选）
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER DEFAULT 0
);

CREATE INDEX idx_srf_product ON style_recommendation_feedback(product_id, deleted);
CREATE INDEX idx_srf_user ON style_recommendation_feedback(user_id, deleted);
CREATE INDEX idx_srf_source ON style_recommendation_feedback(recommendation_source, deleted);
CREATE INDEX idx_srf_created ON style_recommendation_feedback(created_at DESC);

COMMENT ON TABLE style_recommendation_feedback IS '风格推荐反馈表，记录推荐结果和实际效果';
COMMENT ON COLUMN style_recommendation_feedback.recommended_styles IS 'JSON数组，推荐的风格列表，如["professional","warm","enthusiastic"]';
COMMENT ON COLUMN style_recommendation_feedback.selected_styles IS 'JSON数组，用户实际选择生成的风格';
COMMENT ON COLUMN style_recommendation_feedback.recommendation_source IS '推荐来源：rule_based(规则引擎), ml_model(机器学习), hybrid(混合)';
COMMENT ON COLUMN style_recommendation_feedback.effectiveness_scores IS 'JSON对象，各风格的最终效果评分，如{"professional":85.5,"warm":78.2}';
COMMENT ON COLUMN style_recommendation_feedback.is_top3_hit IS '最佳风格是否在推荐的Top-3中（用于计算Top-3命中率）';
COMMENT ON COLUMN style_recommendation_feedback.feedback_type IS '反馈类型：implicit(隐式，基于效果数据), explicit(显式，用户主动评分)';

-- 3. 商品特征缓存表（用于加速相似度计算）
CREATE TABLE IF NOT EXISTS product_feature_cache (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL UNIQUE,
    feature_vector TEXT NOT NULL,  -- JSON数组，特征向量
    category_encoded TEXT,  -- JSON对象，分类特征编码
    price_normalized DECIMAL(10,6),  -- 归一化价格
    text_features TEXT,  -- JSON对象，文本特征（TF-IDF或词嵌入）
    historical_avg_score DECIMAL(5,2),  -- 历史平均评分
    historical_usage_count INTEGER DEFAULT 0,  -- 历史使用次数
    feature_version VARCHAR(20) DEFAULT '1.0',  -- 特征版本
    computed_at TIMESTAMP NOT NULL,
    user_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER DEFAULT 0
);

CREATE INDEX idx_pfc_product ON product_feature_cache(product_id, deleted);
CREATE INDEX idx_pfc_user ON product_feature_cache(user_id, deleted);
CREATE INDEX idx_pfc_version ON product_feature_cache(feature_version, deleted);

COMMENT ON TABLE product_feature_cache IS '商品特征缓存表，存储预计算的特征向量';
COMMENT ON COLUMN product_feature_cache.feature_vector IS 'JSON数组，完整的特征向量，用于相似度计算';
COMMENT ON COLUMN product_feature_cache.category_encoded IS 'JSON对象，分类特征的One-Hot编码';
COMMENT ON COLUMN product_feature_cache.text_features IS 'JSON对象，从商品描述和卖点提取的文本特征';
COMMENT ON COLUMN product_feature_cache.feature_version IS '特征版本号，特征工程变更时递增';

-- 4. 商品相似度矩阵表（用于协同过滤）
CREATE TABLE IF NOT EXISTS product_similarity_matrix (
    id BIGSERIAL PRIMARY KEY,
    product_id_a BIGINT NOT NULL,
    product_id_b BIGINT NOT NULL,
    similarity_score DECIMAL(10,8) NOT NULL,  -- 相似度分数（0-1）
    similarity_type VARCHAR(20) DEFAULT 'cosine',  -- 'cosine', 'euclidean', 'jaccard'
    computed_at TIMESTAMP NOT NULL,
    user_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER DEFAULT 0,
    CONSTRAINT unique_product_pair UNIQUE (product_id_a, product_id_b, user_id, deleted)
);

CREATE INDEX idx_psm_product_a ON product_similarity_matrix(product_id_a, similarity_score DESC, deleted);
CREATE INDEX idx_psm_product_b ON product_similarity_matrix(product_id_b, similarity_score DESC, deleted);
CREATE INDEX idx_psm_user ON product_similarity_matrix(user_id, deleted);

COMMENT ON TABLE product_similarity_matrix IS '商品相似度矩阵表，存储商品间的相似度';
COMMENT ON COLUMN product_similarity_matrix.similarity_score IS '相似度分数，范围0-1，越接近1越相似';
COMMENT ON COLUMN product_similarity_matrix.similarity_type IS '相似度计算方法：cosine(余弦相似度), euclidean(欧氏距离), jaccard(杰卡德系数)';

-- 5. 模型训练日志表
CREATE TABLE IF NOT EXISTS model_training_log (
    id BIGSERIAL PRIMARY KEY,
    model_id BIGINT,  -- 关联 style_recommendation_model.id
    training_status VARCHAR(20) NOT NULL,  -- 'started', 'in_progress', 'completed', 'failed'
    training_type VARCHAR(20),  -- 'full', 'incremental'
    sample_count INTEGER,
    feature_count INTEGER,
    training_duration_ms BIGINT,  -- 训练耗时（毫秒）
    error_message TEXT,
    metrics TEXT,  -- JSON对象，详细的训练指标
    user_id BIGINT NOT NULL,
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER DEFAULT 0
);

CREATE INDEX idx_mtl_model ON model_training_log(model_id, deleted);
CREATE INDEX idx_mtl_user_status ON model_training_log(user_id, training_status, deleted);
CREATE INDEX idx_mtl_started ON model_training_log(started_at DESC);

COMMENT ON TABLE model_training_log IS '模型训练日志表，记录每次训练的详细信息';
COMMENT ON COLUMN model_training_log.training_type IS '训练类型：full(全量训练), incremental(增量训练)';
COMMENT ON COLUMN model_training_log.training_duration_ms IS '训练耗时（毫秒）';
COMMENT ON COLUMN model_training_log.metrics IS 'JSON对象，详细的训练指标，如损失函数值、各轮次准确率等';

-- 初始化默认规则引擎模型记录
INSERT INTO style_recommendation_model (
    user_id, model_type, model_version, model_data,
    training_samples, accuracy, trained_at, is_active
) VALUES (
    0,  -- 系统默认模型
    'rule_based',
    '1.0',
    '{"rules": ["category_based", "price_based", "keyword_based"]}',
    0,
    NULL,
    CURRENT_TIMESTAMP,
    TRUE
) ON CONFLICT DO NOTHING;

COMMENT ON TABLE style_recommendation_model IS '风格推荐模型表 - 支持规则引擎、协同过滤、混合推荐';
