# Attribution 模块修复计划

**生成日期**: 2026-05-09  
**模块**: attribution (归因分析)  
**规划者**: Claude Code  
**基于报告**: architecture-review, code-review, security-audit, performance-analysis, pattern-compliance

---

## 执行摘要

### 总体状况

**当前评分汇总**:
- 架构评审: B (82.2/100)
- 代码评审: C+ (75/100)
- 安全审计: 62/100 (中等偏低)
- 性能分析: 58/100 (需优化)
- 模式合规: C+ (72/100)

**生产就绪度**: 🔴 **不建议上线** - 必须修复 CRITICAL 和 HIGH 问题

### 问题统计

| 优先级 | 数量 | 说明 |
|--------|------|------|
| **P0 阻塞级** | 8 | 数据隔离、缓存、并发控制、超时控制 |
| **P1 高优先级** | 9 | 安全风险、代码重复、测试覆盖 |
| **P2 中优先级** | 11 | 性能优化、代码质量 |
| **P3 低优先级** | 8 | 功能完善、用户体验 |
| **总计** | **36** | - |

### 总工作量估算

| 阶段 | 工作量 | 时间 |
|------|--------|------|
| P0 修复 | 10.5 人日 | 2 周 |
| P1 修复 | 8.5 人日 | 1.5 周 |
| P2 修复 | 9.5 人日 | 2 周 |
| P3 修复 | 7.5 人日 | 1.5 周 |
| **总计** | **36 人日** | **7 周** |

### 预期收益

**修复后评分预测**:
- 架构评审: A- (88/100) ↑ 5.8
- 代码评审: B+ (85/100) ↑ 10
- 安全审计: 85/100 ↑ 23
- 性能分析: 85/100 ↑ 27
- 模式合规: B+ (85/100) ↑ 13

**关键指标改善**:
- 响应时间: 200ms → 5ms (97% 提升)
- 缓存命中率: 0% → 85%
- 数据库负载: 减少 80%
- 安全漏洞: 2 CRITICAL → 0
- 测试覆盖率: <10% → 80%+

---

## P0 阻塞级问题（必须立即修复）

### P0-1: 数据所有权校验缺失 🔴 CRITICAL

**来源**: security-audit (C1), pattern-compliance (P0-3), code-review (P1-1, P1-2)

**问题描述**:
- `getBySessionId()` 未校验 session 是否属于当前用户
- `deleteBySessionId()` 未校验 session 是否属于当前用户
- `getSummary()` 未校验 session 是否属于当前用户
- 用户可通过修改 sessionId 参数越权访问/删除其他用户的归因数据

**影响**:
- **CVSS 评分**: 9.1 (CRITICAL)
- **CWE**: CWE-639 (Authorization Bypass Through User-Controlled Key)
- 数据泄露: GMV、销售额、商品名称、话术内容
- 数据丢失: 恶意删除其他用户的归因数据
- 违反 GDPR、等保 2.0 数据隔离要求

**修复方案**:
```java
// 1. Service 层添加所有权校验
@Override
public List<Map<String, Object>> getBySessionId(Long sessionId, Long userId) {
    // 校验 session 归属
    LiveSession session = sessionRepository.findById(sessionId)
        .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "场次不存在"));
    
    if (!session.getOwnerId().equals(userId)) {
        throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问该场次");
    }
    
    // 查询归因数据
    return attributionRepository.findBySessionIdAndDeleted(sessionId, 0)
        .stream().map(this::toMap).collect(Collectors.toList());
}

// 2. Controller 传递 userId
@PostMapping("/session")
public RESTResult<List<Map<String, Object>>> getBySession(HttpServletRequest request,
        @RequestBody(required = false) java.util.Map<String, Long> body) {
    Long userId = AuthTokenFilter.getUserId(request);
    if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
    Long sessionId = body != null ? body.get("sessionId") : null;
    if (sessionId == null) return RESTResult.error(ErrorCode.VALIDATION_FAIL, "缺少 sessionId");
    List<Map<String, Object>> data = attributionService.getBySessionId(sessionId, userId);
    // ...
}
```

**影响范围**:
- `AttributionServiceImpl.java`: getBySessionId, deleteBySessionId, getSummary
- `AttributionController.java`: getBySession, deleteBySession, getSummary

**工作量**: 1 人日  
**优先级**: P0 - 必须立即修复

