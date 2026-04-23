-- ============================================================
-- 企业微信模块 (wecom) - 表结构
-- 数据库：PostgreSQL
-- 说明：机器人配置、推送规则、消息日志
-- ============================================================

-- ============================================================
-- 1. wc_robot_config — 机器人配置表
-- Entity: cn.gaifan.douyinOperations.module.wecom.entity.WcRobotConfig
-- ============================================================
CREATE TABLE IF NOT EXISTS wc_robot_config (
    id           BIGSERIAL     PRIMARY KEY,
    owner_id     BIGINT        NOT NULL,                                   -- 所属用户 ID
    robot_name   VARCHAR(128)  NOT NULL,                                   -- 机器人名称
    webhook_url  VARCHAR(512)  NOT NULL,                                   -- 企业微信 Webhook 地址
    robot_type   VARCHAR(32)   NOT NULL DEFAULT 'custom',                  -- 机器人类型：data_report/alert/task_reminder/custom
    status       SMALLINT      NOT NULL DEFAULT 1,                         -- 状态：0=禁用 1=启用
    description  VARCHAR(256),                                              -- 备注描述
    deleted      SMALLINT      NOT NULL DEFAULT 0,                         -- 逻辑删除：0=正常 1=已删除
    create_time  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,         -- 创建时间
    update_time  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,         -- 更新时间

    CONSTRAINT chk_robot_type CHECK (robot_type IN ('data_report', 'alert', 'task_reminder', 'custom')),
    CONSTRAINT chk_robot_status CHECK (status IN (0, 1)),
    CONSTRAINT chk_robot_deleted CHECK (deleted IN (0, 1))
);

CREATE INDEX IF NOT EXISTS idx_wc_robot_owner ON wc_robot_config (owner_id, deleted);
CREATE INDEX IF NOT EXISTS idx_wc_robot_type  ON wc_robot_config (owner_id, robot_type, status);

COMMENT ON TABLE  wc_robot_config             IS '企业微信机器人配置表';
COMMENT ON COLUMN wc_robot_config.owner_id    IS '所属用户 ID';
COMMENT ON COLUMN wc_robot_config.robot_name  IS '机器人名称';
COMMENT ON COLUMN wc_robot_config.webhook_url IS '企业微信 Webhook 地址';
COMMENT ON COLUMN wc_robot_config.robot_type  IS '机器人类型：data_report=数据报告, alert=告警, task_reminder=任务提醒, custom=自定义';
COMMENT ON COLUMN wc_robot_config.status      IS '状态：0=禁用, 1=启用';

-- ============================================================
-- 2. wc_push_rule — 推送规则表
-- Entity: cn.gaifan.douyinOperations.module.wecom.entity.WcPushRule
-- ============================================================
CREATE TABLE IF NOT EXISTS wc_push_rule (
    id               BIGSERIAL    PRIMARY KEY,
    owner_id         BIGINT       NOT NULL,                                -- 所属用户 ID
    robot_id         BIGINT       NOT NULL,                                -- 关联机器人 ID（wc_robot_config.id）
    rule_name        VARCHAR(128) NOT NULL,                                -- 规则名称
    trigger_type     VARCHAR(16)  NOT NULL,                                -- 触发类型：scheduled/event
    trigger_config   TEXT         NOT NULL,                                -- 触发配置（JSON）
    message_template TEXT         NOT NULL,                                -- 消息模板（支持 {{变量}} 占位符）
    status           SMALLINT     NOT NULL DEFAULT 1,                      -- 状态：0=禁用 1=启用
    last_trigger_time TIMESTAMP,                                            -- 最后触发时间
    deleted          SMALLINT     NOT NULL DEFAULT 0,                      -- 逻辑删除：0=正常 1=已删除
    create_time      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,      -- 创建时间
    update_time      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,      -- 更新时间

    CONSTRAINT chk_rule_trigger_type CHECK (trigger_type IN ('scheduled', 'event')),
    CONSTRAINT chk_rule_status CHECK (status IN (0, 1)),
    CONSTRAINT chk_rule_deleted CHECK (deleted IN (0, 1)),
    CONSTRAINT fk_rule_robot FOREIGN KEY (robot_id) REFERENCES wc_robot_config (id)
);

