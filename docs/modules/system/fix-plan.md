# System 模块修复计划

## 修复优先级总览

| 优先级 | 问题数量 | 预计工作量 | 说明 |
|--------|---------|-----------|------|
| P0 | 5 | 4.5 人日 | 阻塞级问题，必须立即修复 |
| P1 | 10 | 7.5 人日 | 高优先级，影响核心功能 |
| P2 | 12 | 8.5 人日 | 中优先级，影响代码质量 |
| P3 | 10 | 6.5 人日 | 低优先级，优化改进 |
| **总计** | **37** | **27 人日** | 约 5.5 周 |

## P0 阻塞级问题（必须立即修复）

### P0-1: 告警引擎使用内存存储（生产不可用）
**来源**: 架构审查 + 代码审查 + 性能分析  
**问题描述**: `AlertEngineServiceImpl` 使用 `ConcurrentHashMap` 存储告警规则和记录，数据存储在内存中，应用重启后丢失。无法支持分布式部署，无用户隔离（水平越权风险）。  
**影响**: 
- 生产环境告警功能不可用
- 多实例部署时数据不一致
- 任何用户可修改/删除其他用户的告警规则
- CVSS 3.1: 8.2 (HIGH)

**修复方案**:
1. 创建数据库表 `alert_rule` 和 `alert_record`
2. 添加 `owner_id` 字段实现数据隔离
3. 创建对应的 Entity 和 Repository
4. 迁移 Service 实现到数据库存储
5. Service 层强制过滤 `ownerId = currentUserId`

**预计工作量**: 2 人日  
**验收标准**:
- [ ] 告警规则持久化到数据库
- [ ] 支持用户隔离（owner_id 过滤）
- [ ] 应用重启后数据不丢失
- [ ] 通过集成测试验证

### P0-2: 未认证的指标端点暴露系统信息
**来源**: 安全审计  
**问题描述**: `MetricsController` 所有端点（`/api/v1/system/metrics/*`）均无认证检查，任何人可访问系统指标、CPU、内存、JVM、数据库连接池等敏感信息。  
**影响**: 
- 攻击者可获取系统架构和资源使用情况
- 为进一步攻击提供情报
- CVSS 3.1: 9.1 (CRITICAL)

**修复方案**:
```java
@GetMapping("/prometheus")
public String prometheusMetrics(HttpServletRequest request) {
    if (AuthTokenFilter.getUserId(request) == null) {
        throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
    }
    // 或限制为 admin 角色
    if (!isAdmin(request)) {
        throw new BusinessException(ErrorCode.FORBIDDEN, "无权限访问");
    }
    return metricsCollectorService.collectAllMetrics();
}
```

**预计工作量**: 0.5 人日  
**验收标准**:
- [ ] 所有 `/metrics/*` 端点添加认证检查
- [ ] 未登录用户返回 401
- [ ] 非 admin 用户返回 403
- [ ] 通过安全测试验证

### P0-3: API 密钥脱敏实现错误（破坏加密数据）
**来源**: 安全审计  
**问题描述**: `ExternalApiConfigServiceImpl.maskSensitiveFields()` 直接修改原对象，导致数据库中的加密密钥被永久替换为脱敏值 `****xxxx`，后续无法解密使用。  
**影响**: 
- 第一次查询后，加密密钥被覆盖为脱敏值
- 所有外部 API 调用失败
- CVSS 3.1: 7.5 (HIGH)

**修复方案**:
```java
private ExternalApiConfig maskSensitiveFields(ExternalApiConfig config) {
    ExternalApiConfig masked = new ExternalApiConfig();
    BeanUtils.copyProperties(config, masked);
    if (masked.getApiKeyEncrypted() != null) {
        masked.setApiKeyEncrypted(maskValue(masked.getApiKeyEncrypted()));
    }
    if (masked.getApiSecretEncrypted() != null) {
        masked.setApiSecretEncrypted(maskValue(masked.getApiSecretEncrypted()));
    }
    return masked;
}
```

**预计工作量**: 0.5 人日  
**验收标准**:
- [ ] 脱敏方法使用对象拷贝
- [ ] 原对象不被修改
- [ ] 加密密钥可正常解密
- [ ] 外部 API 调用正常

### P0-4: API 日志拦截器全量读取响应体（内存溢出风险）
**来源**: 性能分析  
**问题描述**: `ApiCallLogInterceptor` 使用 `StreamUtils.copyToByteArray()` 全量读取响应体到内存，大响应体（如文件下载、大数据集）导致内存峰值和 GC 压力。  
**影响**: 
- 大响应体（100MB+）导致内存溢出
- 每次请求都复制响应体，GC 压力大
- 2000 字符截断后仍保留完整副本

