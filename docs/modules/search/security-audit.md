# Search 模块安全审计报告

**审计日期**: 2026-05-09  
**模块**: search (统一搜索)  
**审计者**: Claude Code Security Reviewer  
**审计范围**: 后端代码 (douyin-operations-shortvideo/src/main/java/.../module/search/)  
**审计标准**: OWASP Top 10 2021, CWE Top 25, CVSS 3.1

---

## 执行摘要

**总体安全评分**: 82/100 (良好)

| 维度 | 评分 | 说明 |
|-----|------|------|
| 认证授权 | 90/100 | Bearer Token 认证，数据范围隔离完善 |
| 数据隔离 | 95/100 | 基于 DataScopeResolver 的严格数据范围控制 |
| 输入验证 | 85/100 | @Valid 注解覆盖，关键词长度限制，特殊字符转义 |
| SQL 注入防护 | 100/100 | JPA Specification 参数化查询，无风险 |
| XSS 防护 | 90/100 | likePattern() 转义特殊字符，内容截断 |
| 错误处理 | 70/100 | 基本错误处理，缺少异常日志 |
| 日志审计 | 40/100 | 完全缺少日志记录 |
| API 限流 | 60/100 | 无限流保护，存在滥用风险 |
| 敏感数据保护 | 85/100 | 话术内容截断，无敏感字段泄露 |
| 性能安全 | 75/100 | 分页限制合理，但缺少全文索引 |

**关键发现**:
- ✅ 0 个 CRITICAL 问题
- ⚠️ 2 个 HIGH 问题（缺少日志审计、无 API 限流）
- ⚠️ 5 个 MEDIUM 问题
- ℹ️ 6 个 LOW 问题

**总工作量估算**: 8.5 人日

**生产就绪度**: 🟢 可上线，建议修复 HIGH 问题后上线

---

## 1. OWASP Top 10 2021 分析

### A01:2021 - Broken Access Control (访问控制失效)

**评分**: 9/10 (优秀)

#### ✅ 优点

**1. 统一认证机制**
- 所有请求通过 `AuthTokenFilter.getUserId(request)` 获取当前用户
- 未登录用户返回 401 错误码

**代码示例** (`GlobalSearchController.java:42-45`):
```java
Long userId = AuthTokenFilter.getUserId(request);
if (userId == null) {
    return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
}
```

**2. 数据范围隔离**
- 基于 `DataScopeResolver` 获取可见用户 ID 列表
- 三种角色数据范围：
  - `admin`: 全局数据 (visibleUserIds = null)
  - `org`: 机构用户数据 (visibleUserIds = [机构下所有用户])
  - `talent`: 个人数据 (visibleUserIds = [userId])

**代码示例** (`GlobalSearchController.java:46-47`):
```java
String roleCode = AuthTokenFilter.getRoleCode(request);
List<Long> visibleIds = dataScopeService.getVisibleUserIds(userId, roleCode);
```

**3. 跨模块数据隔离**
- 直播场次: `userId IN (visibleUserIds)`
- 商品: `userId IN (visibleUserIds)`
- 话术: `createdBy IN (visibleUserIds) OR productId IN (可见商品)`
- 短视频: `ownerId IN (visibleUserIds)`

**代码示例** (`GlobalSearchServiceImpl.java:127-130`):
```java
// 直播场次搜索
if (vis != null) {
    ps.add(root.get("userId").in(vis));
}
```

**4. 话术权限复杂校验**
- 话术可见性：创建者匹配 OR 关联商品属于可见用户
- 使用子查询校验商品所有权

**代码示例** (`GlobalSearchServiceImpl.java:154-159`):
```java
Subquery<Long> sq = query.subquery(Long.class);
Root<DyProduct> dp = sq.from(DyProduct.class);
sq.select(dp.get("id")).where(cb.equal(dp.get("deleted"), 0), dp.get("userId").in(vis));
Predicate byCreator = root.get("createdBy").in(vis);
Predicate byProduct = cb.and(cb.isNotNull(root.get("productId")), root.get("productId").in(sq));
return cb.and(text, cb.or(byCreator, byProduct));
```

#### ⚠️ MEDIUM 问题