---

### P0-2: 敏感数据未脱敏直接返回 🔴 CRITICAL

**来源**: security-audit (C2)

**问题描述**:
- 归因数据包含敏感商业信息（GMV、销售额、商品名称、话术内容）
- 直接返回给前端，未做任何脱敏处理
- 无访问审计日志，无法追踪数据访问

**影响**:
- **CVSS 评分**: 8.2 (CRITICAL)
- **CWE**: CWE-200 (Exposure of Sensitive Information)
- 商业数据泄露: 通过 P0-1 漏洞泄露后对竞争对手暴露核心数据
- 违反《个人信息保护法》、GDPR 数据最小化原则

**修复方案**:
```java
// 1. 添加访问审计日志
@Override
public List<Map<String, Object>> getBySessionId(Long sessionId, Long userId) {
    // 记录访问审计
    auditLogService.log("ATTRIBUTION_ACCESS", sessionId, userId);
    // ...
}

// 2. 按需返回字段（摘要不返回完整内容）
private Map<String, Object> toMapSummary(Attribution a) {
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("id", a.getId());
    m.put("attributionType", a.getAttributionType());
    m.put("contributedGmv", a.getContributedGmv());
    m.put("effectScore", a.getEffectScore());
    // 摘要不返回完整内容
    if (a.getScriptContent() != null && a.getScriptContent().length() > 100) {
        m.put("scriptContentPreview", a.getScriptContent().substring(0, 100) + "...");
    }
    return m;
}
```

**工作量**: 2 人日  
**优先级**: P0 - 必须立即修复

---

### P0-3: 未使用 Specification 动态查询 🔴

**来源**: pattern-compliance (P0-1), architecture-review (P1)

**问题描述**:
- Service 层直接调用 Repository 固定方法
- 未使用 JPA Specification 构建动态查询
- 违反 ADR-003 架构决策

**影响**:
- 无法灵活组合查询条件
- 数据隔离未在 Specification 中强制过滤
- 架构不合规

**修复方案**:
```java
@Override
public List<Map<String, Object>> getBySessionId(Long sessionId, Long userId) {
    // 使用 Specification 构建动态查询
    Specification<Attribution> spec = (root, query, cb) -> {
        List<Predicate> predicates = new ArrayList<>();
        
        // 1. 逻辑删除过滤（必须）
        predicates.add(cb.equal(root.get("deleted"), 0));
        
        // 2. 场次过滤
        predicates.add(cb.equal(root.get("sessionId"), sessionId));
        
        // 3. 数据隔离过滤（必须）
        LiveSession session = sessionRepository.findById(sessionId)
            .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "场次不存在"));
        if (!session.getOwnerId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问该场次");
        }
        
        return cb.and(predicates.toArray(new Predicate[0]));
    };
    
    List<Attribution> attrs = attributionRepository.findAll(spec);
    return attrs.stream().map(this::toMap).collect(Collectors.toList());
}
```

**工作量**: 2 人日  
**优先级**: P0 - 必须立即修复

---

### P0-4: 无缓存机制 🔴

**来源**: pattern-compliance (P0-2), performance-analysis (P0-1, P0-5), architecture-review (P2)

**问题描述**:
- 归因汇总查询无缓存（每次都查数据库）
- 归因列表查询无缓存
- 违反 ADR-004 两级缓存策略

**影响**:
- 响应时间: 200-300ms
- 数据库负载高
- 缓存命中率: 0%

**修复方案**:
```java
// 1. 配置缓存
@Configuration
public class AttributionCacheConfig {
    @Bean
    public CacheManager attributionCacheManager() {
        // L1: Caffeine (1分钟)
        CaffeineCache l1 = new CaffeineCache("attribution:summary",
            Caffeine.newBuilder()
                .expireAfterWrite(1, TimeUnit.MINUTES)
                .maximumSize(1000)
                .build());
        
        // L2: Redis (5分钟)
        RedisCacheConfiguration l2Config = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(5));
        
        return new CompositeCacheManager(l1, l2);
    }
}

// 2. 使用缓存
@Cacheable(value = "attribution:summary", key = "#sessionId", unless = "#result.status == 'processing'")
public Map<String, Object> getSummary(Long sessionId) {
    // 查询数据库...
}

@CacheEvict(value = "attribution:summary", key = "#sessionId")
public void deleteBySessionId(Long sessionId) {
    // 删除时清除缓存
}
```

