# Wecom 模块安全审计报告

## 审计概述

**模块名称**: wecom  
**审计日期**: 2026-05-08  
**审计标准**: OWASP Top 10 2021 + CVSS 3.1  
**审计范围**: 企业微信集成模块（机器人管理、推送规则、消息日志）

**审计文件**:
- `WecomController.java` - REST API 控制器（11 个端点）
- `WecomServiceImpl.java` - 业务逻辑层（280 行）
- `WcRobotConfig.java` - 机器人配置实体
- `WcPushRule.java` - 推送规则实体
- `WcMessageLog.java` - 消息日志实体
- `WcRobotConfigRepository.java` - 数据访问层

## 安全评分

| 维度 | 得分 | 说明 |
|------|------|------|
| 认证授权 | 12/20 | ⚠️ 缺少资源级权限校验，存在横向越权风险 |
| 数据保护 | 10/20 | ⚠️ Webhook URL 明文存储，缺少敏感数据加密 |
| 输入验证 | 11/15 | ⚠️ 部分输入未充分验证（URL、JSON） |
| 输出编码 | 13/15 | ✅ 基本 JSON 转义到位，但缺少 HTML 编码 |
| 访问控制 | 8/15 | ❌ 严重：缺少资源所有权校验 |
| 日志审计 | 10/15 | ⚠️ 缺少敏感操作审计日志 |
| **总分** | **64/100** | **等级**: 不足（需改进） |

## 漏洞清单

### CRITICAL 严重漏洞

#### 1. 横向越权 - 缺少资源所有权校验
**CVSS 3.1**: 8.1 (HIGH)  
**向量**: AV:N/AC:L/PR:L/UI:N/S:U/C:H/I:H/A:N

**位置**: `WecomController.java` 多个端点

**问题描述**:
```java
// Line 44-48: robotGet 未校验 ownerId
@PostMapping("/robot/get")
public RESTResult<WcRobotConfigVO> robotGet(HttpServletRequest request, @RequestParam Long id) {
    if (AuthTokenFilter.getUserId(request) == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
    RESTResult<WcRobotConfigVO> r = RESTResult.getSuccess(wecomService.getRobotById(id));
    // ❌ 未校验 robot.ownerId == userId，用户 A 可读取用户 B 的机器人配置
}
```

**影响范围**:
- `/robot/get` - 任意用户可读取他人机器人配置（含 Webhook URL）
- `/robot/delete` - 任意用户可删除他人机器人
- `/robot/update-status` - 任意用户可禁用他人机器人
- `/rule/get` - 任意用户可读取他人推送规则
- `/rule/delete` - 任意用户可删除他人推送规则
- `/rule/update-status` - 任意用户可禁用他人推送规则

**攻击场景**:
```bash
# 攻击者枚举 ID 获取所有用户的 Webhook URL
curl -X POST https://api.example.com/api/v1/wecom/robot/get?id=1 \
  -H "Authorization: Bearer <attacker_token>"
# 返回: {"webhookUrl": "https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=VICTIM_SECRET"}
```

**修复建议** (P0):
```java
// WecomServiceImpl.java - 在所有 getById 方法中添加 ownerId 校验
public WcRobotConfigVO getRobotById(Long id, Long userId) {
    WcRobotConfig robot = robotConfigRepository.findByIdAndDeleted(id, 0)
        .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "机器人配置不存在"));
    if (!robot.getOwnerId().equals(userId)) {
        throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问此资源");
    }
    return toRobotVO(robot);
}
```

---

#### 2. Webhook URL 明文存储
**CVSS 3.1**: 7.5 (HIGH)  
**向量**: AV:N/AC:L/PR:N/UI:N/S:U/C:H/I:N/A:N

**位置**: `WcRobotConfig.java` Line 29-30

**问题描述**:
```java
@Column(name = "webhook_url", nullable = false, length = 512)
private String webhookUrl;
// ❌ Webhook URL 包含敏感 key 参数，明文存储在数据库
// 示例: https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=693a91f6-7xxx-4bc4-97a0-0ec2sifa5aaa
```

