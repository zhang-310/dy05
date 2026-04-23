# P2 升级项说明

> 版本：1.0 | 更新日期：2026-02-28

## 1. 分布式追踪（Zipkin/Jaeger）

**现状**：TraceId 已在 RESTResult 中返回，Micrometer + OpenTelemetry 依赖已存在。

**接入步骤**：
1. 添加 `micrometer-tracing-bridge-otel` + `opentelemetry-exporter-otlp` 依赖
2. 配置 `management.tracing.sampling.probability=1.0`
3. 部署 Zipkin 或 Jaeger Collector，配置 OTLP 接收
4. 环境变量：`OTEL_EXPORTER_OTLP_ENDPOINT=http://zipkin:4317`

## 2. 日志聚合（ELK/Loki）

**现状**：Logback + logstash-logback-encoder 已支持 JSON 日志。

**接入步骤**：
1. **ELK**：配置 Logstash Filebeat 采集 `logs/*.json`，输出到 Elasticsearch
2. **Loki**：使用 Promtail 采集日志，或配置 Logback Loki Appender

## 3. 知识引用率统计

**现状**：`EvolveRoiService.getKnowledgeRefRate()` 已实现，进化 ROI 管理页已展示。

**可选增强**：
- 在 Prometheus 中暴露 `ai_evolve_knowledge_ref_rate` 指标
- 配置告警：引用率 < 5% 时通知

## 4. API Key 加密

**现状**：ai_model.api_key 等字段以明文或简单编码存储。

**建议**：
1. 使用 AES-256-GCM 或 Spring Security Crypto 加密存储
2. 密钥从环境变量 `APP_ENCRYPTION_KEY` 或 KMS 获取
3. 统一 `AiModel`、`SysConfig` 等敏感字段的加解密逻辑

## 5. 文档同步

**待补充**：
- `docs/modules/ai/07-运维面板.md`：Grafana 面板、告警规则、健康检查
- `docs/modules/ai/08-大数据 BI.md`：知识引用率、进化 ROI 报表、数据导出
