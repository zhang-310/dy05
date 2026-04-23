-- ============================================================
-- copy 模块 - 资源数据和菜单配置
-- 模块：文案库管理（Copy Management）
-- 数据库：PostgreSQL
-- 版本：1.0
-- 更新日期：2026-02-25
-- 说明：包含菜单资源、API资源、按钮资源
-- ============================================================

-- ============================================================
-- 1. 菜单资源
-- ============================================================

-- 文案管理菜单（一级菜单）
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, parent_id, sort_order)
VALUES ('menu', '/copy', '文案管理', 'copy', 0, 60)
ON CONFLICT DO NOTHING;

-- 获取一级菜单的 ID 用于二级菜单
-- 文案库管理（二级菜单）
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, parent_id, sort_order)
SELECT 'menu', '/copy/library', '文案库管理', 'copy', id, 1
FROM auth_resource
WHERE resource_code = '/copy' AND deleted = 0 AND parent_id = 0
ON CONFLICT DO NOTHING;

-- 文案审核（二级菜单）
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, parent_id, sort_order)
SELECT 'menu', '/copy/approval', '文案审核', 'copy', id, 2
FROM auth_resource
WHERE resource_code = '/copy' AND deleted = 0 AND parent_id = 0
ON CONFLICT DO NOTHING;

-- 文案模板（二级菜单）
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, parent_id, sort_order)
SELECT 'menu', '/copy/templates', '文案模板', 'copy', id, 3
FROM auth_resource
WHERE resource_code = '/copy' AND deleted = 0 AND parent_id = 0
ON CONFLICT DO NOTHING;

-- ============================================================
-- 2. API 资源 - 文案库管理
-- ============================================================

INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, request_method)
VALUES ('api', '/api/v1/copy/library/search', '文案库查询', 'copy', 'POST')
ON CONFLICT DO NOTHING;

INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, request_method)
VALUES ('api', '/api/v1/copy/library/get', '文案库详情', 'copy', 'POST')
ON CONFLICT DO NOTHING;

INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, request_method)
VALUES ('api', '/api/v1/copy/library/save', '文案库保存', 'copy', 'POST')
ON CONFLICT DO NOTHING;

INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, request_method)
VALUES ('api', '/api/v1/copy/library/delete', '文案库删除', 'copy', 'POST')
ON CONFLICT DO NOTHING;

INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, request_method)
VALUES ('api', '/api/v1/copy/library/increase-use-count', '增加使用次数', 'copy', 'POST')
ON CONFLICT DO NOTHING;

-- ============================================================
-- 3. API 资源 - 文案审核
-- ============================================================

INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, request_method)
VALUES ('api', '/api/v1/copy/approval/search', '审核查询', 'copy', 'POST')
ON CONFLICT DO NOTHING;

INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, request_method)
VALUES ('api', '/api/v1/copy/approval/get', '审核详情', 'copy', 'POST')
ON CONFLICT DO NOTHING;

INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, request_method)
VALUES ('api', '/api/v1/copy/approval/approve', '审核操作', 'copy', 'POST')
ON CONFLICT DO NOTHING;

INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, request_method)
VALUES ('api', '/api/v1/copy/approval/history', '审核历史查询', 'copy', 'POST')
ON CONFLICT DO NOTHING;

-- ============================================================
-- 4. API 资源 - 文案模板
-- ============================================================

INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, request_method)
VALUES ('api', '/api/v1/copy/template/search', '模板查询', 'copy', 'POST')
ON CONFLICT DO NOTHING;

INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, request_method)
VALUES ('api', '/api/v1/copy/template/get', '模板详情', 'copy', 'POST')
ON CONFLICT DO NOTHING;

INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, request_method)
VALUES ('api', '/api/v1/copy/template/save', '模板保存', 'copy', 'POST')
ON CONFLICT DO NOTHING;

INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, request_method)
VALUES ('api', '/api/v1/copy/template/delete', '模板删除', 'copy', 'POST')
ON CONFLICT DO NOTHING;

INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, request_method)
VALUES ('api', '/api/v1/copy/template/generate', '模板生成', 'copy', 'POST')
ON CONFLICT DO NOTHING;

-- ============================================================
-- 5. 按钮资源 - 文案库
-- ============================================================

INSERT INTO auth_resource (resource_type, resource_code, resource_name, module)
SELECT 'button', 'copy:library:add', '新增文案', 'copy'
WHERE NOT EXISTS (SELECT 1 FROM auth_resource WHERE resource_code = 'copy:library:add' AND deleted = 0)
ON CONFLICT DO NOTHING;

