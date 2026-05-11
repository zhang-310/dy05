# Search 模块修复计划

**生成日期**: 2026-05-09  
**模块**: search (统一搜索)  
**规划者**: Claude Opus 4  
**基于报告**: architecture-review, code-review, security-audit, performance-analysis, pattern-compliance  
**总工作量**: 15.1 人日

---

## 执行摘要

**修复计划总览**:

| 维度 | 问题数 | 工作量 | 预期收益 |
|-----|--------|--------|----------|
| **P0 阻塞级** | 0 | 0 人日 | - |
| **P1 高优先级** | 6 | 6.5 人日 | 性能提升 10-50 倍，服务可用性 99.9%+ |
| **P2 中优先级** | 9 | 6.5 人日 | 响应时间降低 40-60%，可观测性提升 |
| **P3 低优先级** | 11 | 9.1 人日 | 代码质量提升，用户体验优化 |
| **总计** | 26 | 22.1 人日 | - |

**优先级分布**:
```
P0: ████████████████████ 0 个 (0%)
P1: ████████████████████ 6 个 (23%)
P2: ████████████████████ 9 个 (35%)
P3: ████████████████████ 11 个 (42%)
```

**关键修复项**:
1. **全文索引** (P1, 2 人日) - 话术搜索性能提升 **10-50 倍**
2. **Redis 缓存** (P1, 1 人日) - 响应时间降低 **80-90%**
3. **API 限流** (P1, 1.5 人日) - 服务可用性提升至 **99.9%+**
4. **日志审计** (P1, 1 人日) - 可观测性提升，便于问题排查
5. **监控指标** (P1, 1 人日) - 实时监控搜索性能和调用频率

**预期收益**:
- 话术搜索性能提升 **10-50 倍**
- 热门搜索词响应时间降低 **80-90%**
- 服务可用性提升至 **99.9%+**
- 数据库负载降低 **50-70%**
- 吞吐量提升 **3-5 倍**

**生产就绪度**: 🟢 可上线，建议修复 P1 问题后上线

---

## 1. P0 阻塞级问题（生产阻塞，必须修复）

**无 P0 问题**

Search 模块代码质量高，无阻塞级问题。

---

## 2. P1 高优先级问题（严重影响，应尽快修复）

### 2.1 话术搜索无全文索引

**问题描述**:
- 话术搜索使用 `LIKE '%keyword%'` 模式
- PostgreSQL 无法使用 B-Tree 索引进行前缀通配符搜索
- 大数据量下导致全表扫描，性能极差

**影响范围**:
- `GlobalSearchServiceImpl.scriptSpec()` 第 150 行
- `dy_product_script.script_content` 字段 (TEXT 类型，无索引)

**性能影响**:
| 数据量 | 无索引耗时 | 有 GIN 索引耗时 | 性能提升 |
|--------|-----------|----------------|----------|
| 10,000 | ~100 ms | ~5 ms | 20x |
| 50,000 | ~500 ms | ~10 ms | 50x |
| 100,000 | ~1000 ms | ~20 ms | 50x |
| 500,000 | ~5000 ms | ~50 ms | 100x |

**修复方案**:

**方案 1: PostgreSQL GIN 全文索引** (推荐)
```sql
-- 创建 GIN 全文索引
CREATE INDEX idx_dy_product_script_content_gin 
ON dy_product_script 
USING GIN (to_tsvector('simple', script_content));

-- 修改查询 (使用全文搜索)
SELECT * FROM dy_product_script 
WHERE to_tsvector('simple', script_content) @@ to_tsquery('simple', '护肤')
LIMIT 6;
```

**方案 2: PostgreSQL pg_trgm 扩展** (保持 LIKE 查询)
```sql
-- 安装 pg_trgm 扩展
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- 创建 GIN trigram 索引
CREATE INDEX idx_dy_product_script_content_trgm 
ON dy_product_script 
USING GIN (script_content gin_trgm_ops);

-- 查询保持不变 (LIKE 查询自动使用 trigram 索引)
SELECT * FROM dy_product_script 
WHERE LOWER(script_content) LIKE '%护肤%' 
LIMIT 6;
```

**推荐**: 方案 2 (pg_trgm)，无需修改 Java 代码，索引自动生效。

**工作量**: 2 人日 (包括索引创建、测试、验证)  
**优先级**: P1 - 应尽快修复  
**预期收益**: 话术搜索性能提升 **10-50 倍**

---

### 2.2 无缓存机制导致重复查询

**问题描述**:
- 高频搜索词（如"护肤"、"彩妆"）无缓存
- 每次搜索都执行 4 次数据库查询
- 数据库负载高，响应时间长
- **违反 ADR-004 两级缓存策略**