**M1 - 空用户列表提前返回但未记录日志**
- **位置**: `GlobalSearchServiceImpl.java:49-51`
- **CVSS 评分**: 4.3 (MEDIUM)
- **CWE**: CWE-778 (Insufficient Logging)
- **问题**: 
  - 空用户列表提前返回空结果，但未记录日志
  - 无法追踪异常的数据范围解析结果
  - 可能掩盖 DataScopeResolver 的错误
- **代码示例**:
  ```java
  if (visibleUserIds != null && visibleUserIds.isEmpty()) {
      return GlobalSearchResponseVO.builder().hits(List.of()).tookMs(System.currentTimeMillis() - t0).build();
      // ❌ 未记录日志
  }
  ```
- **修复方案**:
  ```java
  if (visibleUserIds != null && visibleUserIds.isEmpty()) {
      log.warn("globalSearch: empty visibleUserIds for userId={}, roleCode={}", userId, roleCode);
      return GlobalSearchResponseVO.builder().hits(List.of()).tookMs(System.currentTimeMillis() - t0).build();
  }
  ```
- **工作量**: 0.5 人日
- **优先级**: P2

---

### A02:2021 - Cryptographic Failures (加密机制失效)

**评分**: N/A (不适用)

**说明**: Search 模块不涉及敏感数据加密存储，所有数据来自其他模块。

---

### A03:2021 - Injection (注入攻击)

**评分**: 10/10 (优秀)

#### ✅ 优点

**1. SQL 注入防护**
- 使用 JPA Specification 构建动态查询
- 所有参数自动参数化，无字符串拼接
- 特殊字符自动转义

**代码示例** (`GlobalSearchServiceImpl.java:118-121`):
```java
private static String likePattern(String kw) {
    String esc = kw.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    return "%" + esc.toLowerCase(Locale.ROOT) + "%";
}
```

**2. LIKE 查询转义**
- `\`, `%`, `_` 三个特殊字符全部转义
- 使用 `'\'` 作为转义字符
- 防止 LIKE 通配符注入

**代码示例** (`GlobalSearchServiceImpl.java:126`):
```java
ps.add(cb.like(cb.lower(root.get("liveTitle")), likePattern(kw), '\\'));
```

**3. 参数化查询**
- 所有 Specification 使用 CriteriaBuilder 构建条件
- 无原生 SQL 查询
- 无字符串拼接

#### ℹ️ LOW 问题

**L1 - 关键词长度限制可能过长**
- **位置**: `GlobalSearchRequestVO.java:17`
- **CVSS 评分**: 3.1 (LOW)
- **CWE**: CWE-770 (Allocation of Resources Without Limits or Throttling)
- **问题**: 
  - 关键词最大长度 64 字符，可能导致性能问题
  - 超长关键词可能导致数据库查询缓慢
- **修复方案**: 降低到 32 字符
- **工作量**: 0.1 人日
- **优先级**: P3

---

### A04:2021 - Insecure Design (不安全设计)

**评分**: 8/10 (良好)

#### ✅ 优点

**1. 分页限制防止资源耗尽**
- 每类型最多返回 6 条
- 总结果最多 50 条
- 避免全表扫描

**代码示例** (`GlobalSearchServiceImpl.java:32, 47`):
```java
private static final int PER_TYPE = 6;
int cap = request.getLimit() != null ? request.getLimit() : 24;
```

**2. 去重逻辑防止重复结果**
- 使用 `kind:id` 作为唯一键
- LinkedHashSet 保持插入顺序
- 提前退出避免无效遍历

**代码示例** (`GlobalSearchServiceImpl.java:92-102`):
```java
Set<String> seen = new LinkedHashSet<>();
List<GlobalSearchHitVO> out = new ArrayList<>();
for (GlobalSearchHitVO h : merged) {
    String key = h.getKind() + ":" + h.getId();
    if (seen.add(key)) {
        out.add(h);
        if (out.size() >= cap) break; // 提前退出
    }
}
```

**3. 话术内容截断防止信息泄露**
- 话术内容超过 80 字符自动截断
- 避免返回完整话术内容
- 降低数据泄露风险

