-- ============================================================
-- 统一权限资源树 - 完整重构
-- 匹配 AdminLayout.vue 侧边菜单 + 后端 API
-- 执行：在 schema.sql、auth_role 初始数据之后执行
-- 替代各模块分散的 resource-data.sql，避免重复与冲突
-- ============================================================

BEGIN;

-- 清空旧数据（按依赖顺序）
DELETE FROM auth_role_resource;
DELETE FROM auth_resource;

-- 重置序列
ALTER SEQUENCE auth_resource_id_seq RESTART WITH 1;
ALTER SEQUENCE auth_role_resource_id_seq RESTART WITH 1;

-- ============================================================
-- 1. 顶级菜单 (parent_id = 0)
-- ============================================================
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, parent_id, sort_order) VALUES
  ('menu', '/dashboard',   '工作台',     'system',  0, 1),
  ('menu', '/auth',        '系统管理',   'auth',    0, 10),
  ('menu', '/log',         '日志管理',   'log',     0, 20),
  ('menu', '/config',      '配置管理',   'config',  0, 30),
  ('menu', '/storage',     '存储管理',   'storage', 0, 35),
  ('menu', '/douyin',      '抖音运营',   'douyin',  0, 40),
  ('menu', '/copy',        '文案管理',   'copy',    0, 50),
  ('menu', '/shortvideo',  '短视频管理', 'shortvideo', 0, 60),
  ('menu', '/live',        '直播运维',   'live',    0, 70),
  ('menu', '/script',      '话术管理',   'script',  0, 55),
  ('menu', '/product',     '商品管理',   'product', 0, 80),
  ('menu', '/abtest',      'A/B 测试',   'abtest',  0, 90),
  ('menu', '/ai',          'AI 管理',    'ai',      0, 95);

-- 系统管理子菜单
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, parent_id, sort_order)
SELECT 'menu', '/auth/users',      '用户管理',   'auth', id, 1 FROM auth_resource WHERE resource_code = '/auth'   AND resource_type = 'menu' AND deleted = 0 LIMIT 1;
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, parent_id, sort_order)
SELECT 'menu', '/auth/roles',      '角色管理',   'auth', id, 2 FROM auth_resource WHERE resource_code = '/auth'   AND resource_type = 'menu' AND deleted = 0 LIMIT 1;
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, parent_id, sort_order)
SELECT 'menu', '/auth/resources',  '资源管理',   'auth', id, 3 FROM auth_resource WHERE resource_code = '/auth'   AND resource_type = 'menu' AND deleted = 0 LIMIT 1;
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, parent_id, sort_order)
SELECT 'menu', '/auth/login-logs', '登录日志',   'auth', id, 4 FROM auth_resource WHERE resource_code = '/auth'   AND resource_type = 'menu' AND deleted = 0 LIMIT 1;

-- 日志管理子菜单
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, parent_id, sort_order)
SELECT 'menu', '/log/operation',   '操作日志',   'log', id, 1 FROM auth_resource WHERE resource_code = '/log'    AND resource_type = 'menu' AND deleted = 0 LIMIT 1;
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, parent_id, sort_order)
SELECT 'menu', '/log/system',      '系统日志',   'log', id, 2 FROM auth_resource WHERE resource_code = '/log'    AND resource_type = 'menu' AND deleted = 0 LIMIT 1;

-- 抖音运营子菜单
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, parent_id, sort_order)
SELECT 'menu', '/douyin/accounts', '账号管理',   'douyin', id, 1 FROM auth_resource WHERE resource_code = '/douyin' AND resource_type = 'menu' AND deleted = 0 LIMIT 1;
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, parent_id, sort_order)
SELECT 'menu', '/douyin/videos',   '视频分析',   'douyin', id, 2 FROM auth_resource WHERE resource_code = '/douyin' AND resource_type = 'menu' AND deleted = 0 LIMIT 1;

-- 文案管理子菜单
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, parent_id, sort_order)
SELECT 'menu', '/copy/library',    '文案库',     'copy', id, 1 FROM auth_resource WHERE resource_code = '/copy'    AND resource_type = 'menu' AND deleted = 0 LIMIT 1;
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, parent_id, sort_order)
SELECT 'menu', '/copy/approval',   '文案审批',   'copy', id, 2 FROM auth_resource WHERE resource_code = '/copy'    AND resource_type = 'menu' AND deleted = 0 LIMIT 1;
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, parent_id, sort_order)
SELECT 'menu', '/copy/template',   '文案模板',   'copy', id, 3 FROM auth_resource WHERE resource_code = '/copy'    AND resource_type = 'menu' AND deleted = 0 LIMIT 1;