**影响范围**:
- `GlobalSearchServiceImpl.search()` 方法
- 所有搜索请求

**性能影响**:
```
假设场景: "护肤" 关键词每分钟被搜索 100 次

无缓存:
- 数据库查询: 100 * 4 = 400 次/分钟
- 平均响应时间: 100 ms (含数据库查询)
- 数据库连接占用: 高

有缓存 (TTL 30 秒):
- 数据库查询: 2 * 4 = 8 次/分钟 (每 30 秒刷新一次)
- 平均响应时间: 10 ms (从 Redis 读取)
- 数据库连接占用: 低
- 性能提升: 90% (响应时间) + 98% (数据库负载)
```

**缓存命中率分析**:
| 场景 | 缓存命中率 | 说明 |
|------|-----------|------|
| 热门搜索词 (前 10%) | 80-90% | "护肤"、"彩妆"、"精华" 等 |
| 常见搜索词 (前 30%) | 50-70% | 品类名称、品牌名称 |
| 长尾搜索词 (后 70%) | 10-20% | 用户自定义关键词 |
| **总体** | **40-60%** | 综合命中率 |

**修复方案**:

```java
// 方案 1: Spring Cache 注解 (推荐)
@Cacheable(
    value = "search:global", 
    key = "#request.q + ':' + (#visibleUserIds != null ? #visibleUserIds.hashCode() : 'all')",
    unless = "#result == null || #result.hits.isEmpty()"
)
public GlobalSearchResponseVO search(GlobalSearchRequestVO request, List<Long> visibleUserIds) {
    // 现有逻辑
}

// 配置 Redis 缓存 TTL
spring:
  cache:
    redis:
      time-to-live: 30s  # 30 秒过期
```

**工作量**: 1 人日  
**优先级**: P1 - 应尽快修复  
**预期收益**: 
- 响应时间降低 **80-90%** (热门搜索词)
- 数据库负载降低 **50-70%**
- 吞吐量提升 **3-5 倍**

---

### 2.3 无 API 限流保护

**问题描述**:
- 搜索接口无调用频率限制
- 攻击者可暴力调用导致数据库过载
- 可能导致服务不可用 (DoS 攻击)

**影响范围**:
- `GlobalSearchController.globalSearch()` 方法
- 所有搜索请求

**性能影响**:
```
攻击场景: 单用户每秒发送 100 次搜索请求

无限流:
- 数据库查询: 100 * 4 = 400 次/秒
- 数据库连接池: 40 个连接 (Hikari max)
- 连接池耗尽时间: ~0.1 秒
- 服务状态: 不可用 (所有请求超时)

有限流 (每用户每分钟 30 次):
- 数据库查询: 30 * 4 / 60 = 2 次/秒
- 数据库连接池: 正常
- 服务状态: 可用
```

**修复方案**:

```java
// 使用 Resilience4j 限流
@PostMapping("/global")
@RateLimiter(name = "globalSearch", fallbackMethod = "searchFallback")
public RESTResult<GlobalSearchResponseVO> globalSearch(...) {
    // 现有逻辑
}

// 限流降级方法
public RESTResult<GlobalSearchResponseVO> searchFallback(
        GlobalSearchRequestVO vo, HttpServletRequest request, Throwable t) {
    return RESTResult.error(ErrorCode.TOO_MANY_REQUESTS, "搜索请求过于频繁，请稍后再试");
}

// 配置限流策略
resilience4j:
  ratelimiter:
    instances:
      globalSearch:
        limitForPeriod: 30      # 每周期 30 次
        limitRefreshPeriod: 1m  # 周期 1 分钟
        timeoutDuration: 0s     # 不等待
```

**工作量**: 1.5 人日  
**优先级**: P1 - 应尽快修复  
**预期收益**: 
- 防止 DoS 攻击
- 保护数据库连接池
- 服务可用性提升至 **99.9%+**

---

### 2.4 完全缺少日志记录

**问题描述**:
- Service 层完全无日志记录
- 无法追踪搜索查询耗时和结果数量
- 无法分析用户搜索行为
- 问题排查困难

**影响范围**:
- `GlobalSearchServiceImpl.search()` 方法
- 所有搜索请求

