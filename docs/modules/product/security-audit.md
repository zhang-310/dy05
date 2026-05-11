# Product 模块安全审计报告

**审计日期**: 2026-05-08  
**模块**: product (商品与话术管理)  
**审计者**: Claude Code Security Reviewer  
**审计范围**: 后端代码 (douyin-operations-live/src/main/java/.../module/product/)  
**审计标准**: OWASP Top 10 2021, CWE Top 25, CVSS 3.1

---

## 执行摘要

**总体安全评分**: 75/100 (中等)

| 维度 | 评分 | 说明 |
|-----|------|------|
| 认证授权 | 70/100 | Bearer Token 认证，缺少方法级注解和数据所有权校验 |
| 数据隔离 | 60/100 | 部分接口缺少 userId 过滤，存在 IDOR 风险 |
| 输入验证 | 75/100 | @Valid 注解覆盖，Map 参数未校验，缺少长度限制 |
| SQL 注入防护 | 100/100 | JPA Specification 参数化查询，无风险 |
| XSS 防护 | 85/100 | 后端无 XSS 风险，前端需注意富文本字段 |
| 错误处理 | 65/100 | 错误信息可能泄露内部细节 |
| 日志审计 | 60/100 | 缺少敏感操作审计日志 |
| API 限流 | 10/100 | 无限流保护，AI 生成接口存在滥用风险 |

**关键发现**:
- 🔴 1 个 CRITICAL 问题（数据所有权校验缺失）
- ⚠️ 5 个 HIGH 问题（Map 参数未校验、无 API 限流、SSE 无超时、批量操作无权限校验、ML 模型训练无限流）
- ⚠️ 9 个 MEDIUM 问题
- ℹ️ 6 个 LOW 问题

**总工作量估算**: 18.5 人日

**生产就绪度**: 🔴 必须修复 CRITICAL 和 HIGH 问题后上线

---

## 1. 认证与授权

### ✅ 优点

**统一鉴权机制**: 所有 Controller 使用 `AuthTokenFilter.getUserId(request)` 获取当前用户  
**@CurrentUserId 注解**: 部分 Controller 使用 `@CurrentUserId` 注解简化用户 ID 获取  
**逻辑删除**: Entity 使用 `@SQLRestriction("deleted = 0")` 自动过滤已删除记录  
**乐观锁**: DyProduct 使用 `@Version` 防止并发库存更新

**示例代码** (`ProductScriptVersionController.java:45`):
```java
@PostMapping("/save")
public RESTResult<ProductScriptVersionVO> save(
        @Valid @RequestBody ProductScriptVersionSaveVO vo,
        @CurrentUserId Long userId) {
    ProductScriptVersionVO result = versionService.save(vo, userId);
    return RESTResult.getSuccess(result);
}
```

### 🔴 CRITICAL 问题

**C1 - 删除/修改操作缺少数据所有权校验**
- **位置**: 
  - `ProductController.java:164-174` (delete)
  - `ProductController.java:176-194` (batchDelete)
  - `ProductScriptController.java:291-305` (deleteScript)
  - `StylePresetController.java:87-101` (delete)
- **CVSS 评分**: 9.1 (CRITICAL)
- **CWE**: CWE-639 (Authorization Bypass Through User-Controlled Key)
- **问题**: 
  - 删除操作仅校验用户登录，未校验是否为数据所有者
  - 用户可通过修改 ID 删除其他用户的商品/话术
  - 批量删除未校验每个 ID 的所有权
- **代码示例**:
  ```java
  // ProductController.java:164-174
  @PostMapping("/delete")
  public RESTResult<Void> delete(HttpServletRequest request,
          @RequestBody(required = false) java.util.Map<String, Object> body) {
      Long id = parseLong(body, "id");
      if (id == null) return RESTResult.error(ErrorCode.VALIDATION_FAIL, "缺少 id");
      if (AuthTokenFilter.getUserId(request) == null) 
          return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
      productService.delete(id); // ❌ 未校验 userId 是否匹配
      // ...
  }
  ```
- **利用场景**:
  1. 攻击者登录后获取自己的商品 ID（如 100）
  2. 遍历 ID 101-200，调用 `/api/v1/product/delete` 删除其他用户商品
  3. 批量删除时传入 `{"ids": [1,2,3,...,1000]}`，删除所有用户商品
- **影响**: 
  - 数据丢失（逻辑删除可恢复，但影响业务）
  - 横向越权攻击
  - 违反数据隔离原则