**影响**:
- 数据库泄露时，攻击者可直接使用 Webhook URL 发送任意消息
- 数据库备份、日志文件可能包含明文 URL
- DBA 或运维人员可直接查看敏感凭证

**修复建议** (P0):
```java
// 1. 使用 Spring Crypto 加密存储
@Convert(converter = WebhookUrlEncryptor.class)
private String webhookUrl;

// 2. 实现自定义加密器
@Converter
public class WebhookUrlEncryptor implements AttributeConverter<String, String> {
    @Autowired
    private TextEncryptor encryptor; // 使用 AES-256-GCM
    
    @Override
    public String convertToDatabaseColumn(String attribute) {
        return encryptor.encrypt(attribute);
    }
    
    @Override
    public String convertToEntityAttribute(String dbData) {
        return encryptor.decrypt(dbData);
    }
}
```

---

### HIGH 高危漏洞

#### 3. SSRF - 未验证的 Webhook URL
**CVSS 3.1**: 7.1 (HIGH)  
**向量**: AV:N/AC:L/PR:L/UI:N/S:C/C:L/I:L/A:L

**位置**: `WecomServiceImpl.java` Line 201

**问题描述**:
```java
// Line 183-201: sendMessage 方法
ResponseEntity<String> response = restTemplate.postForEntity(robot.getWebhookUrl(), request, String.class);
// ❌ 未验证 webhookUrl 的目标域名，可能被用于 SSRF 攻击
```

**攻击场景**:
```java
// 攻击者创建机器人，指向内网服务
POST /api/v1/wecom/robot/save
{
  "robotName": "恶意机器人",
  "webhookUrl": "http://localhost:8080/actuator/shutdown",  // 攻击内网服务
  "robotType": "custom"
}

// 触发推送，攻击内网
POST /api/v1/wecom/push
{
  "robotId": 123,
  "messageContent": "trigger"
}
```

**修复建议** (P1):
```java
// WecomServiceImpl.java - 添加 URL 白名单验证
private static final Set<String> ALLOWED_HOSTS = Set.of(
    "qyapi.weixin.qq.com",
    "qyapi.wechat.com"
);

private void validateWebhookUrl(String url) {
    try {
        URI uri = new URI(url);
        if (!"https".equals(uri.getScheme())) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "Webhook URL 必须使用 HTTPS");
        }
        if (!ALLOWED_HOSTS.contains(uri.getHost())) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "Webhook URL 域名不在白名单");
        }
    } catch (URISyntaxException e) {
        throw new BusinessException(ErrorCode.INVALID_PARAM, "Webhook URL 格式错误");
    }
}

// 在 saveRobot 中调用
public long saveRobot(WcRobotConfigSaveVO vo) {
    validateWebhookUrl(vo.getWebhookUrl()); // 添加验证
    // ...
}
```

---

#### 4. JSON 注入 - 不完整的转义
**CVSS 3.1**: 6.5 (MEDIUM)  
**向量**: AV:N/AC:L/PR:L/UI:N/S:U/C:N/I:H/A:N

**位置**: `WecomServiceImpl.java` Line 228-231

**问题描述**:
```java
private String escapeJson(String s) {
    if (s == null) return "";
    return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    // ❌ 缺少对 \t, \b, \f, \u0000 等控制字符的转义
    // ❌ 未处理 Unicode 控制字符（U+0000 到 U+001F）
}
```

**攻击场景**:
```java
// 攻击者发送包含特殊字符的消息
POST /api/v1/wecom/push
{
  "robotId": 1,
  "messageContent": "测试\u0000\u0008\u000c消息"  // 包含 NULL, BS, FF 控制字符
}
// 可能导致 JSON 解析错误或注入攻击
```

