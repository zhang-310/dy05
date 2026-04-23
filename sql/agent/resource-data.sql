-- ============================================================
-- agent 模块 - 资源数据初始化脚本
-- 更新日期：2026-02-25
-- 说明：初始化资源、角色、权限绑定数据
-- ============================================================

-- ============================================================
-- 1. 插入 auth_resource（资源表）- agent 模块相关资源
-- ============================================================

-- agent 模块菜单
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, parent_id, sort_order, deleted)
VALUES ('menu', '/agent', '智能体管理', 'agent', 0, 60, 0)
ON CONFLICT DO NOTHING;

-- 获取智能体ID（假设为新插入的资源）
-- 注：实际执行时需要根据上面的插入获取真实ID

-- agent 子菜单 - 智能体配置
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, parent_id, sort_order, deleted)
SELECT 'menu', '/agent/config', '智能体配置', 'agent', id, 10, 0
FROM auth_resource WHERE resource_code = '/agent' AND deleted = 0
ON CONFLICT DO NOTHING;

-- agent 子菜单 - 对话管理
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, parent_id, sort_order, deleted)
SELECT 'menu', '/agent/conversation', '对话管理', 'agent', id, 20, 0
FROM auth_resource WHERE resource_code = '/agent' AND deleted = 0
ON CONFLICT DO NOTHING;

-- API 资源 - 智能体搜索
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, request_method, sort_order, deleted)
VALUES ('api', '/api/v1/agent/config/search', '搜索智能体', 'agent', 'POST', 1, 0)
ON CONFLICT DO NOTHING;

-- API 资源 - 获取智能体
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, request_method, sort_order, deleted)
VALUES ('api', '/api/v1/agent/config/get', '获取智能体', 'agent', 'POST', 2, 0)
ON CONFLICT DO NOTHING;

-- API 资源 - 保存智能体
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, request_method, sort_order, deleted)
VALUES ('api', '/api/v1/agent/config/save', '保存智能体', 'agent', 'POST', 3, 0)
ON CONFLICT DO NOTHING;

-- API 资源 - 删除智能体
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, request_method, sort_order, deleted)
VALUES ('api', '/api/v1/agent/config/delete', '删除智能体', 'agent', 'POST', 4, 0)
ON CONFLICT DO NOTHING;

-- API 资源 - 启用智能体
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, request_method, sort_order, deleted)
VALUES ('api', '/api/v1/agent/config/enable', '启用智能体', 'agent', 'POST', 5, 0)
ON CONFLICT DO NOTHING;

-- API 资源 - 禁用智能体
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, request_method, sort_order, deleted)
VALUES ('api', '/api/v1/agent/config/disable', '禁用智能体', 'agent', 'POST', 6, 0)
ON CONFLICT DO NOTHING;

-- API 资源 - 搜索对话
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, request_method, sort_order, deleted)
VALUES ('api', '/api/v1/agent/conversation/search', '搜索对话', 'agent', 'POST', 7, 0)
ON CONFLICT DO NOTHING;

-- API 资源 - 获取对话
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, request_method, sort_order, deleted)
VALUES ('api', '/api/v1/agent/conversation/get', '获取对话', 'agent', 'POST', 8, 0)
ON CONFLICT DO NOTHING;

-- API 资源 - 创建对话
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, request_method, sort_order, deleted)
VALUES ('api', '/api/v1/agent/conversation/create', '创建对话', 'agent', 'POST', 9, 0)
ON CONFLICT DO NOTHING;

-- API 资源 - 关闭对话
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, request_method, sort_order, deleted)
VALUES ('api', '/api/v1/agent/conversation/close', '关闭对话', 'agent', 'POST', 10, 0)
ON CONFLICT DO NOTHING;

-- API 资源 - 获取消息
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, request_method, sort_order, deleted)
VALUES ('api', '/api/v1/agent/conversation/messages', '获取消息', 'agent', 'POST', 11, 0)
ON CONFLICT DO NOTHING;

-- API 资源 - 发送消息
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, request_method, sort_order, deleted)
VALUES ('api', '/api/v1/agent/message/send', '发送消息', 'agent', 'POST', 12, 0)
ON CONFLICT DO NOTHING;

-- API 资源 - 获取最新消息
INSERT INTO auth_resource (resource_type, resource_code, resource_name, module, request_method, sort_order, deleted)
VALUES ('api', '/api/v1/agent/message/latest', '获取最新消息', 'agent', 'POST', 13, 0)
ON CONFLICT DO NOTHING;

-- ============================================================
-- 2. 角色-资源绑定（admin 角色绑定所有 agent 资源）
-- ============================================================

-- 获取 admin 角色ID和所有 agent 资源ID，进行绑定
INSERT INTO auth_role_resource (role_id, resource_id)
SELECT
    (SELECT id FROM auth_role WHERE role_code = 'admin' AND deleted = 0 LIMIT 1),
    id
FROM auth_resource
WHERE module = 'agent' AND deleted = 0
ON CONFLICT DO NOTHING;

-- ============================================================
-- 3. 用户测试数据（可选）
-- ============================================================

-- 创建测试智能体（示例）
-- INSERT INTO agent (user_id, agent_name, description, agent_type, system_prompt, model_config, response_mode, status, deleted)
-- VALUES (1, '内容生成助手', '帮助生成高质量内容', 1, '你是一个专业的内容创作助手...', '{"model":"gpt-4","temperature":0.7,"maxTokens":2000}', 1, 1, 0);

-- 创建测试对话（示例）
-- INSERT INTO agent_conversation (agent_id, user_id, conversation_topic, status, message_count, deleted)
-- VALUES (1, 1, '关于春节营销的话题', 1, 0, 0);
