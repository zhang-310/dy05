package cn.gaifan.douyinOperations.module.ai.service.impl;

import cn.gaifan.douyinOperations.module.ai.entity.KnowledgeQualityScore;
import cn.gaifan.douyinOperations.module.ai.repository.EvolutionRuleRepository;
import cn.gaifan.douyinOperations.module.ai.repository.KnowledgeDeduplicationGroupRepository;
import cn.gaifan.douyinOperations.module.ai.repository.KnowledgeEvolutionLogRepository;
import cn.gaifan.douyinOperations.module.ai.repository.KnowledgeQualityScoreRepository;
import cn.gaifan.douyinOperations.module.ai.service.KnowledgeQualityScoringService;
import cn.gaifan.douyinOperations.module.ai.vo.EvolutionReportVO;
import cn.gaifan.douyinOperations.module.live.entity.LiveScriptVersion;
import cn.gaifan.douyinOperations.module.live.repository.LiveScriptVersionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("KnowledgeEvolutionServiceImpl 测试")
class KnowledgeEvolutionServiceImplTest {

    @Mock
    private KnowledgeEvolutionLogRepository evolutionLogRepository;
    @Mock
    private KnowledgeQualityScoreRepository qualityScoreRepository;
    @Mock
    private KnowledgeDeduplicationGroupRepository dedupGroupRepository;
    @Mock
    private EvolutionRuleRepository ruleRepository;
    @Mock
    private KnowledgeQualityScoringService qualityScoringService;
    @Mock
    private LiveScriptVersionRepository liveScriptVersionRepository;

    @InjectMocks
    private KnowledgeEvolutionServiceImpl service;

    @Test
    @DisplayName("generateEvolutionReport 应使用真实版本标题与版本总数")
    void generateEvolutionReport_shouldUseVersionFacts() {
        LocalDate start = LocalDate.of(2026, 4, 1);
        LocalDate end = LocalDate.of(2026, 4, 11);

        KnowledgeQualityScore top = new KnowledgeQualityScore();
        top.setUserId(1L);
        top.setScriptVersionId(101L);
        top.setQualityScore(new BigDecimal("91"));
        top.setUsageCount(6);
        top.setAdoptionRate(new BigDecimal("75"));
        top.setTrend("UP");
        top.setPeriodStart(Date.valueOf(start));
        top.setPeriodEnd(Date.valueOf(end));

        KnowledgeQualityScore second = new KnowledgeQualityScore();
        second.setUserId(1L);
        second.setScriptVersionId(102L);
        second.setQualityScore(new BigDecimal("80"));
        second.setUsageCount(4);
        second.setAdoptionRate(new BigDecimal("55"));
        second.setTrend("STABLE");
        second.setPeriodStart(Date.valueOf(start.minusWeeks(1)));
        second.setPeriodEnd(Date.valueOf(end.minusWeeks(1)));

        LiveScriptVersion version = new LiveScriptVersion();
        version.setId(101L);
        version.setVersionLabel("高转化开场");
        version.setUsageCount(8);
        version.setEffectivenessScore(new BigDecimal("88"));

        when(qualityScoreRepository.findByUserIdAndDeletedOrderByPeriodEndDesc(1L, 0))
                .thenReturn(List.of(top, second));
        when(liveScriptVersionRepository.countByOwnerIdAndDeleted(1L, 0)).thenReturn(2L);
        when(liveScriptVersionRepository.findById(101L)).thenReturn(Optional.of(version));
        when(liveScriptVersionRepository.findById(102L)).thenReturn(Optional.empty());
        when(evolutionLogRepository.countByUserIdAndActionAndCreatedAtGreaterThanEqualAndCreatedAtLessThanAndDeleted(
                eq(1L), eq("auto_import"), eq(Timestamp.valueOf(start.atStartOfDay())),
                eq(Timestamp.valueOf(end.plusDays(1).atStartOfDay())), eq(0))).thenReturn(1L);
        when(evolutionLogRepository.countByUserIdAndActionAndCreatedAtGreaterThanEqualAndCreatedAtLessThanAndDeleted(
                eq(1L), eq("auto_archive"), eq(Timestamp.valueOf(start.atStartOfDay())),
                eq(Timestamp.valueOf(end.plusDays(1).atStartOfDay())), eq(0))).thenReturn(2L);
        when(evolutionLogRepository.countByUserIdAndActionAndCreatedAtGreaterThanEqualAndCreatedAtLessThanAndDeleted(
                eq(1L), eq("merge"), eq(Timestamp.valueOf(start.atStartOfDay())),
                eq(Timestamp.valueOf(end.plusDays(1).atStartOfDay())), eq(0))).thenReturn(3L);
        when(qualityScoringService.calculateLibraryQualityScore(1L)).thenReturn(new BigDecimal("85"));
        when(dedupGroupRepository.countPendingReview(1L)).thenReturn(0L);

        EvolutionReportVO report = service.generateEvolutionReport(1L, "WEEKLY", start, end);

        assertThat(report.getOverview().getTotalScriptsInLibrary()).isEqualTo(2);
        assertThat(report.getOverview().getNewAddedCount()).isEqualTo(1);
        assertThat(report.getTopScripts()).hasSize(2);
        assertThat(report.getTopScripts().get(0).getTitle()).isEqualTo("高转化开场");
        assertThat(report.getTopScripts().get(0).getUsageCount()).isEqualTo(8);
    }
}
