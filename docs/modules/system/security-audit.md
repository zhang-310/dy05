# System 模块安全审计报告

## 审计概述

**模块名称**: system  
**审计日期**: 2026-05-08  
**审计标准**: OWASP Top 10 2021 + CVSS 3.1  
**审计范围**: 系统监控与管理模块（告警、指标、日志、外部API配置、健康检查）

**审计文件**:
- Controllers: 7 个（AlertController, SystemController, MetricsController, MonitoringController, ExternalApiConfigController, TaxonomyController, SystemPerformanceController）
- Services: 6 个（AlertEngineServiceImpl, ExternalApiConfigServiceImpl, ExternalApiGatewayImpl, SystemServiceImpl, DashboardDataServiceImpl, MetricsCollectorServiceImpl）
- Entities: 5 个（ExternalApiConfig, ExternalApiCallLog, SysApiCallLog, SysSyncLog, SysTaxonomyNode）

## 安全评分

| 维度 | 得分 | 说明 |
|------|------|------|
| 认证授权 | 14/20 | 部分端点缺少认证检查，admin权限验证不统一 |
| 数据保护 | 16/20 | API密钥加密存储良好，但脱敏实现有缺陷 |
| 输入验证 | 10/15 | 缺少输入长度限制，类型转换无异常处理 |
| 输出编码 | 12/15 | 日志输出未过滤敏感信息，错误消息泄露内部细节 |
| 访问控制 | 11/15 | 水平越权风险，缺少资源级权限检查 |
| 日志审计 | 13/15 | 审计日志完整，但缺少敏感操作的详细记录 |
| **总分** | **76/100** | **等级**: 良好（需改进） |

## 漏洞清单

### CRITICAL 严重漏洞

#### [CRITICAL-01] 未认证的指标端点暴露系统信息
**文件**: `MetricsController.java`  
**位置**: 第 28-106 行  
**CVSS 3.1**: 9.1 (CRITICAL) - AV:N/AC:L/PR:N/UI:N/S:U/C:H/I:N/A:H

**描述**: `/api/v1/system/metrics/*` 所有端点均无认证检查，任何人可访问系统指标、CPU、内存、JVM、数据库连接池等敏感信息。

```java
@GetMapping("/prometheus")
public String prometheusMetrics() {
    // 无认证检查，直接返回所有指标
    var metrics = metricsCollectorService.collectAllMetrics();
    // ...
}
```

**影响**: 攻击者可获取系统架构、资源使用情况，为进一步攻击提供情报。

**修复建议**:
```java
@GetMapping("/prometheus")
public String prometheusMetrics(HttpServletRequest request) {
    if (AuthTokenFilter.getUserId(request) == null) {
        throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
    }
    // 或限制为 admin 角色
    // ...
}
```

#### [CRITICAL-02] 告警规则内存存储无持久化且无访问控制
**文件**: `AlertEngineServiceImpl.java`  
**位置**: 第 27-30 行  
**CVSS 3.1**: 8.2 (HIGH) - AV:N/AC:L/PR:L/UI:N/S:U/C:H/I:H/A:L

**描述**: 告警规则和记录存储在 `ConcurrentHashMap` 内存中，无持久化，且无用户隔离。任何登录用户可修改/删除其他用户的告警规则。

```java
private final Map<Long, AlertRuleVO> rules = new ConcurrentHashMap<>();
private final Map<Long, AlertRecordVO> records = new ConcurrentHashMap<>();
```

**影响**:
1. 服务重启后所有告警规则丢失
2. 用户A可删除用户B的告警规则（水平越权）
3. 无审计日志记录谁修改了规则

**修复建议**:
1. 创建数据库表存储告警规则（含 owner_id 字段）
2. Service 层强制过滤 `ownerId = currentUserId`
3. 记录所有 CRUD 操作到审计日志

#### [CRITICAL-03] 外部API密钥脱敏实现错误
**文件**: `ExternalApiConfigServiceImpl.java`  
**位置**: 第 206-221 行  
**CVSS 3.1**: 7.5 (HIGH) - AV:N/AC:L/PR:L/UI:N/S:U/C:H/I:N/A:N

**描述**: `maskSensitiveFields()` 方法直接修改原对象，导致加密密钥被永久替换为脱敏值 `****xxxx`，后续无法解密使用。

```java
private ExternalApiConfig maskSensitiveFields(ExternalApiConfig config) {
    if (config.getApiKeyEncrypted() != null) {
        config.setApiKeyEncrypted(maskValue(config.getApiKeyEncrypted())); // 直接修改原对象！
    }
    // ...
    return config;
}
```