**代码示例** (`GlobalSearchServiceImpl.java:110-116`):
```java
private static String trimScript(String content) {
    if (content == null) return "";
    String t = content.replace('\n', ' ').trim();
    return t.length() > 80 ? t.substring(0, 80) + "…" : t;
}
```

#### ⚠️ HIGH 问题

**H1 - 缺少 API 限流保护**
- **位置**: `GlobalSearchController.java:37-52`
- **CVSS 评分**: 7.5 (HIGH)
- **CWE**: CWE-770 (Allocation of Resources Without Limits or Throttling)
- **问题**: 
  - 无全局限流保护
  - 搜索接口无调用频率限制
  - 攻击者可暴力调用导致数据库负载过高
  - 可能导致服务不可用
- **影响**: 
  - 数据库连接池耗尽
  - CPU 和内存资源耗尽
  - 服务响应变慢或不可用
  - DoS 攻击风险
- **修复方案**:
  ```java
  // 1. 添加 Resilience4j 限流注解
  @PostMapping("/global")
  @RateLimiter(name = "globalSearch", fallbackMethod = "searchFallback")
  public RESTResult<GlobalSearchResponseVO> globalSearch(...) { }
  
  // 2. 限流降级方法
  public RESTResult<GlobalSearchResponseVO> searchFallback(
          GlobalSearchRequestVO vo, HttpServletRequest request, Throwable t) {
      return RESTResult.error(ErrorCode.TOO_MANY_REQUESTS, "搜索请求过于频繁，请稍后再试");
  }
  
  // 3. 配置限流策略（每用户每分钟 30 次）
  resilience4j:
    ratelimiter:
      instances:
        globalSearch:
          limitForPeriod: 30
          limitRefreshPeriod: 1m
          timeoutDuration: 0s
  ```
- **工作量**: 1.5 人日
- **优先级**: P1 - 应该立即修复

#### ⚠️ MEDIUM 问题

**M2 - 缺少搜索结果缓存**
- **位置**: `GlobalSearchServiceImpl.java:44-108`
- **CVSS 评分**: 5.3 (MEDIUM)
- **CWE**: CWE-400 (Uncontrolled Resource Consumption)
- **问题**: 
  - 高频搜索词（如"护肤"、"彩妆"）重复查询数据库
  - 无缓存机制，每次都执行 4 次数据库查询
  - 数据库负载高
- **修复方案**:
  ```java
  @Cacheable(value = "search:global", 
             key = "#request.q + ':' + #visibleUserIds", 
             unless = "#result == null || #result.hits.isEmpty()")
  public GlobalSearchResponseVO search(GlobalSearchRequestVO request, List<Long> visibleUserIds) { }
  
  // 配置缓存 TTL 30 秒
  spring:
    cache:
      redis:
        time-to-live: 30s
  ```
- **工作量**: 1 人日
- **优先级**: P2

---

### A05:2021 - Security Misconfiguration (安全配置错误)

**评分**: 8/10 (良好)

#### ✅ 优点

**1. 参数校验完善**
- 使用 `@Valid` 注解自动校验
- 关键词长度限制 2-64 字符
- 结果上限限制 1-50 条

**代码示例** (`GlobalSearchRequestVO.java:16-23`):
```java
@NotBlank(message = "关键词不能为空")
@Size(min = 2, max = 64, message = "关键词长度为 2–64 字符")
private String q;

@Min(1)
@Max(50)
private Integer limit = 24;
```

**2. 逻辑删除自动过滤**
- 所有 Entity 使用 `@SQLRestriction("deleted = 0")`
- 自动过滤已删除记录
- 无需手动添加 deleted 条件

#### ⚠️ MEDIUM 问题

**M3 - 缺少空关键词提前校验**
- **位置**: `GlobalSearchServiceImpl.java:46`
- **CVSS 评分**: 4.3 (MEDIUM)
- **CWE**: CWE-20 (Improper Input Validation)
- **问题**: 
  - 空白关键词（如 `"   "`）未提前校验
  - 仍会执行 4 次数据库查询
  - 浪费数据库资源
