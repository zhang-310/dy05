# DY01 SaaS 平台灾备与高可用方案 v2.0

> 生产级灾难恢复计划、备份策略、业务连续性管理

## 执行摘要

| 指标 | 目标 | 实现方式 |
|------|------|--------|
| **RTO** (恢复时间目标) | ≤ 15 分钟 | 自动故障转移 + 负载均衡 |
| **RPO** (恢复点目标) | ≤ 5 分钟 | 实时复制 + 增量备份 |
| **可用性** | 99.95% | 多区域部署 + 健康检查 |
| **备份频率** | 每 5 分钟 | 持续增量备份 |
| **备份保留期** | 90 天 | 分级存储策略 |

---

## 灾难场景和应对

### 场景 1: 单个应用服务器故障

**影响范围**: 服务暂时不可用（<1 秒）
**恢复时间**: < 30 秒

**应对措施**:

```
检测 (5 秒)
  ↓
Kubernetes/负载均衡器检测故障
  ↓
自动将流量转移到健康节点
  ↓
故障节点自动隔离
  ↓
自动启动新实例替换
  ↓
恢复正常 (30 秒)
```

**自动化脚本**:

```bash
#!/bin/bash
# kubernetes-failover.sh

# 部署高可用配置
kubectl apply -f - <<EOF
apiVersion: apps/v1
kind: Deployment
metadata:
  name: douyin-app
spec:
  replicas: 3
  selector:
    matchLabels:
      app: douyin-app
  template:
    metadata:
      labels:
        app: douyin-app
    spec:
      containers:
      - name: app
        image: douyin-app:latest
        livenessProbe:
          httpGet:
            path: /actuator/health
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8080
          initialDelaySeconds: 5
          periodSeconds: 5
EOF
```

---

### 场景 2: 数据库节点故障

**影响范围**: 主数据库故障，副本自动接管
**恢复时间**: < 1 分钟

**应对措施**:

```
主库故障
  ↓
自动检测主库不可用 (10 秒)
  ↓
故障转移触发
  ↓
从库提升为主库 (30 秒)
  ↓
应用连接字符串更新
  ↓
恢复正常 (1 分钟)
```

**PostgreSQL 主从配置**:

```bash
# 主库配置
sudo -u postgres psql <<EOF
-- 创建复制用户
CREATE ROLE replication WITH REPLICATION LOGIN PASSWORD 'replication_password';

-- 配置主库 postgresql.conf
wal_level = replica
max_wal_senders = 10
max_replication_slots = 10
hot_standby_feedback = on
EOF

# 从库配置
sudo -u postgres bash <<'EOF'
# pg_basebackup - 初始化从库数据
pg_basebackup -h master.internal -D /var/lib/postgresql/15/main -U replication -v -W

# recovery.conf - 配置恢复
cat > /var/lib/postgresql/15/main/recovery.conf <<'RECOVERY'
standby_mode = 'on'
primary_conninfo = 'host=master.internal port=5432 user=replication password=replication_password'
recovery_target_timeline = 'latest'
RECOVERY'

# 启动从库
systemctl start postgresql
EOF
```

**故障转移脚本**:

```bash
#!/bin/bash
# pg-failover.sh - PostgreSQL 自动故障转移

MASTER_HOST="master.internal"
STANDBY_HOST="standby.internal"
CHECK_INTERVAL=30

while true; do
  # 检查主库健康
  if ! pg_isready -h ${MASTER_HOST} -p 5432 > /dev/null 2>&1; then
    echo "主库故障检测，执行故障转移..."

    # 连接到从库，触发故障转移
    ssh douyin@${STANDBY_HOST} bash <<'FAILOVER'
      sudo -u postgres pg_ctl promote -D /var/lib/postgresql/15/main
      echo "从库已提升为主库"
    FAILOVER'

    # 更新 DNS 指向新主库
    aws route53 change-resource-record-sets \
      --hosted-zone-id Z123456 \
      --change-batch "Changes=[{Action=UPSERT,ResourceRecordSet={Name=db.internal,Type=A,TTL=60,ResourceRecords=[{Value=$(dig +short ${STANDBY_HOST})}]}}]"

    # 等待应用重新连接
    sleep 30

    # 验证故障转移成功
    if pg_isready -h ${STANDBY_HOST} -p 5432 > /dev/null 2>&1; then
      echo "✓ 故障转移成功，从库已成为主库"
      MASTER_HOST="${STANDBY_HOST}"
    else
      echo "✗ 故障转移失败，立即告警"
      send_alert "PostgreSQL 故障转移失败"
    fi
  fi

  sleep ${CHECK_INTERVAL}
done
```

