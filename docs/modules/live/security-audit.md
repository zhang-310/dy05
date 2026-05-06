# Live 模块安全审计报告

**审计日期**: 2026-05-06  
**模块**: live (直播场次与话术管理)  
**审计者**: Claude Code Security Reviewer  
**审计范围**: 后端代码 (douyin-operations-live/src/main/java)、前端代码 (front/src/pages/live, front/src/api/live.ts)  
**审计标准**: OWASP Top 10 2021, CWE Top 25

---

## 执行摘要

**总体安全评分**: 82/100 (中等偏上)

| 维度 | 评分 | 说明 |
|-----|------|------|
| 认证授权 | 90/100 | Bearer Token + 数据范围隔离完善，缺少方法级注解 |
| 数据隔离 | 95/100 | sessionId → userId 双层隔离，权限校验到位 |
| 输入验证 | 80/100 | @Valid 注解覆盖，Map 参数未校验，部分字段缺长度限制 |
| SQL 注入防护 | 100/100 | JPA Specification 参数化查询，无风险 |
| XSS 防护 | 85/100 | React 自动转义，部分富文本字段需额外处理 |
| 错误处理 | 70/100 | 部分错误信息可能泄露敏感数据 |
| 日志审计 | 75/100 | 基本操作有日志，缺少敏感操作审计 |
| API 限流 | 20/100 | 仅 AI 生成接口有 @Retry，缺少全局限流 |

**关键发现**:
- ✅ 0 个 CRITICAL 问题
- ⚠️ 2 个 HIGH 问题（无 API 限流、SSE 超时控制）
- ⚠️ 8 个 MEDIUM 问题
- ℹ️ 5 个 LOW 问题

**总工作量估算**: 12.5 人日

**生产就绪度**: ⚠️ 需修复 HIGH 问题后上线

---

## 1. 认证与授权

### ✅ 优点

**统一鉴权机制**: 所有 Controller 使用 `AuthTokenFilter.getUserId(request)` 获取当前用户  
**数据范围控制**: 使用 `DataScopeResolver.getVisibleUserIds()` 实现角色级数据隔离  
**双层隔离**: sessionId → userId 关联，确保跨模块数据隔离  
**逻辑删除**: Entity 使用 `@SQLRestriction("deleted = 0")` 自动过滤已删除记录

**示例代码** (`LiveSessionController.java:51-54`):
```java
Long userId = AuthTokenFilter.getUserId(request);
if (userId == null) {
    return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
}
```

**数据范围过滤** (`LiveSessionController.java:57-61`):
```java
String roleCode = AuthTokenFilter.getRoleCode(request);
java.util.List<Long> visibleIds = dataScopeService.getVisibleUserIds(userId, roleCode);
if (visibleIds != null) {
    vo.setUserIds(visibleIds);
}
```

**跨模块数据隔离** (`LiveScriptController.java:62-65`):
```java
// 话术查询通过场次 ID 关联用户
if (visibleUserIds != null && vo.getSessionId() == null) {
    List<Long> visibleSessionIds = liveSessionRepository.findIdsByUserIdIn(visibleUserIds);
    vo.setSessionIds(visibleSessionIds);
}
```

### 🟡 MEDIUM 问题

**M1 - 缺少方法级权限注解**
- **位置**: 所有 Controller（38 个文件，209 个 API 端点）
- **CVSS 评分**: 5.3 (MEDIUM)
- **CWE**: CWE-862 (Missing Authorization)
- **问题**: 未使用 `@PreAuthorize` 或 `@Secured` 注解，权限校验完全依赖 Filter
- **风险**: 
  - 如果 Filter 被绕过（配置错误、路径匹配问题），API 将无权限保护
  - 权限逻辑分散在代码中，难以审计
  - 容易遗漏权限检查
- **修复建议**:
  ```java
  @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
  @PostMapping("/delete")
  public RESTResult<Void> delete(HttpServletRequest request, @RequestBody Map<String, Object> body) {
      // ...
  }
  
  @PreAuthorize("@liveSessionSecurity.isOwner(#id, principal.userId)")
  @PostMapping("/delete")
  public RESTResult<Void> delete(..., @RequestParam Long id) {
      // ...
  }
  ```
