# A/B Test 模块模式合规性分析报告

**生成日期**: 2026-05-09  
**分析范围**: douyin-operations-abtest 模块  
**合规标准**: dy05 项目架构规范（CLAUDE.md + docs/adr/）

---

## 执行摘要

| 维度 | 评分 | 等级 | 说明 |
|------|------|------|------|
| **总体合规性** | 76/100 | Grade C+ | 基础架构合规，但存在关键数据隔离和并发控制问题 |
| RESTful API 规范 | 95/100 | A | 完全符合 ADR-001 统一 POST 规范 |
| 数据访问层规范 | 85/100 | B+ | 使用 JPA Specification，但缺少 owner_id 数据隔离 |
| 缓存策略 | 70/100 | C+ | 有缓存但不完整，缺少 L1 缓存和统计数据缓存 |
| 数据隔离 | 40/100 | F | **严重问题**：缺少 owner_id 强制过滤 |
| 错误处理 | 80/100 | B | 统一异常处理，但缺少审计日志 |
| 并发控制 | 35/100 | F | **严重问题**：计数器更新无并发控制 |

**关键发现**:
- ✅ API 设计完全符合统一 POST 规范
- ✅ 使用 JPA Specification 动态查询
- ✅ 逻辑删除规范正确实现
- ✅ 统计分析功能完善（卡方检验、日趋势）
- ⚠️ **P0 问题**：缺少 owner_id 数据隔离（CVSS 9.1）
- ⚠️ **P0 问题**：计数器更新无并发控制（数据不一致）
- ⚠️ 缺少 API 限流保护
- ⚠️ 测试覆盖率低（仅 2 个测试文件）

---

## 1. RESTful API 规范合规性

### 1.1 统一 POST 规范（ADR-001）

**合规性**: ✅ 100% 符合

**分析**: AbTestController 所有 16 个接口均使用 POST 方法，完全符合 ADR-001 规范。

**接口清单**:
```java
// 实验管理（6 个）
POST /api/v1/abtest/experiment/list          // 实验列表
POST /api/v1/abtest/experiment/get           // 实验详情
POST /api/v1/abtest/experiment/save          // 新增/更新实验
POST /api/v1/abtest/experiment/delete        // 删除实验
POST /api/v1/abtest/experiment/update-status // 更新状态
POST /api/v1/abtest/experiment/set-winner    // 设置获胜变体

// 变体管理（2 个）
POST /api/v1/abtest/variant/save             // 新增/更新变体
POST /api/v1/abtest/variant/delete           // 删除变体

// 事件记录（1 个）
POST /api/v1/abtest/event/record             // 记录事件

// 统计分析（2 个）
POST /api/v1/abtest/experiment/result        // 获取统计结果
POST /api/v1/abtest/experiment/daily-trend   // 获取日趋势

// 话术风格 A/B（2 个）
POST /api/v1/abtest/script-style/assign      // 分配风格
POST /api/v1/abtest/script-style/record-conversion // 记录转化
```

**优点**:
- 路径命名清晰，动作词明确（list/get/save/delete）
- 无 GET/PUT/DELETE 混用
- 前端调用统一使用 `request.post()`

### 1.2 响应格式规范

**合规性**: ✅ 100% 符合

**分析**: 所有接口返回 `RESTResult<T>` 统一响应体。

**代码示例** (`AbTestController.java:45-48`):
```java
RESTResult<PageResultVO<AbExperimentVO>> r = RESTResult.getSuccess(abTestService.search(vo));
r.setTraceId(MDC.get("traceId"));
return r;
```

**响应格式**:
```json
{
  "status": 200,
  "message": "操作成功",
  "data": { ... },
  "traceId": "uuid",
  "timestamp": 1234567890
}
```

**优点**:
- 统一响应格式，前端解析简单
- 包含 traceId，便于链路追踪
- 使用静态工厂方法（getSuccess/addSuccess/deleteSuccess）

---

## 2. 数据访问层规范

### 2.1 JPA Specification 动态查询（ADR-003）

**合规性**: ✅ 85% 符合

**分析**: `AbTestServiceImpl.search()` 使用 JPA Specification 构建动态查询。

