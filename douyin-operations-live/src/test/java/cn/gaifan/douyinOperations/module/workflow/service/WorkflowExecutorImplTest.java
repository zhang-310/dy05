package cn.gaifan.douyinOperations.module.workflow.service;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.module.live.service.LiveAiService;
import cn.gaifan.douyinOperations.module.live.service.LiveScriptService;
import cn.gaifan.douyinOperations.module.live.vo.LiveAiFullResultVO;
import cn.gaifan.douyinOperations.module.live.vo.LiveAiGenerateVO;
import cn.gaifan.douyinOperations.module.live.vo.LiveScriptVO;
import cn.gaifan.douyinOperations.module.workflow.entity.WorkflowDefinition;
import cn.gaifan.douyinOperations.module.workflow.entity.WorkflowStep;
import cn.gaifan.douyinOperations.module.workflow.repository.WorkflowDefinitionRepository;
import cn.gaifan.douyinOperations.module.workflow.repository.WorkflowStepRepository;
import cn.gaifan.douyinOperations.module.workflow.service.impl.WorkflowExecutorImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("WorkflowExecutorImpl 单元测试")
class WorkflowExecutorImplTest {

    @Mock
    private WorkflowDefinitionRepository definitionRepository;
    @Mock
    private WorkflowStepRepository stepRepository;
    @Mock
    private LiveAiService liveAiService;
    @Mock
    private LiveScriptService liveScriptService;

    private WorkflowExecutorImpl workflowExecutor;

    @BeforeEach
    void setUp() {
        workflowExecutor = new WorkflowExecutorImpl();
        ReflectionTestUtils.setField(workflowExecutor, "definitionRepository", definitionRepository);
        ReflectionTestUtils.setField(workflowExecutor, "stepRepository", stepRepository);
        ReflectionTestUtils.setField(workflowExecutor, "liveAiService", liveAiService);
        ReflectionTestUtils.setField(workflowExecutor, "liveScriptService", liveScriptService);
    }

    @Test
    void execute_shouldRejectBlankWorkflowCode() {
        assertThatThrownBy(() -> workflowExecutor.execute("  ", Map.of()))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo(ErrorCode.VALIDATION_FAIL));
    }

    @Test
    void execute_shouldRejectMissingWorkflowDefinition() {
        when(definitionRepository.findByWorkflowCodeAndDeleted("missing", 0)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> workflowExecutor.execute("missing", Map.of("userId", 1L, "sessionId", 2L)))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo(ErrorCode.DATA_NOT_FOUND));
    }

    @Test
    void execute_shouldRunGenerateAndSaveStepsSuccessfully() {
        WorkflowDefinition definition = new WorkflowDefinition();
        definition.setId(10L);
        definition.setWorkflowCode("live_script_full");

        WorkflowStep generate = new WorkflowStep();
        generate.setDefinitionId(10L);
        generate.setStepCode("generate");
        generate.setSequenceNo(1);

        WorkflowStep save = new WorkflowStep();
        save.setDefinitionId(10L);
        save.setStepCode("save");
        save.setSequenceNo(2);

        LiveAiFullResultVO fullResult = new LiveAiFullResultVO();
        fullResult.setScriptIdsForAttribution(List.of(101L, 102L));
        fullResult.setResults(List.of());

        when(definitionRepository.findByWorkflowCodeAndDeleted("live_script_full", 0))
                .thenReturn(Optional.of(definition));
        when(stepRepository.findByDefinitionIdAndDeletedOrderBySequenceNoAsc(10L, 0))
                .thenReturn(List.of(generate, save));
        when(liveAiService.generateFull(org.mockito.ArgumentMatchers.any(LiveAiGenerateVO.class)))
                .thenReturn(fullResult);
        when(liveScriptService.getBySessionId(anyLong(), anyLong()))
                .thenReturn(List.of(new LiveScriptVO(), new LiveScriptVO()));

        WorkflowExecutor.WorkflowExecuteResult result = workflowExecutor.execute(
                "live_script_full",
                Map.of("userId", 1L, "sessionId", 2L, "personaId", 3L, "modelId", 4L)
        );

        assertThat(result.success()).isTrue();
        assertThat(result.failedStep()).isNull();
        assertThat(result.outputs()).containsKeys("generate", "save");
        assertThat(result.outputs().get("save")).isEqualTo(Map.of("count", 2));

        ArgumentCaptor<LiveAiGenerateVO> captor = ArgumentCaptor.forClass(LiveAiGenerateVO.class);
        verify(liveAiService).generateFull(captor.capture());
        assertThat(captor.getValue().getSessionId()).isEqualTo(2L);
        assertThat(captor.getValue().getPersonaId()).isEqualTo(3L);
        assertThat(captor.getValue().getModelId()).isEqualTo(4L);

        verify(liveScriptService).ensureScriptSlotsForSession(2L, 1L);
        verify(liveScriptService).getBySessionId(2L, 1L);
    }
}
