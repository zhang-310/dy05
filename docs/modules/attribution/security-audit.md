# Attribution 模块安全审计报告

**审计日期**: 2026-05-09  
**模块**: attribution (归因分析)  
**审计者**: Claude Code Security Reviewer  
**审计范围**: 后端代码 (douyin-operations-intelligence/src/main/java/.../module/attribution/)  
**审计标准**: OWASP Top 10 2021, CWE Top 25, CVSS 3.1

---

## 执行摘要

**总体安全评分**: 62/100 (中等偏低)

| 维度 | 评分 | 说明 |
|-----|------|------|
| 认证授权 | 60/100 | Bearer Token 认证，但缺少数据所有权校验 |
| 数据隔离 | 45/100 | 存在严重 IDOR 风险，session 归属未校验 |
| 输入验证 | 75/100 | @Valid 注解覆盖，但缺少边界校验 |
| SQL 注入防护 | 100/100 | JPA Specification 参数化查询，无风险 |
| 敏感数据保护 | 55/100 | GMV/销售数据未脱敏，缺少访问审计 |
| 错误处理 | 65/100 | 错误信息可能泄露内部细节 |
| 日志审计 | 40/100 | 缺少敏感操作审计日志 |
| API 限流 | 10/100 | 无限流保护，AI 调用存在滥用风险 |
| 并发控制 | 30/100 | 无并发控制，可能重复触发 |
| 缓存安全 | 0/100 | 无缓存机制，无缓存投毒风险但性能差 |

**关键发现**:
- 🔴 2 个 CRITICAL 问题（数据所有权校验缺失、敏感数据泄露）
- ⚠️ 6 个 HIGH 问题（无 API 限流、无并发控制、AI 评分提取脆弱等）
- ⚠️ 8 个 MEDIUM 问题
- ℹ️ 6 个 LOW 问题

**总工作量估算**: 18.5 人日

**生产就绪度**: 🔴 必须修复 CRITICAL 和 HIGH 问题后上线

---

## 1. 认证与授权

### ✅ 优点

**统一鉴权机制**: 所有 Controller 使用 `AuthTokenFilter.getUserId(request)` 获取当前用户  
**逻辑删除**: Entity 使用 `@SQLRestriction("deleted = 0")` 自动过滤已删除记录  
**触发归因校验场次存在**: `triggerAttribution()` 校验场次是否存在

**示例代码** (`AttributionServiceImpl.java:46-47`):
```java
LiveSession session = sessionRepository.findById(vo.getSessionId())
        .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "直播场次不存在"));
```

### 🔴 CRITICAL 问题

**C1 - 查询/删除操作缺少数据所有权校验**
- **位置**: 
  - `AttributionServiceImpl.java:61-64` (getBySessionId)
  - `AttributionServiceImpl.java:114-119` (deleteBySessionId)
  - `AttributionController.java:38-50` (getBySession)
  - `AttributionController.java:80-89` (deleteBySession)
- **CVSS 评分**: 9.1 (CRITICAL)
- **CWE**: CWE-639 (Authorization Bypass Through User-Controlled Key)
- **问题**: 
  - 查询场次归因仅校验用户登录，未校验场次是否属于当前用户
  - 删除场次归因未校验场次所有权
  - 用户可通过修改 sessionId 参数查看/删除其他用户的归因数据
  - 违反数据隔离原则


- **代码示例**:
  ```java
  // AttributionServiceImpl.java:61-64
  @Override
  public List<Map<String, Object>> getBySessionId(Long sessionId) {
      return attributionRepository.findBySessionIdAndDeleted(sessionId, 0)
              .stream().map(this::toMap).collect(Collectors.toList());
      // ❌ 未校验 sessionId 是否属于当前用户
  }
  
  // AttributionServiceImpl.java:114-119
  @Override
  @Transactional(rollbackFor = Exception.class)
  public void deleteBySessionId(Long sessionId) {
      List<Attribution> attrs = attributionRepository.findBySessionIdAndDeleted(sessionId, 0);
      attrs.forEach(a -> a.setDeleted(1));
      attributionRepository.saveAll(attrs);
      // ❌ 未校验 sessionId 是否属于当前用户
  }
  ```
- **利用场景**:
  1. 攻击者登录后获取自己的场次 ID（如 100）
  2. 遍历 ID 101-200，调用 `/api/v1/ai/attribution/session` 查看其他用户的归因数据
  3. 同样方式调用 DELETE `/api/v1/ai/attribution/session/{id}` 删除其他用户的归因数据
  4. 获取敏感的商品销售数据、话术内容、GMV 数据
- **影响**: 
  - **数据泄露**: 用户可以越权访问其他用户的归因数据（GMV、销售额、商品名称、话术内容）
  - **数据丢失**: 用户可以删除其他用户的归因数据（逻辑删除可恢复，但影响业务）
  - **横向越权攻击**: 违反数据隔离原则
  - **合规风险**: 违反 GDPR、等保 2.0 数据隔离要求

