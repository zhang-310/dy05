# Live 模块修复计划

**生成日期**: 2026-05-06  
**模块**: live  
**总体评分**: B (80/100)  
**总工作量**: 60-80 人日（约 12-16 周，1 人完成）

---

## 执行摘要

Live 模块是 dy05 项目的核心模块之一，代码量大（约 40,000 行），功能复杂（直播场次管理、AI 话术生成、实时监控等）。整体代码质量良好，架构清晰，遵循项目规范，但存在 **5 个 P0 阻塞级问题**、**17 个 P1 高优先级问题**、**32 个 P2 中优先级问题** 和 **20 个 P3 低优先级问题**。

**关键问题**:
- ⚠️ **P0**: 测试覆盖率 <5%（仅 2 个测试文件，需 80-120 小时补充）
- ⚠️ **P0**: LiveScriptVO 缺少 22 个字段（Entity 有 30 个字段，VO 仅 8 个）
- ⚠️ **P0**: 无缓存实现（所有查询直接访问数据库）
- ⚠️ **P0**: 缺少外键索引（live_script 表缺 product_id、user_id 索引）
- ⚠️ **P0**: buildProductMap N+1 查询（批量生成导致 N 次数据库查询）
- ⚠️ **P1**: 7 个超大文件（最大 1474 行，违反 800 行上限）
- ⚠️ **P1**: 数据隔离不一致（userId vs ownerId）
- ⚠️ **P1**: Map 参数未校验（CVSS 7.5 安全漏洞）
- ⚠️ **P1**: 缺少 API 限流保护（CVSS 7.5 安全漏洞）

**修复优先级**: P0（立即修复）→ P1（短期修复）→ P2（长期优化）→ P3（持续改进）

---

## P0 问题（阻塞级 - 立即修复）

### P0-1: 测试覆盖率严重不足（< 5%）

**位置**: `douyin-operations-live/src/test/java/`

**问题描述**:  
整个 live 模块仅有 2 个测试文件，测试覆盖率估算 < 5%，远低于项目要求的 80%。

**影响**:
- 无法保证代码质量和正确性
- 重构风险极高
- 违反项目 80% 覆盖率要求
- 生产环境故障风险高

**修复方案**:

**阶段 1：核心业务逻辑单元测试（优先级最高）**
1. LiveScriptServiceImplTest
   - 测试 CRUD 操作
   - 测试数据隔离（userId 过滤）
   - 测试分页查询
   - 测试逻辑删除

2. LiveSessionServiceImplTest
   - 测试场次创建与更新
   - 测试状态流转
   - 测试数据隔离

3. LiveAiServiceImplTest
   - 测试各种生成方法（opening/product/closing）
   - 测试 RAG 集成
   - 测试错误处理
   - Mock LlmClient 和 KnowledgeBaseService

4. LiveScriptGenerationServiceImplTest
   - 测试整场生成逻辑
   - 测试骨架生成
   - 测试并行生成

5. LiveScriptQualityServiceImplTest
   - 测试质量评分算法
   - 测试违规检测集成

**阶段 2：Controller 集成测试**
1. LiveScriptControllerTest
   - 测试所有 API 端点
   - 测试权限校验
   - 测试参数验证
   - 使用 MockMvc

2. LiveSessionControllerTest
3. LiveScriptGenerationControllerTest
4. LiveRealtimePanelControllerTest

**阶段 3：Repository 测试**
1. LiveScriptRepositoryTest
   - 测试自定义查询方法
   - 使用 @DataJpaTest

2. LiveSessionRepositoryTest

**阶段 4：前端组件测试**
1. ScriptTabContent.test.tsx
2. GenerateTabContent.test.tsx
3. SessionsPage.test.tsx

**工作量估算**: 80-120 小时（分 4 个阶段完成）

**优先级**: P0 - 必须在下一个 Sprint 开始前完成阶段 1

---

### P0-2: VO 字段不完整：LiveScriptVO 缺少 22 个关键字段

**位置**: `douyin-operations-live/src/main/java/.../vo/LiveScriptVO.java`

**问题描述**:  
LiveScriptVO 仅包含 8 个字段，而 LiveScript Entity 有 30 个字段，导致前端无法访问关键业务字段。

