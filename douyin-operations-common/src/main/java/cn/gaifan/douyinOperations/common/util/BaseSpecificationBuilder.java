package cn.gaifan.douyinOperations.common.util;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * 通用 Specification 构建器
 * 统一数据隔离 + deleted 过滤 + 动态条件构建
 */
public class BaseSpecificationBuilder<T> {

    private final List<SpecCondition<T>> conditions = new ArrayList<>();
    private Long ownerId;
    private String ownerIdField = "ownerId";

    public static <T> BaseSpecificationBuilder<T> of(Class<T> entityClass) {
        return new BaseSpecificationBuilder<>();
    }

    /**
     * 设置 owner_id 数据隔离（强制）
     */
    public BaseSpecificationBuilder<T> withOwner(Long ownerId) {
        this.ownerId = ownerId;
        return this;
    }

    /**
     * 设置 owner_id 字段名（默认 "ownerId"，部分表用 "userId"）
     */
    public BaseSpecificationBuilder<T> ownerField(String fieldName) {
        this.ownerIdField = fieldName;
        return this;
    }

    /**
     * 等值匹配（非 null 时生效）
     */
    public BaseSpecificationBuilder<T> eq(String field, Object value) {
        if (value != null) {
            conditions.add((root, cb) -> cb.equal(root.get(field), value));
        }
        return this;
    }

    /**
     * 模糊查询（非空字符串时生效）
     */
    public BaseSpecificationBuilder<T> like(String field, String value) {
        if (StringUtils.hasText(value)) {
            conditions.add((root, cb) -> cb.like(root.get(field), "%" + value + "%"));
        }
        return this;
    }

    /**
     * IN 查询（非空集合时生效）
     */
    public BaseSpecificationBuilder<T> in(String field, Collection<?> values) {
        if (values != null && !values.isEmpty()) {
            conditions.add((root, cb) -> root.get(field).in(values));
        }
        return this;
    }

    /**
     * 大于等于（非 null 时生效）
     */
    public <Y extends Comparable<Y>> BaseSpecificationBuilder<T> gte(String field, Y value) {
        if (value != null) {
            conditions.add((root, cb) -> cb.greaterThanOrEqualTo(root.get(field), value));
        }
        return this;
    }

    /**
     * 小于等于（非 null 时生效）
     */
    public <Y extends Comparable<Y>> BaseSpecificationBuilder<T> lte(String field, Y value) {
        if (value != null) {
            conditions.add((root, cb) -> cb.lessThanOrEqualTo(root.get(field), value));
        }
        return this;
    }

    /**
     * 时间范围查询
     */
    public BaseSpecificationBuilder<T> between(String field, Timestamp start, Timestamp end) {
        if (start != null) {
            conditions.add((root, cb) -> cb.greaterThanOrEqualTo(root.get(field), start));
        }
        if (end != null) {
            conditions.add((root, cb) -> cb.lessThanOrEqualTo(root.get(field), end));
        }
        return this;
    }

    /**
     * 自定义条件
     */
    public BaseSpecificationBuilder<T> custom(SpecCondition<T> condition) {
        conditions.add(condition);
        return this;
    }

    /**
     * 构建 Specification
     */
    public Specification<T> build() {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 强制数据隔离
            if (ownerId != null) {
                predicates.add(cb.equal(root.get(ownerIdField), ownerId));
            }

            // 强制 deleted = 0（Entity 已有 @SQLRestriction，此处双保险）
            try {
                root.get("deleted");
                predicates.add(cb.equal(root.get("deleted"), 0));
            } catch (IllegalArgumentException ignored) {
                // Entity 没有 deleted 字段，跳过
            }

            // 动态条件
            for (SpecCondition<T> condition : conditions) {
                predicates.add(condition.toPredicate(root, cb));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    @FunctionalInterface
    public interface SpecCondition<T> {
        Predicate toPredicate(Root<T> root, CriteriaBuilder cb);
    }
}
