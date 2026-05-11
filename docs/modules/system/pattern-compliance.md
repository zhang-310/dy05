# System 模块模式合规性检查报告

## 检查概述

**模块名称**: system  
**检查日期**: 2026-05-08  
**检查标准**: 项目架构规范 + ADR 决策记录  
**检查范围**: Controller (7) + Service (8) + Entity (5) + Repository (5)

**模块特点**: System 模块是平台级监控与管理模块，包含 API 调用日志、同步日志、告警引擎、指标收集、健康检查、外部 API 配置、分类体系等功能。部分实体（日志表）无 `deleted` 字段，采用物理删除策略。

## 合规性评分

| 维度 | 得分 | 说明 |
|------|------|------|
| API 规范 | 16/20 | 大部分符合 POST 规范，但存在 GET 方法混用（符合 ADR-001 例外） |
| 数据访问模式 | 18/20 | 使用 JPA Specification，但部分查询未继承 BasicQueryDto |
| 错误处理 | 14/15 | 使用 ErrorCode 常量，错误处理完善 |
| 缓存策略 | 0/15 | 未使用缓存（监控数据实时性要求高，合理） |
| 数据隔离 | 10/15 | 部分表有 owner_id（Taxonomy），日志表无需隔离 |
| 命名规范 | 15/15 | 命名清晰，符合规范 |
| **总分** | **73/100** | **等级**: C（特殊模块，部分规范不适用）|

## 模式检查清单

### 1. API 规范（ADR-001）✅ 部分合规

#### ✅ 符合规范
- **SystemController**: 所有接口使用 POST（`/api-log/list`, `/api-log/stats`, `/health`, `/info`）
- **AlertController**: 告警规则管理使用 POST（`/alert/rule/create`, `/update`, `/delete`）
- **路径格式**: 符合 `/api/v1/<模块>/<资源>/<动作>` 模式
- **返回值**: 统一使用 `RESTResult<T>` 包装
- **参数校验**: 使用 `@Valid` 注解（AlertController）

#### ⚠️ 例外情况（符合 ADR-001 明确例外）
- **MetricsController**: 
  - `/metrics/prometheus` (GET) - Prometheus scrape 标准协议
  - `/metrics/all` (GET) - 指标查询
  - `/metrics/cpu|memory|disk|jvm|database` (GET) - 各类指标
- **AlertController**: 
  - `/dashboard/overview|alerts|performance|logs|traces|health` (GET) - 仪表板数据

**说明**: GET 方法用于 Prometheus 兼容端点和仪表板数据查询，符合 ADR-001 例外规定。

#### ❌ 违规项
- **MetricsController.getMetric()**: 使用 POST + `@RequestBody Map<String, String>`，应定义专用 VO 类

### 2. 数据访问模式（ADR-003）✅ 基本合规

#### ✅ 符合规范
- **Repository**: 所有 Repository 继承 `JpaRepository` + `JpaSpecificationExecutor`
- **动态查询**: SystemServiceImpl 使用 Specification 构建动态查询
  ```java
  Specification<SysApiCallLog> spec = (root, query, cb) -> {
      List<Predicate> predicates = new ArrayList<>();
      if (module != null && !module.isBlank())
          predicates.add(cb.equal(root.get("module"), module));
      // ...
  };
  ```
- **分页**: 使用 `PageRequest.of(page, rows, Sort.by(...))`
- **排序**: 白名单校验（`API_LOG_SORTABLE`）

#### ⚠️ 改进空间
- **未使用 BasicQueryDto**: SystemController 直接使用 `Map<String, Object>` 接收参数，未继承 `BasicQueryDto`
- **参数校验**: 手动校验分页参数，未利用 `BasicQueryDto.validateParams()`
- **N+1 查询**: 使用 JPQL 聚合查询避免 N+1（`queryStatsByModule`, `queryStatsByApiName`）

### 3. 数据隔离（ADR-004）⚠️ 部分适用

#### ✅ 符合规范
- **SysTaxonomyNode**: 包含 `owner_id` 字段，支持平台预置（ownerId=0）和租户私有节点
- **TaxonomyService**: 查询时合并 `ownerId=0` 和当前用户 `ownerId`
  ```java
  List<Long> owners = List.of(0L, userId);
  ```

