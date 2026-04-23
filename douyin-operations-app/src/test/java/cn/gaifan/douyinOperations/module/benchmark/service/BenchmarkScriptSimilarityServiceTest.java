package cn.gaifan.douyinOperations.module.benchmark.service;

import cn.gaifan.douyinOperations.module.benchmark.entity.BenchmarkQualityScript;
import cn.gaifan.douyinOperations.module.benchmark.repository.BenchmarkQualityScriptRepository;
import cn.gaifan.douyinOperations.module.benchmark.service.impl.BenchmarkScriptSimilarityServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BenchmarkScriptSimilarityServiceTest {

    @Mock
    private BenchmarkQualityScriptRepository qualityScriptRepository;

    @InjectMocks
    private BenchmarkScriptSimilarityServiceImpl similarityService;

    private BenchmarkQualityScript script1;
    private Long ownerId;

    @BeforeEach
    void setUp() {
        ownerId = 1L;

        script1 = new BenchmarkQualityScript();
        script1.setId(100L);
        script1.setOwnerId(ownerId);
        script1.setVideoId(1000L);
        script1.setAnalysisId(2000L);
        script1.setScriptContent("这是一个关于护肤品的直播脚本");
        script1.setScriptType("产品介绍");
        script1.setIndustry("护肤");
        script1.setSceneType("直播");
        script1.setQualityScore(BigDecimal.valueOf(85.0));
    }

    @Test
    void testGenerateEmbedding() {
        // Given
        when(qualityScriptRepository.findById(100L)).thenReturn(Optional.of(script1));
        when(qualityScriptRepository.save(any(BenchmarkQualityScript.class))).thenReturn(script1);

        // When
        similarityService.generateEmbedding(100L, ownerId);

        // Then
        verify(qualityScriptRepository).save(any(BenchmarkQualityScript.class));
    }
}