**影响**: 
1. 第一次查询后，数据库中的加密密钥被覆盖为脱敏值
2. 后续调用 `getDecryptedApiKey()` 时解密失败
3. 所有外部API调用失败

**修复建议**:
```java
private ExternalApiConfig maskSensitiveFields(ExternalApiConfig config) {
    ExternalApiConfig masked = new ExternalApiConfig();
    BeanUtils.copyProperties(config, masked);
    if (masked.getApiKeyEncrypted() != null) {
        masked.setApiKeyEncrypted(maskValue(masked.getApiKeyEncrypted()));
    }
    // ...
    return masked;
}
```

### HIGH 高危漏洞

#### [HIGH-01] SQL注入风险 - 动态JPQL拼接
**文件**: `SystemServiceImpl.java`  
**位置**: 第 107-111 行  
**CVSS 3.1**: 7.3 (HIGH) - AV:N/AC:L/PR:L/UI:N/S:U/C:H/I:L/A:L

**描述**: 使用字符串拼接构建JPQL查询，虽然使用了参数化，但字段名直接拼接存在风险。

```java
StringBuilder jpql = new StringBuilder(
    "SELECT COUNT(l), SUM(CASE WHEN l.status=1 THEN 1 ELSE 0 END), AVG(l.durationMs) FROM SysApiCallLog l WHERE 1=1");
if (module != null && !module.isBlank()) { 
    jpql.append(" AND l.module = :module"); // 字段名硬编码，但参数化正确
}
```

**当前状态**: 参数值已参数化，风险较低，但建议使用 Criteria API 或 Specification。

**修复建议**: 使用 JPA Specification 替代字符串拼接。

#### [HIGH-02] 类型转换无异常处理导致DoS
**文件**: `AlertController.java`  
**位置**: 第 45-49 行  
**CVSS 3.1**: 6.5 (MEDIUM) - AV:N/AC:L/PR:L/UI:N/S:U/C:N/I:N/A:H

**描述**: 多处使用 `Map<String, Object>` 接收请求体，类型转换无异常处理，恶意输入可导致 `ClassCastException`。

```java
@PostMapping("/alert/rule/update")
public RESTResult<?> updateAlertRule(@RequestBody Map<String, Object> request) {
    Long ruleId = Long.parseLong(request.get("ruleId").toString()); // NPE + NumberFormatException
    AlertRuleVO vo = convertToAlertRuleVO(request); // 多处类型转换无保护
    // ...
}
```

**影响**: 
1. 发送 `{"ruleId": "abc"}` 导致 `NumberFormatException`
2. 发送 `{"threshold": "not_a_number"}` 导致 `ClassCastException`
3. 发送 `{}` 导致 `NullPointerException`

**修复建议**:
```java
@PostMapping("/alert/rule/update")
public RESTResult<?> updateAlertRule(@Valid @RequestBody AlertRuleUpdateVO vo) {
    // 使用强类型VO + @Valid注解
    alertEngineService.updateAlertRule(vo.getRuleId(), vo);
    return RESTResult.success();
}
```

#### [HIGH-03] 外部API调用未验证响应内容
**文件**: `ExternalApiGatewayImpl.java`  
**位置**: 第 40-89 行  
**CVSS 3.1**: 6.8 (MEDIUM) - AV:N/AC:L/PR:L/UI:N/S:U/C:L/I:H/A:N

**描述**: 直接返回外部API响应体，未验证内容类型和大小，可能导致SSRF或内存溢出。

```java
HttpResponse<String> response = httpClient.send(requestBuilder.build(), 
    HttpResponse.BodyHandlers.ofString()); // 无大小限制
return ResponseEntity.status(response.statusCode()).body(response.body()); // 直接返回
```

**影响**:
1. 外部API返回恶意脚本，传递给前端导致XSS
2. 返回超大响应体（如100MB）导致内存溢出
3. SSRF攻击：攻击者控制 `baseUrl` 访问内网服务

**修复建议**:
1. 限制响应体大小（如10MB）
2. 验证 Content-Type
3. 白名单验证 `baseUrl` 域名
4. 对响应内容进行HTML转义

#### [HIGH-04] 日志记录敏感信息
**文件**: `SystemServiceImpl.java`  
**位置**: 第 203-221 行  
**CVSS 3.1**: 6.5 (MEDIUM) - AV:N/AC:L/PR:L/UI:N/S:U/C:H/I:N/A:N

**描述**: API调用日志记录完整请求参数和响应体，可能包含密码、token等敏感信息。

