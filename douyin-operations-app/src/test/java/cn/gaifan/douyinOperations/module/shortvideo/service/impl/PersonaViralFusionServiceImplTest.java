package cn.gaifan.douyinOperations.module.shortvideo.service.impl;

import cn.gaifan.douyinOperations.module.ai.entity.AiModel;
import cn.gaifan.douyinOperations.module.ai.repository.AiModelRepository;
import cn.gaifan.douyinOperations.module.ai.service.LlmClient;
import cn.gaifan.douyinOperations.module.douyin.entity.DyPersona;
import cn.gaifan.douyinOperations.module.douyin.repository.DyPersonaRepository;
import cn.gaifan.douyinOperations.module.script.service.ViolationWordService;
import cn.gaifan.douyinOperations.module.shortvideo.entity.SvViralVideo;
import cn.gaifan.douyinOperations.module.shortvideo.repository.SvHotTopicRepository;
import cn.gaifan.douyinOperations.module.shortvideo.service.HotspotWindowService;
import cn.gaifan.douyinOperations.module.shortvideo.service.ViralVideoService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PersonaViralFusionServiceImpl 测试")
class PersonaViralFusionServiceImplTest {

    @Mock
    private SvHotTopicRepository hotTopicRepository;
    @Mock
    private DyPersonaRepository personaRepository;
    @Mock
    private AiModelRepository aiModelRepository;
    @Mock
    private ViralVideoService viralVideoService;
    @Mock
    private LlmClient llmClient;
    @Mock
    private HotspotWindowService hotspotWindowService;
    @Mock
    private ViolationWordService violationWordService;

    @InjectMocks
    private PersonaViralFusionServiceImpl service;

    @Test
    @DisplayName("generatePersonaFusedScript 应在推演素材上写明非 ASR 与非真实抽帧")
    void generatePersonaFusedScript_shouldLabelInferredEvidenceInPrompt() {
        ReflectionTestUtils.setField(service, "llmClient", llmClient);
        ReflectionTestUtils.setField(service, "violationWordService", violationWordService);
        SvViralVideo viral = buildInferredViral();
        DyPersona persona = buildPersona();
        AiModel model = buildModel();

        when(viralVideoService.getViralVideo(11L, 9L)).thenReturn(viral);
        when(personaRepository.findByIdAndDeleted(3L, 0)).thenReturn(Optional.of(persona));
        when(aiModelRepository.findByStatusAndDeleted(1, 0)).thenReturn(List.of(model));
        when(llmClient.chat(eq(model), anyString(), anyString()))
                .thenReturn(new LlmClient.LlmResponse("{\"title\":\"新脚本\"}", 128, true, null));

        Map<String, Object> result = service.generatePersonaFusedScript(11L, 3L, "form_imitation", 9L);

        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(llmClient).chat(eq(model), anyString(), promptCaptor.capture());
        String prompt = promptCaptor.getValue();

        assertThat(result.get("script")).isEqualTo("{\"title\":\"新脚本\"}");
        assertThat(result.get("usedVariableTable")).isEqualTo(true);
        assertThat(prompt).contains("【证据口径】");
        assertThat(prompt).contains("- 总体=推演");
        assertThat(prompt).contains("【原视频口播文案（推演稿，非 ASR 实录）】");
        assertThat(prompt).contains("【原视频场景描述（推演场景，非真实抽帧）】");
        assertThat(prompt).contains("不得伪称来自真实 ASR / 真实抽帧");
        assertThat(prompt).doesNotContain("【原视频口播文案（ASR 转写）】");
        assertThat(prompt).doesNotContain("【原视频场景描述（抽帧分析）】");
    }

    @Test
    @DisplayName("matchPersonas 应把推演证据口径注入匹配提示词")
    void matchPersonas_shouldInjectEvidenceContextIntoPrompt() {
        ReflectionTestUtils.setField(service, "llmClient", llmClient);
        SvViralVideo viral = buildInferredViral();
        DyPersona persona = buildPersona();
        AiModel model = buildModel();

        when(viralVideoService.getViralVideo(11L, 9L)).thenReturn(viral);
        when(personaRepository.findByOwnerIdAndDeleted(9L, 0)).thenReturn(List.of(persona));
        when(aiModelRepository.findByStatusAndDeleted(1, 0)).thenReturn(List.of(model));
        when(llmClient.chat(eq(model), anyString(), anyString()))
                .thenReturn(new LlmClient.LlmResponse("""
                        [
                          {"personaId": 3, "matchScore": 0.91, "matchReason": "调性吻合"}
                        ]
                        """, 96, true, null));

        List<Map<String, Object>> result = service.matchPersonas(11L, 9L);

        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(llmClient).chat(eq(model), anyString(), promptCaptor.capture());
        String prompt = promptCaptor.getValue();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).get("personaId")).isEqualTo(3L);
        assertThat(result.get(0).get("personaName")).isEqualTo("理性护肤师");
        assertThat(prompt).contains("【证据口径】");
        assertThat(prompt).contains("- 口播=推演（推演稿，非 ASR 实录）");
        assertThat(prompt).contains("注意：带“推演”标签的字段只能视为结构假设");
    }

    private SvViralVideo buildInferredViral() {
        SvViralVideo viral = new SvViralVideo();
        viral.setId(11L);
        viral.setTitle("熬夜修护爆款");
        viral.setTranscript("【推演口播】先点出熬夜脸问题，再给修护方案");
        viral.setSceneDescriptions("【推演场景】\n[0-3s] 镜前特写 | 人物:博主 道具:精华 镜头:特写 情绪:焦虑");
        viral.setRemakeVariableTable("{\"mustKeep\":[\"前三秒钩子\"]}");
        viral.setDeepAnalysisResult("""
                {
                  "evidenceLevel": "inferred",
                  "evidenceDetails": {
                    "overallLevel": "inferred",
                    "transcriptLevel": "inferred",
                    "sceneLevel": "inferred",
                    "commentLevel": "missing",
                    "hasCommentSamples": false
                  },
                  "viralHypotheses": {
                    "emotionTrigger": "熬夜焦虑"
                  },
                  "remakeVariableTable": {
                    "mustKeep": ["前三秒钩子"]
                  },
                  "structure": {
                    "hook": "先抛问题"
                  }
                }
                """);
        return viral;
    }

    private DyPersona buildPersona() {
        DyPersona persona = new DyPersona();
        persona.setId(3L);
        persona.setOwnerId(9L);
        persona.setPersonaName("理性护肤师");
        persona.setDescription("擅长把复杂护肤问题讲清楚");
        persona.setTone("专业克制");
        return persona;
    }

    private AiModel buildModel() {
        AiModel model = new AiModel();
        model.setId(1L);
        model.setModelName("test-model");
        model.setModelProvider("openai");
        model.setModelVersion("gpt-test");
        model.setStatus(1);
        model.setDeleted(0);
        model.setTemperature(new BigDecimal("0.70"));
        model.setMaxTokens(2048);
        return model;
    }
}