#### ⚠️ 特殊情况（合理）
- **SysApiCallLog**: 无 `owner_id` 字段，管理员全局可见（通过 `isAdmin()` 权限控制）
- **SysSyncLog**: 包含 `user_id` 字段，但非 `owner_id` 模式
- **ExternalApiConfig**: 无 `owner_id` 字段，平台级配置

**说明**: System 模块主要是平台级功能，日志和配置表不需要租户隔离，通过角色权限（admin）控制访问。

### 4. 逻辑删除（ADR-005）⚠️ 部分适用

#### ✅ 符合规范
- **ExternalApiConfig**: 包含 `deleted` 字段 + `@SQLRestriction("deleted = 0")`
- **SysTaxonomyNode**: 包含 `deleted` 字段 + `@SQLRestriction("deleted = 0")`

#### ⚠️ 特殊情况（合理）
- **SysApiCallLog**: 无 `deleted` 字段，注释说明"日志通过定时任务物理清理"
- **SysSyncLog**: 无 `deleted` 字段，注释说明"日志通过定时任务物理清理"
- **Repository**: 提供物理删除方法
  ```java
  @Modifying
  @Query("DELETE FROM SysApiCallLog l WHERE l.createTime < :cutoff")
  int deleteByCreateTimeBefore(@Param("cutoff") Timestamp cutoff);
  ```

**说明**: 日志表采用物理删除策略，避免历史数据无限增长，符合监控系统最佳实践。

### 5. 缓存策略 ❌ 未使用

#### 现状
- 所有 Service 方法未使用 `@Cacheable` / `@CacheEvict` 注解
- 未配置 L1 Caffeine 或 L2 Redis 缓存

#### 合理性分析
- **监控数据**: 实时性要求高，不适合缓存（健康检查、指标收集）
- **日志查询**: 数据变化频繁，缓存收益低
- **告警规则**: AlertEngineServiceImpl 使用内存 `ConcurrentHashMap` 存储（简单实现）

**结论**: 对于 System 模块，不使用缓存是合理的设计选择。

### 6. 错误码规范 ✅ 符合规范

#### ✅ 符合规范
- 使用 `ErrorCode` 常量类
  - `ErrorCode.UNAUTHORIZED` - 未登录
  - `ErrorCode.FORBIDDEN` - 无权限
  - `ErrorCode.VALIDATION_FAIL` - 参数校验失败
  - `ErrorCode.DATA_NOT_FOUND` - 数据不存在
  - `ErrorCode.ALERT_RULE_INVALID` - 告警规则无效
  - `ErrorCode.ALERT_RULE_NOT_FOUND` - 告警规则不存在
- 错误信息清晰，中文提示

### 7. 时间字段管理 ✅ 符合规范

#### ✅ 符合规范
- 所有 Entity 使用 `@PrePersist` / `@PreUpdate` 自动维护时间字段
  ```java
  @PrePersist
  public void prePersist() {
      if (createTime == null) createTime = new Timestamp(System.currentTimeMillis());
      if (updateTime == null) updateTime = new Timestamp(System.currentTimeMillis());
  }
  ```

### 8. 权限控制 ✅ 符合规范

#### ✅ 符合规范
- **SystemController**: 所有接口检查 `isAdmin(request)`
  ```java
  if (!isAdmin(request))
      return RESTResult.error(ErrorCode.FORBIDDEN, "无权限访问");
  ```
- **TaxonomyController**: 保存操作限制 admin 角色
- **AlertController**: 未强制 admin 检查（待确认业务需求）

## 违规清单

### P0 严重违规
无

### P1 高优先级违规

#### 1. MetricsController.getMetric() 参数类型不规范
**文件**: `MetricsController.java:62`  
**问题**: 使用 `Map<String, String>` 接收参数，缺少类型安全和校验  
**建议**: 定义 `MetricQueryVO` 类
```java
@Data
public class MetricQueryVO {
    @NotBlank(message = "指标名称不能为空")
    private String name;
}

@PostMapping("/get")
public RESTResult<?> getMetric(@Valid @RequestBody MetricQueryVO vo) {
    return RESTResult.success(metricsCollectorService.getMetricsByName(vo.getName()));
}
```

#### 2. SystemController 未使用 BasicQueryDto
**文件**: `SystemController.java`  
**问题**: 手动解析 `Map<String, Object>` 参数，未利用 `BasicQueryDto` 的分页校验和排序功能  
**建议**: 定义 SearchVO 类继承 `BasicQueryDto`
```java
@Data
public class ApiLogSearchVO extends BasicQueryDto {
    private String module;
    private String apiName;
    private Integer status;
    private String startTime;
    private String endTime;
}
```

