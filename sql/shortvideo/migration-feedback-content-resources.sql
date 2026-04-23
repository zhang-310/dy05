-- ============================================================
-- 短视频模块 - 补充 feedback / content / project / ai / shot-list 等 API 资源
-- 用法：psql -U postgres -d douyin_operations -f sql/shortvideo/migration-feedback-content-resources.sql
-- 幂等：WHERE NOT EXISTS 可重复执行
-- ============================================================

INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, request_method, parent_id, sort_order)
SELECT 'api', r.c, r.n, 'shortvideo', r.m, 0, 0
FROM (VALUES
  ('/api/v1/short-video/content/calendar', '内容日历', 'POST'),
  ('/api/v1/short-video/content/calendar-stats', '内容日历统计', 'POST'),
  ('/api/v1/short-video/content/publish-time-recommend', '发布时间推荐', 'POST'),
  ('/api/v1/short-video/content/data-trend', '数据趋势', 'POST'),
  ('/api/v1/short-video/feedback/analyze-performance', '效果分析', 'POST'),
  ('/api/v1/short-video/feedback/reflection-report', '反思报告', 'POST'),
  ('/api/v1/short-video/feedback/weekly-report', '周报', 'POST'),
  ('/api/v1/short-video/project/generate-daily', '生成每日任务', 'POST'),
  ('/api/v1/short-video/project/daily-list', '每日任务列表', 'POST'),
  ('/api/v1/short-video/project/update-shoot-status', '更新拍摄状态', 'POST'),
  ('/api/v1/short-video/project/export-script', '导出脚本', 'POST'),
  ('/api/v1/short-video/shot-list/review', '分镜审核', 'POST'),
  ('/api/v1/short-video/ai/generate-copy', 'AI生成文案', 'POST'),
  ('/api/v1/short-video/ai/generate-script', 'AI生成脚本', 'POST'),
  ('/api/v1/short-video/ai/generate-title', 'AI生成标题', 'POST'),
  ('/api/v1/short-video/ai/generate-plan', 'AI生成方案', 'POST'),
  ('/api/v1/short-video/script-template/search', '话术模板搜索', 'POST'),
  ('/api/v1/short-video/script-template/get', '话术模板详情', 'POST'),
  ('/api/v1/short-video/script-template/save', '保存话术模板', 'POST'),
  ('/api/v1/short-video/script-template/delete', '删除话术模板', 'POST'),
  ('/api/v1/short-video/script-template/use-count', '话术模板使用次数', 'POST')
) AS r(c, n, m)
WHERE NOT EXISTS (SELECT 1 FROM auth_resource WHERE resource_code = r.c AND deleted = 0);

-- 绑定 admin 角色
INSERT INTO auth_role_resource (role_id, resource_id)
SELECT
    (SELECT id FROM auth_role WHERE role_code = 'admin' AND deleted = 0 LIMIT 1),
    id
FROM auth_resource
WHERE module = 'shortvideo' AND deleted = 0
  AND resource_code IN (
    '/api/v1/short-video/content/calendar',
    '/api/v1/short-video/content/calendar-stats',
    '/api/v1/short-video/content/publish-time-recommend',
    '/api/v1/short-video/content/data-trend',
    '/api/v1/short-video/feedback/analyze-performance',
    '/api/v1/short-video/feedback/reflection-report',
    '/api/v1/short-video/feedback/weekly-report',
    '/api/v1/short-video/project/generate-daily',
    '/api/v1/short-video/project/daily-list',
    '/api/v1/short-video/project/update-shoot-status',
    '/api/v1/short-video/project/export-script',
    '/api/v1/short-video/shot-list/review',
    '/api/v1/short-video/ai/generate-copy',
    '/api/v1/short-video/ai/generate-script',
    '/api/v1/short-video/ai/generate-title',
    '/api/v1/short-video/ai/generate-plan',
    '/api/v1/short-video/script-template/search',
    '/api/v1/short-video/script-template/get',
    '/api/v1/short-video/script-template/save',
    '/api/v1/short-video/script-template/delete',
    '/api/v1/short-video/script-template/use-count'
  )
  AND id NOT IN (
      SELECT resource_id FROM auth_role_resource
      WHERE role_id = (SELECT id FROM auth_role WHERE role_code = 'admin' AND deleted = 0 LIMIT 1)
  )
ON CONFLICT DO NOTHING;
