-- V153: 智能体评分系统 — 表 + 聚合字段
-- 1. agent 表添加 rating_count / rating_sum 聚合字段（AgentReview 评分系统）
-- 2. agent_review 评分记录表

-- ============================================================
-- 1. agent 表 — 评分聚合字段
-- ============================================================
ALTER TABLE agent ADD COLUMN IF NOT EXISTS rating_count INTEGER NOT NULL DEFAULT 0;
ALTER TABLE agent ADD COLUMN IF NOT EXISTS rating_sum INTEGER NOT NULL DEFAULT 0;

-- ============================================================
-- 2. agent_review — 评分记录表
-- ============================================================
CREATE TABLE IF NOT EXISTS agent_review (
    id              BIGSERIAL       PRIMARY KEY,
    agent_id        BIGINT          NOT NULL,
    user_id         BIGINT          NOT NULL,
    rating          INTEGER         NOT NULL DEFAULT 0,
    content         VARCHAR(1000),
    reply_content   VARCHAR(1000),
    reply_time      TIMESTAMP,
    status          INTEGER         NOT NULL DEFAULT 1,
    deleted         INTEGER         NOT NULL DEFAULT 0,
    create_time     TIMESTAMP,
    update_time     TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_agent_review_agent_id ON agent_review(agent_id) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_agent_review_user_id ON agent_review(user_id) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_agent_review_status ON agent_review(status) WHERE deleted = 0;
