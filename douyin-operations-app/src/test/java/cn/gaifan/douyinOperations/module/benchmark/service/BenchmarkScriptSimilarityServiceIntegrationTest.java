package cn.gaifan.douyinOperations.module.benchmark.service;

import cn.gaifan.douyinOperations.module.benchmark.entity.BenchmarkQualityScript;
import cn.gaifan.douyinOperations.module.benchmark.repository.BenchmarkQualityScriptRepository;
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
class BenchmarkScriptSimilarityServiceIntegrationTest {

    @Autowired
    private BenchmarkScriptSimilarityService similarityService;

    @Autowired
    private BenchmarkQualityScriptRepository qualityScriptRepository;

    private BenchmarkQualityScript script1;
    private BenchmarkQualityScript script2;
    private Long ownerId;

    @BeforeEach
    void setUp() {
        ownerId = 1L;

        // 创建测试脚本 1
        script1 = new BenchmarkQualityScript();
        script1.setOwnerId(ownerId);
        script1.setVideoId(1000L);
        script1.setAnalysisId(2000L);
        script1.setScriptContent("护肤品直播话术");
        script1.setScriptType("产品介绍");
        script1.setIndustry("护肤");
        script1.setSceneType("直播");
        script1.setQualityScore(BigDecimal.valueOf(85.0));
        script1.setEngagementRate(BigDecimal.valueOf(12.5));
        script1.setViralScore(BigDecimal.valueOf(78.0));
        script1.setDeleted(0);
        script1 = qualityScriptRepository.save(script1);

        // 创建测试脚本 2
        script2 = new BenchmarkQualityScript();
        script2.setOwnerId(ownerId);
        script2.setVideoId(1001L);
        script2.setAnalysisId(2001L);
        script2.setScriptContent("护肤品直播话术");
        script2.setScriptType("产品介绍");
        script2.setIndustry("护肤");
        script2.setSceneType("直播");
        script2.setQualityScore(BigDecimal.valueOf(82.0));
        script2.setEngagementRate(BigDecimal.valueOf(11.0));
        script2.setViralScore(BigDecimal.valueOf(75.0));
        script2.setDeleted(0);
        script2 = qualityScriptRepository.save(script2);
    }

    @Test
    void testGenerateEmbedding() {
        // When
        similarityService.generateEmbedding(script1.getId(), ownerId);

        // Then
        BenchmarkQualityScript updated = qualityScriptRepository.findById(script1.getId()).orElseThrow();
        assertThat(updated.getEmbeddingVector()).isNotNull();
    }

    @Test
    void testBatchGenerateEmbeddings() {
        // When
        Integer successCount = similarityService.batchGenerateEmbeddings(
                Arrays.asList(script1.getId(), script2.getId()), ownerId);

        // Then
        assertThat(successCount).isEqualTo(2);

        BenchmarkQualityScript updated1 = qualityScriptRepository.findById(script1.getId()).orElseThrow();
        BenchmarkQualityScript updated2 = qualityScriptRepository.findById(script2.getId()).orElseThrow();
        assertThat(updated1.getEmbeddingVector()).isNotNull();
        assertThat(updated2.getEmbeddingVector()).isNotNull();
    }

    @Test
    void testIndexToMilvus() {
        // Given
        similarityService.generateEmbedding(script1.getId(), ownerId);

        // When
        similarityService.indexToMilvus(script1.getId(), ownerId);

        // Then - 验证索引成功（不抛出异常）
        assertThat(true).isTrue();
    }

    @Test
    void testFindSimilarScripts() {
        // Given
        similarityService.generateEmbedding(script1.getId(), ownerId);
        similarityService.generateEmbedding(script2.getId(), ownerId);

        // When
        List<cn.gaifan.douyinOperations.module.benchmark.vo.BenchmarkScriptSimilarityVO> results =
                similarityService.findSimilarScripts(script1.getId(), ownerId, 10, 0.0);

        // Then
        assertThat(results).isNotEmpty();
    }

    @Test
    void testFindSimilarScriptsByText() {
        // Given
        similarityService.generateEmbedding(script1.getId(), ownerId);
        similarityService.generateEmbedding(script2.getId(), ownerId);

        // When
        List<cn.gaifan.douyinOperations.module.benchmark.vo.BenchmarkScriptSimilarityVO> results =
                similarityService.findSimilarScriptsByText("护肤品直播话术", ownerId, 10, 0.0);

        // Then
        assertThat(results).isNotEmpty();
    }

    @Test
    void testGetUnembeddedScriptIds() {
        // When
        List<Long> unembeddedIds = similarityService.getUnembeddedScriptIds(ownerId, 100);

        // Then
        assertThat(unembeddedIds).contains(script1.getId(), script2.getId());
    }

    @Test
    void testGetUnindexedScriptIds() {
        similarityService.generateEmbedding(script1.getId(), ownerId);

        // When
        List<Long> unindexedIds = similarityService.getUnindexedScriptIds(ownerId, 100);

        // Then
        assertThat(unindexedIds).contains(script1.getId());
    }
}
