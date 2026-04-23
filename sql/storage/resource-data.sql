-- ============================================================
-- 存储管理模块 - 资源初始数据
-- 依赖：schema.sql（sys_file 表）
-- 数据库：PostgreSQL
-- 说明：插入菜单资源、API 资源，以及角色-资源绑定
-- 幂等：使用 ON CONFLICT DO NOTHING，可重复执行
-- ============================================================

-- ============================================================
-- 1. 菜单资源（resource_type = 'menu'）
-- ============================================================
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, request_method, parent_id, sort_order)
VALUES
    ('menu', '/storage', '存储管理', 'storage', NULL, 0, 600)
ON CONFLICT DO NOTHING;

-- ============================================================
-- 2. API 资源（resource_type = 'api'）
-- ============================================================
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, request_method, parent_id, sort_order)
VALUES
    ('api', '/api/v1/storage/upload',      '文件上传',       'storage', 'POST', 0, 0),
    ('api', '/api/v1/storage/file/list',   '文件列表',       'storage', 'POST', 0, 0),
    ('api', '/api/v1/storage/file/delete', '文件删除',       'storage', 'POST', 0, 0),
    ('api', '/api/v1/storage/file/get',    '获取文件详情',   'storage', 'GET',  0, 0)
ON CONFLICT DO NOTHING;

-- ============================================================
-- 3. 按钮资源（resource_type = 'button'）
-- ============================================================
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, request_method, parent_id, sort_order)
VALUES
    ('button', 'storage:upload',  '上传文件',   'storage', NULL, 0, 0),
    ('button', 'storage:delete',  '删除文件',   'storage', NULL, 0, 0)
ON CONFLICT DO NOTHING;

-- ============================================================
-- 4. 角色-资源绑定（admin 角色）
-- ============================================================
INSERT INTO auth_role_resource (role_id, resource_id)
SELECT
    (SELECT id FROM auth_role WHERE role_code = 'admin' AND deleted = 0 LIMIT 1),
    id
FROM auth_resource
WHERE module = 'storage'
  AND deleted = 0
  AND id NOT IN (
      SELECT resource_id FROM auth_role_resource
      WHERE role_id = (SELECT id FROM auth_role WHERE role_code = 'admin' AND deleted = 0 LIMIT 1)
  )
ON CONFLICT DO NOTHING;
