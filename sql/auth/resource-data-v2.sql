-- ============================================================
-- auth_resource + auth_role_resource 初始化数据 v2
-- 匹配 Vue SPA 前端路由 + 后端实际 Controller 端点
-- ============================================================

BEGIN;

-- 清空旧数据
TRUNCATE auth_role_resource RESTART IDENTITY CASCADE;
TRUNCATE auth_resource RESTART IDENTITY CASCADE;

-- ============================================================
-- 1. 菜单资源 (menu) - 按 Vue Router 路径
-- ============================================================

-- 顶级菜单组
INSERT INTO auth_resource (id, resource_type, resource_code, resource_name, module, parent_id, sort_order) VALUES
  (1,  'menu', '/dashboard',       '工作台',     'system',     0, 0),
  (2,  'menu', '/auth',            '系统管理',   'auth',       0, 10),
  (3,  'menu', '/log',             '日志管理',   'log',        0, 20),
  (4,  'menu', '/config',          '系统配置',   'config',     0, 30),
  (5,  'menu', '/storage',         '存储管理',   'storage',    0, 31),
  (6,  'menu', '/douyin',          '抖音运营',   'douyin',     0, 40),
  (7,  'menu', '/copy',            '文案管理',   'copy',       0, 50),
  (8,  'menu', '/script',          '话术管理',   'script',     0, 55),
  (9,  'menu', '/shortvideo',      '短视频',     'shortvideo', 0, 60),
  (10, 'menu', '/live',            '直播管理',   'live',       0, 70),
  (11, 'menu', '/product',         '产品管理',   'product',    0, 80),
  (12, 'menu', '/abtest',          'AB测试',     'abtest',     0, 90),
  (13, 'menu', '/agent',           '智能体',     'agent',      0, 95),
  (14, 'menu', '/ai',              'AI服务',     'ai',         0, 96),
  (15, 'menu', '/wecom',           '企业微信',   'wecom',      0, 100);

-- 系统管理子菜单 (parent_id = 2)
INSERT INTO auth_resource (id, resource_type, resource_code, resource_name, module, parent_id, sort_order) VALUES
  (101, 'menu', '/auth/users',      '用户管理',   'auth', 2, 1),
  (102, 'menu', '/auth/roles',      '角色管理',   'auth', 2, 2),
  (103, 'menu', '/auth/resources',  '资源管理',   'auth', 2, 3),
  (104, 'menu', '/auth/login-logs', '登录日志',   'auth', 2, 4);

-- 日志管理子菜单 (parent_id = 3)
INSERT INTO auth_resource (id, resource_type, resource_code, resource_name, module, parent_id, sort_order) VALUES
  (111, 'menu', '/log/operation',   '操作日志',   'log', 3, 1),
  (112, 'menu', '/log/system',      '系统日志',   'log', 3, 2);

-- 抖音运营子菜单 (parent_id = 6)
INSERT INTO auth_resource (id, resource_type, resource_code, resource_name, module, parent_id, sort_order) VALUES
  (121, 'menu', '/douyin/accounts',   '抖音账号管理', 'douyin', 6, 1),
  (122, 'menu', '/douyin/personas',   '账号人设管理', 'douyin', 6, 2),
  (123, 'menu', '/douyin/guidelines', '人设内容指南', 'douyin', 6, 3);

-- 文案管理子菜单 (parent_id = 7)
INSERT INTO auth_resource (id, resource_type, resource_code, resource_name, module, parent_id, sort_order) VALUES
  (131, 'menu', '/copy/library',   '文案库管理', 'copy', 7, 1),
  (132, 'menu', '/copy/approval',  '文案审核',   'copy', 7, 2),
  (133, 'menu', '/copy/templates', '文案模板',   'copy', 7, 3);

-- 话术管理子菜单 (parent_id = 8)
INSERT INTO auth_resource (id, resource_type, resource_code, resource_name, module, parent_id, sort_order) VALUES
  (141, 'menu', '/script/library',    '话术库管理', 'script', 8, 1),
  (142, 'menu', '/script/violations', '违规话术',   'script', 8, 2);

