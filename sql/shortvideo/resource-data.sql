-- ============================================================
-- shortvideo 模块 - 资源初始数据
-- 依赖：schema.sql（sv_video / sv_plan / sv_viral_video 等 10 张表）
-- 数据库：PostgreSQL
-- 说明：插入菜单资源、API 资源、按钮资源，以及角色-资源绑定
-- 幂等：使用 ON CONFLICT DO NOTHING，可重复执行
-- ============================================================

-- ============================================================
-- 1. 菜单资源（resource_type = 'menu'）
-- ============================================================
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, request_method, parent_id, sort_order)
VALUES
    ('menu', '/shortvideo',           '短视频管理',   'shortvideo', NULL, 0, 700),
    ('menu', '/shortvideo/videos',    '视频列表',     'shortvideo', NULL, 0, 1),
    ('menu', '/shortvideo/plans',     '创作方案',     'shortvideo', NULL, 0, 2),
    ('menu', '/shortvideo/viral',     '爆款库',       'shortvideo', NULL, 0, 3),
    ('menu', '/shortvideo/topics',    '热点话题',     'shortvideo', NULL, 0, 4),
    ('menu', '/shortvideo/categories','视频分类',     'shortvideo', NULL, 0, 5)
ON CONFLICT DO NOTHING;

-- 更新子菜单的 parent_id 指向「短视频管理」
UPDATE auth_resource
SET    parent_id = (SELECT id FROM auth_resource WHERE resource_code = '/shortvideo' AND resource_type = 'menu' AND deleted = 0 LIMIT 1)
WHERE  resource_code IN ('/shortvideo/videos', '/shortvideo/plans', '/shortvideo/viral', '/shortvideo/topics', '/shortvideo/categories')
  AND  resource_type = 'menu'
  AND  deleted = 0
  AND  parent_id = 0;

-- ============================================================
-- 2. API 资源（resource_type = 'api'）
-- ============================================================