#### 3. AlertEngineServiceImpl 使用内存存储
**文件**: `AlertEngineServiceImpl.java:27-30`  
**问题**: 使用 `ConcurrentHashMap` 内存存储，数据不持久化，重启丢失  
**建议**: 
- 短期：添加注释说明"简单实现，生产环境应使用数据库"
- 长期：创建 `alert_rule` 和 `alert_record` 表，使用 JPA 持久化

### P2 中优先级问题

#### 1. AlertController.updateAlertRule() 手动转换 VO
**文件**: `AlertController.java:174-186`  
**问题**: 手动从 Map 转换为 AlertRuleVO，代码冗长且易出错  
**建议**: 直接使用 `@RequestBody AlertRuleVO vo`
```java
@PostMapping("/alert/rule/update")
public RESTResult<?> updateAlertRule(@RequestBody Map<String, Object> request) {
    Long ruleId = Long.parseLong(request.get("ruleId").toString());
    AlertRuleVO vo = convertToAlertRuleVO(request);
    alertEngineService.updateAlertRule(ruleId, vo);
    return RESTResult.success();
}
```
改为：
```java
@PostMapping("/alert/rule/update")
public RESTResult<?> updateAlertRule(@Valid @RequestBody AlertRuleUpdateVO vo) {
    alertEngineService.updateAlertRule(vo.getRuleId(), vo);
    return RESTResult.success();
}
```

#### 2. SystemServiceImpl 使用 @Async 但未配置线程池
**文件**: `SystemServiceImpl.java:203`  
**问题**: `saveApiLog()` 使用 `@Async` 异步保存，但未配置专用线程池  
**建议**: 在 `AsyncConfig` 中配置线程池
```java
@Bean(name = "systemLogExecutor")
public Executor systemLogExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(2);
    executor.setMaxPoolSize(5);
    executor.setQueueCapacity(100);
    executor.setThreadNamePrefix("system-log-");
    executor.initialize();
    return executor;
}

// 使用时指定线程池
@Async("systemLogExecutor")
public void saveApiLog(...) { ... }
```

#### 3. 缺少 API 文档注释
**文件**: 多个 Controller  
**问题**: 部分接口缺少 `@Operation` 注解，Swagger 文档不完整  
**建议**: 为所有接口添加 `@Operation(summary = "...")` 注解

### P3 低优先级问题

#### 1. MetricsController.prometheusMetrics() 异常处理粗糙
**文件**: `MetricsController.java:38-42`  
**问题**: 解析失败时默认返回 0.0，可能掩盖问题  
**建议**: 记录警告日志
```java
try {
    value = Double.parseDouble(metric.getValue().split("/")[0]);
} catch (Exception e) {
    log.warn("Failed to parse metric value: name={}, value={}", 
             metric.getName(), metric.getValue());
    value = 0.0;
}
```

#### 2. SystemServiceImpl 字符串拼接构建 JPQL
**文件**: `SystemServiceImpl.java:106-111`  
**问题**: 使用 StringBuilder 拼接 JPQL，可读性差  
**建议**: 使用 Specification 或 QueryDSL

#### 3. AlertEngineServiceImpl.executeAlertChecks() 使用随机数模拟
**文件**: `AlertEngineServiceImpl.java:146`  
**问题**: 使用 `Math.random()` 模拟指标值，非真实监控  
**建议**: 集成 Micrometer 获取真实指标

## 改进建议

### 立即修复（P1）

1. **定义专用 VO 类**（1 人日）
   - 创建 `ApiLogSearchVO`, `SyncLogSearchVO`, `MetricQueryVO`
   - 继承 `BasicQueryDto` 获得分页校验能力
   - 替换 Controller 中的 `Map<String, Object>` 参数

2. **告警引擎持久化**（2 人日）
   - 创建 `alert_rule` 和 `alert_record` 表
   - 创建对应 Entity 和 Repository
   - 迁移 AlertEngineServiceImpl 到数据库存储

3. **配置异步线程池**（0.5 人日）
   - 在 `AsyncConfig` 中配置 `systemLogExecutor`
   - 更新 `@Async` 注解指定线程池

### 短期改进（P2）