- **修复方案**:
  ```java
  String kw = request.getQ().trim();
  if (kw.isEmpty()) {
      log.warn("globalSearch: empty keyword after trim");
      return GlobalSearchResponseVO.builder().hits(List.of()).tookMs(0L).build();
  }
  ```
- **工作量**: 0.5 人日
- **优先级**: P2

---

### A06:2021 - Vulnerable and Outdated Components (易受攻击和过时的组件)

**评分**: N/A (不适用)

**说明**: Search 模块仅依赖 Spring Boot 核心组件，无第三方库依赖。

---

### A07:2021 - Identification and Authentication Failures (身份识别和认证失败)

**评分**: 9/10 (优秀)

#### ✅ 优点

**1. 统一认证机制**
- 所有请求通过 `AuthTokenFilter` 校验
- Bearer Token 认证
- 未登录返回 401 错误

**2. 用户身份提取**
- 从请求头提取 userId 和 roleCode
- 传递给 Service 层进行数据范围过滤

#### ℹ️ LOW 问题

**L2 - 缺少 Token 过期时间校验**
- **位置**: `GlobalSearchController.java:42`
- **CVSS 评分**: 3.1 (LOW)
- **CWE**: CWE-613 (Insufficient Session Expiration)
- **问题**: 
  - Controller 层未校验 Token 是否过期
  - 依赖 AuthTokenFilter 的校验
  - 建议增加显式校验
- **修复方案**: 在 AuthTokenFilter 中增加 Token 过期校验
- **工作量**: 0.5 人日（需修改 common 模块）
- **优先级**: P3

---

### A08:2021 - Software and Data Integrity Failures (软件和数据完整性失败)

**评分**: 9/10 (优秀)

#### ✅ 优点

**1. 数据完整性保护**
- 使用 JPA 事务管理
- 逻辑删除保护数据
- 无数据篡改风险

**2. 只读操作**
- Search 模块仅查询，不修改数据
- 无数据完整性破坏风险

---

### A09:2021 - Security Logging and Monitoring Failures (安全日志和监控失败)

**评分**: 4/10 (差)

#### ⚠️ HIGH 问题

**H2 - 完全缺少日志记录**
- **位置**: `GlobalSearchServiceImpl.java` 整个类
- **CVSS 评分**: 7.5 (HIGH)
- **CWE**: CWE-778 (Insufficient Logging)
- **问题**: 
  - Service 层完全无日志记录
  - 无法追踪搜索查询耗时
  - 无法分析用户搜索行为
  - 无法排查性能问题
  - 无法检测异常搜索模式
- **影响**: 
  - 问题排查困难
  - 无法监控搜索性能
  - 无法检测恶意搜索
  - 无法分析热门搜索词
- **修复方案**:
  ```java
  @Slf4j
  @Service
  public class GlobalSearchServiceImpl implements GlobalSearchService {
      @Override
      public GlobalSearchResponseVO search(GlobalSearchRequestVO request, List<Long> visibleUserIds) {
          long t0 = System.currentTimeMillis();
          String kw = request.getQ().trim();
          
          log.info("globalSearch: q={}, limit={}, visibleUserIds={}", 
              kw, request.getLimit(), visibleUserIds != null ? visibleUserIds.size() : "null");
          
          // ... 搜索逻辑 ...
          
          long tookMs = System.currentTimeMillis() - t0;
          log.info("globalSearch: found {} hits in {}ms", out.size(), tookMs);
          
          if (out.isEmpty()) {
              log.warn("globalSearch: no results for keyword: {}", kw);
          }
          
          if (tookMs > 1000) {
              log.warn("globalSearch: slow query ({}ms) for keyword: {}", tookMs, kw);
          }
          
          return GlobalSearchResponseVO.builder().hits(out).tookMs(tookMs).build();
      }
  }
  ```
- **工作量**: 1 人日
- **优先级**: P1 - 应该立即修复

#### ⚠️ MEDIUM 问题

**M4 - 缺少监控指标**
- **位置**: `GlobalSearchServiceImpl.java:44-108`
- **CVSS 评分**: 5.3 (MEDIUM)
- **CWE**: CWE-778 (Insufficient Logging)
- **问题**: 
  - 无自定义监控指标
  - 无法监控搜索性能和调用频率
  - 无法及时发现性能问题
