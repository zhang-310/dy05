package cn.gaifan.douyinOperations.module.live.service.impl;

import cn.gaifan.douyinOperations.module.live.entity.LiveEffectivenessConfig;
import cn.gaifan.douyinOperations.module.live.entity.LiveProductData;
import cn.gaifan.douyinOperations.module.live.entity.LiveScript;
import cn.gaifan.douyinOperations.module.live.entity.LiveScriptEffectiveness;
import cn.gaifan.douyinOperations.module.live.entity.LiveSession;
import cn.gaifan.douyinOperations.module.live.entity.LiveSessionData;
import cn.gaifan.douyinOperations.module.live.repository.LiveEffectivenessConfigRepository;
import cn.gaifan.douyinOperations.module.live.repository.LiveProductDataRepository;
import cn.gaifan.douyinOperations.module.live.repository.LiveScriptEffectivenessRepository;
import cn.gaifan.douyinOperations.module.live.repository.LiveScriptRepository;
import cn.gaifan.douyinOperations.module.live.repository.LiveMonitorRepository;
import cn.gaifan.douyinOperations.module.live.repository.LiveSessionDataRepository;
import cn.gaifan.douyinOperations.module.live.repository.LiveSessionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("EffectivenessScoreServiceImpl 测试")
class EffectivenessScoreServiceImplTest {

    @Mock
    private LiveScriptRepository liveScriptRepository;
    @Mock
    private LiveMonitorRepository liveMonitorRepository;
    @Mock
    private LiveScriptEffectivenessRepository effectivenessRepository;
    @Mock
    private LiveSessionRepository liveSessionRepository;
    @Mock
    private LiveSessionDataRepository liveSessionDataRepository;
    @Mock
    private LiveProductDataRepository liveProductDataRepository;
    @Mock
    private LiveEffectivenessConfigRepository liveEffectivenessConfigRepository;

    @InjectMocks
    private EffectivenessScoreServiceImpl service;

    @Test
    @DisplayName("calculateScore 应基于真实场次/商品/脚本增量计算")
    void calculateScore_shouldUseRealSessionFacts() {
        LiveScript script = new LiveScript();
        script.setId(1L);
        script.setSessionId(100L);
        script.setProductId(200L);
        script.setUserId(9L);
        script.setInteractionDelta(12);
        script.setConversionDelta(3);
        script.setViewerDelta(8);
        script.setExecutionTime(120L);
        script.setExecuted(3);

        LiveSession session = new LiveSession();
        session.setId(100L);
        session.setUserId(9L);
        session.setStartTime(Timestamp.valueOf("2026-04-12 10:00:00"));
        session.setEndTime(Timestamp.valueOf("2026-04-12 11:00:00"));

        LiveSessionData sessionData = new LiveSessionData();
        sessionData.setSessionId(100L);
        sessionData.setTotalViewers(120);
        sessionData.setTotalLikes(40L);
        sessionData.setTotalComments(15);
        sessionData.setTotalShares(5);
        sessionData.setTotalOrders(10);
        sessionData.setTotalRevenue(new BigDecimal("1000"));
        sessionData.setAvgStayTime(90);

        LiveProductData productData = new LiveProductData();
        productData.setSessionId(100L);
        productData.setProductId(200L);
        productData.setOrders(6);
        productData.setRevenue(new BigDecimal("600"));
        productData.setConversionRate(new BigDecimal("0.0800"));

        LiveEffectivenessConfig config = new LiveEffectivenessConfig();
        config.setConversionWeight(new BigDecimal("0.30"));
        config.setInteractionWeight(new BigDecimal("0.25"));
        config.setRetentionWeight(new BigDecimal("0.25"));
        config.setGmvWeight(new BigDecimal("0.20"));

        when(liveScriptRepository.findById(1L)).thenReturn(Optional.of(script));
        when(liveSessionRepository.findById(100L)).thenReturn(Optional.of(session));
        when(liveMonitorRepository.findBySessionIdOrderByTimestampAsc(100L)).thenReturn(List.of());
        when(liveSessionDataRepository.findBySessionId(100L)).thenReturn(Optional.of(sessionData));
        when(liveProductDataRepository.findBySessionIdAndProductId(100L, 200L)).thenReturn(Optional.of(productData));
        when(liveEffectivenessConfigRepository.findByUserIdAndIsDefaultAndDeleted(9L, 1, 0)).thenReturn(Optional.of(config));
        when(effectivenessRepository.findByScriptIdAndDeletedOrderByCalculatedAtDesc(1L, 0)).thenReturn(Optional.empty());
        when(effectivenessRepository.save(any(LiveScriptEffectiveness.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Map<String, Object> result = service.calculateScore(1L, 100L);

        assertThat(result.get("conversionRate")).isEqualTo(2.5d);
        assertThat(result.get("interactionRate")).isEqualTo(10.0d);
        assertThat(result.get("completionRate")).isEqualTo(75.0d);
        assertThat(result.get("gmvScore")).isEqualTo(50.0d);
        assertThat(result.get("orders")).isEqualTo(6);
        assertThat(result.get("totalScore")).isEqualTo(3.2d);
    }
}
