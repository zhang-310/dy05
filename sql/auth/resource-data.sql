-- ============================================================
-- auth 模块 - 资源初始数据
-- 依赖：schema.sql（表结构 + 角色初始数据）
-- 数据库：PostgreSQL
-- 说明：插入菜单资源、API 资源，以及角色-资源绑定
-- 幂等：使用 ON CONFLICT DO NOTHING，可重复执行
-- ============================================================

-- ============================================================
-- 1. 菜单资源（resource_type = 'menu'）
-- ============================================================
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, request_method, parent_id, sort_order)
VALUES
    -- 工作台（顶级菜单）
    ('menu', '/dashboard',          '工作台',   'dashboard', NULL, 0, 1),
    -- 系统管理（顶级菜单）
    ('menu', '/auth',               '系统管理', 'auth',      NULL, 0, 900),
    -- 系统管理子菜单（parent_id 通过子查询填充，先用 0 占位，后续更新）
    ('menu', '/auth/users',         '用户管理', 'auth',      NULL, 0, 1),
    ('menu', '/auth/roles',         '角色管理', 'auth',      NULL, 0, 2),
    ('menu', '/auth/resources',     '资源管理', 'auth',      NULL, 0, 3),
    ('menu', '/auth/login-logs',    '登录日志', 'auth',      NULL, 0, 4)
ON CONFLICT DO NOTHING;

-- 更新子菜单的 parent_id 指向「系统管理」
UPDATE auth_resource
SET    parent_id = (SELECT id FROM auth_resource WHERE resource_code = '/auth' AND resource_type = 'menu' AND deleted = 0 LIMIT 1)
WHERE  resource_code IN ('/auth/users', '/auth/roles', '/auth/resources', '/auth/login-logs')
  AND  resource_type = 'menu'
  AND  deleted = 0
  AND  parent_id = 0;

-- ============================================================
-- 2. API 资源（resource_type = 'api'）
-- ============================================================

-- ---------- AuthController ----------
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, request_method, parent_id, sort_order)
VALUES
    ('api', '/api/v1/auth/login',                   '登录',           'auth', 'POST', 0, 0),
    ('api', '/api/v1/auth/logout',                  '登出',           'auth', 'POST', 0, 0),
    ('api', '/api/v1/auth/captcha',                 '获取验证码图片', 'auth', 'GET',  0, 0),
    ('api', '/api/v1/auth/sms/send',                '发送短信验证码', 'auth', 'POST', 0, 0),
    ('api', '/api/v1/auth/forgot-password',         '忘记密码',       'auth', 'POST', 0, 0),
    ('api', '/api/v1/auth/profile',                 '获取个人信息',   'auth', 'POST', 0, 0),
    ('api', '/api/v1/auth/profile/update',          '更新个人信息',   'auth', 'POST', 0, 0),
    ('api', '/api/v1/auth/profile/change-password',  '修改密码',       'auth', 'POST', 0, 0),
    ('api', '/api/v1/auth/menu/search',             '查询菜单列表',   'auth', 'POST', 0, 0),
    ('api', '/api/v1/auth/resource/search',         '查询资源编码',   'auth', 'POST', 0, 0)
ON CONFLICT DO NOTHING;

-- ---------- AuthUserController ----------
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, request_method, parent_id, sort_order)
VALUES
    ('api', '/api/v1/auth/user/search',      '搜索用户',       'auth', 'POST', 0, 0),
    ('api', '/api/v1/auth/user/get',         '获取用户详情',   'auth', 'POST', 0, 0),
    ('api', '/api/v1/auth/user/save',        '保存用户',       'auth', 'POST', 0, 0),
    ('api', '/api/v1/auth/user/ban',         '封禁/解封用户',  'auth', 'POST', 0, 0),
    ('api', '/api/v1/auth/user/login-logs',  '用户登录日志',   'auth', 'POST', 0, 0),
    ('api', '/api/v1/auth/user/online',      '在线用户列表',   'auth', 'POST', 0, 0)
ON CONFLICT DO NOTHING;

-- ---------- AuthRoleController ----------
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, request_method, parent_id, sort_order)
VALUES
    ('api', '/api/v1/auth/role/search',          '搜索角色',       'auth', 'POST', 0, 0),
    ('api', '/api/v1/auth/role/list',            '角色列表',       'auth', 'POST', 0, 0),
    ('api', '/api/v1/auth/role/get',             '获取角色详情',   'auth', 'POST', 0, 0),
    ('api', '/api/v1/auth/role/save',            '保存角色',       'auth', 'POST', 0, 0),
    ('api', '/api/v1/auth/role/delete',          '删除角色',       'auth', 'POST', 0, 0),
    ('api', '/api/v1/auth/role/resources',       '获取角色资源',   'auth', 'POST', 0, 0),
    ('api', '/api/v1/auth/role/resources/save',  '保存角色资源',   'auth', 'POST', 0, 0)
ON CONFLICT DO NOTHING;

-- ---------- AuthResourceController ----------
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, request_method, parent_id, sort_order)
VALUES
    ('api', '/api/v1/auth/resource/list',    '资源列表',       'auth', 'POST', 0, 0),
    ('api', '/api/v1/auth/resource/tree',    '资源树',         'auth', 'POST', 0, 0),
    ('api', '/api/v1/auth/resource/get',     '获取资源详情',   'auth', 'POST', 0, 0),
    ('api', '/api/v1/auth/resource/save',    '保存资源',       'auth', 'POST', 0, 0),
    ('api', '/api/v1/auth/resource/delete',  '删除资源',       'auth', 'POST', 0, 0)
ON CONFLICT DO NOTHING;