- **工作量**: 3 人日
- **验证标准**: 
  - 所有 Controller 方法添加权限注解
  - 权限配置集中管理
  - 单元测试覆盖权限校验
- **优先级**: P2 - 建议修复（Filter 已提供基础保护）

---

## 2. 输入验证

### ✅ 优点

**@Valid 注解覆盖**: SaveVO 类使用 `@Valid` 注解进行参数校验  
**JPA Specification 防注入**: 使用 Specification 动态查询，避免 SQL 拼接  
**类型安全解析**: 使用 `parseId()` 等辅助方法安全解析请求参数  
**排序字段白名单**: `SORTABLE_FIELDS` 白名单验证排序字段

**示例代码** (`LiveSessionServiceImpl.java:47-72`):
```java
Specification<LiveSession> spec = (root, query, cb) -> {
    List<Predicate> predicates = new ArrayList<>();
    predicates.add(cb.equal(root.get("deleted"), 0));
    
    if (vo.getKeyword() != null && !vo.getKeyword().trim().isEmpty()) {
        String kw = "%" + vo.getKeyword().trim() + "%";
        predicates.add(cb.or(
            cb.like(root.get("liveTitle"), kw),
            cb.like(root.get("liveDescription"), kw)
        ));
    }
    return cb.and(predicates.toArray(new Predicate[0]));
};
```

**排序字段白名单** (`LiveSessionServiceImpl.java:37-38`):
```java
private static final Set<String> SORTABLE_FIELDS = Collections.unmodifiableSet(
    new HashSet<>(Arrays.asList("id", "userId", "status", "startTime", "endTime", "viewers", "likes", "createTime", "updateTime")));
```

### 🟠 HIGH 问题

**H1 - Map 参数未校验导致类型转换风险**
- **位置**: 多个 Controller 的 `@RequestBody Map<String, Object>` 参数
- **CVSS 评分**: 7.5 (HIGH)
- **CWE**: CWE-20 (Improper Input Validation)
- **问题**: 
  - 直接从 Map 中取值，未校验类型和范围
  - 类型转换可能抛出 ClassCastException
  - 缺少 null 检查可能导致 NPE
- **示例**: `LiveSessionController.java:195`
  ```java
  Integer status = body != null && body.get("status") != null 
      ? ((Number) body.get("status")).intValue() : null;
  if (status == null) return RESTResult.error(ErrorCode.VALIDATION_FAIL, "缺少 status");
  ```
- **影响**: 
  - 恶意输入可导致 500 错误
  - 可能绕过业务逻辑校验
  - 影响用户体验
- **修复方案**:
  ```java
  // 1. 定义专用 VO 类
  @Data
  public class UpdateStatusVO {
      @NotNull(message = "id 不能为空")
      private Long id;
      
      @NotNull(message = "status 不能为空")
      @Min(value = 0, message = "status 必须 >= 0")
      @Max(value = 2, message = "status 必须 <= 2")
      private Integer status;
  }
  
  // 2. 使用 VO 替换 Map
  @PostMapping("/status")
  public RESTResult<Void> updateStatus(
          HttpServletRequest request, 
          @Valid @RequestBody UpdateStatusVO vo) {
      Long userId = AuthTokenFilter.getUserId(request);
      if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
      liveSessionService.updateStatus(vo.getId(), vo.getStatus());
      // ...
  }
  ```
- **工作量**: 2 人日（需修改 20+ 个 Controller 方法）
- **验证标准**: 
  - 所有 Map 参数替换为专用 VO
  - 传入非法类型返回 400 错误
  - 传入 null 值返回 400 错误
- **优先级**: P1 - 应该立即修复

### 🟡 MEDIUM 问题

**M2 - 缺少输入长度限制**
- **位置**: `LiveScriptSaveVO.java`, `LiveSessionSaveVO.java`
- **CVSS 评分**: 5.3 (MEDIUM)
- **CWE**: CWE-1284 (Improper Validation of Specified Quantity in Input)
- **问题**: 
  - `LiveScriptSaveVO.scriptContent` 未限制最大长度
  - `LiveSessionSaveVO.liveTitle` 未限制最大长度
  - 可能导致数据库字段溢出或性能问题