-- ---------- 统一使用 /api/v1/short-video/* 路径（与 Controller 一致）----------
-- SvVideo / Content ----------
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, request_method, parent_id, sort_order)
VALUES
    ('api', '/api/v1/short-video/content/search',  '查询视频列表',    'shortvideo', 'POST', 0, 0),
    ('api', '/api/v1/short-video/content/get',     '获取视频详情',    'shortvideo', 'POST', 0, 0),
    ('api', '/api/v1/short-video/content/save',    '新增/编辑视频',   'shortvideo', 'POST', 0, 0),
    ('api', '/api/v1/short-video/content/delete',  '删除视频',        'shortvideo', 'POST', 0, 0),
    ('api', '/api/v1/short-video/content/increment-view-count', '播放量+1', 'shortvideo', 'POST', 0, 0)
ON CONFLICT DO NOTHING;

-- SvProject ----------
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, request_method, parent_id, sort_order)
VALUES
    ('api', '/api/v1/short-video/project/list',   '项目列表',    'shortvideo', 'POST', 0, 0),
    ('api', '/api/v1/short-video/project/get',    '项目详情',    'shortvideo', 'POST', 0, 0),
    ('api', '/api/v1/short-video/project/save',  '保存项目',    'shortvideo', 'POST', 0, 0),
    ('api', '/api/v1/short-video/project/delete', '删除项目',    'shortvideo', 'POST', 0, 0)
ON CONFLICT DO NOTHING;

-- SvViralVideo ----------
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, request_method, parent_id, sort_order)
VALUES
    ('api', '/api/v1/short-video/viral/list',     '爆款列表',      'shortvideo', 'POST', 0, 0),
    ('api', '/api/v1/short-video/viral/get',      '爆款详情',    'shortvideo', 'POST', 0, 0),
    ('api', '/api/v1/short-video/viral/collect',  '收藏爆款',    'shortvideo', 'POST', 0, 0),
    ('api', '/api/v1/short-video/viral/delete',   '取消收藏',    'shortvideo', 'POST', 0, 0),
    ('api', '/api/v1/short-video/viral/analyze',  '爆款分析',    'shortvideo', 'POST', 0, 0),
    ('api', '/api/v1/short-video/viral/replicate', '爆款复刻',   'shortvideo', 'POST', 0, 0),
    ('api', '/api/v1/short-video/viral/recommended', '推荐爆款',  'shortvideo', 'POST', 0, 0)
ON CONFLICT DO NOTHING;

-- SvCategory ----------
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, request_method, parent_id, sort_order)
VALUES
    ('api', '/api/v1/short-video/category/list',   '分类列表',  'shortvideo', 'POST', 0, 0),
    ('api', '/api/v1/short-video/category/get',    '分类详情',  'shortvideo', 'POST', 0, 0),
    ('api', '/api/v1/short-video/category/save',   '保存分类',  'shortvideo', 'POST', 0, 0),
    ('api', '/api/v1/short-video/category/delete', '删除分类',  'shortvideo', 'POST', 0, 0)
ON CONFLICT DO NOTHING;

-- SvComment ----------
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, request_method, parent_id, sort_order)
VALUES
    ('api', '/api/v1/short-video/comment/search', '评论搜索',   'shortvideo', 'POST', 0, 0),
    ('api', '/api/v1/short-video/comment/get',    '评论详情',   'shortvideo', 'POST', 0, 0),
    ('api', '/api/v1/short-video/comment/save',   '保存评论',   'shortvideo', 'POST', 0, 0),
    ('api', '/api/v1/short-video/comment/delete', '删除评论',   'shortvideo', 'POST', 0, 0),
    ('api', '/api/v1/short-video/comment/increment-like-count', '评论点赞', 'shortvideo', 'POST', 0, 0)
ON CONFLICT DO NOTHING;

-- 脚本模板 / 脚本 / 分镜 / 素材 / 工作流 / 质量仪表板 等（详见 migration-resource-short-video.sql 补充）

-- ============================================================
-- 3. 按钮资源（resource_type = 'button'）
-- ============================================================
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, request_method, parent_id, sort_order)
VALUES
    ('button', 'shortvideo:video:add',       '新增视频',      'shortvideo', NULL, 0, 0),
    ('button', 'shortvideo:video:edit',      '编辑视频',      'shortvideo', NULL, 0, 0),
    ('button', 'shortvideo:video:delete',    '删除视频',      'shortvideo', NULL, 0, 0),
    ('button', 'shortvideo:video:sync',      '同步视频',      'shortvideo', NULL, 0, 0),
    ('button', 'shortvideo:plan:add',        '新增方案',      'shortvideo', NULL, 0, 0),
    ('button', 'shortvideo:plan:edit',       '编辑方案',      'shortvideo', NULL, 0, 0),
    ('button', 'shortvideo:plan:delete',     '删除方案',      'shortvideo', NULL, 0, 0),
    ('button', 'shortvideo:plan:publish',    '发布方案',      'shortvideo', NULL, 0, 0),
    ('button', 'shortvideo:viral:add',       '新增爆款',      'shortvideo', NULL, 0, 0),
    ('button', 'shortvideo:viral:delete',    '删除爆款',      'shortvideo', NULL, 0, 0),
    ('button', 'shortvideo:category:add',    '新增分类',      'shortvideo', NULL, 0, 0),
    ('button', 'shortvideo:category:edit',   '编辑分类',      'shortvideo', NULL, 0, 0),
    ('button', 'shortvideo:category:delete', '删除分类',      'shortvideo', NULL, 0, 0)
ON CONFLICT DO NOTHING;

-- ============================================================
-- 4. 角色-资源绑定（admin 角色）
-- ============================================================
INSERT INTO auth_role_resource (role_id, resource_id)
SELECT
    (SELECT id FROM auth_role WHERE role_code = 'admin' AND deleted = 0 LIMIT 1),
    id
FROM auth_resource
WHERE module = 'shortvideo' AND deleted = 0
  AND id NOT IN (
      SELECT resource_id FROM auth_role_resource
      WHERE role_id = (SELECT id FROM auth_role WHERE role_code = 'admin' AND deleted = 0 LIMIT 1)
  )
ON CONFLICT DO NOTHING;
