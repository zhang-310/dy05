-- V204: API 白名单扩展
-- 健康检查、Prometheus、Swagger 路径免鉴权
-- （部署时由网关 Nginx 控制外部访问）
COMMENT ON TABLE sys_api_call_log IS 'API 调用日志 — 白名单路径: /actuator/health, /swagger-ui, /api-docs';