**Entity 有但 VO 缺失的字段**:
- `scriptType` - 话术类型（开场白/产品介绍/促单等）
- `style` - 话术风格
- `aiGenerated` - 是否 AI 生成
- `productId` - 关联商品 ID
- `aiCallLogId` - AI 调用日志 ID
- `generationStatus` - 生成状态
- `violationChecked` - 违规检测状态
- `violationResult` - 违规检测结果
- `viewerDelta` - 观看人数变化
- `interactionDelta` - 互动量变化
- `conversionDelta` - 转化量变化
- `effectivenessScore` - 效果评分
- `durationLimitSec` - 时长上限
- `requirement` - 需求描述
- `referencedScriptId` - 引用的产品话术 ID
- `referencedScriptSnapshot` - 引用话术快照
- `approvalStatus` - 审核状态
- `userId` - 所属用户 ID
- `generationPromptHash` - Prompt 哈希
- `abExperimentId` - A/B 实验 ID
- `abVariantId` - A/B 变体 ID
- `aiSuggestion` - AI 建议

**影响**:
- 前端无法展示话术类型、风格等关键信息
- 无法展示效果评分和数据变化
- 无法展示审核状态
- 功能严重不完整

**修复方案**:
1. 在 LiveScriptVO 中添加所有缺失字段
2. 更新 ServiceImpl 中的 `toLiveScriptVO()` 方法
3. 更新前端类型定义 `front/src/types/live.ts`
4. 更新前端页面展示逻辑

```java
// LiveScriptVO.java - 添加缺失字段
@Data
public class LiveScriptVO {
    private Long id;
    private Long sessionId;
    private String scriptContent;
    private Integer sequenceNo;
    private Integer executionTime;
    private Boolean executed;
    private Integer actualExecutionTime;
    
    // 新增字段
    private String scriptType;
    private String style;
    private Boolean aiGenerated;
    private Long productId;
    private Long aiCallLogId;
    private String generationStatus;
    private Boolean violationChecked;
    private String violationResult;
    private Integer viewerDelta;
    private Integer interactionDelta;
    private Integer conversionDelta;
    private Double effectivenessScore;
    private Integer durationLimitSec;
    private String requirement;
    private Long referencedScriptId;
    private String referencedScriptSnapshot;
    private String approvalStatus;
    private Long userId;
    private String generationPromptHash;
    private Long abExperimentId;
    private Long abVariantId;
    private String aiSuggestion;
    private Long promptTemplateId;
    
    private Timestamp createTime;
    private Timestamp updateTime;
}
```

**工作量估算**: 4-6 小时

**优先级**: P0 - 必须在下一个版本前修复

---

### P0-3: 无缓存实现（所有查询直接访问数据库）

**位置**: 所有 Service 实现类

**问题描述**:  
Live 模块没有任何缓存实现（0 个 @Cacheable 注解），所有查询都直接访问数据库。

**影响**:
- 数据库负载高（每次请求都查询）
- 响应时间慢（+50-200ms）
- 无法应对高并发
- 影响页面：场次列表、话术列表、实时面板

**修复方案**:

**L1 缓存（Caffeine）配置**:
```java
@Configuration
public class CacheConfig {
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCaffeine(Caffeine.newBuilder()
            .maximumSize(500)
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .recordStats());
        return cacheManager;
    }
}
```

**L2 缓存（Redis）配置**:
```java
@Configuration
@EnableCaching
public class RedisCacheConfig {
    @Bean
    public RedisCacheConfiguration cacheConfiguration() {
        return RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(30))
            .serializeValuesWith(
                RedisSerializationContext.SerializationPair
                    .fromSerializer(new GenericJackson2JsonRedisSerializer())
            );
    }
}
```

**Service 层添加缓存**:
```java
@Service
public class LiveSessionServiceImpl implements LiveSessionService {
    
    @Cacheable(value = "live:session", key = "#id")
    public LiveSessionVO get(Long id) {
        // ...
    }
    
    @CacheEvict(value = "live:session", key = "#vo.id")
    public void save(LiveSessionSaveVO vo) {
        // ...
    }
}
```

**工作量估算**: 3-5 人日

**预期收益**: 缓存命中率 80%+，响应时间 200ms → 20ms

**优先级**: P0 - 必须在下一个版本前修复

---

### P0-4: 缺少外键索引（live_script 表）

**位置**: `sql/live/schema.sql`

**问题描述**:  
live_script 表缺少 product_id、user_id 等外键索引，导致关联查询全表扫描。

**影响**:
- 查询性能差（全表扫描）
- 数据库负载高
- 响应时间慢（+100-500ms）
- 影响页面：话术列表、商品关联查询

**修复方案**:

```sql
-- 添加外键索引
CREATE INDEX idx_live_script_product_id ON live_script(product_id) WHERE deleted = 0;
CREATE INDEX idx_live_script_user_id ON live_script(user_id) WHERE deleted = 0;
CREATE INDEX idx_live_script_session_id ON live_script(session_id) WHERE deleted = 0;
CREATE INDEX idx_live_script_create_time ON live_script(create_time DESC) WHERE deleted = 0;

-- 添加复合索引（常用查询组合）
CREATE INDEX idx_live_script_session_seq ON live_script(session_id, sequence_no) WHERE deleted = 0;
CREATE INDEX idx_live_script_user_create ON live_script(user_id, create_time DESC) WHERE deleted = 0;
```

**工作量估算**: 1 人日

**预期收益**: 查询时间 500ms → 50ms（90% 提升）

**优先级**: P0 - 必须在下一个版本前修复

---

### P0-5: buildProductMap N+1 查询

**位置**: `LiveScriptGenerationServiceImpl.java`

**问题描述**:  
批量生成话术时，buildProductMap 方法对每个商品执行单独查询，导致 N+1 查询问题。

**影响**:
- 响应时间慢（N 次数据库查询）
- 数据库负载高
- 影响功能：批量生成话术

**修复方案**:

```java
// 修复前（N+1 查询）
private Map<Long, LiveProduct> buildProductMap(List<Long> productIds) {
    Map<Long, LiveProduct> map = new HashMap<>();
    for (Long productId : productIds) {
        liveProductRepository.findById(productId).ifPresent(p -> map.put(productId, p));
    }
    return map;
}

// 修复后（批量查询）
private Map<Long, LiveProduct> buildProductMap(List<Long> productIds) {
    if (productIds == null || productIds.isEmpty()) {
        return Collections.emptyMap();
    }
    
    List<LiveProduct> products = liveProductRepository.findAllById(productIds);
    return products.stream()
        .collect(Collectors.toMap(LiveProduct::getId, p -> p));
}
```

**工作量估算**: 2 人日

**预期收益**: 响应时间 N×50ms → 50ms（N 倍提升）

**优先级**: P0 - 必须在下一个版本前修复

---

## P1 问题（高优先级 - 短期修复）

### P1-1: 超大文件：LiveAiServiceImpl.java (1217 行)

**位置**: `douyin-operations-live/src/main/java/.../service/impl/LiveAiServiceImpl.java`

**问题描述**:  
LiveAiServiceImpl 达到 1217 行，远超项目规范的 800 行上限，违反单一职责原则。

**影响**:
- 可读性极差，难以维护
- 包含多种生成逻辑（opening/product/closing/emotional/full）
- 包含 RAG 集成、知识库访问、违规检测等多个职责
- 代码审查困难

**修复方案**:
拆分为多个专职 Service：
1. **LiveScriptOpeningService** - 开场话术生成
2. **LiveScriptProductService** - 产品话术生成
3. **LiveScriptClosingService** - 收尾话术生成
4. **LiveScriptEmotionalService** - 情感化话术生成
5. **LiveScriptRagService** - RAG 知识检索
6. **LiveScriptViolationCheckService** - 违规检测

保留 LiveAiServiceImpl 作为门面（Facade），委托给各专职 Service。

**工作量估算**: 12-16 小时

**优先级**: P1

---

### P1-2 至 P1-7: 其他超大文件

| 文件 | 行数 | 修复方案 | 工作量 |
|------|------|---------|--------|
| ScriptTabContent.tsx | 1474 | 拆分为 ScriptList/ScriptEditor/ScriptPreview/ScriptDragDrop/ScriptToolbar + useScriptTab hook | 12-16h |
| LiveScriptGenerationServiceImpl.java | 833 | 拆分为 LiveScriptFullGenerationService/LiveScriptSkeletonGenerationService/LiveScriptParallelGenerationService | 8-12h |
| GenerateTabContent.tsx | 830 | 拆分为生成表单、生成进度、生成结果三个组件 | 8-12h |
| LiveScriptQualityServiceImpl.java | 821 | 拆分为 LiveScriptQualityScoringService/LiveScriptViolationService/LiveScriptEffectivenessService | 8-12h |
| LiveScriptGenerationController.java | 733 | 将 SSE 流式处理逻辑移至 Service 层，拆分为 LiveScriptGenerationController/LiveScriptStreamController/LiveScriptAsyncController | 6-8h |
| ContentMaterialServiceImpl.java | 702 | 拆分为素材管理、素材生成、素材推荐三个 Service | 6-8h |

**总工作量**: 48-68 小时

