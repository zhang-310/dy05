# A/B Test 模块安全审计报告

**生成日期**: 2026-05-09  
**审计范围**: douyin-operations-intelligence/module/abtest  
**审计标准**: OWASP Top 10 2021 + CVSS 3.1

---

## 执行摘要

| 维度 | 评分 | 等级 | 说明 |
|------|------|------|------|
| **总体安全性** | 72/100 | Grade C | 存在严重数据隔离缺陷，事件数据隐私保护不足 |
| 认证与授权 | 12/20 | D | 缺少 owner_id 数据隔离校验（Critical） |
| 数据保护 | 14/20 | C | user_fingerprint 明文存储，事件数据无脱敏 |
| 输入验证 | 16/20 | B | 基础校验完善，缺少业务规则校验 |
| 会话管理 | 18/20 | A | Bearer Token 认证规范 |
| 日志与监控 | 12/20 | D | 缺少安全事件日志，异常处理吞掉错误 |

**关键发现**:
- 🔴 **Critical: 1 个** (CVSS 9.1) - 缺少 owner_id 数据隔离，可越权操作他人实验
- 🟠 **High: 3 个** (CVSS 7.0-8.9) - 事件去重无并发保护、user_fingerprint 明文存储、缺少审计日志
- 🟡 **Medium: 5 个** (CVSS 4.0-6.9) - 业务规则校验缺失、统计数据完整性风险
- 🟢 **Low: 2 个** (CVSS < 4.0) - 缓存键无版本号、错误信息泄露

**OWASP Top 10 覆盖**:
- ✅ A01:2021 - Broken Access Control (Critical 发现)
- ✅ A02:2021 - Cryptographic Failures (High 发现)
- ✅ A03:2021 - Injection (无问题)
- ✅ A04:2021 - Insecure Design (Medium 发现)
- ✅ A05:2021 - Security Misconfiguration (Medium 发现)
- ✅ A06:2021 - Vulnerable Components (无问题)
- ✅ A07:2021 - Identification and Authentication Failures (无问题)
- ✅ A08:2021 - Software and Data Integrity Failures (Medium 发现)
- ✅ A09:2021 - Security Logging and Monitoring Failures (High 发现)
- ✅ A10:2021 - Server-Side Request Forgery (无问题)

---

## 1. 认证与授权 (A01:2021 - Broken Access Control)

### 1.1 认证机制

**当前实现**:
```java
// AbTestController.java:39-40
Long userId = AuthTokenFilter.getUserId(request);
if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
```

**优点**:
- ✅ 所有接口要求登录（Bearer Token 认证）
- ✅ 使用统一的 `AuthTokenFilter` 提取 userId
- ✅ 401 错误统一返回 `ErrorCode.UNAUTHORIZED`

**问题**: 无

### 1.2 授权控制

**🔴 Critical 漏洞 #1: 缺少 owner_id 数据隔离校验**

**CVSS 3.1 评分**: 9.1 (Critical)
- **向量**: `CVSS:3.1/AV:N/AC:L/PR:L/UI:N/S:U/C:H/I:H/A:N`
- **攻击向量 (AV:N)**: 网络可达
- **攻击复杂度 (AC:L)**: 低，仅需知道实验 ID
- **所需权限 (PR:L)**: 需要登录（低权限用户）
- **用户交互 (UI:N)**: 无需用户交互
- **影响范围 (S:U)**: 未改变
- **机密性影响 (C:H)**: 高，可读取他人实验数据
- **完整性影响 (I:H)**: 高，可修改/删除他人实验
- **可用性影响 (A:N)**: 无

**受影响接口**:
```java
// AbTestController.java:50-56 - getById 未校验 owner_id
@PostMapping("/experiment/get")
public RESTResult<AbExperimentVO> get(HttpServletRequest request, @RequestParam Long id) {
    if (AuthTokenFilter.getUserId(request) == null) return RESTResult.error(...);
    RESTResult<AbExperimentVO> r = RESTResult.getSuccess(abTestService.getById(id));
    // ❌ 未校验 owner_id，恶意用户可以查看他人的实验
}

// AbTestController.java:70-78 - delete 未校验 owner_id
@PostMapping("/experiment/delete")
public RESTResult<Void> delete(HttpServletRequest request, @RequestParam Long id) {
    if (AuthTokenFilter.getUserId(request) == null) return RESTResult.error(...);
    abTestService.delete(id);
    // ❌ 未校验 owner_id，恶意用户可以删除他人的实验
}

// AbTestController.java:80-89 - updateStatus 未校验 owner_id
// AbTestController.java:91-99 - setWinner 未校验 owner_id
// AbTestController.java:103-110 - saveVariant 未校验实验所有权
// AbTestController.java:112-120 - deleteVariant 未校验实验所有权
// AbTestController.java:124-132 - recordEvent 未校验实验所有权
```