**修复建议** (P1):
```java
// 使用成熟的 JSON 库替代手动转义
private String buildWecomPayload(String messageType, String content) {
    String type = messageType != null ? messageType : "text";
    
    // 使用 Jackson ObjectMapper（已在项目中）
    ObjectMapper mapper = new ObjectMapper();
    Map<String, Object> payload = new HashMap<>();
    payload.put("msgtype", type);
    
    if ("markdown".equals(type)) {
        payload.put("markdown", Map.of("content", content));
    } else {
        payload.put("text", Map.of("content", content));
    }
    
    try {
        return mapper.writeValueAsString(payload);
    } catch (JsonProcessingException e) {
        throw new BusinessException(ErrorCode.INTERNAL_ERROR, "消息序列化失败");
    }
}
```

---

#### 5. 缺少速率限制
**CVSS 3.1**: 6.5 (MEDIUM)  
**向量**: AV:N/AC:L/PR:L/UI:N/S:U/C:N/I:N/A:H

**位置**: `WecomController.java` Line 153-162

**问题描述**:
```java
@PostMapping("/push")
public RESTResult<Void> push(HttpServletRequest request, @Valid @RequestBody WcSendMessageVO vo) {
    // ❌ 无速率限制，攻击者可短时间内发送大量消息
    wecomService.sendMessage(vo, userId);
}
```

**影响**:
- 攻击者可滥用 API 发送垃圾消息
- 可能触发企业微信的频率限制，导致正常消息无法发送
- 消耗服务器资源和网络带宽

**修复建议** (P1):
```java
// 使用 Resilience4j RateLimiter
@RateLimiter(name = "wecomPush", fallbackMethod = "pushFallback")
@PostMapping("/push")
public RESTResult<Void> push(HttpServletRequest request, @Valid @RequestBody WcSendMessageVO vo) {
    Long userId = AuthTokenFilter.getUserId(request);
    if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
    wecomService.sendMessage(vo, userId);
    return RESTResult.updateSuccess(null);
}

public RESTResult<Void> pushFallback(HttpServletRequest request, WcSendMessageVO vo, RateLimitExceededException e) {
    return RESTResult.error(ErrorCode.RATE_LIMIT_EXCEEDED, "发送频率过高，请稍后再试");
}

// application.yml 配置
// resilience4j:
//   ratelimiter:
//     instances:
//       wecomPush:
//         limit-for-period: 10
//         limit-refresh-period: 60s
//         timeout-duration: 0s
```

---

### MEDIUM 中危漏洞

#### 6. 缺少输入长度限制
**CVSS 3.1**: 5.3 (MEDIUM)  
**向量**: AV:N/AC:L/PR:N/UI:N/S:U/C:N/I:N/A:L

**位置**: `WcSendMessageVO.java` Line 12-13

**问题描述**:
```java
@NotBlank(message = "消息内容不能为空")
private String messageContent;
// ❌ 未限制长度，可能导致数据库溢出或内存耗尽
```

**修复建议** (P2):
```java
@NotBlank(message = "消息内容不能为空")
@Size(max = 4096, message = "消息内容不能超过 4096 字符")
private String messageContent;
```

#### 7. 错误消息泄露敏感信息
**CVSS 3.1**: 5.3 (MEDIUM)  
**向量**: AV:N/AC:L/PR:N/UI:N/S:U/C:L/I:N/A:N

**位置**: `WecomServiceImpl.java` Line 208-210

**问题描述**:
```java
catch (Exception e) {
    log.setStatus(0);
    log.setErrorMessage(e.getMessage() != null ? e.getMessage().substring(0, Math.min(e.getMessage().length(), 512)) : "未知错误");
    // ⚠️ 异常消息可能包含敏感信息（堆栈跟踪、内部路径、SQL 语句等）
}
```

**修复建议** (P2):
```java
catch (Exception e) {
    log.setStatus(0);
    // 记录完整错误到日志系统
    logger.error("企业微信消息发送失败: robotId={}, ruleId={}", vo.getRobotId(), vo.getRuleId(), e);
    // 数据库只存储安全的错误摘要
    String safeMessage = e instanceof HttpClientErrorException ? 
        "HTTP " + ((HttpClientErrorException) e).getStatusCode() : "发送失败";
    log.setErrorMessage(safeMessage);
}
```

---

#### 8. 缺少 Webhook URL 格式验证
**CVSS 3.1**: 4.3 (MEDIUM)  
**向量**: AV:N/AC:L/PR:L/UI:N/S:U/C:N/I:L/A:N