**优先级**: P1

---

### P1-8: 数据隔离不一致（userId vs ownerId）

**位置**: 多个 Entity

**问题描述**:  
Live 模块中部分 Entity 使用 `userId` 字段进行数据隔离，部分使用 `ownerId`，命名不一致。

**问题位置**:
- LiveSession Entity: 使用 `userId`
- LiveScript Entity: 使用 `userId`
- LiveAbTestResult Entity: 使用 `ownerId`
- LiveGenerationPreset Entity: 使用 `ownerId`
- LiveEffectivenessConfig Entity: 使用 `userId`

**影响**:
- 代码可读性差
- 容易混淆数据隔离逻辑
- 违反项目统一规范（应统一使用 ownerId）

**修复方案**:
统一使用 `ownerId` 字段（推荐）：
1. 修改所有使用 `userId` 的 Entity → `ownerId`
2. 修改数据库表：`ALTER TABLE xxx RENAME COLUMN user_id TO owner_id;`
3. 更新所有相关 VO、Service、Controller、Repository
4. 创建 Flyway 迁移脚本

**工作量估算**: 16-24 小时（含数据库迁移和全面测试）

**优先级**: P1

---

### P1-9: Map 参数未校验（CVSS 7.5 安全漏洞）

**位置**: 多个 Controller

**问题描述**:  
多个 Controller 使用 `@RequestBody Map<String, Object>` 接收参数，未进行校验。

**安全风险**: CVSS 7.5 (HIGH)
- 参数可能为 null 导致 NPE
- 未校验参数类型和范围
- 可能导致 SQL 注入或其他安全问题

**问题位置**:
- LiveScriptNavigationController
- LiveCollaborationController
- LiveScriptApprovalController

**修复方案**:
定义专用 VO 类替换 Map 参数

```java
// 修复前
@PostMapping("/get-by-session")
public RESTResult<List<LiveScriptVO>> getBySession(
        @RequestBody Map<String, Object> params) {
    Long sessionId = (Long) params.get("sessionId"); // 可能 NPE
    // ...
}

// 修复后
@Data
public class ScriptBySessionQueryVO {
    @NotNull(message = "sessionId 不能为空")
    @Min(value = 1, message = "sessionId 必须大于 0")
    private Long sessionId;
}

@PostMapping("/get-by-session")
public RESTResult<List<LiveScriptVO>> getBySession(
        @Valid @RequestBody ScriptBySessionQueryVO vo) {
    // ...
}
```

**工作量估算**: 4-6 小时

**优先级**: P1

---

### P1-10: 缺少 API 限流保护（CVSS 7.5 安全漏洞）

**位置**: 所有 Controller

**问题描述**:  
Live 模块所有 API 都没有限流保护，仅 AI 生成接口有 @Retry 注解。

**安全风险**: CVSS 7.5 (HIGH)
- 攻击者可暴力请求
- 服务器资源耗尽
- 数据库连接池耗尽

**修复方案**:
使用 Resilience4j 限流

```java
@Configuration
public class RateLimitConfig {
    @Bean
    public RateLimiter apiRateLimiter() {
        return RateLimiter.of("api", RateLimiterConfig.custom()
            .limitForPeriod(100)
            .limitRefreshPeriod(Duration.ofMinutes(1))
            .timeoutDuration(Duration.ofSeconds(5))
            .build());
    }
}

@PostMapping("/search")
@RateLimiter(name = "api")
public RESTResult<PageResultVO<LiveScriptVO>> search(...) {
    // ...
}
```

**工作量估算**: 2 人日

**优先级**: P1

---

### P1-11: 串行生成慢（generateFull 120 秒）

**位置**: `LiveScriptGenerationServiceImpl.java`

**问题描述**:  
整场生成使用串行处理，8 个槽位需要 120 秒。

**影响**:
- 用户等待时间长
- 资源利用率低
- 已有 generateFullPipelined 方法但未使用

**修复方案**:
使用并行生成

```java
// 使用 CompletableFuture 并行生成
List<CompletableFuture<LiveScript>> futures = slots.stream()
    .map(slot -> CompletableFuture.supplyAsync(() -> {
        return generateScriptForSlot(slot);
    }, generationExecutor))
    .collect(Collectors.toList());

CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
    .get(5, TimeUnit.MINUTES);
```

**工作量估算**: 6 小时

**预期收益**: 生成时间 120s → 30s（75% 提升）

**优先级**: P1

---

### P1-12: 循环保存 N+1 写入