**攻击场景**:
1. 攻击者注册账号（userId=100）
2. 攻击者遍历实验 ID（1-10000），调用 `/experiment/get?id=1`
3. 成功读取其他用户的实验配置、变体内容、统计数据
4. 攻击者调用 `/experiment/delete?id=1` 删除竞争对手的实验
5. 攻击者调用 `/experiment/set-winner` 篡改实验结论

**业务影响**:
- **数据泄露**: 竞争对手可窃取 A/B 测试策略、话术风格、转化率数据
- **数据篡改**: 恶意用户可删除或修改他人实验，导致业务决策错误
- **合规风险**: 违反 GDPR、等保 2.0 数据隔离要求

**修复建议** (工作量: 0.5 人日):
```java
// AbTestServiceImpl.java - 添加所有权校验方法
private void checkOwnership(Long experimentId, Long userId) {
    AbExperiment exp = experimentRepository.findByIdAndDeleted(experimentId, 0)
        .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "实验不存在"));
    if (!exp.getOwnerId().equals(userId)) {
        throw new BusinessException(ErrorCode.FORBIDDEN, "无权操作此实验");
    }
}

// 在所有修改接口中调用
public AbExperimentVO getById(Long id, Long userId) {
    checkOwnership(id, userId);
    return getById(id);
}

public void delete(Long id, Long userId) {
    checkOwnership(id, userId);
    delete(id);
}

// 变体操作需校验实验所有权
public long saveVariant(AbVariantSaveVO vo, Long userId) {
    checkOwnership(vo.getExperimentId(), userId);
    return saveVariant(vo);
}
```

---

## 2. 数据保护 (A02:2021 - Cryptographic Failures)

### 2.1 敏感数据加密

**🟠 High 漏洞 #2: user_fingerprint 明文存储**

**CVSS 3.1 评分**: 7.5 (High)
- **向量**: `CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:U/C:H/I:N/A:N`
- **攻击向量 (AV:N)**: 网络可达
- **攻击复杂度 (AC:L)**: 低，数据库泄露即可获取
- **所需权限 (PR:N)**: 无需权限（数据库泄露场景）
- **机密性影响 (C:H)**: 高，泄露用户设备指纹

**问题代码**:
```java
// AbTestServiceImpl.java:186 - 直接存储 user_fingerprint
event.setUserFingerprint(vo.getUserFingerprint());
// ❌ user_fingerprint 可能包含设备指纹、浏览器指纹等隐私数据
```

**数据库存储**:
```sql
-- ab_event 表
user_fingerprint VARCHAR(64) NOT NULL  -- 明文存储
```

**隐私风险**:
- `user_fingerprint` 可能包含：浏览器指纹（Canvas/WebGL）、设备 ID、IP 地址哈希
- 数据库泄露时，攻击者可关联用户行为，进行用户画像
- 违反 GDPR 第 32 条（数据加密要求）

**修复建议** (工作量: 0.3 人日):
```java
// AbTestServiceImpl.java
import org.apache.commons.codec.digest.DigestUtils;

@Transactional(rollbackFor = Exception.class)
public void recordEvent(AbEventSaveVO vo) {
    // SHA256 哈希 user_fingerprint
    String hashedFingerprint = DigestUtils.sha256Hex(vo.getUserFingerprint());
    
    AbEvent event = new AbEvent();
    event.setUserFingerprint(hashedFingerprint);
    // ...
}
```

### 2.2 数据脱敏

**🟡 Medium 漏洞 #3: 事件数据无脱敏**

**CVSS 3.1 评分**: 5.3 (Medium)
- **向量**: `CVSS:3.1/AV:N/AC:L/PR:L/UI:N/S:U/C:L/I:N/A:N`
- **机密性影响 (C:L)**: 低，可查看事件明细

**问题**:
- 事件表无脱敏机制，管理员可查看所有用户行为
- `session_id` 字段可关联用户会话，追踪用户行为路径

**修复建议** (工作量: 0.5 人日):
```java
// 添加数据脱敏注解
@JsonSerialize(using = SensitiveDataSerializer.class)
@Column(name = "user_fingerprint")
private String userFingerprint;

// 日志输出时脱敏
log.info("记录事件: experimentId={}, fingerprint={}", 
    experimentId, maskFingerprint(userFingerprint));
```

---

## 3. 输入验证 (A03:2021 - Injection)

### 3.1 SQL 注入

**优点**:
- ✅ 使用 JPA Specification：参数化查询
- ✅ 使用 `@Query` + `@Param`：参数绑定
- ✅ 无字符串拼接 SQL

**问题**: 无

### 3.2 业务规则校验

**🟡 Medium 漏洞 #4: 缺少业务规则校验**

**CVSS 3.1 评分**: 4.3 (Medium)
- **向量**: `CVSS:3.1/AV:N/AC:L/PR:L/UI:N/S:U/C:N/I:L/A:N`
- **完整性影响 (I:L)**: 低，可创建无效实验