- **修复方案**:
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
  
  @Override
  @Transactional(rollbackFor = Exception.class)
  public void deleteBySessionId(Long sessionId, Long userId) {
      // 校验 session 归属
      LiveSession session = sessionRepository.findById(sessionId)
          .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "场次不存在"));
      
      if (!session.getOwnerId().equals(userId)) {
          throw new BusinessException(ErrorCode.FORBIDDEN, "无权删除该场次");
      }
      
      // 逻辑删除
      List<Attribution> attrs = attributionRepository.findBySessionIdAndDeleted(sessionId, 0);
      attrs.forEach(a -> a.setDeleted(1));
      attributionRepository.saveAll(attrs);
  }
  
  // 2. Controller 传递 userId
  @PostMapping("/session")
  public RESTResult<List<Map<String, Object>>> getBySession(HttpServletRequest request,
          @RequestBody(required = false) java.util.Map<String, Long> body) {
      Long userId = AuthTokenFilter.getUserId(request);
      if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
      Long sessionId = body != null ? body.get("sessionId") : null;
      if (sessionId == null) return RESTResult.error(ErrorCode.VALIDATION_FAIL, "缺少 sessionId");
      List<Map<String, Object>> data = attributionService.getBySessionId(sessionId, userId); // ✅ 传递 userId
      // ...
  }
  ```
- **工作量**: 1 人日
- **优先级**: P0 - 必须立即修复

**C2 - 敏感数据未脱敏直接返回**
- **位置**: 
  - `AttributionServiceImpl.java:155-167` (toMap)
  - `AttributionServiceImpl.java:73-110` (getSummary)
- **CVSS 评分**: 8.2 (CRITICAL)
- **CWE**: CWE-200 (Exposure of Sensitive Information to an Unauthorized Actor)
- **问题**: 
  - 归因数据包含敏感商业信息（GMV、销售额、商品名称、话术内容）
  - 直接返回给前端，未做任何脱敏处理
  - AI 分析报告可能包含敏感信息
  - 无访问审计日志，无法追踪数据访问

- **代码示例**:
  ```java
  // AttributionServiceImpl.java:155-167 - 直接返回所有敏感字段
  private Map<String, Object> toMap(Attribution a) {
      Map<String, Object> m = new LinkedHashMap<>();
      m.put("productName", a.getProductName());           // ❌ 商品名称（商业敏感）
      m.put("scriptContent", a.getScriptContent());       // ❌ 话术内容（商业敏感）
      m.put("contributedGmv", a.getContributedGmv());     // ❌ GMV（商业敏感）
      m.put("contributedSales", a.getContributedSales()); // ❌ 销售额（商业敏感）
      m.put("analysis", a.getAnalysis());                 // ❌ AI 分析报告（可能含敏感信息）
      // ...
  }
  ```
- **影响**: 
  - **商业数据泄露**: GMV、销售额、商品信息一旦通过 C1 漏洞泄露，对竞争对手暴露核心商业数据
  - **合规风险**: 违反《个人信息保护法》、GDPR 数据最小化原则
  - **审计缺失**: 无法追踪敏感数据访问历史
- **修复方案**:
  ```java
  // 1. 添加访问审计日志
  @Override
  public List<Map<String, Object>> getBySessionId(Long sessionId, Long userId) {
      // 记录访问审计
      auditLogService.log("ATTRIBUTION_ACCESS", sessionId, userId);
      // ...
  }
  
  // 2. 按需返回字段（不返回完整内容快照）
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
  
  // 3. 详情接口单独授权
  private Map<String, Object> toMapDetail(Attribution a) {
      // 返回完整内容（需要单独权限校验）
  }
  ```
- **工作量**: 2 人日
- **优先级**: P0 - 必须立即修复


### 🟠 HIGH 问题

**H1 - 缺少 API 限流保护**
- **位置**: 所有 Controller（特别是 AI 调用）
  - `AttributionController.java:27-36` (trigger)
  - `AttributionAsyncProxy.java` (AI 调用)
- **CVSS 评分**: 8.6 (HIGH)
- **CWE**: CWE-770 (Allocation of Resources Without Limits or Throttling)
- **问题**: 
  - 无全局限流保护
  - 归因触发接口无调用频率限制
  - AI 调用无并发控制（可能同时触发多个场次）
  - 攻击者可暴力调用 AI API 导致额度耗尽
- **影响**: 
  - 攻击者可频繁触发归因分析导致 AI 额度耗尽
  - 服务器资源耗尽导致 DoS
  - 成本失控
- **修复方案**:
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
- **工作量**: 1.5 人日
- **优先级**: P1 - 应该立即修复

**H2 - 缺少并发控制，可能重复触发**
- **位置**: `AttributionServiceImpl.java:44-58` (triggerAttribution)
- **CVSS 评分**: 7.5 (HIGH)
- **CWE**: CWE-362 (Concurrent Execution using Shared Resource with Improper Synchronization)
- **问题**: 
  - 同一场次可能被重复触发归因分析
  - 无分布式锁保护
  - 可能产生重复记录
  - 浪费 AI 调用额度
- **修复方案**:
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
      
      // 2. 使用分布式锁（Redis）
      String lockKey = "attribution:trigger:" + vo.getSessionId();
      RLock lock = redissonClient.getLock(lockKey);
      try {
          if (!lock.tryLock(0, 300, TimeUnit.SECONDS)) {
              throw new BusinessException(ErrorCode.OPERATION_FAIL, "该场次正在计算中");
          }
          // 创建任务
          Attribution overall = new Attribution();
          // ...
      } finally {
          lock.unlock();
      }
  }
  ```