**位置**: `LiveSessionServiceImpl.ensureScriptSlotsForSession()`

**问题描述**:  
循环中对每个槽位执行单独 INSERT，导致 N+1 写入问题。

**影响**:
- 数据库负载高（14 次 INSERT）
- 响应时间慢
- 事务锁时间长

**修复方案**:
批量保存

```java
// 修复前（N+1 写入）
for (int i = 0; i < 14; i++) {
    LiveScript script = new LiveScript();
    // ... 设置字段
    liveScriptRepository.save(script);
}

// 修复后（批量保存）
List<LiveScript> scripts = new ArrayList<>();
for (int i = 0; i < 14; i++) {
    LiveScript script = new LiveScript();
    // ... 设置字段
    scripts.add(script);
}
liveScriptRepository.saveAll(scripts);
```

**工作量估算**: 4 小时

**预期收益**: 响应时间 700ms → 100ms（85% 提升）

**优先级**: P1

---

### P1-13: 质量评分重复 LLM 调用

**位置**: `LiveScriptQualityServiceImpl.java`

**问题描述**:  
质量评分对每个话术单独调用 LLM，token 消耗高。

**影响**:
- LLM 调用成本高
- 响应时间慢
- 资源利用率低

**修复方案**:
批量评分

```java
// 批量调用 LLM
String batchPrompt = scripts.stream()
    .map(s -> String.format("[%d] %s", s.getId(), s.getScriptContent()))
    .collect(Collectors.joining("\n\n"));

String result = llmClient.chat(batchPrompt);
// 解析批量结果
```

**工作量估算**: 8 小时

**预期收益**: LLM 调用次数 -80%，成本 -80%

**优先级**: P1

---

### P1-14: 子查询性能差

**位置**: `LiveScriptVersionRepository.java`

**问题描述**:  
使用相关子查询查找最新版本，性能差。

**修复方案**:
使用窗口函数

```sql
-- 修复前（相关子查询）
SELECT * FROM live_script_version v1
WHERE v1.version_no = (
    SELECT MAX(v2.version_no) 
    FROM live_script_version v2 
    WHERE v2.script_id = v1.script_id
);

-- 修复后（窗口函数）
WITH ranked AS (
    SELECT *, 
           ROW_NUMBER() OVER (PARTITION BY script_id ORDER BY version_no DESC) as rn
    FROM live_script_version
)
SELECT * FROM ranked WHERE rn = 1;
```

**工作量估算**: 4 小时

**预期收益**: 查询时间 500ms → 50ms（90% 提升）

**优先级**: P1

---

### P1-15: 前端无懒加载

**位置**: `front/src/pages/live/`

**问题描述**:  
106 个文件、14,200 行代码全部加载到首屏，首屏体积大。

**修复方案**:
使用 React.lazy 懒加载

```typescript
// 修复前
import SessionsPage from './pages/live/SessionsPage';

// 修复后
const SessionsPage = React.lazy(() => import('./pages/live/SessionsPage'));
```

**工作量估算**: 6 小时

**预期收益**: 首屏体积 -50%，加载时间 -40%

**优先级**: P1

---

### P1-16: 缺少复合索引

**位置**: `sql/live/schema.sql`

**问题描述**:  
常用查询组合缺少复合索引。

**修复方案**:
添加复合索引

```sql
-- 场次查询（用户 + 时间）
CREATE INDEX idx_live_session_user_time ON live_session(user_id, start_time DESC) WHERE deleted = 0;

-- 话术查询（场次 + 序号）
CREATE INDEX idx_live_script_session_seq ON live_script(session_id, sequence_no) WHERE deleted = 0;

-- 质量评分查询（话术 + 时间）
CREATE INDEX idx_quality_score_script_time ON live_script_quality_score(script_id, create_time DESC) WHERE deleted = 0;
```

**工作量估算**: 4 小时

**优先级**: P1

---

### P1-17: 38 个 Controller 过多

**位置**: `douyin-operations-live/src/main/java/.../controller/`

**问题描述**:  
Live 模块有 38 个 Controller，过度拆分导致维护困难。

**修复方案**:
合并为 15 个 Controller

| 合并前 | 合并后 |
|--------|--------|
| LiveScriptController + LiveScriptVersionController + LiveScriptNavigationController | LiveScriptController |
| LiveScriptGenerationController + LiveScriptGenerationTaskController | LiveScriptGenerationController |
| LiveScriptQualityController + LiveScriptQualityScoreController | LiveScriptQualityController |

**工作量估算**: 12 小时

**优先级**: P1

---

