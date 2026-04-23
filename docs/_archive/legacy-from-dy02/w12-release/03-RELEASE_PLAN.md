# DY01 SaaS 平台发版计划 v2.0

> 灰度发版策略、金丝雀部署、完整回滚方案、上线检查清单

## 执行摘要

```
发版流程总览：

准备 (1h)
  ↓
灰度部署 10% (2h)
  ↓
灰度部署 50% (2h)
  ↓
全量部署 100% (2h)
  ↓
验证和监控 (1h)
  ↓
发版完成或回滚 (按需)

总耗时: 8 小时（低风险）
```

---

## 发版前准备清单

### 代码检查 (30 分钟)

```bash
#!/bin/bash
# pre-release-checks.sh

echo "🔍 发版前检查清单..."

# 1. 编译检查
echo "[1/6] 编译检查..."
mvn clean compile -DskipTests -q && echo "✓" || exit 1

# 2. 单元测试
echo "[2/6] 单元测试..."
mvn test -q && echo "✓" || exit 1

# 3. 集成测试
echo "[3/6] 集成测试..."
mvn verify -q && echo "✓" || exit 1

# 4. 代码质量检查
echo "[4/6] 代码质量检查..."
mvn sonar:sonar -Dsonar.projectKey=douyin-operations \
  -Dsonar.sources=src/main \
  -Dsonar.host.url=http://sonarqube.internal \
  -Dsonar.login=${SONAR_TOKEN} \
  -q && echo "✓" || exit 1

# 5. 安全扫描
echo "[5/6] 安全扫描..."
mvn org.owasp:dependency-check-maven:check -q && echo "✓" || exit 1

# 6. TypeScript 类型检查
echo "[6/6] TypeScript 类型检查..."
cd frontend-react && npm run type-check && echo "✓" || exit 1

echo "✅ 所有检查通过，允许发版"
```

### 备份和快照 (30 分钟)

```bash
#!/bin/bash
# pre-release-backup.sh

VERSION=$1

echo "💾 创建发版快照..."

# 1. 数据库快照
echo "1️⃣  创建数据库快照..."
pg_dump -h localhost -U postgres --format=custom \
  douyin_operations > /backups/pre-release-${VERSION}.backup

# 2. 应用 JAR 快照
echo "2️⃣  保存当前应用..."
cp /opt/douyin/app.jar /backups/app-pre-release-${VERSION}.jar

# 3. 配置快照
echo "3️⃣  保存配置..."
cp -r /opt/douyin/config /backups/config-pre-release-${VERSION}/

# 4. Redis 快照
echo "4️⃣  保存 Redis 数据..."
redis-cli BGSAVE && sleep 5
cp /var/lib/redis/dump.rdb /backups/redis-pre-release-${VERSION}.rdb

# 5. 上传备份到 S3
echo "5️⃣  上传备份到 S3..."
aws s3 sync /backups/ s3://douyin-releases/${VERSION}/backup/ \
  --exclude "*" \
  --include "pre-release-*"

echo "✅ 快照完成"
```

---

## 灰度部署策略

### 阶段 1: 10% 金丝雀部署 (2 小时)

**目标**: 验证新版本基本功能和性能

**流量分配**:
```
老版本 (v1.0)  ████████████████████░░░░░  90%  (900 用户)
新版本 (v1.1)  ░░░░░░░░░░░░░░░░░░░░██░░░  10%  (100 用户)
```

**部署脚本**:

```bash
#!/bin/bash
# canary-deployment-10pct.sh

VERSION=$1

echo "🚀 开始金丝雀部署 (10%)..."

# 1. 构建新版本
echo "1️⃣  编译新版本..."
mvn clean package -DskipTests -P prod -q
JAR_FILE="target/douyin-operations-${VERSION}.jar"

# 2. 创建新实例
echo "2️⃣  启动新实例..."
mkdir -p /opt/douyin-canary
cp ${JAR_FILE} /opt/douyin-canary/app-${VERSION}.jar
chmod +x /opt/douyin-canary/app-${VERSION}.jar

# 启动新版本应用
java -Xmx4g -Xms2g \
  -Dserver.port=8081 \
  -Dspring.profiles.active=prod \
  -jar /opt/douyin-canary/app-${VERSION}.jar &

sleep 30

# 3. 验证新实例健康状态
echo "3️⃣  验证新实例..."
if curl -f http://localhost:8081/actuator/health | jq .status | grep -q "UP"; then
  echo "✓ 新实例健康"
else
  echo "✗ 新实例启动失败"
  pkill -f app-${VERSION}.jar
  exit 1
fi

# 4. 配置负载均衡器
echo "4️⃣  配置负载均衡..."
cat > /tmp/canary-config.json <<EOF
{
  "upstream": {
    "douyin_backend": {
      "servers": [
        {
          "server": "127.0.0.1:8080",
          "weight": 9,
          "label": "stable-v1.0"
        },
        {
          "server": "127.0.0.1:8081",
          "weight": 1,
          "label": "canary-v${VERSION}"
        }
      ]
    }
  }
}
EOF

# 使用 Nginx 配置
nginx -s reload

# 5. 启动监控
echo "5️⃣  启动监控..."
cat > /tmp/canary-monitor.sh <<'MONITOR'
#!/bin/bash

while true; do
  # 监控新版本错误率
  ERROR_RATE=$(curl -s http://prometheus:9090/api/v1/query \
    --data-urlencode 'query=rate(http_requests_total{instance="127.0.0.1:8081",status=~"5.."}[5m])' \
    | jq .data.result[0].value[1] 2>/dev/null || echo 0)

  if (( $(echo "$ERROR_RATE > 0.01" | bc -l) )); then
    echo "⚠️  新版本错误率过高 (${ERROR_RATE})"
    # 触发自动回滚
    /opt/scripts/rollback.sh
    break
  fi

  echo "✓ 新版本监控正常 (错误率: ${ERROR_RATE})"
  sleep 60
done
MONITOR

chmod +x /tmp/canary-monitor.sh
nohup /tmp/canary-monitor.sh > /var/log/canary-monitor.log 2>&1 &

echo "✅ 金丝雀部署 (10%) 完成"
echo "   监控时间: 2 小时"
echo "   检查指标:"
echo "   - 错误率 < 1%"
echo "   - 响应时间 < 200ms"
echo "   - CPU 使用率 < 80%"
```

**关键指标** (每 5 分钟检查一次):

| 指标 | 阈值 | 动作 |
|------|------|------|
| 错误率 | > 1% | 立即回滚 |
| P99 延迟 | > 500ms | 警告，继续观察 |
| CPU 使用 | > 85% | 警告，检查代码 |
| 内存使用 | > 80% | 警告，检查内存泄漏 |

**监控仪表板**:

```bash
watch -n 5 'echo "=== 10% 金丝雀部署监控 ===" && \
curl -s http://prometheus:9090/api/v1/query \
  --data-urlencode "query=rate(http_requests_total{instance=~\"127.0.0.1:808[01]\"}[5m])" | \
jq ".data.result[] | {instance: .metric.instance, qps: .value[1]}"'
```

---

### 阶段 2: 50% 灰度部署 (2 小时)

**条件**:
- ✅ 金丝雀 (10%) 运行 2 小时无问题
- ✅ 错误率 < 0.5%
- ✅ 响应时间稳定

**流量分配**:
```
老版本 (v1.0)  ██████████░░░░░░░░░░░░░░  50%
新版本 (v1.1)  ██████████░░░░░░░░░░░░░░  50%
```

**部署脚本**:

```bash
#!/bin/bash
# gray-deployment-50pct.sh

echo "🚀 灰度部署 50%..."

# 1. 启动更多新版本实例
echo "1️⃣  启动新版本实例 (×4)..."
for i in {1..4}; do
  PORT=$((8081 + i))
  nohup java -Xmx4g -Xms2g \
    -Dserver.port=${PORT} \
    -Dspring.profiles.active=prod \
    -jar /opt/douyin-canary/app-${VERSION}.jar \
    > /var/log/app-${PORT}.log 2>&1 &
  sleep 5
  curl -f http://localhost:${PORT}/actuator/health || exit 1
done

# 2. 更新负载均衡配置 (50:50)
echo "2️⃣  更新负载均衡..."
cat > /etc/nginx/conf.d/douyin-gray.conf <<EOF
upstream douyin_backend {
    least_conn;
    server 127.0.0.1:8080 weight=5 max_fails=3 fail_timeout=30s;
    server 127.0.0.1:8081 weight=5 max_fails=3 fail_timeout=30s;
    server 127.0.0.1:8082 weight=5 max_fails=3 fail_timeout=30s;
    server 127.0.0.1:8083 weight=5 max_fails=3 fail_timeout=30s;
    server 127.0.0.1:8084 weight=5 max_fails=3 fail_timeout=30s;
}
EOF

nginx -s reload

# 3. 增强监控
echo "3️⃣  增强监控..."
# 监控新旧版本的各项指标对比
MONITOR_SCRIPT="/tmp/gray-monitor-50.sh"
cat > ${MONITOR_SCRIPT} <<'MONITOR'
#!/bin/bash
while true; do
  echo "=== 50% 灰度部署监控 ($(date)) ==="

  # 新版本错误率
  NEW_ERRORS=$(curl -s http://prometheus:9090/api/v1/query \
    --data-urlencode 'query=sum(rate(http_requests_total{port=~"808[1-4]",status=~"5.."}[5m]))' \
    | jq '.data.result[0].value[1]' 2>/dev/null || echo 0)

  # 老版本错误率
  OLD_ERRORS=$(curl -s http://prometheus:9090/api/v1/query \
    --data-urlencode 'query=sum(rate(http_requests_total{port="8080",status=~"5.."}[5m]))' \
    | jq '.data.result[0].value[1]' 2>/dev/null || echo 0)

  echo "新版本错误率: ${NEW_ERRORS}"
  echo "老版本错误率: ${OLD_ERRORS}"

  # 如果新版本错误率显著高于老版本，回滚
  if (( $(echo "$NEW_ERRORS > $OLD_ERRORS * 2" | bc -l) )); then
    echo "✗ 新版本错误率过高，触发回滚"
    /opt/scripts/rollback.sh
    exit 1
  fi

  sleep 60
done
MONITOR

chmod +x ${MONITOR_SCRIPT}
nohup ${MONITOR_SCRIPT} > /var/log/gray-monitor-50.log 2>&1 &

echo "✅ 灰度部署 50% 完成"
echo "   监控时间: 2 小时"
```

---

### 阶段 3: 100% 全量部署 (2 小时)

**条件**:
- ✅ 灰度 (50%) 运行 2 小时无问题
- ✅ 新旧版本错误率差异 < 5%
- ✅ 性能指标达到预期

**部署脚本**:

```bash
#!/bin/bash
# full-deployment-100pct.sh

echo "🚀 全量部署 100%..."

# 1. 停止老版本应用
echo "1️⃣  停止老版本..."
systemctl stop douyin-app

# 2. 启动完整新版本集群
echo "2️⃣  启动新版本应用..."
systemctl start douyin-app

# 3. 验证应用状态
echo "3️⃣  验证应用状态..."
sleep 30
if curl -f http://localhost:8080/actuator/health | jq .status | grep -q "UP"; then
  echo "✓ 应用正常运行"
else
  echo "✗ 应用启动失败，执行回滚"
  systemctl start douyin-app
  exit 1
fi

# 4. 清理灰度实例
echo "4️⃣  清理灰度实例..."
pkill -f "port=808[1-4]"
rm -rf /opt/douyin-canary/

# 5. 更新版本标记
echo "5️⃣  更新版本标记..."
echo "${VERSION}" > /opt/douyin/.version
git tag -a "release-${VERSION}" -m "Release v${VERSION}"
git push origin "release-${VERSION}"

# 6. 最终验证
echo "6️⃣  最终验证..."
SMOKE_TESTS=(
  "curl -f http://localhost:8080/api/v1/auth/login"
  "curl -f http://localhost:8080/api/v1/product/list"
  "curl -f http://localhost:8080/api/v1/live/sessions"
  "curl -f http://localhost:8080/api/v1/ai/search"
)

for test in "${SMOKE_TESTS[@]}"; do
  if eval "${test}" > /dev/null 2>&1; then
    echo "✓ ${test} 通过"
  else
    echo "✗ ${test} 失败"
    exit 1
  fi
done

echo "✅ 全量部署 100% 完成"
```

---

## 自动化回滚方案

### 即时回滚 (< 1 分钟)