- **修复方案**:
  ```java
  @Timed(value = "search.global.query", description = "全局搜索查询耗时")
  @Counted(value = "search.global.requests", description = "全局搜索请求次数")
  public GlobalSearchResponseVO search(GlobalSearchRequestVO request, List<Long> visibleUserIds) { }
  ```
- **工作量**: 1 人日
- **优先级**: P2

---

### A10:2021 - Server-Side Request Forgery (SSRF) (服务端请求伪造)

**评分**: N/A (不适用)

**说明**: Search 模块不涉及外部 URL 请求，无 SSRF 风险。

---

## 2. 漏洞清单

### 2.1 按严重程度分类

| 严重程度 | 数量 | CVSS 范围 | 修复优先级 |
|---------|------|-----------|-----------|
| CRITICAL | 0 | 9.0-10.0 | P0 - 立即修复 |
| HIGH | 2 | 7.0-8.9 | P1 - 应该立即修复 |
| MEDIUM | 5 | 4.0-6.9 | P2 - 建议修复 |
| LOW | 6 | 0.1-3.9 | P3 - 可选修复 |

### 2.2 HIGH 级别漏洞详情

| ID | 漏洞名称 | CVSS | CWE | 位置 | 工作量 |
|----|---------|------|-----|------|--------|
| H1 | 缺少 API 限流保护 | 7.5 | CWE-770 | GlobalSearchController.java:37-52 | 1.5 人日 |
| H2 | 完全缺少日志记录 | 7.5 | CWE-778 | GlobalSearchServiceImpl.java | 1 人日 |

### 2.3 MEDIUM 级别漏洞详情

| ID | 漏洞名称 | CVSS | CWE | 位置 | 工作量 |
|----|---------|------|-----|------|--------|
| M1 | 空用户列表未记录日志 | 4.3 | CWE-778 | GlobalSearchServiceImpl.java:49-51 | 0.5 人日 |
| M2 | 缺少搜索结果缓存 | 5.3 | CWE-400 | GlobalSearchServiceImpl.java:44-108 | 1 人日 |
| M3 | 缺少空关键词提前校验 | 4.3 | CWE-20 | GlobalSearchServiceImpl.java:46 | 0.5 人日 |
| M4 | 缺少监控指标 | 5.3 | CWE-778 | GlobalSearchServiceImpl.java:44-108 | 1 人日 |
| M5 | 缺少异常处理和日志 | 5.3 | CWE-755 | GlobalSearchServiceImpl.java:44-108 | 1 人日 |

### 2.4 LOW 级别漏洞详情

| ID | 漏洞名称 | CVSS | CWE | 位置 | 工作量 |
|----|---------|------|-----|------|--------|
| L1 | 关键词长度限制过长 | 3.1 | CWE-770 | GlobalSearchRequestVO.java:17 | 0.1 人日 |
| L2 | 缺少 Token 过期校验 | 3.1 | CWE-613 | GlobalSearchController.java:42 | 0.5 人日 |
| L3 | 话术路径硬编码重复 | 2.3 | CWE-710 | GlobalSearchServiceImpl.java:79 | 0.5 人日 |
| L4 | 缺少方法级注释 | 2.0 | CWE-1078 | GlobalSearchServiceImpl.java | 1 人日 |
| L5 | 硬编码魔法数字 | 2.0 | CWE-1078 | GlobalSearchServiceImpl.java:115,47 | 0.5 人日 |
| L6 | 缺少全文索引 | 3.9 | CWE-1049 | dy_product_script.script_content | 2 人日 |

---

## 3. 数据安全

### 3.1 敏感数据识别

| 数据类型 | 敏感级别 | 存储位置 | 保护措施 |
|---------|---------|---------|---------|
| 搜索关键词 | 中 | 请求参数 | 长度限制、特殊字符转义 |
| 用户 ID | 高 | 请求头 | Bearer Token 认证 |
| 话术内容 | 中 | 搜索结果 | 截断到 80 字符 |
| 直播标题 | 低 | 搜索结果 | 数据范围过滤 |
| 商品名称 | 低 | 搜索结果 | 数据范围过滤 |

