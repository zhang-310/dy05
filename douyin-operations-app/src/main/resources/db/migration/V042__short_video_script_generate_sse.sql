-- 短视频脚本生成 SSE 流式接口资源
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, request_method, parent_id, sort_order, deleted, create_time, update_time)
SELECT 'api', '/api/v1/short-video/script/generate-sse', 'AI生成脚本(流式)', 'shortvideo', 'POST',
  COALESCE((SELECT id FROM auth_resource WHERE resource_code = '/shortvideo' AND resource_type = 'menu' AND deleted = 0 LIMIT 1), 0),
  0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM auth_resource WHERE resource_code = '/api/v1/short-video/script/generate-sse' AND deleted = 0);

-- admin 角色绑定
INSERT INTO auth_role_resource (role_id, resource_id, create_time)
SELECT r.id, res.id, CURRENT_TIMESTAMP
FROM auth_role r, auth_resource res
WHERE r.role_code = 'admin' AND r.deleted = 0
  AND res.resource_code = '/api/v1/short-video/script/generate-sse' AND res.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM auth_role_resource arr WHERE arr.role_id = r.id AND arr.resource_id = res.id);