**预期收益**:
- 响应时间: 200ms → 5ms (97% 提升)
- 缓存命中率: 85%+
- 数据库负载减少 80%

**工作量**: 1.5 人日  
**优先级**: P0 - 必须立即修复

---

### P0-5: 无并发控制 - 重复触发归因分析 🔴

**来源**: security-audit (H2), performance-analysis (P0-2), architecture-review (P2)

**问题描述**:
- 同一场次可能被重复触发归因分析
- 无分布式锁保护
- 浪费 AI 调用额度

**影响**:
- **CVSS 评分**: 7.5 (HIGH)
- **CWE**: CWE-362 (Concurrent Execution)
- 产生重复记录
- 浪费 AI 额度（每次 2000-5000 tokens）

**修复方案**:
```java
@Override
@Transactional(rollbackFor = Exception.class)
public long triggerAttribution(AttributionTriggerVO vo, Long ownerId) {
    // 1. 检查是否已有计算中的任务
    List<Attribution> processing = attributionRepository
        .findBySessionIdAndAttributionTypeAndDeleted(vo.getSessionId(), "overall", 0)
        .stream()
        .filter(a -> a.getStatus() == 0)
        .toList();
    
    if (!processing.isEmpty()) {
        throw new BusinessException(ErrorCode.OPERATION_FAIL, "该场次正在计算中，请稍后");
    }
    
    // 2. 创建新任务
    Attribution overall = new Attribution();
    // ...
}
```

**工作量**: 1 人日  
**优先级**: P0 - 必须立即修复

---

### P0-6: 异步任务无超时控制 🔴

**来源**: security-audit (H4), performance-analysis (P0-3)

**问题描述**:
- 异步任务无超时限制
- 长时间运行的 AI 调用可能阻塞线程
- 使用默认线程池，未配置

**影响**:
- **CVSS 评分**: 7.5 (HIGH)
- **CWE**: CWE-400 (Uncontrolled Resource Consumption)
- 线程池资源耗尽

**修复方案**:
```java
// 1. 配置专用线程池
@Bean("attributionExecutor")
public Executor attributionExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(5);
    executor.setMaxPoolSize(20);
    executor.setQueueCapacity(50);
    executor.setThreadNamePrefix("attribution-");
    executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
    executor.initialize();
    return executor;
}

// 2. 添加超时控制
@Async("attributionExecutor")
public void asyncAttribution(Long sessionId, Long ownerId) {
    try {
        CompletableFuture.runAsync(() -> {
            doAttribution(sessionId, ownerId);
        }).get(300, TimeUnit.SECONDS);
    } catch (TimeoutException e) {
        log.error("归因分析超时: sessionId={}", sessionId);
        // 更新状态为失败
    }
}
```

**工作量**: 1.5 人日  
**优先级**: P0 - 必须立即修复

---

### P0-7: 批量保存优化 🔴

**来源**: performance-analysis (P0-4)

**问题描述**:
- 循环中单条保存归因记录
- 10 个商品 + 20 个话术 = 30 次数据库写入
- 每次写入 10-20ms → 总计 300-600ms

**修复方案**:
```java
// 批量保存
List<Attribution> batchAttrs = new ArrayList<>();

// 收集商品归因
for (LiveProduct product : products) {
    Attribution attr = new Attribution();
    // 设置字段...
    batchAttrs.add(attr);
}

// 收集话术归因
for (LiveScript script : executedScripts) {
    Attribution attr = new Attribution();
    // 设置字段...
    batchAttrs.add(attr);
}

// 一次性批量保存
attributionRepository.saveAll(batchAttrs);
```

**预期收益**:
- 写入时间: 500ms → 50ms (90% 提升)
- 事务次数: 30 次 → 1 次

**工作量**: 0.5 人日  
**优先级**: P0 - 必须立即修复

---

### P0-8: API 限流保护缺失 🔴

**来源**: security-audit (H1)

**问题描述**:
- 无全局限流保护
- 归因触发接口无调用频率限制
- 攻击者可暴力调用 AI API 导致额度耗尽

**影响**:
- **CVSS 评分**: 8.6 (HIGH)
- **CWE**: CWE-770 (Allocation of Resources Without Limits)
- 服务器资源耗尽导致 DoS
- 成本失控