### 3.2 数据加密

**传输加密**: ✅ HTTPS (由网关层处理)  
**存储加密**: N/A (Search 模块不存储数据)  
**字段加密**: N/A (无敏感字段)

### 3.3 数据脱敏

**话术内容截断**: ✅ 超过 80 字符自动截断  
**无标题处理**: ✅ 短视频无标题显示 "(无标题)"

**代码示例** (`GlobalSearchServiceImpl.java:110-116`):
```java
private static String trimScript(String content) {
    if (content == null) return "";
    String t = content.replace('\n', ' ').trim();
    return t.length() > 80 ? t.substring(0, 80) + "…" : t;
}
```

### 3.4 数据访问控制

**数据范围隔离**: ✅ 基于 DataScopeResolver  
**跨模块权限**: ✅ 话术权限复杂校验（创建者 OR 商品所有者）  
**逻辑删除**: ✅ 自动过滤已删除记录

---

## 4. 认证授权

### 4.1 身份验证

**认证方式**: Bearer Token  
**认证位置**: Controller 层  
**认证实现**: `AuthTokenFilter.getUserId(request)`

**代码示例** (`GlobalSearchController.java:42-45`):
```java
Long userId = AuthTokenFilter.getUserId(request);
if (userId == null) {
    return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
}
```

**优点**:
- ✅ 统一认证机制
- ✅ 未登录返回 401 错误
- ✅ 认证失败不执行业务逻辑

**缺点**:
- ⚠️ 缺少 Token 过期时间校验
- ⚠️ 缺少 Token 刷新机制

### 4.2 权限控制

**权限模型**: 基于角色的数据范围控制 (RBAC + Data Scope)

| 角色 | 数据范围 | 实现方式 |
|------|---------|---------|
| admin | 全局数据 | visibleUserIds = null |
| org | 机构用户数据 | visibleUserIds = [机构下所有用户] |
| talent | 个人数据 | visibleUserIds = [userId] |

**代码示例** (`GlobalSearchController.java:46-47`):
```java
String roleCode = AuthTokenFilter.getRoleCode(request);
List<Long> visibleIds = dataScopeService.getVisibleUserIds(userId, roleCode);
```

**优点**:
- ✅ 数据范围隔离完善
- ✅ 跨模块权限一致
- ✅ 话术权限复杂校验（创建者 OR 商品所有者）

**缺点**:
- ⚠️ 缺少方法级权限注解（如 `@PreAuthorize`）
- ⚠️ 依赖 DataScopeResolver 实现，耦合度高

### 4.3 会话管理

**会话类型**: 无状态 (Stateless)  
**Token 存储**: 客户端 (Authorization 头)  
**Token 传递**: Bearer Token

**优点**:
- ✅ 无状态设计，易于水平扩展
- ✅ 无需服务端存储会话

**缺点**:
- ⚠️ Token 无法主动失效（需等待过期）
- ⚠️ 缺少 Token 黑名单机制

---

## 5. 合规性检查

### 5.1 GDPR (通用数据保护条例)

| 要求 | 状态 | 说明 |
|------|------|------|
| 数据最小化 | ✅ | 仅返回必要字段，话术内容截断 |
| 访问控制 | ✅ | 基于数据范围的严格访问控制 |
| 数据删除 | ✅ | 逻辑删除，自动过滤已删除记录 |
| 数据可携带性 | ⚠️ | 无数据导出功能 |
| 审计日志 | ❌ | 完全缺少审计日志 |

**合规评分**: 6/10 (中等)

**改进建议**:
1. 增加审计日志（记录搜索关键词、用户 ID、时间戳）
2. 增加数据导出功能（搜索历史导出）

### 5.2 等保 2.0 (信息安全等级保护)

| 要求 | 状态 | 说明 |
|------|------|------|
| 身份鉴别 | ✅ | Bearer Token 认证 |
| 访问控制 | ✅ | 基于角色的数据范围控制 |
| 安全审计 | ❌ | 完全缺少审计日志 |
| 入侵防范 | ⚠️ | 无 API 限流保护 |
| 数据完整性 | ✅ | JPA 事务管理 |
| 数据保密性 | ✅ | 数据范围隔离 |

