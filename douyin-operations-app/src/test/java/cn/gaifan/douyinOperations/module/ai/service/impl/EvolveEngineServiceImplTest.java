package cn.gaifan.douyinOperations.module.ai.service.impl;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.module.ai.entity.AiEvolveTask;
import cn.gaifan.douyinOperations.module.ai.repository.AiEvolveTaskRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("EvolveEngineServiceImpl 测试")
class EvolveEngineServiceImplTest {

    @Mock
    private AiEvolveTaskRepository taskRepository;

    @InjectMocks
    private EvolveEngineServiceImpl evolveEngineService;

    @Test
    @DisplayName("cancelTask 应将未结束任务标记为 canceled")
    void cancelTask_shouldMarkTaskCanceled() {
        AiEvolveTask task = new AiEvolveTask();
        task.setId(1L);
        task.setStatus("blocked");

        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));

        evolveEngineService.cancelTask(1L);

        assertThat(task.getStatus()).isEqualTo("canceled");
        assertThat(task.getErrorMessage()).isEqualTo("任务已被手动取消");
        verify(taskRepository).save(task);
    }

    @Test
    @DisplayName("cancelTask 不允许取消已结束任务")
    void cancelTask_shouldRejectTerminalTask() {
        AiEvolveTask task = new AiEvolveTask();
        task.setId(2L);
        task.setStatus("completed");

        when(taskRepository.findById(2L)).thenReturn(Optional.of(task));

        assertThatThrownBy(() -> evolveEngineService.cancelTask(2L))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo(ErrorCode.OPERATION_NOT_ALLOWED));
    }
}
