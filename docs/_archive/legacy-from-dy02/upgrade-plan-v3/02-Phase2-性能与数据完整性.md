# Phase 2 — 性能与数据完整性（HIGH）

## 目标

修复 3 个后端性能问题 + 6 个数据库完整性问题 + 2 个基础设施配置问题。完成后系统在高并发下稳定运行。

---

## 任务 2.1：修复 N+1 查询

**问题**：`AbTestServiceImpl.java:63-64` 分页查询后在 stream 循环中逐条查询 variant，30 条数据产生 31 次 SQL。

**涉及文件**：
- `src/main/java/cn/gaifan/douyinOperations/module/abtest/service/impl/AbTestServiceImpl.java:60-66`
- `src/main/java/cn/gaifan/douyinOperations/module/abtest/repository/AbExperimentRepository.java`

**修复方案**：

方案 A（推荐）— 批量查询替代循环查询：

```java
// 修改前
Page<AbExperiment> page = experimentRepository.findAll(spec, pageable);
List<AbExperimentVO> list = page.getContent().stream().map(e -> {
    AbExperimentVO vo2 = toExperimentVO(e);
    vo2.setVariants(variantRepository.findByExperimentIdAndDeleted(e.getId(), 0)  // N+1!
            .stream().map(this::toVariantVO).collect(Collectors.toList()));
    return vo2;
}).collect(Collectors.toList());

// 修改后 — 一次性批量查询所有 variant
Page<AbExperiment> page = experimentRepository.findAll(spec, pageable);
List<Long> experimentIds = page.getContent().stream()
    .map(AbExperiment::getId).collect(Collectors.toList());

// 单次查询所有关联 variant，按 experimentId 分组
Map<Long, List<AbVariantVO>> variantMap = experimentIds.isEmpty()
    ? Collections.emptyMap()
    : variantRepository.findByExperimentIdInAndDeleted(experimentIds, 0)
        .stream()
        .collect(Collectors.groupingBy(
            AbVariant::getExperimentId,
            Collectors.mapping(this::toVariantVO, Collectors.toList())
        ));

List<AbExperimentVO> list = page.getContent().stream().map(e -> {
    AbExperimentVO vo2 = toExperimentVO(e);
    vo2.setVariants(variantMap.getOrDefault(e.getId(), Collections.emptyList()));
    return vo2;
}).collect(Collectors.toList());
```

Repository 新增方法：

```java
// AbVariantRepository.java
List<AbVariant> findByExperimentIdInAndDeleted(List<Long> experimentIds, Integer deleted);
```

**全局排查**：用以下模式搜索其他模块是否存在类似 N+1：

```bash
# 搜索 stream 内调用 repository 的模式
grep -rn "\.stream()\.map" src/main/java/**/service/impl/*.java | grep -i "repository\."
```

**验收**：
- [ ] 查询 30 条实验数据只产生 2 次 SQL（1 次分页 + 1 次批量 variant）
- [ ] 功能不变，返回数据结构一致
- [ ] 其他模块无类似 N+1 模式

---

## 任务 2.2：修复缓存 Key 不匹配

**问题**：`AbTestServiceImpl.java:81` 的 `@CacheEvict` 使用 `key = "#result"` 指向方法返回值（Long ID），但缓存写入时 key 可能是其他值，导致更新后缓存永远不被清除。

**涉及文件**：
- `src/main/java/cn/gaifan/douyinOperations/module/abtest/service/impl/AbTestServiceImpl.java:81`

**修复方案**：

```java
// 修改前
@CacheEvict(value = "abtest:experiment", key = "#result")
public Long save(AbExperimentSaveVO vo) { ... }

// 修改后 — 清除该缓存空间所有条目（save 操作影响列表缓存）
@CacheEvict(value = "abtest:experiment", allEntries = true)
public Long save(AbExperimentSaveVO vo) { ... }
```

**全局排查**：

```bash
# 搜索所有 @CacheEvict 确认 key 表达式正确
grep -rn "@CacheEvict" src/main/java/ --include="*.java"
```

**验收**：
- [ ] 保存实验后，再次查询返回最新数据（非缓存旧数据）
- [ ] 所有 `@CacheEvict` 的 key 表达式与对应 `@Cacheable` 一致

---

## 任务 2.3：替换 Thread.sleep() 为异步模式

**问题**：Controller/Service 中存在 `Thread.sleep()` 阻塞请求线程，200 线程池下高并发时严重浪费资源。

**涉及文件**：

```bash
# 搜索所有 Thread.sleep 调用
grep -rn "Thread.sleep" src/main/java/ --include="*.java"
```

**修复方案**：