- **工作量**: 1.5 人日
- **优先级**: P1 - 应该立即修复


**H3 - AI 评分提取依赖正则，存在注入风险**
- **位置**: `AttributionServiceImpl.java:137-145` (extractScore)
- **CVSS 评分**: 7.2 (HIGH)
- **CWE**: CWE-20 (Improper Input Validation)
- **问题**: 
  - AI 评分提取使用正则匹配，脆弱且不可靠
  - AI 输出格式变化会导致提取失败
  - 默认返回 50 分（不准确）
  - 可能被恶意 AI 响应注入
- **代码示例**:
  ```java
  private int extractScore(String content) {
      try {
          var matcher = java.util.regex.Pattern.compile("(\\d{1,3})\\s*[/\uff0f\u5206]").matcher(content);
          if (matcher.find()) return Math.min(Integer.parseInt(matcher.group(1)), 100);
          matcher = java.util.regex.Pattern.compile("\u8bc4\u5206[\uff1a:]?\\s*(\\d{1,3})").matcher(content);
          if (matcher.find()) return Math.min(Integer.parseInt(matcher.group(1)), 100);
      } catch (Exception ignored) {}
      return 50; // ❌ 默认返回 50，不准确
  }
  ```
- **影响**: 
  - AI 评分提取失败率高
  - 归因结果不准确
  - 可能被恶意 AI 响应注入（如返回 "999 分"）
- **修复方案**:
  ```java
  // 方案 1: 使用结构化输出（JSON）
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
  
  // 方案 2: 使用 Function Calling
  // 定义 function schema，让 AI 返回结构化数据
  ```
- **工作量**: 1.5 人日
- **优先级**: P1 - 应该立即修复

**H4 - 异步任务无超时控制**
- **位置**: `AttributionAsyncProxy.java` (asyncAttribution)
- **CVSS 评分**: 7.5 (HIGH)
- **CWE**: CWE-400 (Uncontrolled Resource Consumption)
- **问题**: 
  - 异步任务无超时限制
  - 长时间运行的 AI 调用可能阻塞线程
  - 使用默认线程池，未配置
- **修复方案**:
  ```java
  // 1. 配置异步线程池
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
          // 设置超时
          CompletableFuture.runAsync(() -> {
              // 归因计算逻辑
          }).get(300, TimeUnit.SECONDS);
      } catch (TimeoutException e) {
          log.error("归因分析超时: sessionId={}", sessionId);
          // 更新状态为失败
      }
  }
  ```
- **工作量**: 1 人日
- **优先级**: P1 - 应该立即修复


**H5 - Map 参数未校验导致类型转换风险**
- **位置**: 
  - `AttributionController.java:40-50` (getBySession)
  - `AttributionController.java:54-64` (getSummary)
  - `AttributionController.java:68-78` (getById)
- **CVSS 评分**: 7.5 (HIGH)
- **CWE**: CWE-20 (Improper Input Validation)
- **问题**: 
  - 直接从 Map 中取值，未校验类型和范围
  - 类型转换可能抛出 ClassCastException
  - 缺少 null 检查可能导致 NPE
- **代码示例**:
  ```java
  @PostMapping("/session")
  public RESTResult<List<Map<String, Object>>> getBySession(HttpServletRequest request,
          @RequestBody(required = false) java.util.Map<String, Long> body) {
      // ...
      Long sessionId = body != null ? body.get("sessionId") : null; // ❌ 未校验类型
      if (sessionId == null) return RESTResult.error(ErrorCode.VALIDATION_FAIL, "缺少 sessionId");
      // ...
  }
  ```
- **修复方案**: 使用专用 VO 类替换 Map 参数
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
- **工作量**: 1 人日
- **优先级**: P1 - 应该立即修复

**H6 - 归因详情查询无所有权校验**
- **位置**: `AttributionServiceImpl.java:67-70` (getById)
- **CVSS 评分**: 7.8 (HIGH)
- **CWE**: CWE-639 (Authorization Bypass Through User-Controlled Key)
- **问题**: 
  - `getById` 仅校验用户登录，未校验归因记录所有权
  - 用户可通过修改 id 查看其他用户的归因详情
- **修复方案**:
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
- **工作量**: 0.5 人日
- **优先级**: P1 - 应该立即修复


### 🟡 MEDIUM 问题

**M1 - 缺少方法级权限注解**
- **位置**: AttributionController（5 个 API 端点）
- **CVSS 评分**: 5.3 (MEDIUM)
- **CWE**: CWE-862 (Missing Authorization)
- **问题**: 未使用 `@PreAuthorize` 或 `@Secured` 注解，权限校验完全依赖 Filter
- **修复建议**: 添加方法级权限注解
- **工作量**: 0.5 人日
- **优先级**: P2 - 建议修复

