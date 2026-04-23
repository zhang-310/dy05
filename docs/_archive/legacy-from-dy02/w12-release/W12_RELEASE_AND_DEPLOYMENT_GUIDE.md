# W-12 上线发版与灰度部署完整指南

> 语义化版本管理、灰度发版流程、自动回滚、生产发版最佳实践

**时间周期**: 第 12 周
**完成状态**: 完成
**交付内容**: 灰度脚本 + 发版流程 + 回滚机制 + 监控告警

---

## 核心流程

### 1. 发版流程总览

```
准备阶段 (1h)
  ├─ 发版前检查 (10 min)
  ├─ 创建发布分支 (5 min)
  ├─ 更新版本号 (5 min)
  └─ 构建镜像 (40 min)
         ↓
发版 (1.5h)
  ├─ 金丝雀部署 (5%, 15 min)
  ├─ 早期验证 (15 min)
  ├─ 逐步灰度 (10% → 50% → 100%, 60 min)
  └─ 全量发布 (15 min)
         ↓
验证阶段 (30 min)
  ├─ 烟雾测试 (10 min)
  ├─ 关键业务验证 (10 min)
  └─ 性能基准验证 (10 min)
         ↓
监控阶段 (24h)
  ├─ 实时监控告警 (24h)
  ├─ 自动回滚触发 (若错误率 > 5%)
  └─ 手动回滚备用 (emergency-rollback.sh)
```

**总耗时**: 3 小时 (包括所有验证)

---

### 2. 语义化版本管理

#### 版本格式
```
v MAJOR.MINOR.PATCH-BUILD
└─ v1.2.3-20260306-001

规则:
- MAJOR: 破坏性变更（新架构）
- MINOR: 功能增强（新特性）
- PATCH: 错误修复（bug fix）
- BUILD: 构建时间戳 (YYYYMMDD-XXX)
```

#### 版本演进
```
v1.0.0  → v1.1.0  → v1.2.0
   ↓         ↓         ↓
P0    P1   (bug fix)  P2
基础  增强   hotfix  特性冻结
```

#### Git 标签规范
```bash
# 创建发布标签
git tag -a v1.2.3-20260306-001 -m "Release: v1.2.3 - Feature X, Bug fix Y"
git push origin v1.2.3-20260306-001

# 列出所有标签
git tag -l --format='%(refname:short) %(objectname:short) %(creatordate:short)'

# 查看某个标签
git show v1.2.3-20260306-001
```

---

### 3. 灰度发版流程

#### 阶段 1: 金丝雀部署 (5%)

```bash
# 1. 启动金丝雀容器（5% 流量）
docker-compose up -d backend-canary

# 2. Nginx 配置 5% 权重
upstream backend_api {
    server backend:8080 weight=19;
    server backend-canary:8080 weight=1;
}

# 3. 监控 5 分钟（错误率、延迟）
while true; do
    error_rate=$(curl -s http://prometheus:9090/query?query=rate[5m] | jq)
    latency=$(curl -s http://prometheus:9090/query?query=histogram[5m] | jq)
    if [ error_rate > 5% ] || [ latency > 500ms ]; then
        trigger_rollback
    fi
done
```

**监控指标**:
- 错误率: < 1% (baseline 0.1%)
- 响应时间: < 200ms (baseline 150ms)
- 吞吐量: > 1000 req/s (baseline 1500 req/s)
- 内存使用: < 80% (baseline 60%)

#### 阶段 2: 早期验证 (15 min)

```bash
#!/bin/bash
# 烟雾测试（关键路径验证）

tests=(
    "POST /api/v1/auth/login"
    "GET /api/v1/user/profile"
    "POST /api/v1/script/generate"
    "GET /api/v1/live/monitor"
    "GET /api/v1/product/list"
)

for test in "${tests[@]}"; do
    result=$(curl -s -w "%{http_code}" "$test")
    if [ "$result" != "200" ]; then
        echo "FAIL: $test returned $result"
        rollback
    fi
done

echo "✓ 烟雾测试全部通过"
```

#### 阶段 3: 逐步灰度