场景 1 — SSE 推送间隔（如 Agent 聊天流式输出）：

```java
// 修改前
Thread.sleep(80);  // 控制推送速率

// 修改后 — 使用 ScheduledExecutorService 或直接移除
// SSE 本身有背压机制，无需手动 sleep
// 如确需限速，使用非阻塞方式：
CompletableFuture.delayedExecutor(80, TimeUnit.MILLISECONDS)
    .execute(() -> emitter.send(...));
```

场景 2 — 等待外部服务就绪：

```java
// 修改前
Thread.sleep(1000);  // 等待服务就绪

// 修改后 — 使用 Resilience4j Retry（项目已引入）
Retry retry = Retry.of("service-ready", RetryConfig.custom()
    .maxAttempts(3)
    .waitDuration(Duration.ofMillis(500))
    .build());
Retry.decorateRunnable(retry, () -> checkServiceReady()).run();
```

**验收**：
- [ ] `grep -rn "Thread.sleep" src/main/java/` 返回 0 结果（或仅在测试代码中）
- [ ] SSE 流式推送正常
- [ ] 高并发下线程池无阻塞

---

## 任务 2.4：volatile 字段改为 AtomicLong

**问题**：`EvolveScheduler` 中 `volatile long lastRunTime` 的读-检查-写操作非原子，两个线程可能同时通过检查并执行。

**涉及文件**：

```bash
grep -rn "volatile" src/main/java/ --include="*.java"
```

**修复方案**：

```java
// 修改前
private volatile long lastRunTime = 0;

public void maybeRun() {
    if (System.currentTimeMillis() - lastRunTime > interval) {
        lastRunTime = System.currentTimeMillis();  // 非原子操作
        doRun();
    }
}

// 修改后
private final AtomicLong lastRunTime = new AtomicLong(0);

public void maybeRun() {
    long now = System.currentTimeMillis();
    long last = lastRunTime.get();
    if (now - last > interval && lastRunTime.compareAndSet(last, now)) {
        doRun();  // 只有一个线程能进入
    }
}
```

**验收**：
- [ ] 并发调用 `maybeRun()` 不会重复执行
- [ ] `grep -rn "volatile" src/main/java/` 中无 compound read-write 模式

---

## 任务 2.5：数据隔离字段统一

**问题**：不同模块使用不同字段名表示数据归属——Live 用 `user_id`，Shortvideo 用 `owner_id`，Product 用 `user_id`。增加 Service 层遗漏过滤的风险。

**涉及文件**：
- `sql/live/schema.sql` — `live_session.user_id`
- `sql/product/schema.sql` — 相关表的 `user_id`
- 对应 Entity 和 ServiceImpl 文件

**修复方案**：

1. 新增 Flyway 迁移，为使用 `user_id` 的表添加 `owner_id` 别名列（渐进式迁移）：

```sql
-- V068__standardize_owner_id.sql

-- live_session: 添加 owner_id 列，数据从 user_id 复制
ALTER TABLE live_session ADD COLUMN IF NOT EXISTS owner_id BIGINT;
UPDATE live_session SET owner_id = user_id WHERE owner_id IS NULL;

-- 后续版本中废弃 user_id，统一使用 owner_id
-- 注意：不要在本次迁移中删除 user_id，保持向后兼容
```

2. Entity 中添加 `ownerId` 字段，Service 层统一使用 `ownerId` 过滤。

3. 旧的 `userId` 字段标记 `@Deprecated`，下个版本移除。

**验收**：
- [ ] 所有用户私有表都有 `owner_id` 列
- [ ] Service 层查询统一使用 `owner_id` 过滤
- [ ] Flyway 迁移执行成功，数据无丢失

---

## 任务 2.6：补充数据库复合索引

**问题**：高频查询模式缺少复合索引，大表全表扫描。

**涉及文件**：
- `sql/performance/indexes-optimization.sql`
- 新增 Flyway 迁移

**修复方案**：

```sql
-- V069__add_composite_indexes.sql

-- 用户数据 + 时间范围查询（所有含 owner_id 的表通用模式）
CREATE INDEX IF NOT EXISTS idx_live_session_owner_deleted_time
    ON live_session(owner_id, deleted, create_time DESC);

CREATE INDEX IF NOT EXISTS idx_sv_project_owner_deleted_time
    ON sv_project(owner_id, deleted, create_time DESC);

CREATE INDEX IF NOT EXISTS idx_dy_product_owner_deleted_time
    ON dy_product(owner_id, deleted, create_time DESC);

-- 状态 + 时间查询
CREATE INDEX IF NOT EXISTS idx_live_session_status_deleted_time
    ON live_session(status, deleted, create_time DESC);

CREATE INDEX IF NOT EXISTS idx_sv_project_status_deleted_time
    ON sv_project(status, deleted, create_time DESC);

-- AI 知识库高频查询
CREATE INDEX IF NOT EXISTS idx_ai_kb_doc_kb_deleted_time
    ON ai_kb_document(knowledge_base_id, deleted, create_time DESC);

CREATE INDEX IF NOT EXISTS idx_ai_kb_chunk_doc_deleted
    ON ai_kb_chunk(document_id, deleted);

-- 商品话术版本查询
CREATE INDEX IF NOT EXISTS idx_product_script_product_deleted_time
    ON dy_product_script_version(product_id, deleted, create_time DESC);
```

