package cn.gaifan.douyinOperations.module.ai.config;

import cn.gaifan.douyinOperations.module.ai.service.CompetitorInsightService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CompetitorInsightSchedulerTest {

    @Mock
    private CompetitorInsightService competitorInsightService;

    @InjectMocks
    private CompetitorInsightScheduler scheduler;

    @Test
    void dailyCollect_shouldSkipWhenDisabled() {
        ReflectionTestUtils.setField(scheduler, "enabled", false);

        scheduler.dailyCollect();

        verify(competitorInsightService, never()).collectInsights();
        verify(competitorInsightService, never()).ingestHighQualityToKb();
    }

    @Test
    void dailyCollect_shouldCollectWhenEnabled() {
        ReflectionTestUtils.setField(scheduler, "enabled", true);

        scheduler.dailyCollect();

        verify(competitorInsightService).collectInsights();
        verify(competitorInsightService).ingestHighQualityToKb();
    }
}
