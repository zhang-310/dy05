package cn.gaifan.douyinOperations.module.live.config;

import cn.gaifan.douyinOperations.module.live.entity.LiveLearningMemory;
import cn.gaifan.douyinOperations.module.live.repository.LiveLearningMemoryRepository;
import cn.gaifan.douyinOperations.module.live.service.CrossSessionLearningService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LiveLearningMemorySchedulerTest {

    @Mock
    private CrossSessionLearningService crossSessionLearningService;

    @Mock
    private LiveLearningMemoryRepository memoryRepository;

    @InjectMocks
    private LiveLearningMemoryScheduler scheduler;

    @Test
    void decayAllMemories_shouldSkipWhenDisabled() {
        ReflectionTestUtils.setField(scheduler, "schedulerEnabled", false);

        scheduler.decayAllMemories();

        verify(memoryRepository, never()).findByConfidenceLessThanAndDeleted(any(), anyInt());
    }

    @Test
    void archiveIneffectiveMemories_shouldSkipWhenDisabled() {
        ReflectionTestUtils.setField(scheduler, "schedulerEnabled", false);

        scheduler.archiveIneffectiveMemories();

        verify(crossSessionLearningService, never()).decayIneffectiveMemories();
    }

    @Test
    void decayAllMemories_shouldDecayOnlyAboveThresholdWhenEnabled() {
        ReflectionTestUtils.setField(scheduler, "schedulerEnabled", true);
        LiveLearningMemory high = new LiveLearningMemory();
        high.setId(1L);
        high.setConfidence(new BigDecimal("0.80"));
        LiveLearningMemory low = new LiveLearningMemory();
        low.setId(2L);
        low.setConfidence(new BigDecimal("0.10"));
        when(memoryRepository.findByConfidenceLessThanAndDeleted(BigDecimal.ONE, 0)).thenReturn(List.of(high, low));

        scheduler.decayAllMemories();

        verify(memoryRepository).decayConfidence(1L);
        verify(memoryRepository, never()).decayConfidence(2L);
    }
}
