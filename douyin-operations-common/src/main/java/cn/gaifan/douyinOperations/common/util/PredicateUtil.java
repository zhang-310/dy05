package cn.gaifan.douyinOperations.common.util;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import cn.gaifan.douyinOperations.common.vo.SearchTimestamp;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.sql.Timestamp;
import java.util.List;

/**
 * JPA Criteria API 谓词构建工具类
 * <p>
 * 提供常用的查询条件构建方法，简化 JPA Specification 查询的编写。
 * 支持的类型包括：
 * <ul>
 *   <li>时间范围查询（Timestamp）</li>
 *   <li>IN查询（Long、String）</li>
 *   <li>等值查询（Boolean、Long、Integer、Double、String）</li>
 *   <li>模糊查询（String，自动转义特殊字符）</li>
 * </ul>
 * <p>
 * 使用示例：
 * <pre>
 * {@code
 * PredicateUtil<Entity> util = new PredicateUtil<>();
 * List<Predicate> predicates = new ArrayList<>();
 * 
 * // 时间范围查询
 * util.setTimestamp(predicates, root, cb, "createTime", searchTimestamp);
 * 
 * // IN查询
 * util.listLongIn(predicates, root, cb, "status", statusList);
 * 
 * // 等值查询
 * util.stringValue(predicates, root, cb, "name", name);
 * 
 * // 模糊查询
 * util.stringLike(predicates, root, cb, "description", keyword);
 * 
 * // 组合条件
 * Predicate finalPredicate = cb.and(predicates.toArray(new Predicate[0]));
 * }
 * </pre>
 *
 * @param <T> 实体类型
 * @author system
 */
@Component
public class PredicateUtil<T> {

    /**
     * IN查询子句的最大值数量限制
     * 防止生成过大的SQL语句，影响性能
     */
    private static final int MAX_IN_CLAUSE_SIZE = 1000;

    /**
     * 设置时间戳范围查询条件
     * 自动校验时间范围的有效性，如果开始时间晚于结束时间将抛出异常
     *
     * @param list              谓词列表
     * @param root              根路径
     * @param criteriaBuilder   条件构建器
     * @param name              字段名
     * @param date              时间戳搜索对象
     * @throws IllegalArgumentException 如果时间范围无效（开始时间晚于结束时间）
     */
    public void setTimestamp(List<Predicate> list, Root<T> root, CriteriaBuilder criteriaBuilder, 
                            String name, SearchTimestamp date) {
        if (date == null) {
            return;
        }

        // 校验时间范围的有效性
        if (!date.isValid()) {
            throw new IllegalArgumentException(
                    String.format("时间范围无效：开始时间不能晚于结束时间。字段: %s, start=%s, end=%s", 
                            name, date.getStart(), date.getEnd()));
        }

        Timestamp start = date.getStart();
        Timestamp end = date.getEnd();
        Path<Timestamp> path = root.get(name);

        if (start != null && end != null) {
            list.add(criteriaBuilder.between(path, start, end));
        } else if (start != null) {
            list.add(criteriaBuilder.greaterThanOrEqualTo(path, start));
        } else if (end != null) {
            list.add(criteriaBuilder.lessThanOrEqualTo(path, end));
        }
    }

    /**
     * 设置Long类型列表的IN查询条件
     *
     * @param list              谓词列表
     * @param root              根路径
     * @param criteriaBuilder   条件构建器
     * @param name              字段名
     * @param vs                值列表
     * @throws IllegalArgumentException 如果列表大小超过限制
     */
    public void listLongIn(List<Predicate> list, Root<T> root, CriteriaBuilder criteriaBuilder, 
                          String name, List<Long> vs) {
        listIn(list, root, criteriaBuilder, name, vs, Long.class);
    }

    /**
     * 设置String类型列表的IN查询条件
     *
     * @param list              谓词列表
     * @param root              根路径
     * @param criteriaBuilder   条件构建器
     * @param name              字段名
     * @param vs                值列表
     * @throws IllegalArgumentException 如果列表大小超过限制
     */
    public void listStringIn(List<Predicate> list, Root<T> root, CriteriaBuilder criteriaBuilder, 
                            String name, List<String> vs) {
        listIn(list, root, criteriaBuilder, name, vs, String.class);
    }