1. **重构 AlertController.updateAlertRule()**（0.5 人日）
   - 定义 `AlertRuleUpdateVO` 包含 ruleId
   - 移除手动转换逻辑

2. **完善 API 文档**（1 人日）
   - 为所有接口添加 `@Operation` 注解
   - 添加请求/响应示例

3. **改进异常处理**（0.5 人日）
   - 为 MetricsController 添加日志
   - 统一异常处理策略

### 中期优化（P3）

1. **重构 JPQL 查询**（1 人日）
   - 将 SystemServiceImpl 中的字符串拼接改为 Specification
   - 提高代码可读性和类型安全

2. **集成真实监控指标**（3 人日）
   - 替换 AlertEngineServiceImpl 中的随机数模拟
   - 集成 Micrometer 获取 JVM、数据库、HTTP 等指标
   - 实现真实的告警触发逻辑

3. **日志清理定时任务**（1 人日）
   - 实现 `@Scheduled` 定时任务清理过期日志
   - 配置保留天数（如 30 天）
   - 使用批量删除避免长事务

## 架构亮点

### 1. 日志表物理删除策略
- **设计**: SysApiCallLog 和 SysSyncLog 无 `deleted` 字段，通过定时任务物理删除
- **优点**: 避免历史数据无限增长，符合监控系统最佳实践
- **实现**: Repository 提供 `deleteByCreateTimeBefore()` 和批量删除方法

### 2. 权限控制清晰
- **设计**: SystemController 所有接口检查 `isAdmin()`
- **优点**: 监控数据仅管理员可见，安全性高
- **实现**: 使用 `AuthTokenFilter.getRoleCode(request)` 统一鉴权

### 3. 健康检查并行执行
- **设计**: SystemServiceImpl.checkHealth() 使用 `CompletableFuture` 并行检查多个服务
- **优点**: 减少总耗时，5 秒超时控制
- **实现**: 
  ```java
  Map<String, CompletableFuture<Map<String, Object>>> futures = new LinkedHashMap<>();
  futures.put("database", CompletableFuture.supplyAsync(this::checkDatabase));
  // ...
  Map<String, Object> v = e.getValue().get(5, TimeUnit.SECONDS);
  ```

### 4. 可选依赖注入
- **设计**: 使用 `@Autowired(required = false)` 注入可选服务（Redis, RabbitMQ, Milvus, Elasticsearch）
- **优点**: 服务未配置时不影响启动，健康检查返回 "未配置"
- **实现**: 
  ```java
  @Autowired(required = false)
  private MilvusServiceClient milvusClient;
  
  if (milvusClient == null) {
      return Map.of("status", "DOWN", "provider", "未配置");
  }
  ```

### 5. Prometheus 兼容端点
- **设计**: MetricsController 提供 `/metrics/prometheus` 端点，输出标准 Prometheus 格式
- **优点**: 可直接被 Prometheus 抓取，无需额外适配
- **实现**: 
  ```java
  sb.append("# HELP ").append(metric.getName()).append(" ").append(metric.getUnit()).append("\n");
  sb.append("# TYPE ").append(metric.getName()).append(" gauge\n");
  sb.append(metric.getName()).append(" ").append(value).append("\n");
  ```

## 总结

**整体合规性**: C 级（73/100）  

**主要特点**:
- System 模块是平台级监控模块，部分规范（数据隔离、逻辑删除、缓存）不适用或有合理例外
- 日志表采用物理删除策略，符合监控系统最佳实践
- 权限控制清晰，仅管理员可访问
- 健康检查并行执行，性能优化到位

**主要问题**:
1. **P1**: 未使用 BasicQueryDto，手动解析参数（影响代码质量）
2. **P1**: AlertEngineServiceImpl 使用内存存储，数据不持久化（影响生产可用性）
3. **P2**: 部分接口参数使用 Map，缺少类型安全（影响可维护性）

**改进优先级**:
1. **立即**: 定义专用 VO 类，告警引擎持久化（3.5 人日）
2. **短期**: 重构参数处理，完善 API 文档（2 人日）
3. **中期**: 集成真实监控指标，实现日志清理（5 人日）

**预计工作量**: 10.5 人日（完整改进）

**生产就绪度**: ⚠️ 部分就绪
- ✅ 核心功能（日志查询、健康检查）可用
- ⚠️ 告警引擎需持久化后才能用于生产
- ✅ 权限控制完善，安全性高
