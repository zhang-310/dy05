# Douyinapi 模块安全审计报告

## 审计概述

**模块名称**: douyinapi  
**审计日期**: 2026-05-08  
**审计标准**: OWASP Top 10 2021 + CVSS 3.1  
**审计范围**: 抖音 API 封装 + OAuth 2.0 授权

**审计文件**:
- `DouyinOAuthController.java` - OAuth 授权控制器
- `OAuthTokenServiceImpl.java` - Token 管理服务实现
- `DouyinApiClient.java` - 抖音 API 客户端
- `OAuthToken.java` - Token 实体
- `OAuthTokenRepository.java` - Token 数据访问层

## 安全评分

| 维度 | 得分 | 说明 |
|------|------|------|
| 认证授权 | 16/20 | OAuth 2.0 实现良好，但存在 state 验证时序问题 |
| 数据保护 | 14/20 | Token 明文存储，缺少加密保护 |
| 输入验证 | 12/15 | 部分参数缺少验证，存在注入风险 |
| 输出编码 | 13/15 | 日志输出基本安全，但存在敏感信息泄露 |
| 访问控制 | 13/15 | 用户隔离良好，但缺少细粒度权限控制 |
| 日志审计 | 11/15 | 日志记录完整，但敏感数据未脱敏 |
| **总分** | **79/100** | **等级**: 良好（需改进） |

## 漏洞清单

### CRITICAL 严重漏洞

#### C1. Token 明文存储 (CWE-312)
**CVSS 3.1**: 8.1 (HIGH)  
**向量**: AV:N/AC:L/PR:L/UI:N/S:U/C:H/I:H/A:N

**位置**: `OAuthToken.java` 第 33-36 行
```java
@Column(name = "access_token", nullable = false, length = 512)
private String accessToken;

@Column(name = "refresh_token", length = 512)
private String refreshToken;
```

**问题**: OAuth access_token 和 refresh_token 以明文形式存储在数据库中。如果数据库被攻破或发生 SQL 注入，攻击者可直接获取所有用户的授权凭证。

**影响**: 
- 攻击者可冒充用户访问抖音 API
- 可能导致用户数据泄露、账号劫持
- 违反 GDPR/个人信息保护法合规要求

**修复建议**:
```java
// 使用 AES-256-GCM 加密存储
@Column(name = "access_token", nullable = false, length = 1024)
@Convert(converter = TokenEncryptionConverter.class)
private String accessToken;
```

---

#### C2. State 验证存在 TOCTOU 竞态条件 (CWE-367)
**CVSS 3.1**: 7.4 (HIGH)  
**向量**: AV:N/AC:H/PR:N/UI:N/S:U/C:H/I:H/A:N

**位置**: `DouyinOAuthController.java` 第 352-361 行
```java
// 1. 检查 Redis 中是否存在该 state（防重放）
String storedUserId = stringRedisTemplate.opsForValue().get(redisKey);
if (storedUserId == null) {
    return null;
}

// 2. 删除 state，确保一次性使用
stringRedisTemplate.delete(redisKey);
```

**问题**: GET 和 DELETE 操作不是原子的。在高并发场景下，攻击者可能在检查和删除之间重放 state。

**修复建议**:
```java
// 使用 Lua 脚本实现原子性 check-and-delete
String luaScript = """
    local value = redis.call('GET', KEYS[1])
    if value then
        redis.call('DEL', KEYS[1])
        return value
    else
        return nil
    end
    """;
String storedUserId = stringRedisTemplate.execute(
    new DefaultRedisScript<>(luaScript, String.class),
    Collections.singletonList(redisKey)
);
```

---

### HIGH 高危漏洞

#### H1. 缺少 CSRF 保护 (CWE-352)
**CVSS 3.1**: 6.5 (MEDIUM)  
**向量**: AV:N/AC:L/PR:N/UI:R/S:U/C:N/I:H/A:N

**位置**: `DouyinOAuthController.java` 第 115-155 行
```java
@GetMapping("/callback")
public String callback(@RequestParam String code, @RequestParam String state) {
    // 直接处理回调，未验证请求来源
}
```

**问题**: OAuth callback 端点未验证 Referer 或 Origin，攻击者可构造恶意页面诱导用户点击，将授权绑定到攻击者账户。

