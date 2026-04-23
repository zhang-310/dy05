# Docker 部署

## 端口映射（与 application-dev.yml 一致）

| 服务 | 容器端口 | 宿主机端口 | 说明 |
|------|----------|------------|------|
| postgres | 5432 | 5433 | 数据库 |
| redis | 6379 | 6380 | 缓存 |
| rabbitmq | 5672 | 5672 | 消息队列 |
| elasticsearch | 9200 | 9200 | 全文检索 |
| milvus (ai-builtin) | 19530 | 19530 | 向量库 |

## 本机开发：一键启动（推荐）

项目根目录执行：

```powershell
.\start-dev.ps1
```

会自动：启动 Docker 基础服务 → 新窗口启动后端 → 新窗口启动前端。前端支持移动端访问 `http://<本机IP>:3000`。

## 本机开发：Docker 服务 + mvn run（手动）

需所有服务（含 Milvus）正常连接时：

```bash
# 1. 启动基础服务 + AI 服务（含 Milvus、ES、Redis、Postgres、RabbitMQ）
cd docker
docker compose --profile ai-builtin up -d postgres redis rabbitmq elasticsearch etcd minio milvus

# 2. 等待健康检查通过后，在项目根目录启动应用（启用 Milvus）
cd ..
set MILVUS_ENABLED=true   # Windows；Linux/Mac: export MILVUS_ENABLED=true
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

应用连接：Postgres 5433、Redis 6380、RabbitMQ 5672、ES 9200、Milvus 19530。

## 仅基础服务（不含 Milvus）

若暂时不需要 AI 向量能力：

```bash
# 1. 启动基础服务
docker compose up -d postgres redis rabbitmq elasticsearch

# 2. 禁用 Milvus 后启动应用
set MILVUS_ENABLED=false   # Windows
# export MILVUS_ENABLED=false  # Linux/Mac
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

## 快速启动（复用已有 claude-douyin-milvus）

```bash
cd /path/to/dy01

# 可选：复制并修改配置
cp docker/.env.example docker/.env

# 启动（默认连接 host.docker.internal:19631 的 Milvus）
docker compose -f docker/docker-compose.yml up -d
```

访问：**http://localhost:8888**

## AI 服务独立编排（dy-XXX 命名）

若需将 `claude-douyin-milvus` 等迁移为 `dy-*` 命名：

```bash
# 1. 停止旧容器
docker stop claude-douyin-milvus claude-douyin-milvus-etcd claude-douyin-milvus-minio claude-douyin-neo4j
docker rm claude-douyin-milvus claude-douyin-milvus-etcd claude-douyin-milvus-minio claude-douyin-neo4j

# 2. 启动 dy-* 编排（新数据卷，如需保留数据请先备份）
docker compose -f docker/docker-compose.ai.yml up -d
```

新容器：`dy-etcd`、`dy-minio`、`dy-milvus`、`dy-elasticsearch`、`dy-neo4j`。主应用通过 `host.docker.internal:19631` 连接 Milvus，`localhost:9200` 连接 ES。

## 部署模式

| 模式 | 命令 | Milvus 来源 |
|------|------|-------------|
| 默认 | `docker compose -f docker/docker-compose.yml up -d` | 复用 claude-douyin-milvus (19631) |
| 自建 AI | `docker compose -f docker/docker-compose.yml --profile ai-builtin up -d` | 新建 dy-etcd, dy-minio, dy-milvus |

自建模式下，需在 `docker/.env` 中设置：
```
MILVUS_ENABLED=true
MILVUS_HOST=milvus
MILVUS_PORT=19530
```

## 容器与端口

| 容器 | 端口 | 说明 |
|------|------|------|
| dy-nginx | 8888 | 入口 |
| dy-app | 8189 | 后端 API |
| dy-postgres | 5433 | 数据库 |
| dy-redis | 6380 | 缓存 |
| dy-rabbitmq | 5672, 15672 | 消息队列 |
| dy-elasticsearch | 9200 | 全文检索 |

## Milvus 数据删卷重建

数据为空或升级 2.4→2.6 时：

```bash
docker compose -f docker/docker-compose.yml --profile ai-builtin down -v   # 删卷
docker compose -f docker/docker-compose.yml --profile ai-builtin up -d     # 重建
```

或 AI 独立编排：`docker compose -f docker/docker-compose.ai.yml down -v` 后重新 `up -d`。

## 数据库导入（按文档顺序）

```bash
# 重置并导入（需先终止连接，如停止 dy-app）
docker exec dy-postgres psql -U postgres -d postgres -c "SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname = 'douyin_operations' AND pid <> pg_backend_pid();"
docker exec dy-postgres psql -U postgres -d postgres -c "DROP DATABASE IF EXISTS douyin_operations;"
docker exec dy-postgres psql -U postgres -d postgres -c "CREATE DATABASE douyin_operations ENCODING 'UTF8';"

# 执行初始化（项目根目录）
docker run --rm -v "${PWD}:/workspace" -w /workspace --network docker_dy-net -e PGPASSWORD=postgresql postgres:15-alpine psql -h dy-postgres -U postgres -d douyin_operations -f sql/init-docker.sql -v ON_ERROR_STOP=1
```

## 常用命令

```bash
# 构建镜像
docker compose -f docker/docker-compose.yml build

# 查看日志
docker compose -f docker/docker-compose.yml logs -f app

# 停止
docker compose -f docker/docker-compose.yml down
```
