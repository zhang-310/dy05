-- ============================================================
-- 短信模块 (sms) - 表结构
-- 数据库：PostgreSQL
-- 说明：短信服务商配置、模板、发送日志、验证码
-- ============================================================

-- ============================================================
-- 1. sms_provider_config — 短信服务商配置表
-- Entity: cn.gaifan.douyinOperations.module.sms.entity.SmsProviderConfig
-- ============================================================
CREATE TABLE IF NOT EXISTS sms_provider_config (
    id              BIGSERIAL     PRIMARY KEY,
    owner_id        BIGINT        NOT NULL,                                  -- 所属用户 ID（租户隔离）
    provider_code   VARCHAR(32)   NOT NULL,                                  -- 服务商代码：tencent/aliyun/customize
    provider_name   VARCHAR(128)  NOT NULL,                                  -- 服务商名称
    api_key         VARCHAR(256)  NOT NULL,                                  -- API Key / Access Key
    api_secret      VARCHAR(256)  NOT NULL,                                  -- API Secret / Secret Key
    app_id          VARCHAR(128),                                             -- App ID（某些服务商需要）
    sign_name       VARCHAR(128)  NOT NULL DEFAULT 'DefaultSign',            -- 短信签名
    region          VARCHAR(32)   DEFAULT 'cn',                              -- 地区：cn/us/eu 等
    status          SMALLINT      NOT NULL DEFAULT 1,                        -- 状态：0=禁用 1=启用
    is_default      SMALLINT      NOT NULL DEFAULT 0,                        -- 是否为默认配置：0=否 1=是（同一用户只能有一个）
    daily_quota     INTEGER       DEFAULT 1000,                              -- 每日发送配额
    daily_sent_count INTEGER      DEFAULT 0,                                 -- 今日已发送数
    last_reset_time TIMESTAMP,                                                -- 最后一次重置时间
    deleted         SMALLINT      NOT NULL DEFAULT 0,                        -- 逻辑删除：0=正常 1=已删除
    create_time     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,        -- 创建时间
    update_time     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,        -- 更新时间

    CONSTRAINT chk_provider_status CHECK (status IN (0, 1)),
    CONSTRAINT chk_provider_default CHECK (is_default IN (0, 1)),
    CONSTRAINT chk_provider_deleted CHECK (deleted IN (0, 1))
);

CREATE INDEX IF NOT EXISTS idx_sms_provider_owner  ON sms_provider_config (owner_id, deleted);
CREATE INDEX IF NOT EXISTS idx_sms_provider_default ON sms_provider_config (owner_id, is_default) WHERE deleted = 0;

COMMENT ON TABLE  sms_provider_config               IS '短信服务商配置表';
COMMENT ON COLUMN sms_provider_config.provider_code IS '服务商代码：tencent=腾讯云, aliyun=阿里云, customize=自定义';
COMMENT ON COLUMN sms_provider_config.sign_name     IS '短信签名（发送时会加在短信内容前）';
COMMENT ON COLUMN sms_provider_config.daily_quota   IS '每日发送配额限制';
COMMENT ON COLUMN sms_provider_config.is_default    IS '是否为该用户的默认配置';

-- ============================================================
-- 2. sms_template — 短信模板表
-- Entity: cn.gaifan.douyinOperations.module.sms.entity.SmsTemplate
-- ============================================================
CREATE TABLE IF NOT EXISTS sms_template (
    id              BIGSERIAL     PRIMARY KEY,
    owner_id        BIGINT        NOT NULL,                                  -- 所属用户 ID
    template_code   VARCHAR(64)   NOT NULL,                                  -- 模板代码（唯一标识）
    template_name   VARCHAR(128)  NOT NULL,                                  -- 模板名称
    content         TEXT          NOT NULL,                                  -- 模板内容（支持 {{variable}} 占位符）
    provider_code   VARCHAR(32)   NOT NULL,                                  -- 关联的服务商代码
    provider_template_id VARCHAR(128),                                        -- 服务商的模板 ID
    status          SMALLINT      NOT NULL DEFAULT 1,                        -- 状态：0=禁用 1=启用 2=审核中
    template_type   VARCHAR(32)   NOT NULL DEFAULT 'notification',           -- 模板类型：verification=验证码, notification=通知, marketing=营销
    remark          VARCHAR(256),                                             -- 备注
    deleted         SMALLINT      NOT NULL DEFAULT 0,                        -- 逻辑删除：0=正常 1=已删除
    create_time     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,        -- 创建时间
    update_time     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,        -- 更新时间

    CONSTRAINT chk_template_status CHECK (status IN (0, 1, 2)),
    CONSTRAINT chk_template_type CHECK (template_type IN ('verification', 'notification', 'marketing')),
    CONSTRAINT chk_template_deleted CHECK (deleted IN (0, 1))
);

CREATE INDEX IF NOT EXISTS idx_sms_template_owner   ON sms_template (owner_id, deleted);
CREATE INDEX IF NOT EXISTS idx_sms_template_code    ON sms_template (owner_id, template_code) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_sms_template_status  ON sms_template (owner_id, status, deleted);

COMMENT ON TABLE  sms_template                    IS '短信模板表';
COMMENT ON COLUMN sms_template.template_code      IS '模板代码，应用层的唯一标识，如：USER_VERIFY_CODE';
COMMENT ON COLUMN sms_template.template_type      IS '模板类型：verification=验证码, notification=通知, marketing=营销消息';
COMMENT ON COLUMN sms_template.provider_template_id IS '服务商侧的模板 ID（如腾讯云的 Template ID）';