**验收**：
- [ ] Flyway 迁移执行成功
- [ ] `EXPLAIN ANALYZE` 确认高频查询使用索引扫描而非全表扫描
- [ ] 查询响应时间无退化

---

## 任务 2.7：TEXT 列添加长度约束

**问题**：多个 TEXT 列无长度限制，可被大 payload 攻击导致存储耗尽。

**修复方案**：

在 Service 层 save 方法中添加校验（不改数据库，避免迁移风险）：

```java
// 在 SaveVO 中添加 @Size 注解
public class AiKbDocumentSaveVO {
    @Size(max = 500_000, message = "文档内容不能超过 500KB")
    private String content;

    @Size(max = 100_000, message = "模板内容不能超过 100KB")
    private String templateContent;
}

public class LiveSessionSaveVO {
    @Size(max = 200_000, message = "话术内容不能超过 200KB")
    private String scriptContent;
}
```

**全局排查**：

```bash
# 找出所有 TEXT 类型且无 @Size 的 SaveVO 字段
grep -rn "private String" src/main/java/**/vo/*SaveVO.java | grep -v "@Size"
```

**验收**：
- [ ] 超长内容提交返回 400 + 明确错误信息
- [ ] 正常长度内容不受影响

---

## 任务 2.8：Redis 连接池扩容

**问题**：`application.yml:56-58` Redis 连接池 `max-active: 20` 对 200 Tomcat 线程严重不足。

**涉及文件**：
- `src/main/resources/application.yml:56-58`

**修复方案**：

```yaml
# 修改前
spring:
  data:
    redis:
      lettuce:
        pool:
          max-active: 20
          max-idle: 10
          min-idle: 5

# 修改后
spring:
  data:
    redis:
      lettuce:
        pool:
          max-active: ${REDIS_POOL_MAX_ACTIVE:100}
          max-idle: ${REDIS_POOL_MAX_IDLE:50}
          min-idle: ${REDIS_POOL_MIN_IDLE:10}
          max-wait: 3s  # 等待连接超时，避免无限阻塞
```

**验收**：
- [ ] 高并发下 Redis 操作无 `PoolExhaustedException`
- [ ] `max-wait` 超时后返回明确错误而非无限等待

---

## 任务 2.9：LIKE 查询通配符转义

**问题**：`AbTestServiceImpl.java:55` 等处 LIKE 查询未转义用户输入中的 `%` 和 `_`，用户输入 `%` 可匹配全表。

**涉及文件**：

```bash
grep -rn "cb.like" src/main/java/ --include="*.java"
```

**修复方案**：

在 `common/util/` 中添加工具方法：

```java
public final class QueryUtils {
    private QueryUtils() {}

    /**
     * 转义 LIKE 查询中的通配符，防止用户输入 % 或 _ 导致全表匹配
     */
    public static String escapeLike(String keyword) {
        if (keyword == null) return null;
        return keyword.trim()
            .replace("\\", "\\\\")
            .replace("%", "\\%")
            .replace("_", "\\_");
    }

    public static String wrapLike(String keyword) {
        return "%" + escapeLike(keyword) + "%";
    }
}
```

所有 LIKE 查询统一使用：

```java
// 修改前
cb.like(root.get("name"), "%" + vo.getKeyword().trim() + "%")

// 修改后
cb.like(root.get("name"), QueryUtils.wrapLike(vo.getKeyword()))
```

**验收**：
- [ ] 搜索 `%` 不再返回全部数据
- [ ] 搜索 `_` 不再匹配任意单字符
- [ ] 正常关键词搜索不受影响

---

## Phase 2 完成标准

```bash
mvn compile && mvn test
cd frontend-react && npm run type-check && npm run test

# 性能验证
# 1. AbTest 列表查询 SQL 日志只有 2 条（非 31 条）
# 2. Redis 连接池监控无 exhausted 告警
# 3. EXPLAIN ANALYZE 确认新索引生效
```