**修复方案**:
```java
// 仅读取前 2000 字符，不复制完整响应体
private String readFirst2000Chars(InputStream inputStream) throws IOException {
    byte[] buffer = new byte[2000];
    int bytesRead = inputStream.read(buffer);
    return bytesRead > 0 ? new String(buffer, 0, bytesRead, StandardCharsets.UTF_8) : null;
}

@Override
public ClientHttpResponse intercept(HttpRequest request, byte[] body,
                                    ClientHttpRequestExecution execution) throws IOException {
    ClientHttpResponse response = execution.execute(request, body);
    String responseBody = readFirst2000Chars(response.getBody());
    // 异步保存日志，不包装响应
    saveLogAsync(request, body, response.getStatusCode(), responseBody);
    return response; // 直接返回原始流
}
```

**预计工作量**: 1 人日  
**验收标准**:
- [ ] 响应体流式读取，仅截取前 2000 字符
- [ ] 不复制完整响应体到内存
- [ ] 大文件下载不触发 OOM
- [ ] 通过压力测试验证

### P0-5: DashboardDataServiceImpl 返回硬编码假数据
**来源**: 代码审查  
**问题描述**: `DashboardDataServiceImpl` 所有方法返回硬编码数据，无法反映真实系统状态。  
**影响**: 
- 仪表板显示不准确
- 无法用于生产监控
- 误导运维决策

**修复方案**:
1. 对接真实数据源（数据库、Prometheus、Elasticsearch）
2. 实现真实的指标聚合逻辑
3. 查询数据库获取真实告警数据
4. 集成 Micrometer 获取 JVM 指标

**预计工作量**: 0.5 人日  
**验收标准**:
- [ ] 告警数量从数据库查询
- [ ] 性能趋势从 Micrometer 获取
- [ ] 日志统计从数据库聚合
- [ ] 移除所有硬编码数据

## P1 高优先级问题

### P1-1: 日志批量删除未使用事务分批（长事务风险）
**来源**: 性能分析  
**问题描述**: `ApiCallLogCleanupScheduler` 循环调用 `deleteBatchByCreateTimeBefore()`，每次删除 1000 条，但未显式控制事务边界，可能产生长事务。  
**影响**: 
- 大量删除时锁表时间长
- 可能阻塞其他查询
- 删除期间影响日志写入

**修复方案**:
```java
@Scheduled(cron = "${app.system.api-log-cleanup.cron:0 0 2 * * ?}")
public void cleanup() {
    LocalDateTime cutoff = LocalDateTime.now().minusDays(RETENTION_DAYS);
    Timestamp cutoffTs = Timestamp.valueOf(cutoff);
    int totalDeleted = 0;
    int deleted;
    
    do {
        deleted = deleteInNewTransaction(cutoffTs);
        totalDeleted += deleted;
        Thread.sleep(100); // 休眠避免持续占用资源
    } while (deleted > 0);
    
    log.info("API 调用日志清理完成，删除 {} 条", totalDeleted);
}

@Transactional(propagation = Propagation.REQUIRES_NEW, timeout = 10)
public int deleteInNewTransaction(Timestamp cutoffTs) {
    return apiCallLogRepository.deleteBatchByCreateTimeBefore(cutoffTs);
}
```

**预计工作量**: 0.5 人日  
**验收标准**:
- [ ] 每批独立事务，避免长事务
- [ ] 设置 10 秒超时保护
- [ ] 批次间休眠 100ms
- [ ] 通过压力测试验证

### P1-2: MetricsCollectorService 每次调用都重新收集（无缓存）
**来源**: 性能分析  
**问题描述**: `getMetricsByName()` 调用 `collectAllMetrics()` 然后过滤，未使用缓存。每次查询单个指标都收集全部 5 个指标，CPU/内存/磁盘 IO 操作重复执行。  
**影响**: 
- 高频调用时性能下降明显
- CPU 和 IO 压力大

**修复方案**:
```java
@Service
public class MetricsCollectorServiceImpl implements MetricsCollectorService {
    
    private final LoadingCache<String, List<MetricsVO>> metricsCache = Caffeine.newBuilder()
        .expireAfterWrite(30, TimeUnit.SECONDS)
        .maximumSize(10)
        .build(key -> collectAllMetricsInternal());
    
    @Override
    public List<MetricsVO> collectAllMetrics() {
        return metricsCache.get("all");
    }
    
    @Override
    public MetricsVO getMetricsByName(String name) {
        return collectAllMetrics().stream()
            .filter(m -> m.getName().equals(name))
            .findFirst()
            .orElse(null);
    }
}
```