-- 短视频子菜单 (parent_id = 9)
INSERT INTO auth_resource (id, resource_type, resource_code, resource_name, module, parent_id, sort_order) VALUES
  (151, 'menu', '/shortvideo/list',      '短视频管理', 'shortvideo', 9, 1),
  (152, 'menu', '/shortvideo/plans',     '视频方案库', 'shortvideo', 9, 2),
  (153, 'menu', '/shortvideo/topics',    '热点话题',   'shortvideo', 9, 3),
  (154, 'menu', '/shortvideo/benchmark', '爆款视频库', 'shortvideo', 9, 4),
  (155, 'menu', '/shortvideo/analysis',  '视频复盘',   'shortvideo', 9, 5),
  (156, 'menu', '/shortvideo/assets',    '短视频素材', 'shortvideo', 9, 6),
  (157, 'menu', '/shortvideo/generate',  '短视频生成', 'shortvideo', 9, 7);

-- 直播管理子菜单 (parent_id = 10)
INSERT INTO auth_resource (id, resource_type, resource_code, resource_name, module, parent_id, sort_order) VALUES
  (161, 'menu', '/live/sessions', '直播场次',   'live', 10, 1),
  (162, 'menu', '/live/detail',   '直播详情',   'live', 10, 2),
  (163, 'menu', '/live/monitor',  '直播监控',   'live', 10, 3);

-- 产品管理子菜单 (parent_id = 11)
INSERT INTO auth_resource (id, resource_type, resource_code, resource_name, module, parent_id, sort_order) VALUES
  (171, 'menu', '/product/list',  '产品库管理',   'product', 11, 1),
  (172, 'menu', '/product/sales', '产品销售历史', 'product', 11, 2);

-- AB测试子菜单 (parent_id = 12)
INSERT INTO auth_resource (id, resource_type, resource_code, resource_name, module, parent_id, sort_order) VALUES
  (181, 'menu', '/abtest/variants', '话术版本管理', 'abtest', 12, 1),
  (182, 'menu', '/abtest/results',  'AB测试结果',   'abtest', 12, 2);

-- ============================================================
-- 2. API 资源 - 匹配后端 Controller 实际端点
-- ============================================================

-- Auth 认证 API (列顺序: id, resource_type, resource_code, resource_name, request_method, module, parent_id, sort_order)
INSERT INTO auth_resource (id, resource_type, resource_code, resource_name, request_method, module, parent_id, sort_order) VALUES
  (200, 'api', '/api/v1/auth/captcha',                '获取验证码',       'GET',  'auth', 0, 0),
  (201, 'api', '/api/v1/auth/login',                  '用户登录',        'POST', 'auth', 0, 1),
  (202, 'api', '/api/v1/auth/logout',                 '退出登录',        'POST', 'auth', 0, 2),
  (203, 'api', '/api/v1/auth/sms/send',               '发送验证码',      'POST', 'auth', 0, 3),
  (204, 'api', '/api/v1/auth/forgot-password',       '忘记密码',       'POST', 'auth', 0, 4),
  (205, 'api', '/api/v1/auth/profile',                '获取个人信息',    'POST', 'auth', 0, 5),
  (206, 'api', '/api/v1/auth/profile/update',         '更新个人信息',    'POST', 'auth', 0, 6),
  (207, 'api', '/api/v1/auth/profile/change-password','修改密码',       'POST', 'auth', 0, 7),
  (208, 'api', '/api/v1/auth/menu/search',            '获取菜单列表',    'POST', 'auth', 0, 8),
  (209, 'api', '/api/v1/auth/resource/search',       '获取资源编码',    'POST', 'auth', 0, 9);

-- Auth 用户管理 API
INSERT INTO auth_resource (id, resource_type, resource_code, resource_name, request_method, module, parent_id, sort_order) VALUES
  (210, 'api', '/api/v1/auth/user/search',     '用户列表查询',    'POST', 'auth', 0, 10),
  (211, 'api', '/api/v1/auth/user/get',        '用户详情',        'POST', 'auth', 0, 11),
  (212, 'api', '/api/v1/auth/user/save',       '用户新增/编辑',   'POST', 'auth', 0, 12),
  (213, 'api', '/api/v1/auth/user/ban',        '用户封禁/解禁',   'POST', 'auth', 0, 13),
  (214, 'api', '/api/v1/auth/user/login-logs', '用户登录记录',    'POST', 'auth', 0, 14),
  (215, 'api', '/api/v1/auth/user/online',     '在线用户列表',    'POST', 'auth', 0, 15),
  (216, 'api', '/api/v1/auth/user/delete',     '用户删除',        'POST', 'auth', 0, 16);

