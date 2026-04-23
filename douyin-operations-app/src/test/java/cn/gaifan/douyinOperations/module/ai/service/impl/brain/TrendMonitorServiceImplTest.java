package cn.gaifan.douyinOperations.module.ai.service.impl.brain;

import cn.gaifan.douyinOperations.module.ai.service.brain.TrendMonitorService;
import cn.gaifan.douyinOperations.module.tianapi.service.TianApiService;
import cn.gaifan.douyinOperations.module.tianapi.vo.HotItemVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("TrendMonitorServiceImpl 趋势感知服务测试")
class TrendMonitorServiceImplTest {

    private TrendMonitorServiceImpl trendMonitor;

    @Mock
    private TianApiService tianApiService;

    @BeforeEach
    void setUp() {
        trendMonitor = new TrendMonitorServiceImpl();
        ReflectionTestUtils.setField(trendMonitor, "enabled", true);
        ReflectionTestUtils.setField(trendMonitor, "intervalSec", 60);
        ReflectionTestUtils.setField(trendMonitor, "tianApiService", tianApiService);
    }

    @Nested
    @DisplayName("isAvailable")
    class IsAvailableTests {

        @Test
        void isAvailable_enabled_shouldReturnTrue() {
            assertThat(trendMonitor.isAvailable()).isTrue();
        }

        @Test
        void isAvailable_disabled_shouldReturnFalse() {
            ReflectionTestUtils.setField(trendMonitor, "enabled", false);
            assertThat(trendMonitor.isAvailable()).isFalse();
        }
    }

    @Nested
    @DisplayName("getCurrentTrends")
    class GetCurrentTrendsTests {

        @Test
        void getCurrentTrends_disabled_shouldReturnEmpty() {
            ReflectionTestUtils.setField(trendMonitor, "enabled", false);
            assertThat(trendMonitor.getCurrentTrends(null, 10)).isEmpty();
        }

        @Test
        void getCurrentTrends_tianApiNull_shouldReturnEmpty() {
            ReflectionTestUtils.setField(trendMonitor, "tianApiService", null);
            assertThat(trendMonitor.getCurrentTrends(null, 10)).isEmpty();
        }

        @Test
        void getCurrentTrends_tianApiNotEnabled_shouldReturnEmpty() {
            when(tianApiService.isEnabled()).thenReturn(false);
            assertThat(trendMonitor.getCurrentTrends(null, 10)).isEmpty();
        }

        @Test
        void getCurrentTrends_withData_shouldReturnTrends() {
            when(tianApiService.isEnabled()).thenReturn(true);
            HotItemVO h = new HotItemVO();
            h.setWord("测试热搜");
            h.setHotIndex(10000L);
            h.setLabel("热");
            when(tianApiService.douyinHot()).thenReturn(List.of(h));
            when(tianApiService.weiboHot()).thenReturn(List.of());
            when(tianApiService.networkHot()).thenReturn(List.of());

            trendMonitor.monitorAndPersist();
            List<TrendMonitorService.TrendSignal> result = trendMonitor.getCurrentTrends(null, 10);
            assertThat(result).isNotEmpty();
            assertThat(result.get(0).title()).isEqualTo("测试热搜");
        }
    }

    @Nested
    @DisplayName("detectNewTrends")
    class DetectNewTrendsTests {

        @Test
        void detectNewTrends_disabled_shouldReturnEmpty() {
            ReflectionTestUtils.setField(trendMonitor, "enabled", false);
            assertThat(trendMonitor.detectNewTrends()).isEmpty();
        }
    }
}
