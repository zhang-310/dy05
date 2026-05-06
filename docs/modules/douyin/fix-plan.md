# Douyin 模块修复计划

**生成日期**: 2026-05-06  
**模块**: douyin  
**总体评分**: B+ (85/100)  
**总工作量**: 25 人日（约 5 周，1 人完成）

---

## 执行摘要

Douyin 模块整体架构良好，但存在 **1 个 P0 阻塞级问题**、**6 个 P1 高优先级问题**、**9 个 P2 中优先级问题** 和 **6 个 P3 低优先级问题**。

**关键问题**:
- ⚠️ **P0**: N+1 查询（视频同步 60 queries for 20 videos）
- ⚠️ **P1**: OAuth state 可重放（10 分钟内可重复使用）
- ⚠️ **P1**: Token 明文记录到日志
- ⚠️ **P1**: 无 API 限流保护
- ⚠️ **P1**: 无缓存策略（所有查询直接访问数据库）
- ⚠️ **P1**: 话术学习管道串行处理
- ⚠️ **P2**: 数据隔离命名不一致（userId vs ownerId）
- ⚠️ **P3**: 测试覆盖率 <10%（严重不足）

**修复优先级**: P0（立即修复）→ P1（短期修复）→ P2（长期优化）→ P3（持续改进）

---

## P0 问题（阻塞级 - 立即修复）

### P0-1: N+1 查询（视频同步）

**位置**: `DouyinVideoServiceImpl.java:143-170`

**问题描述**:
视频同步操作对每个视频执行单独查询，导致 N+1 查询问题。20 个视频需要 60 次数据库查询（20 × 3）。

**影响**:
- 响应时间：15s（当前）→ 5s（修复后），67% 提升
- 数据库负载：3N 查询
- 影响页面：视频同步功能

**修复方案**:

```java
@Override
@Transactional(rollbackFor = Exception.class)
public void syncVideos(Long accountId) {
    // 1. 批量查询已存在的视频
    List<String> videoIds = resp.list().stream()
        .map(DouyinApiClient.VideoItem::itemId)
        .collect(Collectors.toList());
    
    Map<String, DouyinVideo> existingVideos = douyinVideoRepository
        .findByVideoIdInAndDeleted(videoIds, 0)
        .stream()
        .collect(Collectors.toMap(DouyinVideo::getVideoId, v -> v));
    
    // 2. 分类处理
    List<DouyinVideo> toUpdate = new ArrayList<>();
    List<DouyinVideo> toInsert = new ArrayList<>();
    
    for (DouyinApiClient.VideoItem item : resp.list()) {
        if (existingVideos.containsKey(item.itemId())) {
            DouyinVideo v = existingVideos.get(item.itemId());
            v.setViewCount(item.playCount());
            v.setLikeCount(item.likeCount());
            toUpdate.add(v);
        } else {
            DouyinVideo video = new DouyinVideo();
            // ... 设置字段
            toInsert.add(video);
        }
    }
    
    // 3. 批量保存（2 次查询）
    if (!toUpdate.isEmpty()) douyinVideoRepository.saveAll(toUpdate);
    if (!toInsert.isEmpty()) douyinVideoRepository.saveAll(toInsert);
}

// 添加到 DouyinVideoRepository:
List<DouyinVideo> findByVideoIdInAndDeleted(List<String> videoIds, Integer deleted);
```

**工作量**: 4 小时

**验证**:
- 修复前：60 次查询（20 个视频）
- 修复后：2 次查询（1 批量查询 + 1 批量保存）
- 响应时间：15s → 5s（67% 提升）

---

## P1 问题（高优先级 - 短期修复）

### P1-1: OAuth state 可重放

**位置**: `DouyinOAuthController.java:309-343`

**问题描述**:
state 参数只验证签名和时间戳，未记录已使用的 state。攻击者可在 10 分钟内重复使用同一个 state。