-- Auth 角色管理 API
INSERT INTO auth_resource (id, resource_type, resource_code, resource_name, request_method, module, parent_id, sort_order) VALUES
  (220, 'api', '/api/v1/auth/role/search',          '角色分页查询',    'POST', 'auth', 0, 20),
  (221, 'api', '/api/v1/auth/role/list',             '角色列表',        'POST', 'auth', 0, 21),
  (222, 'api', '/api/v1/auth/role/get',              '角色详情',        'POST', 'auth', 0, 22),
  (223, 'api', '/api/v1/auth/role/save',             '角色新增/编辑',   'POST', 'auth', 0, 23),
  (224, 'api', '/api/v1/auth/role/delete',           '角色删除',        'POST', 'auth', 0, 24),
  (225, 'api', '/api/v1/auth/role/resources',        '获取角色资源',    'POST', 'auth', 0, 25),
  (226, 'api', '/api/v1/auth/role/resources/save',   '保存角色授权',    'POST', 'auth', 0, 26);

-- Auth 资源管理 API
INSERT INTO auth_resource (id, resource_type, resource_code, resource_name, request_method, module, parent_id, sort_order) VALUES
  (230, 'api', '/api/v1/auth/resource/list',   '资源分页列表',    'POST', 'auth', 0, 30),
  (231, 'api', '/api/v1/auth/resource/tree',   '资源菜单树',      'POST', 'auth', 0, 31),
  (235, 'api', '/api/v1/auth/resource/tree-full', '完整资源树',    'POST', 'auth', 0, 35),
  (232, 'api', '/api/v1/auth/resource/get',    '资源详情',        'POST', 'auth', 0, 32),
  (233, 'api', '/api/v1/auth/resource/save',   '资源新增/编辑',   'POST', 'auth', 0, 33),
  (234, 'api', '/api/v1/auth/resource/delete', '资源删除',        'POST', 'auth', 0, 34);

-- OAuth API
INSERT INTO auth_resource (id, resource_type, resource_code, resource_name, request_method, module, parent_id, sort_order) VALUES
  (240, 'api', '/api/v1/auth/oauth/authorize', '第三方登录授权',  'GET',  'auth', 0, 40),
  (241, 'api', '/api/v1/auth/oauth/callback',  '第三方登录回调',  'GET',  'auth', 0, 41),
  (242, 'api', '/api/v1/auth/oauth/bindings',  '第三方绑定列表',  'POST', 'auth', 0, 42),
  (243, 'api', '/api/v1/auth/oauth/bind',      '绑定第三方账号',  'POST', 'auth', 0, 43),
  (244, 'api', '/api/v1/auth/oauth/unbind',    '解绑第三方账号',  'POST', 'auth', 0, 44);

-- Log API
INSERT INTO auth_resource (id, resource_type, resource_code, resource_name, request_method, module, parent_id, sort_order) VALUES
  (300, 'api', '/api/v1/log/operation/page',   '操作日志分页',    'POST', 'log', 0, 0),
  (301, 'api', '/api/v1/log/system/page',      '系统日志分页',    'POST', 'log', 0, 1),
  (302, 'api', '/api/v1/log/operation/export', '操作日志导出',   'POST', 'log', 0, 2),
  (303, 'api', '/api/v1/log/system/export',    '系统日志导出',   'POST', 'log', 0, 3);

-- Config API
INSERT INTO auth_resource (id, resource_type, resource_code, resource_name, request_method, module, parent_id, sort_order) VALUES
  (310, 'api', '/api/v1/config/list',   '配置列表',    'POST', 'config', 0, 0),
  (311, 'api', '/api/v1/config/get',    '配置详情',    'GET',  'config', 0, 1),
  (312, 'api', '/api/v1/config/save',   '配置保存',    'POST', 'config', 0, 2),
  (313, 'api', '/api/v1/config/delete', '配置删除',    'POST', 'config', 0, 3);