**修复方案**:
```java
// 1. 归因触发接口限流
@PostMapping("/trigger")
@RateLimiter(name = "attributionTrigger", fallbackMethod = "triggerFallback")
public RESTResult<Long> trigger(...) { }

// 2. 配置限流策略
resilience4j:
  ratelimiter:
    instances:
      attributionTrigger:
        limitForPeriod: 10        # 每个时间窗口最多 10 次
        limitRefreshPeriod: 1m    # 时间窗口 1 分钟
        timeoutDuration: 0        # 不等待，直接拒绝
```

**工作量**: 1.5 人日  
**优先级**: P0 - 必须立即修复

---

## P1 高优先级问题（应尽快修复）

### P1-1: AI 评分提取依赖正则 ⚠️

**来源**: security-audit (H3), performance-analysis (P1-2), architecture-review (P2)

**问题描述**:
- AI 评分提取使用正则匹配，脆弱且不可靠
- AI 输出格式变化会导致提取失败
- 默认返回 50 分（不准确）

**影响**:
- **CVSS 评分**: 7.2 (HIGH)
- **CWE**: CWE-20 (Improper Input Validation)
- 提取失败率高
- 可能被恶意 AI 响应注入

**修复方案**:
```java
// 使用结构化输出（JSON）
String prompt = """
请以 JSON 格式输出分析结果：
{
  "score": 85,
  "analysis": "本场直播表现优秀...",
  "suggestions": ["建议1", "建议2"]
}
""";

// 解析 JSON
ObjectMapper mapper = new ObjectMapper();
JsonNode result = mapper.readTree(aiResponse);
int score = result.get("score").asInt();
```

**预期收益**:
- 提取成功率: 70% → 99%

**工作量**: 1.5 人日  
**优先级**: P1 - 应尽快修复

---

### P1-2: Map 参数未校验 ⚠️

**来源**: security-audit (H5), pattern-compliance (P1-1)

**问题描述**:
- 直接从 Map 中取值，未校验类型和范围
- 类型转换可能抛出 ClassCastException
- 缺少 null 检查可能导致 NPE

**影响**:
- **CVSS 评分**: 7.5 (HIGH)
- **CWE**: CWE-20 (Improper Input Validation)

**修复方案**:
```java
// 1. 定义 VO
@Data
public class AttributionQueryVO {
    @NotNull(message = "场次ID不能为空")
    private Long sessionId;
}

// 2. Controller 使用 VO
@PostMapping("/session")
public RESTResult<List<Map<String, Object>>> getBySession(HttpServletRequest request,
        @Valid @RequestBody AttributionQueryVO vo) {
    Long userId = AuthTokenFilter.getUserId(request);
    if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
    List<Map<String, Object>> data = attributionService.getBySessionId(vo.getSessionId(), userId);
    // ...
}
```

**工作量**: 1 人日  
**优先级**: P1 - 应尽快修复

---

### P1-3: 归因详情查询无所有权校验 ⚠️

**来源**: security-audit (H6)

**问题描述**:
- `getById` 仅校验用户登录，未校验归因记录所有权
- 用户可通过修改 id 查看其他用户的归因详情

**影响**:
- **CVSS 评分**: 7.8 (HIGH)
- **CWE**: CWE-639 (Authorization Bypass)

**修复方案**:
```java
@Override
public Map<String, Object> getById(Long id, Long userId) {
    Attribution attr = attributionRepository.findByIdAndDeleted(id, 0)
            .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "归因数据不存在"));
    
    // 校验所有权
    if (!attr.getOwnerId().equals(userId)) {
        throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问该归因数据");
    }
    
    return toMap(attr);
}
```

**工作量**: 0.5 人日  
**优先级**: P1 - 应尽快修复

---

### P1-4: 代码重复 - 4 个方法重复定义 ⚠️

**来源**: performance-analysis (P1-3), code-review (P1-3), pattern-compliance (P1-3)

**问题描述**:
- 4 个方法在两个类中完全重复：
  - calculateProductScore()
  - calculateScriptScore()
  - extractScore()
  - findAvailableModel()

**影响**:
- 代码重复 52 行（约 7% 的后端代码）
- 维护成本高
- 违反 DRY 原则