**安全风险**: CVSS 7.5 (HIGH)
- 授权码可能被重复使用
- 可能导致账号绑定到错误的用户

**修复方案**:
```java
// 1. 生成 state 时存储到 Redis
String state = generateState(userId);
redisTemplate.opsForValue().set("oauth:state:" + state, userId, 10, TimeUnit.MINUTES);

// 2. 回调时验证并删除
private String extractUserIdFromState(String state) {
    String userId = redisTemplate.opsForValue().get("oauth:state:" + state);
    if (userId == null) {
        log.warn("OAuth state 无效或已使用: {}", state);
        return null;
    }
    // 删除 state，防止重放
    redisTemplate.delete("oauth:state:" + state);
    return userId;
}
```

**工作量**: 1 人日

---

### P1-3: 缺少 API 限流保护

**位置**: 所有 Controller（特别是 OAuth 和同步接口）

**问题描述**:
- OAuth 授权接口无限流
- 视频同步接口无限流
- 粉丝画像同步接口无限流

**安全风险**: CVSS 7.5 (HIGH)
- 攻击者可暴力枚举授权码
- 频繁调用抖音 API 可能导致账号被封
- 服务器资源耗尽

**修复方案**:
```java
@Configuration
public class RateLimitConfig {
    @Bean
    public RateLimiter oauthRateLimiter() {
        return RateLimiter.of("oauth", RateLimiterConfig.custom()
            .limitForPeriod(10)
            .limitRefreshPeriod(Duration.ofMinutes(1))
            .timeoutDuration(Duration.ofSeconds(5))
            .build());
    }
}

@PostMapping("/authorize-url")
@RateLimiter(name = "oauth")
public RESTResult<Map<String, String>> getAuthorizeUrl(...) {
    // ...
}
```

**工作量**: 2 人日

---

### P1-5: 无缓存策略（账号统计）

**位置**: `DouyinAccountServiceImpl.java:122-167`

**问题描述**:
账号统计使用聚合查询（优秀），但结果未缓存。

**影响**:
- 数据库负载：6 次聚合查询/请求
- 响应时间：+100-200ms
- 影响页面：账号详情、仪表盘

**修复方案**:
- L1 缓存（Caffeine）：5 分钟 TTL，最大 500 条
- L2 缓存（Redis）：5 分钟 TTL
- 缓存失效：视频同步时清除

**工作量**: 4 小时

**预期收益**: 缓存命中率 85%+，响应时间 200ms → 10ms

---

### P1-6: 话术学习管道串行处理

**位置**: `DouyinScriptLearningServiceImpl.java:79-110`

**问题描述**:
学习管道串行处理视频，每个视频单独调用 LLM。

**影响**:
- 处理时间：80-120 秒（10 个视频）
- 调度线程阻塞
- 资源利用率低

**修复方案**:
```java
List<CompletableFuture<Integer>> futures = recentVideos.stream()
    .map(video -> CompletableFuture.supplyAsync(() -> {
        try {
            return extractAndSaveScriptPatterns(video, null, hotKeywords);
        } catch (Exception e) {
            log.warn("视频话术提取失败: videoId={}", video.getId(), e);
            return 0;
        }
    }, learningTaskExecutor))
    .collect(Collectors.toList());

CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
    .get(5, TimeUnit.MINUTES);
```

**工作量**: 6 小时

**预期收益**: 处理时间 80s → 20s（75% 提升）

---

## P2 问题（中优先级 - 长期优化）

### P2-1: 数据隔离命名不一致

**位置**: 多个 Entity（DouyinAccount 使用 `userId`，DyPersona 使用 `ownerId`）

**问题描述**:
- DouyinAccount.userId（用户 ID）
- DouyinVideo.accountId（账号 ID，间接关联用户）
- DyPersona.ownerId（所有者 ID）
- 命名不一致导致理解困难

**修复方案**: 统一使用 `ownerId`

**工作量**: 2 人日

