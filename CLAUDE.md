# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Karpathy 行为指南（andrej-karpathy-skills）

本仓库已接入 [forrestchang/andrej-karpathy-skills](https://github.com/forrestchang/andrej-karpathy-skills) 的核心原则，与下文项目说明 **合并使用**。灵感来源：[Andrej Karpathy 对 LLM 写代码的常见问题的观察](https://x.com/karpathy/status/2015883857489522876)。中文说明见上游 [README.zh.md](https://github.com/forrestchang/andrej-karpathy-skills/blob/main/README.zh.md)。

| 原则 | 要点 |
|------|------|
| **编码前思考** | 明确假设；不确定则提问；有歧义时列出选项，不默默选一种；能说清更简单做法时要说 |
| **简洁优先** | 最少代码解决问题；不增加未要求的功能/抽象/「可配置」；不为不可能场景堆异常分支 |
| **精准修改** | 只改与当前任务相关的代码；不顺便格式化/重构邻域；匹配现有风格；仅清理 **本次改动** 产生的无用引用 |
| **目标驱动** | 把需求写成可验证标准（如：先写失败用例再修通、重构前后测试仍过）；多步任务写简短计划与每步验收 |

**Cursor**：规则文件为 **`.cursor/rules/karpathy-guidelines.mdc`**（`alwaysApply: true`），与本文互补。

## 项目简介

多租户抖音运营一站式 SaaS 平台（dy02），覆盖账号管理、短视频策划、直播话术、AI 内容生成、知识自进化等场景。

本项目由 **dy02** 复制演进为 **dy05**（模块化单体路线）。**产品目标**：打造护肤品与彩妆类直播场次 GMV 千万级。业务边界见 **`docs/_archive/legacy-from-dy02/00-业务范围.md`**（归档）；**文档索引**见 **`docs/README.md`**。

## 技术栈

| 层 | 技术 | 版本 |
|----|------|------|
| 运行时 | JDK | 17 |
| 后端框架 | Spring Boot | 3.3.7 |
| ORM | Spring Data JPA + Hibernate 6 | — |
| 数据库 | PostgreSQL | 15+ |
| 数据库迁移 | Flyway | — |
| 缓存 | Redis 7（L2）+ Caffeine（L1）| 端口 6380 |
| 消息队列 | RabbitMQ 3 | 端口 5672 |
| 搜索 | Elasticsearch 8.15 | 端口 9200 |
| 向量库 | Milvus 2.6（可选）| 端口 19530 |
| 知识图谱 | Neo4j 5.23（可选）| — |
| API 文档 | SpringDoc OpenAPI | 2.6.0 |
| 监控 | Micrometer + OpenTelemetry | — |
| 弹性 | Resilience4j | 2.1.0 |
| AI 集成 | Spring AI | 1.0.0-M4 |
| 测试框架 | JUnit 5 + Mockito + AssertJ | — |
| 容器测试 | TestContainers + Embedded PostgreSQL | — |
| 前端框架 | React | 18.3 |
| 构建工具 | Vite | 6.0 |
| 类型系统 | TypeScript | 5.7 |
| UI 组件库 | MUI (Material-UI) | 6.4 |
| 数据表格 | MUI X Data Grid | 7.28 |
| 状态管理 | Zustand + TanStack React Query | 5.0 + 5.64.2 |
| HTTP 客户端 | Axios | 1.7 |
| 图表库 | ECharts | 6.0 |
| 流程图 | @xyflow/react + @dagrejs/dagre | 12.10.2 + 3.0.0 |
| 拖拽 | @dnd-kit | 6.3.1 |
| 国际化 | i18next | 26.0.1 |
| 测试框架 | Vitest + React Testing Library | — |

## 常用命令

**Windows 控制台中文乱码**：先执行 `chcp 65001`。

```bash
# 启动依赖服务
docker compose -f docker/docker-compose.yml up -d postgres redis rabbitmq elasticsearch
docker compose -f docker/docker-compose.yml up -d milvus  # 可选：向量库
docker compose -f docker/docker-compose.yml up -d neo4j   # 可选：知识图谱
docker compose -f docker/docker-compose.yml down          # 停止所有服务
docker compose -f docker/docker-compose.yml logs -f <service>  # 查看服务日志

# 后端
mvn compile                                     # 编译检查
mvn -pl douyin-operations-app -am spring-boot:run   # 启动开发服务器（端口 8080）；根目录勿裸跑 spring-boot:run
mvn test                                        # 运行所有测试
mvn test -Dtest=AuthUserServiceTest             # 运行单个测试类
mvn test -Dtest="AuthUserServiceTest#testSearch" # 运行单个测试方法
mvn package -DskipTests                         # 打包（跳过测试）
mvn verify                                      # 运行集成测试（含 TestContainers）
mvn clean                                       # 清理构建产物

# 前端（工作目录：front/）
npm install                                     # 安装依赖
npm run dev                                     # 开发服务器（端口 3000，代理 /api → localhost:8080）
npm run build                                   # 生产构建
npm run type-check                              # TypeScript 类型检查
npm run test                                    # Vitest 单次运行
npm run test:watch                              # Vitest 监听模式
npm run test:coverage                           # 覆盖率报告
npm run preview                                 # 预览生产构建
npm run lint                                    # ESLint 检查（如果配置）
```

## 后端架构

### 包结构（多模块）

- **`douyin-operations-common`**：`common/`（config、constant、exception、filter、util、vo、aspect 等主体）。
- **各业务子模块**（`platform`、`integration`、`asset`、`content`、`intelligence`、`douyin`、`payment`、`live`、`shortvideo` 等）：`module/<域>/`（controller / entity / repository / service / vo），详见 **`docs/BUILD.md`**。
- **`douyin-operations-app`**：仅保留 **入口与少量 glue**（如 `DouyinOperationsApplication`、`module.dashboard`、部分 `config`、跨 live/shortvideo 桥接）；**勿**假设全部业务代码仍在 app。

**IDE**：请打开仓库根 **`dy05`**，由 Maven 导入各模块；**勿**将 `java.exe -cp <根目录>/target/classes` 与短类名当作启动方式。

### 模块分层约定

每个模块严格遵循：
```
module/<模块名>/
├── controller/     XxxController.java      — REST 接口（@PostMapping）
├── entity/         Xxx.java                — JPA 实体（@SQLRestriction("deleted = 0")）
├── repository/     XxxRepository.java      — JpaRepository + JpaSpecificationExecutor
├── service/
│   ├── XxxService.java                     — 接口
│   └── impl/XxxServiceImpl.java            — 实现（JPA Specification 动态查询）
└── vo/
    ├── XxxSearchVO.java                    — 查询参数（继承 BasicQueryDto）
    ├── XxxSaveVO.java                      — 保存入参（@Valid 校验）
    └── XxxVO.java                          — 返回值
```

### 核心基类

- **`RESTResult<T>`**：统一响应体 `{ status, message, data, traceId, timestamp }`，成功码 200
  - 静态工厂方法：`success(data)`、`error(status, message)`、`dataNull()`、`forbidden()`
  - 前端自动解包 `data` 字段（见 `front/src/utils/request.ts` 响应拦截器）
- **`BasicQueryDto`**：分页基类，`page`（0-indexed）、`rows`（1-1000，默认30）、`sortName`、`sortOrder`
  - 内置参数校验：`validateParams()` 方法自动修正非法值
- **`PageResultVO<T>`**：分页结果 `{ total, list, pageNum, pageSize }`
- **`ErrorCode`**：错误码常量类（需与 `docs/_archive/legacy-from-dy02/04-错误码注册表.md` 和前端保持一致；修订时请同步迁移至 `docs/modules/` 或新注册表）

### API 规范

- **统一 POST**：所有业务 API 使用 POST 方法（含查询），详见 `docs/adr/001-统一POST接口.md`
  - 例外：SSE 流式接口（部分 GET）、OAuth 回调（GET）、Prometheus metrics（GET）、部分 DELETE/PUT 接口
- **路径模式**：`/api/v1/<模块>/<资源>/<动作>`
  - 查询：`/list`、`/search`、`/get`
  - 修改：`/save`（新增或更新）、`/delete`
- **认证**：Bearer Token（`Authorization: Bearer <token>`），由 Spring Security + 自定义 Filter 校验
- **SSE 流式接口**：路径含 `-sse`、`chat-stream`、`-stream` 后缀，前端使用 `ssePost` 或 `EventSource`

### 数据库规范

- **逻辑删除**：所有表含 `deleted INTEGER DEFAULT 0`，Entity 用 `@SQLRestriction("deleted = 0")`
- **数据隔离**：用户私有表含 `owner_id BIGINT`，Service 层强制过滤（在 Specification 中添加 `ownerId` 条件）
- **时间字段**：`create_time`/`update_time`，Entity 用 `@PrePersist`/`@PreUpdate` 自动维护
- **SQL 先行**：先写 SQL 建表（`sql/<模块>/schema.sql`），再写 Entity，`ddl-auto: validate`
- **跨模块关联**：不使用数据库外键，改为应用层校验（见 `docs/adr/002-无数据库外键.md`）
- **数据库迁移**：使用 Flyway，迁移文件位于 `douyin-operations-app/src/main/resources/db/migration/`
  - 命名规范：`V001__description.sql`、`V002__description.sql`
  - 开发环境默认禁用（`flyway.enabled: false`），生产环境启用

### 缓存策略

- **L1 缓存**：Caffeine 本地内存缓存，配置在 `CacheConfig.java`
- **L2 缓存**：Redis 分布式缓存（端口 6380）
- **缓存层级**：优先查询 L1，未命中则查询 L2，最后查询数据库
- **线程安全**：多线程共享缓存使用 `AtomicReference` + `AtomicLong`

### 监控与可观测性

- **指标收集**：Micrometer + Prometheus，端点 `/actuator/prometheus`
- **分布式追踪**：OpenTelemetry 集成
- **自定义指标**：
  - `http.server.requests` - HTTP 请求统计
  - `api.response.time` - API 响应时间
  - `data.operation.duration` - 数据操作耗时
- **健康检查**：`/actuator/health`

### 弹性与容错

- **Resilience4j**：用于限流、熔断、重试
- **并行查询超时**：Milvus + Elasticsearch 并行查询带超时控制
- **长连接支持**：Tomcat connection-timeout 30 分钟（适配 AI 长请求）

### 测试基础设施

- **单元测试**：JUnit 5 + Mockito + AssertJ
- **集成测试**：TestContainers（Docker 容器化测试）+ Embedded PostgreSQL
- **REST API 测试**：REST Assured
- **覆盖率**：JaCoCo（配置在 pom.xml）
- **测试数据库**：H2 in-memory（快速测试）或 Embedded PostgreSQL（真实环境）

### JPA Specification 动态查询

使用 `JpaSpecificationExecutor` 构建动态查询（见 `docs/adr/003-Specification动态查询.md`）：

```java
Specification<Entity> spec = (root, query, cb) -> {
    List<Predicate> predicates = new ArrayList<>();

    // 数据隔离（必须）
    predicates.add(cb.equal(root.get("ownerId"), userId));

    // 动态条件
    if (StringUtils.hasText(vo.getKeyword())) {
        predicates.add(cb.like(root.get("name"), "%" + vo.getKeyword() + "%"));
    }

    return cb.and(predicates.toArray(new Predicate[0]));
};

Page<Entity> page = repository.findAll(spec, pageable);
```

### 模块清单（27个）

| 模块 | 表前缀 | 说明 |
|------|--------|------|
| auth | auth_ | 用户认证与权限 |
| douyin | dy_ | 抖音账号与 OAuth |
| douyinapi | — | 抖音 API 封装 |
| ai | ai_ | 知识库、RAG、自进化引擎 |
| live | live_ | 直播场次与话术 |
| script | sc_ | 脚本与合规检测 |
| product | dy_ | 商品管理 |
| shortvideo | sv_ | 短视频策划 |
| copy | cp_ | 文案库与模板 |
| agent | agent_ | 智能体对话 |
| payment | pay_ | 支付与订单 |
| abtest | ab_ | A/B 实验 |
| config | sys_config_ | 系统配置 |
| system | sys_ | 监控与告警 |
| storage | sys_file_ | 文件存储上传 |
| log | sys_log_ | 审计与操作日志 |
| wecom | wecom_ | 企业微信推送 |
| messaging | msg_ | 消息通知 |
| sms | sms_ | 短信服务 |
| workflow | workflow_ | 工作流自动化 |
| search | — | 统一搜索（跨模块）|
| attribution | — | 归因分析 |
| dashboard | — | 管理驾驶舱 |
| guiguiya | — | 热点话题同步 |
| slangdict | — | 行业俚语词典 |
| tianapi | — | 天API第三方数据 |
| common | — | 跨模块共享工具 |

## 前端架构

### 目录结构

```
front/
├── src/
│   ├── api/              API 调用层（Axios 封装）
│   ├── components/       可复用组件
│   │   ├── base/         基础组件（StandardDataGrid, StandardDialog...）
│   │   └── layout/       布局组件（AppLayout, Sidebar...）
│   ├── pages/            页面组件（按模块组织）
│   ├── router/           React Router 路由配置
│   ├── stores/           Zustand 状态管理
│   ├── types/            TypeScript 类型定义
│   └── utils/            工具函数（request.ts, auth.ts...）
└── vite.config.ts        Vite 配置（代理 /api → localhost:8080）
```

### UI 与设计说明（可选）

需要贴近某类公开产品的视觉气质时，可从 [awesome-design-md](https://github.com/VoltAgent/awesome-design-md) 选取对应的 `DESIGN.md` 放到仓库根目录，作为与 MUI 并行的设计说明文档供实现参考。

### 前端规范

- **API 调用**：统一通过 `src/api/` 模块，返回类型为 `RESTResult<T>`
  - `request.ts` 响应拦截器自动解包 `data` 字段，业务代码直接获得数据
  - 401/403 自动清除 token 并跳转登录页
- **路由**：使用 React Router v6，路径与后端模块对应
- **状态管理**：
  - **Zustand stores**：全局状态（authStore, liveStore, formDraft, industryBrainStore, liveEditStore, liveGenStore, liveProductStore, modal, pagination, recentVisits, ui, user）
  - **TanStack React Query**：服务端状态管理（useQuery, useMutation），635+ 使用实例
- **组件命名**：大驼峰，页面组件以 `Page` 结尾
- **类型定义**：与后端 VO 对应，放在 `src/types/`
- **基础组件库**：20+ 标准组件（StandardDataGrid, FormDialog, ConfirmDialog, DataTable, DateRangePicker, EmptyState, FilterPanel, KpiCard, LoadingButton, PageHeader, PageSkeleton, RouteErrorBoundary, RowActions, SearchInput, StandardToolbar 等）
- **Context Providers**：ToastContext（notistack）, AiChatContext, CoreDataContext, EditorContext, GenerationContext, QualityContext

### 前端常见问题

**类型安全问题（高优先级）**：
- **避免 `any` 类型**：所有 props、函数参数、返回值必须明确类型
  - ❌ 错误：`function Toolbar(props: any)`
  - ✅ 正确：`interface ToolbarProps { onBack: () => void }; function Toolbar(props: ToolbarProps)`
- **避免不安全的类型转换**：不要使用 `as unknown as` 双重转换
  - ❌ 错误：`(res as unknown as VersionRow[])`
  - ✅ 正确：定义正确的 API 返回类型或使用类型守卫
- **API 返回类型**：所有 API 调用必须指定泛型类型，避免 `any[]` 或 `Record<string, unknown>`
  - ❌ 错误：`request.post<any[]>('/api/list')`
  - ✅ 正确：`request.post<ProductVO[]>('/api/list')`

**类型定义重复问题**：
- **单一数据源原则**：同一类型只在一处定义，其他地方导入使用
  - 如果类型在 hook 中定义（如 `useProductScriptGeneration.ts`），不要在页面组件中重复定义
  - 使用 barrel exports（index.ts）统一导出类型
- **类型导入顺序**：优先从 `@/types` 导入，其次从组件/hook 导入

**错误处理问题**：
- **不要使用空 catch 块**：所有 try-catch 必须处理错误
  - ❌ 错误：`try { await api() } catch (e) { }`
  - ✅ 正确：`try { await api() } catch (e) { console.error('操作失败:', e); enqueueSnackbar('操作失败', { variant: 'error' }) }`
- **API 调用错误反馈**：使用 `notistack` 的 `enqueueSnackbar` 给用户反馈

**组件 Props 设计**：
- **避免过长的 props 列表**：超过 10 个 props 应该分组为对象
  - ❌ 错误：`function ConfigPanel({ prop1, prop2, ..., prop97 })`
  - ✅ 正确：`interface ConfigPanelProps { config: ConfigData; handlers: EventHandlers; ui: UIState }`
- **Props 接口命名**：使用 `XxxProps` 命名规范

**状态管理**：
- **避免重复状态**：同一数据不要在多处维护
- **初始化验证**：状态初始值必须是有效值，使用常量而非魔法字符串
  - ❌ 错误：`const [strategy, setStrategy] = useState('blended')`
  - ✅ 正确：`const [strategy, setStrategy] = useState(FUSION_STRATEGIES[0].value)`

## 关键配置

- 后端端口：8080
- 前端端口：3000（开发），5173（Vite 默认）
- 数据库：PostgreSQL `localhost:5433`，Redis `localhost:6380`
- Hikari 连接池：max 40（默认），min-idle 10
- Redis 连接池：max-active 20, max-idle 10, min-idle 5
- Hibernate 批处理：batch_size 100, fetch_size 500
- Profile：默认 `dev`，其他可用 `prod`, `docker`, `highperf`, `ai`, `swagger`
- Milvus 默认开启（`MILVUS_ENABLED=true`），可通过环境变量关闭
- Tomcat connection-timeout: 30 分钟（适配 AI 长请求）

### 环境变量参考

**AI 服务配置**（50+ 变量）：
- 视频生成：`KLING_*`, `MINIMAX_*`, `RUNWAY_*`, `LUMA_*`, `SEEDANCE_*`, `VEO_*`, `WAN_*`, `PIKA_*`
- 音频服务：`ELEVENLABS_*`（语音克隆）
- Milvus：`MILVUS_ENABLED`, `MILVUS_HOST`, `MILVUS_PORT`
- Elasticsearch：`ES_URIS`, `ES_USE_IK`
- AI Embedding：`AI_EMBEDDING_MODEL`, `AI_EMBEDDING_DIM`, `AI_EMBEDDING_BATCH_SIZE`
- 知识库：`AI_KB_CACHE`, `AI_KB_INIT`, `AI_KB_IMPORT_PARALLELISM`
- RAG：`AI_KB_RAG_ENABLED`, `AI_KB_RAG_TOP_K`, `AI_KB_RAG_MIN_SCORE`

### Docker Compose 服务

**核心服务**（默认启动）：
- PostgreSQL 15（端口 5433）
- Redis 7（端口 6380）
- RabbitMQ 3（端口 5672, 管理界面 15672）
- Elasticsearch 8.15（端口 9200）

**可选服务**：
- Milvus 2.6（端口 19530）- 向量检索
- Neo4j 5.23 - 知识图谱（行业大脑）
- Redis HA 配置（高可用）
- 高性能配置覆盖（highperf profile）

## 新模块开发流程

1. 撰写模块设计文档 → **`docs/modules/<模块>/00-大纲.md`** … `08-测试与验收.md`（历史范文见 `docs/_archive/legacy-from-dy02/modules/<模块>/`）
2. 编写 SQL → `sql/<模块>/schema.sql`
3. 执行 SQL 建表
4. Entity（@Entity + @Table + @SQLRestriction + @PrePersist/@PreUpdate）
5. Repository（extends JpaRepository + JpaSpecificationExecutor）
6. VO（SearchVO extends BasicQueryDto + SaveVO + VO）
7. Service 接口 + ServiceImpl（Specification 动态查询 + 分页）
8. Controller（@PostMapping + @Valid + RESTResult 返回）
9. 前端 API 调用层（`src/api/<模块>.ts`）
10. 前端页面组件（`src/pages/<模块>/`）

## 质量检查要点

### 后端检查

1. `mvn compile` 通过
2. 无 SQL 注入、XSS、硬编码密钥
3. 分页上限检查（`BasicQueryDto` 自动限制 1000）、N+1 查询预防
4. RESTResult 统一返回 + 错误码正确
5. owner_id 数据隔离强制过滤（Service 层 Specification）
6. 错误码三处一致：`docs/_archive/legacy-from-dy02/04-错误码注册表.md` ↔ `ErrorCode.java` ↔ 前端（后续可迁至 `docs/modules/` 单一来源）

### 前端检查

1. **类型检查**：`npm run type-check` 必须通过，无 TypeScript 错误
2. **类型安全**（当前问题统计，截至 2026-04-05）：
   - ✅ `props: any` 使用：已修复（0 处）
   - ⚠️ `as unknown as` 不安全转换：108 处（持续改进中，已从 146 处降至 108 处）
   - ⚠️ `Record<string, unknown>` 泛型使用：313 处（API 层需具体类型定义）
   - 所有新代码必须有明确类型，禁止使用 `any`
   - **近期进展**：
     - `afe1ba80`: 修复 live.ts 导出函数类型安全
     - `14469d14`: 消除 EvolutionPage 11 处不安全转换
     - `4393e442`: 消除 API 层 5 处不安全转换
     - `b4a111b0`: 消除 CopyLibraryPage 7 处不安全转换
     - `0c06bb78`: 消除 5 处 `as unknown as` 转换
3. **错误处理**：
   - ⚠️ 空 catch 块：5+ 处（clipboard、API 调用等）
   - 所有 API 错误必须有用户反馈（enqueueSnackbar）
   - 允许例外：`clipboard.writeText().catch(() => {})` 可以静默失败
4. **类型定义**：
   - 无重复接口定义（同一类型在多处定义）
   - 类型文件位于 `@/types` 或组件/hook 内，不在页面组件中定义
5. **导入规范**：
   - 使用 `@/` 别名，避免 `../../` 相对路径
   - 无未使用的导入（IDE 会标记灰色）
6. **组件规范**：
   - Props 接口命名为 `XxxProps`
   - 超过 10 个 props 的组件需重构为分组对象
7. **构建检查**：`npm run build` 成功，无警告
8. **测试覆盖**：56 个测试文件（Vitest + React Testing Library）

### 前端技术债务清单

**高优先级（影响功能）**：
1. ~~`ProductScriptVersionPage.tsx` - 修复 `as unknown as VersionRow[]` 不安全转换~~ （部分改进中）
2. `ScriptGeneratePanel.tsx` - 重构 97 个参数的 ConfigPanel 函数
3. ~~所有 Toolbar 组件 - 添加 `ToolbarProps` 接口替换 `props: any`~~ （已完成）

**中优先级（代码质量）**：
1. API 层 - 为 313 处 `Record<string, unknown>` 定义具体类型
2. 错误处理 - 为所有 API 调用添加错误反馈
3. 类型重复 - 统一 `ScriptGenStep` 等重复定义的类型

**低优先级（渐进改进）**：
1. 逐步消除 108 处 `as unknown as` 转换（已从 146 处降至 108 处）
2. 优化组件 props 设计，减少参数数量
3. 完善所有组件的 TypeScript 类型覆盖

## 常见陷阱

### 后端陷阱

- **`@Resource` 无 `required` 属性**：Jakarta `@Resource` 不支持 `required=false`，改用 `@Autowired(required = false)`
- **Service 层可选依赖**：跨模块 Service 注入用 `@Autowired(required = false)` + null 检查
- **缓存线程安全**：多线程共享缓存用 `AtomicReference` + `AtomicLong`
- **SSE 流式接口**：路径需含 `-sse` 或 `chat-stream`，否则 `RequestLoggingFilter` 可能缓冲全响应
- **JDK 版本**：以 `pom.xml` 的 JDK 17 为准
- **Flyway 迁移**：开发环境默认禁用（`flyway.enabled: false`），避免自动执行迁移
- **TestContainers**：集成测试需要 Docker 运行，确保 Docker Desktop 已启动
- **并行查询超时**：Milvus + Elasticsearch 并行查询需设置合理超时（避免长时间阻塞）

### 前端陷阱

- **类型安全**：
  - 禁止使用 `any` 类型，所有 props/参数/返回值必须明确类型
  - 禁止 `as unknown as` 双重类型转换，说明类型设计有问题
  - API 调用必须指定泛型：`request.post<ProductVO[]>()` 而非 `request.post<any[]>()`
  - 避免 `Record<string, unknown>` 泛型，定义具体接口类型
- **类型定义重复**：
  - 同一类型只在一处定义（优先 `@/types`，其次 hook/组件）
  - 发现重复定义时，删除本地定义，统一从源头导入
- **错误处理**：
  - 禁止空 catch 块：`catch (e) { }` 必须改为有意义的错误处理
  - API 错误必须用 `enqueueSnackbar` 给用户反馈
- **组件设计**：
  - Props 超过 10 个时分组为对象（config/handlers/ui）
  - 避免在组件内定义超长解构（97 个参数是代码异味）
- **状态初始化**：
  - 使用常量而非魔法字符串：`FUSION_STRATEGIES[0].value` 而非 `'blended'`
  - 确保初始值在有效范围内
- **导入路径**：
  - 使用 `@/` 别名而非相对路径 `../../`
  - Vite 已配置 `@` 指向 `src/`
- **TanStack Query 使用**：
  - 服务端状态优先使用 `useQuery`/`useMutation`，避免在 Zustand 中存储服务端数据
  - 注意查询键（queryKey）的唯一性和缓存失效策略

## 文档入口（dy05 非碎片化）

- **总索引**：`docs/README.md`
- **单一事实来源（重建与治理）**：`docs/SSOT.md`
- **构建与 Maven 模块化**：`docs/BUILD.md`
- **历史全文（只读）**：`docs/_archive/legacy-from-dy02/`

## API 文档索引

详细 API 文档（自 dy02 归档）位于 **`docs/_archive/legacy-from-dy02/api/`**，共 152 个 Controller、1,013 个接口量级。完整索引见 **`docs/_archive/legacy-from-dy02/api/00-INDEX.md`**。

主要模块文档：
- `auth-module.md` - 认证权限（7 Controller, 48 API）
- `live-module.md` - 直播（39 Controller, 192 API）
- `shortvideo-module.md` - 短视频（35 Controller, 185 API）
- `ai-module.md` - AI引擎（18 Controller, 143 API）
- `product-module.md` - 商品（6 Controller, 64 API）
- `script-module.md` - 话术（9 Controller, 41 API）

## 项目统计

**后端**：
- 152 Controller 类
- 262 ServiceImpl 类
- 198 Repository 类
- 370 Entity/VO 类
- 1,882 Java 文件
- 1,013 API 端点

**前端**：
- 530 TypeScript/TSX 文件
- 128 页面目录
- 52 API 模块
- 56 测试文件
- 213 类型定义
- 20+ 基础组件

## 高级特性

### 多提供商视频生成
集成 8+ 视频生成 API：Kling、MiniMax、Runway、Luma、Seedance、Veo、Wan、Pika

### 知识库与 RAG
- 向量检索：Milvus 2.6
- 全文搜索：Elasticsearch 8.15
- 知识图谱：Neo4j 5.23（可选，用于行业大脑）
- 去重与自进化：基于向量相似度的知识去重
- RAG 配置：top-k 检索、最小相似度阈值

### 音频与语音
- ElevenLabs 集成：语音克隆、TTS
- 讯飞 TTS：WebSocket 客户端

### 实时协作
- WebSocket/STOMP 支持
- SSE 流式响应（AI 对话、内容生成）

### 分布式追踪
- OpenTelemetry 集成
- 自定义 span 和 metrics
- Prometheus 指标导出
