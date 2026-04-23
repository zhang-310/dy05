# DY01 SaaS 平台部署指南 v2.0

> 生产级部署、性能调优、监控配置完整指南

## 快速导航

| 章节 | 描述 | 预计时间 |
|------|------|--------|
| [系统要求](#系统要求) | 硬件/软件最低要求 | 5 分钟 |
| [快速开始](#快速开始) | 5 分钟快速部署 | 5 分钟 |
| [本地开发](#本地开发环境) | 开发环境搭建 | 15 分钟 |
| [容器部署](#docker-容器部署) | Docker Compose 部署 | 10 分钟 |
| [生产部署](#生产环境部署) | 高可用生产部署 | 1 小时 |
| [性能调优](#性能调优) | JVM 和数据库优化 | 30 分钟 |
| [监控告警](#监控和告警) | Prometheus/Grafana 配置 | 30 分钟 |
| [故障排查](#故障排查) | 常见问题解决 | 按需 |

---

## 系统要求

### 运行时环境

| 组件 | 最低版本 | 推荐版本 | 作用 |
|------|---------|---------|------|
| JDK | 21.0.0 | 21.0.1+ | 后端运行 |
| Node.js | 18.0.0 | 18.17.0+ | 前端构建 |
| PostgreSQL | 15.0 | 15.3+ | 主数据库 |
| Redis | 7.0 | 7.0.5+ | 缓存存储 |
| RabbitMQ | 3.10.0 | 3.12.0+ | 消息队列 |
| Elasticsearch | 8.10.0 | 8.15.0+ | 日志检索 |

### 硬件配置

#### 开发环境（单机）
```
CPU:    4 核+
内存:   8 GB+
磁盘:   50 GB SSD
网络:   100 Mbps+
```

#### 生产环境（高可用）
```
主应用服务器 (×2)
  CPU:    16 核+
  内存:   32 GB+
  磁盘:   200 GB SSD

数据库服务器 (×2)
  CPU:    8 核+
  内存:   16 GB+
  磁盘:   500 GB SSD

缓存和队列 (×1)
  CPU:    4 核+
  内存:   8 GB+
```

### 网络要求
- 内部通信带宽: 1 Gbps 以上
- 外网出口: 100 Mbps 以上
- 允许的端口: 80, 443, 5432, 6379, 5672, 9200

---

## 快速开始

### 5 分钟快速部署（使用 Docker）

```bash
# 1. 克隆代码
git clone https://github.com/your-org/douyin-operations.git
cd douyin-operations

# 2. 启动所有服务
docker compose -f docker/docker-compose.yml up -d

# 3. 初始化数据库
docker compose exec postgres psql -U postgres -d douyin_operations < sql/init-all.sql

# 4. 检查服务状态
docker compose ps

# 5. 访问应用
open http://localhost:3000        # 前端
open http://localhost:8080/swagger-ui.html  # API 文档
```

**验证：** 如果看到登录页面，说明部署成功！

---

## 本地开发环境

### 方法 1: Docker Compose（推荐）

```bash
# 启动依赖服务
docker compose -f docker/docker-compose.yml up -d \
  postgres redis rabbitmq elasticsearch

# 等待服务就绪（约 30 秒）
sleep 30

# 初始化数据库
psql -h localhost -p 5433 -U postgres -d douyin_operations < sql/init-all.sql

# 启动后端
mvn spring-boot:run

# 启动前端（新终端）
cd frontend-react && npm run dev
```

### 方法 2: 本地安装（高级）

```bash
# 1. 启动 PostgreSQL
brew services start postgresql@15  # macOS
sudo systemctl start postgresql    # Linux

# 2. 启动 Redis
brew services start redis  # macOS
sudo systemctl start redis # Linux

# 3. 启动 RabbitMQ
brew services start rabbitmq  # macOS
sudo systemctl start rabbitmq # Linux

# 4. 初始化数据库
psql -h localhost -U postgres < sql/init-all.sql

# 5. 启动应用
mvn spring-boot:run &
cd frontend-react && npm run dev
```

**验证**:
```bash
# 后端健康检查
curl http://localhost:8080/actuator/health

# 前端可访问
curl -L http://localhost:3000
```

---

## Docker 容器部署

### 构建镜像

```bash
# 后端镜像
docker build -f docker/Dockerfile \
  -t douyin-backend:latest \
  -t douyin-backend:$(git describe --tags) .

# 前端镜像
docker build -f frontend-react/Dockerfile \
  -t douyin-frontend:latest \
  -t douyin-frontend:$(git describe --tags) frontend-react/

# 推送到镜像仓库
docker push douyin-backend:latest
docker push douyin-frontend:latest
```

### 使用 Compose 运行

```yaml
# docker-compose.override.yml - 开发配置
version: '3.8'

services:
  backend:
    environment:
      - SPRING_PROFILES_ACTIVE=dev
      - LOGGING_LEVEL_ROOT=DEBUG
    ports:
      - "8080:8080"
    volumes:
      - ./src:/app/src
      - ./logs:/app/logs

  frontend:
    ports:
      - "3000:3000"
    environment:
      - VITE_API_BASE_URL=http://localhost:8080
```

启动：
```bash
docker compose -f docker-compose.yml -f docker-compose.override.yml up
```

---

## 生产环境部署

### 前置检查

```bash
#!/bin/bash
# deploy-preflight-check.sh

echo "检查 Java 版本..."
java -version 2>&1 | grep -q "21" || { echo "❌ 需要 JDK 21+"; exit 1; }

echo "检查数据库连接..."
pg_isready -h ${DB_HOST:-localhost} -p ${DB_PORT:-5432} || { echo "❌ 数据库不可达"; exit 1; }

echo "检查 Redis 连接..."
redis-cli -h ${REDIS_HOST:-localhost} ping > /dev/null || { echo "❌ Redis 不可达"; exit 1; }

echo "检查 RabbitMQ 连接..."
curl -u guest:guest http://${RABBITMQ_HOST:-localhost}:15672/api/overview > /dev/null 2>&1 || { echo "❌ RabbitMQ 不可达"; exit 1; }

echo "✅ 所有检查通过"
```

### 环境变量配置

创建 `/etc/douyin/prod.env`：

```bash
# ==================== 应用配置 ====================
SPRING_PROFILES_ACTIVE=prod
SPRING_APPLICATION_NAME=douyin-operations

# ==================== 数据库配置 ====================
SPRING_DATASOURCE_URL=jdbc:postgresql://db01.internal:5432/douyin_operations
SPRING_DATASOURCE_USERNAME=app_user
SPRING_DATASOURCE_PASSWORD=<${DB_PASSWORD}>
SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE=25
SPRING_DATASOURCE_HIKARI_MINIMUM_IDLE=5
SPRING_DATASOURCE_HIKARI_CONNECTION_TIMEOUT=30000

# ==================== Redis 配置 ====================
SPRING_REDIS_HOST=redis01.internal
SPRING_REDIS_PORT=6379
SPRING_REDIS_PASSWORD=<${REDIS_PASSWORD}>
SPRING_REDIS_DATABASE=0
SPRING_REDIS_TIMEOUT=10000

# ==================== RabbitMQ 配置 ====================
SPRING_RABBITMQ_HOST=rabbitmq01.internal
SPRING_RABBITMQ_PORT=5672
SPRING_RABBITMQ_USERNAME=app_user
SPRING_RABBITMQ_PASSWORD=<${RABBITMQ_PASSWORD}>
SPRING_RABBITMQ_CONNECTION_TIMEOUT=10000

# ==================== 支付配置 ====================
DOUYIN_MERCHANT_ID=<merchant_id>
DOUYIN_MERCHANT_SECRET=<merchant_secret>
DOUYIN_APP_ID=<app_id>
PAYMENT_ENV=production
PAYMENT_TIMEOUT_MINUTES=30

# ==================== JVM 配置 ====================
JAVA_OPTS=-Xms8g -Xmx8g -XX:+UseG1GC -XX:MaxGCPauseMillis=200
```

### 部署脚本

```bash
#!/bin/bash
# deploy-production.sh

set -e

VERSION=${1:-latest}
DEPLOY_DIR="/opt/douyin"
SERVICE_NAME="douyin-app"

echo "🚀 部署 douyin-operations v${VERSION}"

# 1. 编译
echo "📦 编译应用..."
mvn clean package -DskipTests -P prod
JAR_FILE="target/douyin-operations-*.jar"

# 2. 备份
if [ -f "${DEPLOY_DIR}/app.jar" ]; then
  BACKUP_FILE="${DEPLOY_DIR}/app.jar.backup.$(date +%s)"
  echo "💾 备份当前版本 → ${BACKUP_FILE}"
  mv ${DEPLOY_DIR}/app.jar ${BACKUP_FILE}
fi

# 3. 部署
echo "📮 部署新版本..."
mkdir -p ${DEPLOY_DIR}
cp ${JAR_FILE} ${DEPLOY_DIR}/app.jar
chmod +x ${DEPLOY_DIR}/app.jar

# 4. 启动
echo "🔄 重启服务..."
systemctl restart ${SERVICE_NAME}

# 5. 验证
echo "⏳ 等待服务启动（60 秒）..."
sleep 60

HEALTH_URL="http://localhost:8080/actuator/health"
if curl -f ${HEALTH_URL} > /dev/null 2>&1; then
  echo "✅ 应用已启动 ($(curl -s ${HEALTH_URL} | jq .status))"
else
  echo "❌ 应用启动失败，执行回滚..."
  systemctl stop ${SERVICE_NAME}
  mv ${BACKUP_FILE} ${DEPLOY_DIR}/app.jar
  systemctl start ${SERVICE_NAME}
  exit 1
fi

# 6. 检查 Git 历史
git log -1 --format="%H %s %ci"

echo "🎉 部署完成！"
echo "版本信息："
curl -s http://localhost:8080/actuator/info | jq '.'
```

### Systemd 服务

创建 `/etc/systemd/system/douyin-app.service`：

```ini
[Unit]
Description=DY01 SaaS Application
After=network.target postgresql.service redis.service rabbitmq-server.service
Wants=network-online.target

[Service]
Type=simple
User=douyin
Group=douyin
WorkingDirectory=/opt/douyin

# 环境变量
EnvironmentFile=/etc/douyin/prod.env
Environment="JAVA_HOME=/usr/lib/jvm/java-21-openjdk"

# 启动命令
ExecStart=/usr/bin/java ${JAVA_OPTS} \
  -Dspring.config.location=classpath:application-prod.yml \
  -jar /opt/douyin/app.jar

# 重启策略
Restart=on-failure
RestartSec=5
StartLimitInterval=60s
StartLimitBurst=3

# 日志
StandardOutput=journal
StandardError=journal
SyslogIdentifier=douyin

# 资源限制
LimitNOFILE=65535
LimitNPROC=65535

[Install]
WantedBy=multi-user.target
```

启用：
```bash
sudo systemctl daemon-reload
sudo systemctl enable douyin-app
sudo systemctl start douyin-app
```

### Nginx 反向代理

```nginx
upstream douyin_backend {
    least_conn;
    server app01.internal:8080 weight=1 max_fails=3 fail_timeout=30s;
    server app02.internal:8080 weight=1 max_fails=3 fail_timeout=30s;
    keepalive 32;
}

server {
    listen 80;
    server_name api.example.com;
    return 301 https://$server_name$request_uri;
}

server {
    listen 443 ssl http2;
    server_name api.example.com;

    # SSL 证书
    ssl_certificate /etc/nginx/certs/api.example.com/fullchain.pem;
    ssl_certificate_key /etc/nginx/certs/api.example.com/privkey.pem;
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers ECDHE-ECDSA-AES128-GCM-SHA256:ECDHE-RSA-AES128-GCM-SHA256;
    ssl_prefer_server_ciphers on;
    ssl_session_cache shared:SSL:10m;
    ssl_session_timeout 10m;

    # 日志
    access_log /var/log/nginx/douyin_access.log combined buffer=32k flush=5s;
    error_log /var/log/nginx/douyin_error.log warn;

    # 请求限制
    limit_req_zone $binary_remote_addr zone=api_limit:10m rate=100r/s;
    limit_req zone=api_limit burst=200 nodelay;

    # 代理
    location / {
        proxy_pass http://douyin_backend;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_set_header Connection "";

        # WebSocket
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";

        # 超时 (支持长请求)
        proxy_connect_timeout 30s;
        proxy_send_timeout 600s;
        proxy_read_timeout 600s;

        # 缓冲
        proxy_buffering on;
        proxy_buffer_size 8k;
        proxy_buffers 32 8k;
        proxy_busy_buffers_size 16k;
    }

    # 健康检查（仅内网）
    location /actuator/health {
        access_log off;
        allow 10.0.0.0/8;
        deny all;
        proxy_pass http://douyin_backend;
    }

    # 静态资源
    location ~* \.(js|css|png|jpg|svg|woff|woff2)$ {
        proxy_pass http://douyin_backend;
        expires 30d;
        add_header Cache-Control "public, immutable";
    }

    # Gzip 压缩
    gzip on;
    gzip_types text/plain text/css text/xml text/javascript application/json;
    gzip_min_length 1000;
    gzip_vary on;
}
```

---

## 性能调优

### JVM 调优参数

```bash
# 根据硬件调整 (推荐)
export JAVA_OPTS="\
  -Xms$(expr $(free -b | awk '/^Mem:/{print $2}') / 4 / 1024 / 1024)m \
  -Xmx$(expr $(free -b | awk '/^Mem:/{print $2}') / 3 / 1024 / 1024)m \
  -XX:+UseG1GC \
  -XX:MaxGCPauseMillis=200 \
  -XX:InitiatingHeapOccupancyPercent=35 \
  -XX:G1ReservePercent=10 \
  -XX:+ParallelRefProcEnabled \
  -XX:+AlwaysPreTouch \
  -XX:+UnlockDiagnosticVMOptions \
  -XX:G1SummarizeRSetStatsPeriod=1000 \
  -XX:+PrintGCDetails \
  -XX:+PrintGCDateStamps \
  -Xloggc:/opt/douyin/logs/gc.log"
```

### 数据库连接池

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 25
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
      connection-test-query: "SELECT 1"
      leak-detection-threshold: 60000
      data-source-properties:
        ssl: true
        preparedStatementCacheSize: 250
        preparedStatementCacheSqlLimit: 2048
```

### 缓存优化

```yaml
spring:
  redis:
    jedis:
      pool:
        max-active: 20
        max-idle: 10
        min-idle: 5
        max-wait: -1ms
    timeout: 10000ms
    client-type: jedis
```

---

## 监控和告警

### Prometheus 采集配置

```yaml
# prometheus.yml
global:
  scrape_interval: 15s
  evaluation_interval: 15s

scrape_configs:
  - job_name: 'douyin-app'
    metrics_path: '/actuator/prometheus'
    scrape_interval: 10s
    scrape_timeout: 5s
    static_configs:
      - targets: ['localhost:8080']
        labels:
          environment: 'production'
          service: 'douyin-app'
```

### 关键指标告警

```yaml
# alerts.yml
groups:
  - name: application_alerts
    rules:
      - alert: AppDown
        expr: up{job="douyin-app"} == 0
        for: 1m
        annotations:
          summary: "应用已下线"

      - alert: HighErrorRate
        expr: rate(http_requests_total{status=~"5.."}[5m]) > 0.05
        for: 5m
        annotations:
          summary: "错误率 > 5%"

      - alert: DatabaseSlow
        expr: rate(db_query_milliseconds_sum[5m]) / rate(db_query_milliseconds_count[5m]) > 500
        for: 5m
        annotations:
          summary: "数据库查询平均耗时 > 500ms"
```

---

**维护者**: DevOps 团队
**最后更新**: 2026-03-05
**版本**: 2.0
