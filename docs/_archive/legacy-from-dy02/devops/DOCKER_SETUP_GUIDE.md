# Docker 容器化完整指南

## 📋 概述

本指南说明如何使用 Docker 和 Docker Compose 快速启动完整的 Douyin Operations 应用栈。

**技术栈**：
- **后端**：Spring Boot 3.3 (Java 17)
- **前端**：Vue 3 + TypeScript + Vite
- **数据库**：PostgreSQL 11
- **缓存**：Redis 7
- **反向代理**：Nginx 1.25
- **Web 服务器**：Nginx Alpine

## 🚀 快速开始

### 前提条件

- Docker >= 20.10
- Docker Compose >= 2.0
- 4GB+ 可用 RAM
- 10GB+ 可用磁盘空间

### 1. 克隆或进入项目目录

```bash
cd /path/to/douyin-operations
```

### 2. 复制环境配置

```bash
cp .env.example .env
```

### 3. 使用自动化脚本启动

```bash
chmod +x setup-docker.sh
./setup-docker.sh
```

或使用 Docker Compose 手动启动：

```bash
# 构建镜像
docker-compose build

# 启动所有服务
docker-compose up -d

# 查看日志
docker-compose logs -f
```

### 4. 验证服务状态

```bash
# 查看所有容器
docker-compose ps

# 检查健康状态
docker-compose ps | grep -E "healthy|unhealthy"

# 查看特定服务日志
docker-compose logs backend
docker-compose logs frontend
docker-compose logs postgres
docker-compose logs redis
```

## 📊 服务访问地址

| 服务 | 地址 | 说明 |
|------|------|------|
| 前端应用 | http://localhost | Vue 3 应用首页 |
| API 反向代理 | http://localhost/api | 所有 API 请求 |
| Swagger UI | http://localhost/swagger-ui.html | API 文档 |
| 后端应用 | http://localhost:8080 | 直接访问后端 |
| 健康检查 | http://localhost:8080/actuator/health | 应用健康状态 |
| Actuator 端点 | http://localhost:8080/actuator | Spring Boot 监控 |
| PostgreSQL | localhost:5432 | 数据库连接 |
| Redis | localhost:6379 | 缓存连接 |

## 📁 文件结构说明

```
project-root/
├── Dockerfile                      # 后端容器构建文件（多阶段构建）
├── frontend/
│   └── Dockerfile                 # 前端容器构建文件
├── docker/
│   └── nginx/
│       └── default.conf           # Nginx 虚拟主机配置
├── nginx.conf                      # Nginx 主配置文件
├── docker-compose.yml             # 完整容器编排配置
├── .dockerignore                  # Docker 构建忽略文件
├── docker-entrypoint.sh           # PostgreSQL 初始化脚本
├── .env.example                   # 环境变量示例
├── setup-docker.sh                # 自动化安装脚本
├── sql/                           # 数据库初始化脚本
│   ├── auth/                      # 认证模块
│   ├── config/                    # 配置模块
│   ├── wecom/                     # 企业号模块
│   ├── douyin/                    # 短视频模块
│   └── ...                        # 其他模块
├── pom.xml                        # Maven 配置
└── src/                           # 后端源代码
```

## 🔧 环境变量配置

编辑 `.env` 文件自定义配置：

```bash
# 数据库配置
DB_USER=postgres
DB_PASSWORD=postgresql
DB_NAME=douyin_operations
DB_PORT=5432

# Redis 配置
REDIS_PORT=6379

# 应用配置
BACKEND_PORT=8080
FRONTEND_PORT=80
SPRING_PROFILE=docker
```

## 📦 构建优化

### 后端镜像优化（目标 < 300MB）

使用多阶段构建：

1. **Stage 1**: Dependency Resolver - 缓存 Maven 依赖
2. **Stage 2**: Backend Builder - 编译 Java 源代码
3. **Stage 3**: Runtime - 最小运行时 (JRE only)

**优化技巧**：
- 使用 JRE 而非 JDK（减小 ~200MB）
- 多阶段构建减少最终镜像大小
- Alpine Linux 基础镜像（<100MB）
- 分层 Maven 缓存加速构建

### 前端镜像优化（目标 < 100MB）

1. **Stage 1**: Node Builder - npm install + npm run build
2. **Stage 2**: Nginx Runtime - 仅包含编译产物

**优化技巧**：
- 使用生产优化的 dist 目录
- Alpine Nginx 基础镜像
- 多阶段构建排除 node_modules

## 🏥 健康检查

所有服务都配置了健康检查：

```yaml
healthcheck:
  test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
  interval: 30s          # 检查间隔
  timeout: 10s           # 超时时间
  retries: 3             # 失败重试次数
  start_period: 30s      # 启动等待时间
```

检查容器健康状态：

```bash
# 查看详细信息
docker-compose ps

# 只看健康状态
docker inspect <container-id> --format='{{.State.Health.Status}}'
```

## 📜 数据库初始化

