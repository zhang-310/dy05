package cn.gaifan.douyinOperations.module.live.config;

import cn.gaifan.douyinOperations.module.live.service.CrossSessionLearningService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LiveCrossSessionDecaySchedulerTest {

    @Mock
    private CrossSessionLearningService crossSessionLearningService;

    @InjectMocks
    private LiveCrossSessionDecayScheduler scheduler;

    @Test
    void decayIneffectiveMemories_shouldSkipWhenDisabled() {
        ReflectionTestUtils.setField(scheduler, "schedulerEnabled", false);

        scheduler.decayIneffectiveMemories();

        verify(crossSessionLearningService, never()).decayIneffectiveMemories();
    }
}