**位置**: `WcRobotConfigSaveVO.java` Line 14-15

**问题描述**:
```java
@NotBlank(message = "Webhook 地址不能为空")
private String webhookUrl;
// ❌ 仅检查非空，未验证 URL 格式
```

**修复建议** (P2):
```java
@NotBlank(message = "Webhook 地址不能为空")
@Pattern(regexp = "^https://qyapi\\.weixin\\.qq\\.com/.*", message = "Webhook URL 格式错误")
@Size(max = 512, message = "Webhook URL 长度不能超过 512")
private String webhookUrl;
```

---

### LOW 低危漏洞

#### 9. 缺少审计日志
**CVSS 3.1**: 3.1 (LOW)  
**向量**: AV:N/AC:H/PR:L/UI:N/S:U/C:N/I:L/A:N

**位置**: `WecomServiceImpl.java` 所有修改操作

**问题描述**:
- 机器人删除、状态变更无审计日志
- 推送规则删除、状态变更无审计日志
- 无法追溯谁在何时进行了敏感操作

**修复建议** (P3):
```java
// 使用 Spring AOP 记录审计日志
@Aspect
@Component
public class WecomAuditAspect {
    
    @AfterReturning("@annotation(cn.gaifan.douyinOperations.common.annotation.Audit)")
    public void logAudit(JoinPoint joinPoint) {
        // 记录操作人、操作时间、操作类型、资源 ID
        auditLogService.log(
            userId, 
            joinPoint.getSignature().getName(), 
            Arrays.toString(joinPoint.getArgs())
        );
    }
}

// 在敏感方法上添加注解
@Audit
public void deleteRobot(Long id) { ... }
```

---

#### 10. 缓存键冲突风险
**CVSS 3.1**: 2.6 (LOW)  
**向量**: AV:N/AC:H/PR:L/UI:N/S:U/C:L/I:N/A:N

**位置**: `WecomServiceImpl.java` Line 65, 109

**问题描述**:
```java
@Cacheable(value = "wecom:robot", key = "#id", unless = "#result == null")
public WcRobotConfigVO getRobotById(Long id) { ... }
// ⚠️ 缓存键仅使用 id，未包含 ownerId，可能导致跨用户缓存污染
```

**修复建议** (P3):
```java
@Cacheable(value = "wecom:robot", key = "#id + ':' + #userId", unless = "#result == null")
public WcRobotConfigVO getRobotById(Long id, Long userId) { ... }
```

---

## OWASP Top 10 检查

### A01:2021 - Broken Access Control ❌ 不合格
**评分**: 2/10

**发现的问题**:
1. **横向越权** (CRITICAL): 所有 `get/delete/update-status` 端点缺少资源所有权校验
2. **缺少细粒度权限**: 无角色权限控制（RBAC），所有登录用户权限相同

**代码示例**:
```java
// ❌ 错误示例 - WecomController.java Line 64-70
@PostMapping("/robot/delete")
public RESTResult<Void> robotDelete(HttpServletRequest request, @RequestParam Long id) {
    if (AuthTokenFilter.getUserId(request) == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
    wecomService.deleteRobot(id);  // 未校验 robot.ownerId == userId
    return RESTResult.deleteSuccess(null);
}
```

**修复要求**:
- 所有资源操作必须校验 `ownerId == userId`
- 实现基于角色的访问控制（管理员可跨用户操作）
- 添加资源级权限注解 `@PreAuthorize("@wecomSecurity.canAccess(#id, principal.userId)")`

---

### A02:2021 - Cryptographic Failures ⚠️ 需改进
**评分**: 5/10

**发现的问题**:
1. **敏感数据明文存储** (HIGH): Webhook URL 包含密钥，明文存储在数据库
2. **缺少传输层保护**: 未强制 HTTPS（虽然企业微信要求 HTTPS，但代码未验证）

**修复要求**:
- 使用 AES-256-GCM 加密 `webhookUrl` 字段
- 在 `saveRobot` 时验证 URL 必须使用 HTTPS
- 考虑使用 Spring Vault 管理密钥