**M2 - 缺少输入长度限制**
- **位置**: `AttributionTriggerVO.java`
- **CVSS 评分**: 5.3 (MEDIUM)
- **CWE**: CWE-1284 (Improper Validation of Specified Quantity in Input)
- **问题**: sessionId 未限制范围
- **修复建议**: 添加 `@Min` 和 `@Max` 注解
- **工作量**: 0.5 人日
- **优先级**: P2 - 应尽快修复

**M3 - 错误响应可能泄露内部信息**
- **位置**: `AttributionAsyncProxy.java` (异常处理)
- **CVSS 评分**: 4.3 (MEDIUM)
- **CWE**: CWE-209 (Generation of Error Message Containing Sensitive Information)
- **问题**: 异常信息直接记录到日志，可能包含敏感信息
- **修复建议**: 使用通用错误消息，详细错误记录到日志
- **工作量**: 0.5 人日
- **优先级**: P2 - 应尽快修复

**M4 - 缺少请求体大小限制**
- **位置**: application.yml
- **CVSS 评分**: 5.3 (MEDIUM)
- **CWE**: CWE-400 (Uncontrolled Resource Consumption)
- **问题**: 未配置 `spring.servlet.multipart.max-request-size`
- **修复建议**: 配置请求体大小限制（如 10MB）
- **工作量**: 0.5 人日
- **优先级**: P2 - 应尽快修复

**M5 - 缺少敏感操作审计日志**
- **位置**: 所有 Controller
- **CVSS 评分**: 4.3 (MEDIUM)
- **CWE**: CWE-778 (Insufficient Logging)
- **问题**: 
  - 归因触发、查询、删除未记录到审计日志表
  - 无法满足合规要求（如 GDPR、等保）
  - 无法追踪敏感数据访问历史
- **修复建议**: 实现审计日志表
  ```java
  @Override
  public long triggerAttribution(AttributionTriggerVO vo, Long ownerId) {
      // 记录审计日志
      auditLogService.log("ATTRIBUTION_TRIGGER", vo.getSessionId(), ownerId);
      // ...
  }
  ```
- **工作量**: 1.5 人日
- **优先级**: P2 - 应尽快修复

**M6 - AI 服务调用缺少超时配置**
- **位置**: `AttributionAsyncProxy.java` (LLM 调用)
- **CVSS 评分**: 4.3 (MEDIUM)
- **CWE**: CWE-400 (Uncontrolled Resource Consumption)
- **问题**: 未配置 AI 服务调用超时时间
- **修复建议**: 配置 LlmClient 超时时间（如 60 秒）
- **工作量**: 0.5 人日
- **优先级**: P2 - 应尽快修复

**M7 - 缺少归因算法版本管理**
- **位置**: Attribution 表结构
- **CVSS 评分**: 3.3 (MEDIUM)
- **CWE**: CWE-1059 (Incomplete Documentation)
- **问题**: 
  - 归因算法硬编码，无版本号
  - 算法迭代后无法追溯历史归因使用的算法版本
  - 无法对比不同算法效果
- **修复建议**: 添加 `algorithm_version` 字段
- **工作量**: 1 人日
- **优先级**: P2 - 应尽快修复

**M8 - 缺少归因进度追踪**
- **位置**: Attribution 表结构
- **CVSS 评分**: 3.3 (MEDIUM)
- **CWE**: CWE-1059 (Incomplete Documentation)
- **问题**: 
  - 用户触发归因后不知道计算进度
  - 仅有 status（0=计算中 1=完成 2=失败）
- **修复建议**: 添加 `progress` 字段（0-100）
- **工作量**: 1 人日
- **优先级**: P2 - 应尽快修复


### 🔵 LOW 问题

**L1 - 缺少枚举值校验**
- **位置**: `AttributionTriggerVO.java`
- **CVSS 评分**: 3.3 (LOW)
- **CWE**: CWE-20 (Improper Input Validation)
- **问题**: sessionId 未限制范围
- **修复建议**: 添加 `@Min(value = 1)` 注解
- **工作量**: 0.5 人日
- **优先级**: P3 - 建议改进

**L2 - 日志可能包含敏感信息**
- **位置**: `AttributionAsyncProxy.java` (日志记录)
- **CVSS 评分**: 3.3 (LOW)
- **CWE**: CWE-532 (Insertion of Sensitive Information into Log File)
- **问题**: 日志记录 AI 生成结果，可能包含敏感信息
- **修复建议**: 避免记录敏感内容，仅记录元数据
- **工作量**: 0.5 人日
- **优先级**: P3 - 建议改进

**L3 - 缺少并发控制**
- **位置**: `AttributionServiceImpl.java:44-58` (triggerAttribution)
- **CVSS 评分**: 3.3 (LOW)
- **CWE**: CWE-362 (Concurrent Execution using Shared Resource with Improper Synchronization)
- **问题**: 未使用乐观锁或悲观锁
- **修复建议**: 使用 `@Version` 乐观锁或数据库原子操作
- **工作量**: 0.5 人日
- **优先级**: P3 - 建议改进