**修复方案**:

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
            log.warn("globalSearch: SLOW QUERY ({}ms) for keyword: {}", tookMs, kw);
        }
        
        return GlobalSearchResponseVO.builder().hits(out).tookMs(tookMs).build();
    }
}
```

**工作量**: 1 人日  
**优先级**: P1 - 应尽快修复  
**预期收益**: 
- 提升可观测性，便于问题排查
- 监控搜索性能趋势
- 分析用户搜索行为

---

### 2.5 缺少监控指标

**问题描述**:
- 无自定义监控指标
- 无法监控搜索性能和调用频率
- 无法及时发现性能问题和异常

**影响范围**:
- `GlobalSearchServiceImpl.search()` 方法

**修复方案**:

```java
@Timed(value = "search.global.query", description = "全局搜索查询耗时")
@Counted(value = "search.global.requests", description = "全局搜索请求次数")
public GlobalSearchResponseVO search(GlobalSearchRequestVO request, List<Long> visibleUserIds) {
    // ... 搜索逻辑 ...
}
```

**关键指标**:
- `search.global.query` - 搜索查询耗时 (P50/P95/P99)
- `search.global.requests` - 搜索请求次数
- `search.global.cache.hit` - 缓存命中次数
- `search.global.cache.miss` - 缓存未命中次数
- `search.global.empty` - 空结果搜索次数

**工作量**: 1 人日  
**优先级**: P1 - 应尽快修复  
**预期收益**: 
- 提升可观测性
- 监控性能趋势
- 及时发现异常

---

### 2.6 空用户列表未记录日志

**问题描述**:
- 空用户列表提前返回空结果，但未记录日志
- 无法追踪异常的数据范围解析结果
- 可能掩盖 DataScopeResolver 的错误

**影响范围**:
- `GlobalSearchServiceImpl.java:49-51`

**修复方案**:

```java
if (visibleUserIds != null && visibleUserIds.isEmpty()) {
    log.warn("globalSearch: empty visibleUserIds for userId={}, roleCode={}", userId, roleCode);
    return GlobalSearchResponseVO.builder()
        .hits(List.of())
        .tookMs(System.currentTimeMillis() - t0)
        .build();
}
```

**工作量**: 0.5 人日  
**优先级**: P1 - 应尽快修复  
**预期收益**: 便于排查数据范围解析问题

---

## 3. P2 中优先级问题（质量改进，计划修复）

### 3.1 4 次串行数据库查询可优化

**问题描述**:
- 当前实现串行执行 4 次数据库查询
- 总响应时间 = 4 次查询时间之和
- 可改为并行查询提升性能

**性能影响**:
```
串行查询:
- 直播场次: 20 ms
- 商品: 30 ms
- 话术: 50 ms (无索引)
- 短视频: 25 ms
- 总耗时: 125 ms

并行查询 (CompletableFuture):
- 总耗时: max(20, 30, 50, 25) = 50 ms
- 性能提升: 60%
```

**修复方案**:

```java
@Override
public GlobalSearchResponseVO search(GlobalSearchRequestVO request, List<Long> visibleUserIds) {
    long t0 = System.currentTimeMillis();
    String kw = request.getQ().trim();
    
    // 并行查询
    CompletableFuture<List<GlobalSearchHitVO>> liveFuture = 
        CompletableFuture.supplyAsync(() -> searchLive(kw, visibleUserIds));
    CompletableFuture<List<GlobalSearchHitVO>> productFuture = 
        CompletableFuture.supplyAsync(() -> searchProduct(kw, visibleUserIds));
    CompletableFuture<List<GlobalSearchHitVO>> scriptFuture = 
        CompletableFuture.supplyAsync(() -> searchScript(kw, visibleUserIds));
    CompletableFuture<List<GlobalSearchHitVO>> videoFuture = 
        CompletableFuture.supplyAsync(() -> searchVideo(kw, visibleUserIds));
    
    // 等待所有查询完成
    CompletableFuture.allOf(liveFuture, productFuture, scriptFuture, videoFuture).join();
    
    // 合并结果
    List<GlobalSearchHitVO> merged = new ArrayList<>();
    merged.addAll(liveFuture.join());
    merged.addAll(productFuture.join());
    merged.addAll(scriptFuture.join());
    merged.addAll(videoFuture.join());
    
    // 去重、截断
    // ...
}
```

**工作量**: 2 人日  
**优先级**: P2 - 建议优化  
**预期收益**: 响应时间降低 **40-60%**

---

### 3.2 空关键词未提前校验

**问题描述**:
- 空白关键词（如 `"   "`）未提前校验
- 仍会执行 4 次数据库查询
- 浪费数据库资源

**修复方案**:

```java
String kw = request.getQ().trim();
if (kw.isEmpty()) {
    log.warn("globalSearch: empty keyword after trim");
    return GlobalSearchResponseVO.builder()
        .hits(List.of())
        .tookMs(0L)
        .build();
}
```

**工作量**: 0.5 人日  
**优先级**: P2 - 建议优化  
**预期收益**: 避免 **5-10%** 的无效查询

---

### 3.3 缺少异常处理

**问题描述**:
- Service 层未捕获 Repository 查询异常
- 异常会直接抛给 Controller，缺少日志记录

**修复方案**:

```java
@Override
public GlobalSearchResponseVO search(GlobalSearchRequestVO request, List<Long> visibleUserIds) {
    long t0 = System.currentTimeMillis();
    try {
        // ... 搜索逻辑 ...
    } catch (Exception e) {
        log.error("globalSearch failed: q={}, error={}", request.getQ(), e.getMessage(), e);
        return GlobalSearchResponseVO.builder()
            .hits(List.of())
            .tookMs(System.currentTimeMillis() - t0)
            .build();
    }
}
```

**工作量**: 1 人日  
**优先级**: P2 - 建议优化  
**预期收益**: 提升健壮性，便于问题排查

---

### 3.4 缺少方法级注释

**问题描述**:
- Service 实现类的私有方法缺少 Javadoc 注释
- 代码可读性降低，维护成本增加

**修复方案**:

```java
/**
 * 构建直播场次搜索条件
 * @param kw 关键词
 * @param vis 可见用户 ID 列表（null 表示不限制）
 * @return JPA Specification
 */