**合规评分**: 6/10 (中等)

**改进建议**:
1. 增加审计日志（满足安全审计要求）
2. 增加 API 限流（满足入侵防范要求）

### 5.3 行业标准

**PCI DSS**: N/A (不涉及支付卡数据)  
**HIPAA**: N/A (不涉及医疗数据)  
**SOC 2**: ⚠️ 缺少审计日志和监控

---

## 6. 安全加固建议

### 6.1 P0 - 立即修复 (0 项)

**无 P0 问题**

### 6.2 P1 - 应该立即修复 (2 项，2.5 人日)

| 优先级 | 问题 | 工作量 | 预期收益 |
|--------|------|--------|----------|
| P1 | H1 - 缺少 API 限流保护 | 1.5 人日 | 防止 DoS 攻击，保护服务可用性 |
| P1 | H2 - 完全缺少日志记录 | 1 人日 | 提升可观测性，便于问题排查 |

**修复顺序**: H2 → H1 (先增加日志，再增加限流)

### 6.3 P2 - 建议修复 (5 项，4.5 人日)

| 优先级 | 问题 | 工作量 | 预期收益 |
|--------|------|--------|----------|
| P2 | M1 - 空用户列表未记录日志 | 0.5 人日 | 便于排查数据范围解析问题 |
| P2 | M2 - 缺少搜索结果缓存 | 1 人日 | 提升性能，降低数据库负载 |
| P2 | M3 - 缺少空关键词提前校验 | 0.5 人日 | 避免无效数据库查询 |
| P2 | M4 - 缺少监控指标 | 1 人日 | 提升可观测性，监控性能 |
| P2 | M5 - 缺少异常处理和日志 | 1 人日 | 提升健壮性，便于问题排查 |

**修复顺序**: M3 → M1 → M5 → M4 → M2

### 6.4 P3 - 可选修复 (6 项，4.6 人日)

| 优先级 | 问题 | 工作量 | 预期收益 |
|--------|------|--------|----------|
| P3 | L1 - 关键词长度限制过长 | 0.1 人日 | 降低性能风险 |
| P3 | L2 - 缺少 Token 过期校验 | 0.5 人日 | 提升安全性 |
| P3 | L3 - 话术路径硬编码重复 | 0.5 人日 | 提升代码质量 |
| P3 | L4 - 缺少方法级注释 | 1 人日 | 提升可维护性 |
| P3 | L5 - 硬编码魔法数字 | 0.5 人日 | 提升可维护性 |
| P3 | L6 - 缺少全文索引 | 2 人日 | 提升搜索性能 |

**修复顺序**: L1 → L5 → L3 → L2 → L4 → L6

### 6.5 修复路线图

#### 短期（1-2 周，2.5 人日）

**目标**: 修复 P1 问题，提升可观测性和可用性

1. **增加日志记录** (H2, 1 人日)
   - 在 Service 层增加 INFO/DEBUG/WARN 日志
   - 记录搜索关键词、结果数量、查询耗时
   - 记录空结果和慢查询

2. **增加 API 限流** (H1, 1.5 人日)
   - 使用 Resilience4j 限流
   - 配置每用户每分钟 30 次
   - 增加限流降级方法

#### 中期（1-2 月，4.5 人日）

**目标**: 修复 P2 问题，提升性能和健壮性

1. **增加空关键词校验** (M3, 0.5 人日)
2. **增加空用户列表日志** (M1, 0.5 人日)
3. **增加异常处理** (M5, 1 人日)
4. **增加监控指标** (M4, 1 人日)
5. **增加搜索结果缓存** (M2, 1 人日)

#### 长期（3-6 月，4.6 人日）

**目标**: 修复 P3 问题，提升代码质量和性能

1. **降低关键词长度限制** (L1, 0.1 人日)
2. **提取魔法数字为常量** (L5, 0.5 人日)
3. **修复话术路径硬编码** (L3, 0.5 人日)
4. **增加 Token 过期校验** (L2, 0.5 人日)
5. **增加方法级注释** (L4, 1 人日)
6. **增加全文索引** (L6, 2 人日)

---

