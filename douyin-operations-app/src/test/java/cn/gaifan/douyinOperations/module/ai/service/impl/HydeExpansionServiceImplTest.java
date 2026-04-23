package cn.gaifan.douyinOperations.module.ai.service.impl;

import cn.gaifan.douyinOperations.module.ai.entity.AiModel;
import cn.gaifan.douyinOperations.module.ai.entity.AiTaskModelConfig;
import cn.gaifan.douyinOperations.module.ai.repository.AiModelRepository;
import cn.gaifan.douyinOperations.module.ai.repository.AiTaskModelConfigRepository;
import cn.gaifan.douyinOperations.module.ai.service.LlmClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
@DisplayName("HydeExpansionServiceImpl")
class HydeExpansionServiceImplTest {

    @InjectMocks
    private HydeExpansionServiceImpl hydeExpansionService;

    @Mock
    private AiModelRepository modelRepository;

    @Mock
    private AiTaskModelConfigRepository taskModelConfigRepository;

    @Mock
    private LlmClient llmClient;

    @BeforeEach
    void setUp() {
        org.springframework.test.util.ReflectionTestUtils.setField(hydeExpansionService, "enabled", true);
        org.springframework.test.util.ReflectionTestUtils.setField(hydeExpansionService, "minQueryLength", 6);
        org.springframework.test.util.ReflectionTestUtils.setField(hydeExpansionService, "maxQueryChars", 800);
        org.springframework.test.util.ReflectionTestUtils.setField(hydeExpansionService, "maxOutputChars", 480);
    }

    @Test
    void disabled_returnsEmpty() {
        org.springframework.test.util.ReflectionTestUtils.setField(hydeExpansionService, "enabled", false);
        assertThat(hydeExpansionService.expandHypotheticalPassage("足够长的查询内容")).isEmpty();
        verify(llmClient, never()).chatWithFallback(any(), any(), any());
    }

    @Test
    void shortQuery_returnsEmpty() {
        assertThat(hydeExpansionService.expandHypotheticalPassage("短")).isEmpty();
        verify(llmClient, never()).chatWithFallback(any(), any(), any());
    }

    @Test
    void kbHydeModel_success_returnsPassage() {
        AiModel m = new AiModel();
        m.setId(1L);
        m.setStatus(1);
        m.setDeleted(0);
        AiTaskModelConfig tc = new AiTaskModelConfig();
        tc.setPrimaryModelId(1L);
        when(taskModelConfigRepository.findByTaskCodeAndStatusAndDeleted("kb_hyde", 1, 0)).thenReturn(Optional.of(tc));
        when(modelRepository.findById(1L)).thenReturn(Optional.of(m));
        when(llmClient.chatWithFallback(anyList(), anyString(), anyString()))
                .thenReturn(new LlmClient.LlmResponse("玻尿酸是一种保湿成分，常用于护肤品配方中。", 10, true, null));

        var r = hydeExpansionService.expandHypotheticalPassage("玻尿酸护肤品成分功效说明");
        assertThat(r).isPresent();
        assertThat(r.get()).contains("玻尿酸");
        verify(taskModelConfigRepository).findByTaskCodeAndStatusAndDeleted("kb_hyde", 1, 0);
    }

    @Test
    void fallsBackToQueryRewrite_whenKbHydeMissing() {
        AiModel m = new AiModel();
        m.setId(2L);
        m.setStatus(1);
        m.setDeleted(0);
        AiTaskModelConfig tc = new AiTaskModelConfig();
        tc.setPrimaryModelId(2L);
        when(taskModelConfigRepository.findByTaskCodeAndStatusAndDeleted("kb_hyde", 1, 0)).thenReturn(Optional.empty());
        when(taskModelConfigRepository.findByTaskCodeAndStatusAndDeleted("query_rewrite", 1, 0)).thenReturn(Optional.of(tc));
        when(modelRepository.findById(2L)).thenReturn(Optional.of(m));
        when(llmClient.chatWithFallback(anyList(), anyString(), anyString()))
                .thenReturn(new LlmClient.LlmResponse("假设文档正文足够十二字以上长度", 5, true, null));

        assertThat(hydeExpansionService.expandHypotheticalPassage("查询改写回退路径测试内容")).isPresent();
    }
}