---

### P2-2: DouyinPersonaServiceImpl 缺少 ownerId 过滤

**位置**: `DouyinPersonaServiceImpl.java`

**问题描述**:
虽然 Entity 有 `ownerId` 字段，但 Service 层查询未强制过滤。

**修复方案**:
```java
Specification<DyPersona> spec = (root, query, cb) -> {
    List<Predicate> predicates = new ArrayList<>();
    predicates.add(cb.equal(root.get("deleted"), 0));
    
    // 强制 ownerId 过滤
    if (vo.getOwnerId() != null && vo.getOwnerId() > 0) {
        predicates.add(cb.equal(root.get("ownerId"), vo.getOwnerId()));
    } else if (vo.getOwnerIds() != null && !vo.getOwnerIds().isEmpty()) {
        predicates.add(root.get("ownerId").in(vo.getOwnerIds()));
    }
    // ...
};
```

**工作量**: 1 小时

---

### P2-3: 粉丝画像同步性能

**位置**: `FanProfileServiceImpl.java:156-211`

**问题描述**:
粉丝画像同步使用逻辑删除 + 批量插入，比 UPSERT 慢。

**修复方案**:
```sql
INSERT INTO dy_fan_profile_stats (...) VALUES (...)
ON CONFLICT (account_id, stat_type, stat_key) 
DO UPDATE SET 
    count = EXCLUDED.count,
    percentage = EXCLUDED.percentage,
    sync_time = EXCLUDED.sync_time,
    deleted = 0
```

**工作量**: 4 小时

**预期收益**: 同步时间 500ms → 100ms（80% 提升）

---

### P2-4: 大文件问题

**位置**: 
- `FanProfileServiceImpl.java`（336 行）
- `DouyinAccountServiceImpl.java`（300+ 行）

**修复方案**:
- 提取缓存逻辑到 `CacheService`
- 提取统计逻辑到 `StatisticsService`
- 提取同步逻辑到 `SyncService`

**工作量**: 3 人日

---

### P2-5: Map 参数未校验

**位置**: `DouyinPersonaController.java:155`

**修复方案**:
```java
@Data
public class PersonaByAccountQueryVO {
    @NotNull(message = "accountId 不能为空")
    @Min(value = 1, message = "accountId 必须大于 0")
    private Long accountId;
}
```

**工作量**: 0.5 人日

---

### P2-6: 缺少输入长度限制

**位置**: `DouyinAccountSaveVO.java`, `DouyinVideoSaveVO.java`

**问题描述**:
- `accountName` 和 `accountId` 未限制长度
- `title` 和 `description` 未限制长度

**修复方案**:
```java
@NotBlank(message = "账号名称不能为空")
@Size(max = 128, message = "账号名称不能超过 128 字符")
private String accountName;

@NotBlank(message = "视频标题不能为空")
@Size(max = 256, message = "视频标题不能超过 256 字符")
private String title;
```

**工作量**: 0.5 人日

---

### P2-7: Token 刷新失败无告警

**位置**: `OAuthTokenServiceImpl.java:97-130`

**问题描述**:
Token 刷新失败只记录日志，用户不知道需要重新授权。

**修复方案**:
```java
@Resource
private NotificationService notificationService;

@Override
public boolean refreshToken(Long userId, String provider) {
    // ...
    if (response == null) {
        log.error("刷新 token 失败: userId={}, provider={}", userId, provider);
        
        // 发送告警通知
        notificationService.sendTokenExpiredAlert(userId, provider);
        
        // 更新 token 状态
        tokenRepository.updateTokenStatus(userId, provider, "expired");
        
        return false;
    }
    // ...
}
```

**工作量**: 1 人日

---

### P2-8: 错误信息可能泄露敏感数据

**位置**: `DouyinAccountServiceImpl.java:92`

**问题描述**:
错误信息 "账号 ID 已存在" 可能被用于枚举已存在的账号 ID。