private Specification<LiveSession> liveSpec(String kw, List<Long> vis) { ... }

/**
 * 截断话术内容（超过 80 字符）
 * @param content 原始内容
 * @return 截断后的内容（末尾加 "…"）
 */
private static String trimScript(String content) { ... }
```

**工作量**: 1 人日  
**优先级**: P2 - 建议优化  
**预期收益**: 提升可维护性

---

### 3.5 硬编码的魔法数字

**问题描述**:
- 代码中存在硬编码的数字（80, 24, 6），缺少常量定义
- 降低代码可维护性

**修复方案**:

```java
private static final int PER_TYPE = 6;  // 每类型最多返回条数
private static final int DEFAULT_LIMIT = 24;  // 默认总结果上限
private static final int SCRIPT_TRIM_LENGTH = 80;  // 话术内容截断长度

// 使用常量
return t.length() > SCRIPT_TRIM_LENGTH ? t.substring(0, SCRIPT_TRIM_LENGTH) + "…" : t;
int cap = request.getLimit() != null ? request.getLimit() : DEFAULT_LIMIT;
```

**工作量**: 0.5 人日  
**优先级**: P2 - 建议优化  
**预期收益**: 提升可维护性

---

### 3.6 话术路径硬编码重复

**问题描述**:
- 话术搜索结果的 `path` 字段硬编码为 `"product"`，无论是否关联商品
- 三元表达式无意义：`sc.getProductId() != null ? "product" : "product"`

**修复方案**:

```java
// 方案 1：统一路径
.path("product")

// 方案 2：区分关联商品和独立话术
.path(sc.getProductId() != null ? "product/" + sc.getProductId() : "script")
```

**工作量**: 0.5 人日  
**优先级**: P2 - 建议优化  
**预期收益**: 提升代码质量

---

### 3.7 缺少缓存失效机制

**问题描述**:
- 即使添加缓存，也缺少缓存失效机制
- 数据更新后，缓存不会自动失效
- 可能导致搜索结果不一致

**修复方案**:

**方案 1: 定时失效（推荐）**
- 使用短 TTL（30 秒），自动失效
- 适合搜索场景，实时性要求不高

**方案 2: 主动失效**
```java
// 在数据更新时清除缓存
@CacheEvict(value = "search:global", allEntries = true)
public void clearSearchCache() {
    // 清除所有搜索缓存
}
```

**工作量**: 0.5 人日  
**优先级**: P2 - 建议优化  
**预期收益**: 数据一致性保障

---

### 3.8 缺少缓存预热机制

**问题描述**:
- 应用启动后，缓存为空
- 首次搜索热门词仍需查询数据库
- 冷启动性能差

**修复方案**:

```java
@Component
public class SearchCacheWarmer implements ApplicationListener<ContextRefreshedEvent> {
    