**预计工作量**: 0.5 人日  
**验收标准**:
- [ ] 指标缓存 30 秒
- [ ] 缓存命中时响应时间 < 5ms
- [ ] TPS 提升 10 倍以上

### P1-3: 外部 API 调用无连接池配置
**来源**: 性能分析  
**问题描述**: `ExternalApiGatewayImpl` 使用 `HttpClient.newBuilder()` 创建客户端，但未配置连接池大小和并发限制。  
**影响**: 
- 默认连接池可能不足
- 高并发时连接等待
- 可能耗尽系统资源

**修复方案**:
```java
private final HttpClient httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5))  // 缩短连接超时
        .executor(Executors.newFixedThreadPool(50))  // 限制并发连接数
        .build();
```

**预计工作量**: 0.5 人日  
**验收标准**:
- [ ] 配置最大连接数为 50
- [ ] 连接超时 5 秒
- [ ] 高并发测试通过

### P1-4: SystemServiceImpl 存在 N+1 查询问题
**来源**: 代码审查  
**问题描述**: `getApiLogStats()` 方法执行 3 次数据库查询（总计 + 按模块 + 按 API），可以合并优化。  
**影响**: 
- 高并发下数据库压力大
- 响应时间长

**修复方案**:
1. 使用 CTE 或子查询优化
2. 添加 Redis 缓存（TTL 5 分钟）
3. 使用 `@Cacheable` 注解

**预计工作量**: 0.5 人日  
**验收标准**:
- [ ] 减少数据库查询次数
- [ ] 添加缓存层
- [ ] 响应时间降低 50%

### P1-5: MetricsCollectorServiceImpl CPU 指标使用随机数
**来源**: 代码审查  
**问题描述**: CPU 指标使用 `Math.random()` 模拟，不是真实值。  
**影响**: 
- 监控数据不准确
- 无法用于生产

**修复方案**:
```java
@Override
public MetricsVO collectCpuMetrics() {
    OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
    double cpuLoad = osBean.getSystemCpuLoad() * 100;
    return MetricsVO.builder()
        .name("cpu_usage")
        .value(String.format("%.2f", cpuLoad))
        .unit("%")
        .timestamp(System.currentTimeMillis())
        .build();
}
```

**预计工作量**: 0.5 人日  
**验收标准**:
- [ ] 使用真实 CPU 指标
- [ ] 移除随机数模拟
- [ ] 验证指标准确性

### P1-6: AlertController 手动类型转换存在 NPE 风险
**来源**: 代码审查 + 安全审计  
**问题描述**: 使用 `Map<String, Object>` 接收请求体，类型转换无异常处理，恶意输入可导致 NPE 或 ClassCastException。  
**影响**: 
- 参数异常时抛出 NPE，返回 500 错误
- DoS 攻击风险
- CVSS 3.1: 6.5 (MEDIUM)

**修复方案**:
```java
// 定义专用 VO 类
@Data
public class AlertRuleUpdateVO {
    @NotNull(message = "规则ID不能为空")
    private Long ruleId;
    
    @NotBlank(message = "规则名称不能为空")
    @Size(max = 128, message = "规则名称不能超过128字符")
    private String name;
    
    @NotBlank(message = "指标名称不能为空")
    private String metricName;
    
    @NotNull(message = "阈值不能为空")
    private Double threshold;
    
    // ... 其他字段
}

// Controller 使用强类型 VO
@PostMapping("/alert/rule/update")
public RESTResult<?> updateAlertRule(@Valid @RequestBody AlertRuleUpdateVO vo) {
    alertEngineService.updateAlertRule(vo.getRuleId(), vo);
    return RESTResult.success();
}
```

**预计工作量**: 1 人日  
**验收标准**:
- [ ] 创建专用 VO 类
- [ ] 添加 Bean Validation 注解
- [ ] 移除手动类型转换
- [ ] 通过异常测试验证

### P1-7: 外部 API 调用未验证响应内容
**来源**: 安全审计  
**问题描述**: 直接返回外部 API 响应体，未验证内容类型和大小，可能导致 SSRF 或内存溢出。  
**影响**: 
- 外部 API 返回恶意脚本，传递给前端导致 XSS
- 返回超大响应体导致内存溢出
- SSRF 攻击风险
- CVSS 3.1: 6.8 (MEDIUM)

**修复方案**:
1. 限制响应体大小（如 10MB）
2. 验证 Content-Type
3. 白名单验证 `baseUrl` 域名
4. 对响应内容进行 HTML 转义

**预计工作量**: 1 人日  
**验收标准**:
- [ ] 响应体大小限制 10MB
- [ ] Content-Type 白名单验证
- [ ] baseUrl 域名白名单
- [ ] 通过安全测试验证