**问题代码**:
```java
// AbTestServiceImpl.java:82-108 - save 方法缺少业务规则校验
public long save(AbExperimentSaveVO vo) {
    // ❌ 缺少校验：
    // 1. 变体数量校验（至少 2 个变体）
    // 2. 变体类型校验（必须有 A 和 B）
    // 3. 实验类型校验（experiment_type 是否合法）
    // 4. 状态转换校验（草稿 → 运行中 → 已完成）
}
```

**攻击场景**:
1. 创建只有 1 个变体的实验（无法对比）
2. 创建 2 个 A 变体（无 B 变体）
3. 直接从草稿跳到已完成（跳过运行中）

**修复建议** (工作量: 0.3 人日):
```java
public long save(AbExperimentSaveVO vo) {
    // 校验变体数量
    if (vo.getVariants() != null && vo.getVariants().size() < 2) {
        throw new BusinessException(ErrorCode.INVALID_PARAMS, "至少需要 2 个变体");
    }
    
    // 校验变体类型
    if (vo.getVariants() != null) {
        Set<String> types = vo.getVariants().stream()
                .map(AbVariantSaveVO::getVariantType)
                .collect(Collectors.toSet());
        if (!types.contains("A") || !types.contains("B")) {
            throw new BusinessException(ErrorCode.INVALID_PARAMS, "必须包含 A 和 B 变体");
        }
    }
    
    // 校验实验类型
    if (!Set.of("video", "live", "copy", "script_style").contains(vo.getExperimentType())) {
        throw new BusinessException(ErrorCode.INVALID_PARAMS, "实验类型不合法");
    }
}
```

---

## 4. 不安全设计 (A04:2021 - Insecure Design)

### 4.1 并发控制

**🟠 High 漏洞 #5: 事件去重无并发保护**

**CVSS 3.1 评分**: 7.4 (High)
- **向量**: `CVSS:3.1/AV:N/AC:H/PR:N/UI:N/S:U/C:N/I:H/A:H`
- **攻击复杂度 (AC:H)**: 高，需要高并发场景
- **完整性影响 (I:H)**: 高，统计数据不准确
- **可用性影响 (A:H)**: 高，数据库性能下降

**问题代码**:
```java
// AbTestServiceImpl.java:178-188 - 查询和插入之间有时间窗口
if (eventRepository.existsByVariantIdAndEventTypeAndUserFingerprint(
        vo.getVariantId(), vo.getEventType(), vo.getUserFingerprint())) {
    return;  // ❌ 查询和插入之间有时间窗口，高并发下可能重复记录
}
AbEvent event = new AbEvent();
// ...
eventRepository.save(event);
```

**攻击场景**:
1. 攻击者并发发送 1000 个相同事件请求
2. 多个请求同时通过 `existsByVariantIdAndEventTypeAndUserFingerprint` 检查
3. 多个请求同时插入事件，导致重复记录
4. 统计数据不准确，影响 A/B 测试结论

**修复建议** (工作量: 0.3 人日):
```sql
-- 添加唯一索引（数据库层面保证去重）
CREATE UNIQUE INDEX idx_ab_event_unique 
ON ab_event (variant_id, event_type, user_fingerprint);
```

```java
// 捕获唯一索引冲突异常
@Transactional(rollbackFor = Exception.class)
public void recordEvent(AbEventSaveVO vo) {
    try {
        AbEvent event = new AbEvent();
        // ...
        eventRepository.save(event);
    } catch (DataIntegrityViolationException e) {
        // 唯一索引冲突，说明已记录过，忽略
        log.debug("[RecordEvent] 事件已存在，忽略: {}", vo);
    }
}
```

### 4.2 统计数据完整性

**🟡 Medium 漏洞 #6: 计数器更新无事务保护**

**CVSS 3.1 评分**: 5.9 (Medium)
- **向量**: `CVSS:3.1/AV:N/AC:H/PR:N/UI:N/S:U/C:N/I:H/A:N`
- **完整性影响 (I:H)**: 高，计数器可能不一致

**问题代码**:
```java
// AbTestServiceImpl.java:191-195 - 事件插入和计数器更新不在同一事务
eventRepository.save(event);  // 事务 1
switch (vo.getEventType()) {
    case "view" -> variantRepository.incrementViewCount(vo.getVariantId());  // 事务 2
    case "click" -> variantRepository.incrementClickCount(vo.getVariantId());
    case "conversion" -> variantRepository.incrementConversionCount(vo.getVariantId());
}
// ❌ 如果计数器更新失败，事件已插入，导致数据不一致
```

**修复建议**: 已在 `@Transactional` 注解保护下，但建议添加重试机制

---

## 5. 安全配置错误 (A05:2021 - Security Misconfiguration)

### 5.1 缓存配置

**🟡 Medium 漏洞 #7: 统计结果缓存无过期时间**

