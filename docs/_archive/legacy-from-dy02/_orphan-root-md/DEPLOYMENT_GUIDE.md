# 抖音运营平台 - Docker 部署上线指南

## 部署前准备

### 1. 环境要求

- Docker 20.10+
- Docker Compose 2.0+
- 可用内存：至少 4GB
- 可用磁盘：至少 20GB

### 2. 检查环境

```bash
# 检查 Docker 版本
docker --version
docker compose version

# 检查可用资源
docker system df
docker system info | grep -E "CPUs|Total Memory"
```

## 快速部署

### 1. 进入项目目录

```bash
cd C:/claude/dy01
```

### 2. 创建环境配置文件

```bash
cd docker
cp .env.example .env
```

编辑 `.env` 文件：

```bash
# 数据库配置
POSTGRES_USER=postgres
POSTGRES_PASSWORD=your_secure_password
POSTGRES_DB=douyin_operations
POSTGRES_PORT=5433

# Redis 配置
REDIS_PORT=6380

# RabbitMQ 配置
RABBITMQ_USER=admin
RABBITMQ_PASS=your_rabbitmq_password
RABBITMQ_PORT=5672
RABBITMQ_MGMT_PORT=15672

# Elasticsearch 配置
ES_PORT=9200

# 应用配置
APP_PORT=8189
APP_TOKEN_SECRET=your_jwt_secret_key_min_32_chars

# Nginx 配置
NGINX_PORT=8888

# CORS 配置
CORS_ALLOWED_ORIGINS=http://localhost:8888

# Milvus 配置（使用已有实例）
MILVUS_HOST=host.docker.internal
MILVUS_PORT=19631

# Spring Profile
SPRING_PROFILE=prod
```

### 3. 构建并启动服务

```bash
# 构建镜像
docker compose build

# 启动所有服务（不包含 AI 内置服务）
docker compose up -d

# 查看服务状态
docker compose ps

# 查看日志
docker compose logs -f app
```

### 4. 初始化数据库

```bash
# 等待数据库就绪
docker compose exec postgres pg_isready -U postgres

# 执行数据库初始化脚本
docker run --rm \
  -v ${PWD}/../sql:/sql \
  --network docker_dy-net \
  -e PGPASSWORD=your_secure_password \
  postgres:15-alpine \
  psql -h dy-postgres -U postgres -d douyin_operations -f /sql/init.sql
```

## 服务访问地址

启动成功后，可以通过以下地址访问：

| 服务 | 地址 | 说明 |
|------|------|------|
| 前端应用 | http://localhost:8888 | Nginx 反向代理 |
| 后端 API | http://localhost:8189 | Spring Boot 应用 |
| API 文档 | http://localhost:8189/swagger-ui.html | Swagger UI |
| 健康检查 | http://localhost:8189/actuator/health | Actuator |
| PostgreSQL | localhost:5433 | 数据库 |
| Redis | localhost:6380 | 缓存 |
| RabbitMQ 管理 | http://localhost:15672 | 消息队列管理界面 |
| Elasticsearch | http://localhost:9200 | 搜索引擎 |

## 测试验证

### 1. 健康检查

```bash
# 检查后端健康状态
curl http://localhost:8189/actuator/health

# 预期输出
{
  "status": "UP",
  "components": {
    "db": {"status": "UP"},
    "redis": {"status": "UP"},
    "diskSpace": {"status": "UP"}
  }
}
```

### 2. 数据库连接测试

```bash
# 连接数据库
docker compose exec postgres psql -U postgres -d douyin_operations

# 查看表
\dt

# 查看用户表
SELECT id, username, nickname FROM sys_user LIMIT 5;

# 退出
\q
```

### 3. API 测试

```bash
# 测试登录接口
curl -X POST http://localhost:8189/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "admin123"
  }'

# 测试健康检查
curl http://localhost:8189/actuator/health

# 测试 Swagger 文档
curl http://localhost:8189/v3/api-docs
```

### 4. 前端访问测试

```bash
# 访问前端首页
curl -I http://localhost:8888

# 预期返回 200 OK
```

## 监控和日志

### 查看日志

```bash
# 查看所有服务日志
docker compose logs

# 查看特定服务日志
docker compose logs -f app
docker compose logs -f postgres
docker compose logs -f redis

# 查看最近 100 行日志
docker compose logs --tail=100 app
```

### 查看资源使用

```bash
# 查看容器资源使用情况
docker stats

# 查看特定容器
docker stats dy-app dy-postgres dy-redis
```

### 启用监控（可选）

```bash
# 启动 Prometheus 和 Grafana
docker compose --profile monitoring up -d

# 访问 Prometheus: http://localhost:9090
# 访问 Grafana: http://localhost:3001 (admin/admin)
```

## 常见问题排查

