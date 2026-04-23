-- ============================================================
-- W-05：AI 话术自动优化建议系统 - 数据库迁移脚本
-- 数据库：PostgreSQL
-- 说明：存储话术分析结果、优化建议、重新生成的版本
-- ============================================================

-- ============================================================
-- 1. dy_script_analysis_result — 话术分析结果表
-- Entity: cn.gaifan.douyinOperations.module.product.entity.ScriptAnalysisResult
-- 存储对特定话术版本的分析结果，包括效果评分、弱点识别、风格识别等
-- ============================================================
CREATE TABLE IF NOT EXISTS dy_script_analysis_result (
    id                      BIGSERIAL           PRIMARY KEY,
    script_version_id       BIGINT              NOT NULL,                          -- 关联的话术版本 ID
    owner_id                BIGINT              NOT NULL,                          -- 数据隔离：所有者 ID
    overall_score           DECIMAL(5, 2)       NOT NULL,                          -- 综合评分（0-100）
    interaction_rate        DECIMAL(5, 2)       NOT NULL,                          -- 互动率（%）
    conversion_rate         DECIMAL(5, 2)       NOT NULL,                          -- 转化率（%）
    fan_growth              INTEGER             DEFAULT 0,                         -- 粉丝增长数
    comment_sentiment       DECIMAL(3, 2)       DEFAULT 0.5,                       -- 评论正面率（0-1）
    dominant_style          VARCHAR(64),                                           -- 主导风格（FRIENDLY/HUMOROUS/PREMIUM/INSPIRATIONAL）
    weak_points             JSONB,                                                 -- 弱点识别 JSON 数组
    analysis_type           VARCHAR(64)         NOT NULL,                          -- 分析类型（COMPREHENSIVE/INTERACTION/CONVERSION/COMMENT）
    data_source             VARCHAR(64)         NOT NULL,                          -- 数据来源（LIVE_MONITOR/HISTORICAL）
    created_at              TIMESTAMP           NOT NULL DEFAULT CURRENT_TIMESTAMP,-- 创建时间
    updated_at              TIMESTAMP           NOT NULL DEFAULT CURRENT_TIMESTAMP,-- 更新时间
    deleted_at              TIMESTAMP,                                             -- 删除时间
    deleted                 INTEGER             NOT NULL DEFAULT 0                 -- 逻辑删除标记（0=正常 1=已删除）
);

CREATE INDEX IF NOT EXISTS idx_script_analysis_result_script_version_id
    ON dy_script_analysis_result (script_version_id);
CREATE INDEX IF NOT EXISTS idx_script_analysis_result_owner_id
    ON dy_script_analysis_result (owner_id);
CREATE INDEX IF NOT EXISTS idx_script_analysis_result_overall_score_desc
    ON dy_script_analysis_result (overall_score DESC);
CREATE INDEX IF NOT EXISTS idx_script_analysis_result_created_at_desc
    ON dy_script_analysis_result (created_at DESC);
CREATE INDEX IF NOT EXISTS idx_script_analysis_result_analysis_type
    ON dy_script_analysis_result (analysis_type);
CREATE INDEX IF NOT EXISTS idx_script_analysis_result_deleted
    ON dy_script_analysis_result (deleted);