-- 短视频管理子菜单 - 匹配 AdminLayout
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, parent_id, sort_order)
SELECT 'menu', '/shortvideo/script-creation/persona',  '人设创作',   'shortvideo', id, 1 FROM auth_resource WHERE resource_code = '/shortvideo' AND resource_type = 'menu' AND deleted = 0 LIMIT 1;
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, parent_id, sort_order)
SELECT 'menu', '/shortvideo/script-creation/viral',   '爆款复刻',   'shortvideo', id, 2 FROM auth_resource WHERE resource_code = '/shortvideo' AND resource_type = 'menu' AND deleted = 0 LIMIT 1;
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, parent_id, sort_order)
SELECT 'menu', '/shortvideo/script-creation/trending','热门创作',   'shortvideo', id, 3 FROM auth_resource WHERE resource_code = '/shortvideo' AND resource_type = 'menu' AND deleted = 0 LIMIT 1;
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, parent_id, sort_order)
SELECT 'menu', '/shortvideo/auto-video',     '自动成片',   'shortvideo', id, 4 FROM auth_resource WHERE resource_code = '/shortvideo' AND resource_type = 'menu' AND deleted = 0 LIMIT 1;
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, parent_id, sort_order)
SELECT 'menu', '/shortvideo/daily-task',     '每日任务',   'shortvideo', id, 5 FROM auth_resource WHERE resource_code = '/shortvideo' AND resource_type = 'menu' AND deleted = 0 LIMIT 1;
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, parent_id, sort_order)
SELECT 'menu', '/shortvideo/creation-library','创作库',    'shortvideo', id, 6 FROM auth_resource WHERE resource_code = '/shortvideo' AND resource_type = 'menu' AND deleted = 0 LIMIT 1;

-- 直播运维子菜单
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, parent_id, sort_order)
SELECT 'menu', '/live/sessions',   '直播场次',   'live', id, 1 FROM auth_resource WHERE resource_code = '/live'    AND resource_type = 'menu' AND deleted = 0 LIMIT 1;
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, parent_id, sort_order)
SELECT 'menu', '/live/products',   '直播选品',   'live', id, 2 FROM auth_resource WHERE resource_code = '/live'    AND resource_type = 'menu' AND deleted = 0 LIMIT 1;
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, parent_id, sort_order)
SELECT 'menu', '/live/scripts',    '直播脚本',   'live', id, 3 FROM auth_resource WHERE resource_code = '/live'    AND resource_type = 'menu' AND deleted = 0 LIMIT 1;
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, parent_id, sort_order)
SELECT 'menu', '/script/list',     '话术库',     'script', id, 4 FROM auth_resource WHERE resource_code = '/live'   AND resource_type = 'menu' AND deleted = 0 LIMIT 1;
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, parent_id, sort_order)
SELECT 'menu', '/script/violation','违禁词库',   'script', id, 5 FROM auth_resource WHERE resource_code = '/live'   AND resource_type = 'menu' AND deleted = 0 LIMIT 1;

-- 商品管理子菜单
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, parent_id, sort_order)
SELECT 'menu', '/product/list',         '商品库',    'product', id, 1 FROM auth_resource WHERE resource_code = '/product' AND resource_type = 'menu' AND deleted = 0 LIMIT 1;
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, parent_id, sort_order)
SELECT 'menu', '/product/sales-history','销售历史',  'product', id, 2 FROM auth_resource WHERE resource_code = '/product' AND resource_type = 'menu' AND deleted = 0 LIMIT 1;

