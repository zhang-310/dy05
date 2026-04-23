# ADR-003：JPA Specification 动态查询

## 状态

已采纳

## 上下文

列表查询需要支持多条件组合筛选、动态排序、分页。需要一种灵活的查询构建方式。

## 决策

使用 JPA Specification（`JpaSpecificationExecutor`）构建动态查询。

## 实现模式

```java
Specification<Entity> spec = (root, query, cb) -> {
    List<Predicate> predicates = new ArrayList<>();

    // 数据隔离
    predicates.add(cb.equal(root.get("ownerId"), userId));

    // 动态条件
    if (StringUtils.hasText(vo.getKeyword())) {
        predicates.add(cb.like(root.get("name"), "%" + vo.getKeyword() + "%"));
    }

    return cb.and(predicates.toArray(new Predicate[0]));
};

Page<Entity> page = repository.findAll(spec, pageable);
```

## 后果

- 正面：灵活组合查询条件，无需为每种组合写不同 Repository 方法
- 正面：类型安全，编译期检查
- 正面：与 Spring Data 分页/排序无缝集成
- 负面：代码相对繁琐（Lambda 表达式）
- 负面：复杂 JOIN 查询不够直观