-- Storage API
INSERT INTO auth_resource (id, resource_type, resource_code, resource_name, request_method, module, parent_id, sort_order) VALUES
  (320, 'api', '/api/v1/storage/configured', '存储配置检查',  'GET',  'storage', 0, 0),
  (321, 'api', '/api/v1/storage/list',       '文件列表',      'POST', 'storage', 0, 1),
  (322, 'api', '/api/v1/storage/upload',     '文件上传',      'POST', 'storage', 0, 2),
  (323, 'api', '/api/v1/storage/delete',     '文件删除',      'POST', 'storage', 0, 3),
  (324, 'api', '/api/v1/storage/url',        '文件URL',       'GET',  'storage', 0, 4);

-- System 系统监控 API（仅管理员）
INSERT INTO auth_resource (id, resource_type, resource_code, resource_name, request_method, module, parent_id, sort_order) VALUES
  (330, 'api', '/api/v1/system/api-log/list',   'API调用日志列表',   'POST', 'system', 0, 0),
  (331, 'api', '/api/v1/system/api-log/stats',  'API调用统计',      'POST', 'system', 0, 1),
  (332, 'api', '/api/v1/system/api-log/get',    'API调用日志详情',   'POST', 'system', 0, 2),
  (333, 'api', '/api/v1/system/sync-log/list',  '同步日志列表',     'POST', 'system', 0, 3),
  (334, 'api', '/api/v1/system/health',         '系统健康检查',     'POST', 'system', 0, 4),
  (335, 'api', '/api/v1/system/info',           '系统运行信息',     'POST', 'system', 0, 5);

-- ============================================================
-- 3. 按钮资源 (button)
-- ============================================================
INSERT INTO auth_resource (id, resource_type, resource_code, resource_name, module, parent_id, sort_order) VALUES
  -- 用户管理按钮
  (400, 'button', 'user:create',  '用户-新增', 'auth', 101, 0),
  (401, 'button', 'user:edit',    '用户-编辑', 'auth', 101, 1),
  (402, 'button', 'user:ban',     '用户-封禁', 'auth', 101, 2),
  (403, 'button', 'user:unban',   '用户-解封', 'auth', 101, 3),
  (404, 'button', 'user:export',  '用户-导出', 'auth', 101, 4),
  -- 角色管理按钮
  (410, 'button', 'role:create',  '角色-新增', 'auth', 102, 0),
  (411, 'button', 'role:edit',    '角色-编辑', 'auth', 102, 1),
  (412, 'button', 'role:delete',  '角色-删除', 'auth', 102, 2),
  (413, 'button', 'role:assign',  '角色-授权', 'auth', 102, 3),
  -- 资源管理按钮
  (420, 'button', 'resource:create', '资源-新增', 'auth', 103, 0),
  (421, 'button', 'resource:edit',   '资源-编辑', 'auth', 103, 1),
  (422, 'button', 'resource:delete', '资源-删除', 'auth', 103, 2),
  -- 日志按钮
  (430, 'button', 'log:export',    '日志-导出', 'log', 3, 0),
  -- 配置按钮
  (440, 'button', 'config:save',   '配置-保存', 'config', 4, 0),
  (441, 'button', 'config:delete', '配置-删除', 'config', 4, 1);

-- 重置序列
SELECT setval('auth_resource_id_seq', (SELECT MAX(id) FROM auth_resource));

-- ============================================================
-- 4. 角色-资源绑定
-- ============================================================

-- admin (role_id=1): 绑定所有资源
INSERT INTO auth_role_resource (role_id, resource_id)
  SELECT 1, id FROM auth_resource;

-- user (role_id=2): 仅基础资源（工作台、个人信息、菜单、资源编码、登录/登出）
INSERT INTO auth_role_resource (role_id, resource_id) VALUES
  (2, 1),    -- 工作台菜单
  (2, 201),  -- 登录
  (2, 202),  -- 登出
  (2, 205),  -- 获取个人信息
  (2, 206),  -- 更新个人信息
  (2, 207),  -- 修改密码
  (2, 208),  -- 菜单列表
  (2, 209);  -- 资源编码

COMMIT;
