package cn.gaifan.douyinOperations.module.shortvideo.service.impl;

import cn.gaifan.douyinOperations.module.shortvideo.entity.SvDigitalHumanTask;
import cn.gaifan.douyinOperations.module.shortvideo.repository.SvDigitalHumanTaskRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DigitalHumanPollingServiceImplTest {

    @Mock
    private SvDigitalHumanTaskRepository taskRepository;

    @InjectMocks
    private DigitalHumanPollingServiceImpl pollingService;

    @Test
    void pollPendingTasks_shouldSkipWhenDisabled() {
        ReflectionTestUtils.setField(pollingService, "pollingEnabled", false);

        pollingService.pollPendingTasks();

        verify(taskRepository, never()).findByStatusInAndDeletedAndRetryCountLessThan(any(), anyInt(), anyInt());
    }

    @Test
    void pollPendingTasks_shouldQueryPendingTasksWhenEnabled() {
        ReflectionTestUtils.setField(pollingService, "pollingEnabled", true);
        org.mockito.Mockito.when(taskRepository.findByStatusInAndDeletedAndRetryCountLessThan(List.of("SUBMITTED", "PROCESSING"), 0, 10))
                .thenReturn(List.of());

        pollingService.pollPendingTasks();

        verify(taskRepository).findByStatusInAndDeletedAndRetryCountLessThan(List.of("SUBMITTED", "PROCESSING"), 0, 10);
    }
}
