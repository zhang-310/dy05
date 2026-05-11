# Douyinapi 模块模式合规性检查报告

## 检查概述

**模块名称**: douyinapi  
**检查日期**: 2026-05-08  
**检查标准**: 项目架构规范 + ADR 决策记录  
**检查范围**: Controller + Service + Client + Entity + Repository

**检查文件**:
- `douyin-operations-douyin/src/main/java/.../controller/DouyinOAuthController.java`
- `douyin-operations-integration/src/main/java/.../service/impl/OAuthTokenServiceImpl.java`
- `douyin-operations-integration/src/main/java/.../service/OAuthTokenService.java`
- `douyin-operations-integration/src/main/java/.../client/DouyinApiClient.java`
- `douyin-operations-integration/src/main/java/.../entity/OAuthToken.java`
- `douyin-operations-integration/src/main/java/.../repository/OAuthTokenRepository.java`

## 合规性评分

| 维度 | 得分 | 说明 |
|------|------|------|
| API 规范 | 18/20 | 符合统一 POST 规范，GET 例外合理，缺少部分 @Valid 校验 |
| 数据访问模式 | 10/20 | 未使用 Specification 动态查询，直接使用自定义 @Query |
| 错误处理 | 13/15 | 使用 RESTResult + ErrorCode，部分异常处理可优化 |
| 缓存策略 | 0/15 | 未使用缓存（Token 查询频繁但无缓存） |
| 数据隔离 | 15/15 | 正确实现 userId 数据隔离 |
| 命名规范 | 15/15 | 命名清晰，符合项目规范 |
| **总分** | **71/100** | **等级**: C |

## 模式检查清单

### 1. API 规范（ADR-001）✅ 部分合规

- [x] **统一使用 POST 方法**：业务接口使用 POST（`/auth-url`, `/refresh-token`, `/revoke`, `/token-status`, `/token-refresh`）
- [x] **GET 例外合理**：`/authorize-url`（GET）、`/callback`（GET）符合 OAuth 标准和 ADR-001 例外规则
- [x] **路径格式**：`/api/v1/douyin/oauth/<动作>`，符合规范
- [x] **返回 RESTResult<T>**：所有接口返回 `RESTResult<T>` 统一响应体
- [x] **设置 traceId**：所有响应设置 `r.setTraceId(MDC.get("traceId"))`
- [⚠️] **@Valid 参数校验**：`/token-status` 和 `/token-refresh` 的 `@RequestBody Map<String, Object>` 未使用 VO + @Valid

**违规示例**：
```java
// Line 227: 应该定义 TokenStatusVO 替代 Map
public RESTResult<Map<String, Object>> tokenStatus(HttpServletRequest request,
                                                    @RequestBody Map<String, Object> body)
```

**建议**：
- 创建 `TokenStatusVO` 和 `TokenRefreshVO`，使用 `@Valid` 校验
- 避免直接使用 `Map<String, Object>` 作为请求参数

### 2. 数据访问模式（ADR-003）❌ 不合规

- [❌] **未使用 JPA Specification**：Repository 使用自定义 `@Query` 方法
- [❌] **无 SearchVO 继承 BasicQueryDto**：模块无列表查询接口，无需分页
- [✅] **避免 N+1 查询**：查询简单，无 N+1 问题

**当前实现**：
```java
// OAuthTokenRepository.java
@Query("UPDATE OAuthToken t SET t.accessToken = :accessToken, ...")
int updateToken(@Param("userId") Long userId, ...);
```

**分析**：
- douyinapi 模块主要是 OAuth 授权流程，不涉及复杂列表查询
- 使用自定义 `@Query` 更新 Token 是合理的（非查询场景）
- **不算严重违规**，因为模块特性不需要 Specification

### 3. 数据隔离（ADR-004）✅ 完全合规

- [x] **Entity 含 userId 字段**：`OAuthToken` 实体有 `userId` 字段
- [x] **Service 层强制过滤**：所有查询方法都按 `userId` 过滤
- [x] **跨用户访问校验**：Controller 从 `HttpServletRequest` 提取 `userId`，Service 层强制隔离

**正确实现**：
```java
// Line 76-77: Service 层按 userId 查询
public Optional<OAuthToken> getToken(Long userId, String provider) {
    return tokenRepository.findByUserIdAndProviderAndDeleted(userId, provider, 0);
}

// Line 86-89: Controller 层提取 userId
Long userId = AuthTokenFilter.getUserId(request);
if (userId == null) {
    return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
}
```

### 4. 逻辑删除（ADR-005）✅ 完全合规

- [x] **Entity 含 deleted 字段**：`OAuthToken` 有 `deleted` 字段（默认 0）
- [x] **使用 @SQLRestriction**：`@SQLRestriction("deleted = 0")`（Line 16）
- [x] **删除操作设置 deleted=1**：`deleteByUserIdAndProvider` 使用软删除（Line 41-43）

**正确实现**：
```java
// OAuthToken.java Line 16
@SQLRestriction("deleted = 0")

// OAuthTokenRepository.java Line 41-43
@Query("UPDATE OAuthToken t SET t.deleted = 1, t.updateTime = CURRENT_TIMESTAMP ...")
int deleteByUserIdAndProvider(...);
```