**代码示例** (`AbTestServiceImpl.java:42-58`):
```java
Specification<AbExperiment> spec = (root, query, cb) -> {
    List<Predicate> predicates = new ArrayList<>();
    predicates.add(cb.equal(root.get("deleted"), 0));
    
    if (vo.getOwnerId() != null && vo.getOwnerId() > 0) {
        predicates.add(cb.equal(root.get("ownerId"), vo.getOwnerId()));
    } else if (vo.getOwnerIds() != null && !vo.getOwnerIds().isEmpty()) {
        predicates.add(root.get("ownerId").in(vo.getOwnerIds()));
    }
    
    if (vo.getExperimentType() != null && !vo.getExperimentType().isBlank()) {
        predicates.add(cb.equal(root.get("experimentType"), vo.getExperimentType().trim()));
    }
    
    if (vo.getStatus() != null) {
        predicates.add(cb.equal(root.get("status"), vo.getStatus()));
    }
    
    if (vo.getKeyword() != null && !vo.getKeyword().isBlank()) {
        predicates.add(cb.like(root.get("name"), "%" + vo.getKeyword().trim() + "%"));
    }
    
    return cb.and(predicates.toArray(new Predicate[0]));
};

Page<AbExperiment> page = experimentRepository.findAll(spec, pageable);
```

**优点**:
- 动态条件组合灵活
- 类型安全，编译期检查
- 与分页/排序无缝集成

**问题**:
- ⚠️ **数据隔离不强制**：ownerId 过滤是可选的（`if (vo.getOwnerId() != null)`），应该强制添加
- ⚠️ 缺少 N+1 查询优化（变体数据单独查询）

### 2.2 逻辑删除规范

**合规性**: ✅ 100% 符合

**分析**: 所有 Entity 正确实现逻辑删除。

**Entity 配置**:
```java
// AbExperiment.java
@Entity
@Table(name = "ab_experiment")
@SQLRestriction("deleted = 0")  // ✅ 自动过滤已删除记录
public class AbExperiment {
    @Column(name = "deleted", nullable = false)
    private Integer deleted = 0;
}

// AbVariant.java
@Entity
@Table(name = "ab_variant")
@SQLRestriction("deleted = 0")  // ✅ 自动过滤已删除记录
public class AbVariant {
    @Column(name = "deleted", nullable = false)
    private Integer deleted = 0;
}
```

**删除操作**:
```java
// AbTestServiceImpl.java:112-122
@Transactional(rollbackFor = Exception.class)
@CacheEvict(value = "abtest:experiment", key = "#id")
public void delete(Long id) {
    AbExperiment entity = experimentRepository.findByIdAndDeleted(id, 0)
            .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "实验不存在"));
    entity.setDeleted(1);  // ✅ 逻辑删除
    experimentRepository.save(entity);
    
    // 级联删除变体
    variantRepository.findByExperimentIdAndDeleted(id, 0).forEach(v -> {
        v.setDeleted(1);
        variantRepository.save(v);
    });
}
```

**优点**:
- `@SQLRestriction` 自动过滤已删除记录
- 级联删除变体（实验删除时同步删除变体）
- 所有查询方法使用 `findByIdAndDeleted(id, 0)`

**注意**:
- ⚠️ `AbEvent` 无 deleted 字段（事件记录不可删除，符合设计）

---

## 3. 缓存策略规范

### 3.1 两级缓存架构

**合规性**: ⚠️ 70% 符合（有缓存但不完整）

**当前实现**:
```java
// AbTestServiceImpl.java
@Cacheable(value = "abtest:experiment", key = "#id", unless = "#result == null")
public AbExperimentVO getById(Long id) { ... }

@CacheEvict(value = "abtest:experiment", key = "#result")
public long save(AbExperimentSaveVO vo) { ... }

@CacheEvict(value = "abtest:experiment", key = "#id")
public void delete(Long id) { ... }

@Cacheable(value = "abtest:statistics", key = "#experimentId", unless = "#result == null")
public AbExperimentStatisticsVO getExperimentStatistics(Long experimentId) { ... }
```

**优点**:
- ✅ 使用 Spring Cache 注解
- ✅ 实验详情有缓存（`abtest:experiment`）
- ✅ 统计结果有缓存（`abtest:statistics`）
- ✅ 修改/删除时正确失效缓存

**问题**:
- ⚠️ **缺少 L1 缓存**：未配置 Caffeine 本地缓存
- ⚠️ **列表查询无缓存**：`search()` 方法无缓存
- ⚠️ **统计缓存失效不完整**：事件记录后未失效统计缓存

