package cn.gaifan.douyinOperations.module.shortvideo.service.impl;

import cn.gaifan.douyinOperations.module.ai.entity.AiModel;
import cn.gaifan.douyinOperations.module.ai.repository.AiModelRepository;
import cn.gaifan.douyinOperations.module.ai.repository.AiTaskModelConfigRepository;
import cn.gaifan.douyinOperations.module.ai.service.LlmClient;
import cn.gaifan.douyinOperations.module.douyin.repository.DyPersonaRepository;
import cn.gaifan.douyinOperations.module.shortvideo.entity.SvViralVideo;
import cn.gaifan.douyinOperations.module.shortvideo.repository.SvViralVideoRepository;
import cn.gaifan.douyinOperations.module.shortvideo.vo.AiCopyGenerateVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ShortVideoAiServiceImpl 测试")
class ShortVideoAiServiceImplTest {

    @Mock
    private LlmClient llmClient;
    @Mock
    private AiTaskModelConfigRepository taskModelConfigRepository;
    @Mock
    private AiModelRepository modelRepository;
    @Mock
    private SvViralVideoRepository viralVideoRepository;
    @Mock
    private DyPersonaRepository personaRepository;

    @InjectMocks
    private ShortVideoAiServiceImpl service;

    @Test
    @DisplayName("generateCopy 应优先注入结构化爆款深度分析结果")
    void generateCopy_shouldInjectStructuredViralContext() {
        AiCopyGenerateVO vo = new AiCopyGenerateVO();
        vo.setTopic("护肤");
        vo.setStyle("professional");
        vo.setViralId(11L);

        SvViralVideo viral = new SvViralVideo();
        viral.setId(11L);
        viral.setTitle("爆款护肤视频");
        viral.setViewCount(10000L);
        viral.setLikeCount(800L);
        viral.setCommentCount(120L);
        viral.setShareCount(35L);
        viral.setAnalysisResult("旧摘要");
        viral.setDeepAnalysisResult("""
                {
                  "evidenceLevel": "empirical",
                  "bestRemakeType": "dimensional_upgrade",
                  "transcript": {"fullText": "先说痛点，再给解决方案"},
                  "viralHypotheses": {
                    "emotionTrigger": "护肤焦虑",
                    "rhythmPattern": "快节奏问题-解决",
                    "replicability": "easy"
                  },
                  "remakeVariableTable": {
                    "mustKeep": ["前三秒反差钩子", "解决方案演示"],
                    "replaceable": [
                      {"variable": "人设", "suggestion": "替换为专业护肤顾问"},
                      {"variable": "产品", "suggestion": "替换为自有精华产品"}
                    ]
                  }
                }
                """);

        AiModel model = new AiModel();
        model.setId(1L);
        model.setModelName("test-model");
        model.setModelProvider("openai");
        model.setModelVersion("gpt-test");
        model.setStatus(1);
        model.setDeleted(0);
        model.setTemperature(new BigDecimal("0.70"));
        model.setMaxTokens(2048);

        when(viralVideoRepository.findById(11L)).thenReturn(Optional.of(viral));
        when(taskModelConfigRepository.findByTaskCodeAndStatusAndDeleted(eq("short_video_script"), eq(1), eq(0)))
                .thenReturn(Optional.empty());
        when(modelRepository.findByStatusAndDeleted(1, 0)).thenReturn(List.of(model));
        when(llmClient.chatWithFallback(any(), any(), any()))
                .thenReturn(new LlmClient.LlmResponse("生成结果", 100, true, null));

        String result = service.generateCopy(vo, 7L);

        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(llmClient).chatWithFallback(any(), any(), promptCaptor.capture());
        String prompt = promptCaptor.getValue();

        assertThat(result).isEqualTo("生成结果");
        assertThat(prompt).contains("证据等级：empirical");
        assertThat(prompt).contains("最佳二创方式：dimensional_upgrade");
        assertThat(prompt).contains("实证/推演口播稿：先说痛点，再给解决方案");
        assertThat(prompt).contains("必须保留元素：前三秒反差钩子；解决方案演示");
        assertThat(prompt).contains("可替换变量：人设→替换为专业护肤顾问；产品→替换为自有精华产品");
    }

    @Test
    @DisplayName("generateCopy 在缺少结构化分析时应保留推演标签")
    void generateCopy_shouldPreserveInferredFallbackLabels() {
        AiCopyGenerateVO vo = new AiCopyGenerateVO();
        vo.setTopic("护肤");
        vo.setStyle("professional");
        vo.setViralId(22L);

        SvViralVideo viral = new SvViralVideo();
        viral.setId(22L);
        viral.setTitle("封面推演护肤视频");
        viral.setViewCount(8000L);
        viral.setLikeCount(500L);
        viral.setCommentCount(60L);
        viral.setShareCount(18L);
        viral.setTranscript("【推演口播】先指出熬夜暗沉，再给出修护建议");
        viral.setSceneDescriptions("【推演场景】\n[0-3s] 镜前特写，展示肤色变化");
        viral.setAnalysisResult("旧摘要");
        viral.setDeepAnalysisResult(null);

        AiModel model = new AiModel();
        model.setId(1L);
        model.setModelName("test-model");
        model.setModelProvider("openai");
        model.setModelVersion("gpt-test");
        model.setStatus(1);
        model.setDeleted(0);
        model.setTemperature(new BigDecimal("0.70"));
        model.setMaxTokens(2048);

        when(viralVideoRepository.findById(22L)).thenReturn(Optional.of(viral));
        when(taskModelConfigRepository.findByTaskCodeAndStatusAndDeleted(eq("short_video_script"), eq(1), eq(0)))
                .thenReturn(Optional.empty());
        when(modelRepository.findByStatusAndDeleted(1, 0)).thenReturn(List.of(model));
        when(llmClient.chatWithFallback(any(), any(), any()))
                .thenReturn(new LlmClient.LlmResponse("生成结果", 100, true, null));

        service.generateCopy(vo, 7L);

        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(llmClient).chatWithFallback(any(), any(), promptCaptor.capture());
        String prompt = promptCaptor.getValue();

        assertThat(prompt).contains("推演口播稿（非 ASR 实录）：【推演口播】先指出熬夜暗沉，再给出修护建议");
        assertThat(prompt).contains("推演场景摘要（非真实抽帧）：【推演场景】 [0-3s] 镜前特写，展示肤色变化");
        assertThat(prompt).doesNotContain("转写文案：【推演口播】");
        assertThat(prompt).doesNotContain("场景摘要：【推演场景】");
    }
}
