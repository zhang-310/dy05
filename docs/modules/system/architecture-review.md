# System 模块架构审查报告

## 模块概述

**模块名称**: system  
**功能定位**: 系统监控与管理（API调用日志、数据同步日志、健康检查、指标收集、外部API网关、告警引擎）  
**技术栈**: Spring Boot 3.3.7 + Micrometer + Spring Actuator + JPA + PostgreSQL  
**审查日期**: 2026-05-08  
**代码位置**: `douyin-operations-platform/src/main/java/.../module/system/`

## 架构评分

| 维度 | 得分 | 说明 |
|------|------|------|
| 模块职责清晰度 | 16/20 | 职责较清晰但范围过宽，包含监控、日志、网关、告警等多个子域 |
| 分层合理性 | 18/20 | Controller-Service-Repository 分层清晰，但部分逻辑耦合 |
| 依赖管理 | 14/15 | 使用 `@Autowired(required=false)` 处理可选依赖，设计合理 |
| 扩展性 | 13/15 | 外部API网关设计良好，但告警引擎缺少实现 |
| 可测试性 | 12/15 | 有测试用例，但覆盖率不足，缺少集成测试 |
| 文档完整性 | 10/15 | 缺少模块级文档，仅有代码注释 |
| **总分** | **83/100** | **等级**: B |

## 架构分析

### 1. 模块结构

```
system/
├── config/                          # 配置层（4个类）
│   ├── ApiCallLogInterceptor        # RestTemplate拦截器，自动记录外部API调用
│   ├── ApiCallLogCleanupScheduler   # 定时清理过期日志
│   ├── ExternalApiHealthCheckScheduler  # 外部API健康检查
│   └── SystemRestTemplateConfig     # RestTemplate配置
├── controller/                      # 控制器层（7个类，89个端点）
│   ├── SystemController             # 系统监控（API日志、同步日志、健康检查）
│   ├── MetricsController            # Prometheus指标端点
│   ├── AlertController              # 告警规则管理
│   ├── MonitoringController         # 监控面板
│   ├── SystemPerformanceController  # 性能监控
│   ├── ExternalApiConfigController  # 外部API配置
│   └── TaxonomyController           # 分类法管理
├── entity/                          # 实体层（5个类）
│   ├── SysApiCallLog                # API调用日志（无deleted字段）
│   ├── SysSyncLog                   # 数据同步日志
│   ├── ExternalApiConfig            # 外部API配置（加密存储密钥）
│   ├── ExternalApiCallLog           # 外部API调用日志
│   └── SysTaxonomyNode              # 分类树节点
├── repository/                      # 仓储层（5个类）
│   ├── SysApiCallLogRepository      # 支持批量删除过期日志
│   ├── SysSyncLogRepository
│   ├── ExternalApiConfigRepository
│   ├── ExternalApiCallLogRepository
│   └── SysTaxonomyNodeRepository
├── service/                         # 服务接口层（7个接口）
│   ├── SystemService                # 核心系统服务
│   ├── MetricsCollectorService      # 指标收集
│   ├── AlertEngineService           # 告警引擎
│   ├── DashboardDataService         # 仪表板数据
│   ├── ExternalApiConfigService     # 外部API配置
│   ├── ExternalApiGateway           # 外部API网关
│   └── TaxonomyService              # 分类法服务
├── service/impl/                    # 服务实现层（6个类）
│   ├── SystemServiceImpl            # 实现API日志、同步日志、健康检查
│   ├── MetricsCollectorServiceImpl
│   ├── AlertEngineServiceImpl
│   ├── DashboardDataServiceImpl
│   ├── ExternalApiConfigServiceImpl
│   ├── ExternalApiGatewayImpl       # 使用Java 11 HttpClient
│   └── TaxonomyServiceImpl
└── vo/                              # 值对象层（6个类）
    ├── AlertRuleVO / AlertRecordVO
    ├── DashboardDataVO
    ├── MetricsVO
    └── ExternalApiConfigSaveVO / ExternalApiConfigSearchVO
```

**文件统计**:
- 总计 40 个 Java 文件
- 7 个 Controller（89 个 API 端点）
- 7 个 Service 接口 + 6 个实现类
- 5 个 Entity + 5 个 Repository
- 6 个 VO + 4 个配置类

### 2. 核心组件

#### 2.1 SystemService - 系统监控核心

**职责**:
- API 调用日志管理（查询、统计、详情）
- 数据同步日志管理（启动、进度更新、完成/失败）
- 系统健康检查（并行检查 7 个依赖服务）
- 系统运行信息（JVM、线程、内存）

