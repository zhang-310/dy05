package cn.gaifan.douyinOperations.module.product.task;

import cn.gaifan.douyinOperations.module.product.repository.ProductScriptVersionRepository;
import cn.gaifan.douyinOperations.module.product.service.EffectivenessScoreService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EffectivenessScoreCalculationTaskTest {

    @Mock
    private ProductScriptVersionRepository versionRepository;

    @Mock
    private EffectivenessScoreService scoreService;

    @InjectMocks
    private EffectivenessScoreCalculationTask task;

    @Test
    void calculateEffectivenessScoresScheduled_shouldSkipWhenDisabled() {
        ReflectionTestUtils.setField(task, "schedulerEnabled", false);

        task.calculateEffectivenessScoresScheduled();

        verify(versionRepository, never()).findAll(any(Specification.class));
        verify(scoreService, never()).recalculateAllScores(any(), any());
    }
}