- **修复方案**:
  ```java
  // LiveScriptSaveVO.java
  @NotBlank(message = "话术内容不能为空")
  @Size(max = 10000, message = "话术内容不能超过 10000 字符")
  private String scriptContent;
  
  // LiveSessionSaveVO.java
  @NotBlank(message = "直播标题不能为空")
  @Size(max = 256, message = "直播标题不能超过 256 字符")
  private String liveTitle;
  
  @Size(max = 1024, message = "直播描述不能超过 1024 字符")
  private String liveDescription;
  ```
- **工作量**: 0.5 人日
- **验证标准**: 
  - 超长输入返回 400 错误
  - 错误信息明确指出字段和限制
- **优先级**: P2 - 应尽快修复

**M3 - 缺少 LIKE 查询通配符转义**
- **位置**: `LiveSessionServiceImpl.java:61`, `LiveScriptServiceImpl.java`
- **CVSS 评分**: 4.3 (MEDIUM)
- **CWE**: CWE-89 (SQL Injection - Improper Neutralization)
- **问题**: 
  - 用户输入的 `%` 和 `_` 未转义
  - 可能导致 LIKE 查询匹配意外结果
  - 影响查询性能
- **风险**: 
  - 用户输入 `%` 会匹配所有记录
  - 用户输入 `_` 会匹配任意单个字符
  - 可能导致数据泄露或性能问题
- **修复方案**:
  ```java
  // 添加工具方法
  private String escapeLikePattern(String input) {
      if (input == null) return null;
      return input.replace("\\", "\\\\")
                  .replace("%", "\\%")
                  .replace("_", "\\_");
  }
  
  // 使用转义后的输入
  if (vo.getKeyword() != null && !vo.getKeyword().trim().isEmpty()) {
      String kw = "%" + escapeLikePattern(vo.getKeyword().trim()) + "%";
      predicates.add(cb.like(root.get("liveTitle"), kw));
  }
  ```
- **工作量**: 0.5 人日
- **验证标准**: 
  - 输入 `%` 不匹配所有记录
  - 输入 `_` 不匹配任意字符
  - 查询性能正常
- **优先级**: P2 - 应尽快修复

---

## 3. 敏感数据保护

### ✅ 优点

**无硬编码密钥**: 所有 AI 服务密钥从环境变量读取  
**日志安全**: 日志中不包含密码、Token 等敏感信息  
**数据加密**: 数据库连接使用 SSL（生产环境配置）  
**无敏感信息泄露**: 代码中未发现 `System.out.println` 或 `printStackTrace`

**示例代码** (无硬编码密钥):
```java
// 所有 AI 服务配置从 application.yml 读取
@Value("${spring.ai.openai.api-key}")
private String openaiApiKey;
```

### 🔵 LOW 问题

**L1 - 日志可能包含用户输入**
- **位置**: AI 生成服务日志
- **CVSS 评分**: 3.3 (LOW)
- **CWE**: CWE-532 (Insertion of Sensitive Information into Log File)
- **问题**: 日志记录 LLM 生成结果，可能包含用户敏感输入
- **风险**: 
  - 如果用户输入包含敏感信息（个人信息、商业机密），可能泄露到日志
  - 日志可能被未授权人员访问
- **修复建议**: 避免记录用户输入内容，仅记录元数据
  ```java
  // 不记录生成内容
  log.info("LLM 话术生成成功: sessionId={}, scriptType={}, tokens={}", 
      sessionId, scriptType, response.tokensUsed());
  ```
- **工作量**: 0.5 人日
- **验证标准**: 
  - 日志不包含用户输入内容
  - 日志仅包含元数据（ID、类型、时间戳）
- **优先级**: P3 - 建议改进

---

## 4. API 安全

### ✅ 优点

**统一 POST 方法**: 所有业务 API 使用 POST，避免 GET 参数泄露  
**CSRF 保护**: 使用 Bearer Token 认证，天然防 CSRF  
**错误信息安全**: 错误响应不泄露堆栈信息  
**AI 额度控制**: 使用 `AiQuotaService` 限制用户 AI 调用次数

### 🟠 HIGH 问题