**设计亮点**:
```java
// 并行健康检查，5秒超时
Map<String, CompletableFuture<Map<String, Object>>> futures = new LinkedHashMap<>();
futures.put("database", CompletableFuture.supplyAsync(this::checkDatabase));
futures.put("ollama", CompletableFuture.supplyAsync(this::checkOllama));
futures.put("milvus", CompletableFuture.supplyAsync(this::checkMilvus));
// ... 7个服务并行检查
```

**可选依赖处理**:
```java
@Autowired(required = false)
private RedisConnectionFactory redisConnectionFactory;
@Autowired(required = false)
private MilvusServiceClient milvusClient;
// 运行时检查 null，返回 "未配置" 状态
```

#### 2.2 ApiCallLogInterceptor - 自动日志记录

**职责**: 拦截所有 RestTemplate 请求，自动记录到 `sys_api_call_log`

**安全设计**:
- 参数脱敏：`access_token`、`api_key`、`password` 等替换为 `***`
- 支持 JSON 和 Form 数据脱敏
- 响应体截断（最大 2000 字符）

**模块识别**:
```java
private String resolveModule(URI uri) {
    String host = uri.getHost().toLowerCase();
    if (host.contains("douyin.com")) return "douyin";
    if (host.contains("weixin.qq.com")) return "wecom";
    if (host.contains("ollama") || port == 11434) return "ai";
    return "unknown";
}
```

#### 2.3 ExternalApiGateway - 外部API统一网关

**职责**: 统一管理外部 API 调用（天API、DeepSeek、Qwen 等）

**设计特点**:
- 使用 Java 11 `HttpClient`（非 RestTemplate）
- 从 `ExternalApiConfig` 读取配置（加密存储密钥）
- 自动记录调用日志和健康状态
- 10秒连接超时 + 30秒请求超时

**问题**: 未使用 `ApiCallLogInterceptor`，日志记录逻辑重复

#### 2.4 MetricsCollectorService - 指标收集

**职责**: 收集系统指标并暴露 Prometheus 端点

**指标类型**:
- CPU 使用率
- 内存使用率（JVM heap/non-heap）
- 磁盘空间
- JVM 线程数
- 数据库连接池状态

**Prometheus 端点**: `/api/v1/system/metrics/prometheus`（GET）

#### 2.5 AlertEngineService - 告警引擎

**职责**: 告警规则管理、告警检查、告警记录

**状态**: ⚠️ **仅有接口定义，无实现类**（`AlertEngineServiceImpl` 存在但可能为空实现）

### 3. 数据模型

#### 3.1 SysApiCallLog - API调用日志

```sql
CREATE TABLE sys_api_call_log (
    id               BIGSERIAL PRIMARY KEY,
    module           VARCHAR(64) NOT NULL,      -- douyin / ai / live
    api_name         VARCHAR(256) NOT NULL,
    request_url      VARCHAR(512),
    request_method   VARCHAR(16),
    request_params   TEXT,                      -- 脱敏后
    response_status  INTEGER,
    response_body    TEXT,                      -- 截取前2000字符
    status           INTEGER DEFAULT 1,         -- 1=成功 0=失败
    error_message    VARCHAR(512),
    duration_ms      BIGINT,
    user_id          BIGINT,
    create_time      TIMESTAMP DEFAULT NOW()
);
```

**特点**:
- **无 `deleted` 字段**：通过定时任务物理删除过期数据
- 索引优化：`(module, create_time)`、`(status, create_time)`
- 批量删除：`deleteBatchByCreateTimeBefore()` 每次删除 1000 条

#### 3.2 ExternalApiConfig - 外部API配置

```sql
CREATE TABLE external_api_config (
    id                    BIGSERIAL PRIMARY KEY,
    provider_code         VARCHAR(64) NOT NULL,   -- tianapi / deepseek
    provider_name         VARCHAR(128) NOT NULL,
    category              VARCHAR(32),            -- ai / data / media
    base_url              VARCHAR(512),
    api_key_encrypted     VARCHAR(512),           -- 加密存储
    api_secret_encrypted  VARCHAR(512),
    is_enabled            BOOLEAN DEFAULT TRUE,
    priority              INTEGER DEFAULT 0,
    rate_limit_per_min    INTEGER,
    daily_quota           INTEGER,
    monthly_quota         INTEGER,
    last_health_check     TIMESTAMP,
    health_status         VARCHAR(32),            -- healthy / degraded / down
    avg_latency_ms        INTEGER,
    success_rate_pct      FLOAT,
    extra_config          JSONB,
    deleted               INTEGER DEFAULT 0,
    create_time           TIMESTAMP,
    update_time           TIMESTAMP
);
```