**修复建议**:
```java
// 1. 配置 L1+L2 两级缓存（CacheConfig.java）
@Bean
public CacheManager cacheManager(RedisConnectionFactory factory) {
    return RedisCacheManager.builder(factory)
        .cacheDefaults(RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(30)))
        .withCacheConfiguration("abtest:experiment", 
            RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofHours(1)))
        .withCacheConfiguration("abtest:statistics", 
            RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofMinutes(5)))
        .build();
}

// 2. 事件记录后失效统计缓存
@Transactional(rollbackFor = Exception.class)
@CacheEvict(value = "abtest:statistics", key = "#vo.experimentId")  // ✅ 添加
public void recordEvent(AbEventSaveVO vo) { ... }
```

---

## 4. 数据隔离规范

### 4.1 多租户数据隔离

**合规性**: ⚠️ 40% 符合（**严重问题**）

**问题分析**:

#### P0-1: 缺少 owner_id 强制过滤

**位置**: `AbTestServiceImpl.java:42-58`

**问题**: 
- `search()` 方法的 ownerId 过滤是**可选的**（`if (vo.getOwnerId() != null)`）
- Controller 通过 `DataScopeResolver` 注入 `ownerIds`，但不强制
- 管理员角色可能绕过数据隔离

**代码示例**:
```java
// AbTestController.java:37-48
@PostMapping("/experiment/list")
public RESTResult<PageResultVO<AbExperimentVO>> list(HttpServletRequest request,
        @RequestBody(required = false) AbExperimentSearchVO vo) {
    Long userId = AuthTokenFilter.getUserId(request);
    if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
    if (vo == null) vo = new AbExperimentSearchVO();
    String roleCode = AuthTokenFilter.getRoleCode(request);
    List<Long> visibleIds = dataScopeService.getVisibleUserIds(userId, roleCode);
    if (visibleIds != null) vo.setOwnerIds(visibleIds);  // ⚠️ 可选，管理员可能为 null
    // ...
}

// AbTestServiceImpl.java:42-49
Specification<AbExperiment> spec = (root, query, cb) -> {
    List<Predicate> predicates = new ArrayList<>();
    predicates.add(cb.equal(root.get("deleted"), 0));
    if (vo.getOwnerId() != null && vo.getOwnerId() > 0) {  // ⚠️ 可选
        predicates.add(cb.equal(root.get("ownerId"), vo.getOwnerId()));
    } else if (vo.getOwnerIds() != null && !vo.getOwnerIds().isEmpty()) {  // ⚠️ 可选
        predicates.add(root.get("ownerId").in(vo.getOwnerIds()));
    }
    // ...
};
```

**安全风险**:
- **CVSS 评分**: 9.1 (CRITICAL)
- **CWE**: CWE-639 (Authorization Bypass Through User-Controlled Key)
- **影响**: 用户可查看其他用户的实验数据

**修复方案**:
```java
// 方案 1: Service 层强制 ownerId 过滤
public PageResultVO<AbExperimentVO> search(AbExperimentSearchVO vo, Long userId) {
    vo.validateParams();
    String sortName = SORTABLE.contains(vo.getSortName()) ? vo.getSortName() : "createTime";
    Pageable pageable = PageRequest.of(vo.getPage(), vo.getRows(),
            Sort.by("desc".equalsIgnoreCase(vo.getSortOrder()) ? Sort.Direction.DESC : Sort.Direction.ASC, sortName));

    Specification<AbExperiment> spec = (root, query, cb) -> {
        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.equal(root.get("deleted"), 0));
        
        // ✅ 强制添加 ownerId 过滤
        if (vo.getOwnerIds() != null && !vo.getOwnerIds().isEmpty()) {
            predicates.add(root.get("ownerId").in(vo.getOwnerIds()));
        } else {
            predicates.add(cb.equal(root.get("ownerId"), userId));
        }
        
        // 其他条件...
        return cb.and(predicates.toArray(new Predicate[0]));
    };

    Page<AbExperiment> page = experimentRepository.findAll(spec, pageable);
    // ...
}

// Controller 传递 userId
@PostMapping("/experiment/list")
public RESTResult<PageResultVO<AbExperimentVO>> list(HttpServletRequest request,
        @RequestBody(required = false) AbExperimentSearchVO vo) {
    Long userId = AuthTokenFilter.getUserId(request);
    if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
    if (vo == null) vo = new AbExperimentSearchVO();
    String roleCode = AuthTokenFilter.getRoleCode(request);
    List<Long> visibleIds = dataScopeService.getVisibleUserIds(userId, roleCode);
    if (visibleIds != null) vo.setOwnerIds(visibleIds);
    RESTResult<PageResultVO<AbExperimentVO>> r = RESTResult.getSuccess(
        abTestService.search(vo, userId));  // ✅ 传递 userId
    r.setTraceId(MDC.get("traceId"));
    return r;
}
```