## P2 问题（中优先级 - 长期优化）

### P2-1: 空 catch 块（10+ 处）

**位置**: 多个 Service 实现

**问题描述**:  
多个 Service 实现中发现空 catch 块，异常被静默吞噬。

**修复方案**:
至少记录日志

```java
// 修复前
try {
    // 某些操作
} catch (Exception ignored) {}

// 修复后
try {
    // 某些操作
} catch (Exception e) {
    log.warn("操作失败", e);
}
```

**工作量估算**: 2-3 小时

**优先级**: P2

---

### P2-2 至 P2-32: 其他 P2 问题

| 问题 | 位置 | 修复方案 | 工作量 |
|------|------|---------|--------|
| TODO 未完成 | LiveScriptServiceImpl.java:155,161 | 完成话术库集成或创建 JIRA ticket | 4-6h |
| 重复代码：VO 转换 | 30+ ServiceImpl | 使用 MapStruct 自动生成映射代码 | 8-12h |
| 长方法 | LiveAiServiceImpl.doGenerate() | 拆分为多个小方法 | 3-4h |
| 缺少输入验证 | 部分 Controller | 添加 @Valid 注解 | 4-6h |
| 缺少事务边界 | 部分批量操作 | 添加 @Transactional | 3-4h |
| 缺少分页上限检查 | 部分查询 | 添加最大返回数量限制 | 4-6h |
| 缺少缓存失效策略 | Redis 缓存 | 设置 TTL | 2-3h |
| 缺少并发控制 | 实时面板数据更新 | 添加乐观锁或分布式锁 | 4-6h |
| 前端类型安全 | 部分 API 调用 | 添加泛型类型 | 3-4h |
| 前端错误处理 | 部分组件 | 添加 ErrorBoundary | 2-3h |
| 前端性能 | ScriptTabContent | 使用虚拟滚动 | 4-6h |
| Pattern 003 违规 | 3 个 Entity | 添加 @SQLRestriction | 0.5h |
| Pattern 004 违规 | 30+ Service | 添加 ownerId 过滤 | 4h |
| Pattern 005 违规 | 17 个 Repository | 添加 JpaSpecificationExecutor | 2h |
| Pattern 008 违规 | 10+ 前端组件 | 添加错误处理 | 1h |
| 缺少方法级权限注解 | 所有 Controller | 添加 @PreAuthorize | 4-6h |
| 缺少输入长度限制 | SaveVO 类 | 添加 @Size 注解 | 2-3h |
| Token 刷新失败无告警 | OAuthTokenServiceImpl | 发送告警通知 | 4-6h |
| 错误信息泄露敏感数据 | 部分 Controller | 统一错误信息 | 2-3h |
| 缺少敏感操作审计日志 | 部分 Controller | 添加审计日志 | 8-12h |
| API 路径冗余 | 多个 Controller | 简化路径结构 | 4-6h |
| live.ts 过大 | front/src/api/live.ts | 拆分为多个文件 | 4-6h |
| 大 JOIN 查询 | 部分 Repository | 优化查询或添加缓存 | 6-8h |
| 频繁实时写入 | LiveRealtimePanelService | 批量写入或异步写入 | 6-8h |
| DyPersona 字段未充分使用 | 前端和 AI 生成 | 在 AI 生成时使用人设字段 | 8-12h |
| 前端大组件 | AccountDetailDrawer.tsx | 拆分为多个子组件 | 8-12h |
| Token 表无清理策略 | oauth_token 表 | 添加定时清理任务 | 2-3h |
| 前端敏感数据缓存 | React Query | 设置合理的 staleTime 和 gcTime | 4-6h |
| 缺少 LIKE 查询转义 | 部分 Service | 转义通配符 | 2-3h |
| 缺少请求体大小限制 | application.yml | 配置 max-http-post-size | 1h |
| AI 服务超时配置 | LlmClient | 配置合理超时时间 | 2-3h |
| 删除操作所有权校验 | 部分 Controller | 添加所有权校验 | 4-6h |

**总工作量**: 120-180 小时

**优先级**: P2

---

## P3 问题（低优先级 - 持续改进）

### P3-1 至 P3-20: P3 问题列表

