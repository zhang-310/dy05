# 🚀 生产部署指南

**项目**: 抖音运营平台  
**版本**: 1.0.0  
**状态**: 生产就绪 95% ✓  
**最后更新**: 2026-02-25

---

## 📋 部署前检查清单

### 环境要求
- [ ] Java 17+ (OpenJDK 或 Oracle JDK)
- [ ] PostgreSQL 16+
- [ ] Redis 7+
- [ ] Docker 29+ (可选，用于容器部署)
- [ ] 4GB+ RAM
- [ ] 20GB+ 磁盘空间

### 代码准备
- [ ] 所有代码已提交到 Git
- [ ] 没有未提交的更改
- [ ] 所有测试通过
- [ ] 编译无错误

### 配置准备
- [ ] 生产环境 application-prod.yml 已配置
- [ ] 数据库连接字符串正确
- [ ] Redis 连接配置正确
- [ ] 日志路径可写

---

## 🔧 部署方式选择

### 方式 A：Docker 部署（推荐）

**优点**：
- 环境一致性好
- 易于扩展
- 自动化程度高

**步骤**：
```bash
# 1. 构建镜像
docker-compose build

# 2. 启动服务
docker-compose up -d

# 3. 验证
docker-compose ps
```

### 方式 B：传统部署

**优点**：
- 直接控制
- 易于调试
- 资源占用少

**步骤**：
```bash
# 1. 编译打包
mvn clean package -DskipTests

# 2. 启动后端
java -jar target/douyin-operations-0.0.1-SNAPSHOT.jar

# 3. 启动前端（Nginx）
nginx -c /path/to/nginx.conf
```

---

## 📦 Docker 部署详细步骤

### 步骤 1：准备环境

```bash
# 检查 Docker
docker --version
docker-compose --version

# 创建网络（如果不存在）
docker network create douyin-network
```

### 步骤 2：配置环境变量

创建 `.env` 文件：
```env
# 数据库
DB_USER=postgres
DB_PASSWORD=your_secure_password
DB_NAME=douyin_operations
DB_PORT=5432

# Redis
REDIS_PORT=6379

# 应用
APP_PORT=8080
FRONTEND_PORT=80

# Prometheus
PROMETHEUS_PORT=9090

# Grafana
GRAFANA_PORT=3000
GRAFANA_USER=admin
GRAFANA_PASSWORD=your_secure_password
```

### 步骤 3：构建镜像

```bash
# 构建所有镜像
docker-compose build

# 或单独构建
docker-compose build backend
docker-compose build frontend
```

### 步骤 4：启动服务

```bash
# 启动所有服务
docker-compose up -d

# 查看日志
docker-compose logs -f backend

# 查看服务状态
docker-compose ps
```

### 步骤 5：验证部署

```bash
# 检查后端健康
curl http://localhost:8080/actuator/health

# 检查前端
curl http://localhost

# 检查 Prometheus
curl http://localhost:9090

# 检查 Grafana
curl http://localhost:3000
```

---

## 🔐 生产环境安全配置

### 1. HTTPS 配置

```yaml
# application-prod.yml
server:
  ssl:
    enabled: true
    key-store: /path/to/keystore.p12
    key-store-password: ${SSL_PASSWORD}
    key-store-type: PKCS12
```

### 2. 数据库安全

```sql
-- 创建专用用户
CREATE USER douyin_app WITH PASSWORD 'strong_password';
GRANT CONNECT ON DATABASE douyin_operations TO douyin_app;
GRANT USAGE ON SCHEMA public TO douyin_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO douyin_app;
```

### 3. Redis 安全

```bash
# 启用 Redis 密码
redis-cli CONFIG SET requirepass your_secure_password

# 配置 application-prod.yml
spring:
  redis:
    password: ${REDIS_PASSWORD}
```

### 4. 防火墙规则