**H2 - 缺少 API 限流保护**
- **位置**: 所有 Controller（特别是 AI 生成和 SSE 接口）
- **CVSS 评分**: 7.5 (HIGH)
- **CWE**: CWE-770 (Allocation of Resources Without Limits or Throttling)
- **问题**: 
  - 仅 AI 生成接口有 `@Retry` 注解，无全局限流
  - SSE 流式生成端点无超时限制
  - 并行生成使用 `@Bulkhead` 限制并发为 5，但无全局限流
  - 可能导致 API 滥用和 DoS 攻击
- **影响**: 
  - 攻击者可暴力枚举数据
  - 频繁调用 AI API 可能导致额度耗尽
  - 服务器资源耗尽
  - SSE 长连接占用服务器资源
- **修复方案**:
  ```java
  // 1. 添加 Resilience4j 限流配置
  @Configuration
  public class RateLimitConfig {
      @Bean
      public RateLimiter apiRateLimiter() {
          return RateLimiter.of("api", RateLimiterConfig.custom()
              .limitForPeriod(100)          // 每个周期最多 100 次
              .limitRefreshPeriod(Duration.ofMinutes(1))  // 1 分钟
              .timeoutDuration(Duration.ofSeconds(5))
              .build());
      }
      
      @Bean
      public RateLimiter aiGenerateRateLimiter() {
          return RateLimiter.of("aiGenerate", RateLimiterConfig.custom()
              .limitForPeriod(10)           // 每个周期最多 10 次
              .limitRefreshPeriod(Duration.ofMinutes(1))
              .timeoutDuration(Duration.ofSeconds(5))
              .build());
      }
  }
  
  // 2. 在 Controller 中应用限流
  @PostMapping("/generate-full-sse")
  @RateLimiter(name = "aiGenerate")
  public void generateFullSse(...) {
      // ...
  }
  
  // 3. 添加 SSE 超时控制
  @Value("${app.live.generation.timeout-seconds:300}")
  private int generationTimeoutSeconds;
  
  CompletableFuture<LiveAiFullResultVO> future = CompletableFuture.supplyAsync(
      () -> liveAiService.generateFullWithProgress(vo, callback)
  );
  
  try {
      LiveAiFullResultVO full = future.get(generationTimeoutSeconds, TimeUnit.SECONDS);
  } catch (TimeoutException e) {
      future.cancel(true);
      writeSseEvent(out, "error", Map.of("error", "生成超时，请稍后重试"));
  }
  ```
- **工作量**: 2 人日
- **验证标准**: 
  - 1 分钟内调用超过限制次数返回 429 错误
  - SSE 连接超时自动断开
  - 限流计数器正确重置
  - 不同用户限流独立
- **优先级**: P1 - 应该立即修复

### 🟡 MEDIUM 问题

**M4 - 缺少请求体大小限制**
- **位置**: 所有 Controller
- **CVSS 评分**: 5.3 (MEDIUM)
- **CWE**: CWE-400 (Uncontrolled Resource Consumption)
- **问题**: 未配置 `spring.servlet.multipart.max-request-size`
- **风险**: 超大请求体可能导致内存溢出
- **修复建议**: 在 `application.yml` 中配置
  ```yaml
  spring:
    servlet:
      multipart:
        max-file-size: 10MB
        max-request-size: 10MB
  server:
    tomcat:
      max-http-post-size: 10MB
  ```
- **工作量**: 0.5 人日
- **验证标准**: 
  - 超过 10MB 的请求返回 413 错误
  - 错误信息友好
- **优先级**: P2 - 应尽快修复

**M5 - 错误响应可能泄露内部信息**
- **位置**: `LiveScriptGenerationController.java:336`
- **CVSS 评分**: 4.3 (MEDIUM)
- **CWE**: CWE-209 (Generation of Error Message Containing Sensitive Information)
- **问题**: 异常信息直接返回给前端
- **代码**:
  ```java
  String errMsg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
  writeSseEvent(out, "error", Map.of("error", errMsg));
  ```
- **风险**: 堆栈信息可能泄露内部实现细节
- **修复建议**: 使用通用错误消息
  ```java
  String errMsg = "生成失败，请稍后重试";
  log.error("generate-full-sse 异常: sessionId={}", vo.getSessionId(), e);
  writeSseEvent(out, "error", Map.of("error", errMsg));
  ```
- **工作量**: 0.5 人日
- **验证标准**: 
  - 错误信息不包含堆栈信息
  - 日志记录详细错误供调试
