package cn.gaifan.douyinOperations.module.product.service.impl;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.product.entity.ProductScriptComparisonCache;
import cn.gaifan.douyinOperations.module.product.entity.ProductScriptEffectivenessRecord;
import cn.gaifan.douyinOperations.module.product.entity.ProductScriptVersion;
import cn.gaifan.douyinOperations.module.product.repository.ProductScriptComparisonCacheRepository;
import cn.gaifan.douyinOperations.module.product.repository.ProductScriptEffectivenessRecordRepository;
import cn.gaifan.douyinOperations.module.product.repository.ProductScriptVersionRepository;
import cn.gaifan.douyinOperations.module.product.vo.*;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 商品话术效果评分服务单元测试
 * 纯 Mock，不启动 Spring 容器
 *
 * @author Claude Code
 * @since 2026-03-06
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EffectivenessScoreService 测试")
class EffectivenessScoreServiceImplTest {

    @InjectMocks
    private EffectivenessScoreServiceImpl scoreService;

    @Mock
    private ProductScriptVersionRepository versionRepository;

    @Mock
    private ProductScriptEffectivenessRecordRepository recordRepository;

    @Mock
    private ProductScriptComparisonCacheRepository cacheRepository;

    @Mock
    private ObjectMapper objectMapper;

    private ProductScriptVersion testVersion;
    private Long testProductId = 1L;
    private Long testVersionId = 1L;
    private Long userId = 1L;

    @BeforeEach
    void setUp() {
        testVersion = ProductScriptVersion.builder()
                .id(testVersionId)
                .productId(testProductId)
                .versionNumber(1)
                .content("测试话术内容")
                .style("亲切")
                .effectivenessScore(new BigDecimal("75.00"))
                .conversionRate(new BigDecimal("12.5"))
                .usageCount(50)
                .likesCount(100)
                .commentsCount(20)
                .isActive(true)
                .ownerId(userId)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .deleted(0)
                .build();
    }

    // ================ calculateScore 测试 ================

    @Test
    @DisplayName("计算评分 - 成功计算")
    void testCalculateScoreSuccess() {
        when(versionRepository.findById(testVersionId)).thenReturn(Optional.of(testVersion));

        Double score = scoreService.calculateScore(testVersionId, userId);

        assertNotNull(score);
        assertTrue(score >= 0 && score <= 100);
        verify(versionRepository).findById(testVersionId);
    }