CREATE INDEX IF NOT EXISTS idx_wc_rule_owner   ON wc_push_rule (owner_id, deleted);
CREATE INDEX IF NOT EXISTS idx_wc_rule_trigger ON wc_push_rule (trigger_type, status, deleted);
CREATE INDEX IF NOT EXISTS idx_wc_rule_robot   ON wc_push_rule (robot_id);

COMMENT ON TABLE  wc_push_rule                    IS '企业微信推送规则表';
COMMENT ON COLUMN wc_push_rule.trigger_type       IS '触发类型：scheduled=定时, event=事件驱动';
COMMENT ON COLUMN wc_push_rule.trigger_config     IS '触发配置 JSON：scheduled 时为 {"cron":"0 0 8 * * ?"}, event 时为 {"eventType":"VIRAL_VIDEO_DETECTED"}';
COMMENT ON COLUMN wc_push_rule.message_template   IS '消息模板，支持 {{变量}} 占位符';
COMMENT ON COLUMN wc_push_rule.last_trigger_time  IS '最后一次触发时间';

-- ============================================================
-- 3. wc_message_log — 消息日志表
-- Entity: cn.gaifan.douyinOperations.module.wecom.entity.WcMessageLog
-- 注意：无 deleted 字段，消息日志不支持逻辑删除，超期由定时任务物理清理
-- ============================================================
CREATE TABLE IF NOT EXISTS wc_message_log (
    id              BIGSERIAL    PRIMARY KEY,
    owner_id        BIGINT       NOT NULL,                                 -- 所属用户 ID
    robot_id        BIGINT       NOT NULL,                                 -- 关联机器人 ID
    rule_id         BIGINT,                                                -- 关联规则 ID（手动推送时为 NULL）
    message_type    VARCHAR(16)  NOT NULL DEFAULT 'text',                  -- 消息类型：text/markdown/news
    message_content TEXT         NOT NULL,                                  -- 消息完整内容（JSON）
    status          SMALLINT     NOT NULL DEFAULT 0,                       -- 发送状态：0=失败 1=成功
    error_message   VARCHAR(512),                                           -- 失败原因（status=0 时填写）
    send_time       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,       -- 发送时间
    create_time     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,       -- 创建时间

    CONSTRAINT chk_msg_type CHECK (message_type IN ('text', 'markdown', 'news')),
    CONSTRAINT chk_msg_status CHECK (status IN (0, 1)),
    CONSTRAINT fk_msg_robot FOREIGN KEY (robot_id) REFERENCES wc_robot_config (id),
    CONSTRAINT fk_msg_rule FOREIGN KEY (rule_id) REFERENCES wc_push_rule (id)
);

CREATE INDEX IF NOT EXISTS idx_wc_log_owner_time   ON wc_message_log (owner_id, send_time DESC);
CREATE INDEX IF NOT EXISTS idx_wc_log_owner_status ON wc_message_log (owner_id, status, send_time DESC);
CREATE INDEX IF NOT EXISTS idx_wc_log_rule         ON wc_message_log (rule_id, send_time DESC);
CREATE INDEX IF NOT EXISTS idx_wc_log_robot        ON wc_message_log (robot_id, send_time DESC);

COMMENT ON TABLE  wc_message_log                IS '企业微信消息发送日志表';
COMMENT ON COLUMN wc_message_log.rule_id        IS '关联规则 ID，手动推送时为 NULL';
COMMENT ON COLUMN wc_message_log.message_type   IS '消息类型：text=文本, markdown=Markdown, news=图文卡片';
COMMENT ON COLUMN wc_message_log.status         IS '发送状态：0=失败, 1=成功';
COMMENT ON COLUMN wc_message_log.error_message  IS '失败原因描述';