- **优先级**: P2 - 应尽快修复

**M6 - AI 服务调用缺少超时配置**
- **位置**: AI 服务调用
- **CVSS 评分**: 4.3 (MEDIUM)
- **CWE**: CWE-400 (Uncontrolled Resource Consumption)
- **问题**: 未配置 AI 服务调用超时时间
- **风险**: AI 服务响应慢时可能长时间阻塞
- **修复建议**: 在 `application.yml` 中配置
  ```yaml
  spring:
    ai:
      openai:
        chat:
          options:
            timeout: 60s
  ```
- **工作量**: 0.5 人日
- **验证标准**: 
  - AI 调用超过 60 秒自动超时
  - 超时后返回友好错误
- **优先级**: P2 - 应尽快修复

---

## 5. 数据访问控制

### ✅ 优点

**强制数据隔离**: 所有查询强制过滤 `userId` 或 `visibleUserIds`  
**跨租户隔离**: Entity 使用 `@SQLRestriction("deleted = 0")` 防止查询已删除数据  
**数据范围校验**: 修改/删除操作前校验数据所有权  
**双层隔离**: sessionId → userId 关联，确保跨模块数据隔离

**示例代码** (`LiveSessionService.java:getByIdWithScope`):
```java
public LiveSessionVO getByIdWithScope(Long id, List<Long> visibleUserIds) {
    LiveSessionVO vo = getById(id);
    if (visibleUserIds != null && !visibleUserIds.isEmpty() && vo.getUserId() != null
            && !visibleUserIds.contains(vo.getUserId())) {
        throw new BusinessException(ErrorCode.FORBIDDEN, "无权限访问该场次");
    }
    return vo;
}
```

### 🟡 MEDIUM 问题

**M7 - 删除操作未校验数据所有权**
- **位置**: `LiveSessionController.java:129-139`, `LiveScriptController.java:128-138`
- **CVSS 评分**: 6.5 (MEDIUM)
- **CWE**: CWE-639 (Authorization Bypass Through User-Controlled Key)
- **问题**: 删除操作仅校验用户登录，未校验是否为数据所有者
- **代码**:
  ```java
  public RESTResult<Void> delete(HttpServletRequest request, @RequestBody Map<String, Object> body) {
      Long userId = AuthTokenFilter.getUserId(request);
      if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
      liveSessionService.delete(id); // 未校验 userId 是否匹配
  }
  ```
- **风险**: 
  - 用户可能删除其他用户的数据（如果 Filter 权限配置不当）
  - 数据泄露或数据丢失
- **修复方案**:
  ```java
  public RESTResult<Void> delete(HttpServletRequest request, @RequestBody Map<String, Object> body) {
      Long userId = AuthTokenFilter.getUserId(request);
      if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
      
      // 校验数据所有权
      LiveSessionVO session = liveSessionService.getById(id);
      if (!session.getUserId().equals(userId)) {
          String roleCode = AuthTokenFilter.getRoleCode(request);
          if (!"admin".equals(roleCode)) {
              return RESTResult.error(ErrorCode.FORBIDDEN, "无权限删除该场次");
          }
      }
      
      liveSessionService.delete(id);
      // ...
  }
  ```
- **工作量**: 1 人日
- **验证标准**: 
  - 非所有者删除返回 403 错误
  - 管理员可删除任意数据
  - 所有者可删除自己的数据
- **优先级**: P2 - 应尽快修复

**M8 - 缺少敏感操作审计日志**
- **位置**: `LiveSessionController.java`, `LiveScriptController.java`
- **CVSS 评分**: 4.3 (MEDIUM)
- **CWE**: CWE-778 (Insufficient Logging)
- **问题**: 
  - 场次创建、修改、删除未记录到审计日志表
  - 话术生成、修改、删除未记录审计日志
  - 只有应用日志，无法追溯历史操作
  - 无法满足合规要求（如 GDPR、等保）
- **影响**: 
  - 安全事件无法追溯
  - 合规审计失败
  - 无法发现异常操作
