# DY05 - 抖音运营一站式 SaaS 平台（自 dy02 演进）

多租户抖音运营管理平台，覆盖账号管理、短视频策划、直播话术、AI 内容生成、知识自进化等核心业务场景。

**文档**：**从 [`docs/README.md`](docs/README.md) 进入**（现行仅维护索引 + SSOT + BUILD；历史材料在 `docs/_archive/legacy-from-dy02/`）。开发与命令细节见根目录 **`CLAUDE.md`**。

## 技术栈

| 层 | 技术 | 版本 |
|----|------|------|
| 运行时 | JDK | 17 |
| 后端框架 | Spring Boot | 3.3.7 |
| ORM | Spring Data JPA + Hibernate 6 | -- |
| 数据库 | PostgreSQL | 15+ |
| 缓存 | Redis | 7 |
| 消息队列 | RabbitMQ | 3 |
| 搜索引擎 | Elasticsearch | 8.15 |
| 向量数据库 | Milvus (可选) | 2.6 |
| API 文档 | SpringDoc OpenAPI | 2.6.0 |
| 前端框架 | React | 18 |
| 前端构建 | Vite | 6 |
| 类型系统 | TypeScript | 5.7 |
| UI 组件库 | MUI (Material UI) | 6 |
| 状态管理 | Zustand | 5 |
| 部署 | Docker Compose + Nginx | -- |

## 快速开始

### 环境要求

- JDK 17+
- Node.js 18+ / npm 9+
- Docker 及 Docker Compose
- Maven 3.9+

### 1. 获取代码

```bash
# dy05 为独立工作副本时，在本地创建仓库或从 dy02 复制后初始化 git
cd dy05
```

### 2. 启动依赖服务

```bash
docker compose -f docker/docker-compose.yml up -d postgres redis rabbitmq elasticsearch
```

服务端口一览:

| 服务 | 端口 |
|------|------|
| PostgreSQL | 5433 |
| Redis | 6380 |
| RabbitMQ | 5672 (AMQP) / 15672 (管理界面) |
| Elasticsearch | 9200 |

### 3. 初始化数据库

```bash
psql -h localhost -p 5433 -U postgres -d douyin_operations -f sql/init.sql
```

### 4. 启动后端

```bash
mvn compile
mvn -pl douyin-operations-app -am spring-boot:run
```

后端默认监听 `http://localhost:8080`，自动使用 `dev` Profile。

### 5. 启动前端

```bash
cd front
npm install
npm run dev
```

前端开发服务器监听 `http://localhost:3000`，`/api` 请求代理到后端 `localhost:8080`。

### 6. 访问应用

| 地址 | 说明 |
|------|------|
| http://localhost:3000 | 前端应用 |
| http://localhost:8080/swagger-ui.html | API 文档 (Swagger UI) |
| http://localhost:8080/actuator/health | 健康检查 |

## 项目结构

后端为 **Maven 多模块**（详见 **`docs/BUILD.md`**）：业务域分布在 **`douyin-operations-{contract,common,platform,integration,asset,content,intelligence,douyin,payment,live,shortvideo}`**；**可执行模块**为 **`douyin-operations-app`**（主类 `cn.gaifan.douyinOperations.DouyinOperationsApplication`、Flyway、少量跨域 glue）。包名仍为 `cn.gaifan.douyinOperations.module.*`。

```
dy05/
├── pom.xml                      # 聚合父工程（packaging=pom）
├── douyin-operations-contract/  # 契约
├── douyin-operations-common/    # common 包主体
├── douyin-operations-platform/  # auth / config / log / system 等
├── douyin-operations-integration/
├── douyin-operations-asset/
├── douyin-operations-content/
├── douyin-operations-intelligence/
├── douyin-operations-douyin/
├── douyin-operations-payment/
├── douyin-operations-live/
├── douyin-operations-shortvideo/
├── douyin-operations-app/       # 可执行 Jar、Flyway、跨域 glue（如 dashboard）
│   └── src/main/resources/      # application*.yml、db/migration（Flyway）等
├── sql/                         # SQL 建表与参考脚本（增量以 Flyway 为准，见 sql/README.md）
├── front/                       # React 前端
│   ├── src/
│   │   ├── api/
│   │   ├── pages/
│   │   ├── components/
│   │   └── …
│   └── package.json
├── docker/
│   └── docker-compose.yml
└── CLAUDE.md
```

## 核心功能模块