---

### A03:2021 - Injection ⚠️ 需改进
**评分**: 6/10

**发现的问题**:
1. **JSON 注入** (MEDIUM): 手动拼接 JSON，转义不完整
2. **SQL 注入**: ✅ 使用 JPA Specification，无 SQL 注入风险

**代码示例**:
```java
// ⚠️ 风险代码 - WecomServiceImpl.java Line 223
return "{\"msgtype\":\"text\",\"text\":{\"content\":\"" + escapeJson(content) + "\"}}";
// 应使用 Jackson ObjectMapper
```

**修复要求**:
- 使用 Jackson/Gson 等成熟库序列化 JSON
- 移除手动 `escapeJson` 方法

---

### A04:2021 - Insecure Design ⚠️ 需改进
**评分**: 6/10

**发现的问题**:
1. **SSRF 风险** (HIGH): 未验证 Webhook URL 目标域名
2. **缺少速率限制**: 可被滥用发送大量消息
3. **缺少幂等性**: 消息重复发送无保护机制

**修复要求**:
- 实现 Webhook URL 白名单验证
- 添加 RateLimiter（每用户每分钟 10 次）
- 考虑添加消息去重机制（基于内容哈希）

---

### A05:2021 - Security Misconfiguration ✅ 合格
**评分**: 8/10

**检查结果**:
- ✅ 使用 `@Valid` 进行输入验证
- ✅ 使用 `@Transactional` 保证事务一致性
- ✅ 使用 `@SQLRestriction` 实现逻辑删除
- ⚠️ 缺少全局异常处理器（应该在 common 模块已实现）

---

### A06:2021 - Vulnerable Components ✅ 合格
**评分**: 9/10

**检查结果**:
- ✅ 使用 Spring Boot 3.3.7（最新稳定版）
- ✅ 使用 Jakarta EE 9+（无 javax 遗留依赖）
- ✅ 使用 Resilience4j 2.1.0（最新版）
- ⚠️ 建议定期运行 `mvn dependency:tree` 检查依赖漏洞

---

### A07:2021 - Authentication Failures ⚠️ 需改进
**评分**: 6/10

**发现的问题**:
1. **认证检查不一致**: 部分端点返回 401，部分返回 null
2. **缺少会话管理**: 无 token 过期时间验证
3. **缺少多因素认证**: 敏感操作（删除机器人）无二次验证

**代码示例**:
```java
// ⚠️ 不一致的认证检查
// Line 34: return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
// Line 45: if (AuthTokenFilter.getUserId(request) == null) return RESTResult.error(...);
```

**修复要求**:
- 统一使用 Spring Security `@PreAuthorize("isAuthenticated()")`
- 在 AuthTokenFilter 中验证 token 过期时间
- 敏感操作添加二次验证（如删除机器人需输入名称确认）

---

### A08:2021 - Software and Data Integrity ✅ 合格
**评分**: 8/10

**检查结果**:
- ✅ 使用 `@PrePersist/@PreUpdate` 自动维护时间戳
- ✅ 使用逻辑删除（`deleted` 字段）
- ✅ 使用 `@Transactional` 保证数据一致性
- ⚠️ 缺少消息签名验证（企业微信回调时）

---

### A09:2021 - Security Logging Failures ⚠️ 需改进
**评分**: 5/10

**发现的问题**:
1. **缺少审计日志** (LOW): 敏感操作无审计记录
2. **日志信息不足**: 无操作人、操作时间、操作结果
3. **缺少告警机制**: 异常操作（频繁失败）无告警

**修复要求**:
- 实现审计日志（记录所有 CUD 操作）
- 集成 ELK/Loki 进行日志聚合
- 配置告警规则（如 5 分钟内失败 10 次）

---

### A10:2021 - SSRF ❌ 不合格
**评分**: 3/10

**发现的问题**:
1. **未验证的 URL** (HIGH): `restTemplate.postForEntity(robot.getWebhookUrl(), ...)` 可被用于 SSRF
2. **可访问内网**: 攻击者可指向 `http://localhost:8080/actuator/shutdown`
3. **无超时限制**: 可能导致资源耗尽