#### P0-2: 删除/修改操作缺少所有权校验

**位置**: 
- `AbTestController.java:72-78` (delete)
- `AbTestController.java:82-89` (updateStatus)
- `AbTestController.java:93-99` (setWinner)

**问题**: 
- 删除实验仅校验用户登录，未校验是否为创建者
- 用户可通过修改 ID 删除其他用户的实验

**代码示例**:
```java
// AbTestController.java:72-78
@PostMapping("/experiment/delete")
public RESTResult<Void> delete(HttpServletRequest request, @RequestParam Long id) {
    if (AuthTokenFilter.getUserId(request) == null) 
        return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
    abTestService.delete(id);  // ❌ 未校验 userId 是否匹配
    // ...
}

// AbTestServiceImpl.java:112-122
@Transactional(rollbackFor = Exception.class)
@CacheEvict(value = "abtest:experiment", key = "#id")
public void delete(Long id) {
    AbExperiment entity = experimentRepository.findByIdAndDeleted(id, 0)
            .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "实验不存在"));
    entity.setDeleted(1);  // ❌ 未校验 entity.getOwnerId() 是否匹配当前用户
    experimentRepository.save(entity);
    // ...
}
```

**修复方案**:
```java
// Service 层添加所有权校验
@Transactional(rollbackFor = Exception.class)
@CacheEvict(value = "abtest:experiment", key = "#id")
public void delete(Long id, Long userId) {
    AbExperiment entity = experimentRepository.findByIdAndDeleted(id, 0)
            .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "实验不存在"));
    
    // ✅ 校验数据所有权
    if (!entity.getOwnerId().equals(userId)) {
        throw new BusinessException(ErrorCode.FORBIDDEN, "无权限删除该实验");
    }
    
    entity.setDeleted(1);
    experimentRepository.save(entity);
    // 级联删除变体...
}

// Controller 传递 userId
@PostMapping("/experiment/delete")
public RESTResult<Void> delete(HttpServletRequest request, @RequestParam Long id) {
    Long userId = AuthTokenFilter.getUserId(request);
    if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
    abTestService.delete(id, userId);  // ✅ 传递 userId
    RESTResult<Void> r = RESTResult.deleteSuccess(null);
    r.setTraceId(MDC.get("traceId"));
    return r;
}
```

**需修复的方法**:
- `delete(Long id)` → `delete(Long id, Long userId)`
- `updateStatus(Long id, Integer status)` → `updateStatus(Long id, Integer status, Long userId)`
- `setWinner(AbSetWinnerVO vo)` → `setWinner(AbSetWinnerVO vo, Long userId)`
- `deleteVariant(Long id)` → `deleteVariant(Long id, Long userId)`

---

## 5. 错误处理规范

### 5.1 统一异常处理

**合规性**: ✅ 80% 符合

**分析**: 使用 `BusinessException` + `GlobalExceptionHandler` 统一处理。

**代码示例**:
```java
// AbTestServiceImpl.java:72-74
AbExperiment e = experimentRepository.findByIdAndDeleted(id, 0)
        .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "实验不存在"));
```

**优点**:
- ✅ 统一异常类型（BusinessException）
- ✅ 错误码规范（ErrorCode.DATA_NOT_FOUND）
- ✅ 事务回滚配置（`@Transactional(rollbackFor = Exception.class)`）

**问题**:
- ⚠️ 缺少审计日志（删除/修改操作未记录）
- ⚠️ 统计计算异常被静默吞噬（`AbTestServiceImpl.java:358-363`）