INSERT INTO auth_resource (resource_type, resource_code, resource_name, module)
SELECT 'button', 'copy:library:edit', '编辑文案', 'copy'
WHERE NOT EXISTS (SELECT 1 FROM auth_resource WHERE resource_code = 'copy:library:edit' AND deleted = 0)
ON CONFLICT DO NOTHING;

INSERT INTO auth_resource (resource_type, resource_code, resource_name, module)
SELECT 'button', 'copy:library:delete', '删除文案', 'copy'
WHERE NOT EXISTS (SELECT 1 FROM auth_resource WHERE resource_code = 'copy:library:delete' AND deleted = 0)
ON CONFLICT DO NOTHING;

INSERT INTO auth_resource (resource_type, resource_code, resource_name, module)
SELECT 'button', 'copy:library:view', '查看文案', 'copy'
WHERE NOT EXISTS (SELECT 1 FROM auth_resource WHERE resource_code = 'copy:library:view' AND deleted = 0)
ON CONFLICT DO NOTHING;

-- ============================================================
-- 6. 按钮资源 - 文案审核
-- ============================================================

INSERT INTO auth_resource (resource_type, resource_code, resource_name, module)
SELECT 'button', 'copy:approval:approve', '通过审核', 'copy'
WHERE NOT EXISTS (SELECT 1 FROM auth_resource WHERE resource_code = 'copy:approval:approve' AND deleted = 0)
ON CONFLICT DO NOTHING;

INSERT INTO auth_resource (resource_type, resource_code, resource_name, module)
SELECT 'button', 'copy:approval:reject', '拒绝审核', 'copy'
WHERE NOT EXISTS (SELECT 1 FROM auth_resource WHERE resource_code = 'copy:approval:reject' AND deleted = 0)
ON CONFLICT DO NOTHING;

INSERT INTO auth_resource (resource_type, resource_code, resource_name, module)
SELECT 'button', 'copy:approval:view', '查看审核历史', 'copy'
WHERE NOT EXISTS (SELECT 1 FROM auth_resource WHERE resource_code = 'copy:approval:view' AND deleted = 0)
ON CONFLICT DO NOTHING;

-- ============================================================
-- 7. 按钮资源 - 文案模板
-- ============================================================

INSERT INTO auth_resource (resource_type, resource_code, resource_name, module)
SELECT 'button', 'copy:template:add', '新增模板', 'copy'
WHERE NOT EXISTS (SELECT 1 FROM auth_resource WHERE resource_code = 'copy:template:add' AND deleted = 0)
ON CONFLICT DO NOTHING;

INSERT INTO auth_resource (resource_type, resource_code, resource_name, module)
SELECT 'button', 'copy:template:edit', '编辑模板', 'copy'
WHERE NOT EXISTS (SELECT 1 FROM auth_resource WHERE resource_code = 'copy:template:edit' AND deleted = 0)
ON CONFLICT DO NOTHING;

INSERT INTO auth_resource (resource_type, resource_code, resource_name, module)
SELECT 'button', 'copy:template:delete', '删除模板', 'copy'
WHERE NOT EXISTS (SELECT 1 FROM auth_resource WHERE resource_code = 'copy:template:delete' AND deleted = 0)
ON CONFLICT DO NOTHING;

INSERT INTO auth_resource (resource_type, resource_code, resource_name, module)
SELECT 'button', 'copy:template:generate', '生成测试', 'copy'
WHERE NOT EXISTS (SELECT 1 FROM auth_resource WHERE resource_code = 'copy:template:generate' AND deleted = 0)
ON CONFLICT DO NOTHING;

-- ============================================================
-- 8. admin 角色绑定所有 copy 模块资源
-- 说明：admin 角色自动获得 copy 模块的所有权限
-- ============================================================

-- 获取 admin 角色 ID 和所有 copy 模块资源 ID，建立关联
INSERT INTO auth_role_resource (role_id, resource_id)
SELECT ar.id AS role_id, res.id AS resource_id
FROM auth_role ar, auth_resource res
WHERE ar.role_code = 'admin'
  AND ar.deleted = 0
  AND res.module = 'copy'
  AND res.deleted = 0
  AND NOT EXISTS (
    SELECT 1 FROM auth_role_resource
    WHERE role_id = ar.id AND resource_id = res.id
  )
ON CONFLICT DO NOTHING;