**L4 - Repository 层缺少强制隔离**
- **位置**: AttributionRepository
- **CVSS 评分**: 3.3 (LOW)
- **CWE**: CWE-862 (Missing Authorization)
- **问题**: Repository 方法可绕过 Service 层直接查询所有数据
- **修复建议**: 使用 JPA `@Where` 注解或自定义 Repository 基类强制过滤
- **工作量**: 1 人日
- **优先级**: P3 - 建议改进

**L5 - 归因数据未加密存储**
- **位置**: Attribution 表
- **CVSS 评分**: 3.3 (LOW)
- **CWE**: CWE-311 (Missing Encryption of Sensitive Data)
- **问题**: 
  - 归因数据以明文存储
  - 可能包含敏感信息（GMV、销售额）
- **修复建议**: 对敏感字段加密存储
- **工作量**: 1.5 人日
- **优先级**: P3 - 建议改进

**L6 - conversion_rate 字段未使用**
- **位置**: Attribution 表
- **CVSS 评分**: 2.0 (LOW)
- **CWE**: CWE-1164 (Irrelevant Code)
- **问题**: 字段定义但从未赋值
- **修复建议**: 删除或实现转化率计算
- **工作量**: 0.5 人日
- **优先级**: P3 - 建议改进

---

## 2. 输入验证

### ✅ 优点

**@Valid 注解覆盖**: SaveVO 类使用 `@Valid` 注解进行参数校验  
**JPA Specification 防注入**: 使用 Specification 动态查询，避免 SQL 拼接  
**参数非空校验**: Controller 层校验 sessionId 非空

### 已识别问题

- **H5**: Map 参数未校验导致类型转换风险（已在第 1 节描述）
- **M2**: 缺少输入长度限制（已在第 1 节描述）
- **L1**: 缺少枚举值校验（已在第 1 节描述）

---

## 3. 数据访问控制

### ✅ 优点

**逻辑删除**: Entity 使用 `@SQLRestriction("deleted = 0")` 防止查询已删除数据  
**触发归因校验场次存在**: `triggerAttribution()` 校验场次是否存在  
**owner_id 字段**: 归因记录包含 owner_id 字段（但未强制过滤）

### 🔴 CRITICAL 问题

- **C1**: 查询/删除操作缺少数据所有权校验（已在第 1 节描述）

### 🟠 HIGH 问题

- **H6**: 归因详情查询无所有权校验（已在第 1 节描述）

### 🔵 LOW 问题

- **L3**: 缺少并发控制（已在第 1 节描述）
- **L4**: Repository 层缺少强制隔离（已在第 1 节描述）

## 4. API 安全

### ✅ 优点

**统一 POST 方法**: 所有业务 API 使用 POST，避免 GET 参数泄露  
**CSRF 保护**: 使用 Bearer Token 认证，天然防 CSRF  
**错误信息安全**: 大部分错误响应不泄露堆栈信息

### 已识别问题

- **H1**: 缺少 API 限流保护（已在第 1 节描述）
- **H5**: Map 参数未校验导致类型转换风险（已在第 1 节描述）
- **M3**: 错误响应可能泄露内部信息（已在第 1 节描述）
- **M4**: 缺少请求体大小限制（已在第 1 节描述）

---

## 5. 敏感数据保护

### ✅ 优点

**无硬编码密钥**: 所有 AI 服务密钥从环境变量读取  
**日志安全**: 日志中不包含密码、Token 等敏感信息  
**数据库连接加密**: 数据库连接使用 SSL（生产环境配置）

### 🔴 CRITICAL 问题

- **C2**: 敏感数据未脱敏直接返回（已在第 1 节描述）

### 🔵 LOW 问题

- **L2**: 日志可能包含敏感信息（已在第 1 节描述）
- **L5**: 归因数据未加密存储（已在第 1 节描述）

### 敏感数据清单

| 数据类型 | 字段 | 敏感级别 | 当前保护 | 建议保护 |
|---------|------|---------|---------|---------|
| GMV | contributed_gmv | 高 | 无 | 访问审计 + 脱敏 |
| 销售额 | contributed_sales | 高 | 无 | 访问审计 + 脱敏 |
| 商品名称 | product_name | 中 | 无 | 访问审计 |
| 话术内容 | script_content | 高 | 无 | 访问审计 + 摘要返回 |
| AI 分析 | analysis | 中 | 无 | 访问审计 |
| 用户 ID | owner_id | 中 | 有（数据隔离） | 强制过滤 |

---

## 6. 会话管理

### ✅ 优点

**Token 存储**: Token 存储在 Redis，支持过期和主动失效  
**Token 校验**: AuthTokenFilter 统一校验 Token 有效性  
**登出清理**: 登出时删除 Redis 中的 Token

### 无新增问题

会话管理由 common 模块统一处理，无 attribution 模块特有问题。