**安全设计**:
- API Key 加密存储（`ApiKeyEncryptionService`）
- 健康检查定时更新状态
- 支持优先级和配额管理

### 4. 依赖关系

#### 4.1 模块内依赖

```
SystemController
    └─> SystemService
            ├─> SysApiCallLogRepository
            ├─> SysSyncLogRepository
            ├─> EntityManager (JPQL统计查询)
            └─> 可选依赖（Redis/RabbitMQ/ES/Milvus/BOS）

ApiCallLogInterceptor
    └─> SystemService.saveApiLog() (异步)

ExternalApiGateway
    └─> ExternalApiConfigService
            └─> ApiKeyEncryptionService
```

#### 4.2 跨模块依赖

- **依赖 `common` 模块**: `RESTResult`、`PageResultVO`、`ErrorCode`、`AuthTokenFilter`
- **依赖 `storage` 模块**: `BosStorageService`（健康检查）
- **被依赖**: 其他模块通过 `SystemService.saveApiLog()` 记录日志

#### 4.3 外部依赖

- **Spring Boot Actuator**: 健康检查基础设施
- **Micrometer**: 指标收集
- **Milvus/Elasticsearch/Redis/RabbitMQ**: 可选依赖（通过 `required=false` 处理）

### 5. 设计模式

#### 5.1 拦截器模式（Interceptor Pattern）

`ApiCallLogInterceptor` 实现 `ClientHttpRequestInterceptor`，透明记录所有外部 API 调用。

#### 5.2 网关模式（Gateway Pattern）

`ExternalApiGateway` 统一管理外部 API 调用，封装认证、日志、健康检查逻辑。

#### 5.3 策略模式（Strategy Pattern）

健康检查针对不同服务（Database/Redis/Milvus/ES）使用不同检查策略。

#### 5.4 异步处理（Async Pattern）

```java
@Async
public void saveApiLog(...) {
    // 异步保存日志，不阻塞主流程
}
```

#### 5.5 定时任务（Scheduled Task Pattern）

- `ApiCallLogCleanupScheduler`: 定时清理过期日志
- `ExternalApiHealthCheckScheduler`: 定时健康检查

## 问题清单

### P0 阻塞级问题

**无 P0 问题**

### P1 高优先级问题

1. **告警引擎未实现**
   - **现象**: `AlertEngineService` 接口定义完整，但 `AlertEngineServiceImpl` 可能为空实现
   - **影响**: 告警功能不可用，`AlertController` 的 15 个端点可能返回空数据或报错
   - **建议**: 补充实现或标记为 TODO，避免误导用户

2. **日志记录逻辑重复**
   - **现象**: `ExternalApiGateway` 使用 Java HttpClient，未经过 `ApiCallLogInterceptor`
   - **影响**: 日志记录逻辑在两处维护（拦截器 + 网关），容易不一致
   - **建议**: 统一使用 RestTemplate 或将日志记录逻辑提取为独立服务

3. **缺少 Service 接口实现**
   - **现象**: `DashboardDataService` 接口定义了 6 个方法，但实现类可能为空
   - **影响**: 仪表板功能不完整
   - **建议**: 补充实现或删除未使用的接口

### P2 中优先级问题

4. **模块职责过宽**
   - **现象**: system 模块包含监控、日志、网关、告警、分类法等多个子域
   - **影响**: 模块边界模糊，难以维护
   - **建议**: 拆分为 `monitoring`、`logging`、`gateway`、`alerting` 等独立模块

5. **健康检查超时固定**
   - **现象**: 所有服务健康检查统一 5 秒超时
   - **影响**: 慢服务可能导致整体健康检查超时
   - **建议**: 支持按服务配置超时时间

6. **缺少集成测试**
   - **现象**: 仅有 Controller 单元测试（7 个测试类），缺少 Service 层集成测试
   - **影响**: 无法验证健康检查、日志记录等核心功能
   - **建议**: 使用 TestContainers 编写集成测试

7. **API 日志无保留策略配置**
   - **现象**: 日志清理逻辑硬编码在 Scheduler 中
   - **影响**: 无法灵活调整保留天数
   - **建议**: 通过配置文件控制保留策略（如 `system.log.retention-days=30`）