**CVSS 3.1 评分**: 4.3 (Medium)
- **向量**: `CVSS:3.1/AV:N/AC:L/PR:L/UI:N/S:U/C:L/I:N/A:N`
- **机密性影响 (C:L)**: 低，可能返回过期数据

**问题代码**:
```java
// AbTestServiceImpl.java:241 - 统计结果缓存无 TTL
@Cacheable(value = "abtest:statistics", key = "#experimentId", unless = "#result == null")
public AbExperimentStatisticsVO getExperimentStatistics(Long experimentId) {
    // ❌ 实验运行中，数据实时变化，缓存应该有过期时间（如 5 分钟）
}
```

**修复建议** (工作量: 0.2 人日):
```java
// CacheConfig.java
@Bean
public CacheManager abTestCacheManager(RedisConnectionFactory factory) {
    RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
        .entryTtl(Duration.ofMinutes(5));  // 5 分钟过期
    return RedisCacheManager.builder(factory)
        .cacheDefaults(config)
        .build();
}
```

### 5.2 错误信息泄露

**🟢 Low 漏洞 #8: 错误信息可能泄露内部信息**

**CVSS 3.1 评分**: 3.1 (Low)
- **向量**: `CVSS:3.1/AV:N/AC:H/PR:L/UI:N/S:U/C:L/I:N/A:N`

**问题代码**:
```java
// AbTestServiceImpl.java:358-363 - 异常信息直接返回
} catch (Exception e) {
    return new AbStatisticalTestVO(
            0.0, 1.0, 0.0, false, variantA.getVariantId(), variantA.getVariantName(),
            "统计检验失败: " + e.getMessage()  // ❌ 可能泄露内部异常信息
    );
}
```

**修复建议**: 返回通用错误信息，详细信息记录到日志

---

## 6. 易受攻击的组件 (A06:2021 - Vulnerable Components)

**优点**:
- ✅ Spring Boot 3.3.7（最新稳定版）
- ✅ Apache Commons Math3（统计库，无已知漏洞）
- ✅ PostgreSQL 15（最新版本）

**问题**: 无

---

## 7. 身份识别和认证失败 (A07:2021 - Identification and Authentication Failures)

**优点**:
- ✅ 使用 Bearer Token 认证
- ✅ 统一的 `AuthTokenFilter` 提取 userId
- ✅ 所有接口要求登录

**问题**: 无

---

## 8. 软件和数据完整性失败 (A08:2021 - Software and Data Integrity Failures)

### 8.1 数据完整性

**🟡 Medium 漏洞 #9: 缺少数据完整性校验**

**CVSS 3.1 评分**: 5.3 (Medium)
- **向量**: `CVSS:3.1/AV:N/AC:L/PR:L/UI:N/S:U/C:N/I:L/A:N`

**问题**:
- 变体计数器（view_count/click_count/conversion_count）无完整性校验
- 转化率（conversion_rate）字段冗余，可能与计数器不一致

**修复建议**: 定期校验计数器与事件表数据一致性

---

## 9. 安全日志和监控失败 (A09:2021 - Security Logging and Monitoring Failures)

### 9.1 安全事件日志

**🟠 High 漏洞 #10: 缺少安全事件日志**

**CVSS 3.1 评分**: 7.5 (High)
- **向量**: `CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:U/C:N/I:N/A:H`
- **可用性影响 (A:H)**: 高，无法追溯安全事件

**问题**:
- 无登录失败日志
- 无越权访问尝试日志
- 无敏感操作审计日志（删除实验、设置获胜变体）

**修复建议** (工作量: 0.5 人日):
```java
// 添加安全事件日志
@Aspect
@Component
public class SecurityAuditAspect {
    
    @AfterThrowing(pointcut = "execution(* cn.gaifan..abtest..*(..))", throwing = "ex")
    public void logSecurityException(JoinPoint jp, Exception ex) {
        if (ex instanceof BusinessException && 
            ((BusinessException) ex).getCode() == ErrorCode.FORBIDDEN) {
            log.warn("[SecurityAudit] 越权访问尝试: method={}, args={}, user={}", 
                jp.getSignature(), jp.getArgs(), getCurrentUserId());
        }
    }
    
    @AfterReturning("@annotation(Operation) && execution(* delete*(..))")
    public void logDeletion(JoinPoint jp) {
        log.info("[SecurityAudit] 删除操作: method={}, args={}, user={}", 
            jp.getSignature(), jp.getArgs(), getCurrentUserId());
    }
}
```

### 9.2 异常处理

**🟢 Low 漏洞 #11: 自动收敛任务吞掉异常**

**CVSS 3.1 评分**: 3.7 (Low)
- **向量**: `CVSS:3.1/AV:L/AC:H/PR:H/UI:N/S:U/C:N/I:N/A:H`

**问题代码**:
```java
// AbTestServiceImpl.java:424-427
for (AbExperiment experiment : running) {
    try {
        // ... 收敛逻辑
    } catch (Exception e) {
        // skip individual failures - ❌ 吞掉异常，无日志记录
    }
}
```

