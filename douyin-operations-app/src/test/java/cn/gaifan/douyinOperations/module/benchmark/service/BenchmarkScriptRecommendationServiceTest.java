package cn.gaifan.douyinOperations.module.benchmark.service;

import cn.gaifan.douyinOperations.module.benchmark.entity.BenchmarkAnalysis;
import cn.gaifan.douyinOperations.module.benchmark.repository.BenchmarkAnalysisRepository;
import cn.gaifan.douyinOperations.module.benchmark.service.impl.BenchmarkScriptRecommendationServiceImpl;
import cn.gaifan.douyinOperations.module.benchmark.vo.BenchmarkScriptSimilarityVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BenchmarkScriptRecommendationServiceTest {

    @Mock
    private BenchmarkAnalysisRepository analysisRepository;

    @Mock
    private BenchmarkScriptSimilarityService similarityService;

    @InjectMocks
    private BenchmarkScriptRecommendationServiceImpl recommendationService;

    private BenchmarkAnalysis analysis;
    private Long ownerId;

    @BeforeEach
    void setUp() {
        ownerId = 1L;

        analysis = new BenchmarkAnalysis();
        analysis.setId(200L);
        analysis.setBenchmarkVideoId(1000L);
        analysis.setMergedContent("需要改进的脚本内容");
        analysis.setCreativeType("产品介绍");
    }

    @Test
    void testRecommendByRequirement() {
        // Given
        String requirement = "护肤品直播话术";
        BenchmarkScriptSimilarityVO similarityVO = new BenchmarkScriptSimilarityVO();
        similarityVO.setScriptId(100L);
        similarityVO.setIndustry("护肤");
        similarityVO.setQualityScore(BigDecimal.valueOf(90.0));

        when(similarityService.findSimilarScriptsByText(anyString(), eq(ownerId), anyInt(), anyDouble()))
                .thenReturn(Arrays.asList(similarityVO));

        // When
        List<BenchmarkScriptSimilarityVO> results = recommendationService.recommendByRequirement(requirement, ownerId, 10);

        // Then
        assertThat(results).isNotEmpty();
        assertThat(results.get(0).getIndustry()).isEqualTo("护肤");
    }

    @Test
    void testRecommendImprovementScripts() {
        // Given
        when(analysisRepository.findById(200L)).thenReturn(Optional.of(analysis));

        BenchmarkScriptSimilarityVO similarityVO = new BenchmarkScriptSimilarityVO();
        similarityVO.setScriptId(100L);
        similarityVO.setQualityScore(BigDecimal.valueOf(85.0));

        when(similarityService.findSimilarScriptsByText(anyString(), eq(ownerId), anyInt(), anyDouble()))
                .thenReturn(Arrays.asList(similarityVO));

        // When
        List<BenchmarkScriptSimilarityVO> results = recommendationService.recommendImprovementScripts(200L, ownerId, 5);

        // Then
        assertThat(results).isNotEmpty();
    }
}