**修复方案**:
```java
@Component
public class AttributionAlgorithm {
    public int calculateProductScore(LiveProduct product, BigDecimal totalGmv) { }
    public int calculateScriptScore(LiveScript script) { }
    public int extractScore(String content) { }
    public AiModel findAvailableModel(AiModelRepository repository) { }
}
```

**预期收益**:
- 代码重复减少 52 行
- 维护成本降低 50%

**工作量**: 1 人日  
**优先级**: P1 - 应尽快修复

---

### P1-5: 测试覆盖率极低 ⚠️

**来源**: architecture-review (P1-2), code-review (P1-4)

**问题描述**:
- 仅 1 个 Controller 测试
- 0 个 Service 测试
- 0 个 Repository 测试
- 测试覆盖率 < 10%

**影响**:
- 代码质量无法保证
- 重构风险高
- 回归测试困难

**修复方案**:
```java
// AttributionServiceImplTest.java
@Test
void testTriggerAttribution() { ... }

@Test
void testGetSummary() { ... }

// AttributionAsyncProxyTest.java
@Test
void testCalculateProductScore() { ... }

@Test
void testCalculateScriptScore() { ... }

// AttributionRepositoryTest.java
@Test
void testFindBySessionIdAndDeleted() { ... }
```

**预期收益**:
- 测试覆盖率: <10% → 80%+

**工作量**: 2-3 人日  
**优先级**: P1 - 应尽快修复

---

### P1-6: AI 调用无超时配置 ⚠️

**来源**: performance-analysis (P1-1)

**问题描述**:
- AI 调用无超时配置
- 可能等待 60s+ 无响应

**修复方案**:
```java
private void generateAiAttribution(...) {
    try {
        // 设置超时 30 秒
        LlmClient.LlmResponse response = CompletableFuture
            .supplyAsync(() -> llmClient.chat(model, system, prompt))
            .get(30, TimeUnit.SECONDS);
    } catch (TimeoutException e) {
        log.error("AI 归因分析超时: sessionId={}", sessionId);
    }
}
```

**工作量**: 0.5 人日  
**优先级**: P1 - 应尽快修复

---

### P1-7: 返回 Map 而非 VO 类 ⚠️

**来源**: pattern-compliance (P1-2), code-review (P2)

**问题描述**:
- 使用 Map<String, Object> 返回数据
- 类型不安全
- 字段名硬编码

**修复方案**:
```java
@Data
public class AttributionVO {
    private Long id;
    private Long sessionId;
    private String attributionType;
    private BigDecimal contributedGmv;
    private Integer contributedSales;
    private Integer effectScore;
    private String analysis;
    private Integer status;
    private Timestamp createTime;
}

private AttributionVO toVO(Attribution a) {
    AttributionVO vo = new AttributionVO();
    vo.setId(a.getId());
    vo.setSessionId(a.getSessionId());
    // ... 字段映射
    return vo;
}
```

**工作量**: 0.5 人日  
**优先级**: P1 - 应尽快修复

---

### P1-8: 前端 ECharts 未懒加载 ⚠️

**来源**: architecture-review (P3-4), code-review (P1-5)

**问题描述**:
- 直接导入 ReactECharts 导致首屏体积增加 1111KB

**修复方案**:
```typescript
import { LazyECharts } from '@/utils/echarts-registry'

// 替换所有 ReactECharts 为 LazyECharts
<LazyECharts option={funnelOption} style={{ height: 300 }} />
```

**预期收益**:
- 首屏体积减少 1111KB
- 首屏加载时间减少 30%+

**工作量**: 0.5 人日  
**优先级**: P1 - 应尽快修复

---

### P1-9: 缺少敏感操作审计日志 ⚠️

**来源**: security-audit (M5), pattern-compliance (P2-2)

**问题描述**:
- 归因触发、查询、删除未记录到审计日志表
- 无法满足合规要求（GDPR、等保）

**修复方案**:
```java
@Override
public long triggerAttribution(AttributionTriggerVO vo, Long ownerId) {
    // 记录审计日志
    auditLogService.log("ATTRIBUTION_TRIGGER", vo.getSessionId(), ownerId);
    // ...
}
```

**工作量**: 1.5 人日  
**优先级**: P1 - 应尽快修复

---

## P2 中优先级问题（建议修复）

### P2-1: 缺少只读事务优化

**来源**: pattern-compliance (P2-1)

