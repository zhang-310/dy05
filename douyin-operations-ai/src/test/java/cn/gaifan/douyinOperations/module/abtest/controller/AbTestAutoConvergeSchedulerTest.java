package cn.gaifan.douyinOperations.module.abtest.controller;

import cn.gaifan.douyinOperations.module.abtest.service.AbTestService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AbTestAutoConvergeSchedulerTest {

    @Mock
    private AbTestService abTestService;

    @InjectMocks
    private AbTestAutoConvergeScheduler scheduler;

    @Test
    void scheduledAutoConverge_shouldSkipWhenDisabled() {
        ReflectionTestUtils.setField(scheduler, "enabled", false);

        scheduler.scheduledAutoConverge();

        verify(abTestService, never()).autoConvergeAll();
    }

    @Test
    void scheduledAutoConverge_shouldInvokeServiceWhenEnabled() {
        ReflectionTestUtils.setField(scheduler, "enabled", true);
        when(abTestService.autoConvergeAll()).thenReturn(1);

        scheduler.scheduledAutoConverge();

        verify(abTestService).autoConvergeAll();
    }
}
