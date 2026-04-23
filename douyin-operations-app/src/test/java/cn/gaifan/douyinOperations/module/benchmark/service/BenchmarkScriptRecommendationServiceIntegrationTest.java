package cn.gaifan.douyinOperations.module.benchmark.service;

import cn.gaifan.douyinOperations.module.benchmark.entity.BenchmarkAnalysis;
import cn.gaifan.douyinOperations.module.benchmark.entity.BenchmarkQualityScript;
import cn.gaifan.douyinOperations.module.benchmark.repository.BenchmarkAnalysisRepository;
import cn.gaifan.douyinOperations.module.benchmark.repository.BenchmarkQualityScriptRepository;
import cn.gaifan.douyinOperations.module.benchmark.vo.BenchmarkScriptSimilarityVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class BenchmarkScriptRecommendationServiceIntegrationTest {

    @Autowired
    private BenchmarkScriptRecommendationService recommendationService;

    @Autowired
    private BenchmarkQualityScriptRepository qualityScriptRepository;

    @Autowired
    private BenchmarkAnalysisRepository analysisRepository;

    @Autowired
    private BenchmarkScriptSimilarityService similarityService;

    private BenchmarkQualityScript script1;
    private BenchmarkQualityScript script2;
    private BenchmarkQualityScript script3;
    private BenchmarkAnalysis analysis;
    private Long ownerId;

    @BeforeEach
    void setUp() {
        ownerId = 1L;

        // 创建护肤品直播脚本
        script1 = new BenchmarkQualityScript();
        script1.setOwnerId(ownerId);
        script1.setVideoId(1000L);
        script1.setAnalysisId(2000L);
        script1.setScriptContent("护肤品直播话术");
        script1.setScriptType("产品介绍");
        script1.setIndustry("护肤");
        script1.setSceneType("直播");
        script1.setQualityScore(BigDecimal.valueOf(90.0));
        script1.setEngagementRate(BigDecimal.valueOf(15.0));
        script1.setViralScore(BigDecimal.valueOf(85.0));
        script1.setLikesCount(10000);
        script1.setCommentsCount(500);
        script1.setSharesCount(200);
        script1.setDeleted(0);
        script1 = qualityScriptRepository.save(script1);

        // 创建彩妆直播脚本
        script2 = new BenchmarkQualityScript();
        script2.setOwnerId(ownerId);
        script2.setVideoId(1001L);
        script2.setAnalysisId(2001L);
        script2.setScriptContent("护肤品直播话术");
        script2.setScriptType("产品介绍");
        script2.setIndustry("彩妆");
        script2.setSceneType("直播");
        script2.setQualityScore(BigDecimal.valueOf(85.0));
        script2.setEngagementRate(BigDecimal.valueOf(12.0));
        script2.setViralScore(BigDecimal.valueOf(80.0));
        script2.setLikesCount(8000);
        script2.setDeleted(0);
        script2 = qualityScriptRepository.save(script2);

        // 创建护肤品短视频脚本
        script3 = new BenchmarkQualityScript();
        script3.setOwnerId(ownerId);
        script3.setVideoId(1002L);
        script3.setAnalysisId(2002L);
        script3.setScriptContent("护肤品短视频脚本");
        script3.setScriptType("短视频");
        script3.setIndustry("护肤");
        script3.setSceneType("短视频");
        script3.setQualityScore(BigDecimal.valueOf(88.0));
        script3.setEngagementRate(BigDecimal.valueOf(14.0));
        script3.setViralScore(BigDecimal.valueOf(82.0));
        script3.setDeleted(0);
        script3 = qualityScriptRepository.save(script3);

        similarityService.batchGenerateEmbeddings(
                Arrays.asList(script1.getId(), script2.getId(), script3.getId()), ownerId);

        // 创建分析结果
        analysis = new BenchmarkAnalysis();
        analysis.setOwnerId(ownerId);
        analysis.setBenchmarkVideoId(1000L);
        analysis.setMergedContent("护肤品直播话术");
        analysis.setDeleted(0);
        analysis = analysisRepository.save(analysis);
    }

    @Test
    void testRecommendByRequirement() {
        // When
        List<BenchmarkScriptSimilarityVO> results = recommendationService.recommendByRequirement(
                "护肤品直播话术", ownerId, 10);

        // Then
        assertThat(results).isNotEmpty();
        assertThat(results).allMatch(vo -> vo.getIndustry() != null);
    }

    @Test
    void testRecommendByIndustryAndScene() {
        // When
        List<BenchmarkScriptSimilarityVO> results = recommendationService.recommendByIndustryAndScene(
                "护肤", "直播", ownerId, 10);

        // Then
        assertThat(results).isNotEmpty();
        assertThat(results).allMatch(vo -> "护肤".equals(vo.getIndustry()));
        assertThat(results).allMatch(vo -> "直播".equals(vo.getSceneType()));
    }

    @Test
    void testRecommendByScriptType() {
        // When
        List<BenchmarkScriptSimilarityVO> results = recommendationService.recommendByScriptType(
                "产品介绍", script1.getId(), ownerId, 10);

        // Then
        assertThat(results).isNotEmpty();
        assertThat(results).allMatch(vo -> "产品介绍".equals(vo.getScriptType()));
    }

    @Test
    void testSmartRecommend() {
        // When
        java.util.Map<String, Object> filters = new java.util.HashMap<>();
        filters.put("industry", "护肤");
        filters.put("minQualityScore", 80.0);

        List<BenchmarkScriptSimilarityVO> results = recommendationService.smartRecommend(
                filters, "护肤品直播话术", ownerId, 10);

        // Then
        assertThat(results).isNotEmpty();
        assertThat(results).allMatch(vo -> vo.getQualityScore().compareTo(BigDecimal.valueOf(80.0)) >= 0);
    }

    @Test
    void testGetPopularScripts() {
        // When
        List<BenchmarkScriptSimilarityVO> results = recommendationService.getPopularScripts(ownerId, 10);

        // Then
        assertThat(results).isNotEmpty();
        // 验证按热度排序
        if (results.size() > 1) {
            assertThat(results.get(0).getQualityScore())
                    .isGreaterThanOrEqualTo(results.get(results.size() - 1).getQualityScore());
        }
    }

    @Test
    void testGetLatestQualityScripts() {
        // When
        List<BenchmarkScriptSimilarityVO> results = recommendationService.getLatestQualityScripts(
                ownerId, 10, 80.0);

        // Then
        assertThat(results).isNotEmpty();
        assertThat(results).allMatch(vo -> vo.getQualityScore().compareTo(BigDecimal.valueOf(80.0)) >= 0);
    }

    @Test
    void testRecommendImprovementScripts() {
        // When
        List<BenchmarkScriptSimilarityVO> results = recommendationService.recommendImprovementScripts(
                analysis.getId(), ownerId, 5);

        // Then
        assertThat(results).isNotEmpty();
        assertThat(results).allMatch(vo -> vo.getQualityScore().compareTo(BigDecimal.valueOf(70.0)) > 0);
    }

    @Test
    void testRecommendationQuality() {
        // When
        List<BenchmarkScriptSimilarityVO> results = recommendationService.getPopularScripts(ownerId, 3);

        // Then
        assertThat(results).isNotEmpty();
        // 验证返回的都是高质量脚本
        assertThat(results).allMatch(vo -> vo.getQualityScore().compareTo(BigDecimal.valueOf(80.0)) >= 0);
        // 验证包含必要字段
        assertThat(results).allMatch(vo -> vo.getScriptId() != null);
        assertThat(results).allMatch(vo -> vo.getScriptContent() != null);
    }
}