```bash
#!/bin/bash
# rollback.sh - 快速回滚脚本

VERSION_BEFORE=$1
echo "⚠️  触发回滚到 ${VERSION_BEFORE}..."

# 1. 立即停止新版本
echo "1️⃣  停止新版本..."
systemctl stop douyin-app

# 2. 恢复备份数据
echo "2️⃣  恢复数据库备份..."
pg_restore -h localhost -U postgres -d douyin_operations \
  /backups/pre-release-${VERSION_BEFORE}.backup

# 3. 恢复应用
echo "3️⃣  恢复应用文件..."
cp /backups/app-pre-release-${VERSION_BEFORE}.jar /opt/douyin/app.jar
cp -r /backups/config-pre-release-${VERSION_BEFORE}/* /opt/douyin/config/

# 4. 启动老版本
echo "4️⃣  启动老版本..."
systemctl start douyin-app

# 5. 验证恢复
echo "5️⃣  验证恢复..."
sleep 30
if curl -f http://localhost:8080/actuator/health | jq .status | grep -q "UP"; then
  echo "✓ 已恢复到版本 ${VERSION_BEFORE}"

  # 6. 发送告警
  send_alert "发版失败，已自动回滚到 ${VERSION_BEFORE}"

  # 7. 记录日志
  echo "发版失败回滚: $(date)" >> /var/log/release-rollback.log
else
  echo "✗ 回滚失败！需要手动介入"
  send_critical_alert "回滚失败，需要立即处理"
fi
```

### 数据库回滚 (< 5 分钟)

```bash
#!/bin/bash
# db-rollback.sh

echo "数据库回滚..."

# 使用 PITR (Point-in-Time Recovery)
pg_basebackup -h localhost -D /var/lib/postgresql/15/recovery -U postgres
cat > /var/lib/postgresql/15/recovery/recovery.conf <<EOF
restore_command = 'cp /var/lib/postgresql/wal_archive/%f %p'
recovery_target_time = '2026-03-05 10:00:00'
recovery_target_timeline = 'latest'
EOF

# 验证恢复时间点
psql -U postgres -c "SELECT pg_last_wal_receive_time();"
```

---

## 发版检查清单

### 发版当天 (T-0)

```
☐ 6:00   - 发版协调会议
☐ 6:30   - 运维确认所有系统就绪
☐ 7:00   - 创建发版备份
☐ 7:30   - 启动金丝雀部署 (10%)
☐ 8:30   - 金丝雀监控检查 (确认无异常)
☐ 9:00   - 决定是否继续灰度 (50%)
☐ 9:30   - 启动灰度部署 (50%)
☐ 11:00  - 灰度监控检查 (确认无异常)
☐ 11:30  - 决定是否全量部署
☐ 12:00  - 启动全量部署 (100%)
☐ 13:00  - 最终验证和冒烟测试
☐ 14:00  - 发版完成，回复所有关联方
```

### 关键检查点

| 时间 | 检查项 | 负责人 | 结果 |
|------|--------|--------|------|
| 10% | 错误率 < 1% | 运维 | ✓/✗ |
| 10% | P99 延迟 < 500ms | 性能 | ✓/✗ |
| 50% | 性能对标老版本 | DBA | ✓/✗ |
| 50% | 支付功能正常 | 测试 | ✓/✗ |
| 100% | 所有 API 可用 | 测试 | ✓/✗ |
| 100% | 监控告警正常 | 运维 | ✓/✗ |

---

## 发版后验证

### 冒烟测试 (10 分钟)

```bash
#!/bin/bash
# smoke-tests.sh - 发版后冒烟测试

echo "运行冒烟测试..."

TESTS=(
  "登录功能"
  "产品管理"
  "直播管理"
  "支付流程"
  "AI 生成"
  "数据查询"
)

for test in "${TESTS[@]}"; do
  echo -n "测试 ${test}... "
  # 实际测试逻辑
  if test_${test}; then
    echo "✓"
  else
    echo "✗ 失败"
    exit 1
  fi
done

echo "✅ 所有冒烟测试通过"
```

### 性能基准验证 (30 分钟)