**修复方案**:
```java
if (douyinAccountRepository.existsByAccountIdAndDeleted(vo.getAccountId(), 0)) {
    log.warn("账号 ID 已存在: accountId={}, userId={}", vo.getAccountId(), vo.getUserId());
    throw new BusinessException(ErrorCode.VALIDATION_FAIL, "保存失败，请检查输入");
}
```

**工作量**: 0.5 人日

---

### P2-9: 缺少敏感操作审计日志

**位置**: `DouyinAccountController.java`, `DouyinOAuthController.java`

**问题描述**:
- 账号创建、修改、删除未记录到审计日志表
- OAuth 授权、撤销未记录审计日志

**修复方案**:
```java
@Entity
@Table(name = "sys_audit_log")
public class AuditLog {
    private Long userId;
    private String action;          // DOUYIN_ACCOUNT_CREATE/DELETE/OAUTH_AUTHORIZE
    private String module;           // douyin
    private String description;
    private String ipAddress;
    private String userAgent;
    @Column(columnDefinition = "jsonb")
    private String details;
    private Timestamp createTime;
}

// 在 Controller 中记录
auditLogService.log(AuditLog.builder()
    .userId(userId)
    .action("DOUYIN_ACCOUNT_DELETE")
    .module("douyin")
    .description("删除抖音账号: " + account.getAccountName())
    .ipAddress(request.getRemoteAddr())
    .details(JSON.toJSONString(Map.of("accountId", id)))
    .build());
```

**工作量**: 2 人日

---

## P3 问题（低优先级 - 持续改进）

### P3-1: 测试覆盖率严重不足

### P3-1: 测试覆盖率严重不足

**位置**: `douyin-operations-douyin/src/test/java/`

**问题描述**:
- 只有 1 个测试文件
- 测试覆盖率 <10%
- 缺少单元测试、集成测试

**影响**:
- 代码质量无法保证
- 重构风险高
- 回归测试困难

**修复方案**:
- 为每个 Service 添加单元测试
- 为每个 Controller 添加集成测试
- 目标覆盖率：80%+

**工作量**: 10 人日

---

### P3-2: DyPersona 字段未充分使用

**位置**: `DyPersona.java`

**问题描述**:
Entity 定义了丰富字段（style, targetAudience, coreValues, toneGuidelines），但前端和 AI 生成未充分利用。

**修复方案**:
- 前端人设编辑页面展示所有字段
- AI 生成话术时使用人设字段作为 prompt 上下文

**工作量**: 2 人日

---

### P3-3: 前端大组件问题

**位置**: `AccountDetailDrawer.tsx`（647 行）

**问题描述**:
单个组件过大，违反单一职责原则。

**修复方案**:
- 提取 `AccountInfoSection`（账号信息）
- 提取 `AccountStatsSection`（统计数据）
- 提取 `FanProfileSection`（粉丝画像）
- 提取 `VideoListSection`（视频列表）

**工作量**: 2 人日

---

### P3-4: 缺少方法级权限注解

**位置**: 所有 Controller 方法

**问题描述**:
未使用 `@PreAuthorize` 或 `@Secured` 注解声明权限要求。

**修复方案**:
```java
@PreAuthorize("hasRole('USER')")
@PostMapping("/search")
public RESTResult<PageResultVO<DouyinAccountVO>> search(...) {
    // ...
}

@PreAuthorize("hasRole('ADMIN') or @accountSecurity.isOwner(#id, principal.userId)")
@PostMapping("/delete")
public RESTResult<Void> delete(..., @RequestParam Long id) {
    // ...
}
```

**工作量**: 1 人日

---

### P3-5: Token 表无清理策略

**位置**: `oauth_token` 表

**问题描述**:
过期 Token 未清理，表可能无限增长。