```java
log.setRequestParams(requestParams); // 可能包含密码
log.setResponseBody(responseBody != null && responseBody.length() > 2000
    ? responseBody.substring(0, 2000) : responseBody); // 可能包含token
```

**影响**: 管理员查看日志时可获取用户密码、API密钥等敏感信息。

**修复建议**:
1. 过滤敏感字段：`password`, `token`, `apiKey`, `secret`
2. 使用正则替换为 `***`
3. 仅记录请求参数的键名，不记录值

### MEDIUM 中危漏洞

#### [MEDIUM-01] 缺少请求频率限制
**文件**: 所有 Controller  
**CVSS 3.1**: 5.3 (MEDIUM) - AV:N/AC:L/PR:N/UI:N/S:U/C:N/I:N/A:L

**描述**: 所有端点均无频率限制，攻击者可暴力枚举或DoS攻击。

**修复建议**: 使用 Spring Security 的 `@RateLimiter` 或 Resilience4j 限流。

#### [MEDIUM-02] 健康检查端点泄露架构信息
**文件**: `SystemController.java`, `MonitoringController.java`  
**位置**: 第 137-148 行, 第 283-329 行  
**CVSS 3.1**: 5.3 (MEDIUM) - AV:N/AC:L/PR:L/UI:N/S:U/C:L/I:N/A:N

**描述**: 健康检查端点返回详细的基础设施信息（数据库、Redis、Milvus、Elasticsearch等），帮助攻击者了解系统架构。

```java
@PostMapping("/health")
public RESTResult<Map<String, Object>> health(HttpServletRequest request) {
    // 返回所有依赖服务的状态和延迟
    return RESTResult.getSuccess(systemService.checkHealth());
}
```

**修复建议**: 
1. 仅返回整体状态（UP/DOWN），不返回组件细节
2. 详细信息仅对 admin 角色开放
3. 移除延迟信息（可推断网络拓扑）

#### [MEDIUM-03] 错误消息泄露内部路径
**文件**: `ExternalApiConfigServiceImpl.java`  
**位置**: 第 71, 82, 87 行  
**CVSS 3.1**: 4.3 (MEDIUM) - AV:N/AC:L/PR:L/UI:N/S:U/C:L/I:N/A:N

**描述**: 异常消息包含供应商编码等内部信息。

```java
throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "供应商配置不存在: " + code);
throw new BusinessException(ErrorCode.DATA_ALREADY_EXISTS, "供应商编码已存在: " + saveVO.getProviderCode());
```

**修复建议**: 使用通用错误消息，详细信息仅记录到日志。

#### [MEDIUM-04] 定时任务无异常隔离
**文件**: `AlertEngineServiceImpl.java`  
**位置**: 第 88-102 行  
**CVSS 3.1**: 4.0 (MEDIUM) - AV:N/AC:L/PR:N/UI:N/S:U/C:N/I:N/A:L

**描述**: `@Scheduled` 定时任务中单个规则异常可能影响后续规则执行。

```java
@Scheduled(fixedDelay = 60000)
public void executeAlertChecks() {
    for (AlertRuleVO rule : rules.values()) {
        executeRuleCheck(rule); // 异常未捕获
    }
}
```

**修复建议**: 在循环内捕获异常，确保单个规则失败不影响其他规则。

#### [MEDIUM-05] 缺少CSRF保护
**文件**: 所有 POST 端点  
**CVSS 3.1**: 5.4 (MEDIUM) - AV:N/AC:L/PR:N/UI:R/S:U/C:L/I:L/A:N

**描述**: POST端点未启用CSRF保护，攻击者可构造恶意页面诱导用户执行操作。

**修复建议**: 启用Spring Security CSRF保护，或使用自定义CSRF token。

### LOW 低危漏洞

#### [LOW-01] 缺少输入长度限制
**文件**: `AlertController.java`, `ExternalApiConfigController.java`  
**CVSS 3.1**: 3.1 (LOW) - AV:N/AC:H/PR:L/UI:N/S:U/C:N/I:N/A:L

**描述**: VO类缺少 `@Size` 注解，攻击者可发送超长字符串导致数据库错误或内存溢出。

**修复建议**: 在所有VO字段添加 `@Size(max=xxx)` 注解。

#### [LOW-02] 日志级别不当
**文件**: `AlertEngineServiceImpl.java`  
**位置**: 第 45, 57, 67 行  
**CVSS 3.1**: 2.0 (LOW) - AV:L/AC:L/PR:H/UI:N/S:U/C:L/I:N/A:N

**描述**: 使用 `log.info()` 记录所有告警规则操作，生产环境可能产生大量日志。