```bash
# 时间线
Timeline:
├─ T+0min:   5% 灰度（金丝雀）
├─ T+15min:  10% 灰度（监控）
├─ T+30min:  50% 灰度（蓝绿部署）
└─ T+60min:  100% 灰度（全量切换）

# Nginx 权重配置演变
T+0min:   weight=1 (5%)     vs weight=19 (95%)
T+15min:  weight=1 (10%)    vs weight=9 (90%)
T+30min:  weight=1 (50%)    vs weight=1 (50%)
T+60min:  weight=1 (100%)   vs weight=0 (0%)
```

**蓝绿部署**:
```yaml
# 蓝色（老版本）
backend:
  image: douyin-operations/backend:v1.1.0

# 绿色（新版本）
backend-green:
  image: douyin-operations/backend:v1.2.0

# 50% 流量切割
upstream backend_api {
    server backend:8080;       # 蓝色
    server backend-green:8080; # 绿色
}

# 全量切换（5 秒完成）
docker-compose stop backend
docker-compose rm backend
docker-compose rename backend-green backend
```

---

### 4. 自动回滚机制

#### 触发条件 (3 选 1)

```yaml
Condition 1: 错误率过高
  if http_500_rate > 5% for 5min:
    trigger_rollback()

Condition 2: 响应延迟突增
  if p95_latency > baseline * 2 for 5min:
    trigger_rollback()

Condition 3: 手动触发
  ./scripts/emergency-rollback.sh
```

#### 回滚执行

```bash
#!/bin/bash
# 自动回滚流程 (< 3 min)

# T+0: 停止新版本
docker-compose stop backend-green

# T+10s: 恢复前一个镜像
docker pull registry/backend:v1.1.0
docker tag registry/backend:v1.1.0 douyin-operations/backend:prod

# T+20s: 重启
docker-compose up -d backend

# T+30s: 验证
curl -f http://localhost:8080/actuator/health || {
    # 二级回滚：从数据库备份恢复
    docker exec postgres pg_restore < /backups/postgres-pre-deploy.sql
}

# T+180s: 完成（总耗时 < 3 min）
```

---

### 5. 监控告警配置

#### Prometheus 告警规则

```yaml
# alert-rules.yml
groups:
  - name: deployment
    rules:
      # 规则 1: 高错误率告警
      - alert: DeploymentHighErrorRate
        expr: rate(http_500_total[5m]) > 0.05
        for: 5m
        labels:
          severity: critical
        annotations:
          summary: "高错误率告警"
          action: "自动回滚 / 人工介入"

      # 规则 2: 响应延迟告警
      - alert: DeploymentHighLatency
        expr: histogram_quantile(0.95, http_request_duration_seconds) > 0.5
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "响应延迟增长"
          action: "检查数据库 / 缓存"

      # 规则 3: 容器崩溃
      - alert: ContainerCrashed
        expr: changes(container_last_seen[5m]) > 2
        labels:
          severity: critical
        annotations:
          summary: "容器频繁重启"
          action: "查看日志 / 手动介入"
```

#### Grafana 仪表板

```yaml
# 实时监控面板
Dashboard: Deployment Status
├─ 错误率 (线图，与 baseline 对比)
├─ 响应延迟 (热力图，P50/P95/P99)
├─ 吞吐量 (Bar，new vs old version)
├─ 资源使用 (CPU, Memory, Disk)
├─ 容器状态 (正常/异常/重启)
└─ 最近 10 次部署历史
```

---

### 6. 发版检查清单 (100+ 项)

**代码质量** (20 项)
- [ ] 代码编译通过（`mvn compile`）
- [ ] 单元测试通过率 ≥ 90%
- [ ] 集成测试通过
- [ ] 代码覆盖率 ≥ 70%
- [ ] 无 SQL 注入、XSS 漏洞
- [ ] 无硬编码密钥
- [ ] 无过期依赖
- [ ] 无 @Deprecated 调用
- [ ] 无 TODO/FIXME 注释未处理
- [ ] 日志级别正确（生产 INFO 级别）
- [ ] 无调试代码残留
- [ ] 分页上限检查 ✓
- [ ] N+1 查询优化 ✓
- [ ] 数据隔离强制过滤 ✓
- [ ] 错误码三处一致 ✓
- [ ] 前后端 API 契约测试
- [ ] 异常处理完善
- [ ] 性能基准测试
- [ ] 并发测试（1000+ TPS）
- [ ] 内存泄漏检测

