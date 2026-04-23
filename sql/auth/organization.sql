-- ============================================================
-- 机构与机构成员表
-- 模块：组织管理（Organization）
-- 数据库：PostgreSQL
-- ============================================================

-- 1. auth_organization - 机构表
CREATE TABLE IF NOT EXISTS auth_organization (
    id              BIGSERIAL       PRIMARY KEY,
    org_name        VARCHAR(128)    NOT NULL,                           -- 机构名称
    org_code        VARCHAR(64),                                       -- 机构编码（唯一）
    contact_name    VARCHAR(64),                                       -- 联系人
    contact_phone   VARCHAR(20),                                       -- 联系电话
    status          INTEGER         NOT NULL DEFAULT 1,                 -- 1=正常 0=禁用
    owner_id        BIGINT          NOT NULL,                           -- 创建者（institution 角色用户）
    deleted         INTEGER         NOT NULL DEFAULT 0,
    create_time     TIMESTAMP                DEFAULT CURRENT_TIMESTAMP,
    update_time     TIMESTAMP                DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_auth_org_code ON auth_organization (org_code) WHERE deleted = 0 AND org_code IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_auth_org_owner ON auth_organization (owner_id) WHERE deleted = 0;

COMMENT ON TABLE  auth_organization              IS '机构表';
COMMENT ON COLUMN auth_organization.org_name     IS '机构名称';
COMMENT ON COLUMN auth_organization.org_code     IS '机构编码（唯一）';
COMMENT ON COLUMN auth_organization.contact_name IS '联系人';
COMMENT ON COLUMN auth_organization.contact_phone IS '联系电话';
COMMENT ON COLUMN auth_organization.status       IS '状态：1=正常 0=禁用';
COMMENT ON COLUMN auth_organization.owner_id     IS '创建者（institution 角色用户 ID）';
COMMENT ON COLUMN auth_organization.deleted      IS '逻辑删除：0=正常 1=已删除';

-- 2. auth_org_member - 机构成员表
CREATE TABLE IF NOT EXISTS auth_org_member (
    id              BIGSERIAL       PRIMARY KEY,
    org_id          BIGINT          NOT NULL,                           -- 机构 ID
    user_id         BIGINT          NOT NULL,                           -- 用户 ID（talent）
    role_in_org     VARCHAR(32)              DEFAULT 'member',          -- 机构内角色：owner / admin / member
    status          INTEGER         NOT NULL DEFAULT 0,                 -- 0=待确认 1=已加入 2=已拒绝
    invited_at      TIMESTAMP                DEFAULT CURRENT_TIMESTAMP, -- 邀请时间
    joined_at       TIMESTAMP,                                          -- 加入时间
    deleted         INTEGER         NOT NULL DEFAULT 0,
    create_time     TIMESTAMP                DEFAULT CURRENT_TIMESTAMP,
    update_time     TIMESTAMP                DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_auth_org_member ON auth_org_member (org_id, user_id) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_auth_org_member_user ON auth_org_member (user_id) WHERE deleted = 0;

COMMENT ON TABLE  auth_org_member              IS '机构成员表';
COMMENT ON COLUMN auth_org_member.org_id       IS '机构 ID';
COMMENT ON COLUMN auth_org_member.user_id      IS '用户 ID（talent 角色）';
COMMENT ON COLUMN auth_org_member.role_in_org  IS '机构内角色：owner / admin / member';
COMMENT ON COLUMN auth_org_member.status       IS '状态：0=待确认 1=已加入 2=已拒绝';
COMMENT ON COLUMN auth_org_member.invited_at   IS '邀请时间';
COMMENT ON COLUMN auth_org_member.joined_at    IS '加入时间';
COMMENT ON COLUMN auth_org_member.deleted      IS '逻辑删除：0=正常 1=已删除';
