-- ============================================================
-- agent_review - 智能体评分与评论表
-- 用于智能体市场的用户评分和评论功能
-- ============================================================
CREATE TABLE IF NOT EXISTS agent_review (
    id              BIGSERIAL       PRIMARY KEY,
    agent_id        BIGINT          NOT NULL,                           -- 智能体ID
    user_id         BIGINT          NOT NULL,                           -- 评论用户ID
    rating          INTEGER         NOT NULL,                           -- 评分：1-5星
    content         VARCHAR(1000),                                       -- 评论内容（可选）
    reply_content   VARCHAR(1000),                                       -- 智能体作者回复
    reply_time      TIMESTAMP,                                           -- 回复时间
    status          INTEGER         NOT NULL DEFAULT 1,                 -- 状态：1=可见 0=隐藏
    deleted         INTEGER         NOT NULL DEFAULT 0,                 -- 逻辑删除：0=正常 1=已删除
    create_time     TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,           -- 创建时间
    update_time     TIMESTAMP       DEFAULT CURRENT_TIMESTAMP           -- 更新时间
);

CREATE INDEX IF NOT EXISTS idx_agent_review_agent_id ON agent_review (agent_id) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_agent_review_user_id ON agent_review (user_id) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_agent_review_rating ON agent_review (rating) WHERE deleted = 0;

COMMENT ON TABLE  agent_review              IS '智能体评分与评论表';
COMMENT ON COLUMN agent_review.agent_id      IS '智能体ID';
COMMENT ON COLUMN agent_review.user_id       IS '评论用户ID';
COMMENT ON COLUMN agent_review.rating        IS '评分：1-5星';
COMMENT ON COLUMN agent_review.content       IS '评论内容';
COMMENT ON COLUMN agent_review.reply_content IS '智能体作者回复';
COMMENT ON COLUMN agent_review.reply_time    IS '回复时间';
COMMENT ON COLUMN agent_review.status        IS '状态：1=可见 0=隐藏';
COMMENT ON COLUMN agent_review.deleted       IS '逻辑删除';

-- ============================================================
-- agent 表新增字段：评分统计
-- ============================================================
ALTER TABLE agent ADD COLUMN IF NOT EXISTS rating_count INTEGER NOT NULL DEFAULT 0;
ALTER TABLE agent ADD COLUMN IF NOT EXISTS rating_sum INTEGER NOT NULL DEFAULT 0;

-- 索引
CREATE INDEX IF NOT EXISTS idx_agent_rating ON agent (rating_sum, rating_count) WHERE deleted = 0;