**修复建议**:
```java
} catch (Exception e) {
    log.error("[AutoConverge] 实验 {} 收敛失败", experiment.getId(), e);
}
```

---

## 10. 服务器端请求伪造 (A10:2021 - Server-Side Request Forgery)

**优点**:
- ✅ 无外部 HTTP 请求
- ✅ 无用户可控的 URL 参数

**问题**: 无

---

## 11. 漏洞清单

### Critical (CVSS ≥ 9.0)

| 编号 | 漏洞 | CVSS | 影响 | 文件 | 行号 | 工作量 |
|------|------|------|------|------|------|--------|
| #1 | 缺少 owner_id 数据隔离 | 9.1 | 可越权操作他人实验 | AbTestController.java | 50-132 | 0.5 人日 |

### High (CVSS 7.0-8.9)

| 编号 | 漏洞 | CVSS | 影响 | 文件 | 行号 | 工作量 |
|------|------|------|------|------|------|--------|
| #2 | user_fingerprint 明文存储 | 7.5 | 泄露用户设备指纹 | AbTestServiceImpl.java | 186 | 0.3 人日 |
| #5 | 事件去重无并发保护 | 7.4 | 统计数据不准确 | AbTestServiceImpl.java | 178-188 | 0.3 人日 |
| #10 | 缺少安全事件日志 | 7.5 | 无法追溯安全事件 | 全模块 | - | 0.5 人日 |

### Medium (CVSS 4.0-6.9)

| 编号 | 漏洞 | CVSS | 影响 | 文件 | 行号 | 工作量 |
|------|------|------|------|------|------|--------|
| #3 | 事件数据无脱敏 | 5.3 | 管理员可查看用户行为 | AbEvent.java | 33 | 0.5 人日 |
| #4 | 缺少业务规则校验 | 4.3 | 可创建无效实验 | AbTestServiceImpl.java | 82-108 | 0.3 人日 |
| #6 | 计数器更新无事务保护 | 5.9 | 计数器可能不一致 | AbTestServiceImpl.java | 191-195 | 0.5 人日 |
| #7 | 统计结果缓存无过期时间 | 4.3 | 可能返回过期数据 | AbTestServiceImpl.java | 241 | 0.2 人日 |
| #9 | 缺少数据完整性校验 | 5.3 | 计数器与事件表不一致 | 全模块 | - | 1.0 人日 |

### Low (CVSS < 4.0)

| 编号 | 漏洞 | CVSS | 影响 | 文件 | 行号 | 工作量 |
|------|------|------|------|------|------|--------|
| #8 | 错误信息泄露内部信息 | 3.1 | 可能泄露异常堆栈 | AbTestServiceImpl.java | 358-363 | 0.1 人日 |
| #11 | 自动收敛任务吞掉异常 | 3.7 | 无法追踪失败原因 | AbTestServiceImpl.java | 424-427 | 0.1 人日 |

---

## 12. 修复建议

### 12.1 立即修复（Critical - 1 周内）

**#1: 数据隔离加固（0.5 人日）**

```java
// AbTestServiceImpl.java - 添加所有权校验方法
private void checkOwnership(Long experimentId, Long userId) {
    AbExperiment exp = experimentRepository.findByIdAndDeleted(experimentId, 0)
        .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "实验不存在"));
    if (!exp.getOwnerId().equals(userId)) {
        throw new BusinessException(ErrorCode.FORBIDDEN, "无权操作此实验");
    }
}

// 修改所有接口签名，添加 userId 参数
public AbExperimentVO getById(Long id, Long userId) {
    checkOwnership(id, userId);
    return getById(id);
}

public void delete(Long id, Long userId) {
    checkOwnership(id, userId);
    delete(id);
}

public void updateStatus(Long id, Integer status, Long userId) {
    checkOwnership(id, userId);
    updateStatus(id, status);
}

public void setWinner(AbSetWinnerVO vo, Long userId) {
    checkOwnership(vo.getExperimentId(), userId);
    setWinner(vo);
}

// 变体操作需校验实验所有权
public long saveVariant(AbVariantSaveVO vo, Long userId) {
    checkOwnership(vo.getExperimentId(), userId);
    return saveVariant(vo);
}

public void deleteVariant(Long id, Long userId) {
    AbVariant variant = variantRepository.findByIdAndDeleted(id, 0)
        .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "变体不存在"));
    checkOwnership(variant.getExperimentId(), userId);
    deleteVariant(id);
}

// 事件记录需校验实验所有权
public void recordEvent(AbEventSaveVO vo, Long userId) {
    checkOwnership(vo.getExperimentId(), userId);
    recordEvent(vo);
}

// Controller 层传递 userId
@PostMapping("/experiment/get")
public RESTResult<AbExperimentVO> get(HttpServletRequest request, @RequestParam Long id) {
    Long userId = AuthTokenFilter.getUserId(request);
    if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
    RESTResult<AbExperimentVO> r = RESTResult.getSuccess(abTestService.getById(id, userId));
    r.setTraceId(MDC.get("traceId"));
    return r;
}
```