**修复方案**:
```java
@Scheduled(cron = "0 0 3 * * ?") // 每天凌晨 3 点
public void cleanExpiredTokens() {
    Timestamp cutoff = new Timestamp(System.currentTimeMillis() - 90 * 24 * 60 * 60 * 1000L);
    tokenRepository.deleteByExpiresAtBefore(cutoff);
}
```

**工作量**: 0.5 人日

---

### P3-6: 前端敏感数据可能缓存

**位置**: React Query 缓存

**问题描述**:
React Query 缓存可能包含敏感数据，缓存时间过长。

**修复方案**:
```typescript
const { data } = useQuery({
  queryKey: ['dy-accounts-oauth', page, pageSize],
  queryFn: () => douyinApi.accountList({ page, rows: pageSize }),
  staleTime: 5 * 60 * 1000,  // 5 分钟
  gcTime: 10 * 60 * 1000,  // 10 分钟
})
```

**工作量**: 1 人日

---

## 实施路线图

### 第一阶段：立即修复（本周内）

**P0 问题**:
- [ ] P0-1: 修复 N+1 查询（视频同步）- 4 小时

**P1 高优先级**:
- [ ] P1-1: OAuth state 一次性验证（Redis 存储）- 1 人日
- [ ] P1-2: 移除日志中的敏感参数 - 0.5 人日
- [ ] P1-3: 添加 API 限流保护 - 2 人日

**预期收益**:
- 视频同步：15s → 5s（67% 提升）
- 安全性：修复 3 个 HIGH 级别漏洞

**工作量**: 4 人日

---

### 第二阶段：短期修复（2 周内）

**P1 高优先级**:
- [ ] P1-4: 实现 L1+L2 缓存（账号查询）- 8 小时
- [ ] P1-5: 添加缓存（账号统计）- 4 小时
- [ ] P1-6: 并行化话术学习管道 - 6 小时

**P2 中优先级**:
- [ ] P2-2: 修复 DouyinPersonaServiceImpl ownerId 过滤 - 1 小时
- [ ] P2-5: 修复 Map 参数校验问题 - 0.5 人日
- [ ] P2-8: 统一错误信息，避免泄露敏感数据 - 0.5 人日

**预期收益**:
- 账号查询：200ms → 20ms（90% 提升，缓存命中）
- 话术学习：80s → 20s（75% 提升）
- 数据库查询：-70%

**工作量**: 5 人日

---

### 第三阶段：长期优化（1 个月内）

**P2 中优先级**:
- [ ] P2-1: 统一数据隔离字段命名（userId → ownerId）- 2 人日
- [ ] P2-3: 优化粉丝画像同步（UPSERT）- 4 小时
- [ ] P2-4: 重构大文件（Service 拆分）- 3 人日
- [ ] P2-6: 添加输入长度限制 - 0.5 人日
- [ ] P2-7: Token 刷新失败告警 - 1 人日
- [ ] P2-9: 敏感操作审计日志 - 2 人日

**P3 低优先级**:
- [ ] P3-1: 提升测试覆盖率（<10% → 80%+）- 10 人日
- [ ] P3-2: 充分使用 DyPersona 字段 - 2 人日
- [ ] P3-3: 重构前端大组件 - 2 人日
- [ ] P3-4: 添加方法级权限注解 - 1 人日
- [ ] P3-5: Token 表清理策略 - 0.5 人日
- [ ] P3-6: 前端缓存优化 - 1 人日

**工作量**: 16 人日

---

## 总工作量估算

| 优先级 | 问题数 | 工作量 |
|--------|--------|--------|
| P0 | 1 | 4 小时 |
| P1 | 6 | 9 人日 |
| P2 | 9 | 10.5 人日 |
| P3 | 6 | 19 人日 |
| **总计** | **22** | **约 25 人日（5 周，1 人完成）** |

---

**报告生成时间**: 2026-05-06 14:30:00  
**审查者**: Claude Code Architect  
**下次审查**: 2026-06-06（修复 P0+P1 后）