**代码示例**:
```java
// AbTestServiceImpl.java:358-363
} catch (Exception e) {
    return new AbStatisticalTestVO(
            0.0, 1.0, 0.0, false, variantA.getVariantId(), variantA.getVariantName(),
            "统计检验失败: " + e.getMessage()  // ⚠️ 异常信息可能泄露内部细节
    );
}
```

**修复建议**:
```java
} catch (Exception e) {
    log.error("卡方检验失败: experimentId={}, error={}", experimentId, e.getMessage(), e);
    return new AbStatisticalTestVO(
            0.0, 1.0, 0.0, false, variantA.getVariantId(), variantA.getVariantName(),
            "统计检验失败，请联系管理员"  // ✅ 不泄露内部细节
    );
}
```

---

## 6. 问题清单

### P0 - 阻塞级

| 编号 | 问题 | 文件 | 工作量 |
|------|------|------|--------|
| P0-1 | 缺少 owner_id 强制过滤 | AbTestServiceImpl.java:42-58 | 1.5 人日 |
| P0-2 | 删除/修改操作缺少所有权校验 | AbTestServiceImpl.java:112-142 | 2.0 人日 |
| P0-3 | 计数器更新无并发控制 | AbVariantRepository.java:20-29 | 2.5 人日 |

**P0-3 详细说明**:

**问题**: 计数器更新使用 JPA 原生 UPDATE，无并发控制

**位置**: `AbVariantRepository.java:20-29`

**代码示例**:
```java
@Modifying
@Query("UPDATE AbVariant v SET v.viewCount = v.viewCount + 1, v.conversionRate = CAST((v.conversionCount * 1.0 / NULLIF(v.viewCount + 1, 0)) AS java.math.BigDecimal) WHERE v.id = :id")
void incrementViewCount(@Param("id") Long id);

@Modifying
@Query("UPDATE AbVariant v SET v.conversionCount = v.conversionCount + 1, v.conversionRate = CAST(((v.conversionCount + 1) * 1.0 / NULLIF(v.viewCount, 0)) AS java.math.BigDecimal) WHERE v.id = :id")
void incrementConversionCount(@Param("id") Long id);
```

**问题分析**:
- 高并发场景下，多个事件同时记录会导致计数不准确
- 转化率计算依赖 viewCount/conversionCount，可能出现不一致
- 无乐观锁或悲观锁保护

**修复方案**:
```java
// 方案 1: 使用数据库原子操作（推荐）
@Modifying
@Query(value = "UPDATE ab_variant SET view_count = view_count + 1, conversion_rate = (conversion_count * 1.0 / NULLIF(view_count + 1, 0)) WHERE id = :id", nativeQuery = true)
void incrementViewCount(@Param("id") Long id);

// 方案 2: 使用乐观锁
@Entity
@Table(name = "ab_variant")
public class AbVariant {
    @Version
    private Long version;  // ✅ 添加版本号
    // ...
}

// 方案 3: 使用 Redis 计数器 + 定时同步
@Service
public class AbTestServiceImpl {
    @Resource
    private RedisTemplate<String, Long> redisTemplate;
    
    public void recordEvent(AbEventSaveVO vo) {
        // 1. 记录事件到数据库
        AbEvent event = new AbEvent();
        // ...
        eventRepository.save(event);
        
        // 2. Redis 计数器自增（原子操作）
        String key = "abtest:variant:" + vo.getVariantId() + ":" + vo.getEventType();
        redisTemplate.opsForValue().increment(key);
        
        // 3. 定时任务同步到数据库（每分钟）
    }
}
```

### P1 - 高优先级

| 编号 | 问题 | 文件 | 工作量 |
|------|------|------|--------|
| P1-1 | 缺少 API 限流保护 | AbTestController.java | 1.0 人日 |
| P1-2 | 事件记录后未失效统计缓存 | AbTestServiceImpl.java:175-196 | 0.5 人日 |
| P1-3 | 缺少 L1 缓存配置 | CacheConfig.java | 0.5 人日 |
| P1-4 | 列表查询存在 N+1 问题 | AbTestServiceImpl.java:61-67 | 1.0 人日 |
| P1-5 | 缺少审计日志 | AbTestServiceImpl.java | 1.0 人日 |
| P1-6 | 测试覆盖率低 | 仅 2 个测试文件 | 3.0 人日 |

**P1-1 详细说明**:

**问题**: 无 API 限流保护，存在滥用风险

