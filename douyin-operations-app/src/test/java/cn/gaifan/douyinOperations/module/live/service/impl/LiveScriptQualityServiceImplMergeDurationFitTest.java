package cn.gaifan.douyinOperations.module.live.service.impl;

import cn.gaifan.douyinOperations.module.live.entity.LiveScript;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link LiveScriptQualityServiceImpl#mergeDurationFitHint} 不依赖注入字段，可独立实例化验证落库前提示合并。
 */
class LiveScriptQualityServiceImplMergeDurationFitTest {

    @Test
    void mergeDurationFitHint_whenTooLong_mergesAiSuggestionAndReturnsOkFalse() {
        LiveScriptQualityServiceImpl svc = new LiveScriptQualityServiceImpl();
        LiveScript s = new LiveScript();
        s.setId(99L);
        s.setDurationLimitSec(10);
        // estSec = chars/4.5; need > 10*1.12 => chars > 50.4
        s.setScriptContent("x".repeat(52));

        Map<String, Object> fit = svc.mergeDurationFitHint(s);

        assertThat(fit.get("checked")).isEqualTo(true);
        assertThat(fit.get("ok")).isEqualTo(false);
        assertThat(s.getAiSuggestion()).contains("[时长]");
        assertThat(s.getAiSuggestion()).contains("超过槽位");
    }

    @Test
    void mergeDurationFitHint_whenNoSlotSeconds_skipsHint() {
        LiveScriptQualityServiceImpl svc = new LiveScriptQualityServiceImpl();
        LiveScript s = new LiveScript();
        s.setScriptContent("x".repeat(200));
        s.setDurationLimitSec(null);

        Map<String, Object> fit = svc.mergeDurationFitHint(s);

        assertThat(fit.get("checked")).isEqualTo(false);
        assertThat(fit.get("ok")).isEqualTo(true);
        assertThat(s.getAiSuggestion()).isNull();
    }

    @Test
    void mergeDurationFitHint_whenFits_noSuggestion() {
        LiveScriptQualityServiceImpl svc = new LiveScriptQualityServiceImpl();
        LiveScript s = new LiveScript();
        s.setDurationLimitSec(60);
        s.setScriptContent("短");

        Map<String, Object> fit = svc.mergeDurationFitHint(s);

        assertThat(fit.get("ok")).isEqualTo(true);
        assertThat(s.getAiSuggestion()).isNull();
    }

    @Test
    void mergeDurationFitHint_nullScript_returnsUnchecked() {
        LiveScriptQualityServiceImpl svc = new LiveScriptQualityServiceImpl();
        Map<String, Object> fit = svc.mergeDurationFitHint(null);
        assertThat(fit.get("checked")).isEqualTo(false);
    }
}