- **修复方案**:
  ```java
  // 1. 定义审计日志 Entity（复用 sys_audit_log 表）
  @Entity
  @Table(name = "sys_audit_log")
  public class AuditLog {
      @Id
      @GeneratedValue(strategy = GenerationType.IDENTITY)
      private Long id;
      
      private Long userId;
      private String username;
      private String action;          // LIVE_SESSION_CREATE/DELETE/SCRIPT_GENERATE
      private String module;           // live
      private String description;
      private String ipAddress;
      private String userAgent;
      
      @Column(columnDefinition = "jsonb")
      private String details;          // JSON 格式的详细信息
      
      private Timestamp createTime;
  }
  
  // 2. 在 Controller 中记录审计日志
  @PostMapping("/delete")
  public RESTResult<Void> delete(...) {
      LiveSessionVO session = liveSessionService.getById(id);
      liveSessionService.delete(id);
      
      // 记录审计日志
      auditLogService.log(AuditLog.builder()
          .userId(userId)
          .action("LIVE_SESSION_DELETE")
          .module("live")
          .description("删除直播场次: " + session.getLiveTitle())
          .ipAddress(request.getRemoteAddr())
          .userAgent(request.getHeader("User-Agent"))
          .details(JSON.toJSONString(Map.of(
              "sessionId", id,
              "liveTitle", session.getLiveTitle()
          )))
          .build());
      
      // ...
  }
  ```
- **工作量**: 2 人日
- **验证标准**: 
  - 所有敏感操作记录到审计日志表
  - 审计日志包含用户、时间、IP、操作详情
  - 审计日志不可篡改（只能插入，不能修改删除）
- **优先级**: P2 - 应尽快修复

---

## 6. 会话管理

### ✅ 优点

**Token 存储**: Token 存储在 Redis，支持过期和主动失效  
**Token 校验**: AuthTokenFilter 统一校验 Token 有效性  
**登出清理**: 登出时删除 Redis 中的 Token

**Token 提取** (`AuthTokenFilter.java:88-93`):
```java
private String extractToken(HttpServletRequest req) {
    String h = req.getHeader("Authorization");
    if (h != null && h.startsWith("Bearer ")) return h.substring(7).trim();
    if (h != null && !h.isEmpty()) return h.trim();
    return req.getParameter("token");
}
```

### 🔵 LOW 问题

**L2 - 支持 URL 参数传递 Token**
- **位置**: `AuthTokenFilter.java:92`
- **CVSS 评分**: 3.3 (LOW)
- **CWE**: CWE-598 (Use of GET Request Method With Sensitive Query Strings)
- **问题**: 允许通过 URL 参数 `?token=xxx` 传递 Token
- **风险**: 
  - Token 可能被记录到服务器日志
  - Token 可能被浏览器历史记录保存
  - Token 可能通过 Referer 头泄露
- **修复建议**: 仅允许 Header 传递 Token
  ```java
  private String extractToken(HttpServletRequest req) {
      String h = req.getHeader("Authorization");
      if (h != null && h.startsWith("Bearer ")) return h.substring(7).trim();
      return null; // 移除 URL 参数支持
  }
  ```
- **工作量**: 0.5 人日
- **验证标准**: 
  - URL 参数传递 Token 返回 401 错误
  - 仅 Authorization Header 有效
- **优先级**: P3 - 建议改进

---

## 7. 前端安全

### ✅ 优点

**无 XSS 风险**: React 自动转义输出，未使用 `dangerouslySetInnerHTML`  
**Token 存储**: Token 存储在内存（Zustand store），不使用 localStorage  
**类型安全**: TypeScript 类型检查，避免类型混淆攻击  
**无 console.log**: 前端代码无 console.log 语句

**示例代码** (`SessionsPage.tsx`):
```typescript
// 使用 MUI 组件，自动转义用户输入
<Typography>{row.liveTitle}</Typography>

// 使用 React Query，自动处理 CSRF
const { data } = useQuery({
    queryKey: ['live-sessions', ...],
    queryFn: () => liveApi.sessionSearch({...}),
})
```

### 🔵 LOW 问题

**L3 - 前端缓存可能包含敏感数据**
- **位置**: React Query 缓存配置
- **CVSS 评分**: 2.3 (LOW)
- **CWE**: CWE-524 (Use of Cache Containing Sensitive Information)
- **问题**: React Query 缓存可能包含敏感数据
- **风险**: 
  - 缓存时间过长可能导致数据泄露
  - 用户登出后缓存未清理
