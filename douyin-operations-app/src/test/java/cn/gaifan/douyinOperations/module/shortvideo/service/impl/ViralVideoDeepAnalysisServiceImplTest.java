package cn.gaifan.douyinOperations.module.shortvideo.service.impl;

import cn.gaifan.douyinOperations.module.shortvideo.entity.SvViralVideo;
import cn.gaifan.douyinOperations.module.shortvideo.repository.SvViralVideoRepository;
import cn.gaifan.douyinOperations.module.shortvideo.service.ViralVideoService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ViralVideoDeepAnalysisServiceImpl 测试")
class ViralVideoDeepAnalysisServiceImplTest {

    @Mock
    private SvViralVideoRepository viralVideoRepository;
    @Mock
    private ViralVideoService viralVideoService;
    @Mock
    private ViralDeepAnalyzeAsyncRunner deepAnalyzeAsyncRunner;
    @Mock
    private ViralDeepAnalyzeExecutor viralDeepAnalyzeExecutor;

    @InjectMocks
    private ViralVideoDeepAnalysisServiceImpl service;

    @Test
    @DisplayName("getDeepAnalyzeStatus 应显式返回 evidence 元数据与展示标签")
    void getDeepAnalyzeStatus_shouldExposeEvidenceSummary() {
        SvViralVideo viral = new SvViralVideo();
        viral.setId(11L);
        viral.setDeepAnalyzeStatus("completed");
        viral.setTranscript("【推演口播】先抛问题，再给方案");
        viral.setSceneDescriptions("【推演场景】\n[0-3s] 镜前特写");
        viral.setDeepAnalysisResult("""
                {
                  "evidenceLevel": "inferred",
                  "evidenceDetails": {
                    "overallLevel": "inferred",
                    "transcriptLevel": "inferred",
                    "sceneLevel": "inferred",
                    "commentLevel": "missing",
                    "hasCommentSamples": false
                  }
                }
                """);

        when(viralVideoService.getViralVideo(11L, 9L)).thenReturn(viral);

        Map<String, Object> result = service.getDeepAnalyzeStatus(11L, 9L);

        assertThat(result.get("evidenceLevel")).isEqualTo("inferred");
        assertThat(result.get("transcriptEvidenceLevel")).isEqualTo("inferred");
        assertThat(result.get("sceneEvidenceLevel")).isEqualTo("inferred");
        assertThat(result.get("commentEvidenceLevel")).isEqualTo("missing");
        assertThat(result.get("transcriptDisplayLabel")).isEqualTo("推演口播稿（非 ASR 实录）");
        assertThat(result.get("sceneDisplayLabel")).isEqualTo("推演场景（非真实抽帧）");
        assertThat(result.get("inferenceRisk")).isEqualTo(true);
        assertThat(result.get("evidenceDetails")).isEqualTo(Map.of(
                "overallLevel", "inferred",
                "transcriptLevel", "inferred",
                "sceneLevel", "inferred",
                "commentLevel", "missing",
                "hasCommentSamples", false
        ));
    }
}
