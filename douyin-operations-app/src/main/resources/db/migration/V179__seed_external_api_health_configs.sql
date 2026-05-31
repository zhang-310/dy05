-- Seed the external providers that are wired into the local Docker deployment.
-- Secrets stay in environment variables; this table only gives the health page
-- something real to monitor.
INSERT INTO external_api_config (
    provider_code,
    provider_name,
    category,
    base_url,
    is_enabled,
    priority,
    rate_limit_per_min,
    daily_quota,
    monthly_quota,
    health_status,
    extra_config
)
VALUES
    ('tianapi', 'TianAPI 素材采集', 'data', 'https://apis.tianapi.com', true, 10, 60, 260000, null, 'unknown',
     '{"healthPath":"/","method":"GET","note":"配置密钥由 TIANAPI_API_KEY 环境变量提供"}'::jsonb),
    ('ollama', 'Ollama 本地模型', 'ai', 'http://host.docker.internal:11434', true, 20, 120, null, null, 'unknown',
     '{"healthPath":"/api/tags","method":"GET"}'::jsonb),
    ('milvus', 'Milvus 向量库', 'vector', 'http://host.docker.internal:9091', true, 30, 120, null, null, 'unknown',
     '{"healthPath":"/healthz","method":"GET"}'::jsonb),
    ('elasticsearch', 'Elasticsearch 检索', 'search', 'http://elasticsearch:9200', true, 40, 120, null, null, 'unknown',
     '{"healthPath":"/_cluster/health","method":"GET"}'::jsonb),
    ('rabbitmq', 'RabbitMQ 消息队列', 'message', 'http://rabbitmq:15672', true, 50, 120, null, null, 'unknown',
     '{"healthPath":"/","method":"GET","note":"管理端口仅用于连通性检测"}'::jsonb)
ON CONFLICT (provider_code) WHERE deleted = 0 DO NOTHING;