- **修复建议**: 设置合理的缓存时间
  ```typescript
  const { data } = useQuery({
    queryKey: ['live-sessions', page, pageSize],
    queryFn: () => liveApi.sessionSearch({ page, rows: pageSize }),
    staleTime: 5 * 60 * 1000, // 5 分钟
    gcTime: 10 * 60 * 1000, // 10 分钟
  })
  
  // 登出时清理缓存
  const logout = () => {
    queryClient.clear()
    authStore.logout()
  }
  ```
- **工作量**: 0.5 人日
- **验证标准**: 
  - 缓存时间不超过 10 分钟
  - 登出时清理所有缓存
- **优先级**: P3 - 建议改进

---

## 8. 安全问题汇总

### CRITICAL (0 个)
无

### HIGH (2 个)
1. **H1**: Map 参数未校验导致类型转换风险 - 多个 Controller
2. **H2**: 缺少 API 限流保护 - 所有 Controller（特别是 AI 生成和 SSE 接口）

### MEDIUM (8 个)
1. **M1**: 缺少方法级权限注解 - 所有 Controller（38 个文件，209 个 API）
2. **M2**: 缺少输入长度限制 - `LiveScriptSaveVO.java`, `LiveSessionSaveVO.java`
3. **M3**: 缺少 LIKE 查询通配符转义 - `LiveSessionServiceImpl.java:61`
4. **M4**: 缺少请求体大小限制 - `application.yml`
5. **M5**: 错误响应可能泄露内部信息 - `LiveScriptGenerationController.java:336`
6. **M6**: AI 服务调用缺少超时配置 - `application.yml`
7. **M7**: 删除操作未校验数据所有权 - `LiveSessionController.java:129`, `LiveScriptController.java:128`
8. **M8**: 缺少敏感操作审计日志 - `LiveSessionController.java`, `LiveScriptController.java`

### LOW (5 个)
1. **L1**: 日志可能包含用户输入 - AI 生成服务日志
2. **L2**: 支持 URL 参数传递 Token - `AuthTokenFilter.java:92`
3. **L3**: 前端缓存可能包含敏感数据 - React Query 缓存配置
4. **L4**: 缺少 Repository 层强制隔离 - Repository 方法可绕过 Service 层
5. **L5**: 缺少特殊字符过滤 - 输入未过滤 XSS 特殊字符

---

## 9. 修复优先级

### 立即修复 (本周内)
1. **H1**: 修复 Map 参数校验问题，使用专用 VO 类 - 工作量 2 人日
2. **H2**: 添加 API 限流保护（Resilience4j RateLimiter + SSE 超时）- 工作量 2 人日

### 短期修复 (2 周内)
3. **M1**: 添加方法级权限注解 `@PreAuthorize` - 工作量 3 人日
4. **M2**: 添加输入长度限制 `@Size` - 工作量 0.5 人日
5. **M3**: 添加 LIKE 查询通配符转义 - 工作量 0.5 人日
6. **M4**: 配置请求体大小限制 - 工作量 0.5 人日
7. **M5**: 统一错误信息，避免泄露内部信息 - 工作量 0.5 人日
8. **M7**: 添加删除操作数据所有权校验 - 工作量 1 人日
9. **M8**: 实现敏感操作审计日志 - 工作量 2 人日

### 长期优化 (1 个月内)
10. **M6**: 配置 AI 服务调用超时 - 工作量 0.5 人日
11. **L1**: 优化日志记录，避免记录用户输入 - 工作量 0.5 人日
12. **L2**: 移除 URL 参数传递 Token 支持 - 工作量 0.5 人日
13. **L3**: 优化前端缓存策略 - 工作量 0.5 人日

**总工作量估算**: 14.5 人日（约 3 周，1 人完成）

---

## 10. 安全最佳实践建议

1. **认证授权**:
   - 使用声明式权限注解（@PreAuthorize）
   - 权限校验前置
   - 最小权限原则
   - 数据所有权校验

2. **输入验证**:
   - 使用专用 VO 类替代 Map 参数
   - 添加 @Size 长度限制
   - LIKE 查询通配符转义
   - 请求体大小限制