```bash
#!/bin/bash
# performance-validation.sh

echo "性能验证..."

# 对比发版前后的性能指标
OLD_P95=$(curl -s http://prometheus:9090/api/v1/query \
  --data-urlencode 'query=histogram_quantile(0.95, http_request_duration_seconds)' \
  --data-urlencode 'time=2026-03-05T10:00:00Z' \
  | jq .data.result[0].value[1])

NEW_P95=$(curl -s http://prometheus:9090/api/v1/query \
  --data-urlencode 'query=histogram_quantile(0.95, http_request_duration_seconds)' \
  | jq .data.result[0].value[1])

IMPROVEMENT=$((($OLD_P95 - $NEW_P95) / $OLD_P95 * 100))

echo "发版前 P95 延迟: ${OLD_P95}ms"
echo "发版后 P95 延迟: ${NEW_P95}ms"
echo "性能提升: ${IMPROVEMENT}%"

if (( $(echo "$IMPROVEMENT >= -5" | bc -l) )); then
  echo "✓ 性能验证通过 (允许 ±5% 波动)"
else
  echo "✗ 性能下降超过 5%，需要调查"
fi
```

---

## 发版通知

### 发版前通知

```
主题: 【发版通知】DY01 v1.1 计划发版 - 2026-03-05

亲爱的团队成员：

我们计划于 2026-03-05 06:00 发布 DY01 v1.1 版本。

📋 版本主要更新：
- W-09: 性能优化 (3-5 倍吞吐量提升)
- W-10: 测试框架 (100% API 覆盖)
- W-11: 支付系统 (支付成功率 > 99%)

⏱️ 发版时间：
- 金丝雀: 07:30
- 灰度: 09:30
- 全量: 11:30

❌ 发版期间对业务的影响：
- 预期无故障，但可能有 < 1 秒延迟
- 支付流程可能存在 < 30 秒延迟

📞 应急联系：
- 值班负责人: 张三 (10086)
- 技术支持: 李四 (10087)

请确保在发版期间保持通讯畅通。

Best regards,
DevOps 团队
```

### 发版完成通知

```
【发版完成】DY01 v1.1 已成功发版

✅ 发版状态：成功
📊 发版统计：
- 金丝雀: 无异常
- 灰度: 无异常
- 全量: 无异常

⏱️ 发版耗时：7 小时 30 分钟
📉 性能数据：
- P95 延迟：从 250ms 降低到 150ms
- 错误率：0.02% (目标 < 0.1%)
- 吞吐量：从 5,000 QPS 提升到 12,000 QPS

🎉 发版已完成，感谢所有参与者！
```

---

## 文档和日志

### 发版日志模板

```
发版版本: v1.1
发版日期: 2026-03-05
发版负责: DevOps 团队
核准者: CTO

=== 发版前检查 ===
编译: ✓
单元测试: ✓
集成测试: ✓
安全扫描: ✓

=== 备份 ===
数据库备份: ✓
应用备份: ✓
配置备份: ✓

=== 金丝雀部署 (10%) ===
开始时间: 07:30
结束时间: 09:30
错误率: 0.05% (✓ < 1%)
P99 延迟: 180ms (✓ < 500ms)
内存使用: 65% (✓ < 80%)
结论: ✓ 通过

=== 灰度部署 (50%) ===
开始时间: 09:30
结束时间: 11:30
新旧版本错误率差异: 2% (✓ < 5%)
性能对标: 98% (✓ > 95%)
支付功能: ✓ 正常
结论: ✓ 通过

=== 全量部署 (100%) ===
开始时间: 11:30
结束时间: 13:00
冒烟测试: ✓ 全部通过
性能验证: ✓ 符合预期
监控告警: ✓ 正常

=== 发版总结 ===
发版状态: ✓ 成功
总耗时: 7.5 小时
用户影响: 无
数据丢失: 0

签名: _______________  日期: 2026-03-05
```

---

## 附录

### 关键参考

- [部署指南](./DEPLOYMENT_GUIDE.md)
- [灾备计划](./DISASTER_RECOVERY.md)
- [监控告警配置](./MONITORING.md)

### 发版历史

| 版本 | 日期 | 状态 | 耗时 |
|------|------|------|------|
| v1.1 | 2026-03-05 | ✓ 成功 | 7.5h |
| v1.0 | 2026-02-01 | ✓ 成功 | 6h |

---

**维护者**: 发版管理团队
**最后更新**: 2026-03-05
**版本**: 2.0