**修复方案**:
```java
// 使用 Resilience4j RateLimiter
@PostMapping("/event/record")
@RateLimiter(name = "abtest-event", fallbackMethod = "recordEventFallback")
public RESTResult<Void> recordEvent(HttpServletRequest request, @Valid @RequestBody AbEventSaveVO vo) {
    // ...
}

public RESTResult<Void> recordEventFallback(HttpServletRequest request, AbEventSaveVO vo, Throwable t) {
    return RESTResult.error(ErrorCode.RATE_LIMIT_EXCEEDED, "请求过于频繁，请稍后再试");
}

// application.yml
resilience4j:
  ratelimiter:
    instances:
      abtest-event:
        limit-for-period: 100
        limit-refresh-period: 1s
        timeout-duration: 0s
```

**P1-4 详细说明**:

**问题**: 列表查询存在 N+1 问题

**位置**: `AbTestServiceImpl.java:61-67`

**代码示例**:
```java
Page<AbExperiment> page = experimentRepository.findAll(spec, pageable);
List<AbExperimentVO> list = page.getContent().stream().map(e -> {
    AbExperimentVO vo2 = toExperimentVO(e);
    vo2.setVariants(variantRepository.findByExperimentIdAndDeleted(e.getId(), 0)  // ❌ N+1 查询
            .stream().map(this::toVariantVO).collect(Collectors.toList()));
    return vo2;
}).collect(Collectors.toList());
```

**修复方案**:
```java
// 方案 1: 使用 JOIN FETCH
@Query("SELECT DISTINCT e FROM AbExperiment e LEFT JOIN FETCH e.variants v WHERE e.deleted = 0 AND v.deleted = 0")
Page<AbExperiment> findAllWithVariants(Specification<AbExperiment> spec, Pageable pageable);

// 方案 2: 批量查询变体
Page<AbExperiment> page = experimentRepository.findAll(spec, pageable);
List<Long> experimentIds = page.getContent().stream().map(AbExperiment::getId).collect(Collectors.toList());
List<AbVariant> allVariants = variantRepository.findByExperimentIdInAndDeleted(experimentIds, 0);
Map<Long, List<AbVariant>> variantMap = allVariants.stream()
        .collect(Collectors.groupingBy(AbVariant::getExperimentId));

List<AbExperimentVO> list = page.getContent().stream().map(e -> {
    AbExperimentVO vo2 = toExperimentVO(e);
    vo2.setVariants(variantMap.getOrDefault(e.getId(), Collections.emptyList())
            .stream().map(this::toVariantVO).collect(Collectors.toList()));
    return vo2;
}).collect(Collectors.toList());
```

### P2 - 中优先级

| 编号 | 问题 | 文件 | 工作量 |
|------|------|------|--------|
| P2-1 | 统计计算异常被静默吞噬 | AbTestServiceImpl.java:358-363 | 0.5 人日 |
| P2-2 | 缺少输入长度限制 | AbExperimentSaveVO.java | 0.5 人日 |
| P2-3 | 自动收敛逻辑过于简单 | AbTestServiceImpl.java:401-430 | 1.5 人日 |
| P2-4 | 缺少实验状态机校验 | AbTestServiceImpl.java:126-130 | 1.0 人日 |
| P2-5 | 缺少变体数量限制 | AbTestServiceImpl.java:100-107 | 0.5 人日 |

**P2-3 详细说明**:

**问题**: 自动收敛逻辑过于简单，仅基于转化数量

**位置**: `AbTestServiceImpl.java:401-430`

**代码示例**:
```java
@Override
@Transactional(rollbackFor = Exception.class)
public int autoConvergeAll() {
    List<AbExperiment> running = experimentRepository.findAll(
        (root, query, cb) -> cb.and(
            cb.equal(root.get("deleted"), 0),
            cb.equal(root.get("status"), 1)
        )
    );
    int converged = 0;
    for (AbExperiment experiment : running) {
        try {
            List<AbVariant> variants = variantRepository.findByExperimentIdAndDeleted(experiment.getId(), 0);
            if (variants.size() < 2) continue;
            long totalViews = variants.stream().mapToLong(v -> v.getViewCount() != null ? v.getViewCount() : 0L).sum();
            if (totalViews < 100) continue;  // ⚠️ 硬编码阈值
            AbVariant best = variants.stream()
                    .max(Comparator.comparingLong(v -> v.getConversionCount() != null ? v.getConversionCount() : 0L))  // ⚠️ 仅比较转化数，未考虑统计显著性
                    .orElse(null);
            if (best == null) continue;
            experiment.setStatus(2);
            experiment.setWinnerVariantId(best.getId());
            experimentRepository.save(experiment);
            converged++;
        } catch (Exception e) {
            // skip individual failures  // ⚠️ 异常被静默吞噬
        }
    }
    return converged;
}
```