COMMENT ON TABLE  dy_script_analysis_result                  IS '话术分析结果表';
COMMENT ON COLUMN dy_script_analysis_result.script_version_id IS '关联的话术版本 ID';
COMMENT ON COLUMN dy_script_analysis_result.owner_id         IS '数据隔离：所有者 ID';
COMMENT ON COLUMN dy_script_analysis_result.overall_score    IS '综合评分（0-100）';
COMMENT ON COLUMN dy_script_analysis_result.interaction_rate IS '互动率（%）';
COMMENT ON COLUMN dy_script_analysis_result.conversion_rate  IS '转化率（%）';
COMMENT ON COLUMN dy_script_analysis_result.fan_growth       IS '粉丝增长数';
COMMENT ON COLUMN dy_script_analysis_result.comment_sentiment IS '评论正面率（0-1）';
COMMENT ON COLUMN dy_script_analysis_result.dominant_style   IS '主导风格（FRIENDLY/HUMOROUS/PREMIUM/INSPIRATIONAL）';
COMMENT ON COLUMN dy_script_analysis_result.weak_points      IS '弱点识别 JSON 数组，包含 type/timeRange/severity/description';
COMMENT ON COLUMN dy_script_analysis_result.analysis_type    IS '分析类型（COMPREHENSIVE/INTERACTION/CONVERSION/COMMENT）';
COMMENT ON COLUMN dy_script_analysis_result.data_source      IS '数据来源（LIVE_MONITOR/HISTORICAL）';
COMMENT ON COLUMN dy_script_analysis_result.created_at       IS '创建时间';
COMMENT ON COLUMN dy_script_analysis_result.updated_at       IS '更新时间';
COMMENT ON COLUMN dy_script_analysis_result.deleted_at       IS '删除时间';
COMMENT ON COLUMN dy_script_analysis_result.deleted          IS '逻辑删除标记';

-- ============================================================
-- 2. dy_optimization_suggestion — 优化建议表
-- Entity: cn.gaifan.douyinOperations.module.product.entity.ScriptOptimizationSuggestion
-- 存储基于分析结果的优化建议，包括建议类别、优先级、期望改进等
-- ============================================================
CREATE TABLE IF NOT EXISTS dy_optimization_suggestion (
    id                      BIGSERIAL           PRIMARY KEY,
    script_version_id       BIGINT              NOT NULL,                          -- 关联的话术版本 ID
    analysis_result_id      BIGINT              NOT NULL,                          -- 关联的分析结果 ID
    owner_id                BIGINT              NOT NULL,                          -- 数据隔离：所有者 ID
    category                VARCHAR(64)         NOT NULL,                          -- 建议分类（CONTENT/PACING/STYLE/TOPIC）
    priority                VARCHAR(20)         NOT NULL,                          -- 优先级（LOW/MEDIUM/HIGH/CRITICAL）
    suggestion_content      TEXT                NOT NULL,                          -- 建议内容描述
    related_weak_point      VARCHAR(256),                                          -- 关联的弱点类型
    expected_improvement    JSONB,                                                 -- 期望改进 JSON（包含各指标提升预期）
    adoption_status         VARCHAR(20)         DEFAULT 'PENDING',                 -- 采纳状态（PENDING/ACCEPTED/REJECTED/APPLIED）
    adopted_at              TIMESTAMP,                                             -- 采纳时间
    adoption_notes          TEXT,                                                  -- 采纳备注
    created_at              TIMESTAMP           NOT NULL DEFAULT CURRENT_TIMESTAMP,-- 创建时间
    updated_at              TIMESTAMP           NOT NULL DEFAULT CURRENT_TIMESTAMP,-- 更新时间
    deleted_at              TIMESTAMP,                                             -- 删除时间
    deleted                 INTEGER             NOT NULL DEFAULT 0                 -- 逻辑删除标记（0=正常 1=已删除）
);

CREATE INDEX IF NOT EXISTS idx_optimization_suggestion_script_version_id
    ON dy_optimization_suggestion (script_version_id);
CREATE INDEX IF NOT EXISTS idx_optimization_suggestion_analysis_result_id
    ON dy_optimization_suggestion (analysis_result_id);
CREATE INDEX IF NOT EXISTS idx_optimization_suggestion_owner_id
    ON dy_optimization_suggestion (owner_id);
CREATE INDEX IF NOT EXISTS idx_optimization_suggestion_category
    ON dy_optimization_suggestion (category);
CREATE INDEX IF NOT EXISTS idx_optimization_suggestion_priority
    ON dy_optimization_suggestion (priority);