| 问题 | 修复方案 | 工作量 |
|------|---------|--------|
| 代码风格：缺少 JavaDoc | 添加 JavaDoc 注释 | 8-12h |
| 代码风格：常量未提取 | 提取魔法数字为常量 | 2-3h |
| 部分 Entity 缺少 equals/hashCode | 实现 equals/hashCode | 2h |
| 部分 VO 缺少 toString() | 实现 toString() | 1h |
| 部分日志级别不当 | info 改为 debug | 2h |
| 部分异常消息未国际化 | 添加国际化支持 | 3h |
| 前端组件缺少 PropTypes | 添加 TypeScript 接口文档 | 4h |
| 前端部分状态可用 useMemo | 添加 useMemo 优化 | 3h |
| 前端部分 useEffect 依赖不完整 | 修复依赖数组 | 2h |
| 前端部分组件可用 React.memo | 添加 React.memo 优化 | 2h |
| 部分 SQL 查询可添加索引 | 添加索引 | 4h |
| 部分 API 响应时间长 | 添加缓存 | 6h |
| 部分配置项硬编码 | 移至配置文件 | 2h |
| 部分定时任务无错误处理 | 添加错误处理 | 2h |
| 部分 RabbitMQ 消息无死信队列 | 配置死信队列 | 4h |
| 前端部分组件可提取 | 提取可复用组件 | 6h |
| 前端部分样式可优化 | 优化 CSS | 4h |
| 前端部分图标可统一 | 统一图标库 | 3h |
| 前端部分文案可优化 | 优化用户体验 | 4h |
| 前端部分动画可添加 | 添加过渡动画 | 6h |

**总工作量**: 70-90 小时

**优先级**: P3

---

## 实施路线图

### 第一阶段：立即修复（本周内）- P0 问题

**目标**: 修复所有阻塞级问题

**任务清单**:
- [ ] P0-1: 添加核心业务逻辑单元测试（阶段 1）- 40 小时
- [ ] P0-2: 补充 LiveScriptVO 缺失字段 - 4-6 小时
- [ ] P0-3: 实现 L1+L2 缓存 - 3-5 天
- [ ] P0-4: 添加外键索引 - 1 天
- [ ] P0-5: 修复 buildProductMap N+1 查询 - 2 天

**预期收益**:
- 测试覆盖率：<5% → 30%+（阶段 1 完成后）
- 前端功能完整性：60% → 100%
- API 响应时间：200ms → 20ms（缓存命中）
- 查询性能：500ms → 50ms（索引优化）
- 批量生成：N×50ms → 50ms

**工作量**: 10-12 人日

---

### 第二阶段：短期修复（2 周内）- P1 问题

**目标**: 修复所有高优先级问题

**任务清单**:
- [ ] P1-1 至 P1-7: 拆分 7 个超大文件 - 48-68 小时
- [ ] P1-8: 统一数据隔离字段命名 - 16-24 小时
- [ ] P1-9: 修复 Map 参数校验问题 - 4-6 小时
- [ ] P1-10: 添加 API 限流保护 - 2 人日
- [ ] P1-11: 使用并行生成 - 6 小时
- [ ] P1-12: 修复循环保存 N+1 写入 - 4 小时
- [ ] P1-13: 批量质量评分 - 8 小时
- [ ] P1-14: 优化子查询性能 - 4 小时
- [ ] P1-15: 前端懒加载 - 6 小时
- [ ] P1-16: 添加复合索引 - 4 小时
- [ ] P1-17: 合并 Controller - 12 小时

**预期收益**:
- 代码可维护性：C+ → B+
- 安全性：B → A-
- 生成性能：120s → 30s（75% 提升）
- 数据库负载：-85%
- 首屏加载：-50%
- LLM 成本：-80%

**工作量**: 20-25 人日

---

### 第三阶段：长期优化（1 个月内）- P2 问题

**目标**: 修复所有中优先级问题