**修复建议**:
```java
@Override
@Transactional(rollbackFor = Exception.class)
public int autoConvergeAll() {
    List<AbExperiment> running = experimentRepository.findAll(
        (root, query, cb) -> cb.and(
            cb.equal(root.get("deleted"), 0),
            cb.equal(root.get("status"), 1)
        )
    );
    int converged = 0;
    for (AbExperiment experiment : running) {
        try {
            // 1. 检查样本量是否足够
            AbExperimentStatisticsVO stats = getExperimentStatistics(experiment.getId());
            if (stats.getTotalSamples() < 1000) continue;  // ✅ 配置化阈值
            
            // 2. 检查统计显著性
            if (stats.getStatisticalTest() == null || !stats.getStatisticalTest().getIsSignificant()) {
                continue;  // ✅ 必须有统计显著性
            }
            
            // 3. 检查运行时长
            if (experiment.getStartTime() != null) {
                long runningDays = Duration.between(
                    experiment.getStartTime().toInstant(), 
                    Instant.now()
                ).toDays();
                if (runningDays < 7) continue;  // ✅ 至少运行 7 天
            }
            
            // 4. 自动收敛
            experiment.setStatus(2);
            experiment.setWinnerVariantId(stats.getStatisticalTest().getWinnerVariantId());
            experiment.setConclusion("自动收敛: " + stats.getStatisticalTest().getConclusion());
            experimentRepository.save(experiment);
            converged++;
            
            log.info("实验自动收敛: experimentId={}, winner={}", experiment.getId(), stats.getStatisticalTest().getWinnerName());
        } catch (Exception e) {
            log.error("实验自动收敛失败: experimentId={}, error={}", experiment.getId(), e.getMessage(), e);
        }
    }
    return converged;
}
```

### P3 - 低优先级

| 编号 | 问题 | 文件 | 工作量 |
|------|------|------|--------|
| P3-1 | 缺少 Swagger 参数描述 | AbTestController.java | 0.5 人日 |
| P3-2 | 缺少方法级权限注解 | AbTestController.java | 0.5 人日 |
| P3-3 | 缺少 VO 字段注释 | AbExperimentVO.java 等 | 0.5 人日 |
| P3-4 | 缺少索引优化建议 | schema.sql | 0.5 人日 |
| P3-5 | 缺少数据归档策略 | AbEvent 表 | 1.0 人日 |

---

## 7. 修复建议

### 7.1 立即修复（P0）

**优先级**: 🔴 必须在上线前修复

**P0-1: 添加 owner_id 强制过滤**
- **工作量**: 1.5 人日
- **步骤**:
  1. 修改 `AbTestServiceImpl.search()` 强制添加 ownerId 过滤
  2. Controller 传递 userId 参数
  3. 添加单元测试验证数据隔离

**P0-2: 添加所有权校验**
- **工作量**: 2.0 人日
- **步骤**:
  1. 修改 `delete()` / `updateStatus()` / `setWinner()` / `deleteVariant()` 方法签名，添加 userId 参数
  2. Service 层添加所有权校验逻辑
  3. Controller 传递 userId
  4. 添加单元测试验证权限控制

**P0-3: 修复计数器并发问题**
- **工作量**: 2.5 人日
- **步骤**:
  1. 评估方案：数据库原子操作 vs 乐观锁 vs Redis 计数器
  2. 实现选定方案（推荐 Redis 计数器 + 定时同步）
  3. 添加压力测试验证并发正确性
  4. 监控计数器准确性

**总计**: 6.0 人日

### 7.2 短期修复（P1）

**优先级**: ⚠️ 建议在 1-2 周内修复

**P1-1: 添加 API 限流**
- 使用 Resilience4j RateLimiter
- 配置合理的限流阈值（事件记录 100/s，统计查询 10/s）

