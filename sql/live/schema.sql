-- ============================================================
-- live 模块 - 数据库表结构
-- 模块：直播管理（Live Management）
-- 数据库：PostgreSQL
-- 版本：1.0
-- 创建日期：2026-02-25
-- 说明：包含直播场次、产品关联、话术、监控数据等表
-- ============================================================

-- ============================================================
-- 1. live_session - 直播场次表
-- Entity: cn.gaifan.douyinOperations.module.live.entity.LiveSession
-- ============================================================
CREATE TABLE IF NOT EXISTS live_session (
    id                  BIGSERIAL       PRIMARY KEY,
    user_id             BIGINT          NOT NULL,                           -- 用户ID
    account_id          BIGINT,                                             -- 账号ID（所属账号）
    live_title          VARCHAR(256)    NOT NULL,                           -- 直播标题
    live_description    TEXT,                                               -- 直播描述
    scheduled_time      TIMESTAMP,                                          -- 预定直播时间
    start_time          TIMESTAMP,                                          -- 实际开始时间
    end_time            TIMESTAMP,                                          -- 实际结束时间
    live_url            VARCHAR(512),                                       -- 直播链接
    viewers             INTEGER                 DEFAULT 0,                  -- 观看人数
    likes               BIGINT                  DEFAULT 0,                  -- 点赞数
    status              INTEGER         NOT NULL DEFAULT 0,                 -- 状态：0=计划中 1=直播中 2=已结束 3=已取消
    recording_url       VARCHAR(512),                                       -- 录像URL
    recording_duration  BIGINT,                                             -- 录像时长（秒）
    deleted             INTEGER         NOT NULL DEFAULT 0,                 -- 逻辑删除：0=正常 1=已删除
    create_time         TIMESTAMP                DEFAULT CURRENT_TIMESTAMP, -- 创建时间
    update_time         TIMESTAMP                DEFAULT CURRENT_TIMESTAMP  -- 更新时间
);

CREATE INDEX IF NOT EXISTS idx_live_session_user_id ON live_session(user_id);
CREATE INDEX IF NOT EXISTS idx_live_session_account_id ON live_session(account_id);
CREATE INDEX IF NOT EXISTS idx_live_session_status ON live_session(status);

COMMENT ON TABLE  live_session                 IS '直播场次表';
COMMENT ON COLUMN live_session.user_id         IS '用户ID';
COMMENT ON COLUMN live_session.account_id      IS '账号ID（所属账号）';
COMMENT ON COLUMN live_session.live_title      IS '直播标题';
COMMENT ON COLUMN live_session.live_description IS '直播描述';
COMMENT ON COLUMN live_session.scheduled_time  IS '预定直播时间';
COMMENT ON COLUMN live_session.start_time      IS '实际开始时间';
COMMENT ON COLUMN live_session.end_time        IS '实际结束时间';
COMMENT ON COLUMN live_session.live_url        IS '直播链接';
COMMENT ON COLUMN live_session.viewers         IS '观看人数';
COMMENT ON COLUMN live_session.likes           IS '点赞数';
COMMENT ON COLUMN live_session.status          IS '状态：0=计划中 1=直播中 2=已结束 3=已取消';
COMMENT ON COLUMN live_session.recording_url   IS '录像URL';
COMMENT ON COLUMN live_session.recording_duration IS '录像时长（秒）';
COMMENT ON COLUMN live_session.deleted         IS '逻辑删除：0=正常 1=已删除';

-- ============================================================
-- 2. live_product - 直播产品关系表
-- Entity: cn.gaifan.douyinOperations.module.live.entity.LiveProduct
-- ============================================================
CREATE TABLE IF NOT EXISTS live_product (
    id                  BIGSERIAL       PRIMARY KEY,
    session_id          BIGINT          NOT NULL,                           -- 直播场次ID
    product_id          BIGINT          NOT NULL,                           -- 产品ID
    product_name        VARCHAR(256),                                       -- 产品名称（冗余）
    sale_quantity       INTEGER                 DEFAULT 0,                  -- 销售数量
    revenue             NUMERIC(12, 2)          DEFAULT 0,                  -- 销售额
    position            INTEGER,                                            -- 展示位置（1-5）
    create_time         TIMESTAMP                DEFAULT CURRENT_TIMESTAMP  -- 创建时间
);

CREATE INDEX IF NOT EXISTS idx_live_product_session_id ON live_product(session_id);
CREATE INDEX IF NOT EXISTS idx_live_product_product_id ON live_product(product_id);
CREATE UNIQUE INDEX IF NOT EXISTS uk_live_product_session_product ON live_product(session_id, product_id);