PostgreSQL 自动初始化执行顺序：

1. `sql/auth/schema.sql` - 认证表结构
2. `sql/auth/resource-data.sql` - 认证数据
3. `sql/config/schema.sql` - 配置表
4. ... 其他模块
5. `sql/ai/schema.sql` - AI 模块最后初始化

所有 SQL 脚本存储在 `sql/` 目录，按模块组织。容器启动时自动执行。

## 🔄 生命周期管理

### 启动服务

```bash
# 后台启动
docker-compose up -d

# 前台启动并查看日志
docker-compose up
```

### 查看日志

```bash
# 查看所有日志（最新 100 行）
docker-compose logs --tail=100

# 实时跟踪特定服务
docker-compose logs -f backend

# 查看特定时间范围的日志
docker-compose logs --since 10m
```

### 停止服务

```bash
# 停止但保留容器和数据
docker-compose stop

# 停止并删除容器（保留卷）
docker-compose down

# 停止并清理所有数据（危险！）
docker-compose down -v
```

### 重启服务

```bash
# 重启所有服务
docker-compose restart

# 重启特定服务
docker-compose restart backend
```

## 🛠️ 常见问题排查

### 问题 1：Port already in use

```bash
# 修改 .env 文件中的端口
BACKEND_PORT=8081
FRONTEND_PORT=8080
REDIS_PORT=6380
DB_PORT=5433
```

### 问题 2：PostgreSQL 连接拒绝

```bash
# 等待 PostgreSQL 完全启动（可能需要 30+ 秒）
docker-compose logs postgres

# 检查 PostgreSQL 健康状态
docker-compose exec postgres pg_isready -U postgres
```

### 问题 3：内存不足

```bash
# 删除无用镜像和容器
docker image prune -a
docker container prune

# 检查磁盘使用
docker system df
```

### 问题 4：后端应用启动失败

```bash
# 查看后端日志
docker-compose logs -f backend

# 进入容器调试
docker-compose exec backend sh

# 检查数据库连接
docker-compose exec backend curl http://postgres:5432
```

## 📊 性能监控

### 查看资源使用

```bash
# 实时监控
docker stats

# 查看单个容器
docker stats douyin-backend
```

### 查看镜像大小

```bash
docker images | grep douyin
```

### 清理未使用资源

```bash
# 删除悬空镜像
docker image prune

# 删除未使用的卷
docker volume prune

# 全面清理（警告：会删除所有未使用资源）
docker system prune -a
```

## 🔐 安全性最佳实践

1. **非 root 用户运行**
   - 后端：使用 spring 用户
   - 前端：使用 nginx 用户

2. **网络隔离**
   - 使用独立的 Docker 网络 (douyin-network)
   - 容器间通过服务名通信

3. **环境变量**
   - 敏感信息存储在 `.env` 文件
   - `.env` 加入 `.gitignore`
   - 生产环境使用 secret 管理工具

4. **日志管理**
   - 配置日志轮转（max-size, max-file）
   - 避免敏感信息写入日志

## 📈 生产部署建议

### 建议 1：使用 Docker Swarm 或 Kubernetes

```bash
# Docker Swarm
docker swarm init
docker stack deploy -c docker-compose.yml douyin

# Kubernetes
kubectl apply -f k8s-manifests/
```

### 建议 2：外部数据存储

```yaml
# 使用托管数据库而非容器 PostgreSQL
SPRING_DATASOURCE_URL: jdbc:postgresql://rds.example.com:5432/db
```

### 建议 3：集中日志管理

```yaml
# 配置 ELK Stack 或其他日志聚合工具
logging:
  driver: "json-file"
  options:
    splunk-token: "${SPLUNK_TOKEN}"
    splunk-url: "https://splunk.example.com:8088"
```

### 建议 4：镜像仓库

```bash
# 推送到私有仓库
docker tag douyin-backend:latest registry.example.com/douyin-backend:1.0.0
docker push registry.example.com/douyin-backend:1.0.0
```

## 📚 相关资源

- [Docker 官方文档](https://docs.docker.com/)
- [Docker Compose 文档](https://docs.docker.com/compose/)
- [Spring Boot Docker 指南](https://spring.io/guides/gs/spring-boot-docker/)
- [Nginx 文档](https://nginx.org/en/docs/)
- [PostgreSQL 容器文档](https://hub.docker.com/_/postgres)

## 🤝 故障排除支持

如遇到问题，请检查：

1. Docker 版本 >= 20.10
2. Docker Compose 版本 >= 2.0
3. 足够的 RAM 和磁盘空间
4. 防火墙未阻止所需端口
5. `.env` 文件配置正确

收集调试信息：

```bash
# 导出诊断信息
docker-compose config > debug-config.yml
docker-compose ps > debug-ps.txt
docker-compose logs > debug-logs.txt
docker system df > debug-df.txt
```

---

**最后更新**: 2026-02-25
**维护者**: Douyin Operations Team
**版本**: 1.0.0