- **修复方案**:
  ```java
  // 1. Service 层添加所有权校验
  @Transactional(rollbackFor = Exception.class)
  public void delete(Long id, Long userId) {
      DyProduct entity = dyProductRepository.findByIdAndDeleted(id, 0)
              .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "商品不存在"));
      
      // 校验数据所有权
      if (!entity.getUserId().equals(userId)) {
          throw new BusinessException(ErrorCode.FORBIDDEN, "无权限删除该商品");
      }
      
      entity.setDeleted(1);
      dyProductRepository.save(entity);
  }
  
  // 2. Controller 传递 userId
  @PostMapping("/delete")
  public RESTResult<Void> delete(HttpServletRequest request,
          @RequestBody(required = false) java.util.Map<String, Object> body) {
      Long userId = AuthTokenFilter.getUserId(request);
      if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
      Long id = parseLong(body, "id");
      if (id == null) return RESTResult.error(ErrorCode.VALIDATION_FAIL, "缺少 id");
      productService.delete(id, userId); // ✅ 传递 userId
      // ...
  }
  
  // 3. 批量删除逐个校验
  @Transactional(rollbackFor = Exception.class)
  public void batchDelete(List<Long> ids, Long userId) {
      if (ids == null || ids.isEmpty()) {
          throw new BusinessException(ErrorCode.VALIDATION_FAIL, "商品 ID 列表不能为空");
      }
      List<DyProduct> entities = dyProductRepository.findAllById(ids);
      for (DyProduct entity : entities) {
          // 校验所有权
          if (entity.getDeleted() == 0 && entity.getUserId().equals(userId)) {
              entity.setDeleted(1);
          }
      }
      dyProductRepository.saveAll(entities);
  }
  ```
- **工作量**: 2 人日（需修改 10+ 个 Service 方法）
- **验证标准**: 
  - 非所有者删除返回 403 错误
  - 批量删除仅删除自己的数据
  - 管理员可删除任意数据（需增加角色判断）
- **优先级**: P0 - 必须立即修复

### 🟠 HIGH 问题

**H1 - Map 参数未校验导致类型转换风险**
- **位置**: 多个 Controller 的 `@RequestBody Map<String, Object>` 参数
  - `ProductScriptController.java:62-71` (listScripts)
  - `ProductScriptController.java:84-95` (listScriptsByType)
  - `ProductController.java:59-66` (get)
  - `StylePresetController.java:59-71` (getById)
- **CVSS 评分**: 7.5 (HIGH)
- **CWE**: CWE-20 (Improper Input Validation)
- **问题**: 
  - 直接从 Map 中取值，未校验类型和范围
  - 类型转换可能抛出 ClassCastException
  - 缺少 null 检查可能导致 NPE
- **示例**: `ProductScriptController.java:65`
  ```java
  Long productId = body != null && body.get("productId") != null 
      ? ((Number) body.get("productId")).longValue() : null;
  if (productId == null) return RESTResult.error(ErrorCode.VALIDATION_FAIL, "缺少 productId");
  ```
- **影响**: 
  - 恶意输入可导致 500 错误
  - 可能绕过业务逻辑校验
  - 影响用户体验
- **修复方案**: 使用专用 VO 类替换 Map 参数（参考 live 模块审计报告 H1）
- **工作量**: 2 人日（需修改 30+ 个 Controller 方法）
- **优先级**: P1 - 应该立即修复

**H2 - 缺少 API 限流保护**
- **位置**: 所有 Controller（特别是 AI 生成和 SSE 接口）
  - `ProductScriptController.java:129-141` (generateMultiStyle)
  - `ProductScriptController.java:143-190` (generateMultiStyleSse)
  - `ProductScriptController.java:201-242` (generateBatchStream)
  - `StyleRecommendationController.java:87-105` (trainModel)
- **CVSS 评分**: 8.2 (HIGH)
- **CWE**: CWE-770 (Allocation of Resources Without Limits or Throttling)
- **问题**: 
  - 无全局限流保护
  - AI 生成接口无调用频率限制
  - SSE 流式生成端点无超时限制（600 秒）
  - ML 模型训练无限流，可能导致资源耗尽
- **影响**: 
  - 攻击者可暴力调用 AI API 导致额度耗尽
  - 频繁训练 ML 模型可能导致 CPU/内存耗尽
  - SSE 长连接占用服务器资源
  - 服务器资源耗尽导致 DoS
- **修复方案**: 添加 Resilience4j 限流（参考 live 模块审计报告 H2）
  ```java
  // 1. AI 生成接口限流
  @PostMapping("/generate-multi-style")
  @RateLimiter(name = "aiGenerate", fallbackMethod = "generateFallback")
  public RESTResult<MultiStyleGenerateResultVO> generateMultiStyle(...) {
      // ...
  }
  
  // 2. ML 训练接口限流（每用户每小时 1 次）
  @PostMapping("/train")
  @RateLimiter(name = "mlTrain", fallbackMethod = "trainFallback")
  public RESTResult<TrainResponse> trainModel(...) {
      // ...
  }
  
  // 3. 配置限流策略
  resilience4j:
    ratelimiter:
      instances:
        aiGenerate:
          limitForPeriod: 10
          limitRefreshPeriod: 1m
          timeoutDuration: 5s
        mlTrain:
          limitForPeriod: 1
          limitRefreshPeriod: 1h
          timeoutDuration: 5s
  ```
