package cn.gaifan.douyinOperations.module.system.config;

import cn.gaifan.douyinOperations.module.system.repository.SysApiCallLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ApiCallLogCleanupSchedulerTest {

    @Mock
    private SysApiCallLogRepository apiCallLogRepository;

    @InjectMocks
    private ApiCallLogCleanupScheduler scheduler;

    @Test
    void cleanup_shouldSkipWhenDisabled() {
        ReflectionTestUtils.setField(scheduler, "schedulerEnabled", false);

        scheduler.cleanup();

        verify(apiCallLogRepository, never()).deleteBatchByCreateTimeBefore(org.mockito.ArgumentMatchers.any());
    }
}