---

## 7. 异步处理安全

### ✅ 优点

**异步处理**: 使用 `@Async` 注解实现异步归因计算  
**状态追踪**: 归因记录包含 status 字段（0=计算中 1=完成 2=失败）  
**错误处理**: 异步任务有异常捕获

### 🟠 HIGH 问题

- **H2**: 缺少并发控制，可能重复触发（已在第 1 节描述）
- **H4**: 异步任务无超时控制（已在第 1 节描述）

### 🟡 MEDIUM 问题

- **M6**: AI 服务调用缺少超时配置（已在第 1 节描述）

---

## 8. 安全问题汇总

### CRITICAL (2 个)
1. **C1**: 查询/删除操作缺少数据所有权校验 - AttributionServiceImpl, AttributionController
2. **C2**: 敏感数据未脱敏直接返回 - AttributionServiceImpl

### HIGH (6 个)
1. **H1**: 缺少 API 限流保护 - AttributionController
2. **H2**: 缺少并发控制，可能重复触发 - AttributionServiceImpl
3. **H3**: AI 评分提取依赖正则，存在注入风险 - AttributionServiceImpl
4. **H4**: 异步任务无超时控制 - AttributionAsyncProxy
5. **H5**: Map 参数未校验导致类型转换风险 - AttributionController
6. **H6**: 归因详情查询无所有权校验 - AttributionServiceImpl

### MEDIUM (8 个)
1. **M1**: 缺少方法级权限注解 - AttributionController
2. **M2**: 缺少输入长度限制 - AttributionTriggerVO
3. **M3**: 错误响应可能泄露内部信息 - AttributionAsyncProxy
4. **M4**: 缺少请求体大小限制 - application.yml
5. **M5**: 缺少敏感操作审计日志 - 所有 Controller
6. **M6**: AI 服务调用缺少超时配置 - AttributionAsyncProxy
7. **M7**: 缺少归因算法版本管理 - Attribution 表
8. **M8**: 缺少归因进度追踪 - Attribution 表

### LOW (6 个)
1. **L1**: 缺少枚举值校验 - AttributionTriggerVO
2. **L2**: 日志可能包含敏感信息 - AttributionAsyncProxy
3. **L3**: 缺少并发控制 - AttributionServiceImpl
4. **L4**: Repository 层缺少强制隔离 - AttributionRepository
5. **L5**: 归因数据未加密存储 - Attribution 表
6. **L6**: conversion_rate 字段未使用 - Attribution 表

---

## 9. 修复优先级

### 立即修复 (本周内)
1. **C1**: 修复数据所有权校验问题 - 工作量 1 人日
2. **C2**: 添加敏感数据访问审计和脱敏 - 工作量 2 人日
3. **H1**: 添加 API 限流保护（Resilience4j RateLimiter） - 工作量 1.5 人日
4. **H2**: 添加并发控制（分布式锁） - 工作量 1.5 人日
5. **H3**: 改进 AI 评分提取（结构化输出） - 工作量 1.5 人日
6. **H4**: 添加异步任务超时控制 - 工作量 1 人日
7. **H5**: 修复 Map 参数校验问题，使用专用 VO 类 - 工作量 1 人日
8. **H6**: 添加归因详情查询所有权校验 - 工作量 0.5 人日

### 短期修复 (2 周内)
9. **M1**: 添加方法级权限注解 `@PreAuthorize` - 工作量 0.5 人日
10. **M2**: 添加输入长度限制 `@Size` - 工作量 0.5 人日
11. **M3**: 统一错误信息，避免泄露内部信息 - 工作量 0.5 人日
12. **M4**: 配置请求体大小限制 - 工作量 0.5 人日
13. **M5**: 实现敏感操作审计日志 - 工作量 1.5 人日
14. **M6**: 配置 AI 服务调用超时 - 工作量 0.5 人日
15. **M7**: 添加归因算法版本管理 - 工作量 1 人日
16. **M8**: 添加归因进度追踪 - 工作量 1 人日

### 长期优化 (1 个月内)
17. **L1**: 添加枚举值校验 - 工作量 0.5 人日
18. **L2**: 优化日志记录，避免记录敏感信息 - 工作量 0.5 人日
19. **L3**: 添加并发控制自动重试 - 工作量 0.5 人日
20. **L4**: Repository 层强制隔离 - 工作量 1 人日
21. **L5**: 归因数据加密存储 - 工作量 1.5 人日
22. **L6**: 删除或实现 conversion_rate 字段 - 工作量 0.5 人日

**总工作量估算**: 18.5 人日（约 3.7 周，1 人完成）

---

## 10. 安全最佳实践建议

1. **认证授权**:
   - 使用声明式权限注解（@PreAuthorize）
   - 所有查询/修改/删除操作校验数据所有权
   - 最小权限原则

2. **输入验证**:
   - 使用专用 VO 类替代 Map 参数
   - 添加 @Size 长度限制
   - 添加 @Min/@Max 数值范围限制
   - 添加 @NotNull 非空校验