CREATE INDEX IF NOT EXISTS idx_optimization_suggestion_adoption_status
    ON dy_optimization_suggestion (adoption_status);
CREATE INDEX IF NOT EXISTS idx_optimization_suggestion_created_at_desc
    ON dy_optimization_suggestion (created_at DESC);
CREATE INDEX IF NOT EXISTS idx_optimization_suggestion_deleted
    ON dy_optimization_suggestion (deleted);

COMMENT ON TABLE  dy_optimization_suggestion                IS '优化建议表';
COMMENT ON COLUMN dy_optimization_suggestion.script_version_id IS '关联的话术版本 ID';
COMMENT ON COLUMN dy_optimization_suggestion.analysis_result_id IS '关联的分析结果 ID';
COMMENT ON COLUMN dy_optimization_suggestion.owner_id         IS '数据隔离：所有者 ID';
COMMENT ON COLUMN dy_optimization_suggestion.category        IS '建议分类（CONTENT/PACING/STYLE/TOPIC）';
COMMENT ON COLUMN dy_optimization_suggestion.priority        IS '优先级（LOW/MEDIUM/HIGH/CRITICAL）';
COMMENT ON COLUMN dy_optimization_suggestion.suggestion_content IS '建议内容描述';
COMMENT ON COLUMN dy_optimization_suggestion.related_weak_point IS '关联的弱点类型';
COMMENT ON COLUMN dy_optimization_suggestion.expected_improvement IS '期望改进 JSON';
COMMENT ON COLUMN dy_optimization_suggestion.adoption_status IS '采纳状态（PENDING/ACCEPTED/REJECTED/APPLIED）';
COMMENT ON COLUMN dy_optimization_suggestion.adopted_at      IS '采纳时间';
COMMENT ON COLUMN dy_optimization_suggestion.adoption_notes  IS '采纳备注';
COMMENT ON COLUMN dy_optimization_suggestion.created_at      IS '创建时间';
COMMENT ON COLUMN dy_optimization_suggestion.updated_at      IS '更新时间';
COMMENT ON COLUMN dy_optimization_suggestion.deleted_at      IS '删除时间';
COMMENT ON COLUMN dy_optimization_suggestion.deleted         IS '逻辑删除标记';

-- ============================================================
-- 3. dy_script_regenerated_version — 重新生成的话术版本表
-- Entity: cn.gaifan.douyinOperations.module.product.entity.ScriptRegeneratedVersion
-- 存储基于优化建议重新生成的话术版本，支持多种风格
-- ============================================================
CREATE TABLE IF NOT EXISTS dy_script_regenerated_version (
    id                      BIGSERIAL           PRIMARY KEY,
    script_version_id       BIGINT              NOT NULL,                          -- 关联的原话术版本 ID
    suggestion_id           BIGINT              NOT NULL,                          -- 关联的优化建议 ID
    owner_id                BIGINT              NOT NULL,                          -- 数据隔离：所有者 ID
    generation_style        VARCHAR(64)         NOT NULL,                          -- 生成的话术风格（FRIENDLY/HUMOROUS/PREMIUM/INSPIRATIONAL）
    regenerated_content     TEXT                NOT NULL,                          -- 重新生成的话术内容
    ai_quality_score        DECIMAL(5, 2)       DEFAULT 0,                         -- AI 生成质量评分（0-10）
    estimated_metrics       JSONB,                                                 -- 估计指标（互动率/转化率等预估）
    generation_prompt       TEXT,                                                  -- 生成时使用的提示词
    is_applied              BOOLEAN             DEFAULT FALSE,                     -- 是否已应用到话术版本
    applied_at              TIMESTAMP,                                             -- 应用时间
    approval_status         VARCHAR(20)         DEFAULT 'PENDING',                 -- 审批状态（PENDING/APPROVED/REJECTED）
    approved_by             BIGINT,                                                -- 审批人 ID
    approved_at             TIMESTAMP,                                             -- 审批时间
    approval_notes          TEXT,                                                  -- 审批备注
    created_at              TIMESTAMP           NOT NULL DEFAULT CURRENT_TIMESTAMP,-- 创建时间
    updated_at              TIMESTAMP           NOT NULL DEFAULT CURRENT_TIMESTAMP,-- 更新时间
    deleted_at              TIMESTAMP,                                             -- 删除时间
    deleted                 INTEGER             NOT NULL DEFAULT 0                 -- 逻辑删除标记（0=正常 1=已删除）
);

