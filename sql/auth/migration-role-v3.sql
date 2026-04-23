-- ============================================================
-- 角色迁移脚本 v3：team_admin → institution，新增 talent
-- 执行前提：已有 auth_role 表（schema.sql v3.0）
-- ============================================================

-- 1. team_admin 重命名为 institution
UPDATE auth_role
SET role_code  = 'institution',
    role_name  = '机构管理员',
    sort_order = 5,
    update_time = CURRENT_TIMESTAMP
WHERE role_code = 'team_admin' AND deleted = 0;

-- 2. 同步 auth_user 中引用的 role_code
UPDATE auth_user
SET role_code   = 'institution',
    update_time = CURRENT_TIMESTAMP
WHERE role_code = 'team_admin' AND deleted = 0;

-- 3. 新增 talent 角色
INSERT INTO auth_role (role_code, role_name, status, sort_order)
VALUES ('talent', '达人/主播', 1, 8)
ON CONFLICT DO NOTHING;

-- 4. auth_user 新增 organization_id 字段
ALTER TABLE auth_user ADD COLUMN IF NOT EXISTS organization_id BIGINT;
COMMENT ON COLUMN auth_user.organization_id IS '所属机构 ID（可空，达人可独立存在）';