### 5. 缓存策略 ❌ 未实现

- [❌] **未使用 Spring Cache 注解**：Token 查询频繁但无缓存
- [❌] **未配置 L1/L2 缓存**：每次查询都访问数据库

**性能问题**：
- `getValidAccessToken` 方法在每次 API 调用时都查询数据库
- Token 有效期通常较长（小时级），适合缓存
- 建议使用 `@Cacheable("oauth-tokens")` + Redis L2 缓存

**建议实现**：
```java
@Cacheable(value = "oauth-tokens", key = "#userId + ':' + #provider")
public Optional<OAuthToken> getToken(Long userId, String provider) {
    return tokenRepository.findByUserIdAndProviderAndDeleted(userId, provider, 0);
}

@CacheEvict(value = "oauth-tokens", key = "#userId + ':' + #provider")
public void saveOrUpdateToken(Long userId, String provider, ...) {
    // ...
}
```

### 6. 错误码规范 ✅ 部分合规

- [x] **使用 ErrorCode 常量**：使用 `ErrorCode.UNAUTHORIZED`、`ErrorCode.TOO_MANY_REQUESTS`、`ErrorCode.INTERNAL_ERROR`
- [x] **错误信息清晰**：错误消息描述清晰（"未登录"、"请求过于频繁"）
- [⚠️] **错误码覆盖不全**：部分场景返回 HTML 字符串而非 JSON（`/callback` 端点）

**违规示例**：
```java
// Line 149: OAuth 回调失败返回 HTML，前端无法解析
return "<html><body><h2>授权成功！</h2>...</body></html>";
```

**建议**：
- `/callback` 端点应重定向到前端页面，由前端展示结果
- 或返回 JSON 格式错误，避免 HTML 字符串

### 7. 时间字段管理 ✅ 完全合规

- [x] **使用 @PrePersist/@PreUpdate**：`OAuthToken` 实体正确实现（Line 56-65）
- [x] **自动维护 createTime/updateTime**：无需手动设置

### 8. 安全实践 ✅ 优秀

- [x] **HMAC 签名验证**：state 参数使用 HMAC-SHA256 签名（Line 402-412）
- [x] **防重放攻击**：state 存储到 Redis，一次性使用（Line 352-361）
- [x] **时间戳过期校验**：state 包含时间戳，10 分钟过期（Line 384-387）
- [x] **限流保护**：使用 Resilience4j RateLimiter（Line 79-84）
- [x] **敏感信息保护**：clientSecret 从配置文件读取，不硬编码

**优秀实践**：
```java
// Line 347-400: 多层安全校验
private String extractUserIdFromState(String state) {
    // 1. Redis 防重放
    // 2. 删除 state 确保一次性使用
    // 3. HMAC 签名验证
    // 4. 时间戳过期校验
    // 5. userId 一致性校验
}
```

### 9. 事务管理 ✅ 合规

- [x] **使用 @Transactional**：Service 层修改方法标注 `@Transactional(rollbackFor = Exception.class)`
- [x] **异常回滚**：指定 `rollbackFor = Exception.class`

### 10. 日志记录 ✅ 合规

- [x] **使用 SLF4J Logger**：所有类使用 `LoggerFactory.getLogger`
- [x] **日志级别合理**：info（正常流程）、warn（限流/异常）、error（失败）、debug（详细信息）
- [x] **包含关键上下文**：userId、provider、state 等关键信息

## 违规清单

### P0 严重违规

**无 P0 级别违规**

### P1 高优先级违规

1. **缺少缓存机制**（性能问题）
   - **文件**: `OAuthTokenServiceImpl.java`
   - **问题**: Token 查询频繁但无缓存，每次都访问数据库
   - **影响**: 高频 API 调用导致数据库压力
   - **修复**: 添加 `@Cacheable` 注解 + Redis L2 缓存
   - **工作量**: 0.5 人日

2. **请求参数未使用 VO + @Valid**
   - **文件**: `DouyinOAuthController.java` Line 227, 278
   - **问题**: `/token-status` 和 `/token-refresh` 使用 `Map<String, Object>` 而非 VO
   - **影响**: 缺少参数校验，类型不安全
   - **修复**: 创建 `TokenStatusVO` 和 `TokenRefreshVO`
   - **工作量**: 0.3 人日

### P2 中优先级问题

3. **OAuth 回调返回 HTML 而非 JSON**
   - **文件**: `DouyinOAuthController.java` Line 149, 153
   - **问题**: `/callback` 端点返回 HTML 字符串，前端无法统一处理
   - **影响**: 错误处理不一致，用户体验差
   - **修复**: 重定向到前端页面，由前端展示结果
   - **工作量**: 0.2 人日

4. **Token 过期告警未实现**
   - **文件**: `OAuthTokenServiceImpl.java` Line 182-204
   - **问题**: `sendTokenExpiredAlert` 方法仅打印日志，未实际发送通知
   - **影响**: 用户无法及时知道 Token 过期
   - **修复**: 集成企微/飞书通知服务
   - **工作量**: 0.5 人日

