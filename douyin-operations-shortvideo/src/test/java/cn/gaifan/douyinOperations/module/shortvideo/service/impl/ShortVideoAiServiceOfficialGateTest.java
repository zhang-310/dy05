package cn.gaifan.douyinOperations.module.shortvideo.service.impl;

import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.module.ai.entity.AiModel;
import cn.gaifan.douyinOperations.module.ai.repository.AiModelRepository;
import cn.gaifan.douyinOperations.module.ai.repository.AiTaskModelConfigRepository;
import cn.gaifan.douyinOperations.module.ai.service.LlmClient;
import cn.gaifan.douyinOperations.module.ai.service.OperationalStrategyKnowledgeService;
import cn.gaifan.douyinOperations.module.douyin.repository.DyPersonaRepository;
import cn.gaifan.douyinOperations.module.shortvideo.repository.SvViralVideoRepository;
import cn.gaifan.douyinOperations.module.shortvideo.vo.AiCopyGenerateVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShortVideoAiServiceOfficialGateTest {

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
    @Mock
    private OperationalStrategyKnowledgeService operationalStrategyKnowledgeService;

    @InjectMocks
    private ShortVideoAiServiceImpl service;

    @Test
    @DisplayName("短视频生成必须同时命中 douyin 官方学习和 douyin_weigui 违规规则")
    void generateCopy_shouldRequireOfficialAndViolationReferences() {
        mockModel();
        when(operationalStrategyKnowledgeService.buildShortVideoGenerationContext(any(), any(), anyInt()))
                .thenReturn(new OperationalStrategyKnowledgeService.PromptContext(
                        "<douyin_ops_learning_context>官方规则</douyin_ops_learning_context>",
                        List.of(),
                        List.of(
                                new OperationalStrategyKnowledgeService.OfficialReference(
                                        "douyin", "official_learning", 1L, 11L, "官方学习", "官方学习内容", 0.92),
                                new OperationalStrategyKnowledgeService.OfficialReference(
                                        "douyin_weigui", "violation_rule", 2L, 22L, "违规规则", "违规规则内容", 0.91)
                        )));
        when(llmClient.chatWithFallback(any(), any(), any()))
                .thenReturn(new LlmClient.LlmResponse("生成结果", 100, true, null));

        AiCopyGenerateVO vo = new AiCopyGenerateVO();
        vo.setTopic("护肤");
        vo.setStyle("professional");

        assertThat(service.generateCopy(vo, 7L)).isEqualTo("生成结果");
    }

    @Test
    @DisplayName("短视频生成缺 douyin_weigui 引用时直接阻断")
    void generateCopy_shouldBlockWhenViolationReferenceMissing() {
        mockModel();
        when(operationalStrategyKnowledgeService.buildShortVideoGenerationContext(any(), any(), anyInt()))
                .thenReturn(new OperationalStrategyKnowledgeService.PromptContext(
                        "<douyin_ops_learning_context>只有官方学习</douyin_ops_learning_context>",
                        List.of(),
                        List.of(new OperationalStrategyKnowledgeService.OfficialReference(
                                "douyin", "official_learning", 1L, 11L, "官方学习", "官方学习内容", 0.92))));
        when(llmClient.chatWithFallback(any(), any(), any()))
                .thenReturn(new LlmClient.LlmResponse("生成结果", 100, true, null));

        AiCopyGenerateVO vo = new AiCopyGenerateVO();
        vo.setTopic("护肤");
        vo.setStyle("professional");

        assertThatThrownBy(() -> service.generateCopy(vo, 7L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("官方规则引用门禁未通过");
    }

    private void mockModel() {
        AiModel model = new AiModel();
        model.setId(1L);
        model.setModelName("test-model");
        model.setModelProvider("openai");
        model.setModelVersion("gpt-test");
        model.setStatus(1);
        model.setDeleted(0);
        model.setTemperature(new BigDecimal("0.70"));
        model.setMaxTokens(2048);
        when(taskModelConfigRepository.findByTaskCodeAndStatusAndDeleted(eq("short_video_script"), eq(1), eq(0)))
                .thenReturn(Optional.empty());
        when(modelRepository.findByStatusAndDeleted(1, 0)).thenReturn(List.of(model));
    }
}