-- ---------- OrganizationController ----------
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, request_method, parent_id, sort_order)
VALUES
    ('api', '/api/v1/organization/my',                    '获取我的机构',   'auth', 'GET',  0, 0),
    ('api', '/api/v1/organization/create',                '创建机构',       'auth', 'POST', 0, 0),
    ('api', '/api/v1/organization/update',                '更新机构',       'auth', 'POST', 0, 0),
    ('api', '/api/v1/organization/members',               '机构成员列表',   'auth', 'GET',  0, 0),
    ('api', '/api/v1/organization/invite',                '邀请达人',       'auth', 'POST', 0, 0),
    ('api', '/api/v1/organization/remove',                '移除成员',       'auth', 'POST', 0, 0),
    ('api', '/api/v1/organization/invitations',           '待处理邀请',     'auth', 'GET',  0, 0),
    ('api', '/api/v1/organization/invitation/accept',     '接受邀请',       'auth', 'POST', 0, 0),
    ('api', '/api/v1/organization/invitation/reject',     '拒绝邀请',       'auth', 'POST', 0, 0),
    ('api', '/api/v1/organization/search-talents',        '搜索达人',       'auth', 'GET',  0, 0)
ON CONFLICT DO NOTHING;

-- ---------- DashboardController ----------
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, request_method, parent_id, sort_order)
VALUES
    ('api', '/api/v1/dashboard/admin',   '管理员Dashboard', 'auth', 'GET', 0, 0),
    ('api', '/api/v1/dashboard/org',     '机构Dashboard',   'auth', 'GET', 0, 0),
    ('api', '/api/v1/dashboard/talent',  '达人Dashboard',   'auth', 'GET', 0, 0)
ON CONFLICT DO NOTHING;

-- ---------- OAuth 相关 ----------
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, request_method, parent_id, sort_order)
VALUES
    ('api', '/api/v1/auth/oauth/bindings', '查询第三方绑定', 'auth', 'POST', 0, 0),
    ('api', '/api/v1/auth/oauth/bind',     '绑定第三方',     'auth', 'POST', 0, 0),
    ('api', '/api/v1/auth/oauth/unbind',   '解绑第三方',     'auth', 'POST', 0, 0)
ON CONFLICT DO NOTHING;

-- ============================================================
-- 3. 角色-资源绑定
-- ============================================================

-- ---------- admin 角色：绑定所有资源 ----------
INSERT INTO auth_role_resource (role_id, resource_id)
SELECT r.id, res.id
FROM   auth_role r
       CROSS JOIN auth_resource res
WHERE  r.role_code = 'admin'
  AND  r.deleted = 0
  AND  res.deleted = 0
ON CONFLICT DO NOTHING;

-- ---------- user 角色：仅绑定基本菜单 + 个人中心 API ----------
INSERT INTO auth_role_resource (role_id, resource_id)
SELECT r.id, res.id
FROM   auth_role r
       CROSS JOIN auth_resource res
WHERE  r.role_code = 'user'
  AND  r.deleted = 0
  AND  res.deleted = 0
  AND  res.resource_code IN (
           '/dashboard',
           '/api/v1/auth/profile',
           '/api/v1/auth/profile/update',
           '/api/v1/auth/profile/change-password',
           '/api/v1/auth/menu/search',
           '/api/v1/auth/resource/search',
           '/api/v1/auth/oauth/bindings',
           '/api/v1/auth/oauth/bind',
           '/api/v1/auth/oauth/unbind'
       )
ON CONFLICT DO NOTHING;

-- ---------- institution 角色：机构管理 + 业务运营 + Dashboard ----------
INSERT INTO auth_role_resource (role_id, resource_id)
SELECT r.id, res.id
FROM   auth_role r
       CROSS JOIN auth_resource res
WHERE  r.role_code = 'institution'
  AND  r.deleted = 0
  AND  res.deleted = 0
  AND  res.resource_code IN (
           -- 个人中心
           '/dashboard',
           '/api/v1/auth/profile',
           '/api/v1/auth/profile/update',
           '/api/v1/auth/profile/change-password',
           '/api/v1/auth/menu/search',
           '/api/v1/auth/resource/search',
           -- OAuth 绑定
           '/api/v1/auth/oauth/bindings',
           '/api/v1/auth/oauth/bind',
           '/api/v1/auth/oauth/unbind',
           -- 机构管理
           '/api/v1/organization/my',
           '/api/v1/organization/create',
           '/api/v1/organization/update',
           '/api/v1/organization/members',
           '/api/v1/organization/invite',
           '/api/v1/organization/remove',
           '/api/v1/organization/search-talents',
           -- Dashboard
           '/api/v1/dashboard/org'
       )
ON CONFLICT DO NOTHING;

-- ---------- talent 角色：核心运营 + 邀请处理 + Dashboard ----------
INSERT INTO auth_role_resource (role_id, resource_id)
SELECT r.id, res.id
FROM   auth_role r
       CROSS JOIN auth_resource res
WHERE  r.role_code = 'talent'
  AND  r.deleted = 0
  AND  res.deleted = 0
  AND  res.resource_code IN (
           -- 个人中心
           '/dashboard',
           '/api/v1/auth/profile',
           '/api/v1/auth/profile/update',
           '/api/v1/auth/profile/change-password',
           '/api/v1/auth/menu/search',
           '/api/v1/auth/resource/search',
           -- OAuth 绑定
           '/api/v1/auth/oauth/bindings',
           '/api/v1/auth/oauth/bind',
           '/api/v1/auth/oauth/unbind',
           -- 邀请处理
           '/api/v1/organization/invitations',
           '/api/v1/organization/invitation/accept',
           '/api/v1/organization/invitation/reject',
           -- Dashboard
           '/api/v1/dashboard/talent'
       )
ON CONFLICT DO NOTHING;
