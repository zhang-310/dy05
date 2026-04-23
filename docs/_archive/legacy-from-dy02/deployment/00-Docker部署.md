# Docker 部署

## 目录结构

```
docker/
├── docker-compose.yml                     # 主编排文件
├── docker-compose.override.high-end.yml   # 高配覆盖
├── docker-compose.redis-ha.yml            # Redis 高可用
├── docker-compose.ai.yml                  # AI 服务
├── Dockerfile.backend                     # 后端镜像
├── Dockerfile.frontend / frontend.Dockerfile  # 前端镜像
├── .env.example                           # 环境变量示例
├── deploy.sh / deploy.bat                 # 部署脚本
├── nginx/
│   ├── gateway.conf                       # Nginx 网关配置
│   ├── frontend.conf                      # 前端 Nginx 配置
│   └── default.conf
└── prometheus/
    ├── prometheus.yml                     # Prometheus 配置
    └── alert-rules.yml                    # 告警规则
```

## 服务编排

### 基础服务

```bash
docker compose -f docker/docker-compose.yml up -d
```

| 服务 | 镜像 | 端口 | 内存限制 |
|------|------|------|----------|
| postgres | postgres:15-alpine | 5433:5432 | 512M |
| redis | redis:7-alpine | 6380:6379 | 256M |
| rabbitmq | rabbitmq:3-management-alpine | 5672 + 15672 | 512M |
| elasticsearch | elasticsearch:8.15.0 | 9200 | 1024M |

### AI 扩展服务（Profile: ai-builtin）

```bash
docker compose -f docker/docker-compose.yml --profile ai-builtin up -d
```

| 服务 | 镜像 | 端口 | 说明 |
|------|------|------|------|
| etcd | coreos/etcd:v3.5.5 | — | Milvus 元数据 |
| minio | minio/minio | 9001 | Milvus 对象存储 |
| milvus | milvusdb/milvus:v2.6.11 | 19530 | 向量数据库 |
| neo4j | neo4j:5-community | 7474 + 7687 | 知识图谱 |

### 监控扩展（Profile: monitoring）

```bash
docker compose -f docker/docker-compose.yml --profile monitoring up -d
```

| 服务 | 镜像 | 端口 |
|------|------|------|
| prometheus | prom/prometheus | 9090 |
| grafana | grafana/grafana | 3001 |

启动 monitoring 后，Micrometer 指标由 **Spring Boot Actuator** 导出（默认 `GET /actuator/prometheus`，与 `management.endpoints.web` 配置一致）；另有历史自定义端点 `GET /api/v1/system/metrics/prometheus` 以项目内 `MetricsCollectorService` 组装。**抖音 OAuth / 采集**（`douyin.api.token-health`、`DouyinOAuthTokenHealthProbe`、`DouyinDataCollector`）注册到 MeterRegistry，示例名：`douyin.oauth.tokens.expired`、`douyin.oauth.probe.runs`、`douyin.collector.skipped`（标签 `reason`，如 `token_unavailable`、`token_expired_no_refresh`）。Grafana 可对 `douyin_oauth_tokens_expired_without_refresh`（Prometheus 下划线命名）等做告警。

### 组合启动

```bash
# 基础 + AI + 监控
docker compose -f docker/docker-compose.yml --profile ai-builtin --profile monitoring up -d

# 高配覆盖（增大内存等）
docker compose -f docker/docker-compose.yml -f docker/docker-compose.override.high-end.yml up -d
```

## 环境变量

复制 `docker/.env.example` 为 `docker/.env`，配置以下关键变量：

