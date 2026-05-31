package cn.gaifan.douyinOperations.module.live.config;

import cn.gaifan.douyinOperations.module.live.repository.LiveMonitorRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LiveMonitorArchiveSchedulerTest {

    @Mock
    private LiveMonitorRepository liveMonitorRepository;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private LiveMonitorArchiveScheduler scheduler;

    @Test
    void archive_shouldSkipWhenDisabled() {
        ReflectionTestUtils.setField(scheduler, "schedulerEnabled", false);

        scheduler.archive();

        verify(jdbcTemplate, never()).update(anyString(), org.mockito.ArgumentMatchers.<Object>any());
        verify(liveMonitorRepository, never()).deleteByTimestampBefore(org.mockito.ArgumentMatchers.any());
    }
}
