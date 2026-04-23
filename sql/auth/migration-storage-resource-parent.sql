-- ============================================================
-- 迁移：存储管理 API/按钮 的 parent_id 指向存储管理菜单
-- 问题：资源管理页点击「存储管理」时右侧为空，因这些资源的 parent_id=0
-- 解决：将 storage 模块的 api/button 的 parent_id 设为 /storage 菜单的 id
-- 用法：psql -h localhost -p 5433 -U postgres -d douyin_operations -f sql/auth/migration-storage-resource-parent.sql
-- ============================================================

-- 1. 更新现有 storage API/button 的 parent_id
UPDATE auth_resource r
SET parent_id = m.id
FROM (SELECT id FROM auth_resource WHERE resource_code = '/storage' AND resource_type = 'menu' AND deleted = 0 LIMIT 1) m
WHERE r.module = 'storage'
  AND r.resource_type IN ('api', 'button')
  AND r.deleted = 0
  AND (r.parent_id IS NULL OR r.parent_id = 0);

-- 2. 补齐缺失的 storage 资源（若不存在）
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, request_method, parent_id, sort_order)
SELECT 'api', '/api/v1/storage/configured', '存储配置检查', 'storage', 'GET', m.id, 0
FROM (SELECT id FROM auth_resource WHERE resource_code = '/storage' AND resource_type = 'menu' AND deleted = 0 LIMIT 1) m
WHERE NOT EXISTS (SELECT 1 FROM auth_resource WHERE resource_code = '/api/v1/storage/configured' AND deleted = 0);

INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, request_method, parent_id, sort_order)
SELECT 'api', '/api/v1/storage/url', '文件URL', 'storage', 'GET', m.id, 4
FROM (SELECT id FROM auth_resource WHERE resource_code = '/storage' AND resource_type = 'menu' AND deleted = 0 LIMIT 1) m
WHERE NOT EXISTS (SELECT 1 FROM auth_resource WHERE resource_code = '/api/v1/storage/url' AND deleted = 0);

INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, request_method, parent_id, sort_order)
SELECT 'button', 'storage:upload', '上传文件', 'storage', NULL, m.id, 5
FROM (SELECT id FROM auth_resource WHERE resource_code = '/storage' AND resource_type = 'menu' AND deleted = 0 LIMIT 1) m
WHERE NOT EXISTS (SELECT 1 FROM auth_resource WHERE resource_code = 'storage:upload' AND resource_type = 'button' AND deleted = 0);

INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, request_method, parent_id, sort_order)
SELECT 'button', 'storage:delete', '删除文件', 'storage', NULL, m.id, 6
FROM (SELECT id FROM auth_resource WHERE resource_code = '/storage' AND resource_type = 'menu' AND deleted = 0 LIMIT 1) m
WHERE NOT EXISTS (SELECT 1 FROM auth_resource WHERE resource_code = 'storage:delete' AND resource_type = 'button' AND deleted = 0);

-- 3. admin 角色绑定新增资源（若尚未绑定）
INSERT INTO auth_role_resource (role_id, resource_id)
SELECT r.id AS role_id, res.id AS resource_id
FROM auth_role r
CROSS JOIN auth_resource res
WHERE r.role_code = 'admin' AND r.deleted = 0
  AND res.module = 'storage' AND res.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM auth_role_resource WHERE role_id = r.id AND resource_id = res.id);