- **工作量**: 2.5 人日
- **优先级**: P1 - 应该立即修复

**H3 - SSE 流式接口无超时控制**
- **位置**: 
  - `ProductScriptController.java:143-190` (generateMultiStyleSse)
  - `ProductScriptController.java:201-242` (generateBatchStream)
- **CVSS 评分**: 7.5 (HIGH)
- **CWE**: CWE-400 (Uncontrolled Resource Consumption)
- **问题**: 
  - SSE Emitter 超时设置为 600 秒（10 分钟），但无实际超时控制
  - 长时间运行的 AI 生成任务可能阻塞线程
  - 客户端断开连接后服务端可能继续执行
- **代码示例**:
  ```java
  SseEmitter emitter = new SseEmitter(600_000L); // 10 分钟
  productScriptService.generateMultiStyleScriptsWithProgress(vo, userId, emitter);
  return emitter; // ❌ 无超时控制
  ```
- **修复方案**:
  ```java
  SseEmitter emitter = new SseEmitter(600_000L);
  CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
      productScriptService.generateMultiStyleScriptsWithProgress(vo, userId, emitter);
  });
  
  // 超时控制
  try {
      future.get(600, TimeUnit.SECONDS);
  } catch (TimeoutException e) {
      future.cancel(true);
      emitter.send(SseEmitter.event().name("error")
          .data(Map.of("error", "生成超时，请稍后重试")));
      emitter.complete();
  }
  ```
- **工作量**: 1 人日
- **优先级**: P1 - 应该立即修复

**H4 - 批量操作无权限校验**
- **位置**: 
  - `ProductController.java:176-194` (batchDelete)
  - `ProductScriptController.java:201-242` (generateBatchStream)
- **CVSS 评分**: 7.5 (HIGH)
- **CWE**: CWE-862 (Missing Authorization)
- **问题**: 
  - 批量删除未校验每个 ID 的所有权
  - 批量生成未校验每个 productId 的所有权
  - 可能导致横向越权攻击
- **修复方案**: 在 Service 层逐个校验所有权（见 C1 修复方案）
- **工作量**: 1.5 人日
- **优先级**: P1 - 应该立即修复

**H5 - ML 模型训练无资源限制**
- **位置**: 
  - `StyleRecommendationController.java:87-105` (trainModel)
  - `StyleRecommendationController.java:112-130` (trainModelAsync)
- **CVSS 评分**: 7.5 (HIGH)
- **CWE**: CWE-770 (Allocation of Resources Without Limits or Throttling)
- **问题**: 
  - ML 模型训练无频率限制
  - 异步训练无并发控制
  - 可能导致 CPU/内存耗尽
- **修复方案**:
  ```java
  // 1. 添加限流注解
  @PostMapping("/train")
  @RateLimiter(name = "mlTrain")
  public RESTResult<TrainResponse> trainModel(...) {
      // ...
  }
  
  // 2. 异步训练使用线程池
  @Async("mlTrainExecutor")
  public CompletableFuture<Long> trainModelAsync(Long userId) {
      // ...
  }
  
  // 3. 配置线程池
  @Bean("mlTrainExecutor")
  public Executor mlTrainExecutor() {
      ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
      executor.setCorePoolSize(2);
      executor.setMaxPoolSize(5);
      executor.setQueueCapacity(10);
      executor.setThreadNamePrefix("ml-train-");
      executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
      executor.initialize();
      return executor;
  }
  ```
- **工作量**: 1.5 人日
- **优先级**: P1 - 应该立即修复

### 🟡 MEDIUM 问题

**M1 - 缺少方法级权限注解**
- **位置**: 所有 Controller（8 个文件，80+ 个 API 端点）
- **CVSS 评分**: 5.3 (MEDIUM)
- **CWE**: CWE-862 (Missing Authorization)
- **问题**: 未使用 `@PreAuthorize` 或 `@Secured` 注解，权限校验完全依赖 Filter
- **修复建议**: 添加方法级权限注解（参考 live 模块审计报告 M1）
- **工作量**: 3 人日
- **优先级**: P2 - 建议修复

**M2 - 缺少输入长度限制**
- **位置**: 
  - `ProductScriptSaveVO.java:19` (scriptContent)
  - `ProductSaveVO.java:14` (productName)
  - `ProductSaveVO.java:16` (description)
- **CVSS 评分**: 5.3 (MEDIUM)
- **CWE**: CWE-1284 (Improper Validation of Specified Quantity in Input)
- **问题**: 
  - `scriptContent` 未限制最大长度
  - `description` 未限制最大长度
  - 可能导致数据库字段溢出或性能问题
