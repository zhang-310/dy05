-- ============================================================
-- config 模块 - 权限资源初始化数据
-- ============================================================

-- 1. 菜单资源：配置管理（一级菜单）
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, parent_id, sort_order)
VALUES ('menu', '/config', '配置管理', 'config', 0, 70)
ON CONFLICT DO NOTHING;

-- 2. 子菜单：系统配置
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, parent_id, sort_order)
SELECT 'menu', '/config/system', '系统配置', 'config', id, 1
FROM auth_resource
WHERE resource_code = '/config' AND resource_type = 'menu' AND deleted = 0
ON CONFLICT DO NOTHING;

-- 3. 子菜单：行业分类
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, parent_id, sort_order)
SELECT 'menu', '/config/industry', '行业分类', 'config', id, 2
FROM auth_resource
WHERE resource_code = '/config' AND resource_type = 'menu' AND deleted = 0
ON CONFLICT DO NOTHING;

-- 4. API 资源：配置管理
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, request_method)
VALUES
  ('api', '/api/v1/config/group/list',    '配置分组列表',   'config', 'POST'),
  ('api', '/api/v1/config/group/save',    '保存配置分组',   'config', 'POST'),
  ('api', '/api/v1/config/group/delete',  '删除配置分组',   'config', 'POST'),
  ('api', '/api/v1/config/item/list',     '配置项列表',     'config', 'POST'),
  ('api', '/api/v1/config/item/get',      '获取配置项',     'config', 'POST'),
  ('api', '/api/v1/config/item/save',     '保存配置项',     'config', 'POST'),
  ('api', '/api/v1/config/item/delete',   '删除配置项',     'config', 'POST'),
  ('api', '/api/v1/config/industry/list', '行业分类列表',   'config', 'POST'),
  ('api', '/api/v1/config/industry/save', '保存行业分类',   'config', 'POST'),
  ('api', '/api/v1/config/industry/delete','删除行业分类',  'config', 'POST')
ON CONFLICT DO NOTHING;

-- 5. 按钮资源
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module)
SELECT 'button', 'config:item:save', '保存配置', 'config'
WHERE NOT EXISTS (SELECT 1 FROM auth_resource WHERE resource_code = 'config:item:save' AND deleted = 0)
ON CONFLICT DO NOTHING;

INSERT INTO auth_resource (resource_type, resource_code, resource_name, module)
SELECT 'button', 'config:item:delete', '删除配置', 'config'
WHERE NOT EXISTS (SELECT 1 FROM auth_resource WHERE resource_code = 'config:item:delete' AND deleted = 0)
ON CONFLICT DO NOTHING;

INSERT INTO auth_resource (resource_type, resource_code, resource_name, module)
SELECT 'button', 'config:industry:save', '保存行业分类', 'config'
WHERE NOT EXISTS (SELECT 1 FROM auth_resource WHERE resource_code = 'config:industry:save' AND deleted = 0)
ON CONFLICT DO NOTHING;

-- 6. 角色绑定：admin 拥有 config 模块所有权限
INSERT INTO auth_role_resource (role_id, resource_id)
SELECT ar.id, res.id
FROM auth_role ar, auth_resource res
WHERE ar.role_code = 'admin'
  AND res.module = 'config'
  AND res.deleted = 0
ON CONFLICT DO NOTHING;
