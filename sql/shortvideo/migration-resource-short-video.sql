-- ============================================================
-- 短视频模块 - 统一 API 路径为 /api/v1/short-video/*
-- 用法：psql -U postgres -d douyin_operations -f sql/shortvideo/migration-resource-short-video.sql
-- ============================================================

-- 1. 批量更新：shortvideo -> short-video（与 Controller 一致）
UPDATE auth_resource SET resource_code = REPLACE(resource_code, '/api/v1/shortvideo/', '/api/v1/short-video/')
WHERE resource_code LIKE '/api/v1/shortvideo/%' AND deleted = 0;

-- 2. 特殊映射：旧 resource-data 中 video/* -> content/*
UPDATE auth_resource SET resource_code = '/api/v1/short-video/content/search' WHERE resource_code = '/api/v1/short-video/video/search' AND deleted = 0;
UPDATE auth_resource SET resource_code = '/api/v1/short-video/content/get' WHERE resource_code = '/api/v1/short-video/video/get' AND deleted = 0;
UPDATE auth_resource SET resource_code = '/api/v1/short-video/content/save' WHERE resource_code = '/api/v1/short-video/video/save' AND deleted = 0;
UPDATE auth_resource SET resource_code = '/api/v1/short-video/content/delete' WHERE resource_code = '/api/v1/short-video/video/delete' AND deleted = 0;

-- 2. 补充缺失的 short-video API 资源（按实际 Controller）
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, request_method, parent_id, sort_order)
SELECT 'api', r.c, r.n, 'shortvideo', r.m, 0, 0
FROM (VALUES
  ('/api/v1/short-video/dashboard/stats', '仪表盘统计', 'POST'),
  ('/api/v1/short-video/dashboard/trend', '仪表盘趋势', 'POST'),
  ('/api/v1/short-video/dashboard/projects', '仪表盘项目', 'POST'),
  ('/api/v1/short-video/dashboard/cost-breakdown', '成本分解', 'POST'),
  ('/api/v1/short-video/quick/generate', '快速生成', 'POST'),
  ('/api/v1/short-video/project/list', '项目列表', 'POST'),
  ('/api/v1/short-video/project/get', '项目详情', 'POST'),
  ('/api/v1/short-video/project/save', '保存项目', 'POST'),
  ('/api/v1/short-video/project/delete', '删除项目', 'POST'),
  ('/api/v1/short-video/script/list', '脚本列表', 'POST'),
  ('/api/v1/short-video/script/get', '脚本详情', 'POST'),
  ('/api/v1/short-video/script/save', '保存脚本', 'POST'),
  ('/api/v1/short-video/script/delete', '删除脚本', 'POST'),
  ('/api/v1/short-video/script/generate', 'AI生成脚本', 'POST'),
  ('/api/v1/short-video/script/analyze-viral', '爆款分析', 'POST'),
  ('/api/v1/short-video/shot-list/list', '分镜列表', 'POST'),
  ('/api/v1/short-video/shot-list/get', '分镜详情', 'POST'),
  ('/api/v1/short-video/shot-list/get-by-script', '按脚本获取分镜', 'POST'),
  ('/api/v1/short-video/shot-list/save', '保存分镜', 'POST'),
  ('/api/v1/short-video/shot-list/generate', 'AI生成分镜', 'POST'),
  ('/api/v1/short-video/material/generate-keyframes', '生成关键帧', 'POST'),
  ('/api/v1/short-video/material/generate-keyframes-stream', '生成关键帧(流式)', 'POST'),
  ('/api/v1/short-video/material/generate-voice-batch', '批量生成配音', 'POST'),
  ('/api/v1/short-video/material/img2video-batch', '图生视频批量', 'POST'),
  ('/api/v1/short-video/material/img2video-batch-stream', '图生视频(流式)', 'POST'),
  ('/api/v1/short-video/material/recommend-camera', '运镜推荐', 'POST'),
  ('/api/v1/short-video/material/evaluate-video-quality', '视频质量评分', 'POST'),
  ('/api/v1/short-video/material/retry-keyframe', '重试关键帧', 'POST'),
  ('/api/v1/short-video/edit/auto-compose', '自动剪辑', 'POST'),
  ('/api/v1/short-video/edit/generate-subtitles', '生成字幕', 'POST'),
  ('/api/v1/short-video/publish/generate-title', '生成标题', 'POST'),
  ('/api/v1/short-video/publish/ai-review', 'AI审核', 'POST'),
  ('/api/v1/short-video/publish/douyin', '抖音发布', 'POST'),
  ('/api/v1/short-video/publish/publish', '发布', 'POST'),
  ('/api/v1/short-video/upload/keyframe', '上传关键帧', 'POST'),
  ('/api/v1/short-video/upload/keyframes/batch', '批量上传关键帧', 'POST'),
  ('/api/v1/short-video/upload/video', '上传视频', 'POST'),
  ('/api/v1/short-video/upload/audio', '上传音频', 'POST'),
  ('/api/v1/short-video/upload/thumbnail', '上传封面', 'POST'),
  ('/api/v1/short-video/upload/final-video', '上传成片', 'POST'),
  ('/api/v1/short-video/upload/reference/character', '上传角色参考', 'POST'),
  ('/api/v1/short-video/upload/reference/scene', '上传场景参考', 'POST'),
  ('/api/v1/short-video/upload/reference/list', '参考列表', 'POST'),
  ('/api/v1/short-video/library/list', '素材库列表', 'POST'),
  ('/api/v1/short-video/library/delete', '删除素材', 'POST'),
  ('/api/v1/short-video/video-task/submit', '提交视频任务', 'POST'),
  ('/api/v1/short-video/video-task/status', '任务状态', 'POST'),
  ('/api/v1/short-video/video-task/cancel', '取消任务', 'POST'),
  ('/api/v1/short-video/video-task/retry', '重试任务', 'POST'),
  ('/api/v1/short-video/workflow/execute', '执行工作流', 'POST'),
  ('/api/v1/short-video/workflow/status', '工作流状态', 'POST'),
  ('/api/v1/short-video/workflow/ai-assist', 'AI辅助', 'POST'),
  ('/api/v1/short-video/drama/list', '短剧列表', 'POST'),
  ('/api/v1/short-video/drama/get', '短剧详情', 'POST'),
  ('/api/v1/short-video/drama/create', '创建短剧', 'POST'),
  ('/api/v1/short-video/drama/episodes', '短剧集数', 'POST'),
  ('/api/v1/short-video/drama/add-episode', '添加集数', 'POST'),
  ('/api/v1/short-video/drama/link-episode', '关联集数', 'POST'),
  ('/api/v1/short-video/drama/characters', '短剧角色', 'POST'),
  ('/api/v1/short-video/drama/add-character', '添加角色', 'POST'),
  ('/api/v1/short-video/drama/generate-script', '生成剧本', 'POST'),
  ('/api/v1/short-video/data/collect-hot-videos', '采集爆款', 'POST'),
  ('/api/v1/short-video/data/analyze-viral', '分析爆款', 'POST'),
  ('/api/v1/short-video/music/generate-bgm', '生成BGM', 'POST'),
  ('/api/v1/short-video/music/generate-sfx', '生成音效', 'POST'),
  ('/api/v1/short-video/music/providers', '音乐提供商', 'POST'),
  ('/api/v1/short-video/quality-dashboard/overview', '质量概览', 'POST'),
  ('/api/v1/short-video/quality-dashboard/trend', '质量趋势', 'POST'),
  ('/api/v1/short-video/quality-dashboard/model-ranking', '模型排名', 'POST'),
  ('/api/v1/short-video/quality-dashboard/camera-ranking', '运镜排名', 'POST'),
  ('/api/v1/short-video/quality-dashboard/ai-reflections', 'AI反思', 'POST')
) AS r(c, n, m)
WHERE NOT EXISTS (SELECT 1 FROM auth_resource WHERE resource_code = r.c AND deleted = 0);

-- 3. 绑定 admin 角色
INSERT INTO auth_role_resource (role_id, resource_id)
SELECT
    (SELECT id FROM auth_role WHERE role_code = 'admin' AND deleted = 0 LIMIT 1),
    id
FROM auth_resource
WHERE module = 'shortvideo' AND deleted = 0
  AND resource_code LIKE '/api/v1/short-video/%'
  AND id NOT IN (
      SELECT resource_id FROM auth_role_resource
      WHERE role_id = (SELECT id FROM auth_role WHERE role_code = 'admin' AND deleted = 0 LIMIT 1)
  )
ON CONFLICT DO NOTHING;