**修复要求**:
- 实现 URL 白名单（仅允许 `qyapi.weixin.qq.com`）
- 禁止访问内网 IP（10.0.0.0/8, 172.16.0.0/12, 192.168.0.0/16, 127.0.0.0/8）
- 配置 RestTemplate 超时（连接超时 5s，读取超时 10s）

---

## 修复建议

### 立即修复（P0）- 预计 3 人日

#### 1. 修复横向越权漏洞
**文件**: `WecomServiceImpl.java`

```java
// 为所有 getById 方法添加 userId 参数和校验
public WcRobotConfigVO getRobotById(Long id, Long userId) {
    WcRobotConfig robot = robotConfigRepository.findByIdAndDeleted(id, 0)
        .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "机器人配置不存在"));
    if (!robot.getOwnerId().equals(userId)) {
        throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问此资源");
    }
    return toRobotVO(robot);
}

public void deleteRobot(Long id, Long userId) {
    WcRobotConfig entity = robotConfigRepository.findByIdAndDeleted(id, 0)
        .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "机器人配置不存在"));
    if (!entity.getOwnerId().equals(userId)) {
        throw new BusinessException(ErrorCode.FORBIDDEN, "无权删除此资源");
    }
    entity.setDeleted(1);
    robotConfigRepository.save(entity);
}

// 同样修复 updateRobotStatus, getRuleById, deleteRule, updateRuleStatus
```

**文件**: `WecomController.java`

```java
// 更新所有调用，传入 userId
@PostMapping("/robot/get")
public RESTResult<WcRobotConfigVO> robotGet(HttpServletRequest request, @RequestParam Long id) {
    Long userId = AuthTokenFilter.getUserId(request);
    if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
    RESTResult<WcRobotConfigVO> r = RESTResult.getSuccess(wecomService.getRobotById(id, userId));
    r.setTraceId(MDC.get("traceId"));
    return r;
}
```

---

#### 2. 加密 Webhook URL
**文件**: 新建 `WebhookUrlEncryptor.java`

```java
@Component
@Converter
public class WebhookUrlEncryptor implements AttributeConverter<String, String> {
    
    @Value("${wecom.webhook.encryption.key}")
    private String encryptionKey;
    
    private TextEncryptor encryptor;
    
    @PostConstruct
    public void init() {
        this.encryptor = Encryptors.text(encryptionKey, KeyGenerators.string().generateKey());
    }
    
    @Override
    public String convertToDatabaseColumn(String attribute) {
        return attribute == null ? null : encryptor.encrypt(attribute);
    }
    
    @Override
    public String convertToEntityAttribute(String dbData) {
        return dbData == null ? null : encryptor.decrypt(dbData);
    }
}
```

**文件**: `WcRobotConfig.java`

```java
@Convert(converter = WebhookUrlEncryptor.class)
@Column(name = "webhook_url", nullable = false, length = 1024) // 加密后长度增加
private String webhookUrl;
```

**文件**: `application.yml`

```yaml
wecom:
  webhook:
    encryption:
      key: ${WECOM_ENCRYPTION_KEY:changeme-32-chars-minimum-key}
```

---

### 短期修复（P1）- 预计 2 人日

#### 3. 实现 SSRF 防护
**文件**: `WecomServiceImpl.java`