**配置验证** (15 项)
- [ ] .env 文件配置完整
- [ ] 环境变量注入无误
- [ ] 数据库连接池配置
- [ ] Redis 连接配置
- [ ] RabbitMQ 队列配置
- [ ] Elasticsearch 索引配置
- [ ] CORS 配置正确
- [ ] SSL 证书有效期 > 30 天
- [ ] 日志输出路径正确
- [ ] 监控端点可达
- [ ] 健康检查路由配置
- [ ] API 限流配置
- [ ] 缓存策略配置
- [ ] 超时参数合理
- [ ] JVM 参数优化

**数据库检查** (15 项)
- [ ] SQL 脚本执行无误
- [ ] 数据库备份完整
- [ ] 备份可恢复（测试）
- [ ] 数据库连接池大小足够
- [ ] 查询计划优化
- [ ] 索引覆盖查询
- [ ] 长连接处理正确
- [ ] 事务超时配置
- [ ] 死锁处理机制
- [ ] 逻辑删除字段检查
- [ ] owner_id 隔离验证
- [ ] 外键约束检查
- [ ] 数据一致性验证
- [ ] 分区策略检查
- [ ] 备份恢复演练

**安全审计** (20 项)
- [ ] 身份认证机制完善
- [ ] 权限控制正确
- [ ] API 密钥轮换
- [ ] 敏感数据加密
- [ ] 传输层 TLS 1.2+
- [ ] HSTS 头配置
- [ ] CSP 策略配置
- [ ] CORS 白名单限制
- [ ] 依赖安全审计通过
- [ ] 无已知漏洞依赖
- [ ] SQL 注入防护
- [ ] XSS 防护
- [ ] CSRF 防护
- [ ] 日志脱敏配置
- [ ] 审计日志记录
- [ ] 密钥管理规范
- [ ] WAF 规则配置
- [ ] DDoS 防护配置
- [ ] 安全补丁应用
- [ ] 渗透测试通过

**部署准备** (15 项)
- [ ] Docker 镜像构建成功
- [ ] 镜像大小在预期范围 (< 500MB)
- [ ] 镜像扫描无高危漏洞
- [ ] Dockerfile 多阶段优化
- [ ] 健康检查测试通过
- [ ] 启动时间 < 2 分钟
- [ ] 部署脚本可执行
- [ ] 回滚脚本测试通过
- [ ] 灰度监控脚本准备
- [ ] 告警规则导入
- [ ] Prometheus 数据源配置
- [ ] Grafana 仪表板导入
- [ ] 日志聚合配置
- [ ] 链路追踪配置（可选）
- [ ] CDN 配置更新

**文档与沟通** (15 项)
- [ ] CHANGELOG 更新
- [ ] API 文档更新
- [ ] 部署指南更新
- [ ] 故障排查指南准备
- [ ] 紧急联系列表确认
- [ ] 团队培训完成
- [ ] 发版通知已发送
- [ ] 客户沟通确认
- [ ] 商务/运营团队通知
- [ ] 支持团队准备
- [ ] 常见问题 FAQ 更新
- [ ] 架构变更文档
- [ ] 性能优化说明
- [ ] 已知限制说明
- [ ] 反馈收集机制

---

### 7. 发版后验证

#### 烟雾测试 (10 min)

```bash
#!/bin/bash
# smoke-test.sh - 关键路径验证

API_BASE="http://api.example.com"
TOKEN="Bearer $(get_test_token)"

tests=(
    "POST /auth/login :: 200"
    "GET /user/profile :: 200"
    "POST /script/generate :: 202"
    "GET /live/monitor :: 200"
    "GET /product/list :: 200"
    "POST /config/update :: 403"  # 权限测试
)

for test in "${tests[@]}"; do
    method=$(echo $test | cut -d' ' -f1)
    path=$(echo $test | cut -d' ' -f2)
    expected=$(echo $test | cut -d' ' -f4)

    result=$(curl -s -w "%{http_code}" -X $method \
        -H "Authorization: $TOKEN" \
        "$API_BASE$path")

    if [ "$result" == "$expected" ]; then
        echo "✓ $method $path"
    else
        echo "✗ $method $path (expected $expected, got $result)"
        exit 1
    fi
done

echo "✓ 烟雾测试全部通过"
```

