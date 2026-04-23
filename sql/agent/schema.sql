-- ============================================================
-- agent 模块 - 数据库表结构
-- 模块：智能体管理（Agent Management）
-- 数据库：PostgreSQL
-- 版本：1.0
-- 更新日期：2026-02-25
-- 说明：智能体配置、对话记录、对话消息表
-- ============================================================

-- ============================================================
-- 1. agent - 智能体表
-- Entity: cn.gaifan.douyinOperations.module.agent.entity.Agent
-- ============================================================
CREATE TABLE IF NOT EXISTS agent (
    id              BIGSERIAL       PRIMARY KEY,
    user_id         BIGINT          NOT NULL,                           -- 所属用户ID
    agent_name      VARCHAR(128)    NOT NULL,                           -- 智能体名称
    description     VARCHAR(512),                                       -- 描述信息
    agent_type      INTEGER         NOT NULL,                           -- 智能体类型：1=内容生成 2=话术优化 3=客户服务
    system_prompt   TEXT,                                               -- 系统提示词（Prompt）
    model_config    TEXT,                                               -- 模型配置（JSON格式，如{"model":"gpt-4","temperature":0.7}）
    response_mode   INTEGER         NOT NULL DEFAULT 1,                 -- 响应模式：1=即时 2=异步
    status          INTEGER         NOT NULL DEFAULT 1,                 -- 状态：1=启用 0=禁用
    version         INTEGER         NOT NULL DEFAULT 0,                 -- 版本号（并发控制）
    deleted         INTEGER         NOT NULL DEFAULT 0,                 -- 逻辑删除：0=正常 1=已删除
    create_time     TIMESTAMP                DEFAULT CURRENT_TIMESTAMP, -- 创建时间
    update_time     TIMESTAMP                DEFAULT CURRENT_TIMESTAMP  -- 更新时间
);

CREATE INDEX IF NOT EXISTS idx_agent_user_id ON agent (user_id) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_agent_type ON agent (agent_type) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_agent_status ON agent (status) WHERE deleted = 0;

COMMENT ON TABLE  agent                 IS '智能体表';
COMMENT ON COLUMN agent.user_id         IS '所属用户ID';
COMMENT ON COLUMN agent.agent_name      IS '智能体名称';
COMMENT ON COLUMN agent.description     IS '描述信息';
COMMENT ON COLUMN agent.agent_type      IS '智能体类型：1=内容生成 2=话术优化 3=客户服务';
COMMENT ON COLUMN agent.system_prompt   IS '系统提示词（Prompt）';
COMMENT ON COLUMN agent.model_config    IS '模型配置（JSON格式）';
COMMENT ON COLUMN agent.response_mode   IS '响应模式：1=即时 2=异步';
COMMENT ON COLUMN agent.status          IS '状态：1=启用 0=禁用';
COMMENT ON COLUMN agent.version         IS '版本号（并发控制）';
COMMENT ON COLUMN agent.deleted         IS '逻辑删除：0=正常 1=已删除';

-- ============================================================
-- 2. agent_conversation - 对话记录表
-- Entity: cn.gaifan.douyinOperations.module.agent.entity.AgentConversation
-- ============================================================
CREATE TABLE IF NOT EXISTS agent_conversation (
    id                  BIGSERIAL       PRIMARY KEY,
    agent_id            BIGINT          NOT NULL,                           -- 关联智能体ID
    user_id             BIGINT          NOT NULL,                           -- 对话用户ID
    conversation_topic  VARCHAR(256)    NOT NULL,                           -- 对话主题
    status              INTEGER         NOT NULL DEFAULT 1,                 -- 对话状态：1=进行中 2=已完成
    message_count       INTEGER         NOT NULL DEFAULT 0,                 -- 消息数量
    last_message_time   TIMESTAMP,                                          -- 最后消息时间
    deleted             INTEGER         NOT NULL DEFAULT 0,                 -- 逻辑删除：0=正常 1=已删除
    create_time         TIMESTAMP                DEFAULT CURRENT_TIMESTAMP, -- 创建时间
    update_time         TIMESTAMP                DEFAULT CURRENT_TIMESTAMP  -- 更新时间
);

CREATE INDEX IF NOT EXISTS idx_agent_conversation_agent_id ON agent_conversation (agent_id) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_agent_conversation_user_id ON agent_conversation (user_id) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_agent_conversation_status ON agent_conversation (status) WHERE deleted = 0;

COMMENT ON TABLE  agent_conversation               IS '对话记录表';
COMMENT ON COLUMN agent_conversation.agent_id      IS '关联智能体ID';
COMMENT ON COLUMN agent_conversation.user_id       IS '对话用户ID';
COMMENT ON COLUMN agent_conversation.conversation_topic IS '对话主题';
COMMENT ON COLUMN agent_conversation.status        IS '对话状态：1=进行中 2=已完成';
COMMENT ON COLUMN agent_conversation.message_count IS '消息数量';
COMMENT ON COLUMN agent_conversation.last_message_time IS '最后消息时间';
COMMENT ON COLUMN agent_conversation.deleted       IS '逻辑删除：0=正常 1=已删除';

-- ============================================================
-- 3. agent_message - 对话消息表
-- Entity: cn.gaifan.douyinOperations.module.agent.entity.AgentMessage
-- ============================================================
CREATE TABLE IF NOT EXISTS agent_message (
    id              BIGSERIAL       PRIMARY KEY,
    conversation_id BIGINT          NOT NULL,                           -- 关联对话ID
    sender_type     INTEGER         NOT NULL,                           -- 发送者类型：1=用户 2=智能体
    content         TEXT            NOT NULL,                           -- 消息内容
    tokens          INTEGER                  DEFAULT 0,                 -- 消费Token数
    create_time     TIMESTAMP                DEFAULT CURRENT_TIMESTAMP  -- 创建时间
);

CREATE INDEX IF NOT EXISTS idx_agent_message_conversation_id ON agent_message (conversation_id);
CREATE INDEX IF NOT EXISTS idx_agent_message_sender_type ON agent_message (sender_type);

COMMENT ON TABLE  agent_message             IS '对话消息表';
COMMENT ON COLUMN agent_message.conversation_id IS '关联对话ID';
COMMENT ON COLUMN agent_message.sender_type     IS '发送者类型：1=用户 2=智能体';
COMMENT ON COLUMN agent_message.content         IS '消息内容';
COMMENT ON COLUMN agent_message.tokens          IS '消费Token数';