CREATE INDEX IF NOT EXISTS idx_script_regenerated_version_script_version_id
    ON dy_script_regenerated_version (script_version_id);
CREATE INDEX IF NOT EXISTS idx_script_regenerated_version_suggestion_id
    ON dy_script_regenerated_version (suggestion_id);
CREATE INDEX IF NOT EXISTS idx_script_regenerated_version_owner_id
    ON dy_script_regenerated_version (owner_id);
CREATE INDEX IF NOT EXISTS idx_script_regenerated_version_generation_style
    ON dy_script_regenerated_version (generation_style);
CREATE INDEX IF NOT EXISTS idx_script_regenerated_version_is_applied
    ON dy_script_regenerated_version (is_applied);
CREATE INDEX IF NOT EXISTS idx_script_regenerated_version_approval_status
    ON dy_script_regenerated_version (approval_status);
CREATE INDEX IF NOT EXISTS idx_script_regenerated_version_ai_quality_score_desc
    ON dy_script_regenerated_version (ai_quality_score DESC);
CREATE INDEX IF NOT EXISTS idx_script_regenerated_version_created_at_desc
    ON dy_script_regenerated_version (created_at DESC);
CREATE INDEX IF NOT EXISTS idx_script_regenerated_version_deleted
    ON dy_script_regenerated_version (deleted);

COMMENT ON TABLE  dy_script_regenerated_version                IS '重新生成的话术版本表';
COMMENT ON COLUMN dy_script_regenerated_version.script_version_id IS '关联的原话术版本 ID';
COMMENT ON COLUMN dy_script_regenerated_version.suggestion_id IS '关联的优化建议 ID';
COMMENT ON COLUMN dy_script_regenerated_version.owner_id       IS '数据隔离：所有者 ID';
COMMENT ON COLUMN dy_script_regenerated_version.generation_style IS '生成的话术风格';
COMMENT ON COLUMN dy_script_regenerated_version.regenerated_content IS '重新生成的话术内容';
COMMENT ON COLUMN dy_script_regenerated_version.ai_quality_score IS 'AI 生成质量评分（0-10）';
COMMENT ON COLUMN dy_script_regenerated_version.estimated_metrics IS '估计指标（互动率/转化率等预估）';
COMMENT ON COLUMN dy_script_regenerated_version.generation_prompt IS '生成时使用的提示词';
COMMENT ON COLUMN dy_script_regenerated_version.is_applied     IS '是否已应用到话术版本';
COMMENT ON COLUMN dy_script_regenerated_version.applied_at     IS '应用时间';
COMMENT ON COLUMN dy_script_regenerated_version.approval_status IS '审批状态（PENDING/APPROVED/REJECTED）';
COMMENT ON COLUMN dy_script_regenerated_version.approved_by    IS '审批人 ID';
COMMENT ON COLUMN dy_script_regenerated_version.approved_at    IS '审批时间';
COMMENT ON COLUMN dy_script_regenerated_version.approval_notes IS '审批备注';
COMMENT ON COLUMN dy_script_regenerated_version.created_at     IS '创建时间';
COMMENT ON COLUMN dy_script_regenerated_version.updated_at     IS '更新时间';
COMMENT ON COLUMN dy_script_regenerated_version.deleted_at     IS '删除时间';
COMMENT ON COLUMN dy_script_regenerated_version.deleted        IS '逻辑删除标记';
