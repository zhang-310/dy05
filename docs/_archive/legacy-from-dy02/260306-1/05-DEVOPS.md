# 05 DevOps / CI/CD / 监控分析

> 综合评分：60/100（C 级）
> 发现问题：28 项（P0: 3 / P1: 10 / P2: 15）

---

## P0 - 必须修复

### OPS-01: CI/CD 使用 JDK 17，项目要求 JDK 21

**文件**: `.github/workflows/ci-cd.yml:54-56`
```yaml
- name: Set up JDK 17
  uses: actions/setup-java@v4
  with:
    java-version: '17'  # 应为 21
```
**影响**: 本地编译通过但 CI 失败，或更危险——CI 通过但生产环境行为不同
**修复**: 改为 `java-version: '21'`

---

### OPS-02: 代码质量门禁全部 continue-on-error

**文件**: `.github/workflows/code-quality.yml`
```yaml
- name: Run Checkstyle
  run: mvn checkstyle:check
  continue-on-error: true   # Checkstyle 失败不阻塞

- name: Run PMD
  run: mvn pmd:check
  continue-on-error: true   # PMD 失败不阻塞

- name: Run SpotBugs
  run: mvn spotbugs:check
  continue-on-error: true   # SpotBugs 失败不阻塞
```
**影响**: 代码质量问题永远不会阻塞合并
**修复**: 至少 SpotBugs（安全类检查）必须 fail

---

### OPS-03: 灰度发版副本数与流量不匹配

**文件**: `scripts/deploy-prod.sh:133-142`
- 1% 流量 → 1 副本
- 10% 流量 → 1 副本（竞争）
- 50% 流量 → 2 副本（不够）

**修复**: 调整副本数匹配流量比例

---

## P1 - 应尽快修复

### OPS-04: Dockerfile 缺少 dumb-init
`docker/Dockerfile:40` — JVM 进程作为 PID 1 无法正确处理 SIGTERM
**修复**: `RUN apk add --no-cache dumb-init` + `ENTRYPOINT ["/usr/bin/dumb-init", "--"]`

### OPS-05: Docker Compose 缺少资源限制
仅后端应用配置了 memory limit，其他 8 个服务无限制
**修复**: 为所有服务配置 `deploy.resources.limits`

### OPS-06: 网络隔离缺失
所有容器在同一 bridge 网络，frontend 可直接连接 database
**修复**: 创建 public / internal / data 三层网络

### OPS-07: Prometheus 保留时间仅 15 天
**文件**: `docker-compose.yml:246`
**修复**: 改为 90 天以支持季度趋势分析

### OPS-08: 缺少关键告警规则
缺失告警：
- PostgreSQL 连接异常
- 慢查询 > 1s
- 缓存命中率 < 70%
- ES 分片异常
- MQ 消息堆积
- P99 vs P50 差异过大

### OPS-09: Nginx 缺少 client_max_body_size
**修复**: `client_max_body_size 100M;`

### OPS-10: 部署脚本缺少幂等性
`docker/deploy.sh:40` — 每次重新构建，无变更检查
**修复**: 检查 git diff，无变更跳过构建

### OPS-11: 健康检查重试不足
仅 30 次 x 2 秒 = 60 秒，大镜像可能不够
**修复**: 增加到 90 次或添加指数退避

### OPS-12: 数据库初始化无错误检查
SQL 执行失败不会导致部署失败
**修复**: 添加 `ON_ERROR_STOP=1`

### OPS-13: 紧急回滚确认机制过简
仅 yes/no 确认，易误触发
**修复**: 要求输入版本号进行二次确认

---

## P2 - 优化项

| # | 问题 | 建议 |
|---|------|------|
| OPS-14 | 缺少 Elasticsearch/RabbitMQ/Milvus 指标采集 | 扩展 Prometheus scrape |
| OPS-15 | 告警无分级和 SLA | 添加 P1-P4 + runbook |
| OPS-16 | 前端 ESLint 未在 CI 检查 | 添加 lint 步骤 |
| OPS-17 | 构建产物未保存到 Artifacts | 添加 upload-artifact |
| OPS-18 | OWASP 检查范围过大 | 限制到 src/ 目录 |
| OPS-19 | SPA 路由 try_files 过宽泛 | 区分静态资源和路由 |
| OPS-20 | 缺少 ETag/Last-Modified | 优化缓存策略 |
| OPS-21 | 无 Certbot 自动续期 | 集成 Let's Encrypt |
| OPS-22 | logback-spring.xml 缺失 | 配置日志轮转策略 |
| OPS-23 | Tomcat timeout 30 分钟过长 | 分环境配置 |
| OPS-24 | pre-release-check SQL 注入检测过简 | 使用 SonarQube |
| OPS-25 | 硬编码密钥检查不全面 | 使用 git-secrets |
| OPS-26 | 多个 Dockerfile 命名混乱 | 统一为 backend/frontend |
| OPS-27 | 配置中心缺失 | 考虑 Spring Cloud Config |
| OPS-28 | 无蓝绿部署支持 | 补充零停机部署方案 |
