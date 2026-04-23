package cn.gaifan.douyinOperations.module.ai.config;

import cn.gaifan.douyinOperations.module.ai.repository.AiKnowledgeBaseRepository;
import cn.gaifan.douyinOperations.module.ai.service.EvolutionRuleEngineService;
import cn.gaifan.douyinOperations.module.ai.service.KnowledgeEvolutionService;
import cn.gaifan.douyinOperations.module.ai.service.KnowledgeQualityScoringService;
import cn.gaifan.douyinOperations.module.ai.vo.EvolutionReportVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("KnowledgeEvolutionScheduler 测试")
class KnowledgeEvolutionSchedulerTest {

    @Mock
    private EvolutionRuleEngineService ruleEngineService;
    @Mock
    private KnowledgeEvolutionService evolutionService;
    @Mock
    private KnowledgeQualityScoringService qualityScoringService;
    @Mock
    private AiKnowledgeBaseRepository knowledgeBaseRepository;

    @InjectMocks
    private KnowledgeEvolutionScheduler scheduler;

    @Test
    @DisplayName("dailyEvolutionTask 应对知识库用户执行规则和质量重算")
    void dailyEvolutionTask_shouldProcessKnowledgeBaseUsers() {
        ReflectionTestUtils.setField(scheduler, "knowledgeEvolutionEnabled", true);
        ReflectionTestUtils.setField(scheduler, "maxUsersPerRun", 10);
        ReflectionTestUtils.setField(scheduler, "dailyPeriodDays", 30);
        when(knowledgeBaseRepository.findDistinctUserIds()).thenReturn(Arrays.asList(1L, 2L, null, 2L));
        when(ruleEngineService.triggerAllRules(1L)).thenReturn(Map.of("included", 1));
        when(ruleEngineService.triggerAllRules(2L)).thenReturn(Map.of("included", 0));
        when(evolutionService.recalculateQualityScores(eq(1L), eq(30))).thenReturn(3);
        when(evolutionService.recalculateQualityScores(eq(2L), eq(30))).thenReturn(0);
        when(qualityScoringService.calculateLibraryQualityScore(any())).thenReturn(BigDecimal.TEN);

        scheduler.dailyEvolutionTask();

        verify(ruleEngineService).triggerAllRules(1L);
        verify(ruleEngineService).triggerAllRules(2L);
        verify(evolutionService).recalculateQualityScores(1L, 30);
        verify(evolutionService).recalculateQualityScores(2L, 30);
    }

    @Test
    @DisplayName("weeklyReportGenerationTask 应生成周报")
    void weeklyReportGenerationTask_shouldGenerateReports() {
        ReflectionTestUtils.setField(scheduler, "knowledgeEvolutionEnabled", true);
        ReflectionTestUtils.setField(scheduler, "maxUsersPerRun", 10);
        when(knowledgeBaseRepository.findDistinctUserIds()).thenReturn(List.of(7L));
        when(evolutionService.generateEvolutionReport(eq(7L), eq("WEEKLY"), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(new EvolutionReportVO());
        when(qualityScoringService.calculateLibraryQualityScore(7L)).thenReturn(BigDecimal.ONE);

        scheduler.weeklyReportGenerationTask();

        verify(evolutionService).generateEvolutionReport(eq(7L), eq("WEEKLY"), any(LocalDate.class), any(LocalDate.class));
    }

    @Test
    @DisplayName("lowPerformanceArchivalTask 仅对有候选的用户执行归档")
    void lowPerformanceArchivalTask_shouldArchiveCandidatesOnly() {
        ReflectionTestUtils.setField(scheduler, "knowledgeEvolutionEnabled", true);
        ReflectionTestUtils.setField(scheduler, "maxUsersPerRun", 10);
        when(knowledgeBaseRepository.findDistinctUserIds()).thenReturn(List.of(3L, 4L));
        when(ruleEngineService.evaluateArchivalRule(3L)).thenReturn(List.of(11L, 12L));
        when(ruleEngineService.evaluateArchivalRule(4L)).thenReturn(List.of());
        when(ruleEngineService.executeArchivalRule(3L, List.of(11L, 12L))).thenReturn(2);
        when(qualityScoringService.calculateLibraryQualityScore(any())).thenReturn(BigDecimal.ZERO);

        scheduler.lowPerformanceArchivalTask();

        verify(ruleEngineService).executeArchivalRule(3L, List.of(11L, 12L));
        verify(ruleEngineService, never()).executeArchivalRule(eq(4L), any());
    }

    @Test
    @DisplayName("scheduler disabled 时不执行任何操作")
    void schedulerDisabled_shouldSkipTasks() {
        ReflectionTestUtils.setField(scheduler, "knowledgeEvolutionEnabled", false);

        scheduler.dailyEvolutionTask();

        verify(knowledgeBaseRepository, never()).findDistinctUserIds();
    }
}