**问题**: 查询方法未标记 @Transactional(readOnly = true)

**修复**: 添加只读事务注解

**工作量**: 0.5 人日

---

### P2-2: 缺少方法级权限注解

**来源**: security-audit (M1)

**问题**: 未使用 @PreAuthorize 或 @Secured 注解

**修复**: 添加方法级权限注解

**工作量**: 0.5 人日

---

### P2-3: 缺少输入长度限制

**来源**: security-audit (M2)

**问题**: sessionId 未限制范围

**修复**: 添加 @Min 和 @Max 注解

**工作量**: 0.5 人日

---

### P2-4: 错误响应可能泄露内部信息

**来源**: security-audit (M3)

**问题**: 异常信息直接记录到日志

**修复**: 使用通用错误消息

**工作量**: 0.5 人日

---

### P2-5: 缺少请求体大小限制

**来源**: security-audit (M4)

**问题**: 未配置 spring.servlet.multipart.max-request-size

**修复**: 配置请求体大小限制（10MB）

**工作量**: 0.5 人日

---

### P2-6: 缺少归因算法版本管理

**来源**: security-audit (M7), architecture-review (P3-1)

**问题**: 归因算法硬编码，无版本号

**修复**: 添加 algorithm_version 字段

**工作量**: 1 人日

---

### P2-7: 缺少归因进度追踪

**来源**: security-audit (M8), architecture-review (P3-3)

**问题**: 用户不知道计算进度

**修复**: 添加 progress 字段（0-100）

**工作量**: 1 人日

---

### P2-8: 缺少索引优化

**来源**: performance-analysis (P2-1)

**问题**: 需要使用两个索引

**修复**: 创建复合索引

**工作量**: 0.5 人日

---

### P2-9: 归因算法过于简化

**来源**: performance-analysis (P2-2), architecture-review (P1-1)

**问题**: 平均分配 GMV，未考虑时序关系

**修复**: 基于时间窗口的归因算法

**工作量**: 3-5 人日

---

### P2-10: 前端类型定义重复

**来源**: performance-analysis (P2-4), architecture-review (P2-5)

**问题**: AttributionDetail 在多处定义

**修复**: 提取到 @/types/attribution.ts

**工作量**: 0.5 人日

---

### P2-11: 前端组件过大

**来源**: performance-analysis (P2-5), architecture-review (P2-6), code-review (P2-5)

**问题**: AttributionPage.tsx 678 行

**修复**: 拆分为 4 个独立 Tab 组件

**工作量**: 1 人日

---

## P3 低优先级问题（可选修复）

### P3-1: conversion_rate 字段未使用

**来源**: architecture-review (P3-1), performance-analysis (P3-1), security-audit (L6)

**问题**: 字段定义但从未赋值

**修复**: 删除或实现转化率计算

**工作量**: 0.5 人日

---

### P3-2: 缺少性能监控

**来源**: architecture-review (P3-2), performance-analysis (P3-3)

**问题**: 无归因计算耗时/失败率监控

**修复**: 使用 Micrometer 添加监控指标

**工作量**: 1 人日

---

### P3-3: 缺少枚举值校验

**来源**: security-audit (L1)

**问题**: sessionId 未限制范围

**修复**: 添加 @Min(value = 1) 注解

**工作量**: 0.5 人日

---

### P3-4: 日志可能包含敏感信息

**来源**: security-audit (L2)

**问题**: 日志记录 AI 生成结果

**修复**: 避免记录敏感内容

**工作量**: 0.5 人日

---

### P3-5: Repository 层缺少强制隔离

**来源**: security-audit (L4)

**问题**: Repository 方法可绕过 Service 层

**修复**: 使用 JPA @Where 注解

**工作量**: 1 人日

---

### P3-6: 归因数据未加密存储

**来源**: security-audit (L5)

**问题**: 归因数据以明文存储

**修复**: 对敏感字段加密存储

**工作量**: 1.5 人日

---

### P3-7: 缺少连接池监控

**来源**: performance-analysis (P2-3)

**问题**: 未配置 Hikari 连接池监控

**修复**: 启用连接池监控和泄漏检测

**工作量**: 0.5 人日

---

### P3-8: 缺少归因失败处理

**来源**: architecture-review (P3-5)

**问题**: 前端未处理失败状态

**修复**: 添加失败提示

**工作量**: 0.5 人日