-- AI 管理子菜单 - 完整匹配 AdminLayout
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, parent_id, sort_order)
SELECT 'menu', '/ai/knowledge',  '知识库管理',  'ai', id, 1 FROM auth_resource WHERE resource_code = '/ai' AND resource_type = 'menu' AND deleted = 0 LIMIT 1;
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, parent_id, sort_order)
SELECT 'menu', '/ai/model',     'AI 模型管理',  'ai', id, 2 FROM auth_resource WHERE resource_code = '/ai' AND resource_type = 'menu' AND deleted = 0 LIMIT 1;
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, parent_id, sort_order)
SELECT 'menu', '/ai/prompt',    'Prompt 模板',  'ai', id, 3 FROM auth_resource WHERE resource_code = '/ai' AND resource_type = 'menu' AND deleted = 0 LIMIT 1;
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, parent_id, sort_order)
SELECT 'menu', '/ai/task',      'AI 生成任务',  'ai', id, 4 FROM auth_resource WHERE resource_code = '/ai' AND resource_type = 'menu' AND deleted = 0 LIMIT 1;
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, parent_id, sort_order)
SELECT 'menu', '/ai/media',     '多媒体工作台','ai', id, 5 FROM auth_resource WHERE resource_code = '/ai' AND resource_type = 'menu' AND deleted = 0 LIMIT 1;
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, parent_id, sort_order)
SELECT 'menu', '/ai/evolution', 'AI 进化引擎',  'ai', id, 6 FROM auth_resource WHERE resource_code = '/ai' AND resource_type = 'menu' AND deleted = 0 LIMIT 1;

-- ============================================================
-- 2. API/Button 资源（parent_id 指向对应顶级菜单，以便资源管理页点击时右侧显示）
-- ============================================================

