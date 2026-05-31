package cn.gaifan.douyinOperations.module.live.config;

import cn.gaifan.douyinOperations.module.live.entity.LiveSession;
import cn.gaifan.douyinOperations.module.live.repository.LiveSessionRepository;
import cn.gaifan.douyinOperations.module.live.service.LiveSessionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.sql.Timestamp;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LiveAutoSyncSchedulerTest {

    @Mock
    private LiveSessionRepository sessionRepository;

    @Mock
    private LiveSessionService liveSessionService;

    @InjectMocks
    private LiveAutoSyncScheduler scheduler;

    @Test
    void autoSyncEndedSessions_shouldSkipWhenDisabled() {
        ReflectionTestUtils.setField(scheduler, "schedulerEnabled", false);

        scheduler.autoSyncEndedSessions();

        verify(sessionRepository, never()).findByStatusAndScheduledEndTimeBeforeAndAutoSyncEnabledAndDeleted(any(), any(), any(), any());
        verify(liveSessionService, never()).updateStatus(any(), any());
    }

    @Test
    void autoSyncEndedSessions_shouldEndExpiredSessionsWhenEnabled() {
        ReflectionTestUtils.setField(scheduler, "schedulerEnabled", true);
        LiveSession session = new LiveSession();
        session.setId(9L);
        session.setLiveTitle("test live");
        when(sessionRepository.findByStatusAndScheduledEndTimeBeforeAndAutoSyncEnabledAndDeleted(eq(1), any(Timestamp.class), eq(1), eq(0)))
                .thenReturn(List.of(session));
        when(sessionRepository.findByStatusAndPlannedEndTimeBeforeAndAutoSyncEnabledAndDeleted(eq(1), any(Timestamp.class), eq(1), eq(0)))
                .thenReturn(List.of());

        scheduler.autoSyncEndedSessions();

        verify(sessionRepository).save(session);
        verify(liveSessionService).updateStatus(9L, 2);
    }
}