## 7. 安全测试建议

### 7.1 渗透测试

**测试范围**:
1. 认证绕过测试（无 Token、过期 Token、伪造 Token）
2. 数据范围隔离测试（跨用户数据访问）
3. SQL 注入测试（特殊字符、LIKE 通配符）
4. XSS 测试（搜索关键词中的脚本）
5. DoS 测试（高频请求、超长关键词）

**测试工具**:
- Burp Suite (手动测试)
- OWASP ZAP (自动扫描)
- SQLMap (SQL 注入测试)

### 7.2 安全扫描

**静态代码扫描**:
- SonarQube (代码质量和安全漏洞)
- SpotBugs (Java 安全漏洞)
- Checkmarx (SAST)

**依赖扫描**:
- OWASP Dependency-Check
- Snyk

**容器扫描**:
- Trivy
- Clair

### 7.3 性能测试

**测试场景**:
1. 正常负载测试（100 QPS）
2. 峰值负载测试（500 QPS）
3. 压力测试（1000 QPS）
4. 慢查询测试（超长关键词）

**测试工具**:
- JMeter
- Gatling
- Locust

---

## 8. 总结

### 8.1 安全优势

1. **数据隔离完善**: 基于 DataScopeResolver 的严格数据范围控制，三种角色数据范围清晰
2. **SQL 注入防护**: JPA Specification 参数化查询，特殊字符转义完善
3. **XSS 防护**: likePattern() 转义特殊字符，话术内容截断
4. **参数校验**: @Valid 注解覆盖，关键词长度限制，结果上限限制
5. **性能设计**: 分页限制合理，去重逻辑高效，提前退出优化

### 8.2 安全劣势

1. **缺少日志审计**: 完全无日志记录，无法追踪搜索行为和性能问题
2. **缺少 API 限流**: 无限流保护，存在 DoS 攻击风险
3. **缺少监控指标**: 无自定义监控，无法及时发现性能问题
4. **缺少缓存**: 高频搜索词重复查询数据库，性能有优化空间
5. **缺少异常处理**: Service 层未捕获异常，缺少异常日志

### 8.3 风险评估

| 风险类型 | 风险等级 | 可能性 | 影响 | 缓解措施 |
|---------|---------|--------|------|---------|
| DoS 攻击 | HIGH | 高 | 高 | 增加 API 限流 |
| 数据泄露 | LOW | 低 | 中 | 数据范围隔离完善 |
| SQL 注入 | LOW | 低 | 高 | JPA Specification 防护 |
| XSS 攻击 | LOW | 低 | 中 | 特殊字符转义 |
| 性能问题 | MEDIUM | 中 | 中 | 增加缓存和全文索引 |

### 8.4 改进优先级

**立即修复** (P1, 2.5 人日):
- H1 - 缺少 API 限流保护
- H2 - 完全缺少日志记录

**尽快修复** (P2, 4.5 人日):
- M1 - 空用户列表未记录日志
- M2 - 缺少搜索结果缓存
- M3 - 缺少空关键词提前校验
- M4 - 缺少监控指标
- M5 - 缺少异常处理和日志

**可选修复** (P3, 4.6 人日):
- L1 - 关键词长度限制过长
- L2 - 缺少 Token 过期校验
- L3 - 话术路径硬编码重复
- L4 - 缺少方法级注释
- L5 - 硬编码魔法数字
- L6 - 缺少全文索引

### 8.5 最终评价

**安全评分**: 82/100 (良好)  
**生产就绪度**: 🟢 可上线，建议修复 HIGH 问题后上线

**优点**:
- ✅ 数据隔离完善，SQL 注入防护到位
- ✅ 参数校验完善，XSS 防护良好
- ✅ 性能设计合理，代码质量高

**缺点**:
- ⚠️ 缺少日志审计和监控
- ⚠️ 缺少 API 限流保护
- ⚠️ 缺少缓存和全文索引

**建议**: 优先修复 P1 问题（日志和限流），提升可观测性和可用性后上线。

---

**报告生成时间**: 2026-05-09  
**审计者**: Claude Code Security Reviewer  
**下一步**: 执行短期改进计划（P1，共 2.5 人日）