### 12.2 高优先级修复（High - 2 周内）

**#2: user_fingerprint 哈希（0.3 人日）**

```java
// AbTestServiceImpl.java
import org.apache.commons.codec.digest.DigestUtils;

@Transactional(rollbackFor = Exception.class)
public void recordEvent(AbEventSaveVO vo) {
    // SHA256 哈希 user_fingerprint
    String hashedFingerprint = DigestUtils.sha256Hex(vo.getUserFingerprint());
    
    try {
        AbEvent event = new AbEvent();
        event.setExperimentId(vo.getExperimentId());
        event.setVariantId(vo.getVariantId());
        event.setEventType(vo.getEventType());
        event.setUserFingerprint(hashedFingerprint);  // 存储哈希值
        event.setSessionId(vo.getSessionId());
        eventRepository.save(event);
        
        // 同步更新变体计数
        switch (vo.getEventType()) {
            case "view" -> variantRepository.incrementViewCount(vo.getVariantId());
            case "click" -> variantRepository.incrementClickCount(vo.getVariantId());
            case "conversion" -> variantRepository.incrementConversionCount(vo.getVariantId());
        }
    } catch (DataIntegrityViolationException e) {
        log.debug("[RecordEvent] 事件已存在，忽略");
    }
}
```

**#5: 事件去重唯一索引（0.3 人日）**

```sql
-- sql/abtest/schema.sql
CREATE UNIQUE INDEX idx_ab_event_unique 
ON ab_event (variant_id, event_type, user_fingerprint);
```

**#10: 安全事件日志（0.5 人日）**

```java
// SecurityAuditAspect.java
@Aspect
@Component
@Slf4j
public class SecurityAuditAspect {
    
    @AfterThrowing(pointcut = "execution(* cn.gaifan.douyinOperations.module.abtest..*(..))", 
                   throwing = "ex")
    public void logSecurityException(JoinPoint jp, Exception ex) {
        if (ex instanceof BusinessException) {
            BusinessException bex = (BusinessException) ex;
            if (bex.getCode() == ErrorCode.FORBIDDEN) {
                log.warn("[SecurityAudit] 越权访问尝试: method={}, args={}, user={}, error={}", 
                    jp.getSignature().toShortString(), 
                    maskSensitiveArgs(jp.getArgs()), 
                    getCurrentUserId(), 
                    bex.getMessage());
            }
        }
    }
    
    @AfterReturning("execution(* cn.gaifan.douyinOperations.module.abtest..delete*(..))")
    public void logDeletion(JoinPoint jp) {
        log.info("[SecurityAudit] 删除操作: method={}, args={}, user={}", 
            jp.getSignature().toShortString(), 
            maskSensitiveArgs(jp.getArgs()), 
            getCurrentUserId());
    }
    
    @AfterReturning("execution(* cn.gaifan.douyinOperations.module.abtest..setWinner(..))")
    public void logSetWinner(JoinPoint jp) {
        log.info("[SecurityAudit] 设置获胜变体: method={}, args={}, user={}", 
            jp.getSignature().toShortString(), 
            maskSensitiveArgs(jp.getArgs()), 
            getCurrentUserId());
    }
    
    private Long getCurrentUserId() {
        // 从 SecurityContext 或 MDC 获取
        return null;
    }
    
    private Object[] maskSensitiveArgs(Object[] args) {
        // 脱敏敏感参数
        return args;
    }
}
```

### 12.3 中优先级修复（Medium - 1 个月内）

**#3: 事件数据脱敏（0.5 人日）**

```java
// SensitiveDataSerializer.java
public class SensitiveDataSerializer extends JsonSerializer<String> {
    @Override
    public void serialize(String value, JsonGenerator gen, SerializerProvider serializers) 
            throws IOException {
        if (value == null || value.length() <= 8) {
            gen.writeString("****");
        } else {
            gen.writeString(value.substring(0, 4) + "****" + value.substring(value.length() - 4));
        }
    }
}

// AbEvent.java
@JsonSerialize(using = SensitiveDataSerializer.class)
@Column(name = "user_fingerprint")
private String userFingerprint;
```

**#4: 业务规则校验（0.3 人日）**

