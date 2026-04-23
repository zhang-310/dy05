-- ============================================================
-- log 模块 - 权限资源初始化数据
-- ============================================================

-- 1. 菜单资源：日志管理（一级菜单）
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, parent_id, sort_order)
VALUES ('menu', '/log', '日志管理', 'log', 0, 60)
ON CONFLICT DO NOTHING;

-- 2. 子菜单：操作日志
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, parent_id, sort_order)
SELECT 'menu', '/log/operation', '操作日志', 'log', id, 1
FROM auth_resource
WHERE resource_code = '/log' AND resource_type = 'menu' AND deleted = 0
ON CONFLICT DO NOTHING;

-- 3. 子菜单：系统日志
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, parent_id, sort_order)
SELECT 'menu', '/log/system', '系统日志', 'log', id, 2
FROM auth_resource
WHERE resource_code = '/log' AND resource_type = 'menu' AND deleted = 0
ON CONFLICT DO NOTHING;

-- 4. API 资源：操作日志
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, request_method)
VALUES
  ('api', '/api/v1/log/operation/list',   '操作日志列表',   'log', 'POST'),
  ('api', '/api/v1/log/operation/get',    '操作日志详情',   'log', 'POST'),
  ('api', '/api/v1/log/operation/delete', '删除操作日志',   'log', 'POST'),
  ('api', '/api/v1/log/operation/export', '导出操作日志',   'log', 'POST'),
  ('api', '/api/v1/log/system/list',      '系统日志列表',   'log', 'POST'),
  ('api', '/api/v1/log/system/get',       '系统日志详情',   'log', 'POST')
ON CONFLICT DO NOTHING;

-- 5. 按钮资源
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module)
SELECT 'button', 'log:operation:delete', '删除操作日志', 'log'
WHERE NOT EXISTS (SELECT 1 FROM auth_resource WHERE resource_code = 'log:operation:delete' AND deleted = 0)
ON CONFLICT DO NOTHING;

INSERT INTO auth_resource (resource_type, resource_code, resource_name, module)
SELECT 'button', 'log:operation:export', '导出操作日志', 'log'
WHERE NOT EXISTS (SELECT 1 FROM auth_resource WHERE resource_code = 'log:operation:export' AND deleted = 0)
ON CONFLICT DO NOTHING;

-- 6. 角色绑定：admin 拥有 log 模块所有权限
INSERT INTO auth_role_resource (role_id, resource_id)
SELECT ar.id, res.id
FROM auth_role ar, auth_resource res
WHERE ar.role_code = 'admin'
  AND res.module = 'log'
  AND res.deleted = 0
ON CONFLICT DO NOTHING;