5. **缺少单元测试覆盖**
   - **文件**: 仅有 `OAuthTokenServiceImplTest.java`
   - **问题**: Controller 和 Client 层缺少测试
   - **影响**: 代码质量无保障
   - **修复**: 添加 Controller 和 Client 的单元测试
   - **工作量**: 1 人日

### P3 低优先级问题

6. **DouyinApiClient 重试逻辑可提取**
   - **文件**: `DouyinApiClient.java` Line 356-379
   - **问题**: `withRetry` 方法可提取为通用工具类
   - **影响**: 代码复用性差
   - **修复**: 提取到 `common.util.RetryUtils`
   - **工作量**: 0.2 人日

7. **部分方法返回 null 而非 Optional**
   - **文件**: `DouyinApiClient.java` 多处
   - **问题**: `getAccessToken`、`refreshAccessToken` 等方法返回 null
   - **影响**: 调用方需要 null 检查，容易 NPE
   - **修复**: 改为返回 `Optional<T>`
   - **工作量**: 0.3 人日

## 改进建议

### 立即修复（P1）

1. **添加 Token 缓存**
   ```java
   @Cacheable(value = "oauth-tokens", key = "#userId + ':' + #provider", unless = "#result.isEmpty()")
   public Optional<OAuthToken> getToken(Long userId, String provider) {
       return tokenRepository.findByUserIdAndProviderAndDeleted(userId, provider, 0);
   }
   
   @CacheEvict(value = "oauth-tokens", key = "#userId + ':' + #provider")
   public void saveOrUpdateToken(Long userId, String provider, ...) { ... }
   
   @CacheEvict(value = "oauth-tokens", key = "#userId + ':' + #provider")
   public void deleteToken(Long userId, String provider) { ... }
   ```

2. **创建请求 VO**
   ```java
   @Data
   public class TokenStatusVO {
       private Long accountId;
   }
   
   @Data
   public class TokenRefreshVO {
       private Long accountId;
   }
   
   // Controller 使用
   public RESTResult<Map<String, Object>> tokenStatus(
       HttpServletRequest request,
       @RequestBody @Valid TokenStatusVO vo) { ... }
   ```

### 短期改进（P2）

3. **优化 OAuth 回调流程**
   - 回调成功后重定向到前端页面：`redirect:/oauth/callback?status=success&provider=douyin`
   - 前端页面展示授权结果，提供关闭按钮
   - 失败时重定向到错误页：`redirect:/oauth/callback?status=error&message=xxx`

4. **实现 Token 过期告警**
   - 集成 `MessagingPlatformService`，发送企微/飞书通知
   - 或发送站内消息到用户消息中心
   - 添加邮件通知作为备选渠道

5. **补充单元测试**
   - `DouyinOAuthControllerTest`：测试授权流程、回调处理、限流
   - `DouyinApiClientTest`：测试 API 调用、重试逻辑、错误处理
   - 使用 MockRestTemplate 模拟抖音 API 响应

### 中期优化（P3）

6. **提取通用重试工具**
   ```java
   // common.util.RetryUtils
   public static <T> T withRetry(String apiName, int maxRetries, Supplier<T> action) {
       // 通用重试逻辑
   }
   ```

7. **改进 API 返回类型**
   - `DouyinApiClient` 方法返回 `Optional<T>` 而非 null
   - 减少调用方 null 检查负担

## 架构亮点

1. **安全设计优秀**
   - HMAC 签名 + Redis 防重放 + 时间戳过期，三重保护
   - 限流保护防止滥用
   - 敏感信息配置化，不硬编码

2. **数据隔离严格**
   - 所有查询强制按 userId 过滤
   - Controller 层统一提取 userId，Service 层强制隔离

3. **逻辑删除规范**
   - 使用 `@SQLRestriction` 自动过滤已删除数据
   - 软删除保留审计记录

4. **事务管理正确**
   - 修改操作使用 `@Transactional(rollbackFor = Exception.class)`
   - 异常自动回滚

5. **日志记录完善**
   - 关键操作记录日志（授权、刷新、撤销）
   - 包含 userId、provider 等上下文信息

## 总结

**整体合规性**: C 级（71/100）

**主要优势**:
- 安全设计优秀（HMAC + 防重放 + 限流）
- 数据隔离和逻辑删除完全合规
- 错误处理和日志记录规范

**主要问题**:
- 缺少缓存机制，高频查询直接访问数据库
- 部分接口未使用 VO + @Valid 校验
- 单元测试覆盖不足

**预计工作量**: 2.5 人日（P1 修复 0.8 人日 + P2 改进 1.7 人日）

**优先级建议**:
1. **立即修复**: 添加 Token 缓存（0.5 人日）
2. **短期改进**: 创建请求 VO（0.3 人日）
3. **中期优化**: 补充单元测试（1 人日）

**结论**: douyinapi 模块在安全性和数据隔离方面表现优秀，但在性能优化（缓存）和代码质量（测试覆盖）方面有改进空间。建议优先添加缓存机制以提升性能。