**修复建议**:
```java
@GetMapping("/callback")
public String callback(@RequestParam String code, 
                      @RequestParam String state,
                      @RequestHeader(value = "Referer", required = false) String referer) {
    // 验证 Referer 来自抖音域名
    if (referer == null || !referer.startsWith("https://open.douyin.com")) {
        log.warn("OAuth callback Referer 验证失败: {}", referer);
        return "授权失败：请求来源不合法";
    }
    // ...
}
```

---

#### H2. 敏感信息日志泄露 (CWE-532)
**CVSS 3.1**: 6.2 (MEDIUM)  
**向量**: AV:L/AC:L/PR:L/UI:N/S:U/C:H/I:N/A:N

**位置**: 多处日志输出
```java
// DouyinOAuthController.java:118
log.info("收到抖音 OAuth 回调: code={}, state={}", code, state);

// DouyinOAuthController.java:146
log.info("抖音授权成功: userId={}, openId={}", userId, tokenResponse.openId());
```

**问题**: 日志中记录了 OAuth code、state、openId 等敏感信息。日志文件可能被未授权人员访问。

**修复建议**:
```java
// 使用脱敏工具类
log.info("收到抖音 OAuth 回调: code={}, state={}", 
    maskSensitive(code), maskSensitive(state));

private String maskSensitive(String value) {
    if (value == null || value.length() <= 8) return "***";
    return value.substring(0, 4) + "***" + value.substring(value.length() - 4);
}
```

---

#### H3. 缺少请求签名验证 (CWE-345)
**CVSS 3.1**: 6.8 (MEDIUM)  
**向量**: AV:N/AC:L/PR:L/UI:N/S:U/C:N/I:H/A:N

**位置**: `DouyinApiClient.java` 第 53-84 行
```java
public AccessTokenResponse getAccessToken(String code) {
    // 直接使用 code 换取 token，未验证响应签名
    ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
}
```

**问题**: 未验证抖音 API 响应的签名，可能遭受中间人攻击（MITM）篡改响应数据。

**修复建议**:
```java
// 验证响应签名（如果抖音 API 提供）
if (root.has("signature")) {
    String signature = root.path("signature").asText();
    String expectedSig = hmacSha256(response.getBody(), clientSecret);
    if (!signature.equals(expectedSig)) {
        log.error("抖音 API 响应签名验证失败");
        return null;
    }
}
```

---

### MEDIUM 中危漏洞

#### M1. 缺少速率限制（部分端点）
**CVSS 3.1**: 5.3 (MEDIUM)  
**向量**: AV:N/AC:L/PR:N/UI:N/S:U/C:N/I:N/A:L

**位置**: `DouyinOAuthController.java` 第 160-193 行
```java
@PostMapping("/refresh-token")
public RESTResult<Map<String, Object>> refreshToken(HttpServletRequest request,
                                                     @RequestParam String refreshToken) {
    // 缺少限流保护
}
```

**问题**: `/refresh-token`、`/revoke`、`/token-status` 等端点缺少限流保护，可能被滥用导致服务拒绝。

**修复建议**:
```java
@PostMapping("/refresh-token")
public RESTResult<Map<String, Object>> refreshToken(HttpServletRequest request,
                                                     @RequestParam String refreshToken) {
    try {
        oauthRateLimiter.acquirePermission();
    } catch (RequestNotPermitted e) {
        return RESTResult.error(ErrorCode.TOO_MANY_REQUESTS, "请求过于频繁");
    }
    // ...
}
```

---

#### M2. Token 过期时间过长
**CVSS 3.1**: 4.3 (MEDIUM)  
**向量**: AV:N/AC:L/PR:L/UI:N/S:U/C:L/I:N/A:N

**位置**: `OAuthToken.java` 第 77-81 行
```java
public boolean isExpiringSoon() {
    if (expiresAt == null) return false;
    long thirtyMinutes = 30 * 60 * 1000L;
    return expiresAt.getTime() - System.currentTimeMillis() < thirtyMinutes;
}
```

**问题**: Token 提前刷新时间仅为 30 分钟，对于长期有效的 refresh_token 缺少最大生命周期限制。

**修复建议**:
```java
// 添加 refresh_token 最大生命周期（如 90 天）
@Column(name = "refresh_token_expires_at")
private Timestamp refreshTokenExpiresAt;

public boolean isRefreshTokenExpired() {
    return refreshTokenExpiresAt != null && 
           refreshTokenExpiresAt.before(new Timestamp(System.currentTimeMillis()));
}
```