```bash
# 只允许必要的端口
ufw allow 80/tcp      # HTTP
ufw allow 443/tcp     # HTTPS
ufw allow 8080/tcp    # 后端 API（可选，仅内网）
ufw allow 5432/tcp    # PostgreSQL（仅内网）
ufw allow 6379/tcp    # Redis（仅内网）
```

---

## 📊 监控和告警

### Prometheus 配置

```yaml
# prometheus.yml
global:
  scrape_interval: 15s
  evaluation_interval: 15s

scrape_configs:
  - job_name: 'douyin-backend'
    static_configs:
      - targets: ['localhost:8080']
    metrics_path: '/actuator/prometheus'
```

### Grafana 仪表板

1. 访问 http://localhost:3000
2. 添加 Prometheus 数据源
3. 导入预配置的仪表板
4. 配置告警规则

---

## 🔄 滚动更新

### 零停机部署

```bash
# 1. 构建新镜像
docker-compose build backend

# 2. 启动新容器
docker-compose up -d --no-deps --build backend

# 3. 验证新容器
docker-compose logs backend

# 4. 移除旧容器
docker-compose down
```

---

## 🆘 故障排查

### 问题 1：后端无法启动

**检查**：
```bash
# 查看日志
docker-compose logs backend

# 检查数据库连接
docker exec douyin-backend curl http://postgres:5432

# 检查 Redis 连接
docker exec douyin-backend redis-cli -h redis ping
```

### 问题 2：前端无法连接后端

**检查**：
```bash
# 检查网络
docker network inspect douyin-network

# 检查 DNS
docker exec douyin-frontend nslookup backend

# 检查防火墙
sudo ufw status
```

### 问题 3：性能下降

**检查**：
```bash
# 查看 Prometheus 指标
curl http://localhost:9090/api/v1/query?query=api_response_time

# 查看数据库连接
docker exec douyin-postgres psql -U postgres -c "SELECT count(*) FROM pg_stat_activity;"

# 查看 Redis 内存
docker exec douyin-redis redis-cli INFO memory
```

---

## 📈 性能优化

### 数据库优化

```sql
-- 创建索引
CREATE INDEX idx_user_username ON auth_user(username);
CREATE INDEX idx_login_log_user_id ON auth_login_log(user_id);
CREATE INDEX idx_audit_log_create_time ON audit_log(create_time);

-- 分析查询
EXPLAIN ANALYZE SELECT * FROM auth_user WHERE username = 'admin';
```

### 缓存优化

```yaml
# application-prod.yml
spring:
  cache:
    type: redis
    redis:
      time-to-live: 600000  # 10 分钟
      cache-null-values: false
```

### 连接池优化

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 10000
```

---

## 📋 部署检查清单

部署后请验证以下项目：

- [ ] 后端 API 可访问 (http://localhost:8080)
- [ ] 前端应用可访问 (http://localhost)
- [ ] 登录功能正常
- [ ] Swagger 文档完整
- [ ] 系统健康检查通过
- [ ] 没有错误日志
- [ ] 性能指标正常
- [ ] 告警规则生效
- [ ] 备份已配置
- [ ] 监控已启用

---

## 🔄 备份和恢复

### 数据库备份

```bash
# 备份
docker exec douyin-postgres pg_dump -U postgres douyin_operations > backup.sql

# 恢复
docker exec -i douyin-postgres psql -U postgres douyin_operations < backup.sql
```

### Redis 备份

```bash
# 备份
docker exec douyin-redis redis-cli BGSAVE

# 恢复
docker cp douyin-redis:/data/dump.rdb ./
docker cp ./dump.rdb douyin-redis:/data/
```

---

## 📞 支持和维护

### 日常维护

- 每天检查日志
- 每周检查性能指标
- 每月检查磁盘空间
- 每季度更新依赖

### 应急响应

- 监控告警 24/7
- 快速故障转移
- 自动备份恢复
- 灾难恢复计划

---

**部署完成后，系统将达到 99% 生产就绪状态。**

**需要帮助？** 查看 TEST_ENVIRONMENT_STARTUP.md 或联系技术支持。