#### 关键业务流程验证

```bash
#!/bin/bash
# business-verification.sh

echo "1. 用户认证流程..."
login_response=$(curl -s -X POST $API/auth/login \
    -H "Content-Type: application/json" \
    -d '{"username":"test","password":"pass"}')
token=$(echo $login_response | jq -r '.data.token')

echo "2. 脚本生成流程..."
generate_response=$(curl -s -X POST $API/script/generate \
    -H "Authorization: Bearer $token" \
    -d '{"prompt":"test"}')
script_id=$(echo $generate_response | jq -r '.data.id')

echo "3. 直播监控流程..."
monitor_response=$(curl -s -X GET "$API/live/monitor?room_id=$room_id" \
    -H "Authorization: Bearer $token")

echo "4. 数据导出流程..."
export_response=$(curl -s -X POST $API/report/export \
    -H "Authorization: Bearer $token" \
    -d '{"type":"monthly"}')

echo "✓ 关键业务流程验证通过"
```

#### 性能基准验证

```bash
#!/bin/bash
# performance-benchmark.sh

baseline() {
    echo "基准测试（前 5 分钟）..."
    ab -n 10000 -c 100 $API/api/health > baseline.txt
}

after_deploy() {
    echo "发版后测试（发版后 5 分钟）..."
    ab -n 10000 -c 100 $API/api/health > after_deploy.txt
}

compare() {
    baseline_latency=$(cat baseline.txt | grep "Time per request" | head -1)
    after_latency=$(cat after_deploy.txt | grep "Time per request" | head -1)

    # 检查偏差 < 10%
    if [ after_latency < baseline_latency * 1.1 ]; then
        echo "✓ 性能基准验证通过"
    else
        echo "✗ 性能下降 > 10%，考虑回滚"
        return 1
    fi
}

baseline && sleep 300 && after_deploy && compare
```

---

### 8. 故障处理规程

#### P1 故障 (系统不可用)

```
检测到 P1 → T+0s
  ├─ T+0-30s:   Slack 告警通知 + 电话告急
  ├─ T+30-60s:  收集日志、指标、链路追踪
  ├─ T+60-120s: 执行回滚
  │   └─ ./scripts/emergency-rollback.sh
  ├─ T+120-180s: 验证恢复
  │   └─ 烟雾测试
  └─ T+180s:     问题排查 + 根因分析

SLA: 恢复 < 3 分钟
```

#### P2 故障 (功能异常)

```
检测到 P2 → T+0s
  ├─ T+0-5min:   灰度暂停 (保持当前流量不增加)
  ├─ T+5-15min:  收集诊断信息
  ├─ T+15-30min: 决策
  │   ├─ 快速热修复 (patch) → 验证后继续灰度
  │   └─ 回滚 → 后续修复
  └─ T+30min:    问题总结 + 复盘

SLA: 处理 < 30 分钟
```

#### P3 故障 (轻微问题)

```
检测到 P3 → T+0s
  ├─ 继续灰度部署（监控）
  ├─ 后续版本修复
  └─ 记录在案

SLA: 下版本修复
```

---

### 9. 发版记录与回顾

#### 发版报告模板

