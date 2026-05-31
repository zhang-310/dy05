package cn.gaifan.douyinOperations.module.live.controller;

import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.live.repository.LiveMonitorRepository;
import cn.gaifan.douyinOperations.module.live.repository.LiveSessionRepository;
import cn.gaifan.douyinOperations.module.live.vo.LiveAnalyticsOverviewVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LiveAnalyticsControllerTest {

    @Mock
    private LiveSessionRepository liveSessionRepository;

    @Mock
    private LiveMonitorRepository liveMonitorRepository;

    @InjectMocks
    private LiveAnalyticsController controller;

    @Test
    void overview_shouldAggregateLiveSessionAndMonitorData() {
        when(liveSessionRepository.countByDeleted(0)).thenReturn(3L);
        when(liveMonitorRepository.countActiveRecords()).thenReturn(5L);
        when(liveMonitorRepository.findGlobalMaxGmv()).thenReturn(new BigDecimal("1288.50"));
        when(liveMonitorRepository.findGlobalAvgViewers()).thenReturn(256.4);

        RESTResult<LiveAnalyticsOverviewVO> result = controller.overview();

        assertThat(result.getStatus()).isEqualTo(200);
        assertThat(result.getData().getTotalSessions()).isEqualTo(3L);
        assertThat(result.getData().getTotalGmv()).isEqualTo(1288.50);
        assertThat(result.getData().getAvgViewers()).isEqualTo(256.4);
        assertThat(result.getData().getSource()).isEqualTo("live_session+live_monitor");
        assertThat(result.getData().getDegraded()).isFalse();
        assertThat(result.getData().getFallbackReason()).isEmpty();
    }

    @Test
    void overview_shouldMarkDegradedWhenNoMonitorData() {
        when(liveSessionRepository.countByDeleted(0)).thenReturn(2L);
        when(liveMonitorRepository.countActiveRecords()).thenReturn(0L);
        when(liveMonitorRepository.findGlobalMaxGmv()).thenReturn(BigDecimal.ZERO);
        when(liveMonitorRepository.findGlobalAvgViewers()).thenReturn(null);

        RESTResult<LiveAnalyticsOverviewVO> result = controller.overview();

        assertThat(result.getStatus()).isEqualTo(200);
        assertThat(result.getData().getTotalSessions()).isEqualTo(2L);
        assertThat(result.getData().getTotalGmv()).isEqualTo(0.0);
        assertThat(result.getData().getAvgViewers()).isEqualTo(0.0);
        assertThat(result.getData().getSource()).isEqualTo("live_session");
        assertThat(result.getData().getDegraded()).isTrue();
        assertThat(result.getData().getFallbackReason()).contains("live_monitor");
    }
}