### P1-8: 日志记录敏感信息
**来源**: 安全审计  
**问题描述**: API 调用日志记录完整请求参数和响应体，可能包含密码、token 等敏感信息。  
**影响**: 
- 管理员查看日志时可获取用户密码、API 密钥
- CVSS 3.1: 6.5 (MEDIUM)

**修复方案**:
```java
private static final List<String> SENSITIVE_KEYS = List.of(
    "password", "token", "apiKey", "api_key", "secret", "access_token"
);

private String sanitizeParams(String params) {
    if (params == null) return null;
    String sanitized = params;
    for (String key : SENSITIVE_KEYS) {
        sanitized = sanitized.replaceAll(
            "\"" + key + "\"\\s*:\\s*\"[^\"]*\"",
            "\"" + key + "\":\"***\""
        );
    }
    return sanitized;
}
```

**预计工作量**: 1 人日  
**验收标准**:
- [ ] 敏感字段自动脱敏
- [ ] 日志中无明文密码/token
- [ ] 通过安全审计验证

### P1-9: SystemController 未使用 BasicQueryDto
**来源**: 模式合规性检查  
**问题描述**: 手动解析 `Map<String, Object>` 参数，未利用 `BasicQueryDto` 的分页校验和排序功能。  
**影响**: 
- 代码冗余，维护成本高
- 缺少参数校验

**修复方案**:
```java
@Data
public class ApiLogSearchVO extends BasicQueryDto {
    private String module;
    private String apiName;
    private Integer status;
    private String startTime;
    private String endTime;
}

@PostMapping("/api-log/list")
public RESTResult<?> apiLogList(@Valid @RequestBody ApiLogSearchVO vo, 
                                 HttpServletRequest request) {
    vo.validateParams(); // 自动校验分页参数
    return RESTResult.success(systemService.searchApiLogs(vo));
}
```

**预计工作量**: 1 人日  
**验收标准**:
- [ ] 创建 SearchVO 类继承 BasicQueryDto
- [ ] 移除手动参数解析
- [ ] 利用自动分页校验

### P1-10: 日志记录逻辑重复
**来源**: 架构审查  
**问题描述**: `ExternalApiGateway` 使用 Java HttpClient，未经过 `ApiCallLogInterceptor`，日志记录逻辑在两处维护。  
**影响**: 
- 代码重复，容易不一致
- 维护成本高

**修复方案**:
1. 将 `ExternalApiGateway` 改为使用 RestTemplate
2. 或将日志记录逻辑提取为 `ApiCallLogService`

**预计工作量**: 1 人日  
**验收标准**:
- [ ] 统一日志记录逻辑
- [ ] 移除重复代码
- [ ] 通过集成测试验证

## P2 中优先级问题

### P2-1: SystemController 重复的权限检查代码
**来源**: 代码审查  
**问题描述**: 每个方法都重复相同的权限检查逻辑（10+ 处），违反 DRY 原则。  
**影响**: 
- 代码冗余，维护成本高

**修复方案**:
```java
// 使用 Spring Security 注解
@PreAuthorize("hasRole('ADMIN')")
@PostMapping("/api-log/list")
public RESTResult<?> apiLogList(...) {
    // 无需手动检查权限
}

// 或创建自定义注解 + AOP 切面
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireAdmin {}

@Aspect
@Component
public class AdminCheckAspect {
    @Before("@annotation(RequireAdmin)")
    public void checkAdmin(JoinPoint joinPoint) {
        // 统一权限检查逻辑
    }
}
```

**预计工作量**: 0.5 人日  
**验收标准**:
- [ ] 移除重复权限检查代码
- [ ] 使用注解统一处理
- [ ] 通过权限测试验证

### P2-2: MonitoringController 方法过长（430 行）
**来源**: 代码审查  
**问题描述**: 单个 Controller 430 行，包含 30+ 个方法，职责混杂。  
**影响**: 
- 代码可维护性差
- 违反单一职责原则

**修复方案**:
拆分为多个 Controller：
- `MetricsController` - 指标相关（已存在）
- `LogController` - 日志相关
- `HealthController` - 健康检查
- `DashboardController` - 仪表板聚合

**预计工作量**: 1 人日  
**验收标准**:
- [ ] 每个 Controller 不超过 200 行
- [ ] 职责清晰
- [ ] 通过功能测试验证

### P2-3: 告警检查定时任务无并发控制
**来源**: 性能分析  
**问题描述**: `@Scheduled(fixedDelay = 60000)` 串行执行所有规则检查，规则数量增多时检查周期延长。  
**影响**: 
- 单个规则异常可能阻塞后续规则
- 无法利用多核 CPU