| 模块 | 说明 |
|------|------|
| 认证与权限 (auth) | 多角色权限体系 (管理员/机构/达人)、JWT 认证、OAuth 登录、资源权限控制 |
| 抖音账号 (douyin) | 多账号绑定、OAuth 授权、人设管理、产品管理 |
| 短视频 (shortvideo) | 视频数据管理、爆款分析、AI 创作向导、脚本生成 |
| 直播 (live) | 直播场次管理、话术编排、实时监控、数据分析 |
| 话术生成 (script) | AI 话术生成、违规词检测、模板管理 |
| 文案库 (copy) | 文案管理、审批流、模板库 |
| AI 引擎 (ai) | 知识库 (Milvus + ES 混合检索)、AI 生成、自进化引擎 |
| 文件存储 (storage) | 文件上传/下载、分块上传、可恢复上传 |
| 系统配置 (config) | 系统参数、行业分类管理 |
| 支付 (payment) | 订单管理、支付集成、退款处理 |
| A/B 测试 (abtest) | 实验管理、变体配置、效果分析 |
| 系统监控 (system) | 健康检查、API 日志、Prometheus 指标 |

## 开发指南

### 分层架构

每个业务模块遵循统一的分层结构:

```
module/<模块名>/
├── controller/     XxxController.java       -- REST 接口 (@PostMapping)
├── entity/         Xxx.java                 -- JPA 实体
├── repository/     XxxRepository.java       -- JpaRepository + JpaSpecificationExecutor
├── service/
│   ├── XxxService.java                      -- 接口
│   └── impl/XxxServiceImpl.java             -- 实现
└── vo/
    ├── XxxSearchVO.java                     -- 查询参数 (继承 BasicQueryDto)
    ├── XxxSaveVO.java                       -- 保存入参
    └── XxxVO.java                           -- 返回值
```

### 命名规范

| 类型 | 规则 | 示例 |
|------|------|------|
| 数据库表名 | 蛇形 + 模块前缀 | `auth_user`, `sv_video_data` |
| Entity | 大驼峰 | `AuthUser`, `SvVideoData` |
| Controller | 资源名 + Controller | `AuthUserController` |
| Service | 资源名 + Service / ServiceImpl | `AuthUserService` |
| Repository | 资源名 + Repository | `AuthUserRepository` |
| VO | 资源名 + SearchVO / SaveVO / VO | `AuthUserSearchVO` |
| API 路径 | `/api/v1/模块/资源/动作` | `/api/v1/auth/user/list` |
| 错误码常量 | 全大写下划线 | `AI_QUOTA_EXCEEDED` |

### API 规范

- 所有 API 统一使用 POST 方法 (含查询)
- 请求体为 JSON，响应统一使用 `RESTResult<T>` 封装
- 认证方式: Bearer Token (`Authorization: Bearer <token>`)
- 每个请求自动注入 traceId，便于链路追踪

### 数据库规范

- 逻辑删除: 所有表含 `deleted INTEGER DEFAULT 0`
- 数据隔离: 用户私有表含 `owner_id BIGINT`，Service 层强制过滤
- 时间字段: `create_time` / `update_time`，Entity 自动维护
- SQL 先行: 先写建表 SQL (`sql/<模块>/schema.sql`)，再写 Entity，`ddl-auto: validate`
- 不使用数据库外键约束，改为应用层校验

## 部署

### Docker Compose 部署

```bash
# 基础服务 (PostgreSQL, Redis, RabbitMQ, Elasticsearch)
docker compose -f docker/docker-compose.yml up -d

# 附加 Milvus 向量数据库
docker compose -f docker/docker-compose.yml --profile ai-builtin up -d

# 附加 Prometheus + Grafana 监控
docker compose -f docker/docker-compose.yml --profile monitoring up -d
```

### 生产环境

通过环境变量注入 `SPRING_PROFILES_ACTIVE=prod`，生产配置中应覆盖数据库连接、Redis 地址、密钥等敏感参数。

## 常用命令

```bash
# 后端
mvn compile                                     # 编译检查（全模块）
mvn -pl douyin-operations-app -am spring-boot:run   # 启动（推荐）
mvn test                                        # 全部测试
mvn test -Dtest=AuthUserServiceTest              # 单个测试类
mvn package -DskipTests                         # 打包

# 前端 (工作目录: front/)
npm install                                     # 安装依赖
npm run dev                                     # 开发服务器
npm run build                                   # 构建
npm run type-check                              # TypeScript 类型检查
npm run test                                    # Vitest 单次运行
npm run test:coverage                           # 覆盖率报告
```

## 参考资源

- [awesome-design-md](https://github.com/VoltAgent/awesome-design-md)：各品牌/产品风格的 `DESIGN.md` 合集，可复制到项目根目录，供 AI 辅助实现与参考站点一致的 UI 气质（与现有 MUI 规范配合使用）。

## 贡献指南

1. 从 `master` 分支创建功能分支，分支名格式: `feat/<模块>-<功能描述>` 或 `fix/<问题描述>`
2. 遵循项目的分层架构和命名规范
3. 新增错误码需在三处同步: `ErrorCode.java`、`error-codes.ts`、`docs/04-错误码注册表.md`
4. 确保 `mvn compile` 和 `npm run type-check` 通过
5. 提交 Pull Request，附上变更说明

## 许可证

MIT License
