package cn.gaifan.douyinOperations.module.product.service.impl;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.product.entity.ProductScriptVersion;
import cn.gaifan.douyinOperations.module.product.repository.ProductScriptSnapshotRepository;
import cn.gaifan.douyinOperations.module.product.repository.ProductScriptVersionRepository;
import cn.gaifan.douyinOperations.module.product.vo.ProductScriptRecommendVO;
import cn.gaifan.douyinOperations.module.product.vo.ProductScriptVersionSaveVO;
import cn.gaifan.douyinOperations.module.product.vo.ProductScriptVersionVO;
import cn.gaifan.douyinOperations.module.product.vo.ProductVersionDiffVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 产品话术版本服务单元测试（纯 Mock，不启动 Spring 容器）
 * 针对当前 ProductScriptVersionServiceImpl（ProductScriptVersion 实体 + ProductScriptVersionRepository）
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ProductScriptVersionService 测试")
class ProductScriptVersionServiceImplTest {

    @InjectMocks
    private ProductScriptVersionServiceImpl versionService;

    @Mock
    private ProductScriptVersionRepository versionRepository;

    @Mock
    private ProductScriptSnapshotRepository snapshotRepository;

    private ProductScriptVersion testVersion;
    private Long testProductId = 1L;
    private Long userId = 1L;

    @BeforeEach
    void setUp() {
        testVersion = ProductScriptVersion.builder()
                .id(1L)
                .productId(testProductId)
                .versionNumber(1)
                .content("测试话术内容")
                .style("亲切")
                .effectivenessScore(new BigDecimal("85.00"))
                .conversionRate(new BigDecimal("12.5"))
                .usageCount(10)
                .isActive(true)
                .ownerId(userId)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("save 保存话术版本成功")
    void testSaveSuccess() {
        when(versionRepository.findMaxVersionNumberByProductId(testProductId)).thenReturn(0);
        when(versionRepository.save(any(ProductScriptVersion.class))).thenAnswer(invocation -> {
            ProductScriptVersion e = invocation.getArgument(0);
            e.setId(1L);
            return e;
        });

        ProductScriptVersionSaveVO vo = ProductScriptVersionSaveVO.builder()
                .productId(testProductId)
                .content("新话术内容")
                .style("专业")
                .build();

        ProductScriptVersionVO result = versionService.save(vo, userId);

        assertNotNull(result);
        assertNotNull(result.getId());
        assertEquals(testProductId, result.getProductId());
        assertEquals("新话术内容", result.getContent());
        verify(versionRepository, times(1)).save(any(ProductScriptVersion.class));
    }

    @Test
    @DisplayName("recommend 推荐版本成功")
    void testRecommendSuccess() {
        when(versionRepository.findAll(any(Specification.class))).thenReturn(List.of(testVersion));

        ProductScriptRecommendVO result = versionService.recommend(testProductId, 5, userId);

        assertNotNull(result);
        assertNotNull(result.getVersions());
        assertTrue(result.getVersions().size() > 0);
        verify(versionRepository, atLeast(1)).findAll(any(Specification.class));
    }

    @Test
    @DisplayName("calculateRecommendScore 计算推荐分数")
    void testCalculateRecommendScore() {
        double score = versionService.calculateRecommendScore(80.0, 20, 15.0);
        assertTrue(score >= 0 && score <= 100);
    }

    @Test
    @DisplayName("diffVersions 计算版本差异成功")
    void testDiffVersionsSuccess() {
        ProductScriptVersion versionA = ProductScriptVersion.builder()
                .id(1L)
                .productId(testProductId)
                .versionNumber(1)
                .content("Line 1\nLine 2\nLine 3")
                .build();
        ProductScriptVersion versionB = ProductScriptVersion.builder()
                .id(2L)
                .productId(testProductId)
                .versionNumber(2)
                .content("Line 1\nLine 2 modified\nLine 3\nLine 4")
                .build();

        when(versionRepository.findByProductIdAndVersionNumber(testProductId, 1)).thenReturn(Optional.of(versionA));
        when(versionRepository.findByProductIdAndVersionNumber(testProductId, 2)).thenReturn(Optional.of(versionB));

        ProductVersionDiffVO diff = versionService.diffVersions(testProductId, 1, 2);

        assertNotNull(diff);
        assertNotNull(diff.added);
        assertNotNull(diff.removed);
        assertTrue(diff.added.contains("Line 4"));
        assertTrue(diff.removed.contains("Line 2"));
    }

    @Test
    @DisplayName("diffVersions 版本不存在抛出异常")
    void testDiffVersionsNotFound() {
        when(versionRepository.findByProductIdAndVersionNumber(testProductId, 1)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> versionService.diffVersions(testProductId, 1, 2));
        assertEquals(ErrorCode.DATA_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("diffVersions 参数无效抛出异常")
    void testDiffVersionsInvalidParam() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> versionService.diffVersions(null, 1, 2));
        assertEquals(ErrorCode.VALIDATION_FAIL, ex.getErrorCode());
    }
}