-- ============================================================
-- 3. sms_send_log — 短信发送日志表
-- Entity: cn.gaifan.douyinOperations.module.sms.entity.SmsSendLog
-- 注意：无 deleted 字段，日志不支持逻辑删除，超期由定时任务物理清理
-- ============================================================
CREATE TABLE IF NOT EXISTS sms_send_log (
    id              BIGSERIAL     PRIMARY KEY,
    owner_id        BIGINT        NOT NULL,                                  -- 所属用户 ID
    phone_number    VARCHAR(20)   NOT NULL,                                  -- 目标手机号
    template_code   VARCHAR(64)   NOT NULL,                                  -- 使用的模板代码
    provider_code   VARCHAR(32)   NOT NULL,                                  -- 服务商代码
    provider_request_id VARCHAR(128),                                         -- 服务商返回的请求 ID
    content         TEXT,                                                     -- 实际发送的短信内容
    status          VARCHAR(16)   NOT NULL DEFAULT 'pending',                -- 状态：pending=待发送, sending=发送中, success=成功, failed=失败
    error_message   VARCHAR(256),                                             -- 错误信息（失败时）
    error_code      VARCHAR(32),                                              -- 错误代码（服务商返回）
    send_time       TIMESTAMP,                                                -- 发送时间
    delivered_time  TIMESTAMP,                                                -- 送达时间（如服务商支持）
    cost            DECIMAL(10, 2),                                           -- 成本（通常按条数计算）
    biz_id          VARCHAR(128),                                             -- 业务 ID（用于关联业务数据，如注册/登录）
    biz_type        VARCHAR(32),                                              -- 业务类型：register/login/password_reset/binding
    create_time     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,        -- 创建时间
    update_time     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP         -- 更新时间
);

CREATE INDEX IF NOT EXISTS idx_sms_log_owner       ON sms_send_log (owner_id, create_time DESC);
CREATE INDEX IF NOT EXISTS idx_sms_log_phone       ON sms_send_log (owner_id, phone_number, create_time DESC);
CREATE INDEX IF NOT EXISTS idx_sms_log_status      ON sms_send_log (status, create_time DESC);
CREATE INDEX IF NOT EXISTS idx_sms_log_biz         ON sms_send_log (owner_id, biz_type, biz_id) WHERE biz_id IS NOT NULL;

COMMENT ON TABLE  sms_send_log                   IS '短信发送日志表';
COMMENT ON COLUMN sms_send_log.provider_request_id IS '服务商返回的唯一请求 ID，用于追踪和查询';
COMMENT ON COLUMN sms_send_log.status             IS '状态：pending=待发送, sending=发送中, success=成功, failed=失败';
COMMENT ON COLUMN sms_send_log.biz_id             IS '关联的业务 ID（如用户注册的用户ID）';
COMMENT ON COLUMN sms_send_log.biz_type           IS '业务类型，用于分类统计';

-- ============================================================
-- 4. sms_verification_code — 短信验证码表
-- Entity: cn.gaifan.douyinOperations.module.sms.entity.SmsVerificationCode
-- 说明：存储 OTP 验证码，短期数据，可定期清理
-- ============================================================
CREATE TABLE IF NOT EXISTS sms_verification_code (
    id              BIGSERIAL     PRIMARY KEY,
    owner_id        BIGINT        NOT NULL,                                  -- 所属用户 ID（租户隔离）
    phone_number    VARCHAR(20)   NOT NULL,                                  -- 目标手机号
    biz_type        VARCHAR(32)   NOT NULL,                                  -- 业务类型：register/login/password_reset/binding
    code            VARCHAR(10)   NOT NULL,                                  -- 验证码（通常是 6 位数字）
    attempt_count   SMALLINT      NOT NULL DEFAULT 0,                        -- 尝试次数（防止暴力破解）
    max_attempts    SMALLINT      NOT NULL DEFAULT 5,                        -- 最大尝试次数
    is_verified     SMALLINT      NOT NULL DEFAULT 0,                        -- 是否已验证：0=否 1=是
    verified_time   TIMESTAMP,                                                -- 验证成功时间
    expires_at      TIMESTAMP     NOT NULL,                                  -- 过期时间（通常是 5-10 分钟）
    created_ip      VARCHAR(45),                                              -- 创建时的客户端 IP
    verified_ip     VARCHAR(45),                                              -- 验证时的客户端 IP
    created_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,        -- 创建时间
    updated_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP         -- 更新时间

    -- 注意：不需要 deleted 字段，依靠 expires_at 做过期清理
);

CREATE INDEX IF NOT EXISTS idx_sms_code_phone      ON sms_verification_code (phone_number, biz_type, expires_at DESC);
CREATE INDEX IF NOT EXISTS idx_sms_code_expires    ON sms_verification_code (expires_at) WHERE is_verified = 0;
CREATE INDEX IF NOT EXISTS idx_sms_code_owner      ON sms_verification_code (owner_id, biz_type);

COMMENT ON TABLE  sms_verification_code              IS '短信验证码表，用于存储临时 OTP 验证码';
COMMENT ON COLUMN sms_verification_code.biz_type    IS '业务类型：register=注册, login=登录, password_reset=重置密码, binding=绑定';
COMMENT ON COLUMN sms_verification_code.attempt_count IS '用户尝试验证的次数，超过上限应拒绝';
COMMENT ON COLUMN sms_verification_code.max_attempts IS '最大允许尝试次数（防止暴力破解）';
COMMENT ON COLUMN sms_verification_code.expires_at   IS '验证码过期时间，通常为 5-10 分钟';
COMMENT ON COLUMN sms_verification_code.created_ip   IS '创建验证码时的客户端 IP（用于安全审计）';