**修复建议**: 改为 `log.debug()`，仅在开发环境启用。

#### [LOW-03] 硬编码超时时间
**文件**: `ExternalApiGatewayImpl.java`  
**位置**: 第 28, 49 行  
**CVSS 3.1**: 2.0 (LOW) - AV:N/AC:H/PR:L/UI:N/S:U/C:N/I:N/A:L

**描述**: 连接超时和请求超时硬编码为10秒和30秒，无法根据不同API调整。

**修复建议**: 从配置文件读取超时时间，或在 `ExternalApiConfig` 表中存储。

## OWASP Top 10 检查

### A01:2021 - Broken Access Control

**发现问题**:
1. ✅ **CRITICAL**: MetricsController 所有端点无认证（CRITICAL-01）
2. ✅ **HIGH**: AlertEngineService 无用户隔离，水平越权（CRITICAL-02）
3. ✅ **MEDIUM**: 健康检查端点对所有登录用户开放（MEDIUM-02）

**评分**: 3/10 (严重不足)

**建议**:
- 所有端点强制认证检查
- 资源级权限控制（owner_id过滤）
- 敏感端点限制为admin角色

### A02:2021 - Cryptographic Failures

**发现问题**:
1. ✅ **HIGH**: API密钥脱敏实现错误，破坏加密数据（CRITICAL-03）
2. ✅ **MEDIUM**: 日志记录敏感信息未加密（HIGH-04）
3. ✅ **良好**: 使用 `ApiKeyEncryptionService` 加密存储API密钥

**评分**: 6/10 (需改进)

**建议**:
- 修复脱敏方法，使用对象拷贝
- 日志中敏感字段自动脱敏
- 传输层启用TLS 1.3

### A03:2021 - Injection

**发现问题**:
1. ✅ **LOW**: JPQL使用参数化查询，风险较低（HIGH-01）
2. ✅ **良好**: JPA Specification 动态查询防止SQL注入
3. ⚠️ **潜在风险**: `sortName` 字段名未严格白名单验证

**评分**: 7/10 (良好)

**建议**:
- 所有动态字段名使用白名单验证
- 使用 Criteria API 替代字符串拼接

### A04:2021 - Insecure Design

**发现问题**:
1. ✅ **CRITICAL**: 告警规则内存存储无持久化（CRITICAL-02）
2. ✅ **HIGH**: 外部API调用未验证响应（HIGH-03）
3. ✅ **MEDIUM**: 缺少频率限制（MEDIUM-01）

**评分**: 5/10 (不足)

**建议**:
- 告警规则持久化到数据库
- 外部API响应验证和大小限制
- 全局频率限制和熔断机制

### A05:2021 - Security Misconfiguration

**发现问题**:
1. ✅ **MEDIUM**: 错误消息泄露内部信息（MEDIUM-03）
2. ✅ **LOW**: 日志级别不当（LOW-02）
3. ✅ **良好**: 使用 `@SQLRestriction` 防止查询已删除数据

**评分**: 7/10 (良好)

**建议**:
- 统一错误处理，隐藏内部细节
- 生产环境禁用详细错误堆栈

### A06:2021 - Vulnerable and Outdated Components

**发现问题**:
1. ✅ **良好**: 使用 Spring Boot 3.3.7（最新稳定版）
2. ✅ **良好**: Jakarta EE 10 规范
3. ⚠️ **未检查**: 第三方依赖版本（需运行 `mvn dependency:tree`）

**评分**: 8/10 (良好)

**建议**: 定期运行 `mvn versions:display-dependency-updates` 检查更新。

### A07:2021 - Identification and Authentication Failures

**发现问题**:
1. ✅ **CRITICAL**: MetricsController 无认证（CRITICAL-01）
2. ✅ **良好**: 使用 `AuthTokenFilter.getUserId()` 统一认证
3. ⚠️ **缺失**: 无会话超时配置

**评分**: 6/10 (需改进)

**建议**:
- 所有端点强制认证
- 配置会话超时（如30分钟）
- 实现账户锁定机制

### A08:2021 - Software and Data Integrity Failures

**发现问题**:
1. ✅ **HIGH**: 告警规则无持久化，数据完整性无保证（CRITICAL-02）
2. ✅ **良好**: 使用 `@PrePersist` / `@PreUpdate` 自动维护时间戳
3. ⚠️ **缺失**: 无数据变更审计日志

**评分**: 6/10 (需改进)

**建议**:
- 关键操作记录审计日志（谁、何时、改了什么）
- 实现数据版本控制