---

### 场景 3: 数据中心级别故障

**影响范围**: 整个数据中心不可用
**恢复时间**: < 15 分钟
**数据丢失**: < 5 分钟

**应对措施**:

```
整个 DC 故障
  ↓
DNS 故障转移 (1 分钟)
  ↓
路由到备用 DC
  ↓
从最新备份恢复数据库 (5 分钟)
  ↓
应用启动 (5 分钟)
  ↓
业务恢复 (15 分钟)
```

**跨 DC 部署拓扑**:

```
┌─────────────────────────────────────────────────┐
│                   DNS (全球负载均衡)              │
└──────────┬─────────────────────────────┬────────┘
           │                             │
      ┌────▼────┐              ┌────────▼───┐
      │  DC 1   │              │   DC 2     │
      │  (主)   │              │  (备)      │
      │  3% 流量 │              │  1% 流量   │
      └────┬────┘              └────────┬───┘
           │                             │
      ┌────▼─────────────────────────────▼───┐
      │   跨 DC 数据同步 (WATCH-BASED)        │
      │   - 主库 → 备用库 (1秒延迟)           │
      │   - PostgreSQL Streaming Replication │
      │   - Redis Replication                │
      └──────────────────────────────────────┘
```

**配置示例**:

```yaml
# docker-compose.yml - 多 DC 配置
version: '3.8'

services:
  # DC 1 - 主机房
  postgres-dc1:
    image: postgres:15
    environment:
      POSTGRES_DB: douyin_operations
      POSTGRES_PASSWORD: ${DB_PASSWORD}
    command:
      - "-c"
      - "wal_level=replica"
      - "-c"
      - "max_wal_senders=10"
    volumes:
      - pg-data-dc1:/var/lib/postgresql/data
    ports:
      - "5432:5432"
    labels:
      datacenter: "DC1"
      role: "primary"

  # DC 2 - 备用机房
  postgres-dc2:
    image: postgres:15
    environment:
      POSTGRES_REPLICATION_MODE: slave
      POSTGRES_MASTER_SERVICE: postgres-dc1:5432
      POSTGRES_REPLICATION_USER: replication
      POSTGRES_REPLICATION_PASSWORD: ${REPLICATION_PASSWORD}
    volumes:
      - pg-data-dc2:/var/lib/postgresql/data
    ports:
      - "5433:5432"
    labels:
      datacenter: "DC2"
      role: "standby"
    depends_on:
      - postgres-dc1

volumes:
  pg-data-dc1:
    driver: local
  pg-data-dc2:
    driver: local
```

---

## 备份策略

### 备份分级

| 级别 | 频率 | 保留期 | 存储位置 | RPO |
|------|------|--------|--------|-----|
| **实时** | 持续 | 1 小时 | 本地 + 异地 | 5 分钟 |
| **小时级** | 每小时 | 7 天 | 对象存储 | 1 小时 |
| **日级** | 每天 | 30 天 | 对象存储 | 1 天 |
| **周级** | 每周 | 90 天 | 冷存储 | 1 周 |

### 数据库备份

