-- ============================================================
-- douyin 模块 - 资源初始数据
-- 依赖：schema.sql（表结构）
-- 数据库：PostgreSQL
-- 说明：插入菜单资源、API 资源，以及角色-资源绑定
-- 幂等：使用 ON CONFLICT DO NOTHING，可重复执行
-- ============================================================

-- ============================================================
-- 1. 菜单资源（resource_type = 'menu'）
-- ============================================================
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, request_method, parent_id, sort_order)
VALUES
    -- 抖音管理（顶级菜单）
    ('menu', '/douyin',                     '抖音管理', 'douyin',      NULL, 0, 500),
    -- 抖音管理子菜单
    ('menu', '/douyin/account',             '账号管理', 'douyin',      NULL, 0, 1),
    ('menu', '/douyin/video',               '视频分析', 'douyin',      NULL, 0, 2)
ON CONFLICT DO NOTHING;

-- 更新子菜单的 parent_id 指向「抖音管理」
UPDATE auth_resource
SET    parent_id = (SELECT id FROM auth_resource WHERE resource_code = '/douyin' AND resource_type = 'menu' AND deleted = 0 LIMIT 1)
WHERE  resource_code IN ('/douyin/account', '/douyin/video')
  AND  resource_type = 'menu'
  AND  deleted = 0
  AND  parent_id = 0;

-- ============================================================
-- 2. API 资源（resource_type = 'api'）
-- ============================================================

-- ---------- DouyinAccountController ----------
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, request_method, parent_id, sort_order)
VALUES
    ('api', '/api/v1/douyin/account/search',      '搜索账号',       'douyin', 'POST', 0, 0),
    ('api', '/api/v1/douyin/account/get',         '获取账号详情',   'douyin', 'POST', 0, 0),
    ('api', '/api/v1/douyin/account/save',        '保存账号',       'douyin', 'POST', 0, 0),
    ('api', '/api/v1/douyin/account/delete',      '删除账号',       'douyin', 'POST', 0, 0),
    ('api', '/api/v1/douyin/account/statistics',  '账号统计',       'douyin', 'POST', 0, 0)
ON CONFLICT DO NOTHING;

-- ---------- DouyinVideoController ----------
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, request_method, parent_id, sort_order)
VALUES
    ('api', '/api/v1/douyin/video/search',   '搜索视频',       'douyin', 'POST', 0, 0),
    ('api', '/api/v1/douyin/video/get',      '获取视频详情',   'douyin', 'POST', 0, 0),
    ('api', '/api/v1/douyin/video/save',     '保存视频',       'douyin', 'POST', 0, 0),
    ('api', '/api/v1/douyin/video/sync',     '同步视频',       'douyin', 'POST', 0, 0)
ON CONFLICT DO NOTHING;

-- ============================================================
-- 3. admin 角色授权
-- ============================================================
INSERT INTO auth_role_resource (role_id, resource_id)
SELECT
    (SELECT id FROM auth_role WHERE role_code = 'admin' AND deleted = 0 LIMIT 1) AS role_id,
    id AS resource_id
FROM auth_resource
WHERE resource_code IN (
    '/douyin',
    '/douyin/account',
    '/douyin/video',
    '/api/v1/douyin/account/search',
    '/api/v1/douyin/account/get',
    '/api/v1/douyin/account/save',
    '/api/v1/douyin/account/delete',
    '/api/v1/douyin/account/statistics',
    '/api/v1/douyin/video/search',
    '/api/v1/douyin/video/get',
    '/api/v1/douyin/video/save',
    '/api/v1/douyin/video/sync'
)
AND resource_type IN ('menu', 'api')
AND deleted = 0
ON CONFLICT DO NOTHING;