| 变量 | 默认 | 说明 |
|------|------|------|
| POSTGRES_USER | postgres | 数据库用户 |
| POSTGRES_PASSWORD | （必填） | 数据库密码 |
| POSTGRES_DB | douyin_operations | 数据库名 |
| POSTGRES_PORT | 5433 | PostgreSQL 端口 |
| REDIS_PORT | 6380 | Redis 端口 |
| RABBITMQ_USER | guest | RabbitMQ 用户 |
| RABBITMQ_PASS | （必填） | RabbitMQ 密码 |
| RABBITMQ_PORT | 5672 | RabbitMQ 端口 |
| RABBITMQ_MGMT_PORT | 15672 | RabbitMQ 管理端口 |
| ES_PORT | 9200 | Elasticsearch 端口 |
| ES_SECURITY_ENABLED | false | ES 安全认证 |
| MILVUS_PORT | 19530 | Milvus 端口 |
| MILVUS_HOST | milvus | Milvus 主机 |
| MINIO_ACCESS_KEY | minioadmin | MinIO 访问密钥 |
| MINIO_SECRET_KEY | （必填） | MinIO 密钥 |
| NEO4J_URI | bolt://neo4j:7687 | Neo4j 连接地址 |
| NEO4J_AUTH | neo4j/password | Neo4j 认证 |
| APP_PORT | 8080 | 后端应用端口 |
| APP_TOKEN_SECRET | （必填） | JWT Token 密钥 |
| NGINX_PORT | 80 | Nginx 端口 |
| OLLAMA_URL | http://host.docker.internal:11434 | Ollama 地址 |
| SPRING_PROFILE | prod | Spring Boot Profile（compose 中映射为 SPRING_PROFILES_ACTIVE） |

## 后端部署

```bash
# 构建 JAR
mvn package -DskipTests

# 构建 Docker 镜像
docker build -f docker/Dockerfile.backend -t dy01-backend .

# 运行
docker run -d --name dy01-backend \
  -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e DB_HOST=postgres \
  -e REDIS_HOST=redis \
  dy01-backend
```

## 前端部署

```bash
# 构建
cd frontend-react
npm run build

# 构建 Docker 镜像
docker build -f docker/Dockerfile.frontend -t dy01-frontend .

# 运行（Nginx 服务静态文件 + 反代 API）
docker run -d --name dy01-frontend -p 80:80 dy01-frontend
```

## Nginx 网关

`docker/nginx/gateway.conf` 配置要点：

- 前端静态文件：`/` → React SPA
- API 代理：`/api/*` → 后端 8080
- SSE 路径：禁用 `proxy_buffering`
- WebSocket：升级连接
- 安全头：HSTS、X-Frame-Options 等

## 数据库初始化

首次部署需执行 SQL 初始化：

```bash
# 进入 PostgreSQL 容器
docker exec -it dy-postgres psql -U postgres -d douyin_operations

# 执行初始化脚本
\i /path/to/sql/init-all.sql
```

### Flyway 迁移

项目使用 Flyway 管理增量迁移（`src/main/resources/db/migration/V*.sql`）。根 `application.yml` 默认 `FLYWAY_ENABLED=false`；**本地开发** `application-dev.yml` 已 `flyway.enabled: true`；**`application-docker.yml`（profile `docker`）** 与生产一致：`ddl-auto: validate` + `flyway.enabled: true`，首次启动依赖 Flyway 执行未应用脚本。

生产 / 容器部署请显式启用（若未用 `docker` profile）：

```bash
FLYWAY_ENABLED=true mvn spring-boot:run
# 或 Docker
-e FLYWAY_ENABLED=true
```

**重要迁移**：V043 为 `live_session` 新增 `session_type` 列（2 小时聊天式场次类型）。若未启用 Flyway，可手动执行：

```sql
ALTER TABLE live_session ADD COLUMN IF NOT EXISTS session_type VARCHAR(32) DEFAULT 'standard';
```

## 健康检查

所有服务均配置 Docker healthcheck：
- PostgreSQL: `pg_isready`
- Redis: `redis-cli ping`
- RabbitMQ: `rabbitmq-diagnostics ping`
- Elasticsearch: `curl /_cluster/health`
- Milvus: `curl /healthz`
