package cn.gaifan.douyinOperations.module.douyin.config;

import cn.gaifan.douyinOperations.module.douyin.service.DouyinScriptLearningService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DouyinLearningSchedulerTest {

    @Mock
    private DouyinScriptLearningService douyinScriptLearningService;

    @InjectMocks
    private DouyinLearningScheduler scheduler;

    @Test
    void scheduledLearning_shouldSkipWhenDisabled() {
        ReflectionTestUtils.setField(scheduler, "schedulerEnabled", false);

        scheduler.scheduledLearning();

        verify(douyinScriptLearningService, never()).runLearningPipeline();
    }
}