---

#### M3. 缺少输入长度验证
**CVSS 3.1**: 4.7 (MEDIUM)  
**向量**: AV:N/AC:L/PR:N/UI:N/S:U/C:N/I:N/A:L

**位置**: `DouyinOAuthController.java` 第 117 行
```java
public String callback(@RequestParam String code, @RequestParam String state) {
    // 未验证 code 和 state 长度
}
```

**问题**: 未验证输入参数长度，超长输入可能导致内存溢出或日志文件膨胀。

**修复建议**:
```java
public String callback(@RequestParam String code, @RequestParam String state) {
    if (code == null || code.length() > 512 || state == null || state.length() > 256) {
        log.warn("OAuth callback 参数长度异常");
        return "授权失败：参数格式错误";
    }
    // ...
}
```

---

### LOW 低危漏洞

#### L1. 异常信息泄露
**CVSS 3.1**: 3.1 (LOW)  
**向量**: AV:N/AC:H/PR:L/UI:N/S:U/C:L/I:N/A:N

**位置**: `DouyinOAuthController.java` 第 153 行
```java
return "授权失败：" + e.getMessage();
```

**问题**: 直接返回异常消息给用户，可能泄露内部实现细节。

**修复建议**:
```java
log.error("处理 OAuth 回调失败", e);
return "授权失败，请稍后重试";
```

---

#### L2. 缺少安全响应头
**CVSS 3.1**: 3.7 (LOW)  
**向量**: AV:N/AC:H/PR:N/UI:R/S:U/C:L/I:N/A:N

**位置**: `DouyinOAuthController.java` 第 149 行
```java
return "<html><body><h2>授权成功！</h2>...";
```

**问题**: HTML 响应缺少 `X-Content-Type-Options`、`X-Frame-Options` 等安全头。

**修复建议**:
```java
@GetMapping("/callback")
public ResponseEntity<String> callback(...) {
    HttpHeaders headers = new HttpHeaders();
    headers.add("X-Content-Type-Options", "nosniff");
    headers.add("X-Frame-Options", "DENY");
    headers.add("Content-Security-Policy", "default-src 'none'");
    return ResponseEntity.ok().headers(headers).body(html);
}
```

---

#### L3. 缺少请求 ID 追踪
**CVSS 3.1**: 2.3 (LOW)  
**向量**: AV:L/AC:L/PR:H/UI:N/S:U/C:N/I:N/A:L

**位置**: 所有 API 端点

**问题**: 部分端点未设置 traceId，影响问题排查效率。

**修复建议**: 统一在 Controller 基类或拦截器中设置 traceId。

---

## OWASP Top 10 检查

### A01:2021 - Broken Access Control ⚠️

**发现问题**:
1. **跨账户授权漏洞**: `DouyinOAuthController.java` 第 207-213 行允许通过 `accountId` 参数撤销其他用户的授权，仅检查账户存在性，未验证所有权。

```java
if (body != null && body.get("accountId") != null) {
    Long accountId = ((Number) body.get("accountId")).longValue();
    Optional<DouyinAccount> accountOpt = douyinAccountRepository.findById(accountId);
    if (accountOpt.isPresent()) {
        userId = accountOpt.get().getUserId(); // 未验证 userId 是否属于当前用户
    }
}
```

**CVSS**: 7.1 (HIGH)  
**修复**: 添加所有权验证
```java
DouyinAccount account = accountOpt.get();
if (!account.getUserId().equals(userId)) {
    return RESTResult.error(ErrorCode.FORBIDDEN, "无权操作此账户");
}
```

**评分**: 🔴 **不合格** - 存在高危访问控制漏洞

---

### A02:2021 - Cryptographic Failures 🔴

**发现问题**:
1. **Token 明文存储** (见 C1)
2. **HMAC 密钥管理不当**: `DouyinOAuthController.java` 第 406 行直接使用 `clientSecret` 作为 HMAC 密钥，未进行密钥派生。

```java
mac.init(new SecretKeySpec(clientSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
```

**建议**: 使用专用的签名密钥，通过 HKDF 从 clientSecret 派生。

**评分**: 🔴 **严重不合格** - Token 明文存储是严重安全隐患

---

### A03:2021 - Injection ✅

