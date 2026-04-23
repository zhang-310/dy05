-- ============================================================
-- live 模块 - 资源初始数据
-- 依赖：schema.sql（live_session / live_session_product / live_session_script / live_monitor_data）
-- 数据库：PostgreSQL
-- 说明：插入菜单资源、API 资源、按钮资源，以及角色-资源绑定
-- 幂等：使用 ON CONFLICT DO NOTHING，可重复执行
-- ============================================================

-- ============================================================
-- 1. 菜单资源（resource_type = 'menu'）
-- ============================================================
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, request_method, parent_id, sort_order)
VALUES
    ('menu', '/live',          '直播管理',   'live', NULL, 0, 800),
    ('menu', '/live/sessions', '直播场次',   'live', NULL, 0, 1),
    ('menu', '/live/detail',   '直播详情',   'live', NULL, 0, 2),
    ('menu', '/live/monitor',  '直播监控',   'live', NULL, 0, 3)
ON CONFLICT DO NOTHING;

-- 更新子菜单的 parent_id 指向「直播管理」
UPDATE auth_resource
SET    parent_id = (SELECT id FROM auth_resource WHERE resource_code = '/live' AND resource_type = 'menu' AND deleted = 0 LIMIT 1)
WHERE  resource_code IN ('/live/sessions', '/live/detail', '/live/monitor')
  AND  resource_type = 'menu'
  AND  deleted = 0
  AND  parent_id = 0;

-- ============================================================
-- 2. API 资源（resource_type = 'api'）
-- ============================================================

-- ---------- LiveSession ----------
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, request_method, parent_id, sort_order)
VALUES
    ('api', '/api/v1/live/session/search',  '查询直播场次',    'live', 'POST', 0, 0),
    ('api', '/api/v1/live/session/get',     '获取直播详情',    'live', 'POST', 0, 0),
    ('api', '/api/v1/live/session/save',    '新增/编辑直播',   'live', 'POST', 0, 0),
    ('api', '/api/v1/live/session/delete',  '删除直播',        'live', 'POST', 0, 0),
    ('api', '/api/v1/live/session/start',   '开始直播',        'live', 'POST', 0, 0),
    ('api', '/api/v1/live/session/end',     '结束直播',        'live', 'POST', 0, 0),
    ('api', '/api/v1/live/session/cancel',  '取消直播',        'live', 'POST', 0, 0),
    ('api', '/api/v1/live/session/stats',   '获取统计数据',    'live', 'POST', 0, 0)
ON CONFLICT DO NOTHING;

-- ---------- LiveDetail (产品、话术) ----------
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, request_method, parent_id, sort_order)
VALUES
    ('api', '/api/v1/live/detail/products/add',           '添加直播产品',      'live', 'POST', 0, 0),
    ('api', '/api/v1/live/detail/products/remove',        '移除直播产品',      'live', 'POST', 0, 0),
    ('api', '/api/v1/live/detail/products/list',          '查询直播产品',      'live', 'POST', 0, 0),
    ('api', '/api/v1/live/detail/products/update-sales',  '更新产品销售数据',  'live', 'POST', 0, 0),
    ('api', '/api/v1/live/detail/scripts/list',           '查询直播话术',      'live', 'POST', 0, 0),
    ('api', '/api/v1/live/detail/scripts/save',           '保存直播话术',      'live', 'POST', 0, 0),
    ('api', '/api/v1/live/detail/scripts/delete',         '删除直播话术',      'live', 'POST', 0, 0),
    ('api', '/api/v1/live/detail/scripts/mark-executed',  '标记话术已执行',    'live', 'POST', 0, 0),
    ('api', '/api/v1/live/detail/scripts/pending',        '获取待执行话术',    'live', 'POST', 0, 0)
ON CONFLICT DO NOTHING;

-- ---------- LiveMonitor ----------
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, request_method, parent_id, sort_order)
VALUES
    ('api', '/api/v1/live/monitor/search',      '查询监控数据',      'live', 'POST', 0, 0),
    ('api', '/api/v1/live/monitor/latest',      '获取最新监控数据',  'live', 'POST', 0, 0),
    ('api', '/api/v1/live/monitor/chart-data',  '获取监控图表数据',  'live', 'POST', 0, 0),
    ('api', '/api/v1/live/monitor/record',      '记录监控数据',      'live', 'POST', 0, 0)
ON CONFLICT DO NOTHING;

-- ============================================================
-- 3. 按钮资源（resource_type = 'button'）
-- ============================================================
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, request_method, parent_id, sort_order)
VALUES
    ('button', 'live:session:add',       '新增直播',    'live', NULL, 0, 0),
    ('button', 'live:session:edit',      '编辑直播',    'live', NULL, 0, 0),
    ('button', 'live:session:delete',    '删除直播',    'live', NULL, 0, 0),
    ('button', 'live:session:start',     '开始直播',    'live', NULL, 0, 0),
    ('button', 'live:session:end',       '结束直播',    'live', NULL, 0, 0),
    ('button', 'live:session:cancel',    '取消直播',    'live', NULL, 0, 0),
    ('button', 'live:product:add',       '添加产品',    'live', NULL, 0, 0),
    ('button', 'live:product:remove',    '移除产品',    'live', NULL, 0, 0),
    ('button', 'live:script:add',        '添加话术',    'live', NULL, 0, 0),
    ('button', 'live:script:edit',       '编辑话术',    'live', NULL, 0, 0),
    ('button', 'live:script:delete',     '删除话术',    'live', NULL, 0, 0)
ON CONFLICT DO NOTHING;

-- ============================================================
-- 4. 角色-资源绑定（admin 角色）
-- ============================================================
INSERT INTO auth_role_resource (role_id, resource_id)
SELECT
    (SELECT id FROM auth_role WHERE role_code = 'admin' AND deleted = 0 LIMIT 1),
    id
FROM auth_resource
WHERE module = 'live' AND deleted = 0
  AND id NOT IN (
      SELECT resource_id FROM auth_role_resource
      WHERE role_id = (SELECT id FROM auth_role WHERE role_code = 'admin' AND deleted = 0 LIMIT 1)
  )
ON CONFLICT DO NOTHING;