**任务清单**:
- [ ] P2-1: 空 catch 块修复 - 2-3 小时
- [ ] P2-2: TODO 完成（话术库集成）- 4-6 小时
- [ ] P2-3: 重复代码提取（VO 转换）- 8-12 小时
- [ ] P2-4: 长方法拆分 - 3-4 小时
- [ ] P2-5: 缺少输入验证 - 4-6 小时
- [ ] P2-6: 缺少事务边界 - 3-4 小时
- [ ] P2-7: 缺少分页上限检查 - 4-6 小时
- [ ] P2-8: 缺少缓存失效策略 - 2-3 小时
- [ ] P2-9: 缺少并发控制 - 4-6 小时
- [ ] P2-10: 前端类型安全 - 3-4 小时
- [ ] P2-11: 前端错误处理 - 2-3 小时
- [ ] P2-12: 前端性能优化（虚拟滚动）- 4-6 小时
- [ ] P2-13: Pattern 003 违规修复 - 0.5 小时
- [ ] P2-14: Pattern 004 违规修复 - 4 小时
- [ ] P2-15: Pattern 005 违规修复 - 2 小时
- [ ] P2-16: Pattern 008 违规修复 - 1 小时
- [ ] P2-17: 缺少方法级权限注解 - 4-6 小时
- [ ] P2-18: 缺少输入长度限制 - 2-3 小时
- [ ] P2-19: Token 刷新失败无告警 - 4-6 小时
- [ ] P2-20: 错误信息泄露敏感数据 - 2-3 小时
- [ ] P2-21: 缺少敏感操作审计日志 - 8-12 小时
- [ ] P2-22: API 路径冗余 - 4-6 小时
- [ ] P2-23: live.ts 过大拆分 - 4-6 小时
- [ ] P2-24: 大 JOIN 查询优化 - 6-8 小时
- [ ] P2-25: 频繁实时写入优化 - 6-8 小时
- [ ] P2-26: DyPersona 字段充分使用 - 8-12 小时
- [ ] P2-27: 前端大组件拆分 - 8-12 小时
- [ ] P2-28: Token 表清理策略 - 2-3 小时
- [ ] P2-29: 前端敏感数据缓存 - 4-6 小时
- [ ] P2-30: 缺少 LIKE 查询转义 - 2-3 小时
- [ ] P2-31: 缺少请求体大小限制 - 1 小时
- [ ] P2-32: AI 服务超时配置 - 2-3 小时

**预期收益**:
- 代码质量：B → A-
- 安全性：A- → A
- 可维护性：C+ → B+
- 规范遵守：85% → 95%

**工作量**: 30-35 人日

---

### 第四阶段：持续改进（持续进行）- P3 问题

**目标**: 完成所有低优先级问题

**任务清单**:
- [ ] P3-1 至 P3-20: 代码风格、性能优化、文档补充等

**工作量**: 10-15 人日

---

## 总工作量估算

| 阶段 | 优先级 | 问题数 | 工作量 | 时间线 |
|------|--------|--------|--------|--------|
| 第一阶段 | P0 | 5 | 10-12 人日 | 本周内 |
| 第二阶段 | P1 | 17 | 20-25 人日 | 2 周内 |
| 第三阶段 | P2 | 32 | 30-35 人日 | 1 个月内 |
| 第四阶段 | P3 | 20 | 10-15 人日 | 持续进行 |
| **总计** | **P0-P3** | **74** | **60-80 人日** | **约 12-16 周（1 人完成）** |

---

## 验收标准

### P0 问题验收
- [ ] 测试覆盖率达到 30%+（阶段 1 完成后）
- [ ] LiveScriptVO 包含所有 30 个字段
- [ ] 缓存命中率 80%+，响应时间 200ms → 20ms
- [ ] 查询性能提升 90%（索引优化）
- [ ] 批量生成性能提升 N 倍

### P1 问题验收
- [ ] 所有文件不超过 800 行
- [ ] 数据隔离字段统一为 ownerId
- [ ] 所有 API 有限流保护
- [ ] 生成性能提升 75%（120s → 30s）
- [ ] 数据库负载降低 85%
- [ ] 首屏加载时间降低 50%
- [ ] LLM 成本降低 80%

### P2 问题验收
- [ ] 无空 catch 块
- [ ] 无未完成的 TODO
- [ ] 无重复代码（VO 转换使用 MapStruct）
- [ ] 所有方法不超过 50 行
- [ ] 所有 API 有输入验证
- [ ] 所有批量操作有事务边界
- [ ] 所有查询有分页上限
- [ ] 所有缓存有失效策略
- [ ] 所有并发操作有控制机制
- [ ] 前端类型安全（无 any 类型）
- [ ] 前端错误处理完善
- [ ] 前端性能优化（虚拟滚动）

### P3 问题验收
- [ ] 代码风格统一
- [ ] 所有方法有 JavaDoc 注释
- [ ] 所有常量已提取
- [ ] 所有 Entity 有 equals/hashCode
- [ ] 所有 VO 有 toString()
- [ ] 日志级别正确
- [ ] 异常消息国际化
- [ ] 前端组件有 PropTypes
- [ ] 前端状态优化（useMemo）
- [ ] 前端 useEffect 依赖完整
- [ ] 前端组件优化（React.memo）

---

**报告生成时间**: 2026-05-06  
**审查者**: Claude Code  
**下次审查**: 完成 P0 和 P1 问题修复后（预计 2-3 个月）