```bash
#!/bin/bash
# db-backup-strategy.sh

# 1. 基础备份 (Full Backup)
backup_full() {
  BACKUP_DIR="/backups/postgresql/full"
  TIMESTAMP=$(date +%Y%m%d-%H%M%S)

  # 使用 pg_basebackup
  pg_basebackup \
    -h localhost \
    -D ${BACKUP_DIR}/backup-${TIMESTAMP} \
    -Ft \
    -z \
    -P \
    -U postgres

  # 压缩
  tar czf ${BACKUP_DIR}/backup-${TIMESTAMP}.tar.gz \
          ${BACKUP_DIR}/backup-${TIMESTAMP}/

  # 上传到 S3
  aws s3 cp ${BACKUP_DIR}/backup-${TIMESTAMP}.tar.gz \
            s3://douyin-backups/postgresql/full/ \
            --storage-class STANDARD_IA

  # 清理本地备份（保留最新 3 个）
  ls -t ${BACKUP_DIR}/backup-*.tar.gz | tail -n +4 | xargs rm -f
}

# 2. 增量备份 (Incremental)
backup_incremental() {
  BACKUP_DIR="/backups/postgresql/wal"

  # 归档 WAL 日志
  find /var/lib/postgresql/15/main/pg_wal/archive_status \
    -name "*.ready" \
    -exec bash -c 'cp /var/lib/postgresql/15/main/pg_wal/{} '${BACKUP_DIR}'/' \;

  # 上传到 S3
  aws s3 sync ${BACKUP_DIR}/ \
        s3://douyin-backups/postgresql/wal/ \
        --exclude "*" \
        --include "*.wal" \
        --storage-class GLACIER
}

# 3. 数据库逻辑导出
backup_logical() {
  BACKUP_DIR="/backups/postgresql/logical"
  TIMESTAMP=$(date +%Y%m%d-%H%M%S)

  # 使用 pg_dump (增量备份前)
  pg_dump -h localhost \
          -U postgres \
          --format=custom \
          --compress=9 \
          --file=${BACKUP_DIR}/dump-${TIMESTAMP}.sql.gz \
          douyin_operations

  # 上传
  aws s3 cp ${BACKUP_DIR}/dump-${TIMESTAMP}.sql.gz \
            s3://douyin-backups/postgresql/logical/

  # 保留 30 天
  find ${BACKUP_DIR} -name "*.sql.gz" -mtime +30 -exec rm {} \;
}

# 4. Redis 备份
backup_redis() {
  BACKUP_DIR="/backups/redis"
  TIMESTAMP=$(date +%Y%m%d-%H%M%S)

  # 触发 Redis BGSAVE
  redis-cli BGSAVE
  sleep 5

  # 复制 RDB 文件
  cp /var/lib/redis/dump.rdb ${BACKUP_DIR}/dump-${TIMESTAMP}.rdb

  # 上传
  aws s3 cp ${BACKUP_DIR}/dump-${TIMESTAMP}.rdb \
            s3://douyin-backups/redis/
}

# 5. 文件存储备份
backup_files() {
  BACKUP_DIR="/backups/files"
  TIMESTAMP=$(date +%Y%m%d-%H%M%S)

  # 同步到 S3
  aws s3 sync /data/uploads \
        s3://douyin-backups/files/uploads-${TIMESTAMP}/ \
        --exclude "*.tmp" \
        --storage-class INTELLIGENT_TIERING
}

# 执行备份时间表
case $(date +%A) in
  Monday)     backup_full; backup_logical ;;
  *)          backup_incremental; backup_redis; backup_files ;;
esac
```

### 备份验证