### P3 低优先级问题

8. **Prometheus 指标格式简化**
   - **现象**: `MetricsController.prometheusMetrics()` 手动拼接字符串
   - **影响**: 不支持 labels、histogram 等高级特性
   - **建议**: 使用 Micrometer 的 `PrometheusMeterRegistry`

9. **缺少 API 限流**
   - **现象**: `ExternalApiConfig` 定义了 `rateLimitPerMin`，但未实现限流逻辑
   - **影响**: 无法防止外部 API 配额耗尽
   - **建议**: 集成 Resilience4j RateLimiter

10. **分类法功能孤立**
    - **现象**: `TaxonomyService` 与其他系统功能无关联
    - **影响**: 模块职责不清晰
    - **建议**: 移至独立模块或 `common` 模块

## 改进建议

### 短期改进（1-2周）

1. **补充告警引擎实现**
   - 实现 `AlertEngineServiceImpl` 的核心方法
   - 添加告警规则执行逻辑（基于 Cron 表达式）
   - 集成通知渠道（企业微信、邮件）

2. **统一日志记录**
   - 将 `ExternalApiGateway` 改为使用 RestTemplate
   - 或将日志记录逻辑提取为 `ApiCallLogService`

3. **添加配置项**
   ```yaml
   system:
     log:
       retention-days: 30
       cleanup-batch-size: 1000
     health-check:
       timeout-seconds: 5
       parallel: true
   ```

4. **补充集成测试**
   - 健康检查测试（使用 TestContainers 启动 Redis/PostgreSQL）
   - API 日志记录测试
   - 外部 API 网关测试（Mock HTTP 服务）

### 中期改进（1-2月）

5. **模块拆分**
   ```
   system/
   ├── monitoring/    # 健康检查、指标收集
   ├── logging/       # API日志、同步日志
   ├── gateway/       # 外部API网关
   └── alerting/      # 告警引擎
   ```

6. **实现 API 限流**
   - 使用 Resilience4j RateLimiter
   - 支持按 provider 配置限流策略
   - 配额耗尽时自动切换备用 provider

7. **增强 Prometheus 集成**
   - 使用 Micrometer 注解（`@Timed`、`@Counted`）
   - 暴露自定义业务指标（API 调用量、错误率）
   - 支持 Grafana 仪表板

8. **优化日志存储**
   - 考虑使用时序数据库（InfluxDB/TimescaleDB）
   - 或使用 Elasticsearch 存储日志（支持全文搜索）

### 长期改进（3-6月）

9. **分布式追踪集成**
   - 集成 OpenTelemetry
   - 自动关联 API 调用链路
   - 支持跨服务追踪

10. **告警规则引擎增强**
    - 支持复杂规则（阈值、趋势、异常检测）
    - 支持告警聚合和降噪
    - 支持告警升级策略

11. **外部 API 智能路由**
    - 基于健康状态自动切换 provider
    - 支持负载均衡（轮询、加权、最少连接）
    - 支持熔断和降级

12. **可观测性平台**
    - 统一日志、指标、追踪查询界面
    - 支持自定义仪表板
    - 支持告警规则可视化配置

## 总结

**整体评价**: system 模块架构设计良好，分层清晰，核心功能（API 日志、健康检查、指标收集）实现完整。使用 `@Autowired(required=false)` 处理可选依赖的设计值得借鉴。

**核心优势**:
1. **并行健康检查**: 使用 `CompletableFuture` 并行检查 7 个服务，5 秒超时
2. **自动日志记录**: `ApiCallLogInterceptor` 透明拦截所有外部 API 调用
3. **参数脱敏**: 自动脱敏敏感参数（`access_token`、`api_key` 等）
4. **外部 API 网关**: 统一管理外部 API 配置、认证、日志、健康检查
5. **可选依赖处理**: 优雅处理 Redis/Milvus/ES 等可选依赖

**主要风险**:
1. **告警引擎未实现**: 15 个告警相关端点可能不可用
2. **日志记录逻辑重复**: 拦截器和网关各自实现日志记录
3. **模块职责过宽**: 包含监控、日志、网关、告警等多个子域
4. **缺少集成测试**: 无法验证核心功能的正确性

**预计工作量**:
- 短期改进（P1 问题）: **5 人日**
- 中期改进（模块拆分 + 限流）: **15 人日**
- 长期改进（分布式追踪 + 智能路由）: **30 人日**

**建议优先级**: P1 > P2 > P3，先补充告警引擎实现和集成测试，再考虑模块拆分。
