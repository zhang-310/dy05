package cn.gaifan.douyinOperations.module.ai.service.impl;

import cn.gaifan.douyinOperations.module.ai.entity.AiIndexQueue;
import cn.gaifan.douyinOperations.module.ai.repository.AiIndexQueueRepository;
import cn.gaifan.douyinOperations.module.ai.repository.AiLiveReviewRepository;
import cn.gaifan.douyinOperations.module.ai.repository.AiViralAnalysisRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EvolutionDashboardServiceImplTest {

    @Mock
    private AiViralAnalysisRepository viralRepo;
    @Mock
    private AiLiveReviewRepository liveReviewRepo;
    @Mock
    private AiIndexQueueRepository indexQueueRepo;

    @InjectMocks
    private EvolutionDashboardServiceImpl dashboardService;

    @Test
    void enqueueIndex_shouldSave() {
        when(indexQueueRepo.save(any(AiIndexQueue.class))).thenAnswer(i -> {
            AiIndexQueue q = i.getArgument(0);
            q.setId(99L);
            return q;
        });
        long id = dashboardService.enqueueIndex("viral_analysis", 1L, "content", 3, 7L);
        assertThat(id).isEqualTo(99L);
        verify(indexQueueRepo).save(any(AiIndexQueue.class));
    }

    @Test
    void getEvolutionStats_shouldReturnKeys() {
        when(viralRepo.count()).thenReturn(10L);
        when(viralRepo.findAll(any(Specification.class))).thenReturn(Collections.emptyList());
        when(liveReviewRepo.count()).thenReturn(5L);
        when(liveReviewRepo.findAll(any(Specification.class))).thenReturn(Collections.emptyList());
        when(indexQueueRepo.findAll()).thenReturn(Collections.emptyList());
        Map<String, Object> stats = dashboardService.getEvolutionStats();
        assertThat(stats).containsKeys("viralAnalysisTotal", "indexQueuePending");
    }
}
