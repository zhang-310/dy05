package cn.gaifan.douyinOperations.module.product.service.impl;

import cn.gaifan.douyinOperations.module.product.service.ComplianceService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * ComplianceServiceImpl 单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ComplianceServiceImpl 单元测试")
class ComplianceServiceImplTest {

    @InjectMocks
    private ComplianceServiceImpl complianceService;

    @Mock
    private cn.gaifan.douyinOperations.module.script.service.ComplianceWordService complianceWordService;

    @Mock
    private cn.gaifan.douyinOperations.module.script.service.ViolationWordService violationWordService;

    @Nested
    @DisplayName("checkAndFix 合规检测")
    class CheckAndFixTests {

        @Test
        @DisplayName("null_orBlank_shouldPass")
        void nullOrBlank_shouldPass() {
            assertThat(complianceService.checkAndFix(null)).satisfies(r -> {
                assertThat(r.passed()).isTrue();
                assertThat(r.fixedText()).isEmpty();
                assertThat(r.violations()).isEmpty();
            });
            assertThat(complianceService.checkAndFix("")).satisfies(r -> {
                assertThat(r.passed()).isTrue();
                assertThat(r.fixedText()).isEmpty();
            });
            assertThat(complianceService.checkAndFix("   ")).satisfies(r -> {
                assertThat(r.passed()).isTrue();
            });
        }

        @Test
        @DisplayName("cleanText_shouldPass")
        void cleanText_shouldPass() {
            ComplianceService.ComplianceResult r = complianceService.checkAndFix("这是一段合规的文案");
            assertThat(r.passed()).isTrue();
            assertThat(r.fixedText()).isEqualTo("这是一段合规的文案");
            assertThat(r.violations()).isEmpty();
        }

        @Test
        @DisplayName("absoluteWord_shouldFixAndPass")
        void absoluteWord_shouldFixAndPass() {
            ComplianceService.ComplianceResult r = complianceService.checkAndFix("这是最好的产品");
            assertThat(r.passed()).isTrue();
            assertThat(r.fixedText()).isEqualTo("这是优质的产品");
            assertThat(r.violations()).contains("绝对化用语: 最好");
        }

        @Test
        @DisplayName("medicalWord_shouldFail")
        void medicalWord_shouldFail() {
            ComplianceService.ComplianceResult r = complianceService.checkAndFix("本品具有治疗功效");
            assertThat(r.passed()).isFalse();
            assertThat(r.fixedText()).isEqualTo("本品具有治疗功效");
            assertThat(r.violations()).anyMatch(v -> v.contains("治疗"));
        }

        @Test
        @DisplayName("multipleAbsolute_shouldFixAll")
        void multipleAbsolute_shouldFixAll() {
            ComplianceService.ComplianceResult r = complianceService.checkAndFix("顶级品质，100%纯天然");
            assertThat(r.passed()).isTrue();
            assertThat(r.fixedText()).contains("高端").contains("高");
        }
    }
}