3. **API 安全**:
   - 全局限流保护（Resilience4j）
   - 归因触发接口单独限流（每分钟 10 次）
   - 异步任务超时控制（5 分钟）
   - 错误信息脱敏
   - 请求体大小限制（10MB）

4. **数据访问控制**:
   - 所有查询强制过滤 userId
   - Repository 层强制隔离
   - 归因数据所有权校验
   - 敏感数据访问审计

5. **敏感数据保护**:
   - GMV/销售额数据访问审计
   - 话术内容摘要返回（不返回完整内容）
   - 敏感字段加密存储
   - 定期归档历史数据

6. **日志审计**:
   - 记录所有敏感操作（触发/查询/删除）
   - 脱敏敏感参数
   - 定期归档日志
   - 审计日志不可篡改

7. **异步处理安全**:
   - 配置专用线程池
   - 添加超时控制
   - 添加并发控制（分布式锁）
   - 异常处理和状态更新

8. **定期审计**:
   - 每季度进行安全审计
   - 上线前进行渗透测试
   - 使用 OWASP Dependency Check 扫描依赖漏洞
   - 定期更新依赖版本

---

## 11. 合规性检查

### OWASP Top 10 (2021) 覆盖情况

| 风险 | 状态 | 说明 |
|------|------|------|
| A01:2021 – Broken Access Control | 🔴 高风险 | 缺少数据所有权校验（C1），需立即修复 |
| A02:2021 – Cryptographic Failures | ⚠️ 部分防护 | 敏感数据未加密存储（L5） |
| A03:2021 – Injection | ✅ 已防护 | JPA Specification 参数化查询，无 SQL 注入风险 |
| A04:2021 – Insecure Design | ⚠️ 部分防护 | 缺少限流和超时控制（H1/H4） |
| A05:2021 – Security Misconfiguration | ⚠️ 部分防护 | 缺少请求体大小限制、AI 超时配置 |
| A06:2021 – Vulnerable Components | ✅ 已防护 | 依赖定期更新 |
| A07:2021 – Identification and Authentication Failures | ✅ 已防护 | JWT + Redis 双重验证 |
| A08:2021 – Software and Data Integrity Failures | ✅ 已防护 | 无反序列化漏洞 |
| A09:2021 – Security Logging and Monitoring Failures | 🔴 高风险 | 缺少审计日志表（M5），敏感数据访问无审计（C2） |
| A10:2021 – Server-Side Request Forgery (SSRF) | ✅ 已防护 | 无用户控制的 URL 请求 |

### GDPR 合规性

| 要求 | 状态 | 说明 |
|------|------|------|
| 数据最小化 | 🔴 不合规 | 返回完整敏感数据，未按需返回（C2） |
| 访问控制 | 🔴 不合规 | 缺少数据所有权校验（C1） |
| 数据加密 | ⚠️ 部分合规 | 传输加密（HTTPS），存储未加密（L5） |
| 访问审计 | 🔴 不合规 | 无审计日志表（M5） |
| 数据删除 | ✅ 合规 | 支持逻辑删除 |

### 等保 2.0 合规性

| 要求 | 状态 | 说明 |
|------|------|------|
| 身份鉴别 | ✅ 合规 | Bearer Token 认证 |
| 访问控制 | 🔴 不合规 | 缺少数据所有权校验（C1） |
| 安全审计 | 🔴 不合规 | 无审计日志表（M5） |
| 数据完整性 | ✅ 合规 | 数据库事务保证 |
| 数据保密性 | ⚠️ 部分合规 | 传输加密，存储未加密（L5） |

---

## 12. Attribution 模块特有风险

### 敏感商业数据泄露（CRITICAL）

**风险描述**: 归因数据包含核心商业信息（GMV、销售额、商品名称、话术内容），一旦泄露对竞争对手暴露核心商业数据

**攻击向量**:
- 通过 C1 漏洞越权访问其他用户的归因数据
- 通过 API 遍历获取大量归因数据
- 内部人员滥用权限导出数据
- 数据库备份泄露（未加密）

**防护措施**:
1. 修复数据所有权校验问题（C1）
2. 添加敏感数据访问审计（C2）
3. 按需返回字段，不返回完整内容快照
4. 敏感字段加密存储（L5）
5. 定期审计访问日志

### AI 调用滥用（HIGH）

**风险描述**: 归因分析依赖 AI 调用，无限流保护可能导致额度耗尽

**攻击向量**:
- 频繁触发归因分析
- 同时触发多个场次的归因分析
- 恶意用户暴力调用 API

**防护措施**:
1. 添加 API 限流保护（H1）
2. 添加并发控制（H2）
3. 添加异步任务超时控制（H4）
4. 监控 AI 调用频率和成本

### 归因算法安全（HIGH）

**风险描述**: AI 评分提取依赖正则匹配，可能被恶意 AI 响应注入

**攻击向量**:
- AI 返回恶意格式的评分（如 "999 分"）
- AI 返回无法解析的格式导致默认返回 50 分
- 归因结果不准确影响业务决策