- **修复方案**:
  ```java
  // ProductScriptSaveVO.java
  @NotBlank(message = "话术内容不能为空")
  @Size(max = 10000, message = "话术内容不能超过 10000 字符")
  private String scriptContent;
  
  // ProductSaveVO.java
  @NotBlank(message = "商品名称不能为空")
  @Size(max = 256, message = "商品名称不能超过 256 字符")
  private String productName;
  
  @Size(max = 5000, message = "商品描述不能超过 5000 字符")
  private String description;
  ```
- **工作量**: 0.5 人日
- **优先级**: P2 - 应尽快修复

**M3 - 缺少 LIKE 查询通配符转义**
- **位置**: `ProductServiceImpl.java:52-57`
- **CVSS 评分**: 4.3 (MEDIUM)
- **CWE**: CWE-89 (SQL Injection - Improper Neutralization)
- **问题**: 用户输入的 `%` 和 `_` 未转义
- **修复方案**: 添加转义方法（参考 live 模块审计报告 M3）
- **工作量**: 0.5 人日
- **优先级**: P2 - 应尽快修复

**M4 - 错误响应可能泄露内部信息**
- **位置**: 
  - `ProductScriptController.java:338-340` (generate)
  - `ProductController.java:109-111` (extractFromLink)
  - `StyleRecommendationController.java:103` (trainModel)
- **CVSS 评分**: 4.3 (MEDIUM)
- **CWE**: CWE-209 (Generation of Error Message Containing Sensitive Information)
- **问题**: 异常信息直接返回给前端
- **代码示例**:
  ```java
  } catch (Exception e) {
      return RESTResult.error(ErrorCode.INTERNAL_ERROR, "生成失败: " + e.getMessage());
  }
  ```
- **修复建议**: 使用通用错误消息，详细错误记录到日志
  ```java
  } catch (Exception e) {
      log.error("话术生成失败: productId={}", productId, e);
      return RESTResult.error(ErrorCode.INTERNAL_ERROR, "生成失败，请稍后重试");
  }
  ```
- **工作量**: 0.5 人日
- **优先级**: P2 - 应尽快修复

**M5 - 缺少请求体大小限制**
- **位置**: 所有 Controller
- **CVSS 评分**: 5.3 (MEDIUM)
- **CWE**: CWE-400 (Uncontrolled Resource Consumption)
- **问题**: 未配置 `spring.servlet.multipart.max-request-size`
- **修复建议**: 在 `application.yml` 中配置（参考 live 模块审计报告 M4）
- **工作量**: 0.5 人日
- **优先级**: P2 - 应尽快修复

**M6 - 文件上传缺少安全校验**
- **位置**: `ProductController.java:114-150` (importPaiping)
- **CVSS 评分**: 6.5 (MEDIUM)
- **CWE**: CWE-434 (Unrestricted Upload of File with Dangerous Type)
- **问题**: 
  - 仅校验文件扩展名（.xlsx/.xls），未校验文件内容
  - 未限制文件大小
  - 未校验文件 MIME 类型
  - 可能上传恶意文件
- **修复方案**:
  ```java
  @PostMapping("/import-paiping")
  public RESTResult<Map<String, Object>> importPaiping(
          HttpServletRequest request,
          @RequestParam("file") MultipartFile file) {
      Long userId = AuthTokenFilter.getUserId(request);
      if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
      
      // 1. 校验文件大小（10MB）
      if (file.getSize() > 10 * 1024 * 1024) {
          return RESTResult.error(ErrorCode.VALIDATION_FAIL, "文件大小不能超过 10MB");
      }
      
      // 2. 校验 MIME 类型
      String contentType = file.getContentType();
      if (contentType == null || 
          (!contentType.equals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet") &&
           !contentType.equals("application/vnd.ms-excel"))) {
          return RESTResult.error(ErrorCode.VALIDATION_FAIL, "仅支持 Excel 格式");
      }
      
      // 3. 校验文件扩展名
      String name = file.getOriginalFilename();
      if (name == null || (!name.endsWith(".xlsx") && !name.endsWith(".xls"))) {
          return RESTResult.error(ErrorCode.VALIDATION_FAIL, "仅支持 .xlsx / .xls 格式");
      }
      
      // 4. 使用 try-with-resources 确保流关闭
      try (InputStream is = file.getInputStream()) {
          List<ProductSaveVO> list = paipingImportService.parseFromExcel(is, userId);
          // ...
      } catch (Exception e) {
          log.error("导入排品表失败: userId={}", userId, e);
          return RESTResult.error(ErrorCode.VALIDATION_FAIL, "解析失败，请检查文件格式");
      }
  }
  ```
- **工作量**: 1 人日
- **优先级**: P2 - 应尽快修复