```java
// AbTestServiceImpl.java
public long save(AbExperimentSaveVO vo) {
    // 校验变体数量
    if (vo.getVariants() != null && vo.getVariants().size() < 2) {
        throw new BusinessException(ErrorCode.INVALID_PARAMS, "至少需要 2 个变体");
    }
    
    // 校验变体类型
    if (vo.getVariants() != null) {
        Set<String> types = vo.getVariants().stream()
                .map(AbVariantSaveVO::getVariantType)
                .collect(Collectors.toSet());
        if (!types.contains("A") || !types.contains("B")) {
            throw new BusinessException(ErrorCode.INVALID_PARAMS, "必须包含 A 和 B 变体");
        }
    }
    
    // 校验实验类型
    if (!Set.of("video", "live", "copy", "script_style").contains(vo.getExperimentType())) {
        throw new BusinessException(ErrorCode.INVALID_PARAMS, "实验类型不合法");
    }
    
    // 校验状态转换
    if (vo.getId() != null && vo.getStatus() != null) {
        AbExperiment existing = experimentRepository.findByIdAndDeleted(vo.getId(), 0)
            .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "实验不存在"));
        validateStatusTransition(existing.getStatus(), vo.getStatus());
    }
    
    // ... 原有逻辑
}

private void validateStatusTransition(Integer oldStatus, Integer newStatus) {
    // 0=草稿 1=运行中 2=已完成 3=已暂停
    if (oldStatus == 2 && newStatus != 2) {
        throw new BusinessException(ErrorCode.INVALID_PARAMS, "已完成的实验不能修改状态");
    }
    if (oldStatus == 0 && newStatus == 2) {
        throw new BusinessException(ErrorCode.INVALID_PARAMS, "草稿不能直接完成");
    }
}
```

**#7: 缓存 TTL 配置（0.2 人日）**

```java
// CacheConfig.java
@Bean
public CacheManager abTestCacheManager(RedisConnectionFactory factory) {
    RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
        .entryTtl(Duration.ofMinutes(5))  // 5 分钟过期
        .serializeValuesWith(RedisSerializationContext.SerializationPair
            .fromSerializer(new GenericJackson2JsonRedisSerializer()));
    return RedisCacheManager.builder(factory)
        .cacheDefaults(config)
        .build();
}

// AbTestServiceImpl.java
@Cacheable(value = "abtest:statistics", key = "#experimentId", 
           unless = "#result == null", cacheManager = "abTestCacheManager")
public AbExperimentStatisticsVO getExperimentStatistics(Long experimentId)
```

**#9: 数据完整性校验（1.0 人日）**

```java
// AbTestIntegrityService.java
@Service
@Slf4j
public class AbTestIntegrityService {
    
    @Resource
    private AbVariantRepository variantRepository;
    @Resource
    private AbEventRepository eventRepository;
    
    // 定时任务：每天凌晨 3 点校验数据完整性
    @Scheduled(cron = "0 0 3 * * ?")
    public void checkDataIntegrity() {
        log.info("[IntegrityCheck] 开始校验 A/B 测试数据完整性");
        
        List<AbVariant> variants = variantRepository.findAll();
        int inconsistentCount = 0;
        
        for (AbVariant variant : variants) {
            // 从事件表统计实际计数
            Long actualViewCount = eventRepository.countByVariantIdAndEventType(
                variant.getId(), "view");
            Long actualClickCount = eventRepository.countByVariantIdAndEventType(
                variant.getId(), "click");
            Long actualConversionCount = eventRepository.countByVariantIdAndEventType(
                variant.getId(), "conversion");
            
            // 对比变体表计数器
            if (!variant.getViewCount().equals(actualViewCount) ||
                !variant.getClickCount().equals(actualClickCount) ||
                !variant.getConversionCount().equals(actualConversionCount)) {
                
                log.warn("[IntegrityCheck] 变体 {} 计数器不一致: " +
                    "view({}/{}), click({}/{}), conversion({}/{})",
                    variant.getId(),
                    variant.getViewCount(), actualViewCount,
                    variant.getClickCount(), actualClickCount,
                    variant.getConversionCount(), actualConversionCount);
                
                // 修复计数器
                variant.setViewCount(actualViewCount);
                variant.setClickCount(actualClickCount);
                variant.setConversionCount(actualConversionCount);
                variantRepository.save(variant);
                
                inconsistentCount++;
            }
        }
        
        log.info("[IntegrityCheck] 完成校验，修复 {} 个不一致变体", inconsistentCount);
    }
}
```

### 12.4 低优先级修复（Low - 3 个月内）

**#8: 错误信息脱敏（0.1 人日）**

```java
// AbTestServiceImpl.java
} catch (Exception e) {
    log.error("[ChiSquareTest] 统计检验失败: experimentId={}", experimentId, e);
    return new AbStatisticalTestVO(
            0.0, 1.0, 0.0, false, variantA.getVariantId(), variantA.getVariantName(),
            "统计检验失败，请稍后重试"  // 通用错误信息，不泄露内部异常
    );
}
```

**#11: 异常日志记录（0.1 人日）**

```java
// AbTestServiceImpl.java
for (AbExperiment experiment : running) {
    try {
        // ... 收敛逻辑
    } catch (Exception e) {
        log.error("[AutoConverge] 实验 {} 收敛失败", experiment.getId(), e);
    }
}
```

---

## 13. 合规性检查

