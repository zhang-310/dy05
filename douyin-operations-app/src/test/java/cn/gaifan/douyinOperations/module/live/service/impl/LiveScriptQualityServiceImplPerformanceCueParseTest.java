package cn.gaifan.douyinOperations.module.live.service.impl;

import cn.gaifan.douyinOperations.module.live.config.LiveGenerationProperties;
import cn.gaifan.douyinOperations.module.live.entity.LiveScript;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Q-5：structured-json-enabled 时优先解析 JSON 数组；失败回退到行文本。
 */
class LiveScriptQualityServiceImplPerformanceCueParseTest {

    @Test
    void applyPerformanceCueLlmResponse_structuredJsonArrayAddsHints() throws Exception {
        LiveScriptQualityServiceImpl svc = new LiveScriptQualityServiceImpl();
        LiveScript script = new LiveScript();
        script.setScriptContent("这条话术足够长用来触发情感统计但不要求真实模型 ".repeat(2));
        LiveGenerationProperties.PerformanceCueLlm cfg = new LiveGenerationProperties.PerformanceCueLlm();
        cfg.setStructuredJsonEnabled(true);

        Method m = LiveScriptQualityServiceImpl.class.getDeclaredMethod(
                "applyPerformanceCueLlmResponse", LiveScript.class, String.class, LiveGenerationProperties.PerformanceCueLlm.class);
        m.setAccessible(true);
        int added = (Integer) m.invoke(svc, script, "[\"加强眼神停留\", \"结尾互动提问\"]", cfg);
        assertTrue(added >= 1);
        assertTrue(script.getAiSuggestion() != null && script.getAiSuggestion().contains("[表演·LLM]"));
    }

    @Test
    void applyPerformanceCueLlmResponse_fallbackToLinesWhenJsonInvalid() throws Exception {
        LiveScriptQualityServiceImpl svc = new LiveScriptQualityServiceImpl();
        LiveScript script = new LiveScript();
        script.setScriptContent("这条话术足够长用来触发情感统计但不要求真实模型 ".repeat(2));
        LiveGenerationProperties.PerformanceCueLlm cfg = new LiveGenerationProperties.PerformanceCueLlm();
        cfg.setStructuredJsonEnabled(true);

        Method m = LiveScriptQualityServiceImpl.class.getDeclaredMethod(
                "applyPerformanceCueLlmResponse", LiveScript.class, String.class, LiveGenerationProperties.PerformanceCueLlm.class);
        m.setAccessible(true);
        int added = (Integer) m.invoke(svc, script, "不是json\n第二行提示", cfg);
        assertTrue(added >= 1);
    }
}
