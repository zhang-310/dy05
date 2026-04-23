# 02 后端架构质量分析

> 综合评分：75/100（B 级）
> 发现问题：22 项（P0: 2 / P1: 8 / P2: 12）

---

## 积极发现

- SQL 注入防护强大：所有查询使用参数化（Specification / @Param）
- 异常处理统一：GlobalExceptionHandler 覆盖 7 类异常
- 事务管理规范：写操作标注 @Transactional，只读无标注
- 日志规范：使用 SLF4J 占位符，无敏感信息
- 分页安全：rows 上限 1000，防止超量查询

---

## P0 - 必须修复

### ARCH-01: Specification 查询代码大量重复

**影响范围**: 54 个 ServiceImpl
**重复模式**:
```java
Specification<Xxx> spec = (root, query, cb) -> {
    List<Predicate> predicates = new ArrayList<>();
    predicates.add(cb.equal(root.get("ownerId"), userId));
    predicates.add(cb.equal(root.get("deleted"), 0));
    if (vo.getXxx() != null) predicates.add(cb.like(...));
    // ... 10-20 行条件构建
    return cb.and(predicates.toArray(new Predicate[0]));
};
```

**建议**: 创建 `BaseSpecificationBuilder<T>`
```java
public class BaseSpecificationBuilder<T> {
    public static <T> Specification<T> build(Long ownerId, Consumer<PredicateCollector<T>> config) {
        return (root, query, cb) -> {
            PredicateCollector<T> collector = new PredicateCollector<>(root, cb);
            collector.equalRequired("ownerId", ownerId);
            collector.equalRequired("deleted", 0);
            config.accept(collector);
            return collector.toPredicate();
        };
    }
}
```

---

### ARCH-02: VO 与 Entity 手工转换重复

**影响范围**: 132 个 ServiceImpl，每个都有 `convert()` 方法

**当前代码**:
```java
private XxxVO convert(Xxx entity) {
    return XxxVO.builder()
        .id(entity.getId())
        .name(entity.getName())
        // ... 20+ 字段
        .build();
}
```

**建议**: 引入 MapStruct（编译期代码生成，零运行时开销）
```java
@Mapper(componentModel = "spring")
public interface ProductMapper {
    ProductVO toVO(DyProduct entity);
    DyProduct toEntity(ProductSaveVO vo);
}
```

---

## P1 - 应尽快修复

### ARCH-03: userId 提取逻辑不统一

5 个 Controller 自行实现 `extractUserIdFromAuth()`，其余使用 `AuthTokenFilter.getUserId(request)`。

**建议**: 创建 `@RequireAuth` 注解 + AOP 拦截器统一处理

### ARCH-04: 大数据量无分页查询

**文件**: `ProductScriptVersionServiceImpl.java:307,565`
```java
List<ProductScriptVersion> versions = versionRepository.findAll(spec);  // 无分页！
```
**风险**: 数据量大时 OOM
**修复**: 使用 Stream 或分页批处理

### ARCH-05: 缓存策略不完善

- CacheConfig 配置了 Caffeine + Redis
- 但缺少 `@CacheEvict`（更新时主动失效）
- 频繁更新的字段（effectivenessScore, usageCount）不适合缓存

### ARCH-06: 异步处理使用不足

- RabbitMQ 已配置但使用极少
- 话术生成、效果评分计算等长操作应改为异步

### ARCH-07: 跨模块事件通知缺失

- 模块间通过直接 Service 注入通信
- 建议关键操作使用 Spring Event 或 MQ 解耦

### ARCH-08: @Modifying 方法缺少事务注解

**文件**: `DyProductRepository.java`
```java
@Modifying
@Query("UPDATE DyProduct p SET p.status = :status WHERE p.id = :id")
void updateStatus(@Param("id") Long id, @Param("status") Integer status);
// 缺少 @Transactional
```

### ARCH-09: Timestamp 类型过时

多个 Entity 使用 `java.sql.Timestamp`，应迁移到 `java.time.LocalDateTime`

### ARCH-10: findAll() 无限查询风险

**文件**: `AiViralDetectionScheduler.java`
```java
Set<Long> analyzedVideoIds = viralAnalysisRepository.findAll().stream()...
```
全表加载到内存，数据量大时严重影响性能。

---

## P2 - 优化项

| # | 问题 | 建议 |
|---|------|------|
| ARCH-11 | VO 类缺少 @Size/@Pattern 校验注解 | 补充 Bean Validation |
| ARCH-12 | 部分方法缺少 JavaDoc | 补充关键业务方法文档 |
| ARCH-13 | System.out.println 残留 | 统一改为 log |
| ARCH-14 | 缺少 API 版本兼容策略 | 设计 v2 迁移方案 |
| ARCH-15 | Service 方法过长（>100行） | 拆分为私有方法 |
| ARCH-16 | 硬编码数字常量 | 提取为 Enum 或 Constant |
| ARCH-17 | 缺少限流注解 | 使用 Resilience4j @RateLimiter |
| ARCH-18 | 缺少审计日志 | 关键操作写入审计表 |
| ARCH-19 | 批量操作无 BatchInsert | 使用 JPA saveAll 优化 |
| ARCH-20 | 缺少 API 幂等性保证 | 关键写操作添加幂等键 |
| ARCH-21 | 无 Flyway/Liquibase | 引入数据库迁移版本管理 |
| ARCH-22 | 未使用虚拟线程 | JDK 21 虚拟线程可提升并发 |