**检查结果**:
- ✅ 使用 JPA 参数化查询，无 SQL 注入风险
- ✅ 日志输出使用占位符，无日志注入
- ✅ HTTP 请求使用 RestTemplate，参数正确编码

**示例**: `OAuthTokenRepository.java` 第 28-35 行
```java
@Query("UPDATE OAuthToken t SET t.accessToken = :accessToken, ...")
int updateToken(@Param("userId") Long userId, ...);
```

**评分**: ✅ **合格** - 无注入漏洞

---

### A04:2021 - Insecure Design ⚠️

**发现问题**:
1. **State 验证时序问题** (见 C2)
2. **缺少 OAuth 状态机**: 未记录授权流程状态（pending/completed/failed），无法防止重复授权或检测异常流程。
3. **缺少 Token 撤销通知**: 撤销 Token 后未通知抖音平台，Token 在抖音侧仍然有效。

**建议**: 
- 实现完整的 OAuth 状态机
- 调用抖音 Token 撤销 API（如果提供）

**评分**: ⚠️ **需改进** - 设计存在安全缺陷

---

### A05:2021 - Security Misconfiguration ⚠️

**发现问题**:
1. **默认配置不安全**: `application.yml` 中 `client-secret` 默认值为 `your_client_secret`，生产环境可能忘记修改。
2. **缺少 HTTPS 强制**: 未验证 `callbackUrl` 必须使用 HTTPS。
3. **错误信息过于详细** (见 L1)

**建议**:
```java
@PostConstruct
public void validateConfig() {
    if ("your_client_secret".equals(clientSecret)) {
        throw new IllegalStateException("请配置正确的 DOUYIN_CLIENT_SECRET");
    }
    if (!callbackUrl.startsWith("https://")) {
        throw new IllegalStateException("OAuth callback URL 必须使用 HTTPS");
    }
}
```

**评分**: ⚠️ **需改进** - 配置管理需加强

---

### A06:2021 - Vulnerable Components ✅

**检查结果**:
- ✅ Spring Boot 3.3.7（最新稳定版）
- ✅ Jackson 依赖由 Spring Boot 管理，无已知漏洞
- ✅ 未使用过时或废弃的库

**建议**: 定期运行 `mvn dependency-check:check` 扫描依赖漏洞。

**评分**: ✅ **合格** - 依赖库安全

---

### A07:2021 - Authentication Failures ⚠️

**发现问题**:
1. **缺少 CSRF 保护** (见 H1)
2. **Session 固定风险**: OAuth callback 后未重新生成 session ID。
3. **缺少多因素认证**: 高价值操作（如撤销授权）未要求二次验证。

**建议**:
```java
// 在 callback 成功后重新生成 session
HttpSession session = request.getSession(false);
if (session != null) {
    session.invalidate();
}
request.getSession(true);
```

**评分**: ⚠️ **需改进** - 认证机制需加强

---

### A08:2021 - Software and Data Integrity ⚠️

**发现问题**:
1. **缺少响应签名验证** (见 H3)
2. **未验证 Token 响应完整性**: 抖音 API 返回的 Token 未进行完整性校验。
3. **缺少审计日志**: Token 的创建、刷新、撤销操作未记录到独立的审计日志表。

**建议**: 实现审计日志表 `oauth_audit_log`，记录所有敏感操作。

**评分**: ⚠️ **需改进** - 数据完整性保护不足

---

### A09:2021 - Security Logging Failures ⚠️

**发现问题**:
1. **敏感信息未脱敏** (见 H2)
2. **缺少安全事件告警**: Token 刷新失败、异常登录等安全事件未触发告警。
3. **日志级别不当**: 部分安全事件使用 `log.debug`，生产环境可能不记录。

**示例**: `DouyinApiClient.java` 第 259 行
```java
log.debug("getVideoData 解析失败: itemId={}, err={}", itemId, e.getMessage());
```

**建议**: 安全事件使用 `log.warn` 或 `log.error`。

**评分**: ⚠️ **需改进** - 日志记录需完善

---

### A10:2021 - SSRF ✅

**检查结果**:
- ✅ 所有外部请求使用固定的 `baseUrl`（`https://open.douyin.com`）
- ✅ 未发现用户可控的 URL 参数
- ✅ RestTemplate 配置合理，无 SSRF 风险

**评分**: ✅ **合格** - 无 SSRF 风险

---

## 修复建议