**M7 - 缺少敏感操作审计日志**
- **位置**: 所有 Controller
- **CVSS 评分**: 4.3 (MEDIUM)
- **CWE**: CWE-778 (Insufficient Logging)
- **问题**: 
  - 商品创建、修改、删除未记录到审计日志表
  - 话术生成、修改、删除未记录审计日志
  - ML 模型训练未记录审计日志
  - 无法满足合规要求（如 GDPR、等保）
- **修复建议**: 实现审计日志表（参考 live 模块审计报告 M8）
- **工作量**: 2 人日
- **优先级**: P2 - 应尽快修复

**M8 - 数据范围控制不完善**
- **位置**: 
  - `ProductController.java:42-54` (search)
  - `ProductController.java:253-264` (searchSales)
- **CVSS 评分**: 5.3 (MEDIUM)
- **CWE**: CWE-639 (Authorization Bypass Through User-Controlled Key)
- **问题**: 
  - 使用 `DataScopeResolver.getVisibleUserIds()` 获取可见用户列表
  - 但部分接口（get/delete/update）未使用数据范围控制
  - 可能导致横向越权
- **修复建议**: 所有查询接口统一使用数据范围控制
- **工作量**: 1 人日
- **优先级**: P2 - 应尽快修复

**M9 - AI 服务调用缺少超时配置**
- **位置**: AI 服务调用
- **CVSS 评分**: 4.3 (MEDIUM)
- **CWE**: CWE-400 (Uncontrolled Resource Consumption)
- **问题**: 未配置 AI 服务调用超时时间
- **修复建议**: 在 `application.yml` 中配置（参考 live 模块审计报告 M6）
- **工作量**: 0.5 人日
- **优先级**: P2 - 应尽快修复

---

## 2. 输入验证

### ✅ 优点

**@Valid 注解覆盖**: SaveVO 类使用 `@Valid` 注解进行参数校验  
**JPA Specification 防注入**: 使用 Specification 动态查询，避免 SQL 拼接  
**排序字段白名单**: `SORTABLE` 白名单验证排序字段  
**乐观锁**: DyProduct 使用 `@Version` 防止并发更新

**示例代码** (`ProductServiceImpl.java:25-26`):
```java
private static final Set<String> SORTABLE = Set.of(
    "id", "userId", "productName", "price", "inventory", "status", "featured", "createTime");
```

### 🔵 LOW 问题

**L1 - 缺少数值范围校验**
- **位置**: 
  - `ProductSaveVO.java:19` (price)
  - `ProductSaveVO.java:21` (inventory)
- **CVSS 评分**: 3.3 (LOW)
- **CWE**: CWE-1284 (Improper Validation of Specified Quantity in Input)
- **问题**: 
  - `price` 未限制最小值（可能为负数）
  - `inventory` 未限制范围
- **修复建议**:
  ```java
  @NotNull(message = "售价不能为空")
  @DecimalMin(value = "0.01", message = "售价必须大于 0")
  @DecimalMax(value = "999999.99", message = "售价不能超过 999999.99")
  private BigDecimal price;
  
  @Min(value = 0, message = "库存不能为负数")
  @Max(value = 999999999, message = "库存不能超过 999999999")
  private Long inventory;
  ```
- **工作量**: 0.5 人日
- **优先级**: P3 - 建议改进

**L2 - 缺少枚举值校验**
- **位置**: 
  - `ProductScriptSaveVO.java:17` (scriptType)
  - `ProductScriptSaveVO.java:24` (style)
  - `ProductScriptSaveVO.java:36` (scene)
- **CVSS 评分**: 3.3 (LOW)
- **CWE**: CWE-20 (Improper Input Validation)
- **问题**: 
  - `scriptType` 未限制可选值
  - `style` 未限制可选值
  - `scene` 未限制可选值
- **修复建议**:
  ```java
  @NotNull(message = "话术类型不能为空")
  @Pattern(regexp = "^(formal|casual|humorous|premium)$", 
           message = "话术类型必须为 formal/casual/humorous/premium")
  private String scriptType;
  
  @Pattern(regexp = "^(专业|亲和|幽默|高端|简洁)$", 
           message = "风格必须为 专业/亲和/幽默/高端/简洁")
  private String style;
  ```
- **工作量**: 0.5 人日
- **优先级**: P3 - 建议改进

**L3 - 日志可能包含用户输入**
- **位置**: AI 生成服务日志
- **CVSS 评分**: 3.3 (LOW)
- **CWE**: CWE-532 (Insertion of Sensitive Information into Log File)
- **问题**: 日志记录 LLM 生成结果，可能包含用户敏感输入
- **修复建议**: 避免记录用户输入内容，仅记录元数据（参考 live 模块审计报告 L1）
- **工作量**: 0.5 人日
- **优先级**: P3 - 建议改进

---

## 3. 数据访问控制