**修复方案**:
```java
@Configuration
public class AlertExecutorConfig {
    @Bean("alertCheckExecutor")
    public Executor alertCheckExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("alert-check-");
        executor.initialize();
        return executor;
    }
}

@Service
public class AlertEngineServiceImpl implements AlertEngineService {
    @Resource
    private Executor alertCheckExecutor;
    
    @Scheduled(fixedDelay = 60000)
    public void executeAlertChecks() {
        List<CompletableFuture<Void>> futures = rules.values().stream()
            .filter(AlertRuleVO::getEnabled)
            .map(rule -> CompletableFuture.runAsync(
                () -> executeRuleCheck(rule), 
                alertCheckExecutor
            ))
            .toList();
        
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .orTimeout(50, TimeUnit.SECONDS)
            .join();
    }
}
```

**预计工作量**: 1 人日  
**验收标准**:
- [ ] 告警检查并行执行
- [ ] 设置超时保护
- [ ] 检查耗时降低 80%

### P2-4: SysApiCallLog 缺少索引优化
**来源**: 代码审查  
**问题描述**: Entity 未定义索引，查询可能全表扫描。  
**影响**: 
- 查询性能差

**修复方案**:
```java
@Table(name = "sys_api_call_log", indexes = {
    @Index(name = "idx_module_time", columnList = "module, create_time"),
    @Index(name = "idx_api_name", columnList = "api_name"),
    @Index(name = "idx_user_time", columnList = "user_id, create_time")
})
public class SysApiCallLog { ... }
```

**预计工作量**: 0.5 人日  
**验收标准**:
- [ ] 添加索引定义
- [ ] 查询性能提升 10 倍以上

### P2-5: AlertEngineServiceImpl 定时任务无分布式锁
**来源**: 代码审查  
**问题描述**: 多实例部署时，每个实例都会执行定时任务，导致重复检查。  
**影响**: 
- 多实例环境下重复执行
- 资源浪费

**修复方案**:
```java
// 使用 ShedLock
@Scheduled(fixedDelay = 60000)
@SchedulerLock(name = "alertChecks", lockAtMostFor = "50s", lockAtLeastFor = "10s")
public void executeAlertChecks() {
    // 告警检查逻辑
}
```

**预计工作量**: 0.5 人日  
**验收标准**:
- [ ] 添加分布式锁
- [ ] 多实例环境仅一个实例执行
- [ ] 通过集成测试验证

### P2-6: 健康检查超时设置不合理
**来源**: 代码审查  
**问题描述**: 所有健康检查共享 5 秒超时，单个慢服务会拖累整体响应。  
**影响**: 
- 健康检查可能超时

**修复方案**:
为不同服务设置不同超时：
- 数据库/Redis: 2 秒
- Elasticsearch/Milvus: 3 秒
- 外部 API: 5 秒

**预计工作量**: 0.5 人日  
**验收标准**:
- [ ] 按服务配置超时时间
- [ ] 添加熔断机制
- [ ] 通过压力测试验证

### P2-7: 参数脱敏使用正则表达式（性能问题）
**来源**: 性能分析  
**问题描述**: 每次日志记录都编译正则表达式，高频调用时 CPU 占用高。  
**影响**: 
- CPU 占用高

**修复方案**:
```java
// 预编译正则表达式
private static final Map<String, Pattern> SENSITIVE_PATTERNS = SENSITIVE_KEYS.stream()
    .collect(Collectors.toMap(
        key -> key,
        key -> Pattern.compile("(\"" + key + "\"\\s*:\\s*)\"[^\"]*\"", Pattern.CASE_INSENSITIVE)
    ));

private String sanitizeJson(String json) {
    StringBuilder sb = new StringBuilder(json);
    for (Map.Entry<String, Pattern> entry : SENSITIVE_PATTERNS.entrySet()) {
        sb = new StringBuilder(entry.getValue().matcher(sb).replaceAll("$1\"***\""));
    }
    return truncate(sb.toString(), MAX_PARAMS_LEN);
}
```

**预计工作量**: 0.5 人日  
**验收标准**:
- [ ] 预编译正则表达式
- [ ] CPU 占用降低 50%

### P2-8: 缺少请求频率限制
**来源**: 安全审计  
**问题描述**: 所有端点均无频率限制，攻击者可暴力枚举或 DoS 攻击。  
**影响**: 
- DoS 攻击风险
- CVSS 3.1: 5.3 (MEDIUM)

**修复方案**:
```java
@RateLimiter(name = "systemApi")
@PostMapping("/api-log/list")
public RESTResult<?> apiLogList(...) {
    // 限流保护
}
```