---

## 实施路线图

### 第一阶段：P0 修复（2 周）

**目标**: 修复阻塞级问题，确保生产可用

**任务清单**:
1. P0-1: 数据所有权校验 - 1 人日
2. P0-2: 敏感数据脱敏 - 2 人日
3. P0-3: Specification 动态查询 - 2 人日
4. P0-4: 实现两级缓存 - 1.5 人日
5. P0-5: 并发控制 - 1 人日
6. P0-6: 超时控制 - 1.5 人日
7. P0-7: 批量保存优化 - 0.5 人日
8. P0-8: API 限流保护 - 1.5 人日

**总工作量**: 10.5 人日

**验收标准**:
- ✅ 所有查询/删除操作都校验 session 归属
- ✅ 敏感数据访问有审计日志
- ✅ 所有查询使用 Specification
- ✅ 缓存命中率 > 80%
- ✅ 同一场次不能重复触发
- ✅ 异步任务 5 分钟超时
- ✅ 批量保存时间 < 100ms
- ✅ 归因触发接口限流（每分钟 10 次）

**预期收益**:
- 安全漏洞: 2 CRITICAL → 0
- 响应时间: 200ms → 5ms (97% 提升)
- 数据库负载: 减少 80%
- AI 额度节省: 50%+

---

### 第二阶段：P1 修复（1.5 周）

**目标**: 修复高优先级问题，提升代码质量

**任务清单**:
1. P1-1: AI 评分提取优化 - 1.5 人日
2. P1-2: Map 参数校验 - 1 人日
3. P1-3: 归因详情所有权校验 - 0.5 人日
4. P1-4: 提取重复代码 - 1 人日
5. P1-5: 添加单元测试 - 2-3 人日
6. P1-6: AI 调用超时 - 0.5 人日
7. P1-7: 定义 VO 类 - 0.5 人日
8. P1-8: ECharts 懒加载 - 0.5 人日
9. P1-9: 审计日志 - 1.5 人日

**总工作量**: 8.5-9.5 人日

**验收标准**:
- ✅ AI 评分提取成功率 > 95%
- ✅ 所有 API 使用专用 VO 类
- ✅ 测试覆盖率 > 80%
- ✅ 代码重复减少 52 行
- ✅ 前端首屏体积减少 1111KB

**预期收益**:
- AI 调用超时风险降低 90%
- 维护成本降低 50%
- 类型安全性提升

---

### 第三阶段：P2 修复（2 周）

**目标**: 完善功能，优化性能

**任务清单**:
1. P2-1 ~ P2-8: 基础优化 - 4 人日
2. P2-9: 归因算法改进 - 3-5 人日
3. P2-10 ~ P2-11: 前端优化 - 1.5 人日

**总工作量**: 8.5-10.5 人日

**验收标准**:
- ✅ 所有查询方法有只读事务
- ✅ 归因算法准确性提升 50%+
- ✅ 前端组件拆分完成

**预期收益**:
- 归因准确性大幅提升
- 性能进一步优化

---

### 第四阶段：P3 修复（1.5 周）

**目标**: 完善细节，提升用户体验

**任务清单**:
1. P3-1 ~ P3-8: 细节优化 - 7.5 人日

**总工作量**: 7.5 人日

**验收标准**:
- ✅ 性能监控指标完善
- ✅ 用户体验提升

---

## 验收标准

### 第一阶段验收（P0）

**功能验收**:
- [ ] 用户无法越权访问其他用户的归因数据
- [ ] 用户无法越权删除其他用户的归因数据
- [ ] 敏感数据访问有审计日志记录
- [ ] 所有查询使用 Specification 构建
- [ ] 归因汇总查询有缓存（响应时间 < 10ms）
- [ ] 同一场次不能重复触发归因分析
- [ ] 异步任务 5 分钟超时自动终止
- [ ] 批量保存时间 < 100ms
- [ ] 归因触发接口限流（每分钟 10 次）

**性能验收**:
- [ ] 归因汇总响应时间 < 10ms（缓存命中）
- [ ] 缓存命中率 > 80%
- [ ] 数据库查询次数减少 80%

**安全验收**:
- [ ] 无 CRITICAL 级别安全漏洞
- [ ] 无 HIGH 级别安全漏洞（数据隔离相关）

---

### 第二阶段验收（P1）