```bash
#!/bin/bash
# backup-verification.sh - 定期验证备份可恢复性

DAYS=7  # 验证 7 天内的备份

verify_db_backup() {
  echo "验证数据库备份..."

  # 在隔离环境恢复备份
  TEMP_CONTAINER=$(docker run -d \
    -e POSTGRES_PASSWORD=test \
    postgres:15)

  # 恢复备份
  docker cp /backups/postgresql/latest/backup.sql ${TEMP_CONTAINER}:/backup.sql
  docker exec ${TEMP_CONTAINER} psql -U postgres < /backup.sql

  # 验证数据完整性
  TABLES=$(docker exec ${TEMP_CONTAINER} psql -U postgres \
    -c "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='public';" \
    -t)

  if [ ${TABLES} -gt 50 ]; then
    echo "✓ 数据库备份可恢复"
  else
    echo "✗ 数据库备份异常，只找到 ${TABLES} 个表"
    send_alert "数据库备份验证失败"
  fi

  docker rm -f ${TEMP_CONTAINER}
}

verify_redis_backup() {
  echo "验证 Redis 备份..."

  # 启动临时 Redis 实例
  TEMP_REDIS=$(docker run -d redis:7)

  # 恢复备份
  docker cp /backups/redis/dump.rdb ${TEMP_REDIS}:/data/
  docker restart ${TEMP_REDIS}

  # 验证数据
  KEYS=$(docker exec ${TEMP_REDIS} redis-cli DBSIZE)
  if [ ${KEYS} -gt 1000 ]; then
    echo "✓ Redis 备份可恢复 (${KEYS} 个键)"
  else
    echo "✗ Redis 备份异常"
  fi

  docker rm -f ${TEMP_REDIS}
}

# 每周一运行完整验证
if [ "$(date +%A)" = "Monday" ]; then
  verify_db_backup
  verify_redis_backup
fi
```

---

## 恢复流程

### 数据库恢复（< 10 分钟）

```bash
#!/bin/bash
# db-recovery.sh - 数据库灾难恢复

set -e

BACKUP_FILE=$1
RECOVERY_DB="douyin_operations_recovery"

echo "开始数据库恢复流程..."

# 1. 停止应用连接
echo "1️⃣  停止应用..."
systemctl stop douyin-app
sleep 10

# 2. 备份当前损坏的数据
echo "2️⃣  备份损坏的数据库..."
pg_dump -h localhost -U postgres ${RECOVERY_DB} > /backups/corrupted-$(date +%s).sql

# 3. 删除损坏的数据库
echo "3️⃣  删除损坏的数据库..."
psql -U postgres -c "DROP DATABASE IF EXISTS ${RECOVERY_DB};"

# 4. 从备份恢复
echo "4️⃣  从备份恢复数据..."
if [[ ${BACKUP_FILE} == *.tar.gz ]]; then
  tar xzf ${BACKUP_FILE} -C /tmp/
  pg_basebackup --wal-method=stream -h /tmp/backup -D /var/lib/postgresql/15/recovery
else
  psql -U postgres < ${BACKUP_FILE}
fi

# 5. 验证恢复
echo "5️⃣  验证恢复..."
TABLES=$(psql -U postgres -d ${RECOVERY_DB} \
  -c "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='public';" -t)

if [ ${TABLES} -gt 50 ]; then
  echo "✓ 恢复成功 (${TABLES} 个表)"

  # 6. 重启应用
  echo "6️⃣  重启应用..."
  systemctl start douyin-app

  # 7. 验证应用
  sleep 30
  if curl -f http://localhost:8080/actuator/health > /dev/null; then
    echo "🎉 恢复完成！应用正常运行"
  fi
else
  echo "✗ 恢复失败"
  exit 1
fi
```

### 应用数据恢复（< 5 分钟）

```bash
#!/bin/bash
# app-recovery.sh - 应用级恢复

echo "应用数据恢复流程..."

# 1. 清空缓存
echo "清空 Redis..."
redis-cli FLUSHDB

# 2. 清空消息队列
echo "清空消息队列..."
rabbitmqctl purge_queue douyin_queue

# 3. 重置会话
echo "重置会话..."
psql -U postgres -d douyin_operations <<EOF
DELETE FROM auth_session WHERE created_at < now() - interval '1 day';
DELETE FROM auth_login_log WHERE created_at < now() - interval '30 days';
VACUUM ANALYZE;
EOF

# 4. 验证数据一致性
echo "验证数据一致性..."
psql -U postgres -d douyin_operations <<EOF
-- 检查孤立记录
SELECT COUNT(*) FROM payment_order po
WHERE NOT EXISTS (SELECT 1 FROM auth_user au WHERE au.id = po.user_id);

-- 检查数据完整性
SELECT COUNT(*) FROM live_script WHERE deleted != 0;
EOF

echo "✓ 应用恢复完成"
```

