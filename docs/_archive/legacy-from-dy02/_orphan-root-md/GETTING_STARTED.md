# 新人上手指南

本文档帮助新加入的开发者快速搭建本地开发环境并运行项目。

## 1. 环境准备

请确保本地已安装以下工具:

| 工具 | 最低版本 | 说明 |
|------|----------|------|
| JDK | 21 | 推荐使用 Eclipse Temurin 或 GraalVM |
| Maven | 3.9+ | 后端构建工具 |
| Node.js | 18+ | 前端运行时 |
| npm | 9+ | 随 Node.js 一起安装 |
| Docker | 24+ | 容器运行环境 |
| Docker Compose | 2.20+ | 容器编排 |
| Git | 2.40+ | 版本控制 |

验证安装:

```bash
java -version        # 应显示 21.x
mvn -version         # 应显示 3.9.x
node -v              # 应显示 v18.x 或更高
docker --version     # 应显示 24.x 或更高
```

## 2. 克隆项目

```bash
git clone <repo-url> dy01
cd dy01
```

## 3. 启动依赖服务

项目依赖 PostgreSQL、Redis、RabbitMQ 和 Elasticsearch，通过 Docker Compose 一键启动:

```bash
docker compose -f docker/docker-compose.yml up -d postgres redis rabbitmq elasticsearch
```

等待所有容器就绪 (约 30 秒):

```bash
docker compose -f docker/docker-compose.yml ps
```

确认所有服务状态为 `running` 或 `healthy`。

服务端口:

| 服务 | 端口 | 用户名 | 密码 |
|------|------|--------|------|
| PostgreSQL | 5433 | postgres | postgresql |
| Redis | 6380 | -- | -- |
| RabbitMQ | 5672 / 15672 | guest | guest |
| Elasticsearch | 9200 | -- | -- |

## 4. 初始化数据库

首次运行需执行建表脚本:

```bash
# 方式一: 使用本地 psql
psql -h localhost -p 5433 -U postgres -d douyin_operations -f sql/init.sql

# 方式二: 使用 Docker 内的 psql
docker exec -i dy-postgres psql -U postgres -d douyin_operations < sql/init.sql
```

如果数据库 `douyin_operations` 尚不存在:

```bash
psql -h localhost -p 5433 -U postgres -c "CREATE DATABASE douyin_operations;"
```

## 5. 启动后端

```bash
# 编译检查
mvn compile

# 启动应用 (自动使用 dev Profile)
mvn spring-boot:run
```

后端启动后监听 `http://localhost:8080`。

Windows 环境如果控制台出现中文乱码，先执行:

```bash
chcp 65001
```

## 6. 启动前端

```bash
cd frontend-react
npm install
npm run dev
```

前端开发服务器启动后访问 `http://localhost:3000`，`/api` 路径会自动代理到后端。

## 7. 访问 Swagger UI

浏览器打开:

```
http://localhost:8080/swagger-ui.html
```

Swagger UI 列出了所有后端 API，可直接在页面上测试接口。

## 8. 创建测试用户

通过 Swagger UI 或 curl 调用注册接口:

```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "Test@123456",
    "nickname": "测试用户"
  }'
```

使用返回的凭据登录:

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "Test@123456"
  }'
```

响应中的 `data.token` 即为 JWT Token，在后续请求中通过 `Authorization: Bearer <token>` 头携带。

## 9. 常见问题排查

### PostgreSQL 连接失败

**现象**: `Connection refused` 或 `could not connect to server`

**排查步骤**:
1. 确认容器正在运行: `docker ps | grep postgres`
2. 确认端口为 5433 (非默认 5432): `docker port dy-postgres`
3. 确认密码为 `postgresql`: 检查 `docker/docker-compose.yml` 中的 `POSTGRES_PASSWORD`

### Redis 连接失败

**现象**: `Unable to connect to Redis`

**排查步骤**:
1. 确认 Redis 容器正在运行: `docker ps | grep redis`
2. 确认端口为 6380 (非默认 6379)
3. 测试连接: `redis-cli -p 6380 ping`

### 前端编译错误

**现象**: `npm run dev` 报 TypeScript 错误

**排查步骤**:
1. 删除 `node_modules` 并重新安装: `rm -rf node_modules && npm install`
2. 检查 Node.js 版本是否 >= 18
3. 运行类型检查定位问题: `npm run type-check`

### 后端启动失败

**现象**: `mvn spring-boot:run` 报错

**排查步骤**:
1. 确认 JDK 版本为 21: `java -version`
2. 确认所有依赖服务已启动: `docker compose -f docker/docker-compose.yml ps`
3. 检查端口是否被占用: `netstat -ano | findstr 8080` (Windows) 或 `lsof -i :8080` (Mac/Linux)
4. 查看详细日志: `mvn spring-boot:run -X`

### Elasticsearch 启动失败

**现象**: ES 容器反复重启

**排查步骤**:
1. 检查日志: `docker logs dy-elasticsearch`
2. 常见原因是内存不足，ES 默认需要至少 2GB 堆内存
3. 在 Docker Desktop 中分配至少 4GB 内存给 Docker

### 数据库初始化脚本执行失败

**现象**: `psql` 报错 `relation already exists` 或 `database does not exist`

**排查步骤**:
1. 如果数据库不存在，先创建: `CREATE DATABASE douyin_operations;`
2. 如果表已存在，脚本应支持 `IF NOT EXISTS`，可安全重复执行
3. 检查 `sql/init.sql` 文件是否存在且完整
