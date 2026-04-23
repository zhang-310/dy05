-- AI 运维管理 API 资源（仅 admin）
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, request_method, parent_id, sort_order)
SELECT 'api', '/api/v1/ai/admin/infra/documents/pg', 'PostgreSQL 文档分页', 'ai', 'POST', 0, 201
WHERE NOT EXISTS (SELECT 1 FROM auth_resource WHERE resource_code = '/api/v1/ai/admin/infra/documents/pg' AND deleted = 0);

INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, request_method, parent_id, sort_order)
SELECT 'api', '/api/v1/ai/admin/infra/documents/es', 'ES 文档分页', 'ai', 'POST', 0, 202
WHERE NOT EXISTS (SELECT 1 FROM auth_resource WHERE resource_code = '/api/v1/ai/admin/infra/documents/es' AND deleted = 0);

INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, request_method, parent_id, sort_order)
SELECT 'api', '/api/v1/ai/admin/infra/milvus/stats', 'Milvus 向量统计', 'ai', 'GET', 0, 203
WHERE NOT EXISTS (SELECT 1 FROM auth_resource WHERE resource_code = '/api/v1/ai/admin/infra/milvus/stats' AND deleted = 0);

INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, request_method, parent_id, sort_order)
SELECT 'api', '/api/v1/ai/admin/infra/queue', '索引队列表分页', 'ai', 'POST', 0, 204
WHERE NOT EXISTS (SELECT 1 FROM auth_resource WHERE resource_code = '/api/v1/ai/admin/infra/queue' AND deleted = 0);

-- admin 角色绑定
INSERT INTO auth_role_resource (role_id, resource_id)
SELECT r.id, res.id FROM auth_role r, auth_resource res
WHERE r.role_code = 'admin' AND r.deleted = 0
  AND res.resource_code IN (
    '/api/v1/ai/admin/infra/documents/pg',
    '/api/v1/ai/admin/infra/documents/es',
    '/api/v1/ai/admin/infra/milvus/stats',
    '/api/v1/ai/admin/infra/queue'
  )
  AND res.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM auth_role_resource WHERE role_id = r.id AND resource_id = res.id);