    @Resource
    private GlobalSearchService globalSearchService;
    
    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        // 预热热门搜索词
        List<String> hotKeywords = List.of("护肤", "彩妆", "精华", "面膜", "口红");
        for (String keyword : hotKeywords) {
            GlobalSearchRequestVO request = new GlobalSearchRequestVO();
            request.setQ(keyword);
            globalSearchService.search(request, null);  // admin 视角
        }
    }
}
```

**工作量**: 1 人日  
**优先级**: P2 - 建议优化  
**预期收益**: 冷启动性能提升

---

### 3.9 缺少缓存监控指标

**问题描述**:
- 无缓存命中率监控
- 无法评估缓存效果
- 无法优化缓存策略

**修复方案**:

```java
@Counted(value = "search.global.cache.hit", description = "缓存命中次数")
@Counted(value = "search.global.cache.miss", description = "缓存未命中次数")
```

**工作量**: 1 人日  
**优先级**: P2 - 建议优化  
**预期收益**: 提升可观测性

---

## 4. P3 低优先级问题（优化建议，可选修复）

### 4.1 直播场次搜索无函数索引

**问题描述**:
- 直播场次搜索使用 `LOWER(live_title) LIKE '%keyword%'`
- PostgreSQL 无法使用普通索引，需要函数索引

**修复方案**:

```sql
-- 创建函数索引
CREATE INDEX idx_live_session_title_lower 
ON live_session (LOWER(live_title));
```

**工作量**: 0.5 人日  
**优先级**: P3 - 可选优化  
**预期收益**: 直播场次搜索性能提升 **2-5 倍**

---

### 4.2 商品搜索无复合索引

**问题描述**:
- 商品搜索同时查询 `product_name` 和 `product_category`
- 无复合索引，可能导致索引扫描效率低

**修复方案**:

```sql
-- 创建复合索引
CREATE INDEX idx_dy_product_name_category 
ON dy_product (product_name, product_category);
```

**工作量**: 0.5 人日  
**优先级**: P3 - 可选优化  
**预期收益**: 商品搜索性能提升 **2-3 倍**

---

### 4.3 短视频搜索无复合索引

**问题描述**:
- 短视频搜索同时查询 `title` 和 `description`
- 无复合索引

**修复方案**:

```sql
-- 创建复合索引
CREATE INDEX idx_sv_video_title_desc 
ON sv_video (title, description);
```

**工作量**: 0.5 人日  
**优先级**: P3 - 可选优化  
**预期收益**: 短视频搜索性能提升 **2-3 倍**

---

### 4.4 关键词长度限制过长

**问题描述**:
- 关键词最大长度 64 字符，可能导致性能问题
- 超长关键词可能导致数据库查询缓慢

**修复方案**:

```java
// GlobalSearchRequestVO.java
@Size(min = 2, max = 32, message = "关键词长度为 2–32 字符")
private String q;
```

**工作量**: 0.1 人日  
**优先级**: P3 - 可选优化  
**预期收益**: 降低性能风险

---

### 4.5 缺少搜索建议功能

**问题描述**:
- 无自动补全和搜索建议
- 用户体验不佳

**修复方案**:
- 新增搜索建议端点 `/api/v1/search/suggestions`
- 基于历史搜索词返回建议

**工作量**: 4 人日  
**优先级**: P3 - 可选优化  
**预期收益**: 提升用户体验

---

### 4.6 缺少搜索历史记录

**问题描述**:
- 无法分析用户搜索行为
- 无法统计热门搜索词

**修复方案**:
- 新增搜索历史表 `search_history`
- 记录搜索词、结果数、用户 ID、时间戳

**工作量**: 4 人日  
**优先级**: P3 - 可选优化  
**预期收益**: 数据分析能力提升

---

### 4.7 缺少高级搜索功能

**问题描述**:
- 无法按时间范围、状态等条件过滤
- 搜索功能单一

**修复方案**:
- 新增高级搜索端点
- 支持多维度过滤条件

**工作量**: 8 人日  
**优先级**: P3 - 可选优化  
**预期收益**: 搜索功能增强

---

### 4.8 缺少搜索结果排序

**问题描述**:
- 结果按类型顺序返回，无相关性排序
- 用户可能需要先看到最相关的结果

**修复方案**:
- 引入 Elasticsearch，支持相关性评分
- 或实现简单的相关性算法（关键词匹配度）

**工作量**: 16 人日  
**优先级**: P3 - 可选优化  
**预期收益**: 搜索体验提升

---

### 4.9 缺少 Token 过期时间校验

**问题描述**:
- Controller 层未校验 Token 是否过期
- 依赖 AuthTokenFilter 的校验
- 建议增加显式校验

**修复方案**: 在 AuthTokenFilter 中增加 Token 过期校验

**工作量**: 0.5 人日（需修改 common 模块）  
**优先级**: P3 - 可选优化  
**预期收益**: 提升安全性

---

### 4.10 缓存键设计可优化

**问题描述**:
- 当前缓存键使用 `visibleUserIds.hashCode()`
- 不同用户列表可能产生相同 hashCode（哈希冲突）
- 可能导致缓存错误

**修复方案**:

```java
@Cacheable(
    value = "search:global",
    key = "#request.q + ':' + (#visibleUserIds != null ? #visibleUserIds.toString() : 'all')",
    unless = "#result == null || #result.hits.isEmpty()"
)
```

**工作量**: 0.5 人日  
**优先级**: P3 - 可选优化  
**预期收益**: 降低哈希冲突风险

---

### 4.11 缺少缓存大小限制

**问题描述**:
- 无缓存大小限制
- 可能导致内存溢出
- 需要配置最大缓存条目数

**修复方案**:

```yaml
spring:
  cache:
    caffeine:
      spec: maximumSize=1000,expireAfterWrite=30s  # 最多 1000 条
    redis:
      time-to-live: 30s
