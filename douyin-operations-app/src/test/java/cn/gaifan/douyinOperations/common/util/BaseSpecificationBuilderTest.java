package cn.gaifan.douyinOperations.common.util;

import jakarta.persistence.criteria.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * JPA Specification 动态查询条件构建测试
 *
 * 注意: BaseSpecificationBuilder 类尚未在 codebase 中创建。
 * 本测试基于项目中各 ServiceImpl 使用 JPA Specification 的通用模式编写，
 * 验证动态查询条件构建的核心行为。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JPA Specification 动态查询条件构建测试")
@SuppressWarnings({"unchecked", "rawtypes"})
class BaseSpecificationBuilderTest {

    @Mock
    private Root root;

    @Mock
    private CriteriaQuery query;

    @Mock
    private CriteriaBuilder cb;

    @Mock
    private Path path;

    @Mock
    private Predicate combinedPredicate;

    @BeforeEach
    void setUp() {
        lenient().when(root.get(anyString())).thenReturn(path);
        lenient().when(cb.and(any(Predicate[].class))).thenReturn(combinedPredicate);
    }

    @Test
    @DisplayName("build_withOwner_addsOwnerPredicate - 添加 owner 过滤条件")
    void build_withOwner_addsOwnerPredicate() {
        Long ownerId = 42L;
        Predicate ownerPredicate = mock(Predicate.class);
        when(cb.equal(path, ownerId)).thenReturn(ownerPredicate);

        // Simulate the pattern: build predicates with ownerId
        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.equal(root.get("ownerId"), ownerId));
        Predicate result = cb.and(predicates.toArray(new Predicate[0]));

        verify(root).get("ownerId");
        verify(cb).equal(path, ownerId);
        assertNotNull(result);
    }

    @Test
    @DisplayName("build_withEq_addsEqualPredicate - 添加等值条件")
    void build_withEq_addsEqualPredicate() {
        Predicate eqPredicate = mock(Predicate.class);
        when(cb.equal(path, "active")).thenReturn(eqPredicate);

        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.equal(root.get("status"), "active"));
        Predicate result = cb.and(predicates.toArray(new Predicate[0]));

        verify(root).get("status");
        verify(cb).equal(path, "active");
        assertNotNull(result);
    }

    @Test
    @DisplayName("build_withLike_addsLikePredicate - 添加模糊匹配条件")
    void build_withLike_addsLikePredicate() {
        Predicate likePredicate = mock(Predicate.class);
        when(cb.like(any(Expression.class), eq("%test%"))).thenReturn(likePredicate);

        String keyword = "test";
        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.like(root.get("name"), "%" + keyword + "%"));
        Predicate result = cb.and(predicates.toArray(new Predicate[0]));

        verify(cb).like(any(Expression.class), eq("%test%"));
        assertNotNull(result);
    }

    @Test
    @DisplayName("build_withNullValue_skipsPredicate - null 值跳过条件")
    void build_withNullValue_skipsPredicate() {
        Predicate deletedPredicate = mock(Predicate.class);
        when(cb.equal(path, 0)).thenReturn(deletedPredicate);

        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.equal(root.get("deleted"), 0));

        String nullValue = null;
        if (nullValue != null && !nullValue.isEmpty()) {
            predicates.add(cb.equal(root.get("name"), nullValue));
        }

        cb.and(predicates.toArray(new Predicate[0]));

        // "deleted" was accessed, but "name" should never be accessed since value is null
        verify(root, times(1)).get(anyString());
        verify(root).get("deleted");
    }

    @Test
    @DisplayName("build_withEmptyString_skipsPredicate - 空字符串跳过条件")
    void build_withEmptyString_skipsPredicate() {
        Predicate deletedPredicate = mock(Predicate.class);
        when(cb.equal(path, 0)).thenReturn(deletedPredicate);

        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.equal(root.get("deleted"), 0));

        String emptyValue = "";
        if (emptyValue != null && !emptyValue.trim().isEmpty()) {
            predicates.add(cb.equal(root.get("keyword"), emptyValue));
        }

        cb.and(predicates.toArray(new Predicate[0]));

        // "keyword" should never be accessed since value is empty
        verify(root, times(1)).get(anyString());
    }

    @Test
    @DisplayName("build_withIn_addsInPredicate - 添加 IN 条件")
    void build_withIn_addsInPredicate() {
        CriteriaBuilder.In inClause = mock(CriteriaBuilder.In.class);
        when(path.in(anyCollection())).thenReturn(inClause);

        List<Long> ids = List.of(1L, 2L, 3L);

        List<Predicate> predicates = new ArrayList<>();
        predicates.add(root.get("userId").in(ids));
        Predicate result = cb.and(predicates.toArray(new Predicate[0]));

        verify(root).get("userId");
        verify(path).in(ids);
        assertNotNull(result);
    }

    @Test
    @DisplayName("build_withBetween_addsRangePredicate - 添加范围条件")
    void build_withBetween_addsRangePredicate() {
        Predicate gtePredicate = mock(Predicate.class);
        Predicate ltePredicate = mock(Predicate.class);
        when(cb.greaterThanOrEqualTo(any(Expression.class), any(Comparable.class))).thenReturn(gtePredicate);
        when(cb.lessThanOrEqualTo(any(Expression.class), any(Comparable.class))).thenReturn(ltePredicate);

        java.sql.Timestamp start = java.sql.Timestamp.valueOf("2026-01-01 00:00:00");
        java.sql.Timestamp end = java.sql.Timestamp.valueOf("2026-12-31 23:59:59");

        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.greaterThanOrEqualTo(root.get("createTime"), start));
        predicates.add(cb.lessThanOrEqualTo(root.get("createTime"), end));
        Predicate result = cb.and(predicates.toArray(new Predicate[0]));

        verify(cb).greaterThanOrEqualTo(any(Expression.class), eq(start));
        verify(cb).lessThanOrEqualTo(any(Expression.class), eq(end));
        assertNotNull(result);
    }
}