### 13.1 GDPR（通用数据保护条例）

| 条款 | 要求 | 当前状态 | 合规性 |
|------|------|----------|--------|
| 第 5 条 | 数据最小化 | ✅ 仅收集必要数据（user_fingerprint） | 合规 |
| 第 6 条 | 合法处理基础 | ⚠️ 需明确用户同意收集设备指纹 | 部分合规 |
| 第 15 条 | 访问权 | ⚠️ 无用户数据导出功能 | 不合规 |
| 第 17 条 | 删除权 | ❌ 事件表无 deleted 字段，无法删除 | 不合规 |
| 第 32 条 | 数据加密 | ❌ user_fingerprint 明文存储 | 不合规 |

**改进建议**:
1. 添加用户同意机制（Cookie Banner）
2. 实现用户数据导出功能（GDPR 第 15 条）
3. 实现用户数据删除功能（GDPR 第 17 条）
4. 对 user_fingerprint 进行哈希加密（GDPR 第 32 条）

### 13.2 等保 2.0（中国网络安全等级保护）

| 控制项 | 要求 | 当前状态 | 合规性 |
|--------|------|----------|--------|
| 身份鉴别 | 用户身份唯一标识 | ✅ Bearer Token 认证 | 合规 |
| 访问控制 | 数据隔离 | ❌ 缺少 owner_id 校验 | 不合规 |
| 安全审计 | 操作日志记录 | ⚠️ 缺少安全事件日志 | 部分合规 |
| 数据完整性 | 数据防篡改 | ⚠️ 计数器无完整性校验 | 部分合规 |
| 数据保密性 | 敏感数据加密 | ❌ user_fingerprint 明文存储 | 不合规 |

**改进建议**:
1. 修复 owner_id 数据隔离缺陷（Critical）
2. 添加安全事件审计日志（High）
3. 实现数据完整性校验（Medium）
4. 对敏感数据进行加密存储（High）

---

## 14. 总结

### 14.1 总体评估

**总分**: 72/100 (Grade C)

**评级说明**:
- **A (90-100)**: 优秀，无重大安全问题
- **B (80-89)**: 良好，存在少量中低风险问题
- **C (70-79)**: 及格，存在高风险问题需修复
- **D (60-69)**: 不及格，存在严重安全问题
- **F (<60)**: 危险，存在多个严重安全问题

**生产就绪**: ❌ **否** - 存在 Critical 级别数据隔离缺陷，必须修复后才能上线

### 14.2 关键指标

| 指标 | 数量 | 说明 |
|------|------|------|
| **Critical 问题** | 1 个 | 缺少 owner_id 数据隔离（CVSS 9.1） |
| **High 问题** | 3 个 | user_fingerprint 明文、事件去重、安全日志 |
| **Medium 问题** | 5 个 | 数据脱敏、业务校验、缓存配置、数据完整性 |
| **Low 问题** | 2 个 | 错误信息泄露、异常处理 |
| **总工作量** | 约 4.3 人日 | Critical: 0.5 + High: 1.1 + Medium: 2.5 + Low: 0.2 |

### 14.3 优先级建议

**立即修复（1 周内）**:
1. ✅ #1: 数据隔离加固（0.5 人日）- **阻塞上线**

**短期修复（2 周内）**:
1. ✅ #2: user_fingerprint 哈希（0.3 人日）
2. ✅ #5: 事件去重唯一索引（0.3 人日）
3. ✅ #10: 安全事件日志（0.5 人日）

**中期优化（1 个月内）**:
1. ✅ #3: 事件数据脱敏（0.5 人日）
2. ✅ #4: 业务规则校验（0.3 人日）
3. ✅ #7: 缓存 TTL 配置（0.2 人日）
4. ✅ #9: 数据完整性校验（1.0 人日）

**长期改进（3 个月内）**:
1. ✅ #8: 错误信息脱敏（0.1 人日）
2. ✅ #11: 异常日志记录（0.1 人日）
3. ✅ GDPR 合规改进（用户数据导出/删除）
4. ✅ 等保 2.0 合规改进（安全审计完善）

### 14.4 风险评估

**上线前必须修复**:
- 🔴 #1: 数据隔离缺陷（CVSS 9.1）- **阻塞上线**

**上线后 2 周内修复**:
- 🟠 #2: user_fingerprint 明文存储（CVSS 7.5）
- 🟠 #5: 事件去重无并发保护（CVSS 7.4）
- 🟠 #10: 缺少安全事件日志（CVSS 7.5）

**可延后修复**:
- 🟡 #3-#9: Medium 级别问题（CVSS 4.3-5.9）
- 🟢 #8, #11: Low 级别问题（CVSS 3.1-3.7）

---

**报告生成**: Claude Code (Opus 4.6)  
**审查状态**: 待人工复核  
**下一步**: 修复 Critical 问题 → 补充 High 级别修复 → 优化 Medium 级别问题 → 完善合规性