```java
private static final Set<String> ALLOWED_HOSTS = Set.of(
    "qyapi.weixin.qq.com",
    "qyapi.wechat.com"
);

private static final Pattern INTERNAL_IP_PATTERN = Pattern.compile(
    "^(10\\.|172\\.(1[6-9]|2[0-9]|3[01])\\.|192\\.168\\.|127\\.)"
);

private void validateWebhookUrl(String url) {
    try {
        URI uri = new URI(url);
        
        // 1. 必须使用 HTTPS
        if (!"https".equals(uri.getScheme())) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "Webhook URL 必须使用 HTTPS");
        }
        
        // 2. 域名白名单
        if (!ALLOWED_HOSTS.contains(uri.getHost())) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "Webhook URL 域名不在白名单");
        }
        
        // 3. 禁止内网 IP（防止 DNS 重绑定攻击）
        InetAddress addr = InetAddress.getByName(uri.getHost());
        if (addr.isLoopbackAddress() || addr.isLinkLocalAddress() || 
            INTERNAL_IP_PATTERN.matcher(addr.getHostAddress()).find()) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "禁止访问内网地址");
        }
        
    } catch (URISyntaxException | UnknownHostException e) {
        throw new BusinessException(ErrorCode.INVALID_PARAM, "Webhook URL 格式错误");
    }
}

public long saveRobot(WcRobotConfigSaveVO vo) {
    validateWebhookUrl(vo.getWebhookUrl()); // 添加验证
    // ... 原有逻辑
}
```

---

#### 4. 添加速率限制
**文件**: `application.yml`

```yaml
resilience4j:
  ratelimiter:
    instances:
      wecomPush:
        limit-for-period: 10
        limit-refresh-period: 60s
        timeout-duration: 0s
```

**文件**: `WecomController.java`

```java
@RateLimiter(name = "wecomPush", fallbackMethod = "pushFallback")
@PostMapping("/push")
public RESTResult<Void> push(HttpServletRequest request, @Valid @RequestBody WcSendMessageVO vo) {
    Long userId = AuthTokenFilter.getUserId(request);
    if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
    wecomService.sendMessage(vo, userId);
    RESTResult<Void> r = RESTResult.updateSuccess(null);
    r.setTraceId(MDC.get("traceId"));
    return r;
}

public RESTResult<Void> pushFallback(HttpServletRequest request, WcSendMessageVO vo, RateLimitExceededException e) {
    RESTResult<Void> r = RESTResult.error(ErrorCode.RATE_LIMIT_EXCEEDED, "发送频率过高，请稍后再试");
    r.setTraceId(MDC.get("traceId"));
    return r;
}
```

---

#### 5. 修复 JSON 注入
**文件**: `WecomServiceImpl.java`

```java
@Resource
private ObjectMapper objectMapper;

private String buildWecomPayload(String messageType, String content) {
    String type = messageType != null ? messageType : "text";
    
    Map<String, Object> payload = new HashMap<>();
    payload.put("msgtype", type);
    
    if ("markdown".equals(type)) {
        payload.put("markdown", Map.of("content", content));
    } else {
        payload.put("text", Map.of("content", content));
    }
    
    try {
        return objectMapper.writeValueAsString(payload);
    } catch (JsonProcessingException e) {
        throw new BusinessException(ErrorCode.INTERNAL_ERROR, "消息序列化失败");
    }
}

// 删除 escapeJson 方法
```

---

### 中期改进（P2）- 预计 1 人日

#### 6. 增强输入验证
**文件**: `WcRobotConfigSaveVO.java`

```java
@NotBlank(message = "Webhook 地址不能为空")
@Pattern(regexp = "^https://qyapi\\.weixin\\.qq\\.com/.*", message = "Webhook URL 格式错误")
@Size(max = 512, message = "Webhook URL 长度不能超过 512")
private String webhookUrl;

@Size(max = 128, message = "机器人名称不能超过 128 字符")
private String robotName;

@Size(max = 256, message = "描述不能超过 256 字符")
private String description;
```

**文件**: `WcSendMessageVO.java`

```java
@NotBlank(message = "消息内容不能为空")
@Size(max = 4096, message = "消息内容不能超过 4096 字符")
private String messageContent;

@Pattern(regexp = "^(text|markdown)$", message = "消息类型只能是 text 或 markdown")
private String messageType = "text";
```

---

#### 7. 改进错误处理
**文件**: `WecomServiceImpl.java`

```java
private static final Logger logger = LoggerFactory.getLogger(WecomServiceImpl.class);

try {
    // ... 发送逻辑
} catch (HttpClientErrorException e) {
    log.setStatus(0);
    log.setErrorMessage("HTTP " + e.getStatusCode().value());
    logger.error("企业微信消息发送失败: robotId={}, statusCode={}", 
        vo.getRobotId(), e.getStatusCode(), e);
} catch (Exception e) {
    log.setStatus(0);
    log.setErrorMessage("发送失败");
    logger.error("企业微信消息发送异常: robotId={}", vo.getRobotId(), e);
}
```