**功能验收**:
- [ ] AI 评分提取成功率 > 95%
- [ ] 所有 API 使用专用 VO 类（无 Map 参数）
- [ ] 归因详情查询校验所有权
- [ ] 代码重复减少 52 行
- [ ] 测试覆盖率 > 80%
- [ ] AI 调用 30 秒超时
- [ ] 前端 ECharts 懒加载

**代码质量验收**:
- [ ] 无代码重复（DRY 原则）
- [ ] 类型安全（无 Map 参数/返回值）
- [ ] 测试覆盖率达标

---

### 第三阶段验收（P2）

**功能验收**:
- [ ] 所有查询方法有只读事务
- [ ] 归因算法准确性提升 50%+
- [ ] 前端组件拆分完成（4 个独立 Tab）

**性能验收**:
- [ ] 查询时间进一步优化

---

### 第四阶段验收（P3）

**功能验收**:
- [ ] 性能监控指标完善
- [ ] 用户体验提升（进度追踪、失败提示）

---

## 风险评估

### 高风险项

**R1: 归因算法改进（P2-9）**
- **风险**: 算法复杂度高，可能引入新 bug
- **缓解**: 充分测试，保留旧算法作为备份
- **工作量**: 3-5 人日

**R2: 测试覆盖率提升（P1-5）**
- **风险**: 测试编写耗时，可能延期
- **缓解**: 优先核心功能测试，逐步完善
- **工作量**: 2-3 人日

### 中风险项

**R3: 缓存实现（P0-4）**
- **风险**: 缓存失效策略可能不完善
- **缓解**: 充分测试缓存失效场景
- **工作量**: 1.5 人日

**R4: 并发控制（P0-5）**
- **风险**: 分布式锁可能影响性能
- **缓解**: 使用数据库状态检查作为备选方案
- **工作量**: 1 人日

### 低风险项

**R5: 前端组件拆分（P2-11）**
- **风险**: 拆分后可能影响功能
- **缓解**: 充分测试，保留原组件作为备份
- **工作量**: 1 人日

---

## 资源需求

### 人力需求

| 角色 | 工作量 | 说明 |
|------|--------|------|
| 后端开发 | 25 人日 | P0-P3 后端修复 |
| 前端开发 | 5 人日 | P1-P3 前端修复 |
| 测试工程师 | 6 人日 | 单元测试、集成测试 |
| **总计** | **36 人日** | **约 7 周（1 人完成）** |

### 技术依赖

**新增依赖**:
- Resilience4j RateLimiter（限流）
- Redisson（分布式锁，可选）
- Jackson（JSON 解析）

**配置变更**:
- 缓存配置（Caffeine + Redis）
- 线程池配置
- 限流配置

---

## 总结

### 当前状态

**总体评分**: 69.4/100 (C+)

**主要问题**:
1. 🔴 数据隔离缺失（CRITICAL）
2. 🔴 敏感数据泄露（CRITICAL）
3. 🔴 无缓存机制
4. 🔴 无并发控制
5. 🔴 异步任务无超时

### 修复后预期状态

**预期评分**: 85.6/100 (B+)

**关键改进**:
- 安全漏洞: 2 CRITICAL → 0
- 响应时间: 200ms → 5ms (97% 提升)
- 缓存命中率: 0% → 85%
- 数据库负载: 减少 80%
- 测试覆盖率: <10% → 80%+
- 代码重复: 减少 52 行

### 投资回报分析

**总投资**: 36 人日（约 7 周，1 人完成）

**预期回报**:
- 用户体验大幅提升（响应时间 97% 提升）
- 成本节省（AI 额度节省 50%+，数据库负载减少 80%）
- 维护成本降低（代码重复减少，测试覆盖完善）
- 系统稳定性提升（超时控制，并发控制）
- 安全合规（消除 CRITICAL 漏洞，满足 GDPR/等保要求）

**ROI**: 高（投资 7 周，长期收益显著）

---

**报告生成日期**: 2026-05-09  
**下次审查建议**: 2026-08-09（3 个月后）  
**相关文档**:
- `docs/modules/attribution/architecture-review.md`
- `docs/modules/attribution/code-review.md`
- `docs/modules/attribution/security-audit.md`
- `docs/modules/attribution/performance-analysis.md`
- `docs/modules/attribution/pattern-compliance.md`