COMMENT ON TABLE  live_product             IS '直播产品关系表';
COMMENT ON COLUMN live_product.session_id  IS '直播场次ID';
COMMENT ON COLUMN live_product.product_id  IS '产品ID';
COMMENT ON COLUMN live_product.product_name IS '产品名称（冗余）';
COMMENT ON COLUMN live_product.sale_quantity IS '销售数量';
COMMENT ON COLUMN live_product.revenue     IS '销售额';
COMMENT ON COLUMN live_product.position    IS '展示位置（1-5）';

-- ============================================================
-- 3. live_script - 直播话术表
-- Entity: cn.gaifan.douyinOperations.module.live.entity.LiveScript
-- ============================================================
CREATE TABLE IF NOT EXISTS live_script (
    id                      BIGSERIAL       PRIMARY KEY,
    session_id              BIGINT          NOT NULL,                       -- 直播场次ID
    script_content          TEXT            NOT NULL,                       -- 话术内容
    sequence_no             INTEGER,                                        -- 话术顺序
    execution_time          BIGINT,                                         -- 应该执行的时间点（相对于直播开始，单位秒）
    executed                INTEGER                 DEFAULT 0,              -- 是否已执行：0=未执行 1=已执行
    actual_execution_time   TIMESTAMP,                                      -- 实际执行时间
    deleted                 INTEGER         NOT NULL DEFAULT 0,             -- 逻辑删除：0=正常 1=已删除
    create_time             TIMESTAMP                DEFAULT CURRENT_TIMESTAMP, -- 创建时间
    update_time             TIMESTAMP                DEFAULT CURRENT_TIMESTAMP  -- 更新时间
);

CREATE INDEX IF NOT EXISTS idx_live_script_session_id ON live_script(session_id);
CREATE INDEX IF NOT EXISTS idx_live_script_executed ON live_script(executed);

COMMENT ON TABLE  live_script                   IS '直播话术表';
COMMENT ON COLUMN live_script.session_id        IS '直播场次ID';
COMMENT ON COLUMN live_script.script_content    IS '话术内容';
COMMENT ON COLUMN live_script.sequence_no       IS '话术顺序';
COMMENT ON COLUMN live_script.execution_time    IS '应该执行的时间点（相对于直播开始，单位秒）';
COMMENT ON COLUMN live_script.executed          IS '是否已执行：0=未执行 1=已执行';
COMMENT ON COLUMN live_script.actual_execution_time IS '实际执行时间';
COMMENT ON COLUMN live_script.deleted           IS '逻辑删除：0=正常 1=已删除';

-- ============================================================
-- 4. live_monitor - 直播监控数据表
-- Entity: cn.gaifan.douyinOperations.module.live.entity.LiveMonitor
-- ============================================================
CREATE TABLE IF NOT EXISTS live_monitor (
    id                      BIGSERIAL       PRIMARY KEY,
    session_id              BIGINT          NOT NULL,                       -- 直播场次ID
    timestamp               TIMESTAMP       NOT NULL,                       -- 数据采集时间
    viewers                 INTEGER                 DEFAULT 0,              -- 观看人数
    likes                   BIGINT                  DEFAULT 0,              -- 点赞数
    comments                INTEGER                 DEFAULT 0,              -- 评论数
    shares                  INTEGER                 DEFAULT 0,              -- 分享数
    product_impressions     INTEGER                 DEFAULT 0,              -- 商品曝光数
    create_time             TIMESTAMP                DEFAULT CURRENT_TIMESTAMP  -- 记录时间
);

CREATE INDEX IF NOT EXISTS idx_live_monitor_session_id ON live_monitor(session_id);
CREATE INDEX IF NOT EXISTS idx_live_monitor_timestamp ON live_monitor(timestamp);
CREATE INDEX IF NOT EXISTS idx_live_monitor_session_time ON live_monitor(session_id, timestamp);

COMMENT ON TABLE  live_monitor                  IS '直播监控数据表';
COMMENT ON COLUMN live_monitor.session_id       IS '直播场次ID';
COMMENT ON COLUMN live_monitor.timestamp        IS '数据采集时间';
COMMENT ON COLUMN live_monitor.viewers          IS '观看人数';
COMMENT ON COLUMN live_monitor.likes            IS '点赞数';
COMMENT ON COLUMN live_monitor.comments         IS '评论数';
COMMENT ON COLUMN live_monitor.shares           IS '分享数';
COMMENT ON COLUMN live_monitor.product_impressions IS '商品曝光数';