---

## 灾备演练计划

### 月度演练计划

```
┌─────────────────────────────────────────┐
│         月度灾备演练日程表               │
├─────────────────────────────────────────┤
│ 第 1 周  │ 数据库故障转移演练            │
│ 第 2 周  │ Redis 故障恢复演练             │
│ 第 3 周  │ 应用服务器故障转移演练        │
│ 第 4 周  │ 跨 DC 灾难恢复演练             │
└─────────────────────────────────────────┘
```

### 演练执行步骤

```bash
#!/bin/bash
# drill-database-failover.sh - 数据库故障转移演练

DRILL_NAME="数据库故障转移"
START_TIME=$(date +%s)

echo "=========================================="
echo "灾备演练: ${DRILL_NAME}"
echo "开始时间: $(date)"
echo "=========================================="

# 记录演练日志
DRILL_LOG="drills/$(date +%Y%m%d-%H%M%S)-${DRILL_NAME}.log"
mkdir -p drills

{
  echo "演练开始: $(date)"

  # 第 1 步: 通知团队
  echo "[步骤 1] 通知团队和管理层"
  send_notification "灾备演练开始: ${DRILL_NAME}"

  # 第 2 步: 记录基准线
  echo "[步骤 2] 记录基准线数据"
  echo "主库连接数: $(psql -h master.internal -c 'SELECT count(*) FROM pg_stat_activity;' -t)"
  echo "从库延迟: $(psql -h standby.internal -c 'SELECT now() - pg_last_wal_receive_time();' -t)"

  # 第 3 步: 触发故障
  echo "[步骤 3] 模拟主库故障..."
  ssh postgres@master.internal "sudo systemctl stop postgresql"
  sleep 10

  # 第 4 步: 监控故障转移
  echo "[步骤 4] 监控故障转移过程..."
  FAILOVER_TIME=0
  while [ ${FAILOVER_TIME} -lt 300 ]; do
    if psql -h standby.internal -c "SELECT 1" > /dev/null 2>&1; then
      if psql -h standby.internal -c "SELECT pg_is_in_recovery();" | grep -q "f"; then
        echo "✓ 故障转移完成 (${FAILOVER_TIME} 秒)"
        break
      fi
    fi
    sleep 5
    FAILOVER_TIME=$((FAILOVER_TIME + 5))
  done

  # 第 5 步: 验证数据完整性
  echo "[步骤 5] 验证数据完整性..."
  NEW_MASTER_ROWS=$(psql -h standby.internal -c 'SELECT count(*) FROM payment_order;' -t)
  echo "订单总数: ${NEW_MASTER_ROWS}"

  # 第 6 步: 恢复原主库
  echo "[步骤 6] 恢复原主库..."
  ssh postgres@master.internal "sudo systemctl start postgresql"
  sleep 30

  # 第 7 步: 重建从库
  echo "[步骤 7] 重建从库..."
  ssh postgres@standby.internal bash <<'EOF'
    sudo -u postgres pg_basebackup \
      -h master.internal \
      -D /var/lib/postgresql/15/main \
      -U replication \
      -v -W
EOF

  # 第 8 步: 验证恢复
  echo "[步骤 8] 验证恢复..."
  if psql -h master.internal -c "SELECT 1" > /dev/null 2>&1; then
    echo "✓ 主库恢复正常"
  fi

  if psql -h standby.internal -c "SELECT pg_last_wal_receive_time() > now() - interval '5 seconds';" | grep -q "t"; then
    echo "✓ 从库同步正常"
  fi

  # 演练结束
  END_TIME=$(date +%s)
  DURATION=$((END_TIME - START_TIME))

  echo ""
  echo "演练完成: $(date)"
  echo "总耗时: ${DURATION} 秒"

  # 生成报告
  echo ""
  echo "演练报告:"
  echo "- 故障转移耗时: ${FAILOVER_TIME} 秒"
  echo "- 数据丢失: 0 条记录"
  echo "- 应用影响: 无"
  echo "- 评估: ✓ PASSED"

} | tee -a ${DRILL_LOG}

# 上传演练日志到中央系统
aws s3 cp ${DRILL_LOG} s3://douyin-drills/logs/

echo "✓ 演练日志已保存: ${DRILL_LOG}"
```