**预计工作量**: 1 人日  
**验收标准**:
- [ ] 添加全局限流
- [ ] 配置每分钟 100 次请求限制
- [ ] 通过压力测试验证

### P2-9: 健康检查端点泄露架构信息
**来源**: 安全审计  
**问题描述**: 健康检查端点返回详细的基础设施信息，帮助攻击者了解系统架构。  
**影响**: 
- 信息泄露风险
- CVSS 3.1: 5.3 (MEDIUM)

**修复方案**:
1. 仅返回整体状态（UP/DOWN），不返回组件细节
2. 详细信息仅对 admin 角色开放
3. 移除延迟信息

**预计工作量**: 0.5 人日  
**验收标准**:
- [ ] 普通用户仅看到整体状态
- [ ] admin 用户可查看详细信息
- [ ] 通过安全测试验证

### P2-10: 错误消息泄露内部路径
**来源**: 安全审计  
**问题描述**: 异常消息包含供应商编码等内部信息。  
**影响**: 
- 信息泄露风险
- CVSS 3.1: 4.3 (MEDIUM)

**修复方案**:
使用通用错误消息，详细信息仅记录到日志。

**预计工作量**: 0.5 人日  
**验收标准**:
- [ ] 错误消息不包含内部信息
- [ ] 详细信息记录到日志
- [ ] 通过安全测试验证

### P2-11: 定时任务无异常隔离
**来源**: 安全审计  
**问题描述**: 定时任务中单个规则异常可能影响后续规则执行。  
**影响**: 
- 单个规则失败影响其他规则
- CVSS 3.1: 4.0 (MEDIUM)

**修复方案**:
```java
@Scheduled(fixedDelay = 60000)
public void executeAlertChecks() {
    for (AlertRuleVO rule : rules.values()) {
        try {
            executeRuleCheck(rule);
        } catch (Exception e) {
            log.error("告警规则检查失败: ruleId={}, error={}", rule.getId(), e.getMessage());
        }
    }
}
```

**预计工作量**: 0.5 人日  
**验收标准**:
- [ ] 异常隔离
- [ ] 单个规则失败不影响其他规则
- [ ] 通过异常测试验证

### P2-12: 缺少 CSRF 保护
**来源**: 安全审计  
**问题描述**: POST 端点未启用 CSRF 保护。  
**影响**: 
- CSRF 攻击风险
- CVSS 3.1: 5.4 (MEDIUM)

**修复方案**:
启用 Spring Security CSRF 保护。

**预计工作量**: 1 人日  
**验收标准**:
- [ ] 启用 CSRF 保护
- [ ] 通过安全测试验证

## P3 低优先级问题

### P3-1: 缺少输入长度限制
**来源**: 安全审计  
**问题描述**: VO 类缺少 `@Size` 注解，攻击者可发送超长字符串。  
**影响**: 
- 数据库错误或内存溢出风险
- CVSS 3.1: 3.1 (LOW)

**修复方案**:
在所有 VO 字段添加 `@Size(max=xxx)` 注解。

**预计工作量**: 1 人日  
**验收标准**:
- [ ] 所有 VO 字段添加长度限制
- [ ] 通过参数测试验证

### P3-2: 日志级别不当
**来源**: 代码审查 + 安全审计  
**问题描述**: 使用 `log.info()` 记录所有告警规则操作，生产环境可能产生大量日志。  
**影响**: 
- 日志量过大
- CVSS 3.1: 2.0 (LOW)

**修复方案**:
改为 `log.debug()`，仅在开发环境启用。

**预计工作量**: 0.5 人日  
**验收标准**:
- [ ] 调整日志级别
- [ ] 生产环境日志量减少

### P3-3: 魔法数字未定义常量
**来源**: 代码审查  
**问题描述**: 硬编码 `2000` 字符限制、`5` 秒超时等魔法数字。  
**影响**: 
- 代码可读性差

**修复方案**:
```java
private static final int MAX_RESPONSE_BODY_LENGTH = 2000;
private static final int HEALTH_CHECK_TIMEOUT_SECONDS = 5;
private static final int BATCH_DELETE_SIZE = 1000;
```

**预计工作量**: 0.5 人日  
**验收标准**:
- [ ] 所有魔法数字定义为常量
- [ ] 代码可读性提升

### P3-4: ExternalApiConfig 敏感字段命名不清晰
**来源**: 代码审查  
**问题描述**: 字段名暗示已加密，但未强制加密逻辑。  
**影响**: 
- 可能误用

**修复方案**:
1. 创建 `EncryptionService` 统一处理加密
2. 在 `@PrePersist` 中自动加密
3. 提供 `getDecryptedApiKey()` 方法