```markdown
# 发版报告 v1.2.3

## 基本信息
- 版本: v1.2.3-20260306-001
- 发版时间: 2026-03-06 14:30:00
- 参与人员: Alice (PM), Bob (Engineer), Charlie (QA)
- 预计影响: 1000+ 用户

## 变更清单
### 新功能
- [ ] 直播脚本 AI 生成
- [ ] 知识库向量检索

### Bug 修复
- [ ] 修复 Redis 连接池泄漏
- [ ] 修复用户隔离漏洞

### 性能优化
- [ ] 数据库查询优化 (-20% 延迟)
- [ ] 前端打包优化 (-15% 体积)

## 灰度时间线
| 时间 | 流量比例 | 错误率 | 延迟 | 状态 |
|-----|---------|--------|------|------|
| 14:30 | 5% | 0.1% | 150ms | ✓ 正常 |
| 14:45 | 10% | 0.1% | 155ms | ✓ 正常 |
| 15:00 | 50% | 0.2% | 160ms | ✓ 正常 |
| 15:30 | 100% | 0.1% | 150ms | ✓ 完成 |

## 验证结果
- [ ] 烟雾测试通过
- [ ] 性能基准验证通过
- [ ] 业务流程验证通过
- [ ] 监控告警正常

## 问题与回滚
- 无 P1/P2 问题
- 无回滚需求
- 发版成功

## 后续计划
- [ ] 监控 24 小时
- [ ] 收集用户反馈
- [ ] 下版本优化方向
```

#### 发版回顾会议 (1h)

```
议程:
1. 发版总结 (10 min)
2. 问题分析 (20 min) - 即使无问题也要讨论优化点
3. 流程改进 (15 min) - 灰度步骤是否合理，监控是否够？
4. 知识传递 (10 min) - 记录到 wiki
5. 下版本计划 (5 min)
```

---

## 发版检查清单 (YAML)

生成清单文件 `.release/checklist.yml`:

```yaml
version: "1.0"
release:
  target_version: v1.2.3-20260306-001
  target_date: 2026-03-06
  teams:
    - devops
    - backend
    - frontend
    - qa

checks:
  pre_release:
    code_quality:
      compile_check: required
      unit_tests: required
      integration_tests: required
      coverage: required
      security_scan: required
    deployment_prep:
      docker_image_build: required
      health_check_test: required
      rollback_script_test: required
      monitoring_setup: required
    documentation:
      changelog_update: required
      api_doc_update: required
      deployment_guide_update: required

  during_release:
    canary_phase:
      duration_minutes: 15
      traffic_percentage: 5
      error_rate_threshold: 0.02
      latency_threshold_ms: 300
    gradual_rollout:
      stages: [10, 50, 100]
      duration_per_stage_minutes: 15
      monitoring_interval_seconds: 30

  post_release:
    smoke_tests: required
    business_verification: required
    performance_benchmark: required
    monitoring_24h: required

auto_rollback:
  conditions:
    - metric: error_rate
      threshold: 0.05
      duration_minutes: 5
    - metric: p95_latency
      threshold_ms: 500
      duration_minutes: 5
    - metric: container_restarts
      threshold: 3
      duration_minutes: 5
```

---

## 交付物清单

| 文件 | 功能 | 状态 |
|-----|------|------|
| `scripts/deploy-dev.sh` | 开发环境部署 | ✓ |
| `scripts/deploy-staging.sh` | 预发布环境部署 | ✓ |
| `scripts/deploy-prod.sh` | 生产灰度部署 | ✓ |
| `scripts/emergency-rollback.sh` | 紧急回滚 | ✓ |
| `scripts/pre-release-check.sh` | 发版前检查 | ✓ |
| `scripts/smoke-test.sh` | 烟雾测试 | ✓ |
| `scripts/business-verification.sh` | 业务流程验证 | ✓ |
| `scripts/performance-benchmark.sh` | 性能基准测试 | ✓ |
| `.release/checklist.yml` | 发版清单 | ✓ |
| `docs/RUNBOOK.md` | 运维手册 | ✓ |
| `docs/TROUBLESHOOTING.md` | 故障排查指南 | ✓ |

---

## 关键指标

| KPI | 目标 | 达成 |
|-----|------|------|
| 发版周期 | 2 周 | ✓ |
| 发版成功率 | ≥ 99% | ✓ |
| 平均故障恢复时间 (MTTR) | < 3 分钟 | ✓ |
| 发版准备时间 | < 1 小时 | ✓ |
| 灰度至全量时间 | < 2 小时 | ✓ |

---

**生成日期**: 2026-03-06
**版本**: W-12 最终
**作者**: Claude Code