**防护措施**:
1. 改进 AI 评分提取（H3）
2. 使用结构化输出（JSON）
3. 添加评分范围校验（0-100）
4. 记录 AI 原始响应用于审计

---

## 13. 审计结论

**总体评价**: Attribution 模块安全性中等偏低，存在 2 个 CRITICAL 级别漏洞（数据所有权校验缺失、敏感数据泄露），必须修复后上线。

**主要优势**:
- 统一的认证授权机制
- JPA Specification 防 SQL 注入
- 无硬编码密钥和敏感信息泄露
- 异步处理设计合理
- 逻辑删除机制

**需要改进**:
- 修复数据所有权校验问题（P0）
- 添加敏感数据访问审计和脱敏（P0）
- 添加 API 限流和并发控制（P1）
- 改进 AI 评分提取（P1）
- 添加异步任务超时控制（P1）
- 修复 Map 参数校验问题（P1）
- 实现审计日志表（P2）

**生产就绪建议**: 必须修复 2 个 CRITICAL 和 6 个 HIGH 优先级问题后可上线，MEDIUM 和 LOW 问题可在后续迭代中修复。

**安全评分**: 62/100 (中等偏低)

**与其他模块对比**:

| 维度 | Live 模块 | Agent 模块 | Attribution 模块 | 说明 |
|-----|-----------|------------|------------------|------|
| 认证授权 | ⭐⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐ | Attribution 缺少数据所有权校验 |
| 数据隔离 | ⭐⭐⭐⭐⭐ | ⭐⭐ | ⭐⭐ | Attribution session 归属未校验 |
| 输入验证 | ⭐⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐ | Attribution Map 参数未校验 |
| API 限流 | ⭐ | ⭐ | ⭐ | 三者都缺少全局限流 |
| 错误处理 | ⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐ | 三者都有泄露风险 |
| 敏感数据保护 | ⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐ | Attribution 敏感数据未脱敏 |
| 审计日志 | ⭐⭐ | ⭐⭐ | ⭐ | Attribution 缺少审计日志表 |

**Attribution 模块特有风险**:
- 敏感商业数据泄露（CRITICAL）
- AI 调用滥用（HIGH）
- 归因算法安全（HIGH）
- 缺少并发控制（HIGH）

**Attribution 模块优势**:
- 异步处理设计合理
- JPA Specification 防 SQL 注入
- 逻辑删除机制

---

## 14. 安全加固路线图

### 第一阶段：修复 CRITICAL 问题（1 周）

**目标**: 修复数据所有权校验和敏感数据泄露问题

**任务**:
1. 修复 C1：添加 session 归属校验（1 人日）
2. 修复 C2：添加敏感数据访问审计和脱敏（2 人日）
3. 验证修复效果（0.5 人日）

**验收标准**:
- 所有查询/删除操作都校验 session 归属
- 敏感数据访问有审计日志
- 话术内容返回摘要而非完整内容

### 第二阶段：修复 HIGH 问题（2 周）

**目标**: 添加 API 限流、并发控制、超时控制

**任务**:
1. 修复 H1：添加 API 限流保护（1.5 人日）
2. 修复 H2：添加并发控制（1.5 人日）
3. 修复 H3：改进 AI 评分提取（1.5 人日）
4. 修复 H4：添加异步任务超时控制（1 人日）
5. 修复 H5：修复 Map 参数校验问题（1 人日）
6. 修复 H6：添加归因详情查询所有权校验（0.5 人日）
7. 验证修复效果（1 人日）

**验收标准**:
- 归因触发接口有限流保护（每分钟 10 次）
- 同一场次不能重复触发
- AI 评分提取使用结构化输出
- 异步任务有超时控制（5 分钟）
- 所有 API 使用专用 VO 类

### 第三阶段：修复 MEDIUM 问题（2 周）

**目标**: 完善权限注解、审计日志、算法版本管理

**任务**:
1. 修复 M1-M8（6 人日）
2. 验证修复效果（1 人日）

**验收标准**:
- 所有 API 有方法级权限注解
- 敏感操作有审计日志
- 归因记录包含算法版本号
- 归因记录包含进度字段

### 第四阶段：修复 LOW 问题（1 周）

**目标**: 完善输入校验、数据加密、Repository 隔离

**任务**:
1. 修复 L1-L6（5 人日）
2. 验证修复效果（0.5 人日）

**验收标准**:
- 所有输入有枚举值校验
- 日志不包含敏感信息
- 敏感字段加密存储
- Repository 层强制隔离

---

**审计完成日期**: 2026-05-09  
**下次审计建议**: 2026-08-09（3 个月后）  
**相关文档**: 
- `docs/modules/attribution/architecture-review.md` - Attribution 模块架构评审
- `docs/modules/attribution/code-review.md` - Attribution 模块代码评审
- `docs/modules/agent/security-audit.md` - Agent 模块安全审计
- `docs/modules/live/security-audit.md` - Live 模块安全审计
- `docs/adr/002-无数据库外键.md` - 跨模块关联设计
- `docs/adr/003-Specification动态查询.md` - 动态查询安全