-- Auth API（parent = 系统管理 /auth）
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, request_method, parent_id, sort_order)
SELECT 'api', '/api/v1/auth/login',                   '登录',           'auth', 'POST', id, 0 FROM auth_resource WHERE resource_code = '/auth' AND resource_type = 'menu' AND deleted = 0 LIMIT 1;
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, request_method, parent_id, sort_order)
SELECT 'api', '/api/v1/auth/logout',                  '登出',           'auth', 'POST', id, 0 FROM auth_resource WHERE resource_code = '/auth' AND resource_type = 'menu' AND deleted = 0 LIMIT 1;
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, request_method, parent_id, sort_order)
SELECT 'api', '/api/v1/auth/captcha',                 '获取验证码',     'auth', 'GET',  id, 0 FROM auth_resource WHERE resource_code = '/auth' AND resource_type = 'menu' AND deleted = 0 LIMIT 1;
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, request_method, parent_id, sort_order)
SELECT 'api', '/api/v1/auth/sms/send',                '发送验证码',     'auth', 'POST', id, 0 FROM auth_resource WHERE resource_code = '/auth' AND resource_type = 'menu' AND deleted = 0 LIMIT 1;
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, request_method, parent_id, sort_order)
SELECT 'api', '/api/v1/auth/forgot-password',         '忘记密码',       'auth', 'POST', id, 0 FROM auth_resource WHERE resource_code = '/auth' AND resource_type = 'menu' AND deleted = 0 LIMIT 1;
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, request_method, parent_id, sort_order)
SELECT 'api', '/api/v1/auth/profile',                 '获取个人信息',   'auth', 'POST', id, 0 FROM auth_resource WHERE resource_code = '/auth' AND resource_type = 'menu' AND deleted = 0 LIMIT 1;
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, request_method, parent_id, sort_order)
SELECT 'api', '/api/v1/auth/profile/update',          '更新个人信息',   'auth', 'POST', id, 0 FROM auth_resource WHERE resource_code = '/auth' AND resource_type = 'menu' AND deleted = 0 LIMIT 1;
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, request_method, parent_id, sort_order)
SELECT 'api', '/api/v1/auth/profile/change-password', '修改密码',       'auth', 'POST', id, 0 FROM auth_resource WHERE resource_code = '/auth' AND resource_type = 'menu' AND deleted = 0 LIMIT 1;
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, request_method, parent_id, sort_order)
SELECT 'api', '/api/v1/auth/menu/search',             '菜单列表',       'auth', 'POST', id, 0 FROM auth_resource WHERE resource_code = '/auth' AND resource_type = 'menu' AND deleted = 0 LIMIT 1;
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, request_method, parent_id, sort_order)
SELECT 'api', '/api/v1/auth/resource/search',         '资源编码',       'auth', 'POST', id, 0 FROM auth_resource WHERE resource_code = '/auth' AND resource_type = 'menu' AND deleted = 0 LIMIT 1;
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, request_method, parent_id, sort_order)
SELECT 'api', '/api/v1/auth/user/search',             '用户搜索',       'auth', 'POST', id, 0 FROM auth_resource WHERE resource_code = '/auth' AND resource_type = 'menu' AND deleted = 0 LIMIT 1;
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, request_method, parent_id, sort_order)
SELECT 'api', '/api/v1/auth/user/get',               '用户详情',       'auth', 'POST', id, 0 FROM auth_resource WHERE resource_code = '/auth' AND resource_type = 'menu' AND deleted = 0 LIMIT 1;
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, request_method, parent_id, sort_order)
SELECT 'api', '/api/v1/auth/user/save',              '保存用户',       'auth', 'POST', id, 0 FROM auth_resource WHERE resource_code = '/auth' AND resource_type = 'menu' AND deleted = 0 LIMIT 1;
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, request_method, parent_id, sort_order)
SELECT 'api', '/api/v1/auth/user/ban',               '封禁用户',       'auth', 'POST', id, 0 FROM auth_resource WHERE resource_code = '/auth' AND resource_type = 'menu' AND deleted = 0 LIMIT 1;
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, request_method, parent_id, sort_order)
SELECT 'api', '/api/v1/auth/user/login-logs',         '用户登录日志',   'auth', 'POST', id, 0 FROM auth_resource WHERE resource_code = '/auth' AND resource_type = 'menu' AND deleted = 0 LIMIT 1;
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, request_method, parent_id, sort_order)
SELECT 'api', '/api/v1/auth/role/list',              '角色列表',       'auth', 'POST', id, 0 FROM auth_resource WHERE resource_code = '/auth' AND resource_type = 'menu' AND deleted = 0 LIMIT 1;
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, request_method, parent_id, sort_order)
SELECT 'api', '/api/v1/auth/role/search',            '角色搜索',       'auth', 'POST', id, 0 FROM auth_resource WHERE resource_code = '/auth' AND resource_type = 'menu' AND deleted = 0 LIMIT 1;
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, request_method, parent_id, sort_order)
SELECT 'api', '/api/v1/auth/role/get',               '角色详情',       'auth', 'POST', id, 0 FROM auth_resource WHERE resource_code = '/auth' AND resource_type = 'menu' AND deleted = 0 LIMIT 1;
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, request_method, parent_id, sort_order)
SELECT 'api', '/api/v1/auth/role/save',              '保存角色',       'auth', 'POST', id, 0 FROM auth_resource WHERE resource_code = '/auth' AND resource_type = 'menu' AND deleted = 0 LIMIT 1;
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, request_method, parent_id, sort_order)
SELECT 'api', '/api/v1/auth/role/delete',            '删除角色',       'auth', 'POST', id, 0 FROM auth_resource WHERE resource_code = '/auth' AND resource_type = 'menu' AND deleted = 0 LIMIT 1;
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, request_method, parent_id, sort_order)
SELECT 'api', '/api/v1/auth/role/resources',         '角色资源',       'auth', 'POST', id, 0 FROM auth_resource WHERE resource_code = '/auth' AND resource_type = 'menu' AND deleted = 0 LIMIT 1;
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, request_method, parent_id, sort_order)
SELECT 'api', '/api/v1/auth/role/resources/save',    '保存角色资源',   'auth', 'POST', id, 0 FROM auth_resource WHERE resource_code = '/auth' AND resource_type = 'menu' AND deleted = 0 LIMIT 1;
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, request_method, parent_id, sort_order)
SELECT 'api', '/api/v1/auth/resource/list',           '资源列表',       'auth', 'POST', id, 0 FROM auth_resource WHERE resource_code = '/auth' AND resource_type = 'menu' AND deleted = 0 LIMIT 1;
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, request_method, parent_id, sort_order)
SELECT 'api', '/api/v1/auth/resource/tree',           '资源树',         'auth', 'POST', id, 0 FROM auth_resource WHERE resource_code = '/auth' AND resource_type = 'menu' AND deleted = 0 LIMIT 1;
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, request_method, parent_id, sort_order)
SELECT 'api', '/api/v1/auth/resource/get',            '资源详情',       'auth', 'POST', id, 0 FROM auth_resource WHERE resource_code = '/auth' AND resource_type = 'menu' AND deleted = 0 LIMIT 1;
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, request_method, parent_id, sort_order)
SELECT 'api', '/api/v1/auth/resource/save',           '保存资源',       'auth', 'POST', id, 0 FROM auth_resource WHERE resource_code = '/auth' AND resource_type = 'menu' AND deleted = 0 LIMIT 1;
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, request_method, parent_id, sort_order)
SELECT 'api', '/api/v1/auth/resource/delete',         '删除资源',       'auth', 'POST', id, 0 FROM auth_resource WHERE resource_code = '/auth' AND resource_type = 'menu' AND deleted = 0 LIMIT 1;
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, request_method, parent_id, sort_order)
SELECT 'api', '/api/v1/auth/oauth/bindings',         '第三方绑定列表', 'auth', 'POST', id, 0 FROM auth_resource WHERE resource_code = '/auth' AND resource_type = 'menu' AND deleted = 0 LIMIT 1;
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, request_method, parent_id, sort_order)
SELECT 'api', '/api/v1/auth/oauth/bind',             '绑定第三方',     'auth', 'POST', id, 0 FROM auth_resource WHERE resource_code = '/auth' AND resource_type = 'menu' AND deleted = 0 LIMIT 1;
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, request_method, parent_id, sort_order)
SELECT 'api', '/api/v1/auth/oauth/unbind',           '解绑第三方',     'auth', 'POST', id, 0 FROM auth_resource WHERE resource_code = '/auth' AND resource_type = 'menu' AND deleted = 0 LIMIT 1;
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, request_method, parent_id, sort_order)
SELECT 'api', '/api/v1/dashboard/admin/stats',        '管理员统计',     'auth', 'GET',  id, 0 FROM auth_resource WHERE resource_code = '/auth' AND resource_type = 'menu' AND deleted = 0 LIMIT 1;
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, request_method, parent_id, sort_order)
SELECT 'api', '/api/v1/dashboard/org/stats',          '机构统计',       'auth', 'GET',  id, 0 FROM auth_resource WHERE resource_code = '/auth' AND resource_type = 'menu' AND deleted = 0 LIMIT 1;
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, request_method, parent_id, sort_order)
SELECT 'api', '/api/v1/dashboard/talent/stats',       '达人统计',       'auth', 'GET',  id, 0 FROM auth_resource WHERE resource_code = '/auth' AND resource_type = 'menu' AND deleted = 0 LIMIT 1;