### ✅ 优点

**逻辑删除**: Entity 使用 `@SQLRestriction("deleted = 0")` 防止查询已删除数据  
**乐观锁**: DyProduct 使用 `@Version` 防止并发库存更新  
**数据范围控制**: 部分接口使用 `DataScopeResolver.getVisibleUserIds()` 实现角色级数据隔离

**示例代码** (`DyProduct.java:17-18`):
```java
@Entity
@Table(name = "dy_product")
@SQLRestriction("deleted = 0")
public class DyProduct {
    // ...
}
```

### 🔵 LOW 问题

**L4 - Repository 层缺少强制隔离**
- **位置**: 所有 Repository
- **CVSS 评分**: 3.3 (LOW)
- **CWE**: CWE-862 (Missing Authorization)
- **问题**: Repository 方法可绕过 Service 层直接查询所有数据
- **修复建议**: 使用 JPA `@Where` 注解或自定义 Repository 基类强制过滤
- **工作量**: 1 人日
- **优先级**: P3 - 建议改进

**L5 - 缺少并发控制**
- **位置**: 
  - `ProductServiceImpl.java:146-159` (updateInventory)
- **CVSS 评分**: 3.3 (LOW)
- **CWE**: CWE-362 (Concurrent Execution using Shared Resource with Improper Synchronization)
- **问题**: 
  - 虽然使用了 `@Version` 乐观锁
  - 但异常处理仅提示重试，未自动重试
- **修复建议**:
  ```java
  @Transactional(rollbackFor = Exception.class)
  public void updateInventory(Long id, Long quantity) {
      int maxRetries = 3;
      for (int i = 0; i < maxRetries; i++) {
          try {
              DyProduct entity = dyProductRepository.findByIdAndDeleted(id, 0)
                  .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "商品不存在"));
              long newInventory = (entity.getInventory() != null ? entity.getInventory() : 0) + quantity;
              if (newInventory < 0) {
                  throw new BusinessException(ErrorCode.VALIDATION_FAIL, "库存不足");
              }
              entity.setInventory(newInventory);
              dyProductRepository.save(entity);
              return; // 成功
          } catch (OptimisticLockException e) {
              if (i == maxRetries - 1) {
                  throw new BusinessException(ErrorCode.INVENTORY_CONFLICT, 
                      "库存更新失败，请稍后重试");
              }
              // 重试前等待随机时间
              Thread.sleep(50 + new Random().nextInt(100));
          }
      }
  }
  ```
- **工作量**: 0.5 人日
- **优先级**: P3 - 建议改进

**L6 - 缺少级联删除保护**
- **位置**: `ProductServiceImpl.java:124-129` (delete)
- **CVSS 评分**: 3.3 (LOW)
- **CWE**: CWE-404 (Improper Resource Shutdown or Release)
- **问题**: 
  - 删除商品时未检查是否有关联的话术
  - 可能导致孤儿数据
- **修复建议**:
  ```java
  @Transactional(rollbackFor = Exception.class)
  public void delete(Long id, Long userId) {
      DyProduct entity = dyProductRepository.findByIdAndDeleted(id, 0)
          .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "商品不存在"));
      
      // 校验所有权
      if (!entity.getUserId().equals(userId)) {
          throw new BusinessException(ErrorCode.FORBIDDEN, "无权限删除该商品");
      }
      
      // 检查是否有关联的话术
      long scriptCount = dyProductScriptRepository.countByProductIdAndDeleted(id, 0);
      if (scriptCount > 0) {
          throw new BusinessException(ErrorCode.VALIDATION_FAIL, 
              "该商品有 " + scriptCount + " 条关联话术，请先删除话术");
      }
      
      entity.setDeleted(1);
      dyProductRepository.save(entity);
  }
  ```
- **工作量**: 0.5 人日
- **优先级**: P3 - 建议改进

---

## 4. API 安全

### ✅ 优点

**统一 POST 方法**: 大部分业务 API 使用 POST，避免 GET 参数泄露  
**CSRF 保护**: 使用 Bearer Token 认证，天然防 CSRF  
**错误信息安全**: 大部分错误响应不泄露堆栈信息

### 已识别问题

- **H2**: 缺少 API 限流保护（已在第 1 节描述）
- **H3**: SSE 流式接口无超时控制（已在第 1 节描述）
- **M4**: 错误响应可能泄露内部信息（已在第 1 节描述）
- **M5**: 缺少请求体大小限制（已在第 1 节描述）

---

## 5. 敏感数据保护

### ✅ 优点

**无硬编码密钥**: 所有 AI 服务密钥从环境变量读取  
**日志安全**: 日志中不包含密码、Token 等敏感信息  
**数据加密**: 数据库连接使用 SSL（生产环境配置）

### 已识别问题

- **L3**: 日志可能包含用户输入（已在第 2 节描述）