---

### 长期优化（P3）- 预计 2 人日

#### 8. 实现审计日志
**文件**: 新建 `WecomAuditAspect.java`

```java
@Aspect
@Component
public class WecomAuditAspect {
    
    @Resource
    private AuditLogService auditLogService;
    
    @AfterReturning("execution(* cn.gaifan.douyinOperations.module.wecom.service.impl.WecomServiceImpl.delete*(..))")
    public void logDelete(JoinPoint joinPoint) {
        Long userId = getCurrentUserId();
        String method = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();
        
        auditLogService.log(AuditLog.builder()
            .userId(userId)
            .module("wecom")
            .action(method)
            .resourceId(args.length > 0 ? String.valueOf(args[0]) : null)
            .timestamp(Timestamp.from(Instant.now()))
            .build());
    }
    
    // 同样记录 updateStatus, saveRobot, saveRule 等操作
}
```

---

#### 9. 配置 RestTemplate 超时
**文件**: 新建 `WecomConfig.java`

```java
@Configuration
public class WecomConfig {
    
    @Bean("wecomRestTemplate")
    public RestTemplate wecomRestTemplate() {
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
        factory.setConnectTimeout(5000);  // 连接超时 5s
        factory.setReadTimeout(10000);    // 读取超时 10s
        
        return new RestTemplate(factory);
    }
}
```

**文件**: `WecomServiceImpl.java`

```java
@Resource
@Qualifier("wecomRestTemplate")
private RestTemplate restTemplate;
```

---

#### 10. 优化缓存策略
**文件**: `WecomServiceImpl.java`

```java
@Cacheable(value = "wecom:robot", key = "#id + ':' + #userId", unless = "#result == null")
public WcRobotConfigVO getRobotById(Long id, Long userId) { ... }

@CacheEvict(value = "wecom:robot", key = "#result + ':' + #vo.ownerId")
public long saveRobot(WcRobotConfigSaveVO vo) { ... }
```

---

## 总结

### 整体安全性
**评级**: ⚠️ **不足（需改进）**

wecom 模块存在 **2 个严重漏洞**（横向越权、明文存储敏感数据）和 **3 个高危漏洞**（SSRF、JSON 注入、缺少速率限制），必须在生产部署前修复。

### 关键风险

1. **横向越权** (CRITICAL): 任意用户可读取/删除/修改他人的机器人配置和推送规则
2. **敏感数据泄露** (HIGH): Webhook URL 明文存储，数据库泄露时可被直接利用
3. **SSRF 攻击** (HIGH): 未验证的 URL 可被用于攻击内网服务
4. **资源滥用** (MEDIUM): 缺少速率限制，可被滥用发送大量消息

### 生产就绪
❌ **不就绪** - 必须修复 P0 和 P1 问题后才能上线

### 预计工作量
- **P0（立即修复）**: 3 人日
- **P1（短期修复）**: 2 人日
- **P2（中期改进）**: 1 人日
- **P3（长期优化）**: 2 人日
- **总计**: 8 人日

### 修复优先级
1. **第一周**: 修复横向越权 + 加密 Webhook URL（P0）
2. **第二周**: 实现 SSRF 防护 + 速率限制 + JSON 注入修复（P1）
3. **第三周**: 增强输入验证 + 改进错误处理（P2）
4. **第四周**: 实现审计日志 + 优化配置（P3）

### 复测建议
修复完成后，建议进行以下测试：
1. **渗透测试**: 使用 Burp Suite 测试横向越权和 SSRF
2. **负载测试**: 验证速率限制是否生效
3. **安全扫描**: 使用 OWASP ZAP 进行自动化扫描
4. **代码审计**: 使用 SonarQube 进行静态代码分析

---

**审计人**: Claude Opus 4  
**审计日期**: 2026-05-08  
**下次审计**: 2026-08-08（修复完成后 3 个月）
