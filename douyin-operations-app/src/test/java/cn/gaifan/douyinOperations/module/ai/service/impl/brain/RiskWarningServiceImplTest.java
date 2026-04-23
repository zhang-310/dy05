package cn.gaifan.douyinOperations.module.ai.service.impl.brain;

import cn.gaifan.douyinOperations.module.ai.service.brain.RiskWarningService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@DisplayName("RiskWarningServiceImpl 风险预警服务测试")
class RiskWarningServiceImplTest {

    private RiskWarningServiceImpl riskWarningService;

    @Mock
    private cn.gaifan.douyinOperations.module.script.service.ComplianceWordService complianceWordService;

    @BeforeEach
    void setUp() {
        riskWarningService = new RiskWarningServiceImpl();
        ReflectionTestUtils.setField(riskWarningService, "enabled", true);
        ReflectionTestUtils.setField(riskWarningService, "complianceWordService", complianceWordService);
    }

    @Nested
    @DisplayName("isAvailable")
    class IsAvailableTests {

        @Test
        void isAvailable_enabled_shouldReturnTrue() {
            assertThat(riskWarningService.isAvailable()).isTrue();
        }

        @Test
        void isAvailable_disabled_shouldReturnFalse() {
            ReflectionTestUtils.setField(riskWarningService, "enabled", false);
            assertThat(riskWarningService.isAvailable()).isFalse();
        }
    }

    @Nested
    @DisplayName("warn")
    class WarnTests {

        @Test
        void warn_disabled_shouldReturnEmpty() {
            ReflectionTestUtils.setField(riskWarningService, "enabled", false);
            assertThat(riskWarningService.warn("测试内容", 1L)).isEmpty();
        }

        @Test
        void warn_nullContent_shouldReturnEmpty() {
            assertThat(riskWarningService.warn(null, 1L)).isEmpty();
        }

        @Test
        void warn_normalContent_shouldReturnList() {
            List<RiskWarningService.RiskItem> result = riskWarningService.warn("这是一段正常文案", 1L);
            assertThat(result).isNotNull();
        }

        @Test
        void warn_contentWithProhibited_shouldDetect() {
            List<RiskWarningService.RiskItem> result = riskWarningService.warn("国家级顶尖产品", 1L);
            assertThat(result).isNotEmpty();
        }
    }

    @Nested
    @DisplayName("warnBatch")
    class WarnBatchTests {

        @Test
        void warnBatch_disabled_shouldReturnEmpty() {
            ReflectionTestUtils.setField(riskWarningService, "enabled", false);
            assertThat(riskWarningService.warnBatch(List.of("a", "b"), 1L)).isEmpty();
        }

        @Test
        void warnBatch_nullContents_shouldReturnEmpty() {
            assertThat(riskWarningService.warnBatch(null, 1L)).isEmpty();
        }

        @Test
        void warnBatch_emptyContents_shouldReturnEmpty() {
            assertThat(riskWarningService.warnBatch(List.of(), 1L)).isEmpty();
        }
    }

    @Nested
    @DisplayName("getStats")
    class GetStatsTests {

        @Test
        void getStats_initially_shouldReturnZeroTotal() {
            var stats = riskWarningService.getStats();
            assertThat(stats.totalChecks()).isZero();
        }

        @Test
        void getStats_afterWarn_shouldIncrement() {
            riskWarningService.warn("test", 1L);
            var stats = riskWarningService.getStats();
            assertThat(stats.totalChecks()).isEqualTo(1);
        }
    }
}
