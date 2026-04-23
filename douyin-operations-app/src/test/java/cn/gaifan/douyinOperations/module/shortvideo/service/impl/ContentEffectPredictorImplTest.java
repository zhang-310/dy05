package cn.gaifan.douyinOperations.module.shortvideo.service.impl;

import cn.gaifan.douyinOperations.module.ai.entity.AiModel;
import cn.gaifan.douyinOperations.module.ai.repository.AiModelRepository;
import cn.gaifan.douyinOperations.module.ai.service.LlmClient;
import cn.gaifan.douyinOperations.module.shortvideo.entity.SvVideo;
import cn.gaifan.douyinOperations.module.shortvideo.repository.SvVideoRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ContentEffectPredictorImpl 测试")
class ContentEffectPredictorImplTest {

    @Mock
    private LlmClient llmClient;
    @Mock
    private AiModelRepository modelRepository;
    @Mock
    private SvVideoRepository svVideoRepository;

    @InjectMocks
    private ContentEffectPredictorImpl predictor;

    @Test
    @DisplayName("predict 应附带历史基线并输出校准区间")
    void predict_shouldApplyHistoricalBaseline() {
        ReflectionTestUtils.setField(predictor, "objectMapper", new ObjectMapper());
        ReflectionTestUtils.setField(predictor, "effectPredictMaxScriptChars", 500);
        ReflectionTestUtils.setField(predictor, "effectPredictModelLimit", 3);
        ReflectionTestUtils.setField(predictor, "effectPredictMaxOutputBullets", 6);
        ReflectionTestUtils.setField(predictor, "effectPredictCalibrationFactor", 1.0d);
        ReflectionTestUtils.setField(predictor, "effectPredictHistoryLookbackDays", 90);
        ReflectionTestUtils.setField(predictor, "effectPredictHistoryMinSamples", 3);
        ReflectionTestUtils.setField(predictor, "effectPredictHistoryBaselineWeight", 0.35d);

        AiModel model = new AiModel();
        model.setId(1L);
        model.setModelName("test-model");
        model.setModelProvider("openai");
        model.setModelVersion("gpt-test");
        model.setStatus(1);
        model.setDeleted(0);
        model.setTemperature(new BigDecimal("0.70"));
        model.setMaxTokens(2048);

        SvVideo v1 = buildVideo(8000L, 600, 100, 100);
        SvVideo v2 = buildVideo(10000L, 1000, 500, 500);
        SvVideo v3 = buildVideo(12000L, 1200, 600, 600);

        when(modelRepository.findByStatusAndDeleted(1, 0)).thenReturn(List.of(model));
        when(llmClient.chatWithFallback(any(), any(), any())).thenReturn(new LlmClient.LlmResponse("""
                {
                  "viewRange": {"min": 5000, "max": 20000},
                  "likeRange": {"min": 200, "max": 1000},
                  "engagementRate": "3-5%",
                  "confidence": 0.7,
                  "strengths": ["标题吸引力强"],
                  "weaknesses": ["缺少互动引导"],
                  "suggestions": ["在结尾增加互动提问"]
                }
                """, 120, true, null));
        when(svVideoRepository.findByOwnerIdAndPublishTimeAfterAndDeleted(eq(9L), any(Timestamp.class), eq(0)))
                .thenReturn(List.of(v1, v2, v3));

        Map<String, Object> result = predictor.predict(9L, "脚本文案", "护肤选题", "2026-04-12 20:00");

        assertThat(result.get("baselineApplied")).isEqualTo(true);
        assertThat(result.get("historicalBaseline")).isEqualTo(Map.of(
                "sampleSize", 3,
                "medianViews", 10000L,
                "medianEngagementRate", 20.0d
        ));
        assertThat(result.get("baselineAdjustedViewRange")).isEqualTo(Map.of(
                "min", 4125L,
                "max", 19125L
        ));
        assertThat(result.get("baselineAdjustedEngagementRate")).isEqualTo("9.6%");
    }

    private SvVideo buildVideo(long views, int likes, int comments, int shares) {
        SvVideo video = new SvVideo();
        video.setOwnerId(9L);
        video.setAccountId(1L);
        video.setPublishTime(Timestamp.from(Instant.now()));
        video.setViewCount(views);
        video.setLikeCount(likes);
        video.setCommentCount(comments);
        video.setShareCount(shares);
        return video;
    }
}