-- Log API（parent = 日志管理 /log）
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, request_method, parent_id, sort_order)
SELECT 'api', '/api/v1/log/operation/page',  '操作日志列表', 'log', 'POST', id, 0 FROM auth_resource WHERE resource_code = '/log' AND resource_type = 'menu' AND deleted = 0 LIMIT 1;
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, request_method, parent_id, sort_order)
SELECT 'api', '/api/v1/log/system/page',     '系统日志列表', 'log', 'POST', id, 0 FROM auth_resource WHERE resource_code = '/log' AND resource_type = 'menu' AND deleted = 0 LIMIT 1;
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, request_method, parent_id, sort_order)
SELECT 'api', '/api/v1/log/operation/export', '导出操作日志', 'log', 'POST', id, 0 FROM auth_resource WHERE resource_code = '/log' AND resource_type = 'menu' AND deleted = 0 LIMIT 1;
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, request_method, parent_id, sort_order)
SELECT 'api', '/api/v1/log/system/export',    '导出系统日志', 'log', 'POST', id, 0 FROM auth_resource WHERE resource_code = '/log' AND resource_type = 'menu' AND deleted = 0 LIMIT 1;

-- Config API（parent = 配置管理 /config）
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, request_method, parent_id, sort_order)
SELECT 'api', '/api/v1/config/list',          '配置列表',   'config', 'POST', id, 0 FROM auth_resource WHERE resource_code = '/config' AND resource_type = 'menu' AND deleted = 0 LIMIT 1;
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, request_method, parent_id, sort_order)
SELECT 'api', '/api/v1/config/get',           '获取配置',   'config', 'GET',  id, 0 FROM auth_resource WHERE resource_code = '/config' AND resource_type = 'menu' AND deleted = 0 LIMIT 1;
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, request_method, parent_id, sort_order)
SELECT 'api', '/api/v1/config/save',          '保存配置',   'config', 'POST', id, 0 FROM auth_resource WHERE resource_code = '/config' AND resource_type = 'menu' AND deleted = 0 LIMIT 1;
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, request_method, parent_id, sort_order)
SELECT 'api', '/api/v1/config/delete',        '删除配置',   'config', 'POST', id, 0 FROM auth_resource WHERE resource_code = '/config' AND resource_type = 'menu' AND deleted = 0 LIMIT 1;

