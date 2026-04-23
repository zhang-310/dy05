# 04 DevOps 与运维分析

## 1. Docker 安全

| 问题 | 文件:行号 | 严重性 |
|------|---------|--------|
| Elasticsearch 未启用安全认证 | docker-compose.yml:97 `xpack.security.enabled: "false"` | HIGH |
| Minio 默认密钥 minioadmin | docker-compose.ai.yml:52-53 | HIGH |
| Milvus seccomp:unconfined | docker-compose.ai.yml:74-75 | MEDIUM |
| Milvus 无内存限制 | docker-compose.ai.yml:70 | MEDIUM |

## 2. Nginx 安全

| 问题 | 文件:行号 |
|------|---------|
| /actuator 访问控制太宽泛(172.28.0.0/16) | gateway.conf:150 |
| SSL证书路径硬编码 | gateway.conf:42 |

已有优势：
- HSTS、CSP、X-Frame-Options 等安全头完整 ✓
- 速率限制（API 100r/s, 前端 200r/s）✓
- Gzip 压缩级别6 ✓
- 缓存策略（JS/CSS 30d, 图片 90d）✓

## 3. 监控告警

缺失项：
- Redis/Elasticsearch 无 Prometheus exporter
- Grafana 无 Dashboard JSON 定义
- 测试覆盖率门槛未强制执行

已有优势：
- 131+ Prometheus 告警规则 + AI 专用告警 ✓
- logback-spring.xml 完整（audit/performance/application分离）✓
- JSON 格式支持 ELK ✓

## 4. 备份策略

- PostgreSQL：仅有数据卷，无自动备份/PITR
- Redis：仅有数据卷
- 建议：pgbackrest + 定期快照

## 5. CI/CD

已有优势：
- 多 Job 流水线（测试→质量→构建→部署）✓
- OWASP + Trivy 安全扫描 ✓
- Staging/Production 环境分离 ✓
- SBOM 生成 ✓

缺失：SonarQube 质量门仍 continue-on-error:true