**预计工作量**: 1 人日  
**验收标准**:
- [ ] 自动加密/解密
- [ ] 统一加密服务

### P3-5: 硬编码超时时间
**来源**: 安全审计  
**问题描述**: 连接超时和请求超时硬编码，无法根据不同 API 调整。  
**影响**: 
- 灵活性差
- CVSS 3.1: 2.0 (LOW)

**修复方案**:
从配置文件读取超时时间，或在 `ExternalApiConfig` 表中存储。

**预计工作量**: 0.5 人日  
**验收标准**:
- [ ] 超时时间可配置
- [ ] 支持按 API 配置

### P3-6: Prometheus 指标格式简化
**来源**: 架构审查  
**问题描述**: 手动拼接字符串，不支持 labels、histogram 等高级特性。  
**影响**: 
- 功能受限

**修复方案**:
使用 Micrometer 的 `PrometheusMeterRegistry`。

**预计工作量**: 1 人日  
**验收标准**:
- [ ] 使用 Micrometer 标准 API
- [ ] 支持高级特性

### P3-7: 缺少 API 限流实现
**来源**: 架构审查  
**问题描述**: `ExternalApiConfig` 定义了 `rateLimitPerMin`，但未实现限流逻辑。  
**影响**: 
- 无法防止外部 API 配额耗尽

**修复方案**:
集成 Resilience4j RateLimiter。

**预计工作量**: 1 人日  
**验收标准**:
- [ ] 实现限流逻辑
- [ ] 配额耗尽时自动切换备用 provider

### P3-8: 分类法功能孤立
**来源**: 架构审查  
**问题描述**: `TaxonomyService` 与其他系统功能无关联。  
**影响**: 
- 模块职责不清晰

**修复方案**:
移至独立模块或 `common` 模块。

**预计工作量**: 0.5 人日  
**验收标准**:
- [ ] 移动到合适位置
- [ ] 模块职责清晰

### P3-9: SystemPerformanceController 全部为桩实现
**来源**: 性能分析  
**问题描述**: 所有接口返回空数据或固定值。  
**影响**: 
- 前端性能监控页面无法展示真实数据

**修复方案**:
对接 Micrometer、Actuator、数据库慢查询日志。

**预计工作量**: 3 人日  
**验收标准**:
- [ ] 实现真实性能监控
- [ ] 对接慢查询日志
- [ ] 前端展示真实数据

### P3-10: 缺少集成测试
**来源**: 架构审查 + 代码审查  
**问题描述**: 仅有 Controller 单元测试，缺少 Service 层集成测试。  
**影响**: 
- 无法验证核心功能

**修复方案**:
使用 TestContainers 编写集成测试。

**预计工作量**: 1.5 人日  
**验收标准**:
- [ ] 健康检查测试
- [ ] API 日志记录测试
- [ ] 外部 API 网关测试
- [ ] 测试覆盖率 > 80%

## 修复路线图

### 第一阶段：P0 问题修复（4.5 人日）

**目标**: 修复阻塞级问题，确保生产可用性

1. **P0-2: 未认证的指标端点** (0.5 人日)
   - 为 MetricsController 添加认证检查
   - 限制为 admin 角色访问

2. **P0-3: API 密钥脱敏实现错误** (0.5 人日)
   - 修复脱敏方法，使用对象拷贝
   - 验证加密解密流程

3. **P0-5: DashboardDataServiceImpl 返回硬编码假数据** (0.5 人日)
   - 对接真实数据源
   - 移除硬编码数据

4. **P0-1: 告警引擎使用内存存储** (2 人日)
   - 创建数据库表
   - 实现持久化逻辑
   - 添加用户隔离

5. **P0-4: API 日志拦截器全量读取响应体** (1 人日)
   - 实现流式读取
   - 限制内存占用

**里程碑**: 第 1 周结束，生产阻塞问题全部解决

### 第二阶段：P1 问题修复（7.5 人日）

**目标**: 修复高优先级问题，提升性能和安全性

1. **P1-2: MetricsCollectorService 无缓存** (0.5 人日)
2. **P1-3: 外部 API 调用无连接池配置** (0.5 人日)
3. **P1-4: SystemServiceImpl N+1 查询** (0.5 人日)
4. **P1-5: CPU 指标使用随机数** (0.5 人日)
5. **P1-1: 日志批量删除未使用事务分批** (0.5 人日)
6. **P1-6: AlertController 手动类型转换** (1 人日)
7. **P1-7: 外部 API 调用未验证响应** (1 人日)
8. **P1-8: 日志记录敏感信息** (1 人日)
9. **P1-9: SystemController 未使用 BasicQueryDto** (1 人日)
10. **P1-10: 日志记录逻辑重复** (1 人日)