-- Storage API + Button（parent_id 指向存储管理菜单，以便资源管理页点击「存储管理」时右侧能显示）
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, request_method, parent_id, sort_order)
SELECT 'api', '/api/v1/storage/configured', '存储配置检查', 'storage', 'GET',  id, 0 FROM auth_resource WHERE resource_code = '/storage' AND resource_type = 'menu' AND deleted = 0 LIMIT 1;
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, request_method, parent_id, sort_order)
SELECT 'api', '/api/v1/storage/list',       '文件列表',    'storage', 'POST', id, 1 FROM auth_resource WHERE resource_code = '/storage' AND resource_type = 'menu' AND deleted = 0 LIMIT 1;
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, request_method, parent_id, sort_order)
SELECT 'api', '/api/v1/storage/upload',     '文件上传',    'storage', 'POST', id, 2 FROM auth_resource WHERE resource_code = '/storage' AND resource_type = 'menu' AND deleted = 0 LIMIT 1;
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, request_method, parent_id, sort_order)
SELECT 'api', '/api/v1/storage/delete',     '文件删除',    'storage', 'POST', id, 3 FROM auth_resource WHERE resource_code = '/storage' AND resource_type = 'menu' AND deleted = 0 LIMIT 1;
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, request_method, parent_id, sort_order)
SELECT 'api', '/api/v1/storage/url',        '文件URL',     'storage', 'GET',  id, 4 FROM auth_resource WHERE resource_code = '/storage' AND resource_type = 'menu' AND deleted = 0 LIMIT 1;
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, request_method, parent_id, sort_order)
SELECT 'button', 'storage:upload',          '上传文件',    'storage', NULL,   id, 5 FROM auth_resource WHERE resource_code = '/storage' AND resource_type = 'menu' AND deleted = 0 LIMIT 1;
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, request_method, parent_id, sort_order)
SELECT 'button', 'storage:delete',          '删除文件',    'storage', NULL,   id, 6 FROM auth_resource WHERE resource_code = '/storage' AND resource_type = 'menu' AND deleted = 0 LIMIT 1;

-- Douyin API（parent = 抖音运营 /douyin）
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, request_method, parent_id, sort_order)
SELECT 'api', '/api/v1/douyin/account/search',  '账号搜索',   'douyin', 'POST', id, 0 FROM auth_resource WHERE resource_code = '/douyin' AND resource_type = 'menu' AND deleted = 0 LIMIT 1;
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, request_method, parent_id, sort_order)
SELECT 'api', '/api/v1/douyin/account/get',     '账号详情',   'douyin', 'POST', id, 0 FROM auth_resource WHERE resource_code = '/douyin' AND resource_type = 'menu' AND deleted = 0 LIMIT 1;
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, request_method, parent_id, sort_order)
SELECT 'api', '/api/v1/douyin/account/save',    '保存账号',   'douyin', 'POST', id, 0 FROM auth_resource WHERE resource_code = '/douyin' AND resource_type = 'menu' AND deleted = 0 LIMIT 1;
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, request_method, parent_id, sort_order)
SELECT 'api', '/api/v1/douyin/video/search',    '视频搜索',   'douyin', 'POST', id, 0 FROM auth_resource WHERE resource_code = '/douyin' AND resource_type = 'menu' AND deleted = 0 LIMIT 1;
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, request_method, parent_id, sort_order)
SELECT 'api', '/api/v1/douyin/persona/list',    '人设列表',   'douyin', 'POST', id, 0 FROM auth_resource WHERE resource_code = '/douyin' AND resource_type = 'menu' AND deleted = 0 LIMIT 1;
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, request_method, parent_id, sort_order)
SELECT 'api', '/api/v1/douyin/persona/save',    '保存人设',   'douyin', 'POST', id, 0 FROM auth_resource WHERE resource_code = '/douyin' AND resource_type = 'menu' AND deleted = 0 LIMIT 1;

-- Copy API（parent = 文案管理 /copy）
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, request_method, parent_id, sort_order)
SELECT 'api', '/api/v1/copy/library/search',   '文案库搜索', 'copy', 'POST', id, 0 FROM auth_resource WHERE resource_code = '/copy' AND resource_type = 'menu' AND deleted = 0 LIMIT 1;
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, request_method, parent_id, sort_order)
SELECT 'api', '/api/v1/copy/template/list',    '模板列表',   'copy', 'POST', id, 0 FROM auth_resource WHERE resource_code = '/copy' AND resource_type = 'menu' AND deleted = 0 LIMIT 1;

