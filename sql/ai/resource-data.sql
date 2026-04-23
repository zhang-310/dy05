-- ============================================================
-- ai 模块 - 资源数据初始化（使用 auth_resource 规范）
-- ============================================================

-- AI 顶级菜单
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, request_method, parent_id, sort_order)
VALUES ('menu', '/ai', 'AI 能力', 'ai', NULL, 0, 100)
ON CONFLICT DO NOTHING;

-- AI 子菜单与 API（parent_id 指向 /ai，后续更新）
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, request_method, parent_id, sort_order)
VALUES
    ('menu', '/ai/model', 'AI 模型管理', 'ai', NULL, 0, 1),
    ('api', '/api/v1/ai/model/search', 'AI 模型列表', 'ai', 'POST', 0, 11),
    ('api', '/api/v1/ai/model/get', 'AI 模型详情', 'ai', 'POST', 0, 12),
    ('api', '/api/v1/ai/model/save', 'AI 模型保存', 'ai', 'POST', 0, 13),
    ('api', '/api/v1/ai/model/delete', 'AI 模型删除', 'ai', 'POST', 0, 14),
    ('menu', '/ai/prompt', 'Prompt 模板管理', 'ai', NULL, 0, 2),
    ('api', '/api/v1/ai/prompt/search', 'Prompt 模板列表', 'ai', 'POST', 0, 21),
    ('api', '/api/v1/ai/prompt/get', 'Prompt 模板详情', 'ai', 'POST', 0, 22),
    ('api', '/api/v1/ai/prompt/save', 'Prompt 模板保存', 'ai', 'POST', 0, 23),
    ('api', '/api/v1/ai/prompt/delete', 'Prompt 模板删除', 'ai', 'POST', 0, 24),
    ('api', '/api/v1/ai/prompt/preview', 'Prompt 模板预览', 'ai', 'POST', 0, 25),
    ('menu', '/ai/knowledge', '知识库管理', 'ai', NULL, 0, 3),
    ('api', '/api/v1/ai/knowledge/search', '知识库列表', 'ai', 'POST', 0, 31),
    ('api', '/api/v1/ai/knowledge/get', '知识库详情', 'ai', 'POST', 0, 32),
    ('api', '/api/v1/ai/knowledge/save', '知识库保存', 'ai', 'POST', 0, 33),
    ('api', '/api/v1/ai/knowledge/delete', '知识库删除', 'ai', 'POST', 0, 34),
    ('api', '/api/v1/ai/knowledge/upload', '知识库上传', 'ai', 'POST', 0, 35),
    ('menu', '/ai/generation', 'AI 生成任务', 'ai', NULL, 0, 4),
    ('api', '/api/v1/ai/generation/search', 'AI 生成任务列表', 'ai', 'POST', 0, 41),
    ('api', '/api/v1/ai/generation/submit', 'AI 生成任务提交', 'ai', 'POST', 0, 42),
    ('api', '/api/v1/ai/generation/result', 'AI 生成任务结果', 'ai', 'POST', 0, 43)
ON CONFLICT DO NOTHING;

-- 更新子菜单的 parent_id 指向 /ai
UPDATE auth_resource SET parent_id = (SELECT id FROM auth_resource WHERE resource_code = '/ai' AND resource_type = 'menu' AND deleted = 0 LIMIT 1)
WHERE resource_code IN ('/ai/model', '/ai/prompt', '/ai/knowledge', '/ai/generation') AND resource_type = 'menu' AND deleted = 0 AND parent_id = 0;

-- admin 角色绑定所有 AI 资源
INSERT INTO auth_role_resource (role_id, resource_id)
SELECT r.id, res.id FROM auth_role r, auth_resource res
WHERE r.role_code = 'admin' AND r.deleted = 0
  AND res.module = 'ai' AND res.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM auth_role_resource WHERE role_id = r.id AND resource_id = res.id);

-- user 角色绑定 prompt/knowledge/generation 相关 API（不含 model 管理）
INSERT INTO auth_role_resource (role_id, resource_id)
SELECT r.id, res.id FROM auth_role r, auth_resource res
WHERE r.role_code = 'user' AND r.deleted = 0
  AND res.resource_code IN (
    '/api/v1/ai/prompt/search', '/api/v1/ai/prompt/get', '/api/v1/ai/prompt/save', '/api/v1/ai/prompt/delete', '/api/v1/ai/prompt/preview',
    '/api/v1/ai/knowledge/search', '/api/v1/ai/knowledge/get', '/api/v1/ai/knowledge/save', '/api/v1/ai/knowledge/delete', '/api/v1/ai/knowledge/upload',
    '/api/v1/ai/generation/search', '/api/v1/ai/generation/submit', '/api/v1/ai/generation/result'
  )
  AND res.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM auth_role_resource WHERE role_id = r.id AND resource_id = res.id);