**里程碑**: 第 3 周结束，核心功能优化完成

### 第三阶段：P2 问题修复（8.5 人日）

**目标**: 提升代码质量和可维护性

1. **P2-1: 重复的权限检查代码** (0.5 人日)
2. **P2-4: SysApiCallLog 缺少索引** (0.5 人日)
3. **P2-5: 定时任务无分布式锁** (0.5 人日)
4. **P2-6: 健康检查超时设置不合理** (0.5 人日)
5. **P2-7: 参数脱敏使用正则表达式** (0.5 人日)
6. **P2-9: 健康检查端点泄露架构信息** (0.5 人日)
7. **P2-10: 错误消息泄露内部路径** (0.5 人日)
8. **P2-11: 定时任务无异常隔离** (0.5 人日)
9. **P2-2: MonitoringController 方法过长** (1 人日)
10. **P2-3: 告警检查定时任务无并发控制** (1 人日)
11. **P2-8: 缺少请求频率限制** (1 人日)
12. **P2-12: 缺少 CSRF 保护** (1 人日)

**里程碑**: 第 5 周结束，代码质量显著提升

### 第四阶段：P3 问题修复（6.5 人日）

**目标**: 完善功能和优化细节

1. **P3-2: 日志级别不当** (0.5 人日)
2. **P3-3: 魔法数字未定义常量** (0.5 人日)
3. **P3-5: 硬编码超时时间** (0.5 人日)
4. **P3-8: 分类法功能孤立** (0.5 人日)
5. **P3-1: 缺少输入长度限制** (1 人日)
6. **P3-4: ExternalApiConfig 敏感字段命名不清晰** (1 人日)
7. **P3-6: Prometheus 指标格式简化** (1 人日)
8. **P3-7: 缺少 API 限流实现** (1 人日)
9. **P3-10: 缺少集成测试** (1.5 人日)
10. **P3-9: SystemPerformanceController 全部为桩实现** (3 人日)

**里程碑**: 第 7 周结束，所有问题修复完成

## 总结

**总工作量**: 27 人日（约 5.5 周）  

**关键里程碑**:
- P0 修复完成：第 1 周
- P1 修复完成：第 3 周
- P2 修复完成：第 5 周
- 全部修复完成：第 7 周

**生产就绪评估**:
- **当前状态**: ❌ 不可上线（存在 5 个 P0 问题）
  - 告警引擎无持久化
  - 指标端点无认证
  - API 密钥脱敏错误
  - 日志拦截器内存溢出风险
  - 仪表板返回假数据
  
- **P0 修复后**: ⚠️ 可上线但需监控（存在 10 个 P1 问题）
  - 核心功能可用
  - 存在性能和安全隐患
  - 需要密切监控
  
- **P0+P1 修复后**: ✅ 生产就绪（推荐状态）
  - 核心功能完善
  - 性能和安全性达标
  - 可稳定运行
  
- **全部修复后**: ✅ 生产优化（最佳状态）
  - 代码质量优秀
  - 功能完整
  - 可维护性强

**优先级建议**:
1. **立即执行**: P0 问题（1 周内完成）
2. **短期规划**: P1 问题（3 周内完成）
3. **中期规划**: P2 问题（5 周内完成）
4. **长期优化**: P3 问题（7 周内完成）

**关键风险**:
1. **告警引擎重构**: 涉及数据迁移，需要充分测试
2. **日志拦截器修改**: 影响所有外部 API 调用，需要灰度发布
3. **权限检查重构**: 涉及所有 Controller，需要回归测试

**资源需求**:
- 后端开发: 1 人全职（7 周）
- 测试工程师: 0.5 人（配合测试）
- DBA: 0.2 人（数据库优化）

**验收标准**:
- [ ] 所有 P0 问题修复并通过测试
- [ ] 所有 P1 问题修复并通过测试
- [ ] 单元测试覆盖率 > 80%
- [ ] 集成测试覆盖核心功能
- [ ] 安全审计通过（无 CRITICAL/HIGH 漏洞）
- [ ] 性能测试通过（响应时间 < 200ms，TPS > 1000）
- [ ] 代码审查通过（无 P0/P1 问题）

**后续改进方向**:
1. **模块拆分**: 将 system 模块拆分为 monitoring、logging、gateway、alerting 等独立模块
2. **分布式追踪**: 集成 OpenTelemetry，实现跨服务追踪
3. **智能告警**: 支持复杂规则、趋势分析、异常检测
4. **可观测性平台**: 统一日志、指标、追踪查询界面

---

**报告生成日期**: 2026-05-08  
**报告版本**: v1.0  
**下次审查**: P0 修复后复审（预计 2026-05-15）