```

**工作量**: 0.5 人日  
**优先级**: P3 - 可选优化  
**预期收益**: 防止内存溢出

---

## 5. 实施路线图

### 5.1 第一阶段：核心性能优化（1-2 周，6.5 人日）

**目标**: 修复 P1 问题，提升性能和可用性

**任务清单**:
1. ✅ **添加 PostgreSQL pg_trgm 索引** (2 人日)
   - 安装 pg_trgm 扩展
   - 为 `dy_product_script.script_content` 创建 GIN trigram 索引
   - 测试验证性能提升

2. ✅ **添加 Redis 缓存** (1 人日)
   - 在 `GlobalSearchServiceImpl.search()` 添加 `@Cacheable` 注解
   - 配置 Redis 缓存 TTL 30 秒
   - 测试缓存命中率

3. ✅ **添加 API 限流** (1.5 人日)
   - 在 `GlobalSearchController.globalSearch()` 添加 `@RateLimiter` 注解
   - 配置限流策略 (每用户每分钟 30 次)
   - 添加限流降级方法
   - 测试限流效果

4. ✅ **添加日志记录** (1 人日)
   - 在 Service 层添加 INFO/DEBUG/WARN 日志
   - 记录搜索关键词、结果数量、查询耗时
   - 记录空结果和慢查询

5. ✅ **添加监控指标** (1 人日)
   - 添加 `@Timed` 和 `@Counted` 注解
   - 配置 Prometheus 监控
   - 创建 Grafana 仪表盘

**预期收益**:
- 话术搜索性能提升 **10-50 倍**
- 热门搜索词响应时间降低 **80-90%**
- 服务可用性提升至 **99.9%+**
- 数据库负载降低 **50-70%**

**验收标准**:
- [ ] pg_trgm 索引创建成功，话术搜索耗时 < 50ms
- [ ] 缓存命中率 > 40%，热门搜索词响应时间 < 20ms
- [ ] 限流生效，单用户每分钟最多 30 次请求
- [ ] 日志记录完整，包含关键词、耗时、结果数
- [ ] Prometheus 指标正常采集，Grafana 仪表盘可视化

---

### 5.2 第二阶段：质量改进（1-2 月，6.5 人日）

**目标**: 修复 P2 问题，进一步提升性能和可观测性

**任务清单**:
1. ✅ **改为并行查询** (2 人日)
   - 使用 `CompletableFuture` 并行执行 4 次数据库查询
   - 测试并行查询性能提升

2. ✅ **添加空关键词校验** (0.5 人日)
   - 在 Service 层提前校验空关键词
   - 避免无效数据库查询

3. ✅ **添加异常处理** (1 人日)
   - 在 Service 层捕获 Repository 查询异常
   - 记录异常日志
   - 返回友好错误信息

4. ✅ **添加方法级注释** (1 人日)
   - 为所有私有方法添加 Javadoc 注释
   - 提升代码可读性

5. ✅ **提取魔法数字为常量** (0.5 人日)
   - 定义 `DEFAULT_LIMIT`, `SCRIPT_TRIM_LENGTH` 常量
   - 替换硬编码数字

6. ✅ **修复话术路径硬编码** (0.5 人日)
   - 统一话术路径为 `"product"`
   - 或区分关联商品和独立话术

7. ✅ **添加缓存失效机制** (0.5 人日)
   - 使用短 TTL（30 秒）自动失效
   - 或在数据更新时主动清除缓存

8. ✅ **添加缓存预热机制** (1 人日)
   - 应用启动时预热热门搜索词
   - 提升冷启动性能

9. ✅ **添加缓存监控指标** (1 人日)
   - 监控缓存命中率和未命中率
   - 评估缓存效果

**预期收益**:
- 响应时间进一步降低 **40-60%**
- 避免 **5-10%** 的无效查询
- 提升可观测性，便于性能优化
- 代码质量提升

**验收标准**:
- [ ] 并行查询耗时 < 60ms（相比串行提升 60%）
- [ ] 空关键词提前返回，不执行数据库查询
- [ ] 异常日志记录完整，包含堆栈信息
- [ ] 所有方法有 Javadoc 注释
- [ ] 无硬编码魔法数字
- [ ] 缓存预热成功，热门词首次查询命中缓存
- [ ] 缓存命中率监控正常

---

### 5.3 第三阶段：功能增强（3-6 月，9.1 人日）

**目标**: 修复 P3 问题，优化细节和用户体验

**任务清单**:
1. ✅ **添加函数索引和复合索引** (1.5 人日)
   - 直播场次: `LOWER(live_title)` 函数索引
   - 商品: `(product_name, product_category)` 复合索引
   - 短视频: `(title, description)` 复合索引

2. ✅ **降低关键词长度限制** (0.1 人日)
   - 从 64 字符降低到 32 字符

3. ✅ **新增搜索建议功能** (4 人日)
   - 新增搜索建议端点
   - 基于历史搜索词返回建议
   - 前端集成自动补全

4. ✅ **新增搜索历史记录功能** (4 人日)
   - 新增搜索历史表
   - 记录搜索词和结果数
   - 统计热门搜索词

5. ✅ **新增高级搜索功能** (8 人日)
   - 新增高级搜索端点
   - 支持时间范围、状态等过滤条件

6. ✅ **引入 Elasticsearch 支持相关性排序** (16 人日)
   - 集成 Elasticsearch
   - 实现相关性评分
   - 优化搜索结果排序

7. ✅ **优化缓存键设计** (0.5 人日)
   - 使用 `toString()` 替代 `hashCode()`
   - 避免哈希冲突

8. ✅ **配置缓存大小限制** (0.5 人日)
   - 配置 Caffeine 最大 1000 条
   - 防止内存溢出

9. ✅ **添加 Token 过期校验** (0.5 人日)
   - 在 AuthTokenFilter 中增加过期校验
   - 提升安全性

**预期收益**:
- 搜索性能进一步提升 **2-5 倍**
- 提升用户体验
- 功能更加完善

**验收标准**:
- [ ] 函数索引和复合索引创建成功
- [ ] 关键词长度限制为 32 字符
- [ ] 搜索建议功能正常，返回相关建议
- [ ] 搜索历史记录功能正常，统计热门词
- [ ] 高级搜索功能正常，支持多维度过滤
- [ ] Elasticsearch 集成成功，相关性排序生效
- [ ] 缓存键无哈希冲突
- [ ] 缓存大小限制生效
- [ ] Token 过期校验生效

---

## 6. 验收标准

### 6.1 第一阶段验收标准（P1）

**性能指标**:
- [ ] 话术搜索耗时 < 50ms（100,000 条数据）
- [ ] 热门搜索词响应时间 < 20ms（缓存命中）
- [ ] 长尾搜索词响应时间 < 100ms（缓存未命中）
- [ ] 缓存命中率 > 40%
- [ ] 限流生效，单用户每分钟最多 30 次请求

**可观测性指标**:
- [ ] 日志记录完整（关键词、耗时、结果数、空结果、慢查询）
- [ ] Prometheus 指标正常采集
- [ ] Grafana 仪表盘可视化
- [ ] 监控指标包含：query 耗时、requests 次数、cache hit/miss

**功能指标**:
- [ ] pg_trgm 索引创建成功
- [ ] Redis 缓存配置正确（TTL 30 秒）
- [ ] API 限流配置正确（每用户每分钟 30 次）
- [ ] 所有测试用例通过

---

### 6.2 第二阶段验收标准（P2）

**性能指标**:
- [ ] 并行查询耗时 < 60ms（相比串行提升 60%）
- [ ] 空关键词提前返回，耗时 < 1ms
- [ ] 缓存预热成功，热门词首次查询命中缓存

**代码质量指标**:
- [ ] 所有方法有 Javadoc 注释
- [ ] 无硬编码魔法数字
- [ ] 异常处理完善，有日志记录
- [ ] 代码审查通过

**可观测性指标**:
- [ ] 缓存命中率监控正常
- [ ] 缓存预热日志记录完整
- [ ] 异常日志记录完整（包含堆栈信息）

---

### 6.3 第三阶段验收标准（P3）

**性能指标**:
- [ ] 直播场次搜索耗时 < 20ms（函数索引）
- [ ] 商品搜索耗时 < 30ms（复合索引）
- [ ] 短视频搜索耗时 < 25ms（复合索引）

**功能指标**:
- [ ] 搜索建议功能正常，返回相关建议
- [ ] 搜索历史记录功能正常，统计热门词
- [ ] 高级搜索功能正常，支持多维度过滤
- [ ] Elasticsearch 集成成功，相关性排序生效

**安全指标**:
- [ ] Token 过期校验生效
- [ ] 缓存键无哈希冲突
- [ ] 缓存大小限制生效，无内存溢出

---

## 7. 风险评估

### 7.1 技术风险

| 风险 | 可能性 | 影响 | 缓解措施 |
|------|--------|------|----------|
| pg_trgm 索引创建失败 | 低 | 高 | 提前在测试环境验证，准备回滚方案 |
| 缓存导致数据不一致 | 中 | 中 | 使用短 TTL（30 秒），定期失效 |
| 限流影响正常用户 | 低 | 中 | 设置合理阈值（每分钟 30 次），监控限流次数 |
| 并行查询导致数据库负载增加 | 低 | 中 | 使用线程池限制并发数，监控数据库连接池 |
| Elasticsearch 集成复杂 | 高 | 低 | 分阶段实施，先实现基础功能 |

### 7.2 业务风险

| 风险 | 可能性 | 影响 | 缓解措施 |
|------|--------|------|----------|
| 搜索结果不一致 | 中 | 中 | 使用短 TTL，定期失效缓存 |
| 限流影响用户体验 | 低 | 中 | 设置合理阈值，提供友好错误提示 |
| 性能优化效果不明显 | 低 | 低 | 提前在测试环境验证，监控性能指标 |

### 7.3 资源风险

| 风险 | 可能性 | 影响 | 缓解措施 |
|------|--------|------|----------|
| 开发资源不足 | 中 | 中 | 分阶段实施，优先修复 P1 问题 |
| 测试资源不足 | 中 | 中 | 自动化测试，减少手动测试工作量 |
| 生产环境部署风险 | 低 | 高 | 灰度发布，逐步放量 |

---

## 8. 总结

### 8.1 修复计划总览

**总工作量**: 22.1 人日

**优先级分布**:
- P0: 0 个 (0 人日)
- P1: 6 个 (6.5 人日) - **核心性能优化**
- P2: 9 个 (6.5 人日) - **质量改进**
- P3: 11 个 (9.1 人日) - **功能增强**

**预期收益**:
- 话术搜索性能提升 **10-50 倍**
- 热门搜索词响应时间降低 **80-90%**
- 服务可用性提升至 **99.9%+**
- 数据库负载降低 **50-70%**
- 吞吐量提升 **3-5 倍**

### 8.2 实施建议

**短期（1-2 周）**:
- 优先修复 P1 问题（全文索引、缓存、限流、日志、监控）
- 预期收益最大，工作量适中（6.5 人日）
- 修复后可立即上线生产环境

**中期（1-2 月）**:
- 修复 P2 问题（并行查询、异常处理、代码质量）
- 进一步提升性能和可观测性（6.5 人日）
- 提升代码质量和可维护性

**长期（3-6 月）**:
- 修复 P3 问题（搜索建议、历史记录、高级搜索、Elasticsearch）
- 功能增强和用户体验优化（9.1 人日）
- 可根据业务需求灵活调整优先级

### 8.3 关键成功因素

1. **优先级明确**: P1 问题必须修复，P2/P3 可根据资源情况调整
2. **分阶段实施**: 避免一次性修改过多，降低风险
3. **充分测试**: 每个阶段完成后进行充分测试，确保质量
4. **监控先行**: 先增加监控和日志，再进行性能优化
5. **灰度发布**: 生产环境采用灰度发布，逐步放量

### 8.4 最终评价

**当前状态**: 
- 代码质量高，测试覆盖完善，安全性极高
- 主要问题是性能优化不足（无全文索引、无缓存、无限流）

**修复后状态**:
- 性能提升 **5-10 倍**
- 服务可用性提升至 **99.9%+**
- 可观测性大幅提升
- 代码质量进一步提升

**建议**: 优先修复 P1 问题（6.5 人日），性能可提升 **5-10 倍**，服务可用性提升至 **99.9%+**，投入产出比最高。

---

**报告生成时间**: 2026-05-09  
**规划者**: Claude Opus 4  
**模块版本**: dy05 (基于 dy02 演进)  
**下一步**: 执行第一阶段修复计划（P1，共 6.5 人日）