### 1. 容器启动失败

```bash
# 查看容器状态
docker compose ps

# 查看失败原因
docker compose logs app

# 重启服务
docker compose restart app
```

### 2. 数据库连接失败

```bash
# 检查数据库是否就绪
docker compose exec postgres pg_isready -U postgres

# 检查网络连接
docker compose exec app ping postgres

# 查看数据库日志
docker compose logs postgres
```

### 3. 端口冲突

```bash
# 检查端口占用
netstat -ano | findstr "8189"
netstat -ano | findstr "5433"

# 修改 .env 文件中的端口配置
# 然后重启服务
docker compose down
docker compose up -d
```

### 4. 内存不足

```bash
# 查看 Docker 资源限制
docker system info | grep -E "CPUs|Total Memory"

# 调整应用内存限制（编辑 docker-compose.yml）
deploy:
  resources:
    limits:
      memory: 1024M  # 降低内存限制
```

## 数据备份

### 备份数据库

```bash
# 备份数据库
docker compose exec postgres pg_dump -U postgres douyin_operations > backup_$(date +%Y%m%d_%H%M%S).sql

# 或使用 Docker 命令
docker run --rm \
  --network docker_dy-net \
  -e PGPASSWORD=your_secure_password \
  -v ${PWD}:/backup \
  postgres:15-alpine \
  pg_dump -h dy-postgres -U postgres douyin_operations > /backup/backup.sql
```

### 恢复数据库

```bash
# 恢复数据库
docker compose exec -T postgres psql -U postgres douyin_operations < backup.sql

# 或使用 Docker 命令
docker run --rm \
  --network docker_dy-net \
  -e PGPASSWORD=your_secure_password \
  -v ${PWD}:/backup \
  postgres:15-alpine \
  psql -h dy-postgres -U postgres douyin_operations < /backup/backup.sql
```

## 更新部署

### 1. 拉取最新代码

```bash
cd C:/claude/dy01
git pull origin main
```

### 2. 重新构建镜像

```bash
cd docker
docker compose build --no-cache app frontend
```

### 3. 滚动更新

```bash
# 停止旧容器
docker compose stop app frontend

# 启动新容器
docker compose up -d app frontend

# 验证更新
docker compose ps
docker compose logs -f app
```

### 4. 数据库迁移（如有需要）

```bash
# 执行迁移脚本
docker run --rm \
  -v ${PWD}/../sql:/sql \
  --network docker_dy-net \
  -e PGPASSWORD=your_secure_password \
  postgres:15-alpine \
  psql -h dy-postgres -U postgres -d douyin_operations -f /sql/migrations/migrate-all.sql
```

## 停止和清理

### 停止服务

```bash
# 停止所有服务
docker compose down

# 停止并删除数据卷（谨慎使用）
docker compose down -v
```

### 清理资源

```bash
# 清理未使用的镜像
docker image prune -a

# 清理未使用的容器
docker container prune

# 清理未使用的卷
docker volume prune

# 清理所有未使用资源
docker system prune -a --volumes
```

## 生产环境建议

### 1. 安全配置

- 修改所有默认密码
- 使用强密码（至少 16 位）
- 配置防火墙规则
- 启用 HTTPS（配置 SSL 证书）
- 限制数据库外部访问

### 2. 性能优化

- 调整 JVM 参数（Dockerfile 中的 JAVA_OPTS）
- 配置数据库连接池
- 启用 Redis 持久化
- 配置 Nginx 缓存

### 3. 监控告警

- 启用 Prometheus + Grafana 监控
- 配置日志收集（ELK Stack）
- 设置告警规则
- 定期检查资源使用

### 4. 备份策略

- 每日自动备份数据库
- 保留最近 7 天的备份
- 定期测试恢复流程
- 备份到远程存储

## 技术支持

如遇到问题，请查看：

1. 日志文件：`docker compose logs`
2. 健康检查：`http://localhost:8189/actuator/health`
3. 数据库状态：`docker compose exec postgres pg_isready`
4. 项目文档：`docs/` 目录

## 附录

### 完整启动命令

```bash
# 1. 进入目录
cd C:/claude/dy01/docker

# 2. 启动服务
docker compose up -d

# 3. 初始化数据库
docker run --rm \
  -v ${PWD}/../sql:/sql \
  --network docker_dy-net \
  -e PGPASSWORD=your_secure_password \
  postgres:15-alpine \
  psql -h dy-postgres -U postgres -d douyin_operations -f /sql/init.sql

# 4. 验证部署
curl http://localhost:8189/actuator/health
curl -I http://localhost:8888

# 5. 查看日志
docker compose logs -f app
```

### 快速重启

```bash
# 重启所有服务
docker compose restart

# 重启特定服务
docker compose restart app
docker compose restart nginx
```