-- Script API（parent = 话术管理 /script，script 菜单在 live 下，用 /live 作为 parent）
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, request_method, parent_id, sort_order)
SELECT 'api', '/api/v1/script/list',          '话术列表',   'script', 'POST', id, 0 FROM auth_resource WHERE resource_code = '/live' AND resource_type = 'menu' AND deleted = 0 LIMIT 1;
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, request_method, parent_id, sort_order)
SELECT 'api', '/api/v1/script/save',          '保存话术',   'script', 'POST', id, 0 FROM auth_resource WHERE resource_code = '/live' AND resource_type = 'menu' AND deleted = 0 LIMIT 1;
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, request_method, parent_id, sort_order)
SELECT 'api', '/api/v1/script/violation/list','违禁词列表','script', 'POST', id, 0 FROM auth_resource WHERE resource_code = '/live' AND resource_type = 'menu' AND deleted = 0 LIMIT 1;

-- Live API（parent = 直播运维 /live）
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, request_method, parent_id, sort_order)
SELECT 'api', '/api/v1/live/session/search',  '直播场次搜索', 'live', 'POST', id, 0 FROM auth_resource WHERE resource_code = '/live' AND resource_type = 'menu' AND deleted = 0 LIMIT 1;
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, request_method, parent_id, sort_order)
SELECT 'api', '/api/v1/live/session/save',    '保存直播',     'live', 'POST', id, 0 FROM auth_resource WHERE resource_code = '/live' AND resource_type = 'menu' AND deleted = 0 LIMIT 1;
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, request_method, parent_id, sort_order)
SELECT 'api', '/api/v1/live/product/list',    '直播选品列表', 'live', 'POST', id, 0 FROM auth_resource WHERE resource_code = '/live' AND resource_type = 'menu' AND deleted = 0 LIMIT 1;
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, request_method, parent_id, sort_order)
SELECT 'api', '/api/v1/live/script/list',     '直播脚本列表', 'live', 'POST', id, 0 FROM auth_resource WHERE resource_code = '/live' AND resource_type = 'menu' AND deleted = 0 LIMIT 1;

-- Product API（parent = 商品管理 /product）
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, request_method, parent_id, sort_order)
SELECT 'api', '/api/v1/product/search',       '商品搜索',   'product', 'POST', id, 0 FROM auth_resource WHERE resource_code = '/product' AND resource_type = 'menu' AND deleted = 0 LIMIT 1;
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, request_method, parent_id, sort_order)
SELECT 'api', '/api/v1/product/save',         '保存商品',   'product', 'POST', id, 0 FROM auth_resource WHERE resource_code = '/product' AND resource_type = 'menu' AND deleted = 0 LIMIT 1;
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, request_method, parent_id, sort_order)
SELECT 'api', '/api/v1/product/sales/history','销售历史',   'product', 'POST', id, 0 FROM auth_resource WHERE resource_code = '/product' AND resource_type = 'menu' AND deleted = 0 LIMIT 1;

-- AI API（parent = AI 管理 /ai）
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, request_method, parent_id, sort_order)
SELECT 'api', '/api/v1/ai/model/list',        'AI 模型列表',     'ai', 'POST', id, 0 FROM auth_resource WHERE resource_code = '/ai' AND resource_type = 'menu' AND deleted = 0 LIMIT 1;
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, request_method, parent_id, sort_order)
SELECT 'api', '/api/v1/ai/model/save',         '保存 AI 模型',    'ai', 'POST', id, 0 FROM auth_resource WHERE resource_code = '/ai' AND resource_type = 'menu' AND deleted = 0 LIMIT 1;
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, request_method, parent_id, sort_order)
SELECT 'api', '/api/v1/ai/prompt/list',       'Prompt 列表',     'ai', 'POST', id, 0 FROM auth_resource WHERE resource_code = '/ai' AND resource_type = 'menu' AND deleted = 0 LIMIT 1;
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, request_method, parent_id, sort_order)
SELECT 'api', '/api/v1/ai/knowledge/list',    '知识库列表',      'ai', 'POST', id, 0 FROM auth_resource WHERE resource_code = '/ai' AND resource_type = 'menu' AND deleted = 0 LIMIT 1;
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, request_method, parent_id, sort_order)
SELECT 'api', '/api/v1/ai/knowledge/save',    '保存知识库',      'ai', 'POST', id, 0 FROM auth_resource WHERE resource_code = '/ai' AND resource_type = 'menu' AND deleted = 0 LIMIT 1;
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, request_method, parent_id, sort_order)
SELECT 'api', '/api/v1/ai/task/list',         'AI 任务列表',     'ai', 'POST', id, 0 FROM auth_resource WHERE resource_code = '/ai' AND resource_type = 'menu' AND deleted = 0 LIMIT 1;
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, request_method, parent_id, sort_order)
SELECT 'api', '/api/v1/ai/task/create',       '创建 AI 任务',    'ai', 'POST', id, 0 FROM auth_resource WHERE resource_code = '/ai' AND resource_type = 'menu' AND deleted = 0 LIMIT 1;

