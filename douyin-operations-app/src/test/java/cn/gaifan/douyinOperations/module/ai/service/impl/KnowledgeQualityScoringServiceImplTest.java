package cn.gaifan.douyinOperations.module.ai.service.impl;

import cn.gaifan.douyinOperations.module.ai.entity.KnowledgeQualityScore;
import cn.gaifan.douyinOperations.module.ai.repository.AiLiveReviewRepository;
import cn.gaifan.douyinOperations.module.ai.repository.KnowledgeEvolutionLogRepository;
import cn.gaifan.douyinOperations.module.ai.repository.KnowledgeQualityScoreRepository;
import cn.gaifan.douyinOperations.module.live.entity.LiveMonitor;
import cn.gaifan.douyinOperations.module.live.entity.LiveScriptVersion;
import cn.gaifan.douyinOperations.module.live.repository.LiveMonitorRepository;
import cn.gaifan.douyinOperations.module.live.repository.LiveScriptVersionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("KnowledgeQualityScoringServiceImpl 测试")
class KnowledgeQualityScoringServiceImplTest {

    @Mock
    private KnowledgeQualityScoreRepository qualityScoreRepository;
    @Mock
    private KnowledgeEvolutionLogRepository evolutionLogRepository;
    @Mock
    private LiveScriptVersionRepository liveScriptVersionRepository;
    @Mock
    private LiveMonitorRepository liveMonitorRepository;
    @Mock
    private AiLiveReviewRepository aiLiveReviewRepository;

    @InjectMocks
    private KnowledgeQualityScoringServiceImpl service;

    @Test
    @DisplayName("calculateAndSaveQualityScore 应使用版本与场次监控真实数据")
    void calculateAndSaveQualityScore_shouldUseVersionAndMonitorFacts() {
        LocalDate periodStart = LocalDate.of(2026, 4, 1);
        LocalDate periodEnd = LocalDate.of(2026, 4, 11);

        LiveScriptVersion version = new LiveScriptVersion();
        version.setId(1L);
        version.setOwnerId(1L);
        version.setSessionId(10L);
        version.setEffectivenessScore(new BigDecimal("82"));
        version.setUsageCount(8);
        version.setLastUsedTime(Timestamp.valueOf("2026-04-10 10:00:00"));

        LiveScriptVersion peer = new LiveScriptVersion();
        peer.setId(2L);
        peer.setOwnerId(1L);
        peer.setSessionId(11L);
        peer.setUsageCount(4);

        LiveMonitor monitor = new LiveMonitor();
        monitor.setSessionId(10L);
        monitor.setTotalViewers(100);
        monitor.setOnlineCount(80);
        monitor.setLikes(20L);
        monitor.setComments(10);
        monitor.setShares(5);
        monitor.setNewFollowers(3);
        monitor.setOrders(4);

        when(qualityScoreRepository.findByScriptVersionIdAndPeriodStartAndPeriodEndAndDeleted(
                eq(1L), any(), any(), eq(0))).thenReturn(Optional.empty());
        when(qualityScoreRepository.findFirstByScriptVersionIdAndDeletedOrderByPeriodEndDesc(1L, 0))
                .thenReturn(Optional.empty());
        when(liveScriptVersionRepository.findById(1L)).thenReturn(Optional.of(version));
        when(liveScriptVersionRepository.findByOwnerIdAndDeleted(1L, 0)).thenReturn(List.of(version, peer));
        when(liveMonitorRepository.findBySessionIdAndTimestampBetween(eq(10L), any(), any()))
                .thenReturn(List.of(monitor));
        when(aiLiveReviewRepository.findBySessionIdAndDeleted(10L, 0)).thenReturn(Optional.empty());
        when(qualityScoreRepository.save(any(KnowledgeQualityScore.class))).thenAnswer(invocation -> invocation.getArgument(0));

        KnowledgeQualityScore saved = service.calculateAndSaveQualityScore(1L, 1L, periodStart, periodEnd);

        assertThat(saved.getEffectivenessScore()).isEqualByComparingTo("82.00");
        assertThat(saved.getUsageCount()).isEqualTo(8);
        assertThat(saved.getAdoptionRate()).isEqualByComparingTo("100.00");
        assertThat(saved.getEngagementRate()).isEqualByComparingTo("38.00");
        assertThat(saved.getConversionRate()).isEqualByComparingTo("4.00");
        assertThat(saved.getAvgSentimentScore()).isEqualByComparingTo("55.10");
        assertThat(saved.getQualityScore()).isEqualByComparingTo("63.27");
        assertThat(saved.getNotes()).contains("live_script_version");
    }
}