---

## 6. 会话管理

### ✅ 优点

**Token 存储**: Token 存储在 Redis，支持过期和主动失效  
**Token 校验**: AuthTokenFilter 统一校验 Token 有效性  
**登出清理**: 登出时删除 Redis 中的 Token

### 无新增问题

会话管理由 common 模块统一处理，无 product 模块特有问题。

---

## 7. 安全问题汇总

### CRITICAL (1 个)
1. **C1**: 删除/修改操作缺少数据所有权校验 - 多个 Controller

### HIGH (5 个)
1. **H1**: Map 参数未校验导致类型转换风险 - 多个 Controller
2. **H2**: 缺少 API 限流保护 - 所有 Controller（特别是 AI 生成和 SSE 接口）
3. **H3**: SSE 流式接口无超时控制 - ProductScriptController
4. **H4**: 批量操作无权限校验 - ProductController
5. **H5**: ML 模型训练无资源限制 - StyleRecommendationController

### MEDIUM (9 个)
1. **M1**: 缺少方法级权限注解 - 所有 Controller（8 个文件，80+ 个 API）
2. **M2**: 缺少输入长度限制 - ProductScriptSaveVO, ProductSaveVO
3. **M3**: 缺少 LIKE 查询通配符转义 - ProductServiceImpl
4. **M4**: 错误响应可能泄露内部信息 - 多个 Controller
5. **M5**: 缺少请求体大小限制 - application.yml
6. **M6**: 文件上传缺少安全校验 - ProductController
7. **M7**: 缺少敏感操作审计日志 - 所有 Controller
8. **M8**: 数据范围控制不完善 - ProductController
9. **M9**: AI 服务调用缺少超时配置 - application.yml

### LOW (6 个)
1. **L1**: 缺少数值范围校验 - ProductSaveVO
2. **L2**: 缺少枚举值校验 - ProductScriptSaveVO
3. **L3**: 日志可能包含用户输入 - AI 生成服务日志
4. **L4**: Repository 层缺少强制隔离 - 所有 Repository
5. **L5**: 缺少并发控制 - ProductServiceImpl
6. **L6**: 缺少级联删除保护 - ProductServiceImpl

---

## 8. 修复优先级

### 立即修复 (本周内)
1. **C1**: 修复数据所有权校验问题 - 工作量 2 人日
2. **H1**: 修复 Map 参数校验问题，使用专用 VO 类 - 工作量 2 人日
3. **H2**: 添加 API 限流保护（Resilience4j RateLimiter） - 工作量 2.5 人日
4. **H3**: 添加 SSE 超时控制 - 工作量 1 人日
5. **H4**: 添加批量操作权限校验 - 工作量 1.5 人日
6. **H5**: 添加 ML 模型训练资源限制 - 工作量 1.5 人日

### 短期修复 (2 周内)
7. **M1**: 添加方法级权限注解 `@PreAuthorize` - 工作量 3 人日
8. **M2**: 添加输入长度限制 `@Size` - 工作量 0.5 人日
9. **M3**: 添加 LIKE 查询通配符转义 - 工作量 0.5 人日
10. **M4**: 统一错误信息，避免泄露内部信息 - 工作量 0.5 人日
11. **M5**: 配置请求体大小限制 - 工作量 0.5 人日
12. **M6**: 添加文件上传安全校验 - 工作量 1 人日
13. **M7**: 实现敏感操作审计日志 - 工作量 2 人日
14. **M8**: 完善数据范围控制 - 工作量 1 人日

### 长期优化 (1 个月内)
15. **M9**: 配置 AI 服务调用超时 - 工作量 0.5 人日
16. **L1**: 添加数值范围校验 - 工作量 0.5 人日
17. **L2**: 添加枚举值校验 - 工作量 0.5 人日
18. **L3**: 优化日志记录，避免记录用户输入 - 工作量 0.5 人日
19. **L4**: Repository 层强制隔离 - 工作量 1 人日
20. **L5**: 添加并发控制自动重试 - 工作量 0.5 人日
21. **L6**: 添加级联删除保护 - 工作量 0.5 人日

**总工作量估算**: 23.5 人日（约 5 周，1 人完成）

---

## 9. 安全最佳实践建议

1. **认证授权**:
   - 使用声明式权限注解（@PreAuthorize）
   - 所有修改/删除操作校验数据所有权
   - 批量操作逐个校验权限
   - 最小权限原则

2. **输入验证**:
   - 使用专用 VO 类替代 Map 参数
   - 添加 @Size 长度限制
   - 添加 @Min/@Max 数值范围限制
   - 添加 @Pattern 枚举值校验
   - LIKE 查询通配符转义
   - 文件上传校验 MIME 类型和大小