-- ============================================================
-- 3. 角色-资源绑定
-- ============================================================

-- admin: 所有资源
INSERT INTO auth_role_resource (role_id, resource_id)
SELECT r.id, res.id FROM auth_role r CROSS JOIN auth_resource res
WHERE r.role_code = 'admin' AND r.deleted = 0 AND res.deleted = 0;

-- user: 基础资源
INSERT INTO auth_role_resource (role_id, resource_id)
SELECT r.id, res.id FROM auth_role r CROSS JOIN auth_resource res
WHERE r.role_code = 'user' AND r.deleted = 0 AND res.deleted = 0
  AND res.resource_code IN (
    '/dashboard', '/api/v1/auth/profile', '/api/v1/auth/profile/update', '/api/v1/auth/profile/change-password',
    '/api/v1/auth/menu/search', '/api/v1/auth/resource/search',
    '/api/v1/auth/oauth/bindings', '/api/v1/auth/oauth/bind', '/api/v1/auth/oauth/unbind',
    '/api/v1/auth/login', '/api/v1/auth/logout', '/api/v1/auth/captcha'
  );

-- institution: 机构 + 业务
INSERT INTO auth_role_resource (role_id, resource_id)
SELECT r.id, res.id FROM auth_role r CROSS JOIN auth_resource res
WHERE r.role_code = 'institution' AND r.deleted = 0 AND res.deleted = 0
  AND res.resource_code IN (
    '/dashboard', '/api/v1/auth/profile', '/api/v1/auth/profile/update', '/api/v1/auth/profile/change-password',
    '/api/v1/auth/menu/search', '/api/v1/auth/resource/search',
    '/api/v1/auth/oauth/bindings', '/api/v1/auth/oauth/bind', '/api/v1/auth/oauth/unbind',
    '/api/v1/dashboard/org/stats',
    '/douyin', '/douyin/accounts', '/douyin/videos', '/copy', '/copy/library', '/shortvideo', '/live', '/live/sessions', '/product', '/product/list', '/product/sales-history',
    '/api/v1/douyin/account/search', '/api/v1/douyin/account/get', '/api/v1/douyin/account/save',
    '/api/v1/douyin/video/search', '/api/v1/douyin/persona/list', '/api/v1/douyin/persona/save',
    '/api/v1/copy/library/search', '/api/v1/product/search', '/api/v1/product/save', '/api/v1/product/sales/history',
    '/api/v1/live/session/search', '/api/v1/live/session/save', '/api/v1/ai/knowledge/list', '/api/v1/ai/task/list', '/api/v1/ai/task/create'
  );

-- talent: 达人资源
INSERT INTO auth_role_resource (role_id, resource_id)
SELECT r.id, res.id FROM auth_role r CROSS JOIN auth_resource res
WHERE r.role_code = 'talent' AND r.deleted = 0 AND res.deleted = 0
  AND res.resource_code IN (
    '/dashboard', '/api/v1/auth/profile', '/api/v1/auth/profile/update', '/api/v1/auth/profile/change-password',
    '/api/v1/auth/menu/search', '/api/v1/auth/resource/search',
    '/api/v1/auth/oauth/bindings', '/api/v1/auth/oauth/bind', '/api/v1/auth/oauth/unbind',
    '/api/v1/dashboard/talent/stats',
    '/shortvideo', '/live', '/live/sessions', '/product', '/product/list', '/product/sales-history', '/ai', '/ai/knowledge', '/ai/task', '/ai/media',
    '/api/v1/douyin/persona/list', '/api/v1/douyin/persona/save',
    '/api/v1/live/session/search', '/api/v1/live/session/save', '/api/v1/product/search',
    '/api/v1/ai/knowledge/list', '/api/v1/ai/task/list', '/api/v1/ai/task/create'
  );

COMMIT;