    /**
     * 通用的IN查询条件构建方法
     *
     * @param list              谓词列表
     * @param root              根路径
     * @param criteriaBuilder   条件构建器
     * @param name              字段名
     * @param values            值列表
     * @param valueType         值类型
     * @param <V>               值类型泛型
     * @throws IllegalArgumentException 如果列表大小超过限制
     */
    private <V> void listIn(List<Predicate> list, Root<T> root, CriteriaBuilder criteriaBuilder,
                           String name, List<V> values, Class<V> valueType) {
        if (values == null || values.isEmpty()) {
            return;
        }

        if (values.size() > MAX_IN_CLAUSE_SIZE) {
            throw new IllegalArgumentException(
                    String.format("IN查询列表大小超过限制 %d，当前大小: %d", MAX_IN_CLAUSE_SIZE, values.size()));
        }

        Path<V> path = root.get(name);
        CriteriaBuilder.In<V> in = criteriaBuilder.in(path);
        for (V value : values) {
            in.value(value);
        }
        list.add(in);
    }

    /**
     * 设置Boolean类型等值查询条件
     *
     * @param list              谓词列表
     * @param root              根路径
     * @param criteriaBuilder   条件构建器
     * @param name              字段名
     * @param v                 值
     */
    public void booleanValue(List<Predicate> list, Root<T> root, CriteriaBuilder criteriaBuilder, 
                            String name, Boolean v) {
        if (v != null) {
            list.add(criteriaBuilder.equal(root.get(name).as(Boolean.class), v));
        }
    }

    /**
     * 设置Long类型等值查询条件
     *
     * @param list              谓词列表
     * @param root              根路径
     * @param criteriaBuilder   条件构建器
     * @param name              字段名
     * @param v                 值
     */
    public void longValue(List<Predicate> list, Root<T> root, CriteriaBuilder criteriaBuilder, 
                         String name, Long v) {
        if (v != null) {
            list.add(criteriaBuilder.equal(root.get(name).as(Long.class), v));
        }
    }

    /**
     * 设置Integer类型等值查询条件
     *
     * @param list              谓词列表
     * @param root              根路径
     * @param criteriaBuilder   条件构建器
     * @param name              字段名
     * @param v                 值
     */
    public void integerValue(List<Predicate> list, Root<T> root, CriteriaBuilder criteriaBuilder, 
                            String name, Integer v) {
        if (v != null) {
            list.add(criteriaBuilder.equal(root.get(name).as(Integer.class), v));
        }
    }

    /**
     * 设置Double类型等值查询条件
     *
     * @param list              谓词列表
     * @param root              根路径
     * @param criteriaBuilder   条件构建器
     * @param name              字段名
     * @param v                 值
     */
    public void doubleValue(List<Predicate> list, Root<T> root, CriteriaBuilder criteriaBuilder, 
                           String name, Double v) {
        if (v != null) {
            list.add(criteriaBuilder.equal(root.get(name).as(Double.class), v));
        }
    }

    /**
     * 设置String类型模糊查询条件
     * 自动转义LIKE模式中的特殊字符，防止SQL注入风险
     *
     * @param list              谓词列表
     * @param root              根路径
     * @param criteriaBuilder   条件构建器
     * @param name              字段名
     * @param v                 值
     */
    public void stringLike(List<Predicate> list, Root<T> root, CriteriaBuilder criteriaBuilder, 
                          String name, String v) {
        if (StringUtils.isNotBlank(v)) {
            String escapedValue = escapeLikePattern(v);
            list.add(criteriaBuilder.like(root.get(name).as(String.class), "%" + escapedValue + "%"));
        }
    }

    /**
     * 转义LIKE模式中的特殊字符
     * 转义字符：\, %, _
     *
     * @param input 输入字符串
     * @return 转义后的字符串
     */
    private String escapeLikePattern(String input) {
        if (input == null) {
            return null;
        }
        return input.replace("\\", "\\\\")
                    .replace("%", "\\%")
                    .replace("_", "\\_");
    }

    /**
     * 设置String类型等值查询条件
     *
     * @param list              谓词列表
     * @param root              根路径
     * @param criteriaBuilder   条件构建器
     * @param name              字段名
     * @param v                 值
     */
    public void stringValue(List<Predicate> list, Root<T> root, CriteriaBuilder criteriaBuilder, 
                           String name, String v) {
        if (StringUtils.isNotBlank(v)) {
            list.add(criteriaBuilder.equal(root.get(name).as(String.class), v));
        }
    }
}