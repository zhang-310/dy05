-- ============================================================
-- 企业微信模块 - 资源初始数据
-- 依赖：schema.sql（wc_robot_config / wc_push_rule / wc_message_log）
-- 数据库：PostgreSQL
-- 说明：插入菜单资源、API 资源，以及角色-资源绑定
-- 幂等：使用 ON CONFLICT DO NOTHING，可重复执行
-- ============================================================

-- ============================================================
-- 1. 菜单资源（resource_type = 'menu'）
-- ============================================================
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, request_method, parent_id, sort_order)
VALUES
    -- 企业微信（顶级菜单）
    ('menu', '/wecom',          '企业微信',      'wecom', NULL, 0, 900),
    -- 子菜单
    ('menu', '/wecom/robots',   '机器人管理',    'wecom', NULL, 0, 1),
    ('menu', '/wecom/rules',    '推送规则',      'wecom', NULL, 0, 2),
    ('menu', '/wecom/logs',     '消息日志',      'wecom', NULL, 0, 3)
ON CONFLICT DO NOTHING;

-- 更新子菜单的 parent_id 指向「企业微信」
UPDATE auth_resource
SET    parent_id = (SELECT id FROM auth_resource WHERE resource_code = '/wecom' AND resource_type = 'menu' AND deleted = 0 LIMIT 1)
WHERE  resource_code IN ('/wecom/robots', '/wecom/rules', '/wecom/logs')
  AND  resource_type = 'menu'
  AND  deleted = 0
  AND  parent_id = 0;

-- ============================================================
-- 2. API 资源（resource_type = 'api'）
-- ============================================================

-- ---------- 机器人 (wc_robot_config) ----------
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, request_method, parent_id, sort_order)
VALUES
    ('api', '/api/v1/wecom/robot/search',  '查询机器人列表',    'wecom', 'POST', 0, 0),
    ('api', '/api/v1/wecom/robot/get',     '获取机器人详情',    'wecom', 'POST', 0, 0),
    ('api', '/api/v1/wecom/robot/save',    '新增或更新机器人',  'wecom', 'POST', 0, 0),
    ('api', '/api/v1/wecom/robot/delete',  '删除机器人',        'wecom', 'POST', 0, 0),
    ('api', '/api/v1/wecom/robot/toggle',  '启用/禁用机器人',   'wecom', 'POST', 0, 0)
ON CONFLICT DO NOTHING;

-- ---------- 推送规则 (wc_push_rule) ----------
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, request_method, parent_id, sort_order)
VALUES
    ('api', '/api/v1/wecom/rule/search',   '查询推送规则列表',  'wecom', 'POST', 0, 0),
    ('api', '/api/v1/wecom/rule/get',      '获取推送规则详情',  'wecom', 'POST', 0, 0),
    ('api', '/api/v1/wecom/rule/save',     '新增或更新推送规则','wecom', 'POST', 0, 0),
    ('api', '/api/v1/wecom/rule/delete',   '删除推送规则',      'wecom', 'POST', 0, 0),
    ('api', '/api/v1/wecom/rule/toggle',   '启用/禁用推送规则', 'wecom', 'POST', 0, 0)
ON CONFLICT DO NOTHING;

-- ---------- 消息日志 (wc_message_log) ----------
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, request_method, parent_id, sort_order)
VALUES
    ('api', '/api/v1/wecom/log/search',    '查询消息日志列表',  'wecom', 'POST', 0, 0),
    ('api', '/api/v1/wecom/log/get',       '获取消息日志详情',  'wecom', 'POST', 0, 0),
    ('api', '/api/v1/wecom/message/send',  '手动发送消息',      'wecom', 'POST', 0, 0)
ON CONFLICT DO NOTHING;

-- ============================================================
-- 3. 按钮资源（resource_type = 'button'）
-- ============================================================
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, request_method, parent_id, sort_order)
VALUES
    ('button', 'wecom:robot:add',      '新增机器人',      'wecom', NULL, 0, 0),
    ('button', 'wecom:robot:edit',     '编辑机器人',      'wecom', NULL, 0, 0),
    ('button', 'wecom:robot:delete',   '删除机器人',      'wecom', NULL, 0, 0),
    ('button', 'wecom:robot:toggle',   '启用/禁用机器人', 'wecom', NULL, 0, 0),
    ('button', 'wecom:rule:add',       '新增推送规则',    'wecom', NULL, 0, 0),
    ('button', 'wecom:rule:edit',      '编辑推送规则',    'wecom', NULL, 0, 0),
    ('button', 'wecom:rule:delete',    '删除推送规则',    'wecom', NULL, 0, 0),
    ('button', 'wecom:rule:toggle',    '启用/禁用规则',   'wecom', NULL, 0, 0),
    ('button', 'wecom:message:send',   '手动发送消息',    'wecom', NULL, 0, 0),
    ('button', 'wecom:log:view',       '查看消息日志',    'wecom', NULL, 0, 0)
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
WHERE module = 'wecom' AND resource_type = 'menu' AND deleted = 0
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
WHERE module = 'wecom' AND resource_type = 'api' AND deleted = 0
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
WHERE module = 'wecom' AND resource_type = 'button' AND deleted = 0
  AND id NOT IN (
      SELECT resource_id FROM auth_role_resource
      WHERE role_id = (SELECT id FROM auth_role WHERE role_code = 'admin' AND deleted = 0 LIMIT 1)
  )
ON CONFLICT DO NOTHING;