    @Test
    @DisplayName("计算评分 - 版本不存在")
    void testCalculateScoreNotFound() {
        when(versionRepository.findById(testVersionId)).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> scoreService.calculateScore(testVersionId, userId));
    }

    @Test
    @DisplayName("计算评分 - 无权限访问")
    void testCalculateScoreUnauthorized() {
        when(versionRepository.findById(testVersionId)).thenReturn(Optional.of(testVersion));

        assertThrows(BusinessException.class, () -> scoreService.calculateScore(testVersionId, 999L));
    }

    @Test
    @DisplayName("计算评分 - 使用次数为零")
    void testCalculateScoreZeroUsage() {
        testVersion.setUsageCount(0);
        when(versionRepository.findById(testVersionId)).thenReturn(Optional.of(testVersion));

        Double score = scoreService.calculateScore(testVersionId, userId);

        assertNotNull(score);
        // 仅基础分 50 + 其他部分
        assertTrue(score >= 50);
    }

    // ================ recalculateAllScores 测试 ================

    @Test
    @DisplayName("重新计算评分 - 成功")
    void testRecalculateAllScoresSuccess() {
        ProductScriptVersion version2 = ProductScriptVersion.builder()
                .id(2L)
                .productId(testProductId)
                .versionNumber(2)
                .content("另一个话术")
                .style("幽默")
                .effectivenessScore(new BigDecimal("80.00"))
                .conversionRate(new BigDecimal("15.0"))
                .usageCount(100)
                .likesCount(200)
                .commentsCount(30)
                .ownerId(userId)
                .deleted(0)
                .build();

        List<ProductScriptVersion> versions = Arrays.asList(testVersion, version2);
        when(versionRepository.findAll(any(Specification.class))).thenReturn(versions);
        // calculateScore internally calls findById for each version
        when(versionRepository.findById(testVersionId)).thenReturn(Optional.of(testVersion));
        when(versionRepository.findById(2L)).thenReturn(Optional.of(version2));
        when(versionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(recordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Integer result = scoreService.recalculateAllScores(testProductId, userId);

        assertNotNull(result);
        assertEquals(2, result);
        verify(versionRepository, atLeastOnce()).findAll(any(Specification.class));
    }

    @Test
    @DisplayName("重新计算评分 - 没有版本")
    void testRecalculateAllScoresNoVersions() {
        when(versionRepository.findAll(any(Specification.class))).thenReturn(Collections.emptyList());

        Integer result = scoreService.recalculateAllScores(testProductId, userId);

        assertEquals(0, result);
    }

    // ================ getRanking 测试 ================

    @Test
    @DisplayName("获取排行榜 - 按评分排序")
    void testGetRankingByScore() {
        Page<ProductScriptVersion> page = new PageImpl<>(Collections.singletonList(testVersion));
        when(versionRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        PageResultVO<ScriptRankingVO> result = scoreService.getRanking(testProductId, null, "score", 0, 10, userId);

        assertNotNull(result);
        assertEquals(1, result.getList().size());
        assertEquals(1, result.getList().get(0).getRank());
    }

    @Test
    @DisplayName("获取排行榜 - 按使用次数排序")
    void testGetRankingByUsage() {
        Page<ProductScriptVersion> page = new PageImpl<>(Collections.singletonList(testVersion));
        when(versionRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        PageResultVO<ScriptRankingVO> result = scoreService.getRanking(testProductId, null, "usage", 0, 10, userId);

        assertNotNull(result);
        assertEquals(1, result.getList().size());
    }

    @Test
    @DisplayName("获取排行榜 - topN 限制")
    void testGetRankingWithTopN() {
        Page<ProductScriptVersion> page = new PageImpl<>(Collections.singletonList(testVersion));
        when(versionRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        PageResultVO<ScriptRankingVO> result = scoreService.getRanking(testProductId, 5, "score", 0, 10, userId);

        assertNotNull(result);
        assertTrue(result.getList().size() <= 5);
    }

    // ================ compareVersions 测试 ================

    @Test
    @DisplayName("对比版本 - 成功对比两个版本")
    void testCompareVersionsSuccess() {
        ProductScriptVersion version2 = ProductScriptVersion.builder()
                .id(2L)
                .productId(testProductId)
                .versionNumber(2)
                .content("另一个话术")
                .style("幽默")
                .effectivenessScore(new BigDecimal("80.00"))
                .usageCount(100)
                .ownerId(userId)
                .deleted(0)
                .build();

        when(versionRepository.findAllById(Arrays.asList(testVersionId, 2L)))
                .thenReturn(Arrays.asList(testVersion, version2));

        ScriptComparisonVO result = scoreService.compareVersions(Arrays.asList(testVersionId, 2L), userId);

        assertNotNull(result);
        assertEquals(2, result.getVersions().size());
        assertEquals("version_compare", result.getComparisonType());
        assertNotNull(result.getRecommendation());
    }

    @Test
    @DisplayName("对比版本 - 空版本列表")
    void testCompareVersionsEmpty() {
        assertThrows(BusinessException.class, () -> scoreService.compareVersions(null, userId));
        assertThrows(BusinessException.class, () -> scoreService.compareVersions(new ArrayList<>(), userId));
    }

    @Test
    @DisplayName("对比版本 - 超过最大数量")
    void testCompareVersionsTooMany() {
        List<Long> versionIds = Arrays.asList(1L, 2L, 3L, 4L, 5L, 6L);
        assertThrows(BusinessException.class, () -> scoreService.compareVersions(versionIds, userId));
    }

    @Test
    @DisplayName("对比版本 - 无权限")
    void testCompareVersionsUnauthorized() {
        ProductScriptVersion unauthorizedVersion = ProductScriptVersion.builder()
                .id(2L)
                .ownerId(999L) // 不同的 owner
                .build();

        when(versionRepository.findAllById(Arrays.asList(testVersionId, 2L)))
                .thenReturn(Arrays.asList(testVersion, unauthorizedVersion));

        assertThrows(BusinessException.class, () -> scoreService.compareVersions(Arrays.asList(testVersionId, 2L), userId));
    }

    // ================ getTrend 测试 ================

    @Test
    @DisplayName("获取趋势 - 成功")
    void testGetTrendSuccess() {
        ProductScriptEffectivenessRecord record1 = ProductScriptEffectivenessRecord.builder()
                .calculatedAt(LocalDateTime.now().minusDays(10))
                .scoreValue(new BigDecimal("70.00"))
                .scoreLevel("C")
                .usageCountSnapshot(40)
                .conversionRateSnapshot(new BigDecimal("10.0"))
                .likesSnapshot(80)
                .build();

        ProductScriptEffectivenessRecord record2 = ProductScriptEffectivenessRecord.builder()
                .calculatedAt(LocalDateTime.now())
                .scoreValue(new BigDecimal("75.00"))
                .scoreLevel("C")
                .usageCountSnapshot(50)
                .conversionRateSnapshot(new BigDecimal("12.5"))
                .likesSnapshot(100)
                .build();

        when(versionRepository.findById(testVersionId)).thenReturn(Optional.of(testVersion));
        when(recordRepository.findByScriptVersionIdOrderByCalculatedAtDesc(testVersionId))
                .thenReturn(Arrays.asList(record2, record1));

        ScriptTrendVO result = scoreService.getTrend(testVersionId, 30, userId);

        assertNotNull(result);
        assertNotNull(result.getTrendPoints());
        assertEquals("上升", result.getTrendDirection());
    }

    @Test
    @DisplayName("获取趋势 - 版本不存在")
    void testGetTrendNotFound() {
        when(versionRepository.findById(testVersionId)).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> scoreService.getTrend(testVersionId, 30, userId));
    }

    // ================ getStyleComparison 测试 ================

    @Test
    @DisplayName("风格对比 - 成功")
    void testGetStyleComparisonSuccess() throws Exception {
        ProductScriptVersion version2 = ProductScriptVersion.builder()
                .id(2L)
                .productId(testProductId)
                .style("幽默")
                .effectivenessScore(new BigDecimal("80.00"))
                .usageCount(60)
                .ownerId(userId)
                .deleted(0)
                .build();

        when(versionRepository.findAll(any(Specification.class)))
                .thenReturn(Arrays.asList(testVersion, version2));
        when(cacheRepository.findByProductIdAndComparisonTypeAndOwnerIdAndDeleted(
                testProductId, "style_compare", userId, 0))
                .thenReturn(Optional.empty());
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");
        when(cacheRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ScriptComparisonVO result = scoreService.getStyleComparison(testProductId, userId);

        assertNotNull(result);
        assertEquals("style_compare", result.getComparisonType());
    }

    // ================ recordSnapshot 测试 ================

    @Test
    @DisplayName("记录快照 - 成功")
    void testRecordSnapshotSuccess() {
        when(versionRepository.findById(testVersionId)).thenReturn(Optional.of(testVersion));
        when(recordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        boolean result = scoreService.recordSnapshot(testVersionId, userId);

        assertTrue(result);
        verify(recordRepository).save(any());
    }

    // ================ clearComparisonCache 测试 ================

    @Test
    @DisplayName("清除缓存 - 成功")
    void testClearComparisonCacheSuccess() {
        when(versionRepository.count(any(Specification.class))).thenReturn(1L);
        when(cacheRepository.deleteByProductId(testProductId)).thenReturn(3);

        Integer result = scoreService.clearComparisonCache(testProductId, userId);

        assertEquals(3, result);
        verify(cacheRepository).deleteByProductId(testProductId);
    }

    @Test
    @DisplayName("清除缓存 - 无权限")
    void testClearComparisonCacheUnauthorized() {
        when(versionRepository.count(any(Specification.class))).thenReturn(0L);

        assertThrows(BusinessException.class, () -> scoreService.clearComparisonCache(testProductId, userId));
    }

    // ================ getAnalysis 测试 ================

    @Test
    @DisplayName("获取分析 - 成功")
    void testGetAnalysisSuccess() {
        when(versionRepository.findById(testVersionId)).thenReturn(Optional.of(testVersion));
        when(recordRepository.findByScriptVersionIdOrderByCalculatedAtDesc(testVersionId))
                .thenReturn(Collections.emptyList());

        ScriptEffectivenessAnalysisVO result = scoreService.getAnalysis(testVersionId, userId);

        assertNotNull(result);
        assertNotNull(result.getCurrentScore());
    }

    // ================ determineScoreLevel 测试 ================

    @Test
    @DisplayName("判定评分等级 - A 级")
    void testDetermineScorelevelA() {
        assertEquals("A", scoreService.determineScoreLevel(95.0));
    }

    @Test
    @DisplayName("判定评分等级 - B 级")
    void testDetermineScorelevelB() {
        assertEquals("B", scoreService.determineScoreLevel(85.0));
    }

    @Test
    @DisplayName("判定评分等级 - C 级")
    void testDetermineScorelevelC() {
        assertEquals("C", scoreService.determineScoreLevel(75.0));
    }

    @Test
    @DisplayName("判定评分等级 - D 级")
    void testDetermineScorelevelD() {
        assertEquals("D", scoreService.determineScoreLevel(65.0));
    }

    @Test
    @DisplayName("判定评分等级 - F 级")
    void testDetermineScorelevelF() {
        assertEquals("F", scoreService.determineScoreLevel(50.0));
        assertEquals("F", scoreService.determineScoreLevel(null));
    }

    // ================ getRecommendation 测试 ================

    @Test
    @DisplayName("获取推荐 - A 级")
    void testGetRecommendationA() {
        String rec = scoreService.getRecommendation("A");
        assertNotNull(rec);
        assertTrue(rec.contains("优秀"));
    }

    @Test
    @DisplayName("获取推荐 - C 级")
    void testGetRecommendationC() {
        String rec = scoreService.getRecommendation("C");
        assertNotNull(rec);
        assertTrue(rec.contains("优化"));
    }

    @Test
    @DisplayName("获取推荐 - F 级")
    void testGetRecommendationF() {
        String rec = scoreService.getRecommendation("F");
        assertNotNull(rec);
        assertTrue(rec.contains("重新创建"));
    }

    // ================ 数据隔离测试 ================

    @Test
    @DisplayName("数据隔离 - calculateScore 强制验证 owner_id")
    void testDataIsolationCalculateScore() {
        when(versionRepository.findById(testVersionId)).thenReturn(Optional.of(testVersion));

        // 使用不同的 userId 应该抛出异常
        assertThrows(BusinessException.class, () -> scoreService.calculateScore(testVersionId, 999L));
    }

    @Test
    @DisplayName("数据隔离 - getTrend 强制验证 owner_id")
    void testDataIsolationGetTrend() {
        when(versionRepository.findById(testVersionId)).thenReturn(Optional.of(testVersion));

        // 使用不同的 userId 应该抛出异常
        assertThrows(BusinessException.class, () -> scoreService.getTrend(testVersionId, 30, 999L));
    }
}
