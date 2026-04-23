package cn.gaifan.douyinOperations.module.ai.service.impl;

import cn.gaifan.douyinOperations.module.ai.entity.KnowledgeDeduplicationGroup;
import cn.gaifan.douyinOperations.module.ai.entity.KnowledgeEvolutionLog;
import cn.gaifan.douyinOperations.module.ai.entity.KnowledgeQualityScore;
import cn.gaifan.douyinOperations.module.ai.repository.EvolutionRuleRepository;
import cn.gaifan.douyinOperations.module.ai.repository.KnowledgeDeduplicationGroupRepository;
import cn.gaifan.douyinOperations.module.ai.repository.KnowledgeEvolutionLogRepository;
import cn.gaifan.douyinOperations.module.ai.repository.KnowledgeQualityScoreRepository;
import cn.gaifan.douyinOperations.module.ai.service.KnowledgeQualityScoringService;
import cn.gaifan.douyinOperations.module.live.entity.LiveScriptVersion;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("EvolutionRuleEngineServiceImpl 测试")
class EvolutionRuleEngineServiceImplTest {

    @Mock
    private EvolutionRuleRepository ruleRepository;
    @Mock
    private KnowledgeEvolutionLogRepository evolutionLogRepository;
    @Mock
    private KnowledgeQualityScoreRepository qualityScoreRepository;
    @Mock
    private KnowledgeDeduplicationGroupRepository dedupGroupRepository;
    @Mock
    private LiveScriptVersionRepository liveScriptVersionRepository;
    @Mock
    private KnowledgeQualityScoringService qualityScoringService;

    @InjectMocks
    private EvolutionRuleEngineServiceImpl service;

    @Test
    @DisplayName("executeInclusionRule 应激活并推荐版本")
    void executeInclusionRule_shouldActivateAndRecommendVersion() {
        LiveScriptVersion version = new LiveScriptVersion();
        version.setId(11L);
        version.setOwnerId(1L);
        version.setVersionStatus("draft");
        version.setIsRecommended(0);

        KnowledgeQualityScore score = new KnowledgeQualityScore();
        score.setQualityScore(new BigDecimal("88.5"));

        when(liveScriptVersionRepository.findById(11L)).thenReturn(Optional.of(version));
        when(qualityScoreRepository.findFirstByScriptVersionIdAndDeletedOrderByPeriodEndDesc(11L, 0))
                .thenReturn(Optional.of(score));
        when(liveScriptVersionRepository.save(any(LiveScriptVersion.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(evolutionLogRepository.save(any(KnowledgeEvolutionLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        int processed = service.executeInclusionRule(1L, List.of(11L));

        assertThat(processed).isEqualTo(1);
        assertThat(version.getVersionStatus()).isEqualTo("active");
        assertThat(version.getIsRecommended()).isEqualTo(1);
        assertThat(version.getRecommendScore()).isEqualByComparingTo("88.5");
        assertThat(version.getRecommendReason()).contains("自动纳入");
    }

    @Test
    @DisplayName("executeArchivalRule 应归档并取消推荐")
    void executeArchivalRule_shouldArchiveVersion() {
        LiveScriptVersion version = new LiveScriptVersion();
        version.setId(12L);
        version.setOwnerId(1L);
        version.setVersionStatus("active");
        version.setIsRecommended(1);
        version.setRecommendScore(new BigDecimal("90"));

        when(liveScriptVersionRepository.findById(12L)).thenReturn(Optional.of(version));
        when(liveScriptVersionRepository.save(any(LiveScriptVersion.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(evolutionLogRepository.save(any(KnowledgeEvolutionLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        int processed = service.executeArchivalRule(1L, List.of(12L));

        assertThat(processed).isEqualTo(1);
        assertThat(version.getVersionStatus()).isEqualTo("archived");
        assertThat(version.getIsRecommended()).isEqualTo(0);
        assertThat(version.getRecommendScore()).isEqualByComparingTo("0");
        assertThat(version.getRecommendReason()).contains("自动归档");
    }

    @Test
    @DisplayName("executeDedupRule 应归档 duplicate 并更新去重组状态")
    void executeDedupRule_shouldArchiveDuplicateAndUpdateGroup() {
        LiveScriptVersion master = new LiveScriptVersion();
        master.setId(21L);
        master.setOwnerId(1L);
        master.setVersionStatus("active");
        master.setIsRecommended(0);

        LiveScriptVersion duplicate = new LiveScriptVersion();
        duplicate.setId(22L);
        duplicate.setOwnerId(1L);
        duplicate.setVersionStatus("active");
        duplicate.setIsRecommended(1);

        KnowledgeDeduplicationGroup group = new KnowledgeDeduplicationGroup();
        group.setMasterScriptId(21L);
        group.setDuplicateScriptId(22L);
        group.setMergeStatus("DETECTED");
        group.setVariantType("VARIANT");
        group.setIsActive(1);

        when(liveScriptVersionRepository.findById(21L)).thenReturn(Optional.of(master));
        when(liveScriptVersionRepository.findById(22L)).thenReturn(Optional.of(duplicate));
        when(liveScriptVersionRepository.save(any(LiveScriptVersion.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(dedupGroupRepository.findByMasterScriptIdAndDuplicateScriptIdAndDeleted(21L, 22L, 0))
                .thenReturn(Optional.of(group));
        when(dedupGroupRepository.save(any(KnowledgeDeduplicationGroup.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(evolutionLogRepository.save(any(KnowledgeEvolutionLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        int processed = service.executeDedupRule(1L, List.of(Map.of(
                "masterScriptId", 21L,
                "duplicateIds", List.of(22L),
                "similarity", new BigDecimal("0.91")
        )));

        assertThat(processed).isEqualTo(1);
        assertThat(master.getIsRecommended()).isEqualTo(1);
        assertThat(duplicate.getVersionStatus()).isEqualTo("archived");
        assertThat(duplicate.getBasedOnVersionId()).isEqualTo(21L);
        assertThat(group.getMergeStatus()).isEqualTo("MERGED");
        assertThat(group.getVariantType()).isEqualTo("ARCHIVED");
        assertThat(group.getIsActive()).isEqualTo(0);
        assertThat(group.getMergedAt()).isNotNull();
    }
}