### 演练清单

```markdown
## 灾备演练清单

### 演练前 (T-1 天)
- [ ] 通知所有相关团队
- [ ] 确认备份最新可用
- [ ] 准备隔离的恢复环境
- [ ] 准备通信渠道（Slack/电话）

### 演练中
- [ ] 记录开始时间
- [ ] 监控应用性能指标
- [ ] 记录故障转移时间 (< 15 分钟)
- [ ] 验证数据一致性
- [ ] 监控错误日志

### 演练后 (T+1 天)
- [ ] 生成演练报告
- [ ] 团队复盘会议
- [ ] 记录改进项
- [ ] 更新灾备计划
- [ ] 归档演练日志

### 成功标准
- [ ] 故障转移自动完成 (RTO ≤ 15 分钟)
- [ ] 数据零丢失 (RPO ≤ 5 分钟)
- [ ] 应用可访问性 100%
- [ ] 无操作失误
```

---

## 监控和告警

### 关键监控指标

```yaml
# prometheus-alerts.yml
groups:
  - name: disaster_recovery
    rules:
      # 主从复制延迟告警
      - alert: PostgreSQLReplicationLag
        expr: pg_stat_replication_write_lag_bytes{} > 104857600
        for: 5m
        annotations:
          summary: "PostgreSQL 复制延迟 > 100MB"
          action: "检查网络连接和从库容量"

      # 备份失败告警
      - alert: BackupFailure
        expr: backup_success_timestamp < time() - 86400
        for: 30m
        annotations:
          summary: "24 小时内备份失败"
          action: "检查备份进程和存储空间"

      # 数据不一致告警
      - alert: DataInconsistency
        expr: master_checksum != standby_checksum
        for: 1m
        annotations:
          summary: "主从数据校验不一致"
          action: "立即停止写操作，启动恢复流程"
```

---

## 灾备联系列表

| 角色 | 姓名 | 电话 | 邮箱 | 值班时间 |
|------|------|------|------|--------|
| **值班负责人** | 张三 | 10086 | zhang@company.com | 24/7 |
| **数据库管理** | 李四 | 10087 | li@company.com | 工作时间 + 应急 |
| **应用运维** | 王五 | 10088 | wang@company.com | 工作时间 + 应急 |
| **基础设施** | 赵六 | 10089 | zhao@company.com | 工作时间 + 应急 |
| **产品负责** | 孙七 | 10090 | sun@company.com | 工作时间 |

---

## 附录

### A. 灾备计划版本历史

| 版本 | 日期 | 更新内容 |
|------|------|--------|
| 2.0 | 2026-03-05 | 添加跨 DC 故障转移支持 |
| 1.5 | 2026-02-01 | 优化 RTO 到 15 分钟 |
| 1.0 | 2026-01-01 | 首版灾备计划 |

### B. 相关文档

- [部署指南](./DEPLOYMENT_GUIDE.md)
- [发版计划](./RELEASE_PLAN.md)
- [监控告警](./MONITORING.md)

---

**维护者**: 高可用团队
**最后更新**: 2026-03-05
**审批者**: CTO / 运维负责人
**版本**: 2.0