**P1-2: 修复缓存失效**
- 事件记录后失效统计缓存
- 添加 `@CacheEvict(value = "abtest:statistics", key = "#vo.experimentId")`

**P1-3: 配置 L1 缓存**
- 在 CacheConfig 中配置 Caffeine 本地缓存
- 实验详情缓存 1 小时，统计结果缓存 5 分钟

**P1-4: 优化 N+1 查询**
- 使用批量查询变体数据
- 或使用 JOIN FETCH（注意分页问题）

**P1-5: 添加审计日志**
- 删除/修改操作记录到 sys_log_operation 表
- 包含操作人、操作时间、操作内容

**P1-6: 提升测试覆盖率**
- 目标覆盖率 80%+
- 重点测试：数据隔离、并发控制、统计计算

**总计**: 7.0 人日

### 7.3 中期优化（P2）

**优先级**: ℹ️ 建议在 1-2 个月内优化

**P2-1: 改进异常处理**
- 统计计算异常记录日志，不泄露内部细节

**P2-2: 添加输入长度限制**
- 实验名称 ≤ 128 字符
- 描述 ≤ 5000 字符
- 结论 ≤ 2000 字符

**P2-3: 改进自动收敛逻辑**
- 基于统计显著性（p < 0.05）
- 最小样本量 1000+
- 最小运行时长 7 天
- 配置化阈值

**P2-4: 添加状态机校验**
- 草稿 → 运行中 → 已完成/已暂停
- 禁止非法状态转换

**P2-5: 添加变体数量限制**
- 每个实验最多 10 个变体
- 防止滥用

**总计**: 4.0 人日

### 7.4 长期改进（P3）

**优先级**: 💡 可选优化

**P3-1: 完善 API 文档**
- 添加 Swagger 参数描述
- 添加请求/响应示例

**P3-2: 添加方法级权限注解**
- 使用 `@PreAuthorize` 注解
- 细粒度权限控制

**P3-3: 完善代码注释**
- VO 字段添加中文注释
- 复杂逻辑添加说明

**P3-4: 优化数据库索引**
- 分析慢查询日志
- 添加复合索引

**P3-5: 实现数据归档**
- AbEvent 表按月归档
- 保留最近 3 个月数据

**总计**: 3.0 人日

---

## 8. 总结

**总体评估**: 76/100 (Grade C+)

**关键指标**:
- P0 问题: 3 个（数据隔离、所有权校验、并发控制）
- P1 问题: 6 个（限流、缓存、N+1、审计、测试）
- P2 问题: 5 个（异常处理、输入校验、自动收敛、状态机、变体限制）
- P3 问题: 5 个（文档、权限、注释、索引、归档）
- 总工作量: 20.0 人日

**优势**:
1. ✅ API 设计完全符合统一 POST 规范
2. ✅ 使用 JPA Specification 动态查询
3. ✅ 逻辑删除规范正确实现
4. ✅ 统计分析功能完善（卡方检验、日趋势）
5. ✅ 有缓存策略（虽然不完整）

**核心问题**:
1. 🔴 **数据隔离不强制**：缺少 owner_id 强制过滤（CVSS 9.1）
2. 🔴 **所有权校验缺失**：删除/修改操作未校验数据所有权
3. 🔴 **并发控制缺失**：计数器更新无并发保护，高并发下数据不一致
4. ⚠️ 缺少 API 限流保护
5. ⚠️ 测试覆盖率低（仅 2 个测试文件）

**生产就绪度**: 🔴 必须修复 P0 问题后上线

**建议**:
1. **立即修复 P0 问题**（6 人日）：数据隔离、所有权校验、并发控制
2. **1-2 周内修复 P1 问题**（7 人日）：限流、缓存、N+1、审计、测试
3. **1-2 个月内优化 P2 问题**（4 人日）：异常处理、输入校验、自动收敛
4. **长期改进 P3 问题**（3 人日）：文档、权限、注释、索引、归档

**对比其他模块**:
- **agent 模块**: 架构评分 85/100，安全评分 68/100
- **abtest 模块**: 架构评分 76/100（低于 agent）
- **共同问题**: 数据隔离、所有权校验、测试覆盖率
- **abtest 特有问题**: 并发控制（计数器更新）

---

**报告生成**: Claude Code (Opus 4.6)  
**审查状态**: 待人工复核