### A09:2021 - Security Logging and Monitoring Failures

**发现问题**:
1. ✅ **良好**: 完整的API调用日志（SysApiCallLog）
2. ✅ **良好**: 外部API调用日志（ExternalApiCallLog）
3. ✅ **HIGH**: 日志记录敏感信息（HIGH-04）
4. ⚠️ **缺失**: 无实时告警通知机制

**评分**: 7/10 (良好)

**建议**:
- 日志自动脱敏
- 集成告警通知（邮件/企业微信）
- 实时监控异常登录

### A10:2021 - Server-Side Request Forgery (SSRF)

**发现问题**:
1. ✅ **HIGH**: ExternalApiGateway 未验证目标URL（HIGH-03）
2. ⚠️ **风险**: `baseUrl` 可由管理员配置，需白名单验证

**评分**: 5/10 (不足)

**建议**:
- URL白名单验证（仅允许特定域名）
- 禁止访问内网IP（127.0.0.1, 10.x.x.x, 192.168.x.x）
- 使用代理服务器隔离外部请求

## 修复建议

### 立即修复（P0）

1. **[CRITICAL-01] 为 MetricsController 添加认证检查**
   - 文件: `MetricsController.java`
   - 工作量: 0.5人日
   - 风险: 低（仅添加认证检查）

2. **[CRITICAL-03] 修复 API密钥脱敏方法**
   - 文件: `ExternalApiConfigServiceImpl.java`
   - 工作量: 0.5人日
   - 风险: 中（需测试加密解密流程）

### 短期修复（P1）

3. **[CRITICAL-02] 告警规则持久化到数据库**
   - 文件: 新建 `alert_rule` 和 `alert_record` 表
   - 工作量: 2人日
   - 风险: 中（需数据迁移）

4. **[HIGH-02] 使用强类型VO替换Map**
   - 文件: `AlertController.java`, `MonitoringController.java`
   - 工作量: 1人日
   - 风险: 低（向后兼容）

5. **[HIGH-03] 外部API响应验证**
   - 文件: `ExternalApiGatewayImpl.java`
   - 工作量: 1人日
   - 风险: 中（可能影响现有集成）

6. **[HIGH-04] 日志敏感信息脱敏**
   - 文件: `SystemServiceImpl.java`
   - 工作量: 1人日
   - 风险: 低（仅修改日志记录）

### 中期改进（P2）

7. **[MEDIUM-01] 实现全局频率限制**
   - 工作量: 2人日
   - 使用 Resilience4j RateLimiter

8. **[MEDIUM-02] 健康检查端点权限细化**
   - 工作量: 0.5人日
   - 详细信息仅对admin开放

9. **[MEDIUM-05] 启用CSRF保护**
   - 工作量: 1人日
   - 配置 Spring Security CSRF

### 长期优化（P3）

10. **[LOW-01] 添加输入长度限制**
    - 工作量: 1人日
    - 所有VO添加 `@Size` 注解

11. **[LOW-03] 配置化超时时间**
    - 工作量: 0.5人日
    - 从配置文件读取

12. **实现审计日志系统**
    - 工作量: 3人日
    - 记录所有敏感操作

## 总结

**整体安全性**: 良好（76/100），但存在3个严重漏洞需立即修复。

**关键风险**:
1. **未认证的指标端点**：攻击者可获取系统架构信息
2. **告警规则无持久化**：数据丢失风险 + 水平越权
3. **API密钥脱敏错误**：破坏加密数据，导致外部API调用失败

**优点**:
1. ✅ API密钥加密存储（使用 `ApiKeyEncryptionService`）
2. ✅ 完整的API调用日志和同步日志
3. ✅ 使用JPA Specification防止SQL注入
4. ✅ 统一的认证过滤器（`AuthTokenFilter`）

**生产就绪**: ⚠️ **不建议**（需修复3个CRITICAL漏洞）

**预计工作量**: 
- P0（立即修复）: 1人日
- P1（短期修复）: 5人日
- P2（中期改进）: 3.5人日
- P3（长期优化）: 4.5人日
- **总计**: 14人日

**建议优先级**:
1. 立即修复 CRITICAL-01 和 CRITICAL-03（1人日内完成）
2. 1周内完成 CRITICAL-02 和所有 HIGH 级别漏洞
3. 1个月内完成所有 MEDIUM 级别改进
4. 3个月内完成 LOW 级别优化

---

**审计人**: Claude Opus 4  
**审计工具**: 人工代码审查 + OWASP Top 10 2021 + CVSS 3.1  
**下次审计**: 2026-06-08（修复后复审）

