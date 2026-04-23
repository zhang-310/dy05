-- ============================================================
-- auth 模块 - 数据库表结构
-- 模块：用户认证与授权（Authentication & Authorization）
-- 数据库：PostgreSQL
-- 版本：3.0
-- 更新日期：2026-02-25
-- 说明：字段名、类型、默认值已与 Entity 层完全对齐
-- 升级：auth_login_log 新增 username/status/fail_reason，user_id 改为可空（支持失败审计）
-- 若从 v2.0 升级，请执行 sql/auth/migration-v2-to-v3.sql
-- ============================================================

-- ============================================================
-- 1. auth_role - 角色表
-- Entity: cn.gaifan.douyinOperations.module.auth.entity.AuthRole
-- ============================================================
CREATE TABLE IF NOT EXISTS auth_role (
    id          BIGSERIAL       PRIMARY KEY,
    role_code   VARCHAR(64)     NOT NULL,                           -- 角色编码（唯一标识）：admin, institution, talent, user
    role_name   VARCHAR(64)     NOT NULL,                           -- 角色名称：平台管理员, 机构管理员, 达人/主播, 普通用户
    status      INTEGER         NOT NULL DEFAULT 1,                 -- 状态：1=正常 0=禁用
    sort_order  INTEGER                  DEFAULT 0,                 -- 排序
    deleted     INTEGER         NOT NULL DEFAULT 0,                 -- 逻辑删除：0=正常 1=已删除
    create_time TIMESTAMP                DEFAULT CURRENT_TIMESTAMP, -- 创建时间
    update_time TIMESTAMP                DEFAULT CURRENT_TIMESTAMP  -- 更新时间
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_auth_role_code ON auth_role (role_code) WHERE deleted = 0;

COMMENT ON TABLE  auth_role              IS '角色表';
COMMENT ON COLUMN auth_role.role_code    IS '角色编码（唯一标识）：admin, institution, talent, user';
COMMENT ON COLUMN auth_role.role_name    IS '角色名称：平台管理员, 机构管理员, 达人/主播, 普通用户';
COMMENT ON COLUMN auth_role.status       IS '状态：1=正常 0=禁用';
COMMENT ON COLUMN auth_role.sort_order   IS '排序';
COMMENT ON COLUMN auth_role.deleted      IS '逻辑删除：0=正常 1=已删除';

-- ============================================================
-- 2. auth_user - 用户表
-- Entity: cn.gaifan.douyinOperations.module.auth.entity.AuthUser
-- 注意: status 语义为 0=正常 1=封禁（与 auth_role.status 相反）
-- ============================================================
CREATE TABLE IF NOT EXISTS auth_user (
    id              BIGSERIAL       PRIMARY KEY,
    username        VARCHAR(64)     NOT NULL,                           -- 用户名（唯一）
    password_hash   VARCHAR(128)    NOT NULL,                           -- 密码哈希（BCrypt）
    mobile          VARCHAR(20),                                        -- 手机号
    email           VARCHAR(128),                                       -- 邮箱
    nickname        VARCHAR(64),                                        -- 昵称
    avatar_url      VARCHAR(256),                                       -- 头像 URL
    role_code       VARCHAR(32)     NOT NULL DEFAULT 'user',            -- 角色编码，关联 auth_role.role_code
    status          INTEGER         NOT NULL DEFAULT 0,                 -- 状态：0=正常 1=封禁
    banned_at       TIMESTAMP,                                          -- 封禁时间
    banned_reason   VARCHAR(256),                                       -- 封禁原因
    last_login_at   TIMESTAMP,                                          -- 最后登录时间
    organization_id BIGINT,                                             -- 所属机构 ID（可空，达人可独立存在）
    deleted         INTEGER         NOT NULL DEFAULT 0,                 -- 逻辑删除：0=正常 1=已删除
    create_time     TIMESTAMP                DEFAULT CURRENT_TIMESTAMP, -- 注册时间
    update_time     TIMESTAMP                DEFAULT CURRENT_TIMESTAMP  -- 更新时间
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_auth_user_username ON auth_user (username) WHERE deleted = 0;

COMMENT ON TABLE  auth_user                  IS '用户表';
COMMENT ON COLUMN auth_user.username         IS '用户名（唯一）';
COMMENT ON COLUMN auth_user.password_hash    IS '密码哈希（BCrypt）';
COMMENT ON COLUMN auth_user.mobile           IS '手机号';
COMMENT ON COLUMN auth_user.email            IS '邮箱';
COMMENT ON COLUMN auth_user.nickname         IS '昵称';
COMMENT ON COLUMN auth_user.avatar_url       IS '头像 URL';
COMMENT ON COLUMN auth_user.role_code        IS '角色编码，关联 auth_role.role_code';
COMMENT ON COLUMN auth_user.status           IS '状态：0=正常 1=封禁';
COMMENT ON COLUMN auth_user.banned_at        IS '封禁时间';
COMMENT ON COLUMN auth_user.banned_reason    IS '封禁原因';
COMMENT ON COLUMN auth_user.last_login_at    IS '最后登录时间';
COMMENT ON COLUMN auth_user.organization_id  IS '所属机构 ID（可空，达人可独立存在）';
COMMENT ON COLUMN auth_user.deleted          IS '逻辑删除：0=正常 1=已删除';

-- ============================================================
-- 3. auth_resource - 资源表（菜单/API/按钮）
-- Entity: cn.gaifan.douyinOperations.module.auth.entity.AuthResource
-- ============================================================
CREATE TABLE IF NOT EXISTS auth_resource (
    id              BIGSERIAL       PRIMARY KEY,
    resource_type   VARCHAR(16)     NOT NULL,                           -- 类型：menu / api / button
    resource_code   VARCHAR(256)    NOT NULL,                           -- 资源编码（菜单路径/API路径/按钮标识）
    resource_name   VARCHAR(128),                                       -- 资源名称
    module          VARCHAR(64),                                        -- 所属模块：auth, douyin, live...
    request_method  VARCHAR(16),                                        -- 请求方式：GET/POST（api 类型用）
    parent_id       BIGINT                   DEFAULT 0,                 -- 父级 ID（menu 类型用于构建树）
    sort_order      INTEGER                  DEFAULT 0,                 -- 排序
    deleted         INTEGER         NOT NULL DEFAULT 0,                 -- 逻辑删除
    create_time     TIMESTAMP                DEFAULT CURRENT_TIMESTAMP,
    update_time     TIMESTAMP                DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE  auth_resource                 IS '资源表（菜单/API/按钮）';
COMMENT ON COLUMN auth_resource.resource_type   IS '类型：menu / api / button';
COMMENT ON COLUMN auth_resource.resource_code   IS '资源编码（菜单路径/API路径/按钮标识）';
COMMENT ON COLUMN auth_resource.resource_name   IS '资源名称';
COMMENT ON COLUMN auth_resource.module          IS '所属模块：auth, douyin, live...';
COMMENT ON COLUMN auth_resource.request_method  IS '请求方式：GET/POST（api 类型用）';
COMMENT ON COLUMN auth_resource.parent_id       IS '父级 ID（menu 类型用于构建树）';
COMMENT ON COLUMN auth_resource.sort_order      IS '排序';
COMMENT ON COLUMN auth_resource.deleted         IS '逻辑删除：0=正常 1=已删除';

-- ============================================================
-- 4. auth_role_resource - 角色-资源关联表
-- Entity: cn.gaifan.douyinOperations.module.auth.entity.AuthRoleResource
-- 注意：关联表无 deleted 字段，授权更新采用"事务内全删重建"策略
-- ============================================================
CREATE TABLE IF NOT EXISTS auth_role_resource (
    id          BIGSERIAL   PRIMARY KEY,
    role_id     BIGINT      NOT NULL,                           -- 角色 ID
    resource_id BIGINT      NOT NULL,                           -- 资源 ID
    create_time TIMESTAMP            DEFAULT CURRENT_TIMESTAMP  -- 创建时间
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_auth_role_resource ON auth_role_resource (role_id, resource_id);

COMMENT ON TABLE  auth_role_resource              IS '角色-资源关联表（无逻辑删除，全删重建）';
COMMENT ON COLUMN auth_role_resource.role_id      IS '角色 ID';
COMMENT ON COLUMN auth_role_resource.resource_id  IS '资源 ID';

-- ============================================================
-- 5. auth_login_log - 登录日志表
-- Entity: cn.gaifan.douyinOperations.module.auth.entity.AuthLoginLog
-- 说明：支持成功/失败审计，user_id 失败时可为空（如用户不存在）
-- ============================================================
CREATE TABLE IF NOT EXISTS auth_login_log (
    id          BIGSERIAL       PRIMARY KEY,
    user_id     BIGINT,                                             -- 用户 ID（失败时可为空）
    username    VARCHAR(64),                                        -- 用户名（冗余，失败时记录尝试的用户名）
    login_type  VARCHAR(32)     NOT NULL DEFAULT 'password',        -- 登录方式：password / sms / email / oauth
    device_type VARCHAR(16)     NOT NULL DEFAULT 'web',             -- 设备类型：web / ios / android
    ip          VARCHAR(64),                                        -- 登录 IP
    user_agent  VARCHAR(256),                                       -- 浏览器 User-Agent
    status      INTEGER         NOT NULL DEFAULT 1,                 -- 1=成功 0=失败
    fail_reason VARCHAR(256),                                       -- 失败原因（如：密码错误、用户不存在）
    login_time  TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP  -- 登录时间
);

CREATE INDEX IF NOT EXISTS idx_auth_login_log_user_time ON auth_login_log (user_id, login_time DESC) WHERE user_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_auth_login_log_login_time ON auth_login_log (login_time DESC);

COMMENT ON TABLE  auth_login_log              IS '登录日志表（支持成功/失败审计）';
COMMENT ON COLUMN auth_login_log.user_id      IS '用户 ID（失败时可为空）';
COMMENT ON COLUMN auth_login_log.username    IS '用户名（冗余，失败时记录尝试的用户名）';
COMMENT ON COLUMN auth_login_log.login_type   IS '登录方式：password / sms / email / oauth';
COMMENT ON COLUMN auth_login_log.device_type  IS '设备类型：web / ios / android';
COMMENT ON COLUMN auth_login_log.ip           IS '登录 IP';
COMMENT ON COLUMN auth_login_log.user_agent   IS '浏览器 User-Agent';
COMMENT ON COLUMN auth_login_log.status       IS '1=成功 0=失败';
COMMENT ON COLUMN auth_login_log.fail_reason  IS '失败原因';
COMMENT ON COLUMN auth_login_log.login_time   IS '登录时间';

-- ============================================================
-- 6. auth_verify_code - 验证码表
-- Entity: cn.gaifan.douyinOperations.module.auth.entity.AuthVerifyCode
-- ============================================================
CREATE TABLE IF NOT EXISTS auth_verify_code (
    id          BIGSERIAL       PRIMARY KEY,
    target      VARCHAR(128)    NOT NULL,                           -- 手机号或邮箱
    code        VARCHAR(16)     NOT NULL,                           -- 验证码
    type        VARCHAR(32)     NOT NULL,                           -- 用途：login / forgot_password
    expire_at   TIMESTAMP       NOT NULL,                           -- 过期时间
    used        INTEGER         NOT NULL DEFAULT 0,                 -- 是否已使用：0=未用 1=已用
    try_count   INTEGER         NOT NULL DEFAULT 0,                 -- 尝试次数
    create_time TIMESTAMP                DEFAULT CURRENT_TIMESTAMP  -- 创建时间
);

COMMENT ON TABLE  auth_verify_code              IS '验证码表';
COMMENT ON COLUMN auth_verify_code.target       IS '手机号或邮箱';
COMMENT ON COLUMN auth_verify_code.code         IS '验证码';
COMMENT ON COLUMN auth_verify_code.type         IS '用途：login / forgot_password';
COMMENT ON COLUMN auth_verify_code.expire_at    IS '过期时间';
COMMENT ON COLUMN auth_verify_code.used         IS '是否已使用：0=未用 1=已用';
COMMENT ON COLUMN auth_verify_code.try_count    IS '尝试次数';

-- ============================================================
-- 7. auth_third_party_bind - 第三方绑定表
-- Entity: cn.gaifan.douyinOperations.module.auth.entity.AuthThirdPartyBind
-- ============================================================
CREATE TABLE IF NOT EXISTS auth_third_party_bind (
    id          BIGSERIAL       PRIMARY KEY,
    user_id     BIGINT          NOT NULL,                           -- 用户 ID
    provider    VARCHAR(32)     NOT NULL,                           -- 第三方平台：douyin / wechat / qq / volcano
    open_id     VARCHAR(256)    NOT NULL,                           -- 第三方平台用户 ID
    union_id    VARCHAR(256),                                       -- 联合 ID
    nickname    VARCHAR(64),                                        -- 第三方昵称
    avatar      VARCHAR(512),                                       -- 第三方头像
    deleted     INTEGER         NOT NULL DEFAULT 0,                 -- 逻辑删除
    create_time TIMESTAMP                DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP                DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_auth_third_party_bind ON auth_third_party_bind (provider, open_id) WHERE deleted = 0;

COMMENT ON TABLE  auth_third_party_bind              IS '第三方绑定表';
COMMENT ON COLUMN auth_third_party_bind.user_id      IS '用户 ID';
COMMENT ON COLUMN auth_third_party_bind.provider     IS '第三方平台：douyin / wechat / qq / volcano';
COMMENT ON COLUMN auth_third_party_bind.open_id      IS '第三方平台用户 ID';
COMMENT ON COLUMN auth_third_party_bind.union_id     IS '联合 ID';
COMMENT ON COLUMN auth_third_party_bind.nickname     IS '第三方昵称';
COMMENT ON COLUMN auth_third_party_bind.avatar       IS '第三方头像';
COMMENT ON COLUMN auth_third_party_bind.deleted      IS '逻辑删除：0=正常 1=已删除';

-- ============================================================
-- 初始数据
-- ============================================================
INSERT INTO auth_role (role_code, role_name, status, sort_order) VALUES
('admin', '平台管理员', 1, 1),
('institution', '机构管理员', 1, 5),
('talent', '达人/主播', 1, 8),
('user', '普通用户', 1, 10)
ON CONFLICT DO NOTHING;