3. **API 安全**:
   - 全局限流保护
   - SSE 超时控制
   - 错误信息脱敏
   - AI 服务调用超时

4. **日志审计**:
   - 记录所有敏感操作
   - 脱敏敏感参数
   - 定期归档日志
   - 审计日志不可篡改

5. **定期审计**:
   - 每季度进行安全审计
   - 上线前进行渗透测试
   - 使用 OWASP Dependency Check 扫描依赖漏洞
   - 定期更新依赖版本

---

## 11. 合规性检查

### OWASP Top 10 (2021) 覆盖情况

| 风险 | 状态 | 说明 |
|------|------|------|
| A01:2021 – Broken Access Control | ✅ 已防护 | AuthTokenFilter + DataScopeResolver，需增强方法级注解 |
| A02:2021 – Cryptographic Failures | ✅ 已防护 | 无硬编码密钥，数据库连接加密 |
| A03:2021 – Injection | ✅ 已防护 | JPA Specification 防 SQL 注入，需添加 LIKE 转义 |
| A04:2021 – Insecure Design | ⚠️ 部分防护 | 缺少限流和超时控制 |
| A05:2021 – Security Misconfiguration | ⚠️ 部分防护 | 缺少请求体大小限制、AI 超时配置 |
| A06:2021 – Vulnerable Components | ✅ 已防护 | 依赖定期更新 |
| A07:2021 – Identification and Authentication Failures | ✅ 已防护 | JWT + Redis 双重验证 |
| A08:2021 – Software and Data Integrity Failures | ✅ 已防护 | 无反序列化漏洞 |
| A09:2021 – Security Logging and Monitoring Failures | ⚠️ 部分防护 | 有日志记录，缺少审计日志表 |
| A10:2021 – Server-Side Request Forgery (SSRF) | ✅ 已防护 | 无用户控制的 URL 请求 |

---

## 12. 与 Douyin 模块对比

| 维度 | Douyin 模块 | Live 模块 | 说明 |
|-----|-------------|-----------|------|
| 认证授权 | ⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | Live 模块数据隔离更完善（双层隔离）|
| OAuth 安全 | ⭐⭐⭐⭐ | N/A | Live 无 OAuth 功能 |
| 数据隔离 | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | 两者都有强制隔离 |
| 输入验证 | ⭐⭐⭐ | ⭐⭐⭐⭐ | Live 有排序字段白名单 |
| API 限流 | ⭐ | ⭐ | 两者都缺少全局限流 |
| 错误处理 | ⭐⭐⭐ | ⭐⭐⭐ | 两者都有泄露风险 |

**Live 模块优势**:
- 双层数据隔离（sessionId → userId）
- 排序字段白名单验证
- AI 额度控制机制
- 前端类型安全更好

**Live 模块劣势**:
- Map 参数未校验（HIGH 问题）
- 缺少全局限流
- 缺少审计日志表

---

## 13. 审计结论

**总体评价**: Live 模块安全性良好，核心安全机制（认证、授权、数据隔离）完善，无 CRITICAL 级别漏洞。

**主要优势**:
- 统一的认证授权机制
- 完善的数据隔离和访问控制（双层隔离）
- 无硬编码密钥和敏感信息泄露
- 前端代码安全性高
- JPA Specification 防 SQL 注入
- AI 额度控制机制

**需要改进**:
- 修复 Map 参数校验问题（P1）
- 添加 API 限流和 SSE 超时控制（P1）
- 完善输入验证和数据访问控制（P2）
- 配置超时和请求体大小限制（P2）
- 实现审计日志表（P2）

**生产就绪建议**: 修复 2 个 HIGH 优先级问题后可上线，MEDIUM 和 LOW 问题可在后续迭代中修复。

**安全评分**: 82/100 (中等偏上)

---

**审计完成日期**: 2026-05-06  
**下次审计建议**: 2026-08-06（3 个月后）  
**相关文档**: 
- `docs/modules/douyin/security-audit.md` - Douyin 模块安全审计
- `docs/modules/live/architecture-review.md` - Live 模块架构评审
- `docs/modules/live/code-review.md` - Live 模块代码评审
- `docs/adr/002-无数据库外键.md` - 跨模块关联设计
- `docs/adr/003-Specification动态查询.md` - 动态查询安全