3. **API 安全**:
   - 全局限流保护（Resilience4j）
   - AI 生成接口单独限流（每分钟 10 次）
   - ML 训练接口严格限流（每小时 1 次）
   - SSE 超时控制（10 分钟）
   - 错误信息脱敏
   - 请求体大小限制（10MB）

4. **数据访问控制**:
   - 所有查询强制过滤 userId
   - 使用数据范围控制（DataScopeResolver）
   - Repository 层强制隔离
   - 级联删除保护

5. **日志审计**:
   - 记录所有敏感操作（创建/修改/删除/生成/训练）
   - 脱敏敏感参数
   - 定期归档日志
   - 审计日志不可篡改

6. **定期审计**:
   - 每季度进行安全审计
   - 上线前进行渗透测试
   - 使用 OWASP Dependency Check 扫描依赖漏洞
   - 定期更新依赖版本

---

## 10. 合规性检查

### OWASP Top 10 (2021) 覆盖情况

| 风险 | 状态 | 说明 |
|------|------|------|
| A01:2021 – Broken Access Control | 🔴 高风险 | 缺少数据所有权校验（C1），需立即修复 |
| A02:2021 – Cryptographic Failures | ✅ 已防护 | 无硬编码密钥，数据库连接加密 |
| A03:2021 – Injection | ✅ 已防护 | JPA Specification 防 SQL 注入，需添加 LIKE 转义 |
| A04:2021 – Insecure Design | ⚠️ 部分防护 | 缺少限流和超时控制（H2/H3/H5） |
| A05:2021 – Security Misconfiguration | ⚠️ 部分防护 | 缺少请求体大小限制、AI 超时配置 |
| A06:2021 – Vulnerable Components | ✅ 已防护 | 依赖定期更新 |
| A07:2021 – Identification and Authentication Failures | ✅ 已防护 | JWT + Redis 双重验证 |
| A08:2021 – Software and Data Integrity Failures | ✅ 已防护 | 无反序列化漏洞 |
| A09:2021 – Security Logging and Monitoring Failures | ⚠️ 部分防护 | 有日志记录，缺少审计日志表（M7） |
| A10:2021 – Server-Side Request Forgery (SSRF) | ✅ 已防护 | 无用户控制的 URL 请求 |

---

## 11. 与 Live 模块对比

| 维度 | Live 模块 | Product 模块 | 说明 |
|-----|-----------|--------------|------|
| 认证授权 | ⭐⭐⭐⭐ | ⭐⭐⭐ | Product 缺少数据所有权校验（C1）|
| 数据隔离 | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ | Product 部分接口缺少 userId 过滤 |
| 输入验证 | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ | 两者都有 Map 参数问题 |
| API 限流 | ⭐ | ⭐ | 两者都缺少全局限流 |
| 错误处理 | ⭐⭐⭐ | ⭐⭐⭐ | 两者都有泄露风险 |
| 文件上传 | N/A | ⭐⭐ | Product 有文件上传，缺少安全校验 |
| ML 功能 | N/A | ⭐⭐ | Product 有 ML 训练，缺少资源限制 |

**Product 模块特有风险**:
- 数据所有权校验缺失（CRITICAL）
- 文件上传安全校验不足
- ML 模型训练无资源限制
- 批量操作无权限校验

**Product 模块优势**:
- 使用 @CurrentUserId 注解简化代码
- 乐观锁防止并发库存更新
- 排序字段白名单验证

---

## 12. 审计结论

**总体评价**: Product 模块安全性中等，存在 1 个 CRITICAL 级别漏洞（数据所有权校验缺失），必须修复后上线。

**主要优势**:
- 统一的认证授权机制
- JPA Specification 防 SQL 注入
- 无硬编码密钥和敏感信息泄露
- 乐观锁防止并发库存更新
- 排序字段白名单验证

**需要改进**:
- 修复数据所有权校验问题（P0）
- 修复 Map 参数校验问题（P1）
- 添加 API 限流和 SSE 超时控制（P1）
- 添加批量操作权限校验（P1）
- 添加 ML 模型训练资源限制（P1）
- 完善输入验证和数据访问控制（P2）
- 实现审计日志表（P2）

**生产就绪建议**: 必须修复 1 个 CRITICAL 和 5 个 HIGH 优先级问题后可上线，MEDIUM 和 LOW 问题可在后续迭代中修复。

**安全评分**: 75/100 (中等)

---

**审计完成日期**: 2026-05-08  
**下次审计建议**: 2026-08-08（3 个月后）  
**相关文档**: 
- `docs/modules/live/security-audit.md` - Live 模块安全审计
- `docs/modules/product/architecture-review.md` - Product 模块架构评审（待创建）
- `docs/modules/product/code-review.md` - Product 模块代码评审（待创建）
- `docs/adr/002-无数据库外键.md` - 跨模块关联设计
- `docs/adr/003-Specification动态查询.md` - 动态查询安全

