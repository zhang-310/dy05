-- ============================================================
-- abtest 模块 - 资源初始数据
-- 依赖：schema.sql（ab_experiment / ab_variant / ab_event）
-- 数据库：PostgreSQL
-- 说明：插入菜单资源、API 资源，以及角色-资源绑定
-- 幂等：使用 ON CONFLICT DO NOTHING，可重复执行
-- ============================================================

-- ============================================================
-- 1. 菜单资源（resource_type = 'menu'）
-- ============================================================
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, request_method, parent_id, sort_order)
VALUES
    -- AB 测试管理（顶级菜单）
    ('menu', '/abtest',              'A/B 测试',      'abtest', NULL, 0, 500),
    -- AB 测试子菜单
    ('menu', '/abtest/experiments',  '实验管理',      'abtest', NULL, 0, 1),
    ('menu', '/abtest/events',       '事件记录',      'abtest', NULL, 0, 2)
ON CONFLICT DO NOTHING;

-- 更新子菜单的 parent_id 指向「A/B 测试」
UPDATE auth_resource
SET    parent_id = (SELECT id FROM auth_resource WHERE resource_code = '/abtest' AND resource_type = 'menu' AND deleted = 0 LIMIT 1)
WHERE  resource_code IN ('/abtest/experiments', '/abtest/events')
  AND  resource_type = 'menu'
  AND  deleted = 0
  AND  parent_id = 0;

-- ============================================================
-- 2. API 资源（resource_type = 'api'）
-- ============================================================

-- ---------- 实验 (ab_experiment) ----------
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, request_method, parent_id, sort_order)
VALUES
    ('api', '/api/v1/abtest/experiment/search',  '查询实验列表',      'abtest', 'POST', 0, 0),
    ('api', '/api/v1/abtest/experiment/get',     '获取实验详情',      'abtest', 'POST', 0, 0),
    ('api', '/api/v1/abtest/experiment/save',    '新增或更新实验',    'abtest', 'POST', 0, 0),
    ('api', '/api/v1/abtest/experiment/delete',  '删除实验',          'abtest', 'POST', 0, 0),
    ('api', '/api/v1/abtest/experiment/start',   '启动实验',          'abtest', 'POST', 0, 0),
    ('api', '/api/v1/abtest/experiment/stop',    '停止实验',          'abtest', 'POST', 0, 0),
    ('api', '/api/v1/abtest/experiment/conclude','结束实验并设置结论', 'abtest', 'POST', 0, 0)
ON CONFLICT DO NOTHING;

-- ---------- 变体 (ab_variant) ----------
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, request_method, parent_id, sort_order)
VALUES
    ('api', '/api/v1/abtest/variant/search',     '查询变体列表',      'abtest', 'POST', 0, 0),
    ('api', '/api/v1/abtest/variant/get',        '获取变体详情',      'abtest', 'POST', 0, 0),
    ('api', '/api/v1/abtest/variant/save',       '新增或更新变体',    'abtest', 'POST', 0, 0),
    ('api', '/api/v1/abtest/variant/delete',     '删除变体',          'abtest', 'POST', 0, 0),
    ('api', '/api/v1/abtest/variant/set-winner', '设置获胜变体',      'abtest', 'POST', 0, 0)
ON CONFLICT DO NOTHING;

-- ---------- 事件 (ab_event) ----------
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, request_method, parent_id, sort_order)
VALUES
    ('api', '/api/v1/abtest/event/record',  '记录事件',        'abtest', 'POST', 0, 0),
    ('api', '/api/v1/abtest/event/search',  '查询事件列表',    'abtest', 'POST', 0, 0),
    ('api', '/api/v1/abtest/event/stats',   '事件统计',        'abtest', 'POST', 0, 0)
ON CONFLICT DO NOTHING;

-- ============================================================
-- 3. 按钮资源（resource_type = 'button'）
-- ============================================================
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, request_method, parent_id, sort_order)
VALUES
    ('button', 'abtest:experiment:add',      '新增实验',        'abtest', NULL, 0, 0),
    ('button', 'abtest:experiment:edit',     '编辑实验',        'abtest', NULL, 0, 0),
    ('button', 'abtest:experiment:delete',   '删除实验',        'abtest', NULL, 0, 0),
    ('button', 'abtest:experiment:start',    '启动实验',        'abtest', NULL, 0, 0),
    ('button', 'abtest:experiment:stop',     '停止实验',        'abtest', NULL, 0, 0),
    ('button', 'abtest:variant:add',         '新增变体',        'abtest', NULL, 0, 0),
    ('button', 'abtest:variant:edit',        '编辑变体',        'abtest', NULL, 0, 0),
    ('button', 'abtest:variant:delete',      '删除变体',        'abtest', NULL, 0, 0),
    ('button', 'abtest:variant:winner',      '设置获胜变体',    'abtest', NULL, 0, 0),
    ('button', 'abtest:event:view',          '查看事件记录',    'abtest', NULL, 0, 0)
ON CONFLICT DO NOTHING;

-- ============================================================
-- 4. 角色-资源绑定（admin 角色）
-- ============================================================

-- 菜单
INSERT INTO auth_role_resource (role_id, resource_id)
SELECT
    (SELECT id FROM auth_role WHERE role_code = 'admin' AND deleted = 0 LIMIT 1),
    id
FROM auth_resource
WHERE module = 'abtest' AND resource_type = 'menu' AND deleted = 0
  AND id NOT IN (
      SELECT resource_id FROM auth_role_resource
      WHERE role_id = (SELECT id FROM auth_role WHERE role_code = 'admin' AND deleted = 0 LIMIT 1)
  )
ON CONFLICT DO NOTHING;

-- API
INSERT INTO auth_role_resource (role_id, resource_id)
SELECT
    (SELECT id FROM auth_role WHERE role_code = 'admin' AND deleted = 0 LIMIT 1),
    id
FROM auth_resource
WHERE module = 'abtest' AND resource_type = 'api' AND deleted = 0
  AND id NOT IN (
      SELECT resource_id FROM auth_role_resource
      WHERE role_id = (SELECT id FROM auth_role WHERE role_code = 'admin' AND deleted = 0 LIMIT 1)
  )
ON CONFLICT DO NOTHING;

-- 按钮
INSERT INTO auth_role_resource (role_id, resource_id)
SELECT
    (SELECT id FROM auth_role WHERE role_code = 'admin' AND deleted = 0 LIMIT 1),
    id
FROM auth_resource
WHERE module = 'abtest' AND resource_type = 'button' AND deleted = 0
  AND id NOT IN (
      SELECT resource_id FROM auth_role_resource
      WHERE role_id = (SELECT id FROM auth_role WHERE role_code = 'admin' AND deleted = 0 LIMIT 1)
  )
ON CONFLICT DO NOTHING;
