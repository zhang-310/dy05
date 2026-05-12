-- ============================================================
-- messaging 模块 - 数据库表结构
-- 模块：企微/飞书入站 Webhook、回调配置
-- 数据库：PostgreSQL
-- 说明：接收企微/飞书回调，验签、解析、路由到 Agent，并调用平台 API 回复
-- ============================================================

-- ============================================================
-- 1. msg_platform_config — 企微/飞书接入配置表
-- Entity: cn.gaifan.douyinOperations.module.messaging.entity.MsgPlatformConfig
-- ============================================================
CREATE TABLE IF NOT EXISTS msg_platform_config (
    id               BIGSERIAL     PRIMARY KEY,
    owner_id         BIGINT        NOT NULL,                              -- 所属用户 ID（绑定到平台后用于路由）
    platform         VARCHAR(32)   NOT NULL,                              -- 平台：wecom / feishu
    app_id           VARCHAR(128),                                         -- 应用 ID（飞书 app_id，企微 agentid 等）
    corp_id          VARCHAR(128),                                         -- 企业 ID（企微 corp_id，飞书为空）
    secret           VARCHAR(512),                                         -- 应用密钥 / Secret
    callback_token   VARCHAR(256),                                         -- 回调 URL 校验 Token（企微/飞书）
    callback_encoding_aes_key VARCHAR(256),                                -- 回调消息加解密 Key（企微用）
    agent_id         BIGINT,                                              -- 绑定的智能体 ID（消息路由到此 Agent）
    status           INTEGER       NOT NULL DEFAULT 1,                   -- 0=禁用 1=启用
    extra_config     TEXT,                                                -- 扩展配置 JSON
    deleted          INTEGER       NOT NULL DEFAULT 0,
    create_time      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_msg_platform CHECK (platform IN ('wecom', 'feishu')),
    CONSTRAINT chk_msg_status CHECK (status IN (0, 1))
);

CREATE INDEX IF NOT EXISTS idx_msg_config_owner ON msg_platform_config (owner_id, deleted);
CREATE INDEX IF NOT EXISTS idx_msg_config_platform ON msg_platform_config (platform, status) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_msg_config_webhook ON msg_platform_config (platform, callback_token, deleted);  -- P2-001: Webhook 查询优化

COMMENT ON TABLE  msg_platform_config             IS '企微/飞书接入配置表';
COMMENT ON COLUMN msg_platform_config.platform    IS '平台：wecom=企业微信, feishu=飞书';
COMMENT ON COLUMN msg_platform_config.callback_token IS '回调 URL 校验 Token';
COMMENT ON COLUMN msg_platform_config.callback_encoding_aes_key IS '企微消息加解密 EncodingAESKey';
COMMENT ON COLUMN msg_platform_config.agent_id IS '绑定的智能体 ID，消息路由到此 Agent';

-- ============================================================
-- 2. msg_message_history — 消息历史记录表（P1-5）
-- Entity: cn.gaifan.douyinOperations.module.messaging.entity.MsgMessageHistory
-- ============================================================
CREATE TABLE IF NOT EXISTS msg_message_history (
    id               BIGSERIAL     PRIMARY KEY,
    config_id        BIGINT        NOT NULL,                              -- 关联 msg_platform_config.id
    platform         VARCHAR(32)   NOT NULL,                              -- 平台：wecom / feishu
    message_id       VARCHAR(128),                                         -- 平台消息 ID
    sender_id        VARCHAR(128),                                         -- 发送者 ID（open_id / user_id）
    receiver_id      VARCHAR(128),                                         -- 接收者 ID
    message_type     VARCHAR(32),                                          -- 消息类型：text / image / file 等
    content          TEXT,                                                 -- 消息内容
    raw_payload      TEXT,                                                 -- 原始回调 JSON
    direction        VARCHAR(16)   NOT NULL,                              -- 方向：inbound=入站 / outbound=出站
    agent_id         BIGINT,                                              -- 处理的智能体 ID
    agent_response   TEXT,                                                 -- 智能体回复内容
    status           INTEGER       NOT NULL DEFAULT 0,                   -- 0=待处理 1=已处理 2=失败
    error_message    TEXT,                                                 -- 错误信息
    deleted          INTEGER       NOT NULL DEFAULT 0,
    create_time      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_msg_history_platform CHECK (platform IN ('wecom', 'feishu')),
    CONSTRAINT chk_msg_history_direction CHECK (direction IN ('inbound', 'outbound')),
    CONSTRAINT chk_msg_history_status CHECK (status IN (0, 1, 2))
);

CREATE INDEX IF NOT EXISTS idx_msg_history_config ON msg_message_history (config_id, deleted);
CREATE INDEX IF NOT EXISTS idx_msg_history_sender ON msg_message_history (sender_id, create_time DESC) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_msg_history_agent ON msg_message_history (agent_id, create_time DESC) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_msg_history_status ON msg_message_history (status, create_time) WHERE deleted = 0;

COMMENT ON TABLE  msg_message_history             IS '消息历史记录表';
COMMENT ON COLUMN msg_message_history.direction   IS '方向：inbound=入站, outbound=出站';
COMMENT ON COLUMN msg_message_history.status      IS '状态：0=待处理, 1=已处理, 2=失败';
