package cn.gaifan.douyinOperations.module.benchmark.service;

import cn.gaifan.douyinOperations.module.benchmark.entity.BenchmarkAnalysis;
import cn.gaifan.douyinOperations.module.benchmark.entity.BenchmarkQualityScript;
import cn.gaifan.douyinOperations.module.benchmark.entity.BenchmarkVideo;
import cn.gaifan.douyinOperations.module.benchmark.repository.BenchmarkAnalysisRepository;
import cn.gaifan.douyinOperations.module.benchmark.repository.BenchmarkQualityScriptRepository;
import cn.gaifan.douyinOperations.module.benchmark.repository.BenchmarkVideoRepository;
import cn.gaifan.douyinOperations.module.benchmark.vo.BenchmarkQualityScriptVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class BenchmarkScriptAutoIngestServiceIntegrationTest {

    @Autowired
    private BenchmarkScriptAutoIngestService autoIngestService;

    @Autowired
    private BenchmarkVideoRepository videoRepository;

    @Autowired
    private BenchmarkAnalysisRepository analysisRepository;

    @Autowired
    private BenchmarkQualityScriptRepository qualityScriptRepository;

    private BenchmarkVideo video;
    private BenchmarkAnalysis analysis;
    private Long ownerId;

    @BeforeEach
    void setUp() {
        ownerId = 1L;
        autoIngestService.setIngestThreshold(BigDecimal.valueOf(70.0));

        // 创建测试视频
        video = new BenchmarkVideo();
        video.setOwnerId(ownerId);
        video.setBenchmarkAccountId(1001L);
        video.setVideoId("test_video_123");
        video.setLikeCount(5000);
        video.setCommentCount(500);
        video.setShareCount(200);
        video.setFavoriteCount(300);
        video.setViewCount(50000L);
        video.setDuration(60);
        video.setDeleted(0);
        video = videoRepository.save(video);

        // 创建测试分析结果
        analysis = new BenchmarkAnalysis();
        analysis.setOwnerId(ownerId);
        analysis.setBenchmarkVideoId(video.getId());
        analysis.setMergedContent("这是一个高质量的直播脚本内容");
        analysis.setTranscriptText("语音识别文本");
        analysis.setApiDescription("API 获取的描述");
        analysis.setScriptBreakdown("{\"type\":\"product_intro\"}");
        analysis.setCreativeType("产品介绍");
        analysis.setHookStrategy("痛点切入");
        analysis.setContentStructure("三段式");
        analysis.setDeleted(0);
        analysis = analysisRepository.save(analysis);
    }

    @Test
    void testAutoIngestAfterAnalysis_Success() {
        autoIngestService.setIngestThreshold(BigDecimal.ZERO);

        // When
        BenchmarkQualityScriptVO result = autoIngestService.autoIngestAfterAnalysis(analysis, ownerId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getVideoId()).isEqualTo(video.getId());
        assertThat(result.getAnalysisId()).isEqualTo(analysis.getId());
        assertThat(result.getScriptContent()).isNotEmpty();
        assertThat(result.getQualityScore()).isGreaterThan(BigDecimal.ZERO);

        // 验证数据库中已保存
        Optional<BenchmarkQualityScript> saved = qualityScriptRepository.findById(result.getId());
        assertThat(saved).isPresent();
    }

    @Test
    void testAutoIngestAfterAnalysis_LowQuality() {
        // Given - 低质量视频
        BenchmarkVideo lowQualityVideo = new BenchmarkVideo();
        lowQualityVideo.setOwnerId(ownerId);
        lowQualityVideo.setBenchmarkAccountId(1002L);
        lowQualityVideo.setVideoId("low_quality_video");
        lowQualityVideo.setLikeCount(10);
        lowQualityVideo.setCommentCount(1);
        lowQualityVideo.setShareCount(0);
        lowQualityVideo.setFavoriteCount(0);
        lowQualityVideo.setViewCount(100L);
        lowQualityVideo.setDuration(60);
        lowQualityVideo.setDeleted(0);
        lowQualityVideo = videoRepository.save(lowQualityVideo);

        BenchmarkAnalysis lowQualityAnalysis = new BenchmarkAnalysis();
        lowQualityAnalysis.setOwnerId(ownerId);
        lowQualityAnalysis.setBenchmarkVideoId(lowQualityVideo.getId());
        lowQualityAnalysis.setMergedContent("低质量脚本");
        lowQualityAnalysis.setDeleted(0);
        lowQualityAnalysis = analysisRepository.save(lowQualityAnalysis);

        // When
        BenchmarkQualityScriptVO result = autoIngestService.autoIngestAfterAnalysis(lowQualityAnalysis, ownerId);

        // Then
        assertThat(result).isNull();
    }

    @Test
    void testBatchAutoIngest() {
        autoIngestService.setIngestThreshold(BigDecimal.ZERO);

        // Given
        BenchmarkAnalysis analysis2 = new BenchmarkAnalysis();
        analysis2.setOwnerId(ownerId);
        analysis2.setBenchmarkVideoId(video.getId());
        analysis2.setMergedContent("另一个高质量脚本");
        analysis2.setDeleted(0);
        analysis2 = analysisRepository.save(analysis2);

        // When
        Integer successCount = autoIngestService.batchAutoIngest(
                java.util.Arrays.asList(analysis.getId(), analysis2.getId()), ownerId);

        // Then
        assertThat(successCount).isEqualTo(1);
        assertThat(qualityScriptRepository.findByAnalysisIdAndDeleted(analysis.getId(), 0)).isPresent();
        assertThat(qualityScriptRepository.findByAnalysisIdAndDeleted(analysis2.getId(), 0)).isNotPresent();
    }

    @Test
    void testThresholdConfiguration() {
        // When
        BigDecimal defaultThreshold = autoIngestService.getIngestThreshold();
        autoIngestService.setIngestThreshold(BigDecimal.valueOf(85.0));
        BigDecimal newThreshold = autoIngestService.getIngestThreshold();

        // Then
        assertThat(defaultThreshold).isEqualTo(BigDecimal.valueOf(70.0));
        assertThat(newThreshold).isEqualTo(BigDecimal.valueOf(85.0));
    }
}