### 立即修复（P0）- 预计 3 人日

1. **[C1] Token 加密存储**
   - 实现 `TokenEncryptionConverter` 使用 AES-256-GCM 加密
   - 密钥管理：使用 AWS KMS / Azure Key Vault / HashiCorp Vault
   - 迁移脚本：加密现有明文 Token

2. **[C2] 修复 State TOCTOU 竞态**
   - 使用 Redis Lua 脚本实现原子性操作
   - 添加集成测试验证并发安全性

3. **[A01] 修复跨账户授权漏洞**
   - 在 `/revoke`、`/token-status`、`/token-refresh` 中添加所有权验证
   - 编写安全测试用例

---

### 短期修复（P1）- 预计 2 人日

4. **[H1] 添加 CSRF 保护**
   - 验证 OAuth callback 的 Referer 头
   - 实现 double-submit cookie 模式

5. **[H2] 敏感信息脱敏**
   - 实现 `SensitiveDataMasker` 工具类
   - 批量替换日志输出中的敏感字段

6. **[M1] 补充限流保护**
   - 为所有 OAuth 端点添加 `@RateLimiter` 注解
   - 配置合理的限流阈值（如 10 req/min/user）

---

### 中期改进（P2）- 预计 3 人日

7. **[A04] 实现 OAuth 状态机**
   - 新增 `oauth_authorization_log` 表记录授权流程
   - 状态：`initiated` → `callback_received` → `token_obtained` → `completed`

8. **[A08] 实现审计日志**
   - 新增 `oauth_audit_log` 表
   - 记录字段：userId, action, provider, timestamp, ipAddress, userAgent, result

9. **[M2] Token 生命周期管理**
   - 添加 `refresh_token_expires_at` 字段
   - 实现定时任务清理过期 Token

10. **[A05] 配置安全加固**
    - 添加 `@PostConstruct` 配置验证
    - 强制 HTTPS callback URL
    - 移除默认配置值

---

### 长期优化（P3）- 预计 2 人日

11. **[H3] 响应签名验证**
    - 研究抖音 API 是否提供响应签名
    - 实现签名验证逻辑

12. **[L2] 安全响应头**
    - 配置 Spring Security 全局响应头
    - 添加 CSP、HSTS、X-Frame-Options

13. **[A09] 安全监控告警**
    - 集成 Prometheus + Grafana
    - 配置告警规则：Token 刷新失败率 > 10%、异常 IP 登录等

14. **依赖扫描自动化**
    - CI/CD 集成 OWASP Dependency-Check
    - 每周自动扫描并生成报告

---

## 合规性检查

### GDPR / 个人信息保护法

| 要求 | 状态 | 说明 |
|------|------|------|
| 数据加密 | ❌ | Token 明文存储，违反加密要求 |
| 最小化原则 | ✅ | 仅存储必要的 OAuth 字段 |
| 访问控制 | ⚠️ | 存在跨账户访问漏洞 |
| 数据删除 | ✅ | 实现软删除机制 |
| 审计日志 | ❌ | 缺少独立审计日志表 |

**合规评分**: 40% - **不合规**

---

## 总结

**整体安全性**: 良好，但存在严重的 Token 明文存储问题和多个高危漏洞。

**关键风险**:
1. 🔴 **Token 明文存储** - 数据库泄露将导致所有用户授权被劫持
2. 🔴 **State TOCTOU 竞态** - 高并发下可能被重放攻击
3. 🟡 **跨账户授权漏洞** - 可撤销他人授权
4. 🟡 **敏感信息日志泄露** - 日志文件可能被未授权访问

**生产就绪**: ❌ **不推荐** - 必须修复 P0 问题后才能上线

**预计工作量**: 
- P0（必须）: 3 人日
- P1（强烈建议）: 2 人日
- P2（建议）: 3 人日
- P3（可选）: 2 人日
- **总计**: 10 人日

**下一步行动**:
1. 立即启动 Token 加密存储改造（P0-1）
2. 修复 State 竞态条件（P0-2）
3. 修复跨账户授权漏洞（P0-3）
4. 完成 P0 修复后进行渗透测试验证
5. 逐步实施 P1-P3 改进项

---

**审计人**: Claude Opus 4  
**审计工具**: 人工代码审查 + OWASP Top 10 2021 + CVSS 3.1  
**报告版本**: 1.0
